package de.petanqueturniermanager.jedergegenjeden;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_Update;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteSheet;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJTriplette2Gruppen4TurnierTestDaten;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJTurnierTestDaten;

/**
 * Dokumentiert das aktuelle Verhalten von JGJ (Jeder-gegen-Jeden) bei einem Ausstieg NACH bereits
 * erstelltem Spielplan: wie bei Poule/KO/TripTete wird der komplette Round-Robin-Spielplan in einem
 * einzigen Schritt erzeugt ({@link JGJTurnierTestDaten#generate()}) – es gibt kein "nächste
 * Runde"-Konzept. JGJ kennt außerdem KEINEN eigenen "Ausgestiegen"-Wert (nur
 * {@code AKTIV_WERT_NIMMT_TEIL = 1}, siehe {@code JGJMeldeListeDelegate}) – ein Ausstieg wird daher
 * durch Leeren der Aktiv-Zelle simuliert, dem einzigen verfügbaren "nicht aktiv"-Zustand.
 * <p>
 * Anders als bei Poule/TripTete liest {@link JGJRanglisteSheet#upDateSheet()} bei jedem Aufbau
 * {@code getAktiveMeldungen()} und iteriert bei der Statistikberechnung strikt über diese gefilterte
 * Liste (kein automatisches Nachregistrieren aus dem Spielplan wie bei TripTete) – ein
 * nachträglich deaktiviertes Team fällt daher aus der Rangliste raus, obwohl der Spielplan selbst
 * unverändert bleibt. Dieser Test hält beides bewusst fest.
 */
public class JGJAusstiegMittenImTurnierUITest extends BaseCalcUITest {

    private static final int AUSGESTIEGENES_TEAM_NR = 1;

    private JGJTurnierTestDaten testDaten;

    @BeforeEach
    public void setup() {
        testDaten = new JGJTurnierTestDaten(wkingSpreadsheet);
    }

    @Test
    public void ausstiegNachTurnierende_aendertSpielplanNichtEntferntAberAusRangliste() throws GenerateException {
        testDaten.generate();

        List<List<Integer>> spielplanVorAusstieg = leseSpielplanWerte();

        // Team deaktivieren (Aktiv-Zelle leeren - JGJ kennt keinen eigenen "Ausgestiegen"-Wert)
        JGJMeldeListeSheet_Update meldeliste = new JGJMeldeListeSheet_Update(wkingSpreadsheet);
        int zeile = meldeliste.getSpielerZeileNr(AUSGESTIEGENES_TEAM_NR);
        assertThat(zeile).as("Team %d muss in der Meldeliste gefunden werden", AUSGESTIEGENES_TEAM_NR)
                .isGreaterThan(0);
        sheetHlp.clearValInCell(meldeliste.getXSpreadSheet(), Position.from(meldeliste.getAktivSpalte(), zeile));

        // Spielplan darf durch den nachträglichen Ausstieg nicht verändert werden - es gibt keinen
        // Regenerierungs-Mechanismus, der ihn überhaupt anfassen würde.
        List<List<Integer>> spielplanNachAusstieg = leseSpielplanWerte();
        assertThat(spielplanNachAusstieg)
                .as("Spielplan darf durch einen nachträglichen Ausstieg nicht verändert werden")
                .isEqualTo(spielplanVorAusstieg);

        // Rangliste erneut aufbauen: liest getAktiveMeldungen() neu ein, das deaktivierte Team fällt raus.
        new JGJRanglisteSheet(wkingSpreadsheet).upDateSheet();

        XSpreadsheet rangliste = sheetHlp.findByName(SheetNamen.rangliste());
        assertThat(rangliste).as("Rangliste-Sheet").isNotNull();
        RangePosition ranglisteRange = RangePosition.from(
                JGJRanglisteSheet.TEAM_NR_SPALTE, JGJRanglisteSheet.ERSTE_DATEN_ZEILE,
                JGJRanglisteSheet.TEAM_NR_SPALTE, JGJRanglisteSheet.ERSTE_DATEN_ZEILE + 20);
        RangeData ranglisteDaten = RangeHelper
                .from(rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument(), ranglisteRange)
                .getDataFromRange();
        boolean ausgestiegenesTeamInRangliste = ranglisteDaten.stream()
                .anyMatch(row -> !row.isEmpty() && row.get(0).getIntVal(-1) == AUSGESTIEGENES_TEAM_NR);
        assertThat(ausgestiegenesTeamInRangliste)
                .as("Deaktiviertes Team wird konsequent nicht mehr in der Rangliste geführt")
                .isFalse();
    }

