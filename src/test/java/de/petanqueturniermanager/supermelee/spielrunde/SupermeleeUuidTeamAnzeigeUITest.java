package de.petanqueturniermanager.supermelee.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TeamUuidFormel;
import de.petanqueturniermanager.basesheet.meldeliste.TeamnummerAendernSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;
import de.petanqueturniermanager.supermelee.meldeliste.TestSuperMeleeMeldeListeErstellen;

/** UUID-Namen und die zugehörigen Nummern müssen in Runde und Spielerübersicht dieselbe Meldung meinen. */
public class SupermeleeUuidTeamAnzeigeUITest extends BaseCalcUITest {
    private XSpreadsheet meldeliste;
    private XSpreadsheet runde;
    private XSpreadsheet plan;
    private final Map<String, String> namen = new HashMap<>();

    @BeforeEach
    public void seed() {
        RandomSource.setSeed(42L);
    }

    @AfterEach
    public void resetRandom() {
        RandomSource.reset();
    }

    @ParameterizedTest
    @CsvSource({ "Triplette,12", "Doublette,12", "Triplette,10", "Doublette,10" })
    public void uuidNamenInBeidenModiUndGemischtenRunden(SuperMeleeMode modus, int anzahl) throws Exception {
        erstelle(modus, anzahl);
        int spieler = 0;
        int leerePlaetze = 0;
        for (int zeile = SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE;
                zeile < SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE + anzahl / 4; zeile++) {
            for (int platz = 0; platz < 6; platz++) {
                Position nr = Position.from(SpielrundeSheetKonstanten.ERSTE_SPIELERNR_SPALTE + platz, zeile);
                Position name = Position.from(SpielrundeSheetKonstanten.ERSTE_SPALTE_RUNDESPIELPLAN + platz, zeile);
                String nrFormel = sheetHlp.getFormulaFromCell(runde, nr);
                if (nrFormel.isBlank()) {
                    assertThat(sheetHlp.getTextFromCell(runde, name)).isBlank();
                    assertThat(sheetHlp.getFormulaFromCell(runde, name)).doesNotContain("PTMTEAMANZEIGE", "MATCH(", "IFNA");
                    leerePlaetze++;
                } else {
                    pruefeName(runde, nr, name);
                    spieler++;
                }
            }
        }
        assertThat(spieler).isEqualTo(anzahl);
        if (modus == SuperMeleeMode.Doublette || anzahl == 10) {
            assertThat(leerePlaetze).isPositive();
        }
        for (int zeile = SpielrundePlan.ERSTE_DATEN_ZEILE;
                zeile < SpielrundePlan.ERSTE_DATEN_ZEILE + anzahl; zeile++) {
            pruefeName(plan, Position.from(SpielrundePlan.SPIELER_NR_SPALTE, zeile),
                    Position.from(SpielrundePlan.SPIELER_NAME_OFFS_SPALTE, zeile));
            assertThat(sheetHlp.getTextFromCell(plan, Position.from(SpielrundePlan.SPIELER_TEAM_OFFS_SPALTE, zeile)))
                    .isIn("A", "B");
            assertThat(plan.getCellByPosition(SpielrundePlan.SPIELER_BAHN_NR_OFFS_SPALTE, zeile).getError()).isZero();
        }
    }

