/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.planungsrechner;

import java.time.LocalTime;
import java.util.List;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.addins.GlobalImpl;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
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
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.numberformat.UserNumberFormat;

/**
 * Eigenständiger, vom Turniersystem unabhängiger Planungsrechner (Sheet „Planungsrechner").
 * <p>
 * Zwei vollständige Blöcke nebeneinander, jeweils mit eigenem Rundenplan: Block A leitet aus
 * Turnier-Start/-Ende das Zeitlimit pro Runde bzw. Durchgang ab; Block B leitet aus einem
 * vorgegebenen Zeitlimit das Turnier-Ende ab. Beide Blöcke berechnen daraus live ihren eigenen
 * kompletten Zeitplan über alle Runden (Block A mit dem selbst errechneten Zeitlimit, Block B mit
 * dem eingegebenen). Siehe {@link PlanungsrechnerRechner} für die reine Rechenlogik.
 * <p>
 * Rührt ausschließlich das eigene Sheet an — keine Turniersystem-Property, keine fremden Sheets.
 * Der Menüpunkt ist deshalb unabhängig vom aktiven Turniersystem immer aufrufbar (siehe
 * {@code ProtocolHandler}). Im Turnier-Modus (Kiosk-Modus) wird das Sheet trotzdem geschützt —
 * analog zum Teilnehmer-Sheet ist es unabhängig vom aktiven {@code TurnierSystem} in
 * {@link de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager} als „globales"
 * Sheet verankert (nicht in {@code BlattschutzRegistry}, die dokumentweit an genau ein aktives
 * Turniersystem gebunden ist), mit den Eingabezellen aus {@link #editierbareEingabeBereiche()}
 * als einzige weiterhin editierbare Bereiche.
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

    /**
     * Bereiche der Eingabezellen (gelb markiert, siehe {@link #markiereEingabe}), die im
     * Turnier-Modus trotz Sheet-Schutz editierbar bleiben müssen — je einer pro Block, da beide
     * Eingabespalten unterbrechungsfrei die Zeilen {@code ZEILE_START}..letzte Eingabezeile
     * belegen. Wird von {@code BlattschutzManager.mitGlobalenSchutzInfos()} verwendet, damit die
     * Layout-Konstanten (einzige Quelle der Wahrheit) nicht dupliziert werden müssen.
     */
    public static List<RangePosition> editierbareEingabeBereiche() {
        return List.of(
                RangePosition.from(SPALTE_A_WERT, ZEILE_START, SPALTE_A_WERT, ZEILE_A_RUNDEN_PAUSE),
                RangePosition.from(SPALTE_B_WERT, ZEILE_START, SPALTE_B_WERT, ZEILE_B_ZEITLIMIT));
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
    @SuppressWarnings("try") // scopeFuer()-Ergebnis wird bewusst nur für seinen close()-Seiteneffekt gehalten
    public void doRun() throws GenerateException {
        processBoxinfo("processbox.erstelle.sheet", SheetNamen.planungsrechner());

        // Dieser Runner deklariert TurnierSystem.KEIN, daher öffnet SheetRunner.run() für ihn nie
        // den Blattschutz-Command-Scope (dessen Guard prüft turnierSystem != TurnierSystem.KEIN).
        // NewSheet.create() kann unten bei "Neu erstellen?" -> Ja das Sheet per removeSheet()
        // entfernen, was ensureUnprotectedInScope() voraussetzt — ohne eigenen Scope würfe das im
        // Turnier-Modus eine IllegalStateException. scopeFuer() ist genau für Aufrufer außerhalb
        // des normalen SheetRunner-Scopes gedacht und bei inaktivem Turnier-Modus bzw.
        // TurnierSystem.KEIN ein No-Op; beim Schließen wird u.a. dieses Sheet automatisch wieder
        // geschützt (siehe BlattschutzManager.mitGlobalenSchutzInfos).
        TurnierSystem aktivesSystem = new DocumentPropertiesHelper(getWorkingSpreadsheet()).getTurnierSystemAusDocument();
        try (var ignored = BlattschutzManager.get().scopeFuer(aktivesSystem, getWorkingSpreadsheet())) {
            // Ohne .useIfExist(): existiert das Sheet bereits, fragt NewSheet.create() den Nutzer
            // per Ja/Nein-Dialog, ob es neu erstellt werden soll — bei „Nein" bleibt es unverändert
            // stehen und wird nur aktiviert.
            NewSheet neuesSheet = NewSheet.from(this, SheetNamen.planungsrechner(),
                    SheetMetadataHelper.SCHLUESSEL_PLANUNGSRECHNER)
                    .pos(DefaultSheetPos.PLANUNGSRECHNER).hideGrid()
                    .tabColor(0xD9EAF7).setActiv().create();

            XSpreadsheet sheet = getXSpreadSheet();
            if (sheet == null) {
                return;
            }

            // Alle Ausgaben sind PTM.PLANUNG.*-Formeln (siehe schreibeBlockA/B) und rechnen live
            // neu, sobald der Nutzer eine Eingabezelle ändert — nur bei Neuerstellung wird das
            // Gerüst inkl. Formeln (neu) geschrieben.
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
    }

    // ── Block A: Turnier-Start/-Ende bekannt -> Zeitlimit + eigener Rundenplan ──────────────

    private void schreibeBlockA(XSpreadsheet sheet) throws GenerateException {
        sectionHeader(sheet, SPALTE_A_LABEL, SPALTE_A_TAB_ENDE, I18n.get("planungsrechner.block.a.header"));
        eingabeZeit(sheet, SPALTE_A_LABEL, SPALTE_A_WERT, ZEILE_START, "planungsrechner.label.turnier.start",
                LocalTime.of(9, 0));
        eingabeZeit(sheet, SPALTE_A_LABEL, SPALTE_A_WERT, ZEILE_A_ENDE, "planungsrechner.label.turnier.ende",
                LocalTime.of(17, 0));
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
        eingabeZeit(sheet, SPALTE_B_LABEL, SPALTE_B_WERT, ZEILE_START, "planungsrechner.label.turnier.start",
                LocalTime.of(9, 0));
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
        markiereEingabe(sheet, Position.from(wertSpalte, zeile), null);
    }

    /**
     * Schreibt einen echten Calc-Zeitwert (Bruchteil des 24h-Tages) statt eines Text-Strings, damit
     * die Zelle als Zeit-formatierbar gilt und {@code UserNumberFormat.TIME} (HH:MM) greift. Wird
     * von {@code PTM.PLANUNG.*} (siehe {@link GlobalImpl#ptmplanungzeitlimit}) ebenfalls als
     * Bruchteil des Tages gelesen.
     */
    private void eingabeZeit(XSpreadsheet sheet, int labelSpalte, int wertSpalte, int zeile, String labelKey,
            LocalTime defaultWert) throws GenerateException {
        label(sheet, labelSpalte, zeile, I18n.get(labelKey));
        getSheetHelper().setValInCell(sheet, Position.from(wertSpalte, zeile), defaultWert.toSecondOfDay() / 86400.0);
        markiereEingabe(sheet, Position.from(wertSpalte, zeile), UserNumberFormat.TIME);
    }

    private void markiereEingabe(XSpreadsheet sheet, Position pos, UserNumberFormat zahlenFormat)
            throws GenerateException {
        CellProperties eigenschaften = CellProperties.from().setCellBackColor(INPUT_COLOR)
                .setHoriJustify(CellHoriJustify.RIGHT);
        if (zahlenFormat != null) {
            eigenschaften.numberFormat(zahlenFormat);
        }
        getSheetHelper().setFormatInCell(StringCellValue.from(sheet, pos).setCellProperties(eigenschaften));
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