    /**
     * Regression: Im Gruppenmodus bleibt jedes Team nach einem Ausstieg in der Gruppe, in der es laut Spielplan
     * gespielt hat. Früher teilte die Rangliste die aktiven Teams neu auf, sodass ein Team aus Gruppe B in Gruppe A
     * rutschte.
     */
    @Test
    public void ausstiegImGruppenmodus_verschiebtKeineTeamsZwischenGruppen() throws GenerateException {
        new JGJTriplette2Gruppen4TurnierTestDaten(wkingSpreadsheet).generate();
        List<List<Integer>> gruppenVorAusstieg = leseRanglisteGruppen();
        assertThat(gruppenVorAusstieg).as("Testturnier hat zwei Gruppen à 4 Teams").hasSize(2)
                .allSatisfy(gruppe -> assertThat(gruppe).hasSize(4));
        int aussteiger = gruppenVorAusstieg.get(0).get(0);

        JGJMeldeListeSheet_Update meldeliste = new JGJMeldeListeSheet_Update(wkingSpreadsheet);
        sheetHlp.clearValInCell(meldeliste.getXSpreadSheet(),
                Position.from(meldeliste.getAktivSpalte(), meldeliste.getSpielerZeileNr(aussteiger)));
        new JGJRanglisteSheet(wkingSpreadsheet).upDateSheet();

        List<List<Integer>> gruppenNachAusstieg = leseRanglisteGruppen();
        List<Integer> erwarteteGruppeA = gruppenVorAusstieg.get(0).stream().filter(nr -> nr != aussteiger).toList();
        assertThat(gruppenNachAusstieg).hasSize(2);
        assertThat(gruppenNachAusstieg.get(0)).as("Gruppe A ohne den Aussteiger")
                .containsExactlyInAnyOrderElementsOf(erwarteteGruppeA);
        assertThat(gruppenNachAusstieg.get(1)).as("Gruppe B bleibt unverändert")
                .containsExactlyInAnyOrderElementsOf(gruppenVorAusstieg.get(1));
    }

    /** Teamnummern je Gruppe aus der Rangliste (Gruppen-Kopfzeilen tragen den Gruppennamen). */
    private List<List<Integer>> leseRanglisteGruppen() throws GenerateException {
        XSpreadsheet rangliste = sheetHlp.findByName(SheetNamen.rangliste());
        assertThat(rangliste).as("Rangliste-Sheet").isNotNull();
        RangeData daten = RangeHelper.from(rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                RangePosition.from(JGJRanglisteSheet.TEAM_NR_SPALTE, JGJRanglisteSheet.ERSTE_DATEN_ZEILE,
                        JGJRanglisteSheet.TEAM_NR_SPALTE, JGJRanglisteSheet.ERSTE_DATEN_ZEILE + 30))
                .getDataFromRange();
        String gruppenName = I18n.get("jgj.gruppe.name");
        List<List<Integer>> gruppen = new ArrayList<>();
        for (var zeile : daten) {
            String text = zeile.get(0).getStringVal();
            int nr = zeile.get(0).getIntVal(-1);
            if (text != null && text.startsWith(gruppenName)) {
                gruppen.add(new ArrayList<>());
            } else if (nr > 0 && !gruppen.isEmpty()) {
                gruppen.get(gruppen.size() - 1).add(nr);
            } else {
                break;
            }
        }
        return gruppen;
    }

    private List<List<Integer>> leseSpielplanWerte() throws GenerateException {
        XSpreadsheet sheet = sheetHlp.findByName(SheetNamen.spielplan());
        assertThat(sheet).as("Spielplan-Sheet muss vorhanden sein").isNotNull();
        RangePosition range = RangePosition.from(
                JGJSpielPlanSheet.SPIEL_NR_SPALTE, JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
                JGJSpielPlanSheet.SPIELPNKT_B_SPALTE, JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE + 500);
        RangeData data = RangeHelper.from(sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), range)
                .getDataFromRange();

        List<List<Integer>> werte = new ArrayList<>();
        for (var row : data) {
            List<Integer> zeile = new ArrayList<>();
            for (var cell : row) {
                zeile.add(cell.getIntVal(0));
            }
            werte.add(zeile);
        }
        return werte;
    }
}