    @Test
    public void nummerUndNameAendernBehaltenSpielerTeamUndBahn() throws Exception {
        erstelle(SuperMeleeMode.Triplette, 10);
        int zeile = SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE;
        Position nr = Position.from(SpielrundeSheetKonstanten.ERSTE_SPIELERNR_SPALTE, zeile);
        Position name = Position.from(SpielrundeSheetKonstanten.ERSTE_SPALTE_RUNDESPIELPLAN, zeile);
        String uuid = TeamUuidFormel.findeUuid(sheetHlp.getFormulaFromCell(runde, nr)).orElseThrow();
        int alteNr = sheetHlp.getIntFromCell(runde, nr);
        int planZeile = findePlanZeile(alteNr);
        String team = sheetHlp.getTextFromCell(plan, Position.from(SpielrundePlan.SPIELER_TEAM_OFFS_SPALTE, planZeile));
        String bahn = sheetHlp.getTextFromCell(plan, Position.from(SpielrundePlan.SPIELER_BAHN_NR_OFFS_SPALTE, planZeile));
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.SUPERMELEE.getId());
        new TeamnummerAendernSheet(wkingSpreadsheet).mitEingabe(alteNr, 42).doRun();
        int meldeZeile = findeMeldeZeile(uuid);
        sheetHlp.setStringValueInCell(StringCellValue.from(meldeliste, Position.from(1, meldeZeile), "Neuer Vorname"));
        sheetHlp.setStringValueInCell(StringCellValue.from(meldeliste, Position.from(2, meldeZeile), "Neuer Nachname"));
        recalcAll();
        assertThat(sheetHlp.getIntFromCell(runde, nr)).isEqualTo(42);
        assertThat(sheetHlp.getIntFromCell(plan, Position.from(SpielrundePlan.SPIELER_NR_SPALTE, planZeile))).isEqualTo(42);
        assertThat(sheetHlp.getTextFromCell(runde, name)).isEqualTo("Neuer Vorname Neuer Nachname");
        assertThat(sheetHlp.getTextFromCell(plan, Position.from(SpielrundePlan.SPIELER_NAME_OFFS_SPALTE, planZeile)))
                .isEqualTo("Neuer Vorname Neuer Nachname");
        assertThat(sheetHlp.getTextFromCell(plan, Position.from(SpielrundePlan.SPIELER_TEAM_OFFS_SPALTE, planZeile))).isEqualTo(team);
        assertThat(sheetHlp.getTextFromCell(plan, Position.from(SpielrundePlan.SPIELER_BAHN_NR_OFFS_SPALTE, planZeile))).isEqualTo(bahn);
    }

    @Test
    public void andereUuidBeiGleicherNummerUebernimmtKeineAlteRunde() throws Exception {
        erstelle(SuperMeleeMode.Triplette, 12);
        int zeile = SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE;
        Position nr = Position.from(SpielrundeSheetKonstanten.ERSTE_SPIELERNR_SPALTE, zeile);
        String uuid = TeamUuidFormel.findeUuid(sheetHlp.getFormulaFromCell(runde, nr)).orElseThrow();
        int planZeile = findePlanZeile(sheetHlp.getIntFromCell(runde, nr));
        var nrZelle = meldeliste.getCellByPosition(0, findeMeldeZeile(uuid));
        nrZelle.setFormula(nrZelle.getFormula().replace(uuid, "ffffffff-0000-4000-8000-000000000001"));
        recalcAll();
        assertThat(runde.getCellByPosition(SpielrundeSheetKonstanten.ERSTE_SPALTE_RUNDESPIELPLAN, zeile).getError()).isNotZero();
        assertThat(plan.getCellByPosition(SpielrundePlan.SPIELER_NAME_OFFS_SPALTE, planZeile).getError()).isNotZero();
        assertThat(sheetHlp.getTextFromCell(runde, Position.from(SpielrundeSheetKonstanten.ERSTE_SPALTE_RUNDESPIELPLAN, zeile))).isNotBlank();
    }

    private void erstelle(SuperMeleeMode modus, int anzahl) throws Exception {
        var test = new TestSuperMeleeMeldeListeErstellen(wkingSpreadsheet, doc);
        test.initMitAlleDieSpielen(anzahl);
        new SuperMeleeKonfigurationSheet(wkingSpreadsheet).setSuperMeleeMode(modus);
        meldeliste = test.getMeldeListeSheetNew().getXSpreadSheet();
        for (int zeile = 0; zeile < 40; zeile++) {
            var uuid = TeamUuidFormel.findeUuid(meldeliste.getCellByPosition(0, zeile).getFormula());
            if (uuid.isPresent()) {
                String vorname = sheetHlp.getTextFromCell(meldeliste, Position.from(1, zeile));
                String nachname = sheetHlp.getTextFromCell(meldeliste, Position.from(2, zeile));
                namen.put(uuid.get(), (vorname + " " + nachname).trim());
            }
        }
        new SpielrundeSheet_Naechste(wkingSpreadsheet).run();
        new SpielrundePlan(wkingSpreadsheet).run();
        runde = sheetHlp.findByName(SpielrundeSheetKonstanten.sheetName(1, 1));
        plan = sheetHlp.findByName(SheetNamen.spielrundePlan(1, 1));
        assertThat(runde).isNotNull();
        assertThat(plan).isNotNull();
        recalcAll();
    }

    private void pruefeName(XSpreadsheet sheet, Position nr, Position name) throws Exception {
        String uuid = TeamUuidFormel.findeUuid(sheetHlp.getFormulaFromCell(sheet, nr)).orElseThrow();
        assertThat(sheetHlp.getFormulaFromCell(sheet, name)).containsIgnoringCase("PTMUUIDTEAMANZEIGE")
                .contains(uuid).doesNotContain("MATCH(", "VLOOKUP", "IFNA");
        assertThat(sheetHlp.getTextFromCell(sheet, name)).isEqualTo(namen.get(uuid));
    }

    private int findePlanZeile(int nr) {
        for (int zeile = SpielrundePlan.ERSTE_DATEN_ZEILE; zeile < 40; zeile++) {
            if (sheetHlp.getIntFromCell(plan, Position.from(SpielrundePlan.SPIELER_NR_SPALTE, zeile)) == nr) {
                return zeile;
            }
        }
        throw new AssertionError("Spieler fehlt in Übersicht: " + nr);
    }

    private int findeMeldeZeile(String uuid) throws Exception {
        for (int zeile = 0; zeile < 40; zeile++) {
            if (TeamUuidFormel.findeUuid(meldeliste.getCellByPosition(0, zeile).getFormula()).filter(uuid::equals).isPresent()) {
                return zeile;
            }
        }
        throw new AssertionError("UUID fehlt: " + uuid);
    }
}
