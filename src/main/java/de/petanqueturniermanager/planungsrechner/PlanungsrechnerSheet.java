/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.planungsrechner;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.addins.GlobalImpl;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;

/**
 * Eigenständiger, vom Turniersystem unabhängiger Planungsrechner (Sheet „Planungsrechner").
 * <p>
 * Zwei vollständige Blöcke nebeneinander, jeweils mit eigenem Rundenplan: Block A leitet aus
 * Turnier-Start/-Ende das Zeitlimit pro Runde bzw. Durchgang ab; Block B leitet aus einem
 * vorgegebenen Zeitlimit das Turnier-Ende ab. Beide Blöcke berechnen daraus live ihren eigenen
 * kompletten Zeitplan über alle Runden (Block A mit dem selbst errechneten Zeitlimit, Block B mit
 * dem eingegebenen). Siehe {@link PlanungsrechnerRechner} für die reine Rechenlogik.
 * <p>
 * Rührt ausschließlich das eigene Sheet an — keine Turniersystem-Property, keine fremden Sheets,
 * kein fremder Blattschutz. Der Menüpunkt ist deshalb unabhängig vom aktiven Turniersystem immer
 * aufrufbar (siehe {@code ProtocolHandler}).
 */
public class PlanungsrechnerSheet extends SheetRunner implements ISheet {

    private static final int HEADER_COLOR = 0xD9EAF7;
    private static final int INPUT_COLOR = 0xFFF2CC;

    // ── Spalten: zwei Blöcke nebeneinander, Spalte 4 bleibt als Trennlücke frei ─────────────
    private static final int SPALTE_A_LABEL = 0;
    private static final int SPALTE_A_WERT = 1;
    private static final int SPALTE_A_TAB_ENDE = 3; // Rundenplan A: Spalten 0-3

    private static final int SPALTE_B_LABEL = 5;
    private static final int SPALTE_B_WERT = 6;
    private static final int SPALTE_B_TAB_ENDE = 8; // Rundenplan B: Spalten 5-8

    // ── Zeilen: beide Blöcke stehen nebeneinander in denselben Zeilen ───────────────────────
    private static final int ZEILE_TITEL = 0;
    private static final int ZEILE_HEADER = 2;
    private static final int ZEILE_START = 3;

    private static final int ZEILE_A_ENDE = 4;
    private static final int ZEILE_A_TEAMS = 5;
    private static final int ZEILE_A_BAHNEN = 6;
    private static final int ZEILE_A_RUNDEN = 7;
    private static final int ZEILE_A_DURCHGANG_PAUSE = 8;
    private static final int ZEILE_A_RUNDEN_PAUSE = 9;

    private static final int ZEILE_B_TEAMS = 4;
    private static final int ZEILE_B_BAHNEN = 5;
    private static final int ZEILE_B_RUNDEN = 6;
    private static final int ZEILE_B_DURCHGANG_PAUSE = 7;
    private static final int ZEILE_B_RUNDEN_PAUSE = 8;
    private static final int ZEILE_B_ZEITLIMIT = 9;

    private static final int ZEILE_ERGEBNIS_DURCHGAENGE = 11;
    /** Hauptergebnis: Zeitlimit (Block A) bzw. Turnier-Ende (Block B). */
    private static final int ZEILE_ERGEBNIS_HAUPT = 12;

    private static final int ZEILE_TAB_HEADER = 14;
    private static final int ERSTE_TAB_DATEN_ZEILE = 15;

