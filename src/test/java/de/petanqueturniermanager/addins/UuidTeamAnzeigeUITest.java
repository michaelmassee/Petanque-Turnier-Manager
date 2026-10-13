package de.petanqueturniermanager.addins;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import com.sun.star.sheet.XSpreadsheet;
import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.NrZellFormel;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.Position;

class UuidTeamAnzeigeUITest extends BaseCalcUITest {
    private static final String UUID = "abba7c10-dbbf-4910-b7ec-b78af3070b1f";

    static Stream<Arguments> anzeigeVarianten() {
        var faelle = new java.util.ArrayList<Arguments>();
        for (var formation : de.petanqueturniermanager.basesheet.meldeliste.Formation.values()) {
            for (boolean teamnameSpalte : new boolean[] {false, true}) {
                if (!formation.hatSpielerSpalte() && !teamnameSpalte) continue;
                for (boolean verein : new boolean[] {false, true}) {
                    for (boolean teamnameAnzeigen : new boolean[] {false, true}) {
                        if (teamnameAnzeigen && !teamnameSpalte) continue;
                        var namen = new java.util.ArrayList<String>();
                        if (teamnameSpalte) namen.add(" Les Tireurs ");
                        var spieler = new java.util.ArrayList<String>();
                        for (int i = 1; i <= formation.getAnzSpieler(); i++) {
                            namen.add(" Vorname" + i + " ");
                            namen.add(" Nachname" + i + " ");
                            if (verein) namen.add(" Club" + i + " ");
                            spieler.add("Vorname" + i + " Nachname" + i + (verein ? " (Club" + i + ")" : ""));
                        }
                        String erwartet = teamnameAnzeigen ? "Les Tireurs" : String.join(" / ", spieler);
                        faelle.add(Arguments.of(formation.name(), teamnameSpalte, teamnameAnzeigen, verein,
                                formation.getAnzSpieler(), namen.toArray(String[]::new), erwartet));
                    }
                }
            }
        }
        return faelle.stream();
    }

    @ParameterizedTest(name = "{0}: Teamspalte={1}, Teamname={2}, Verein={3}")
    @MethodSource("anzeigeVarianten")
    void neueFormelEntsprichtDerBisherigenKombination(String formation, boolean teamnameSpalte, boolean teamname,
            boolean verein, int anzahl, String[] namen, String erwartet) throws Exception {
        XSpreadsheet sheet = sheetHlp.getSheetByIdx(0);
        schreibeTeam(sheet, 3, UUID, 7, namen);
        String bereich = "$A$2:$Z$20";
        boolean versatz = teamnameSpalte && !teamname;
        String neu = GlobalImpl.FORMAT_PTM_UUID_TEAM_ANZEIGE(teamname, anzahl, verein, "\"" + UUID + "\"", bereich, versatz);
        String alt = GlobalImpl.FORMAT_PTM_TEAM_ANZEIGE(teamname, anzahl, verein,
                "INDEX(" + (versatz ? "$B$2:$Z$20" : bereich) + ";PTM.ALG.UUIDZEILE(\"" + UUID + "\";$A$2:$A$20);0)");
        sheetHlp.setFormulaInCell(sheet, Position.from(26, 0), neu);
        sheetHlp.setFormulaInCell(sheet, Position.from(27, 0), alt);

        assertThat(sheetHlp.getTextFromCell(sheet, Position.from(26, 0))).isEqualTo(erwartet)
                .isEqualTo(sheetHlp.getTextFromCell(sheet, Position.from(27, 0)));
        assertThat(sheetHlp.getFormulaFromCell(sheet, Position.from(26, 0))).containsIgnoringCase("PTMUUIDTEAMANZEIGE");
    }

    @Test
    void namenUndZeilenAenderungenWerdenOhneNeueFormelUebernommen() throws Exception {
        XSpreadsheet sheet = sheetHlp.getSheetByIdx(0);
        schreibeTeam(sheet, 3, UUID, 7, new String[] {"Anna", "Albatros", "Bert", "Boule"});
        Position anzeige = Position.from(26, 0);
        sheetHlp.setFormulaInCell(sheet, anzeige,
                GlobalImpl.FORMAT_PTM_UUID_TEAM_ANZEIGE(false, 2, false, "\"" + UUID + "\"", "$A$2:$Z$20", false));
        assertThat(sheetHlp.getTextFromCell(sheet, anzeige)).isEqualTo("Anna Albatros / Bert Boule");

        schreibeTeam(sheet, 8, UUID, 42, new String[] {"Clara", "Calc", "Dora", "Doublette"});
        schreibeTeam(sheet, 3, "e9e9caec-e0b1-4fe0-8fee-a229279b9f73", 7,
                new String[] {"Andere", "Meldung", "Andere", "Person"});
        assertThat(sheetHlp.getTextFromCell(sheet, anzeige)).isEqualTo("Clara Calc / Dora Doublette");

        Lo.qi(com.sun.star.text.XTextRange.class, sheet.getCellByPosition(1, 8)).setString("Christa");
        assertThat(sheetHlp.getTextFromCell(sheet, anzeige)).isEqualTo("Christa Calc / Dora Doublette");
    }

