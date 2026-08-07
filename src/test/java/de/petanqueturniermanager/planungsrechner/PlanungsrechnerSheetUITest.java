package de.petanqueturniermanager.planungsrechner;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.util.CellProtection;
import com.sun.star.util.XProtectable;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzRegistry;
import de.petanqueturniermanager.helper.sheet.numberformat.NumberFormatHelper;
import de.petanqueturniermanager.helper.sheet.numberformat.UserNumberFormat;
import de.petanqueturniermanager.toolbar.TurnierModus;

/**
 * UI-Tests für den eigenständigen, vom Turniersystem unabhängigen Planungsrechner.
 * <p>
 * Zwei vollständige Blöcke nebeneinander (gleiche Zeilen, unterschiedliche Spaltengruppen), jeder
 * mit eigenem Rundenplan. Die Ausgabezellen sind {@code PTM.PLANUNG.*}-Formeln (siehe
 * {@code GlobalImpl}) und rechnen live neu, sobald der Nutzer eine Eingabezelle ändert — es gibt
 * keinen Java-Recompute-Schritt mehr. Add-In-Formeln wertet LibreOffice über den UNO-Socket im
 * Test nicht synchron aus (siehe {@code GlobalImplUITest}), daher wird hier nur die Formel-
 * <em>Struktur</em> geprüft; die tatsächliche Berechnung ist über direkte {@code GlobalImpl}-
 * Aufrufe in {@code GlobalImplUITest} abgedeckt.
 */
public class PlanungsrechnerSheetUITest extends BaseCalcUITest {

    // Sheet-Layout (Spiegelbild der Konstanten in PlanungsrechnerSheet)
    private static final int SPALTE_A_LABEL = 0;
    private static final int SPALTE_A_WERT = 1;
    private static final int SPALTE_B_LABEL = 5;
    private static final int SPALTE_B_WERT = 6;

    private static final int ZEILE_START = 3;
    private static final int ZEILE_A_ENDE = 4;
    private static final int ZEILE_A_TEAMS = 5;
    private static final int ZEILE_A_BAHNEN = 6;
    private static final int ZEILE_B_ZEITLIMIT = 9;
    private static final int ZEILE_ERGEBNIS_DURCHGAENGE = 11;
    private static final int ZEILE_ERGEBNIS_HAUPT = 12;
    private static final int ERSTE_TAB_DATEN_ZEILE = 15;

    @Test
    public void testErstellungMitDefaultwerten_eingabezellen() throws Exception {
        new PlanungsrechnerSheet(wkingSpreadsheet).doRun();

        XSpreadsheet sheet = new PlanungsrechnerSheet(wkingSpreadsheet).getXSpreadSheet();
        assertThat(sheet).isNotNull();

        // Block A Defaults: 16 Teams, 3 Bahnen (Eingaben sind literale Werte, keine Formeln)
        assertThat(sheetHlp.getIntFromCell(sheet, Position.from(SPALTE_A_WERT, ZEILE_A_TEAMS))).isEqualTo(16);
        assertThat(sheetHlp.getIntFromCell(sheet, Position.from(SPALTE_A_WERT, ZEILE_A_BAHNEN))).isEqualTo(3);
        // Block B Default: Zeitlimit 15
        assertThat(sheetHlp.getTextFromCell(sheet, Position.from(SPALTE_B_WERT, ZEILE_B_ZEITLIMIT))).isEqualTo("15");
    }

