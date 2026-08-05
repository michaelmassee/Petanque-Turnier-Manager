package de.petanqueturniermanager.planungsrechner;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;

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

    @Test
    public void testWiederholterMenueaufruf_erzeugtSheetNichtErneut() throws Exception {
        new PlanungsrechnerSheet(wkingSpreadsheet).doRun();
        XSpreadsheet ersterAufruf = new PlanungsrechnerSheet(wkingSpreadsheet).getXSpreadSheet();

        // Eingabe manuell aendern, wie es der Nutzer vor einem erneuten Menueklick taete
        sheetHlp.setValInCell(ersterAufruf, Position.from(SPALTE_A_WERT, ZEILE_A_TEAMS), 24);

        new PlanungsrechnerSheet(wkingSpreadsheet).doRun();
        XSpreadsheet zweiterAufruf = new PlanungsrechnerSheet(wkingSpreadsheet).getXSpreadSheet();

        // dasselbe Sheet, keine Neuerstellung, manuelle Eingabe bleibt unangetastet
        assertThat(sheetHlp.getIntFromCell(zweiterAufruf, Position.from(SPALTE_A_WERT, ZEILE_A_TEAMS))).isEqualTo(24);
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
}
