/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.triptete.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel.MeldelisteStatus;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;

/**
 * Regressionstest: Trip-Tête hat keine SP-Spalte, die Aktiv-Spalte folgt direkt auf die Spieler.
 * Der Spieler-DB-Dialog las den Checkin eine Spalte zu weit rechts und zählte eingecheckte Teams
 * deshalb als nur angemeldet.
 */
class TripTeteSpielerDbZaehlerUITest extends BaseCalcUITest {

    private static final List<List<String>> TEAMS = List.of(
            List.of("Anna", "Arndt", "Bernd", "Bauer", "Clara", "Cordes"),
            List.of("Dieter", "Dahl", "Eva", "Ernst", "Frank", "Fuchs"));

    @ParameterizedTest(name = "Teamname={0}")
    @ValueSource(booleans = { true, false })
    void eingechecktesTeamZaehltAlsCheckin(boolean teamnameAktiv) throws Exception {
        TripTeteMeldeListeSheetNew meldeListe = new TripTeteMeldeListeSheetNew(wkingSpreadsheet);
        meldeListe.getKonfigurationSheet().setMeldeListeTeamnameAnzeigen(teamnameAktiv);
        meldeListe.createMeldeliste();
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
                TurnierSystem.TRIPTETE.getId());

        MeldelisteZiel ziel = MeldelisteZielFactory.fuerAktivesSheet(wkingSpreadsheet).orElseThrow();
        for (List<String> team : TEAMS) {
            ziel.schreibeBlock(List.of(spieler(team.get(0), team.get(1)), spieler(team.get(2), team.get(3)),
                    spieler(team.get(4), team.get(5))));
        }
        assertThat(ziel.getMeldelisteStatus()).as("Übernommene Teams sind nur angemeldet")
                .isEqualTo(new MeldelisteStatus(TEAMS.size(), 0, TEAMS.size()));

        TripTeteMeldeListeDelegate delegate = new TripTeteMeldeListeDelegate(meldeListe, wkingSpreadsheet);
        sheetHlp.setNumberValueInCell(NumberCellValue.from(meldeListe.getXSpreadSheet(),
                Position.from(delegate.getAktivSpalte(), TripTeteMeldeListeDelegate.ERSTE_DATEN_ZEILE_OVERRIDE))
                .setValue(1));

        assertThat(ziel.getMeldelisteStatus()).as("Checkin in der Trip-Tête-Aktiv-Spalte wird gezählt")
                .isEqualTo(new MeldelisteStatus(TEAMS.size() - 1, 1, TEAMS.size()));
    }

    private static SpielerMitVerein spieler(String vorname, String nachname) {
        return new SpielerMitVerein(0, vorname, nachname, null, null, List.of(), List.of(), null);
    }
}