    /**
     * Regression: die Uhrzeit-Eingabezellen (Start/Ende) müssen echte, mit HH:MM formatierte
     * Calc-Zeitwerte sein (Bruchteil des Tages), nicht bloß Text — sonst greift weder das
     * NumberFormat noch kann der Nutzer per Zeit-Eingabe/-Spinner bequem editieren.
     */
    @Test
    public void testUhrzeitEingabezellen_sindAlsZeitWerteFormatiert() throws Exception {
        new PlanungsrechnerSheet(wkingSpreadsheet).doRun();
        XSpreadsheet sheet = new PlanungsrechnerSheet(wkingSpreadsheet).getXSpreadSheet();

        int zeitFormatIdx = NumberFormatHelper.from(wkingSpreadsheet.getWorkingSpreadsheetDocument())
                .getIdx(UserNumberFormat.TIME);

        // Block A: Start 09:00, Ende 17:00 als Bruchteil des 24h-Tages
        assertThat(sheet.getCellByPosition(SPALTE_A_WERT, ZEILE_START).getValue())
                .as("Block A Start = 09:00").isEqualTo(9.0 / 24.0, Offset.offset(1e-9));
        assertThat(sheet.getCellByPosition(SPALTE_A_WERT, ZEILE_A_ENDE).getValue())
                .as("Block A Ende = 17:00").isEqualTo(17.0 / 24.0, Offset.offset(1e-9));
        // Block B: Start 09:00
        assertThat(sheet.getCellByPosition(SPALTE_B_WERT, ZEILE_START).getValue())
                .as("Block B Start = 09:00").isEqualTo(9.0 / 24.0, Offset.offset(1e-9));

        for (Position pos : new Position[] { Position.from(SPALTE_A_WERT, ZEILE_START),
                Position.from(SPALTE_A_WERT, ZEILE_A_ENDE), Position.from(SPALTE_B_WERT, ZEILE_START) }) {
            XPropertySet propSet = Lo.qi(XPropertySet.class, sheet.getCellByPosition(pos.getSpalte(), pos.getZeile()));
            assertThat(propSet.getPropertyValue("NumberFormat")).as("NumberFormat HH:MM fuer " + pos)
                    .isEqualTo(zeitFormatIdx);
        }
    }

    @Test
    public void testAusgabezellen_sindPtmPlanungFormeln() throws Exception {
        new PlanungsrechnerSheet(wkingSpreadsheet).doRun();
        XSpreadsheet sheet = new PlanungsrechnerSheet(wkingSpreadsheet).getXSpreadSheet();

        // Block A: Durchgaenge + Zeitlimit sind PTM.PLANUNG-Formeln
        assertThat(sheetHlp.getFormulaFromCell(sheet, Position.from(SPALTE_A_WERT, ZEILE_ERGEBNIS_DURCHGAENGE)))
                .as("Block A Durchgaenge pro Runde")
                .containsIgnoringCase("DURCHGAENGEPRORUNDE");
        assertThat(sheetHlp.getFormulaFromCell(sheet, Position.from(SPALTE_A_WERT, ZEILE_ERGEBNIS_HAUPT)))
                .as("Block A Zeitlimit")
                .containsIgnoringCase("ZEITLIMIT");
        assertThat(sheetHlp.getFormulaFromCell(sheet, Position.from(SPALTE_A_LABEL, ZEILE_ERGEBNIS_HAUPT)))
                .as("Block A Zeitlimit-Label wechselt live zwischen 'pro Runde'/'pro Durchgang'")
                .startsWith("=IF(");

        // Block B: Durchgaenge + Turnier-Ende sind PTM.PLANUNG-Formeln
        assertThat(sheetHlp.getFormulaFromCell(sheet, Position.from(SPALTE_B_WERT, ZEILE_ERGEBNIS_DURCHGAENGE)))
                .as("Block B Durchgaenge pro Runde")
                .containsIgnoringCase("DURCHGAENGEPRORUNDE");
        assertThat(sheetHlp.getFormulaFromCell(sheet, Position.from(SPALTE_B_WERT, ZEILE_ERGEBNIS_HAUPT)))
                .as("Block B Turnier-Ende")
                .containsIgnoringCase("TURNIERENDE");
    }

