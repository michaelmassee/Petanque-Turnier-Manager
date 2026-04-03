/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.rangliste;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.PouleRanglisteRechner;
import de.petanqueturniermanager.algorithmen.PouleTeamErgebnis;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;
import de.petanqueturniermanager.poule.vorrunde.AbstractPouleVorrundeSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellt die Vorrunden-Rangliste für das Poule-A/B-Turniersystem.
 * <p>
 * Liest die Ergebnisse aus dem Poule-Vorrunde-Sheet, berechnet Siege, Niederlagen
 * und Punkte pro Gruppe, sortiert per {@link PouleRanglisteRechner} und schreibt
 * das Ranglistenblatt.
 * <p>
 * Die ersten Plätze jeder Gruppe kommen ins A-Turnier, die übrigen ins B-Turnier.
 */
public class PouleVorrundenRanglisteSheet extends SheetRunner implements ISheet {

    private static final Logger logger = LogManager.getLogger(PouleVorrundenRanglisteSheet.class);

    // Farbe des Ranglistensheets (unterscheidet sich vom Vorrunden-Sheet "4a8faa")
    private static final String SHEET_COLOR = "facd73";

    // Hintergrundfarben für A- und B-Turnier-Zeilen
    private static final int FARBE_A_TURNIER = 0x00CC66;
    private static final int FARBE_B_TURNIER = 0xFF6666;

    // Spalten der Rangliste
    private static final int SPALTE_PLATZ   = 0;
    private static final int SPALTE_GRUPPE  = 1;
    private static final int SPALTE_NR      = 2;
    private static final int SPALTE_NAME    = 3;
    private static final int SPALTE_SIEGE   = 4;
    private static final int SPALTE_NDLG    = 5;
    private static final int SPALTE_PKT_PLUS  = 6;
    private static final int SPALTE_PKT_MINUS = 7;
    private static final int SPALTE_DIFF    = 8;
    private static final int SPALTE_TURNIER = 9;
    private static final int LETZTE_SPALTE  = SPALTE_TURNIER;

    private static final int HEADER_ZEILEN  = 2;

    // Spaltenbreiten
    private static final int BREITE_PLATZ   = 900;
    private static final int BREITE_GRUPPE  = 1500;
    private static final int BREITE_NR      = 900;
    private static final int BREITE_NAME    = 4000;
    private static final int BREITE_ZAHL    = 1200;
    private static final int BREITE_TURNIER = 1500;

    private final PouleKonfigurationSheet konfigurationSheet;

    public PouleVorrundenRanglisteSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.POULE, "Poule-Vorrunden-Rangliste");
        konfigurationSheet = new PouleKonfigurationSheet(workingSpreadsheet);
    }

    @Override
    public XSpreadsheet getXSpreadSheet() throws GenerateException {
        return SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDEN_RANGLISTE,
                SheetNamen.pouleVorrundenRangliste());
    }

    @Override
    public TurnierSheet getTurnierSheet() throws GenerateException {
        return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
    }

    @Override
    protected PouleKonfigurationSheet getKonfigurationSheet() {
        return konfigurationSheet;
    }

    @Override
    public void doRun() throws GenerateException {
        processBoxinfo("processbox.poule.rangliste.erstellen");

        var vorrundeSheet = SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDE,
                SheetNamen.pouleVorrunde());

        if (vorrundeSheet == null) {
            MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("msg.caption.fehler"))
                    .message(I18n.get("poule.ko.fehler.keine.ergebnisse"))
                    .show();
            return;
        }

        NewSheet.from(this, SheetNamen.pouleVorrundenRangliste(),
                        SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDEN_RANGLISTE)
                .tabColor(SHEET_COLOR)
                .pos(DefaultSheetPos.POULE_RANGLISTE)
                .forceCreate()
                .hideGrid()
                .create();

        var xSheet = getXSpreadSheet();

        berechnungUndSchreiben(vorrundeSheet, xSheet);
    }

    /**
     * Berechnet die Rangliste aus dem Vorrunde-Sheet und schreibt sie in das Ziel-Sheet.
     * Wird sowohl vom Vollaufbau ({@link #doRun()}) als auch vom Update-Pfad aufgerufen.
     *
     * @param vorrundeSheet Quell-Sheet mit den Vorrunde-Ergebnissen
     * @param xSheet        Ziel-Sheet für die Rangliste
     */
    protected void berechnungUndSchreiben(XSpreadsheet vorrundeSheet, XSpreadsheet xSheet)
            throws GenerateException {

        var gruppenErgebnisse = leseGruppenErgebnisse(vorrundeSheet);
        var sortiertGruppen = sortiereGruppen(gruppenErgebnisse);

        spaltenBreitenSetzen(xSheet);
        headerSchreiben(xSheet);

        int aktuelleZeile = HEADER_ZEILEN;
        for (int gruppenIndex = 0; gruppenIndex < sortiertGruppen.size(); gruppenIndex++) {
            SheetRunner.testDoCancelTask();
            var gruppe = sortiertGruppen.get(gruppenIndex);
            aktuelleZeile = schreibeGruppenZeilen(xSheet, gruppe, gruppenIndex + 1, aktuelleZeile);
        }

        int letzteDatenZeile = aktuelleZeile - 1;
        if (letzteDatenZeile >= HEADER_ZEILEN) {
            printBereichSetzen(xSheet, letzteDatenZeile);
        }

        if (SheetRunner.isRunning()) {
            SheetFreeze.from(xSheet, getWorkingSpreadsheet()).anzZeilen(HEADER_ZEILEN).doFreeze();
        }
    }

    /**
     * Überladene Version für den Update-Pfad, der das Ziel-Sheet selbst beschafft.
     *
     * @param vorrundeSheet Quell-Sheet mit den Vorrunde-Ergebnissen
     */
    protected void berechnungUndSchreiben(XSpreadsheet vorrundeSheet) throws GenerateException {
        var xSheet = getXSpreadSheet();
        if (xSheet == null) {
            logger.warn("Rangliste-Sheet nicht vorhanden, Update wird übersprungen.");
            return;
        }
        berechnungUndSchreiben(vorrundeSheet, xSheet);
    }

    /**
     * Liest alle Datenzeilen aus dem Vorrunde-Sheet und gruppiert die Ergebnisse
     * nach Poule-Nummer.
     *
     * @return Liste von Gruppen, jeweils als Map teamNr → Ergebnisakkumulator
     */
    private List<Map<Integer, int[]>> leseGruppenErgebnisse(XSpreadsheet vorrundeSheet)
            throws GenerateException {

        var readRange = RangePosition.from(
                AbstractPouleVorrundeSheet.SPALTE_POULE_NR,
                AbstractPouleVorrundeSheet.ERSTE_DATEN_ZEILE,
                AbstractPouleVorrundeSheet.SPALTE_ERG_B,
                AbstractPouleVorrundeSheet.ERSTE_DATEN_ZEILE + 9999);

        RangeData rowsData = RangeHelper
                .from(vorrundeSheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), readRange)
                .getDataFromRange();

        List<Map<Integer, int[]>> gruppen = new ArrayList<>();
        Map<Integer, int[]> aktuelleGruppe = null;

        for (RowData row : rowsData) {
            if (row.size() < 7) {
                break;
            }

            // Spalten-Offsets: SPALTE_POULE_NR=0, TEAM_A_NR=1, TEAM_A_NAME=2, TEAM_B_NR=3, TEAM_B_NAME=4, ERG_A=5, ERG_B=6
            int pouleNrZellWert = row.get(0).getIntVal(0);
            int teamANr = row.get(1).getIntVal(0);
            int teamBNr = row.get(3).getIntVal(0);

            // Leerzeile (Spacer) zwischen Gruppen: beide TeamNr = 0
            if (teamANr == 0 && teamBNr == 0) {
                aktuelleGruppe = null;
                continue;
            }

            // Neue Gruppe beginnt wenn pouleNrZellWert > 0 (merged cell, erste Zeile hat Wert)
            if (pouleNrZellWert > 0 || aktuelleGruppe == null) {
                aktuelleGruppe = new HashMap<>();
                gruppen.add(aktuelleGruppe);
            }

            int ergA = row.get(5).getIntVal(0);
            int ergB = row.get(6).getIntVal(0);

            if (teamANr > 0 && teamBNr == 0) {
                // Freilos für Team A → Sieg ohne Punkte
                aktuelleGruppe.computeIfAbsent(teamANr, k -> new int[4])[0]++;
                continue;
            }

            if (teamANr <= 0) {
                continue;
            }

            // Beiden Teams initialisieren wenn nicht vorhanden
            aktuelleGruppe.computeIfAbsent(teamANr, k -> new int[4]);
            aktuelleGruppe.computeIfAbsent(teamBNr, k -> new int[4]);

            if (ergA > 0 || ergB > 0) {
                // [0]=siege, [1]=niederlagen, [2]=punkte+, [3]=punkte-
                aktuelleGruppe.get(teamANr)[2] += ergA;
                aktuelleGruppe.get(teamANr)[3] += ergB;
                aktuelleGruppe.get(teamBNr)[2] += ergB;
                aktuelleGruppe.get(teamBNr)[3] += ergA;

                if (ergA > ergB) {
                    aktuelleGruppe.get(teamANr)[0]++;
                    aktuelleGruppe.get(teamBNr)[1]++;
                } else if (ergB > ergA) {
                    aktuelleGruppe.get(teamBNr)[0]++;
                    aktuelleGruppe.get(teamANr)[1]++;
                }
            }
        }

        return gruppen;
    }

    /**
     * Baut aus den rohen Gruppenstatistiken {@link PouleTeamErgebnis}-Objekte auf und
     * sortiert jede Gruppe per {@link PouleRanglisteRechner}.
     */
    private List<List<PouleTeamErgebnis>> sortiereGruppen(List<Map<Integer, int[]>> gruppenRoh) {
        var rechner = new PouleRanglisteRechner();
        var sortiertGruppen = new ArrayList<List<PouleTeamErgebnis>>();

        for (var gruppeRoh : gruppenRoh) {
            var ergebnisse = new ArrayList<PouleTeamErgebnis>();
            for (var entry : gruppeRoh.entrySet()) {
                int teamNr = entry.getKey();
                int[] stats = entry.getValue();
                int siege = stats[0];
                int niederlagen = stats[1];
                int pktPlus = stats[2];
                int pktMinus = stats[3];
                int diff = pktPlus - pktMinus;
                ergebnisse.add(new PouleTeamErgebnis(teamNr, siege, niederlagen, diff, pktPlus,
                        List.of()));
            }
            sortiertGruppen.add(rechner.sortiere(ergebnisse));
        }

        return sortiertGruppen;
    }

    /**
     * Schreibt die Datenzeilen für eine Gruppe und gibt die nächste freie Zeile zurück.
     */
    private int schreibeGruppenZeilen(XSpreadsheet xSheet, List<PouleTeamErgebnis> gruppe,
            int gruppenNr, int startZeile) throws GenerateException {

        int aktuelleZeile = startZeile;
        int gruppenGroesse = gruppe.size();

        for (int platzIndex = 0; platzIndex < gruppenGroesse; platzIndex++) {
            var erg = gruppe.get(platzIndex);
            int platz = platzIndex + 1;
            boolean istATurnier = istATurnierPlatz(platz, gruppenGroesse);
            int hintergrundFarbe = istATurnier ? FARBE_A_TURNIER : FARBE_B_TURNIER;
            String turnierBuchstabe = istATurnier ? "A" : "B";

            schreibeDatenZeile(xSheet, aktuelleZeile, platz, gruppenNr, erg,
                    turnierBuchstabe, hintergrundFarbe);
            aktuelleZeile++;
        }

        return aktuelleZeile;
    }

    /**
     * Bestimmt ob ein Platz in einer Gruppe zum A-Turnier gehört.
     * 4er-Poule: Platz 1+2 = A, Platz 3+4 = B.
     * 3er-Poule: Platz 1+2 = A, Platz 3 = B.
     */
    private boolean istATurnierPlatz(int platz, int gruppenGroesse) {
        return platz <= 2;
    }

    /**
     * Schreibt eine einzelne Datenzeile in das Ranglistenblatt.
     */
    private void schreibeDatenZeile(XSpreadsheet xSheet, int zeile, int platz,
            int gruppenNr, PouleTeamErgebnis erg, String turnier,
            int hintergrundFarbe) throws GenerateException {

        var border = BorderFactory.from().allThin().toBorder();

        getSheetHelper().setNumberValueInCell(
                NumberCellValue.from(xSheet, Position.from(SPALTE_PLATZ, zeile))
                        .setValue(platz)
                        .setBorder(border)
                        .setHoriJustify(CellHoriJustify.CENTER)
                        .setCellBackColor(hintergrundFarbe));

        getSheetHelper().setNumberValueInCell(
                NumberCellValue.from(xSheet, Position.from(SPALTE_GRUPPE, zeile))
                        .setValue(gruppenNr)
                        .setBorder(border)
                        .setHoriJustify(CellHoriJustify.CENTER)
                        .setCellBackColor(hintergrundFarbe));

        getSheetHelper().setNumberValueInCell(
                NumberCellValue.from(xSheet, Position.from(SPALTE_NR, zeile))
                        .setValue(erg.teamNr())
                        .setBorder(border)
                        .setHoriJustify(CellHoriJustify.CENTER)
                        .setCellBackColor(hintergrundFarbe));

        // Name per VLOOKUP aus Meldeliste
        String vlookup = "VLOOKUP(" + Position.from(SPALTE_NR, zeile).getAddress()
                + ";$'" + SheetNamen.meldeliste() + "'.$A$4:$B$999;2;0)";
        getSheetHelper().setFormulaInCell(
                StringCellValue.from(xSheet, Position.from(SPALTE_NAME, zeile), vlookup)
                        .setBorder(border)
                        .setCellBackColor(hintergrundFarbe));

        getSheetHelper().setNumberValueInCell(
                NumberCellValue.from(xSheet, Position.from(SPALTE_SIEGE, zeile))
                        .setValue(erg.siege())
                        .setBorder(border)
                        .setHoriJustify(CellHoriJustify.CENTER)
                        .setCellBackColor(hintergrundFarbe));

        getSheetHelper().setNumberValueInCell(
                NumberCellValue.from(xSheet, Position.from(SPALTE_NDLG, zeile))
                        .setValue(erg.niederlagen())
                        .setBorder(border)
                        .setHoriJustify(CellHoriJustify.CENTER)
                        .setCellBackColor(hintergrundFarbe));

        getSheetHelper().setNumberValueInCell(
                NumberCellValue.from(xSheet, Position.from(SPALTE_PKT_PLUS, zeile))
                        .setValue(erg.erzieltePunkte())
                        .setBorder(border)
                        .setHoriJustify(CellHoriJustify.CENTER)
                        .setCellBackColor(hintergrundFarbe));

        int pktMinus = erg.erzieltePunkte() - erg.punkteDifferenz();
        getSheetHelper().setNumberValueInCell(
                NumberCellValue.from(xSheet, Position.from(SPALTE_PKT_MINUS, zeile))
                        .setValue(pktMinus)
                        .setBorder(border)
                        .setHoriJustify(CellHoriJustify.CENTER)
                        .setCellBackColor(hintergrundFarbe));

        getSheetHelper().setNumberValueInCell(
                NumberCellValue.from(xSheet, Position.from(SPALTE_DIFF, zeile))
                        .setValue(erg.punkteDifferenz())
                        .setBorder(border)
                        .setHoriJustify(CellHoriJustify.CENTER)
                        .setCellBackColor(hintergrundFarbe));

        getSheetHelper().setStringValueInCell(
                StringCellValue.from(xSheet, Position.from(SPALTE_TURNIER, zeile), turnier)
                        .setBorder(border)
                        .setHoriJustify(CellHoriJustify.CENTER)
                        .setCharWeight(FontWeight.BOLD)
                        .setCellBackColor(hintergrundFarbe));
    }

    private void headerSchreiben(XSpreadsheet xSheet) throws GenerateException {
        int headerFarbe = konfigurationSheet.getMeldeListeHeaderFarbe();
        TableBorder2 border = BorderFactory.from().allThin().boldLn().forBottom().toBorder();

        getSheetHelper().setStringValueInCell(
                StringCellValue.from(xSheet, Position.from(SPALTE_PLATZ, 0), I18n.get("column.header.platz"))
                        .setCellBackColor(headerFarbe)
                        .setBorder(border)
                        .setCharWeight(FontWeight.BOLD)
                        .setHoriJustify(CellHoriJustify.CENTER)
                        .setVertJustify(CellVertJustify2.CENTER)
                        .setShrinkToFit(true).setRotate90().setEndPosMergeZeilePlus(1));

        // schreibeHeaderZelle(xSheet, SPALTE_PLATZ, I18n.get("column.header.platz"), headerFarbe, border);
        schreibeHeaderZelle(xSheet, SPALTE_GRUPPE, I18n.get("poule.rangliste.header.gruppe"), headerFarbe, border);
        schreibeHeaderZelle(xSheet, SPALTE_NR, I18n.get("column.header.nr"), headerFarbe, border);
        schreibeHeaderZelle(xSheet, SPALTE_NAME, I18n.get("column.header.name"), headerFarbe, border);



        // Spiele
        getSheetHelper().setStringValueInCell(
                StringCellValue.from(xSheet, Position.from(SPALTE_SIEGE, 0), I18n.get("column.header.spiele"))
                        .setCellBackColor(headerFarbe)
                        .setBorder(BorderFactory.from().allThin().doubleLn().forLeft().toBorder())
                        .setCharWeight(FontWeight.BOLD)
                        .setHoriJustify(CellHoriJustify.CENTER)
                        .setVertJustify(CellVertJustify2.CENTER)
                        .setShrinkToFit(true).setEndPosMergeSpaltePlus(1));

        getSheetHelper().setStringValueInCell(
                StringCellValue.from(xSheet, Position.from(SPALTE_SIEGE, 1),  I18n.get("schweizer.rangliste.spalte.punkte.plus"))
                        .setCellBackColor(headerFarbe)
                        .setBorder(BorderFactory.from().allThin().doubleLn().forLeft().boldLn().forBottom().toBorder())
                        .setCharWeight(FontWeight.BOLD)
                        .setHoriJustify(CellHoriJustify.CENTER)
                        .setVertJustify(CellVertJustify2.CENTER)
                        .setShrinkToFit(true));

        getSheetHelper().setStringValueInCell(
                StringCellValue.from(xSheet, Position.from(SPALTE_NDLG, 1),  I18n.get("schweizer.rangliste.spalte.punkte.minus"))
                        .setCellBackColor(headerFarbe)
                        .setBorder(border)
                        .setCharWeight(FontWeight.BOLD)
                        .setHoriJustify(CellHoriJustify.CENTER)
                        .setVertJustify(CellVertJustify2.CENTER)
                        .setShrinkToFit(true));

        // Punke
        getSheetHelper().setStringValueInCell(
                StringCellValue.from(xSheet, Position.from(SPALTE_PKT_PLUS, 0), I18n.get("column.header.punkte"))
                        .setCellBackColor(headerFarbe)
                        .setBorder(BorderFactory.from().allThin().boldLn().forLeft().boldLn().forRight().toBorder())
                        .setCharWeight(FontWeight.BOLD)
                        .setHoriJustify(CellHoriJustify.CENTER)
                        .setVertJustify(CellVertJustify2.CENTER)
                        .setShrinkToFit(true).setEndPosMergeSpaltePlus(2));

        getSheetHelper().setStringValueInCell(
                StringCellValue.from(xSheet, Position.from(SPALTE_PKT_PLUS, 1), I18n.get("schweizer.rangliste.spalte.punkte.plus"))
                        .setCellBackColor(headerFarbe)
                        .setBorder(BorderFactory.from().allThin().boldLn().forLeft().forBottom().toBorder())
                        .setCharWeight(FontWeight.BOLD)
                        .setHoriJustify(CellHoriJustify.CENTER)
                        .setVertJustify(CellVertJustify2.CENTER)
                        .setShrinkToFit(true));

        getSheetHelper().setStringValueInCell(
                StringCellValue.from(xSheet, Position.from(SPALTE_PKT_MINUS, 1), I18n.get("schweizer.rangliste.spalte.punkte.minus"))
                        .setCellBackColor(headerFarbe)
                        .setBorder(border)
                        .setCharWeight(FontWeight.BOLD)
                        .setHoriJustify(CellHoriJustify.CENTER)
                        .setVertJustify(CellVertJustify2.CENTER)
                        .setShrinkToFit(true));

        getSheetHelper().setStringValueInCell(
                StringCellValue.from(xSheet, Position.from(SPALTE_DIFF, 1), I18n.get("schweizer.rangliste.spalte.punkte.differenz"))
                        .setCellBackColor(headerFarbe)
                        .setBorder(BorderFactory.from().allThin().boldLn().forRight().forBottom().toBorder())
                        .setCharWeight(FontWeight.BOLD)
                        .setHoriJustify(CellHoriJustify.CENTER)
                        .setVertJustify(CellVertJustify2.CENTER)
                        .setShrinkToFit(true));



        schreibeHeaderZelle(xSheet, SPALTE_TURNIER, I18n.get("poule.rangliste.header.turnier"), headerFarbe, border);
    }

    private void schreibeHeaderZelle(XSpreadsheet xSheet, int spalte, String text,
            int farbe, TableBorder2 border) throws GenerateException {
        getSheetHelper().setStringValueInCell(
                StringCellValue.from(xSheet, Position.from(spalte, 0), text)
                        .setCellBackColor(farbe)
                        .setBorder(border)
                        .setCharWeight(FontWeight.BOLD)
                        .setHoriJustify(CellHoriJustify.CENTER)
                        .setVertJustify(CellVertJustify2.CENTER)
                        .setShrinkToFit(true).setEndPosMergeZeilePlus(1));
    }

    private void spaltenBreitenSetzen(XSpreadsheet xSheet) throws GenerateException {
        getSheetHelper().setColumnProperties(xSheet, SPALTE_PLATZ,
                ColumnProperties.from().setWidth(BREITE_PLATZ).setHoriJustify(CellHoriJustify.CENTER));
        getSheetHelper().setColumnProperties(xSheet, SPALTE_GRUPPE,
                ColumnProperties.from().setWidth(BREITE_GRUPPE).setHoriJustify(CellHoriJustify.CENTER));
        getSheetHelper().setColumnProperties(xSheet, SPALTE_NR,
                ColumnProperties.from().setWidth(BREITE_NR).setHoriJustify(CellHoriJustify.CENTER));
        getSheetHelper().setColumnProperties(xSheet, SPALTE_NAME,
                ColumnProperties.from().setWidth(BREITE_NAME));
        getSheetHelper().setColumnProperties(xSheet, SPALTE_SIEGE,
                ColumnProperties.from().setWidth(BREITE_ZAHL).setHoriJustify(CellHoriJustify.CENTER));
        getSheetHelper().setColumnProperties(xSheet, SPALTE_NDLG,
                ColumnProperties.from().setWidth(BREITE_ZAHL).setHoriJustify(CellHoriJustify.CENTER));
        getSheetHelper().setColumnProperties(xSheet, SPALTE_PKT_PLUS,
                ColumnProperties.from().setWidth(BREITE_ZAHL).setHoriJustify(CellHoriJustify.CENTER));
        getSheetHelper().setColumnProperties(xSheet, SPALTE_PKT_MINUS,
                ColumnProperties.from().setWidth(BREITE_ZAHL).setHoriJustify(CellHoriJustify.CENTER));
        getSheetHelper().setColumnProperties(xSheet, SPALTE_DIFF,
                ColumnProperties.from().setWidth(BREITE_ZAHL).setHoriJustify(CellHoriJustify.CENTER));
        getSheetHelper().setColumnProperties(xSheet, SPALTE_TURNIER,
                ColumnProperties.from().setWidth(BREITE_TURNIER).setHoriJustify(CellHoriJustify.CENTER));
    }

    private void printBereichSetzen(XSpreadsheet xSheet, int letzteDatenZeile) throws GenerateException {
        PrintArea.from(xSheet, getWorkingSpreadsheet())
                .setPrintArea(RangePosition.from(
                        Position.from(SPALTE_PLATZ, 0),
                        Position.from(LETZTE_SPALTE, letzteDatenZeile)));
    }
}