    public PlanungsrechnerSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.KEIN, "PlanungsrechnerSheet");
    }

    @Override
    public XSpreadsheet getXSpreadSheet() throws GenerateException {
        return SheetMetadataHelper.findeSheetUndHeile(getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.SCHLUESSEL_PLANUNGSRECHNER, SheetNamen.LEGACY_PLANUNGSRECHNER);
    }

    @Override
    public TurnierSheet getTurnierSheet() throws GenerateException {
        return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
    }

    @Override
    protected IKonfigurationSheet getKonfigurationSheet() {
        return null;
    }

    @Override
    public void doRun() throws GenerateException {
        processBoxinfo("processbox.erstelle.sheet", SheetNamen.planungsrechner());

        NewSheet neuesSheet = NewSheet.from(this, SheetNamen.planungsrechner(),
                SheetMetadataHelper.SCHLUESSEL_PLANUNGSRECHNER)
                .pos(DefaultSheetPos.PLANUNGSRECHNER).useIfExist().hideGrid()
                .tabColor(0xD9EAF7).setActiv().create();

        XSpreadsheet sheet = getXSpreadSheet();
        if (sheet == null) {
            return;
        }

        // Alle Ausgaben sind PTM.PLANUNG.*-Formeln (siehe schreibeBlockA/B) und rechnen live neu,
        // sobald der Nutzer eine Eingabezelle ändert. Bei erneutem Menüklick auf ein bereits
        // bestehendes Sheet ist daher nichts mehr nachzuberechnen — nur bei Neuerstellung wird
        // das Gerüst inkl. Formeln einmalig geschrieben.
        if (neuesSheet.isDidCreate()) {
            titel(sheet);
            schreibeBlockA(sheet);
            schreibeBlockB(sheet);
            for (int spalte : new int[] { SPALTE_A_LABEL, SPALTE_A_WERT, SPALTE_B_LABEL, SPALTE_B_WERT }) {
                getSheetHelper().setOptimaleBreitePlusMarge(sheet, spalte, SheetHelper.OPTIMALE_BREITE_MARGE);
            }
        }

        if (SheetRunner.isRunning()) {
            getSheetHelper().setActiveSheet(sheet);
            SheetRunner.unterdrückeNaechstesSelectionChange();
        }
    }

    // ── Block A: Turnier-Start/-Ende bekannt -> Zeitlimit + eigener Rundenplan ──────────────

    private void schreibeBlockA(XSpreadsheet sheet) throws GenerateException {
        sectionHeader(sheet, SPALTE_A_LABEL, SPALTE_A_TAB_ENDE, I18n.get("planungsrechner.block.a.header"));
        eingabeZeit(sheet, SPALTE_A_LABEL, SPALTE_A_WERT, ZEILE_START, "planungsrechner.label.turnier.start", "09:00");
        eingabeZeit(sheet, SPALTE_A_LABEL, SPALTE_A_WERT, ZEILE_A_ENDE, "planungsrechner.label.turnier.ende", "17:00");
        eingabeInt(sheet, SPALTE_A_LABEL, SPALTE_A_WERT, ZEILE_A_TEAMS, "planungsrechner.label.anzahl.teams", 16);
        eingabeInt(sheet, SPALTE_A_LABEL, SPALTE_A_WERT, ZEILE_A_BAHNEN, "planungsrechner.label.anzahl.bahnen", 3);
        eingabeInt(sheet, SPALTE_A_LABEL, SPALTE_A_WERT, ZEILE_A_RUNDEN, "planungsrechner.label.anzahl.runden", 5);
        eingabeInt(sheet, SPALTE_A_LABEL, SPALTE_A_WERT, ZEILE_A_DURCHGANG_PAUSE, "planungsrechner.label.durchgang.pause", 5);
        eingabeInt(sheet, SPALTE_A_LABEL, SPALTE_A_WERT, ZEILE_A_RUNDEN_PAUSE, "planungsrechner.label.runden.pause", 10);

        String start = adresse(SPALTE_A_WERT, ZEILE_START);
        String ende = adresse(SPALTE_A_WERT, ZEILE_A_ENDE);
        String teams = adresse(SPALTE_A_WERT, ZEILE_A_TEAMS);
        String bahnen = adresse(SPALTE_A_WERT, ZEILE_A_BAHNEN);
        String runden = adresse(SPALTE_A_WERT, ZEILE_A_RUNDEN);
        String durchgangPause = adresse(SPALTE_A_WERT, ZEILE_A_DURCHGANG_PAUSE);
        String rundenPause = adresse(SPALTE_A_WERT, ZEILE_A_RUNDEN_PAUSE);

        label(sheet, SPALTE_A_LABEL, ZEILE_ERGEBNIS_DURCHGAENGE, I18n.get("planungsrechner.label.durchgaenge.pro.runde"));
        getSheetHelper().setFormulaInCell(sheet, Position.from(SPALTE_A_WERT, ZEILE_ERGEBNIS_DURCHGAENGE),
                GlobalImpl.FORMAT_PTM_PLANUNG_DURCHGAENGE_PRO_RUNDE(teams, bahnen));
        String durchgaenge = adresse(SPALTE_A_WERT, ZEILE_ERGEBNIS_DURCHGAENGE);

        boldFormat(sheet, Position.from(SPALTE_A_LABEL, ZEILE_ERGEBNIS_HAUPT));
        getSheetHelper().setFormulaInCell(sheet, Position.from(SPALTE_A_LABEL, ZEILE_ERGEBNIS_HAUPT),
                "IF(" + durchgaenge + "<=1;\"" + I18n.get("planungsrechner.label.zeitlimit.pro.runde")
                        + "\";\"" + I18n.get("planungsrechner.label.zeitlimit.pro.durchgang") + "\")");
        getSheetHelper().setFormulaInCell(sheet, Position.from(SPALTE_A_WERT, ZEILE_ERGEBNIS_HAUPT),
                GlobalImpl.FORMAT_PTM_PLANUNG_ZEITLIMIT(start, ende, teams, bahnen, runden, durchgangPause, rundenPause));
        String zeitlimit = adresse(SPALTE_A_WERT, ZEILE_ERGEBNIS_HAUPT);

        schreibeTabellenHeader(sheet, SPALTE_A_LABEL);
        RangeHelper.from(sheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                RangePosition.from(SPALTE_A_LABEL, ERSTE_TAB_DATEN_ZEILE, SPALTE_A_TAB_ENDE,
                        ERSTE_TAB_DATEN_ZEILE + PlanungsrechnerRechner.MAX_ZEITPLAN_ZEILEN - 1))
                .setArrayFormula("=" + GlobalImpl.FORMAT_PTM_PLANUNG_ZEITPLAN(start, teams, bahnen, runden,
                        durchgangPause, rundenPause, zeitlimit));
    }

    // ── Block B: Zeitlimit bekannt -> Turnier-Ende + eigener Rundenplan ─────────────────────

    private void schreibeBlockB(XSpreadsheet sheet) throws GenerateException {
        sectionHeader(sheet, SPALTE_B_LABEL, SPALTE_B_TAB_ENDE, I18n.get("planungsrechner.block.b.header"));
        eingabeZeit(sheet, SPALTE_B_LABEL, SPALTE_B_WERT, ZEILE_START, "planungsrechner.label.turnier.start", "09:00");
        eingabeInt(sheet, SPALTE_B_LABEL, SPALTE_B_WERT, ZEILE_B_TEAMS, "planungsrechner.label.anzahl.teams", 16);
        eingabeInt(sheet, SPALTE_B_LABEL, SPALTE_B_WERT, ZEILE_B_BAHNEN, "planungsrechner.label.anzahl.bahnen", 3);
        eingabeInt(sheet, SPALTE_B_LABEL, SPALTE_B_WERT, ZEILE_B_RUNDEN, "planungsrechner.label.anzahl.runden", 5);
        eingabeInt(sheet, SPALTE_B_LABEL, SPALTE_B_WERT, ZEILE_B_DURCHGANG_PAUSE, "planungsrechner.label.durchgang.pause", 5);
        eingabeInt(sheet, SPALTE_B_LABEL, SPALTE_B_WERT, ZEILE_B_RUNDEN_PAUSE, "planungsrechner.label.runden.pause", 10);
        eingabeInt(sheet, SPALTE_B_LABEL, SPALTE_B_WERT, ZEILE_B_ZEITLIMIT, "planungsrechner.label.zeitlimit", 15);

        String start = adresse(SPALTE_B_WERT, ZEILE_START);
        String teams = adresse(SPALTE_B_WERT, ZEILE_B_TEAMS);
        String bahnen = adresse(SPALTE_B_WERT, ZEILE_B_BAHNEN);
        String runden = adresse(SPALTE_B_WERT, ZEILE_B_RUNDEN);
        String durchgangPause = adresse(SPALTE_B_WERT, ZEILE_B_DURCHGANG_PAUSE);
        String rundenPause = adresse(SPALTE_B_WERT, ZEILE_B_RUNDEN_PAUSE);
        String zeitlimit = adresse(SPALTE_B_WERT, ZEILE_B_ZEITLIMIT);

        label(sheet, SPALTE_B_LABEL, ZEILE_ERGEBNIS_DURCHGAENGE, I18n.get("planungsrechner.label.durchgaenge.pro.runde"));
        getSheetHelper().setFormulaInCell(sheet, Position.from(SPALTE_B_WERT, ZEILE_ERGEBNIS_DURCHGAENGE),
                GlobalImpl.FORMAT_PTM_PLANUNG_DURCHGAENGE_PRO_RUNDE(teams, bahnen));

        label(sheet, SPALTE_B_LABEL, ZEILE_ERGEBNIS_HAUPT, I18n.get("planungsrechner.label.turnier.ende.ergebnis"));
        getSheetHelper().setFormulaInCell(sheet, Position.from(SPALTE_B_WERT, ZEILE_ERGEBNIS_HAUPT),
                GlobalImpl.FORMAT_PTM_PLANUNG_TURNIER_ENDE(start, teams, bahnen, runden, durchgangPause, rundenPause,
                        zeitlimit));

        schreibeTabellenHeader(sheet, SPALTE_B_LABEL);
        RangeHelper.from(sheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                RangePosition.from(SPALTE_B_LABEL, ERSTE_TAB_DATEN_ZEILE, SPALTE_B_TAB_ENDE,
                        ERSTE_TAB_DATEN_ZEILE + PlanungsrechnerRechner.MAX_ZEITPLAN_ZEILEN - 1))
                .setArrayFormula("=" + GlobalImpl.FORMAT_PTM_PLANUNG_ZEITPLAN(start, teams, bahnen, runden,
                        durchgangPause, rundenPause, zeitlimit));
    }

    // ── Gemeinsame Bau-Helfer ────────────────────────────────────────────────────────────────

    private void titel(XSpreadsheet sheet) throws GenerateException {
        var titel = StringCellValue.from(sheet, Position.from(SPALTE_A_LABEL, ZEILE_TITEL),
                SheetNamen.planungsrechner())
                .setEndPosMergeSpalte(SPALTE_B_TAB_ENDE)
                .setCellProperties(CellProperties.from().setCellBackColor(HEADER_COLOR)
                        .setCharWeight(FontWeight.BOLD).setCharHeight(16).centerJustify());
        getSheetHelper().setStringValueInCell(titel);
    }

    private void sectionHeader(XSpreadsheet sheet, int labelSpalte, int tabEndeSpalte, String text)
            throws GenerateException {
        var header = StringCellValue.from(sheet, Position.from(labelSpalte, ZEILE_HEADER), text)
                .setEndPosMergeSpalte(tabEndeSpalte)
                .setCellProperties(CellProperties.from().setCellBackColor(HEADER_COLOR)
                        .setCharWeight(FontWeight.BOLD));
        getSheetHelper().setStringValueInCell(header);
    }

    private void label(XSpreadsheet sheet, int spalte, int zeile, String text) throws GenerateException {
        getSheetHelper().setStringValueInCell(StringCellValue.from(sheet, Position.from(spalte, zeile), text)
                .setCellProperties(CellProperties.from().setCharWeight(FontWeight.BOLD)));
    }

    private void eingabeInt(XSpreadsheet sheet, int labelSpalte, int wertSpalte, int zeile, String labelKey,
            int defaultWert) throws GenerateException {
        label(sheet, labelSpalte, zeile, I18n.get(labelKey));
        getSheetHelper().setValInCell(sheet, Position.from(wertSpalte, zeile), defaultWert);
        markiereEingabe(sheet, Position.from(wertSpalte, zeile));
    }

    private void eingabeZeit(XSpreadsheet sheet, int labelSpalte, int wertSpalte, int zeile, String labelKey,
            String defaultWert) throws GenerateException {
        label(sheet, labelSpalte, zeile, I18n.get(labelKey));
        getSheetHelper().setStringValueInCell(StringCellValue.from(sheet, Position.from(wertSpalte, zeile), defaultWert));
        markiereEingabe(sheet, Position.from(wertSpalte, zeile));
    }

    private void markiereEingabe(XSpreadsheet sheet, Position pos) throws GenerateException {
        getSheetHelper().setFormatInCell(StringCellValue.from(sheet, pos)
                .setCellProperties(CellProperties.from().setCellBackColor(INPUT_COLOR)
                        .setHoriJustify(CellHoriJustify.RIGHT)));
    }

    private void schreibeTabellenHeader(XSpreadsheet sheet, int startSpalte) throws GenerateException {
        String[] header = {
                I18n.get("planungsrechner.tabelle.runde"),
                I18n.get("planungsrechner.tabelle.durchgang"),
                I18n.get("planungsrechner.tabelle.start"),
                I18n.get("planungsrechner.tabelle.ende")
        };
        for (int i = 0; i < header.length; i++) {
            getSheetHelper().setStringValueInCell(
                    StringCellValue.from(sheet, Position.from(startSpalte + i, ZEILE_TAB_HEADER), header[i])
                            .setCellProperties(CellProperties.from().setCellBackColor(HEADER_COLOR)
                                    .setCharWeight(FontWeight.BOLD).centerJustify().setAllThinBorder()));
        }
    }

    private String adresse(int spalte, int zeile) {
        return Position.from(spalte, zeile).getAddress();
    }

    private void boldFormat(XSpreadsheet sheet, Position pos) throws GenerateException {
        getSheetHelper().setFormatInCell(StringCellValue.from(sheet, pos)
                .setCellProperties(CellProperties.from().setCharWeight(FontWeight.BOLD)));
    }
}