    @Test
    public void testRundenplaene_sindArrayFormelnUeberFesteZeilenzahlNebeneinander() throws Exception {
        new PlanungsrechnerSheet(wkingSpreadsheet).doRun();
        XSpreadsheet sheet = new PlanungsrechnerSheet(wkingSpreadsheet).getXSpreadSheet();

        int letzteZeile = ERSTE_TAB_DATEN_ZEILE + PlanungsrechnerRechner.MAX_ZEITPLAN_ZEILEN - 1;

        // Block A: eigener Rundenplan links (Spalten 0-3), mit dem selbst errechneten Zeitlimit
        assertThat(sheetHlp.getFormulaFromCell(sheet, Position.from(SPALTE_A_LABEL, ERSTE_TAB_DATEN_ZEILE)))
                .as("Rundenplan A beginnt mit PTM.PLANUNG.ZEITPLAN")
                .containsIgnoringCase("ZEITPLAN");
        assertThat(sheetHlp.getFormulaFromCell(sheet, Position.from(SPALTE_A_LABEL, letzteZeile)))
                .as("Rundenplan A: letzte Zeile der festen Obergrenze gehoert noch zur Array-Formel")
                .containsIgnoringCase("ZEITPLAN");

        // Block B: eigener Rundenplan rechts daneben (Spalten 5-8), mit dem eingegebenen Zeitlimit
        assertThat(sheetHlp.getFormulaFromCell(sheet, Position.from(SPALTE_B_LABEL, ERSTE_TAB_DATEN_ZEILE)))
                .as("Rundenplan B beginnt mit PTM.PLANUNG.ZEITPLAN")
                .containsIgnoringCase("ZEITPLAN");
        assertThat(sheetHlp.getFormulaFromCell(sheet, Position.from(SPALTE_B_LABEL, letzteZeile)))
                .as("Rundenplan B: letzte Zeile der festen Obergrenze gehoert noch zur Array-Formel")
                .containsIgnoringCase("ZEITPLAN");
    }

    /**
     * Zweiter Menüklick auf ein bereits bestehendes Sheet fragt „Neu erstellen?" (Standard-
     * Verhalten von {@code NewSheet.create()} ohne {@code .useIfExist()}). Im (headless) Test
     * beantwortet {@code MessageBox.setDialogeUeberspringen()} einen {@code WARN_YES_NO}-Dialog
     * automatisch mit Ja (siehe {@code MessageBox.show()}), das Sheet wird also neu aufgebaut —
     * das ist hier zugleich der Beleg, dass der Dialog tatsächlich verdrahtet ist: eine manuelle
     * Eingabe wird beim zweiten Aufruf auf den Default zurückgesetzt statt erhalten zu bleiben.
     */
    @Test
    public void testWiederholterMenueaufruf_bestehendesSheet_fragtNeuErstellenUndBautNeuAuf() throws Exception {
        new PlanungsrechnerSheet(wkingSpreadsheet).doRun();
        XSpreadsheet ersterAufruf = new PlanungsrechnerSheet(wkingSpreadsheet).getXSpreadSheet();

        // Eingabe manuell aendern, wie es der Nutzer vor einem erneuten Menueklick taete
        sheetHlp.setValInCell(ersterAufruf, Position.from(SPALTE_A_WERT, ZEILE_A_TEAMS), 24);

        new PlanungsrechnerSheet(wkingSpreadsheet).doRun();
        XSpreadsheet zweiterAufruf = new PlanungsrechnerSheet(wkingSpreadsheet).getXSpreadSheet();

        assertThat(sheetHlp.getIntFromCell(zweiterAufruf, Position.from(SPALTE_A_WERT, ZEILE_A_TEAMS)))
                .as("Neu erstellen (Ja) setzt die manuelle Eingabe auf den Default zurueck")
                .isEqualTo(16);
        assertThat(sheetHlp.getFormulaFromCell(zweiterAufruf, Position.from(SPALTE_A_WERT, ZEILE_ERGEBNIS_HAUPT)))
                .containsIgnoringCase("ZEITLIMIT");
    }

    @Test
    public void testBestehendesTurnier_planungsrechnerAendertNichtsAmFremdenTurnier() throws Exception {
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.SCHWEIZER.getId());
        sheetHlp.newIfNotExist("Fremdes-Sheet", (short) 0);
        XSpreadsheet fremdesSheet = sheetHlp.findByName("Fremdes-Sheet");
        sheetHlp.setStringValueInCell(StringCellValue.from(fremdesSheet, Position.from(0, 0), "Marker-Wert"));

        new PlanungsrechnerSheet(wkingSpreadsheet).doRun();
        // zweiter Aufruf auf das bereits existierende Planungsrechner-Sheet darf ebenfalls
        // nichts am fremden Turnier aendern
        new PlanungsrechnerSheet(wkingSpreadsheet).doRun();

