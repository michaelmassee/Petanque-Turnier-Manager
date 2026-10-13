package de.petanqueturniermanager.supermelee.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerListeSortModus;
import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerSheetBuilder;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.supermelee.SpielTagNr;

public class SupermeleeTeilnehmerNamenUITest extends BaseCalcUITest {
    private static final String[][] NAMEN = {
            { "Anna", "Zimmer" }, { "Zoe", "Adams" }, { "Berta", "Müller" },
            { "Chris", "Bauer" }, { "Erik", "Weber" }, { "Fiona", "Klein" }
    };

    @ParameterizedTest
    @EnumSource(value = TeilnehmerListeSortModus.class, names = { "NAME", "NUMMER" })
    public void uuidNamenMitKonfigurierterSortierung(TeilnehmerListeSortModus sortierung) throws Exception {
        erstelle(sortierung);
        XSpreadsheet teilnehmer = teilnehmer().getXSpreadSheet();
        int[] reihenfolge = sortierung == TeilnehmerListeSortModus.NAME
                ? new int[] { 2, 4, 6, 3, 5, 1 } : new int[] { 1, 2, 3, 4, 5, 6 };
        for (int index = 0; index < reihenfolge.length; index++) {
            int nr = reihenfolge[index];
            int zeile = TeilnehmerSheetBuilder.ERSTE_DATEN_ZEILE + index;
            assertThat(sheetHlp.getIntFromCell(teilnehmer, Position.from(0, zeile))).isEqualTo(nr);
            pruefeUuidFormeln(teilnehmer, 0, zeile);
            assertThat(sheetHlp.getTextFromCell(teilnehmer, Position.from(1, zeile)))
                    .isEqualTo(NAMEN[nr - 1][0] + " " + NAMEN[nr - 1][1]);
        }
    }

    @Test
    public void aktualisierenBehaeltFormatBeiEinzelnamen() throws Exception {
        XSpreadsheet meldeliste = erstelle(TeilnehmerListeSortModus.NUMMER);
        schreibeName(meldeliste, 2, "", "Zimmer");
        schreibeName(meldeliste, 3, "Zoe", "");
        schreibeName(meldeliste, 4, "Neue Berta", "Neuer Nachname");
        new SupermeleeTeilnehmerSheetUpdate(wkingSpreadsheet).doRun();
        XSpreadsheet teilnehmer = teilnehmer().getXSpreadSheet();
        int erste = TeilnehmerSheetBuilder.ERSTE_DATEN_ZEILE;
        assertThat(sheetHlp.getTextFromCell(teilnehmer, Position.from(1, erste))).isEqualTo("Zimmer");
        assertThat(sheetHlp.getTextFromCell(teilnehmer, Position.from(1, erste + 1))).isEqualTo("Zoe");
        assertThat(sheetHlp.getTextFromCell(teilnehmer, Position.from(1, erste + 2)))
                .isEqualTo("Neue Berta Neuer Nachname");
    }