    @Test
    void fehlendeUuidErbtKeineAndereMeldung() throws Exception {
        XSpreadsheet sheet = sheetHlp.getSheetByIdx(0);
        schreibeTeam(sheet, 3, "e9e9caec-e0b1-4fe0-8fee-a229279b9f73", 7, new String[] {"Anna", "Albatros"});
        Position anzeige = Position.from(26, 0);
        sheetHlp.setFormulaInCell(sheet, anzeige,
                GlobalImpl.FORMAT_PTM_UUID_TEAM_ANZEIGE(false, 2, false, "\"" + UUID + "\"", "$A$2:$Z$20", false));
        assertThat(sheet.getCellByPosition(26, 0).getError()).isNotZero();
        assertThat(sheetHlp.getTextFromCell(sheet, anzeige)).doesNotContain("Anna");
    }

    @ParameterizedTest
    @EnumSource(de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode.class)
    void supermeleeMeldelisteFunktioniertInBeidenModi(
            de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode modus) throws Exception {
        var meldeliste = new de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New(wkingSpreadsheet);
        meldeliste.createMeldelisteWithParams(modus);
        XSpreadsheet sheet = meldeliste.getXSpreadSheet();
        schreibeTeam(sheet, meldeliste.getErsteDatenZiele(), UUID, 7, new String[] {"Anna", "Albatros"});
        sheetHlp.setFormulaInCell(sheet, Position.from(26, 0), GlobalImpl.FORMAT_PTM_UUID_TEAM_ANZEIGE(
                false, 1, false, "\"" + UUID + "\"", "$A$1:$Z$999", false));
        assertThat(sheetHlp.getTextFromCell(sheet, Position.from(26, 0))).isEqualTo("Anna Albatros");
    }