        assertThat(docPropHelper.getTurnierSystemAusDocument()).isEqualTo(TurnierSystem.SCHWEIZER);
        assertThat(sheetHlp.findByName("Fremdes-Sheet")).isNotNull();
        assertThat(sheetHlp.getTextFromCell(sheetHlp.findByName("Fremdes-Sheet"), Position.from(0, 0)))
                .isEqualTo("Marker-Wert");
    }

    /**
     * Deckt den ursprünglich gemeldeten Bug ab: Sheet existiert bereits, Turnier-Modus wird
     * danach aktiviert (der reguläre Weg über {@code TurnierModus.aktivierenIntern()} ->
     * {@code BlattschutzManager.schuetzen()}, hier direkt nachgebaut statt über den UI-Toggle).
     */
    @Test
    public void testTurniermodus_planungsrechnerWirdGeschuetzt_eingabezellenBleibenOffen() throws Exception {
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.SCHWEIZER.getId());
        new PlanungsrechnerSheet(wkingSpreadsheet).doRun();
        XSpreadsheet sheet = new PlanungsrechnerSheet(wkingSpreadsheet).getXSpreadSheet();
        assertThat(sheet).isNotNull();

        var konfig = BlattschutzRegistry.fuer(TurnierSystem.SCHWEIZER).orElseThrow();
        TurnierModus.get().setAktivForTest(true);
        try {
            BlattschutzManager.get().schuetzen(konfig, wkingSpreadsheet);

            assertThat(Lo.qi(XProtectable.class, sheet).isProtected())
                    .as("Planungsrechner muss beim Aktivieren des Turniermodus mitgeschuetzt werden")
                    .isTrue();
            assertThat(istZelleEditierbar(sheet, SPALTE_A_WERT, ZEILE_A_TEAMS))
                    .as("Block A Eingabezelle bleibt trotz Schutz editierbar").isTrue();
            assertThat(istZelleEditierbar(sheet, SPALTE_B_WERT, ZEILE_B_ZEITLIMIT))
                    .as("Block B Eingabezelle bleibt trotz Schutz editierbar").isTrue();
            assertThat(istZelleEditierbar(sheet, SPALTE_A_WERT, ZEILE_ERGEBNIS_HAUPT))
                    .as("Ausgabezelle (Formel) bleibt gesperrt").isFalse();
        } finally {
            BlattschutzManager.get().entsperren(konfig, wkingSpreadsheet);
            TurnierModus.get().setAktivForTest(false);
        }
    }

    /**
     * Deckt den Fall ab, in dem der Turnier-Modus bereits aktiv ist, BEVOR das
     * Planungsrechner-Sheet je erzeugt wurde: {@link PlanungsrechnerSheet} deklariert
     * {@code TurnierSystem.KEIN}, daher öffnet {@code SheetRunner.run()} für seinen eigenen Lauf
     * nie den Blattschutz-Command-Scope — ohne den expliziten Nachzieh-Schritt in
     * {@code PlanungsrechnerSheet.doRun()} bliebe das frisch erzeugte Sheet bis zum nächsten
     * Kiosk-Toggle ungeschützt.
     */
    @Test
    public void testTurniermodus_bereitsAktivVorErstellung_planungsrechnerWirdTrotzdemGeschuetzt() throws Exception {
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.SCHWEIZER.getId());
        var konfig = BlattschutzRegistry.fuer(TurnierSystem.SCHWEIZER).orElseThrow();

        TurnierModus.get().setAktivForTest(true);
        try {
            new PlanungsrechnerSheet(wkingSpreadsheet).doRun();

            XSpreadsheet sheet = new PlanungsrechnerSheet(wkingSpreadsheet).getXSpreadSheet();
            assertThat(sheet).isNotNull();
            assertThat(Lo.qi(XProtectable.class, sheet).isProtected())
                    .as("Planungsrechner muss auch bei bereits aktivem Turniermodus sofort geschuetzt werden")
                    .isTrue();
        } finally {
            BlattschutzManager.get().entsperren(konfig, wkingSpreadsheet);
            TurnierModus.get().setAktivForTest(false);
        }
    }

    private boolean istZelleEditierbar(XSpreadsheet sheet, int spalte, int zeile) {
        try {
            var cell = sheet.getCellByPosition(spalte, zeile);
            XPropertySet props = Lo.qi(XPropertySet.class, cell);
            CellProtection cp = (CellProtection) props.getPropertyValue("CellProtection");
            return !cp.IsLocked;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