    @Test
    public void mehrereBloeckeBehaltenUuidBeiNummernUndNamensaenderung() throws Exception {
        XSpreadsheet meldeliste = erstelle(TeilnehmerListeSortModus.NUMMER);
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_ANZ_TEILNEHMER_IN_SPALTE, 2);
        new SupermeleeTeilnehmerSheetUpdate(wkingSpreadsheet).doRun();
        XSpreadsheet sheet = teilnehmer().getXSpreadSheet();
        for (int index = 0; index < 6; index++) {
            int spalte = (index / 2) * 3;
            int zeile = TeilnehmerSheetBuilder.ERSTE_DATEN_ZEILE + index % 2;
            pruefeUuidFormeln(sheet, spalte, zeile);
            assertThat(sheetHlp.getIntFromCell(sheet, Position.from(spalte, zeile))).isEqualTo(index + 1);
            assertThat(sheetHlp.getTextFromCell(sheet, Position.from(spalte + 1, zeile)))
                    .isEqualTo(NAMEN[index][0] + " " + NAMEN[index][1]);
        }
        // Gleiche UUID und dieselbe Zeile: Nummer und Name aktualisieren ohne Neuaufbau des Aushangs.
        var nrZelle = meldeliste.getCellByPosition(0, 2);
        String uuid = de.petanqueturniermanager.basesheet.meldeliste.TeamUuidFormel.findeUuid(nrZelle.getFormula()).orElseThrow();
        nrZelle.setFormula("=" + de.petanqueturniermanager.addins.GlobalImpl.FORMAT_PTM_TNR_UUID(42, uuid));
        schreibeName(meldeliste, 2, "Neue Anna", "Neuer Nachname");
        recalcAll();
        assertThat(sheetHlp.getIntFromCell(sheet, Position.from(0, 1))).isEqualTo(42);
        assertThat(sheetHlp.getTextFromCell(sheet, Position.from(1, 1))).isEqualTo("Neue Anna Neuer Nachname");
        pruefeUuidFormeln(sheet, 0, 1);
    }

    @Test
    public void andereUuidBeiGleicherNummerUebernimmtKeinenTeilnehmer() throws Exception {
        XSpreadsheet meldeliste = erstelle(TeilnehmerListeSortModus.NUMMER);
        XSpreadsheet sheet = teilnehmer().getXSpreadSheet();
        var nrZelle = meldeliste.getCellByPosition(0, 2);
        String uuid = de.petanqueturniermanager.basesheet.meldeliste.TeamUuidFormel.findeUuid(nrZelle.getFormula()).orElseThrow();
        nrZelle.setFormula(nrZelle.getFormula().replace(uuid, "ffffffff-0000-4000-8000-000000000001"));
        recalcAll();
        assertThat(sheet.getCellByPosition(0, 1).getError()).isNotZero();
        assertThat(sheet.getCellByPosition(1, 1).getError()).isNotZero();
    }

    private void pruefeUuidFormeln(XSpreadsheet sheet, int spalte, int zeile) {
        String nrFormel = sheetHlp.getFormulaFromCell(sheet, Position.from(spalte, zeile));
        String uuid = de.petanqueturniermanager.basesheet.meldeliste.TeamUuidFormel.findeUuid(nrFormel).orElseThrow();
        assertThat(sheetHlp.getFormulaFromCell(sheet, Position.from(spalte + 1, zeile)))
                .containsIgnoringCase("PTMUUIDTEAMANZEIGE").contains(uuid).doesNotContain("MATCH(", "VLOOKUP", "IFNA");
    }

    private XSpreadsheet erstelle(TeilnehmerListeSortModus sortierung) throws Exception {
        var test = new TestSuperMeleeMeldeListeErstellen(wkingSpreadsheet, doc);
        test.initMitAlleDieSpielen(6);
        docPropHelper.setStringProperty(BasePropertiesSpalte.KONFIG_PROP_TEILNEHMER_LISTE_SORT_MODUS, sortierung.getKey());
        XSpreadsheet meldeliste = test.getMeldeListeSheetNew().getXSpreadSheet();
        for (int i = 0; i < NAMEN.length; i++) {
            schreibeName(meldeliste, 2 + i, NAMEN[i][0], NAMEN[i][1]);
        }
        teilnehmer().generate();
        return meldeliste;
    }

    private SupermeleeTeilnehmerSheet teilnehmer() {
        var sheet = new SupermeleeTeilnehmerSheet(wkingSpreadsheet);
        sheet.setSpielTagNr(SpielTagNr.from(1));
        return sheet;
    }

    private void schreibeName(XSpreadsheet sheet, int zeile, String vorname, String nachname) {
        sheetHlp.setStringValueInCell(StringCellValue.from(sheet, Position.from(1, zeile), vorname));
        sheetHlp.setStringValueInCell(StringCellValue.from(sheet, Position.from(2, zeile), nachname));
    }
}