    @Test
    void expliziterStandardVersatzUndGrossgeschriebeneUuid() throws Exception {
        XSpreadsheet sheet = sheetHlp.getSheetByIdx(0);
        schreibeTeam(sheet, 3, UUID, 7, new String[] {"Anna", "Albatros"});
        String basis = GlobalImpl.FORMAT_PTM_UUID_TEAM_ANZEIGE(false, 1, false,
                "\"" + UUID.toUpperCase(java.util.Locale.ROOT) + "\"", "$A$2:$Z$20", false);
        sheetHlp.setFormulaInCell(sheet, Position.from(26, 0), basis);
        sheetHlp.setFormulaInCell(sheet, Position.from(27, 0), basis.substring(0, basis.length() - 1) + ";0)");
        assertThat(sheetHlp.getTextFromCell(sheet, Position.from(26, 0))).isEqualTo("Anna Albatros");
        assertThat(sheetHlp.getTextFromCell(sheet, Position.from(27, 0))).isEqualTo("Anna Albatros");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ungueltig", " abba7c10-dbbf-4910-b7ec-b78af3070b1f "})
    void ungueltigeUuidLiefertFehler(String uuid) throws Exception {
        XSpreadsheet sheet = sheetHlp.getSheetByIdx(0);
        schreibeTeam(sheet, 3, UUID, 7, new String[] {"Anna", "Albatros"});
        sheetHlp.setFormulaInCell(sheet, Position.from(26, 0), GlobalImpl.FORMAT_PTM_UUID_TEAM_ANZEIGE(
                false, 1, false, "\"" + uuid + "\"", "$A$2:$Z$20", false));
        assertThat(sheet.getCellByPosition(26, 0).getError()).isNotZero();
    }

    static Stream<Arguments> leereNamen() {
        return Stream.of(
                Arguments.of(new String[] {"", "", ""}, ""),
                Arguments.of(new String[] {" Anna ", "", ""}, "Anna"),
                Arguments.of(new String[] {"", " Albatros ", ""}, "Albatros"),
                Arguments.of(new String[] {"Anna", "Albatros", "", "", "", "Nur Club", "Dora", "Doublette", "West"},
                        "Anna Albatros / Dora Doublette (West)"));
    }

    @ParameterizedTest
    @MethodSource("leereNamen")
    void leereUndTeilweiseNamenOhneUeberfluessigeTrenner(String[] namen, String erwartet) throws Exception {
        XSpreadsheet sheet = sheetHlp.getSheetByIdx(0);
        schreibeTeam(sheet, 3, UUID, 7, namen);
        sheetHlp.setFormulaInCell(sheet, Position.from(26, 0), GlobalImpl.FORMAT_PTM_UUID_TEAM_ANZEIGE(
                false, 3, true, "\"" + UUID + "\"", "$A$2:$Z$20", false));
        assertThat(sheetHlp.getTextFromCell(sheet, Position.from(26, 0))).isEqualTo(erwartet);
    }

    @Test
    void gleicheUuidInZweiDokumentenVerwendetJeweilsDenEigenenBereich() throws Exception {
        var zweites = de.petanqueturniermanager.comp.OfficeDocumentHelper.from(loader).createCalc();
        try {
            XSpreadsheet a = sheetHlp.getSheetByIdx(0);
            XSpreadsheet b = Lo.qi(XSpreadsheet.class,
                    zweites.getSheets().getByName(zweites.getSheets().getElementNames()[0]));
            schreibeTeam(a, 3, UUID, 7, new String[] {"Anna", "Dokument A"});
            schreibeTeam(b, 3, UUID, 7, new String[] {"Bert", "Dokument B"});
            String formel = "=" + GlobalImpl.FORMAT_PTM_UUID_TEAM_ANZEIGE(false, 1, false,
                    "\"" + UUID + "\"", "$A$2:$Z$20", false);
            a.getCellByPosition(26, 0).setFormula(formel);
            b.getCellByPosition(26, 0).setFormula(formel);
            Lo.qi(com.sun.star.frame.XModel.class, zweites).getCurrentController().getFrame().activate();
            assertThat(Lo.qi(com.sun.star.text.XTextRange.class, a.getCellByPosition(26, 0)).getString())
                    .isEqualTo("Anna Dokument A");
            assertThat(Lo.qi(com.sun.star.text.XTextRange.class, b.getCellByPosition(26, 0)).getString())
                    .isEqualTo("Bert Dokument B");
        } finally {
            de.petanqueturniermanager.comp.OfficeDocumentHelper.closeDoc(zweites);
        }
    }

    @Test
    void uuidAusZellreferenzWechseltDieTeamanzeige() throws Exception {
        XSpreadsheet sheet = sheetHlp.getSheetByIdx(0);
        String andereUuid = "e9e9caec-e0b1-4fe0-8fee-a229279b9f73";
        schreibeTeam(sheet, 3, UUID, 7, new String[] {"Anna", "Albatros"});
        schreibeTeam(sheet, 5, andereUuid, 8, new String[] {"Bert", "Boule"});
        var uuidZelle = Lo.qi(com.sun.star.text.XTextRange.class, sheet.getCellByPosition(26, 0));
        uuidZelle.setString(UUID);
        sheetHlp.setFormulaInCell(sheet, Position.from(27, 0), GlobalImpl.FORMAT_PTM_UUID_TEAM_ANZEIGE(
                false, 1, false, "$AA$1", "$A$2:$Z$20", false));
        assertThat(sheetHlp.getTextFromCell(sheet, Position.from(27, 0))).isEqualTo("Anna Albatros");
        uuidZelle.setString(andereUuid);
        assertThat(sheetHlp.getTextFromCell(sheet, Position.from(27, 0))).isEqualTo("Bert Boule");
    }

    @Test
    void leererTeamnameBleibtLeerUndLiestKeineSpielerAlsTeamname() throws Exception {
        XSpreadsheet sheet = sheetHlp.getSheetByIdx(0);
        schreibeTeam(sheet, 3, UUID, 7, new String[] {"", "Anna", "Albatros"});
        sheetHlp.setFormulaInCell(sheet, Position.from(26, 0), GlobalImpl.FORMAT_PTM_UUID_TEAM_ANZEIGE(
                true, 1, false, "\"" + UUID + "\"", "$A$2:$Z$20", false));
        assertThat(sheet.getCellByPosition(26, 0).getError()).isZero();
        assertThat(sheetHlp.getTextFromCell(sheet, Position.from(26, 0))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "2", "1.5"})
    void ungueltigerSpaltenversatzLiefertFehler(String versatz) throws Exception {
        XSpreadsheet sheet = sheetHlp.getSheetByIdx(0);
        schreibeTeam(sheet, 3, UUID, 7, new String[] {"Anna", "Albatros"});
        String basis = GlobalImpl.FORMAT_PTM_UUID_TEAM_ANZEIGE(false, 1, false,
                "\"" + UUID + "\"", "$A$2:$Z$20", false);
        sheetHlp.setFormulaInCell(sheet, Position.from(26, 0), basis.substring(0, basis.length() - 1) + ";" + versatz + ")");
        assertThat(sheet.getCellByPosition(26, 0).getError()).isNotZero();
    }

    private void schreibeTeam(XSpreadsheet sheet, int zeile, String uuid, int nr, String[] namen) throws Exception {
        sheetHlp.setFormulaInCell(sheet, Position.from(0, zeile), NrZellFormel.team(uuid).formel(nr));
        for (int i = 0; i < namen.length; i++) {
            Lo.qi(com.sun.star.text.XTextRange.class, sheet.getCellByPosition(i + 1, zeile)).setString(namen[i]);
        }
    }
}
