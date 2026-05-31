package de.petanqueturniermanager.triptete.meldeliste;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.star.sheet.ConditionOperator;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeHelper;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.RangeProperties;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SortHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.triptete.konfiguration.TripTeteKonfigurationSheet;

/**
 * Delegate für die Trip-Tête-Meldeliste.
 * <p>
 * Spaltenstruktur: Nr | [Teamname] | Spieler-Blöcke (Vorname + Nachname [+ Vereinsname]) | Aktiv<br>
 * Formation fest TRIPLETTE (3 Spieler), nicht konfigurierbar. Keine SP-Spalte.
 * 3 Header-Zeilen (ERSTE_DATEN_ZEILE = 3).
 */
class TripTeteMeldeListeDelegate implements MeldeListeKonstanten {

    static final int MIN_ANZAHL_MELDUNGEN_ZEILEN = 16;
    static final int DRITTE_HEADER_ZEILE = 2;
    static final int ERSTE_DATEN_ZEILE_OVERRIDE = 3;

    private static final int NR_SPALTE_WIDTH = 800;
    private static final int NAME_SPALTE_WIDTH = 3000;
    private static final int TEAMNAME_SPALTE_WIDTH = 3000;
    private static final int VEREINSNAME_SPALTE_WIDTH = 2500;
    private static final int AKTIV_SPALTE_WIDTH = 700;

    private final ISheet sheet;
    private final TripTeteKonfigurationSheet konfigurationSheet;

    TripTeteMeldeListeDelegate(ISheet sheet, WorkingSpreadsheet ws) {
        this.sheet = sheet;
        konfigurationSheet = new TripTeteKonfigurationSheet(ws);
    }

    TripTeteKonfigurationSheet getKonfigurationSheet() {
        return konfigurationSheet;
    }

    // ---------------------------------------------------------------
    // Spalten-Berechnung (abhängig von Konfiguration)
    // ---------------------------------------------------------------

    int getTeamNrSpalte() {
        return SPIELER_NR_SPALTE; // = 0
    }

    int getTeamnameSpalte() {
        return konfigurationSheet.isMeldeListeTeamnameAnzeigen() ? 1 : -1;
    }

    int getSpaltenProSpieler() {
        return konfigurationSheet.isMeldeListeVereinsnameAnzeigen() ? 3 : 2;
    }

    int getErsterSpielerOffset() {
        return konfigurationSheet.isMeldeListeTeamnameAnzeigen() ? 2 : 1;
    }

    int getVornameSpalte(int spielerIdx) {
        return getErsterSpielerOffset() + spielerIdx * getSpaltenProSpieler();
    }

    int getNachnameSpalte(int spielerIdx) {
        return getVornameSpalte(spielerIdx) + 1;
    }

    int getVereinsnameSpalte(int spielerIdx) {
        if (!konfigurationSheet.isMeldeListeVereinsnameAnzeigen()) {
            return -1;
        }
        return getVornameSpalte(spielerIdx) + 2;
    }

    int getAktivSpalte() {
        return getErsterSpielerOffset() + Formation.TRIPLETTE.getAnzSpieler() * getSpaltenProSpieler();
    }

    // ---------------------------------------------------------------
    // Sheet-Aufbau
    // ---------------------------------------------------------------

    void upDateSheet() throws GenerateException {
        sheet.processBoxinfo("processbox.meldeliste.sortieren");
        TurnierSheet.from(sheet.getXSpreadSheet(), sheet.getWorkingSpreadsheet()).setActiv();

        sortiereUndNummeriere();
        insertHeaderInSheet(konfigurationSheet.getMeldeListeHeaderFarbe());
        formatDatenSpalten();

        SheetFreeze.from(sheet.getXSpreadSheet(), sheet.getWorkingSpreadsheet()).anzZeilen(3).doFreeze();
    }

    private void sortiereUndNummeriere() throws GenerateException {
        XSpreadsheet xSheet = sheet.getXSpreadSheet();
        SheetHelper sh = sheet.getSheetHelper();
        int letzte = letzteZeileMitDaten(xSheet) + MIN_ANZAHL_MELDUNGEN_ZEILEN;

        int naechsteNr = berechneNaechsteFreieNr(sh, xSheet, letzte);

        for (int z = ERSTE_DATEN_ZEILE_OVERRIDE; z <= letzte; z++) {
            String vorname = sh.getTextFromCell(xSheet, Position.from(getVornameSpalte(0), z));
            if (vorname == null || vorname.isBlank()) {
                sh.clearValInCell(xSheet, Position.from(getTeamNrSpalte(), z));
            } else {
                int vorhandeneNr = sh.getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), z));
                if (vorhandeneNr <= 0) {
                    sh.setNumberValueInCell(NumberCellValue.from(xSheet, Position.from(getTeamNrSpalte(), z))
                            .setValue(naechsteNr++));
                }
            }
        }

        int letzteNachNummern = letzteZeileMitDaten(xSheet);
        if (letzteNachNummern >= ERSTE_DATEN_ZEILE_OVERRIDE) {
            RangePosition sortRange = RangePosition.from(getTeamNrSpalte(), ERSTE_DATEN_ZEILE_OVERRIDE,
                    getAktivSpalte(), letzteNachNummern);
            SortHelper.from(sheet, sortRange).spalteToSort(getTeamNrSpalte()).aufSteigendSortieren(true).doSort();
        }
    }

    private int berechneNaechsteFreieNr(SheetHelper sh, XSpreadsheet xSheet, int letzte) throws GenerateException {
        int max = 0;
        for (int z = ERSTE_DATEN_ZEILE_OVERRIDE; z <= letzte; z++) {
            int nr = sh.getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), z));
            if (nr > max) {
                max = nr;
            }
        }
        return max + 1;
    }

    private void insertHeaderInSheet(int headerColor) throws GenerateException {
        int anzSpieler = Formation.TRIPLETTE.getAnzSpieler();
        int spaltenProSpieler = getSpaltenProSpieler();
        boolean teamnameAktiv = konfigurationSheet.isMeldeListeTeamnameAnzeigen();
        boolean vereinsnameAktiv = konfigurationSheet.isMeldeListeVereinsnameAnzeigen();

        ColumnProperties colPropNr = ColumnProperties.from().setWidth(NR_SPALTE_WIDTH)
                .setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER)
                .margin(MeldeListeKonstanten.CELL_MARGIN);
        ColumnProperties colPropName = ColumnProperties.from().setWidth(NAME_SPALTE_WIDTH)
                .setHoriJustify(CellHoriJustify.LEFT).setVertJustify(CellVertJustify2.CENTER)
                .margin(MeldeListeKonstanten.CELL_MARGIN);

        sheet.getSheetHelper().setStringValueInCell(
                StringCellValue.from(sheet.getXSpreadSheet(), Position.from(getTeamNrSpalte(), ZWEITE_HEADER_ZEILE),
                        I18n.get("column.header.nr"))
                        .addColumnProperties(colPropNr)
                        .setCellBackColor(headerColor)
                        .setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().doubleLn().forRight()
                                .toBorder())
                        .setVertJustify(CellVertJustify2.CENTER)
                        .setCharWeight(com.sun.star.awt.FontWeight.BOLD)
                        .setComment(I18n.get("ko.meldeliste.comment.startnummer"))
                        .setEndPosMergeZeilePlus(1)
                        .setShrinkToFit(true));

        if (teamnameAktiv) {
            sheet.getSheetHelper().setStringValueInCell(
                    StringCellValue.from(sheet.getXSpreadSheet(), Position.from(1, ZWEITE_HEADER_ZEILE),
                            I18n.get("column.header.teamname"))
                            .addColumnProperties(colPropName.setWidth(TEAMNAME_SPALTE_WIDTH))
                            .setCellBackColor(headerColor)
                            .setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
                            .setVertJustify(CellVertJustify2.CENTER)
                            .setCharWeight(com.sun.star.awt.FontWeight.BOLD)
                            .setEndPosMergeZeilePlus(1)
                            .setShrinkToFit(true));
        }

        for (int s = 0; s < anzSpieler; s++) {
            int vornameSpalte = getVornameSpalte(s);
            String spielerTitel = I18n.get("column.header.spieler") + " " + (s + 1);

            sheet.getSheetHelper().setStringValueInCell(
                    StringCellValue.from(sheet.getXSpreadSheet(), Position.from(vornameSpalte, ZWEITE_HEADER_ZEILE),
                            spielerTitel)
                            .addColumnProperties(colPropName)
                            .setCellBackColor(headerColor)
                            .setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
                            .setVertJustify(CellVertJustify2.CENTER)
                            .setHoriJustify(CellHoriJustify.CENTER)
                            .setCharWeight(com.sun.star.awt.FontWeight.BOLD)
                            .setEndPosMergeSpalte(vornameSpalte + spaltenProSpieler - 1)
                            .setShrinkToFit(true));

            sheet.getSheetHelper().setStringValueInCell(
                    StringCellValue.from(sheet.getXSpreadSheet(), Position.from(vornameSpalte, DRITTE_HEADER_ZEILE),
                            I18n.get("column.header.vorname"))
                            .addColumnProperties(colPropName)
                            .setCellBackColor(headerColor)
                            .setBorder(BorderFactory.from().allThin().boldLn().forLeft().toBorder())
                            .setVertJustify(CellVertJustify2.CENTER)
                            .setShrinkToFit(true));

            sheet.getSheetHelper().setStringValueInCell(
                    StringCellValue.from(sheet.getXSpreadSheet(), Position.from(getNachnameSpalte(s), DRITTE_HEADER_ZEILE),
                            I18n.get("column.header.nachname"))
                            .addColumnProperties(colPropName)
                            .setCellBackColor(headerColor)
                            .setBorder(BorderFactory.from().allThin().toBorder())
                            .setVertJustify(CellVertJustify2.CENTER)
                            .setShrinkToFit(true));

            if (vereinsnameAktiv) {
                sheet.getSheetHelper().setStringValueInCell(
                        StringCellValue.from(sheet.getXSpreadSheet(),
                                Position.from(getVereinsnameSpalte(s), DRITTE_HEADER_ZEILE),
                                I18n.get("column.header.vereinsname"))
                                .addColumnProperties(colPropName.setWidth(VEREINSNAME_SPALTE_WIDTH))
                                .setCellBackColor(headerColor)
                                .setBorder(BorderFactory.from().allThin().toBorder())
                                .setVertJustify(CellVertJustify2.CENTER)
                                .setShrinkToFit(true));
            }
        }

        ColumnProperties colPropAktiv = ColumnProperties.from().setWidth(AKTIV_SPALTE_WIDTH)
                .setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER)
                .margin(MeldeListeKonstanten.CELL_MARGIN);
        sheet.getSheetHelper().setStringValueInCell(
                StringCellValue.from(sheet.getXSpreadSheet(), Position.from(getAktivSpalte(), ZWEITE_HEADER_ZEILE),
                        I18n.get("column.header.aktiv"))
                        .addColumnProperties(colPropAktiv)
                        .setCellBackColor(headerColor)
                        .setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
                        .setVertJustify(CellVertJustify2.CENTER)
                        .setCharWeight(com.sun.star.awt.FontWeight.BOLD)
                        .setComment(I18n.get("schweizer.meldeliste.comment.aktiv"))
                        .setRotate90()
                        .setEndPosMergeZeilePlus(1)
                        .setShrinkToFit(true));

        sheet.getSheetHelper().setStringValueInCell(
                StringCellValue.from(sheet.getXSpreadSheet(), Position.from(0, ERSTE_HEADER_ZEILE),
                        I18n.get("meldeliste.header.turniersystem",
                                de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem.TRIPTETE.getBezeichnung()))
                        .setEndPosMergeSpaltePlus(2)
                        .setCharWeight(com.sun.star.awt.FontWeight.BOLD)
                        .setHoriJustify(CellHoriJustify.LEFT)
                        .setVertJustify(CellVertJustify2.TOP)
                        .setShrinkToFit(true)
                        .setCharColor("00599d"));
    }

    private void formatDatenSpalten() throws GenerateException {
        int letzteDatenZeile = getLetzteDatenZeileUseMin();

        MeldungenHintergrundFarbeGeradeStyle farbeGerade = konfigurationSheet.getMeldeListeHintergrundFarbeGeradeStyle();
        MeldungenHintergrundFarbeUnGeradeStyle farbeUngerade = konfigurationSheet.getMeldeListeHintergrundFarbeUnGeradeStyle();

        RangePosition nrRange = RangePosition.from(getTeamNrSpalte(), ERSTE_DATEN_ZEILE_OVERRIDE,
                getTeamNrSpalte(), letzteDatenZeile);
        RangeHelper.from(sheet, nrRange).setRangeProperties(
                RangeProperties.from().setBorder(
                        BorderFactory.from().allThin().boldLn().forTop().forLeft().doubleLn().forRight().toBorder()));

        String kondDoppeltNr = "COUNTIF(" + Position.from(getTeamNrSpalte(), 0).getSpalteAddressWith$() + ";"
                + ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")>1";
        ConditionalFormatHelper.from(sheet, nrRange).clear()
                .formulaIsText().styleIsFehler().applyAndDoReset()
                .formula1(kondDoppeltNr).operator(ConditionOperator.FORMULA).styleIsFehler().applyAndDoReset()
                .formula1("0").formula2("" + MeldungenSpalte.MAX_ANZ_MELDUNGEN)
                .operator(ConditionOperator.NOT_BETWEEN).styleIsFehler().applyAndDoReset()
                .formulaIsEvenRow().style(farbeGerade).applyAndDoReset()
                .formulaIsOddRow().style(farbeUngerade).applyAndDoReset();

        if (konfigurationSheet.isMeldeListeTeamnameAnzeigen()) {
            RangePosition teamnameRange = RangePosition.from(1, ERSTE_DATEN_ZEILE_OVERRIDE, 1, letzteDatenZeile);
            RangeHelper.from(sheet, teamnameRange).setRangeProperties(
                    RangeProperties.from().setBorder(
                            BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder()));
        }

        int anzSpieler = Formation.TRIPLETTE.getAnzSpieler();
        for (int s = 0; s < anzSpieler; s++) {
            int ersteSpielerSpalte = getVornameSpalte(s);
            int letzteSpielerSpalte = konfigurationSheet.isMeldeListeVereinsnameAnzeigen()
                    ? getVereinsnameSpalte(s)
                    : getNachnameSpalte(s);
            RangePosition spielerRange = RangePosition.from(ersteSpielerSpalte, ERSTE_DATEN_ZEILE_OVERRIDE,
                    letzteSpielerSpalte, letzteDatenZeile);
            RangeHelper.from(sheet, spielerRange).setRangeProperties(
                    RangeProperties.from().setBorder(
                            BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder()));
        }

        RangePosition aktivRange = RangePosition.from(getAktivSpalte(), ERSTE_DATEN_ZEILE_OVERRIDE,
                getAktivSpalte(), letzteDatenZeile);
        RangeHelper.from(sheet, aktivRange).setRangeProperties(
                RangeProperties.from().centerJustify()
                        .setBorder(BorderFactory.from().allThin().toBorder()));

        String kondAktivUngueltig = "AND(NOT(ISBLANK(" + ConditionalFormatHelper.FORMULA_CURRENT_CELL + "));"
                + ConditionalFormatHelper.FORMULA_CURRENT_CELL + "<>1)";
        ConditionalFormatHelper.from(sheet, aktivRange).clear()
                .formula1(kondAktivUngueltig).operator(ConditionOperator.FORMULA)
                .styleIsFehler().applyAndDoReset()
                .formulaIsEvenRow().style(farbeGerade).applyAndDoReset()
                .formulaIsOddRow().style(farbeUngerade).applyAndDoReset();

        EditierbaresZelleFormatHelper.anwenden(sheet,
                RangePosition.from(1, ERSTE_DATEN_ZEILE_OVERRIDE, getAktivSpalte(), letzteDatenZeile));
    }

    // ---------------------------------------------------------------
    // Daten einlesen
    // ---------------------------------------------------------------

    TeamMeldungen getAlleMeldungen() throws GenerateException {
        sheet.processBoxinfo("processbox.meldeliste.sortieren");
        XSpreadsheet xSheet = sheet.getXSpreadSheet();
        SheetHelper sh = sheet.getSheetHelper();
        int letzte = letzteZeileMitDaten(xSheet);
        int aktivSpalte = getAktivSpalte();

        TeamMeldungen meldungen = new TeamMeldungen();
        for (int z = ERSTE_DATEN_ZEILE_OVERRIDE; z <= letzte; z++) {
            int nr = sh.getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), z));
            if (nr <= 0) {
                continue;
            }
            int aktiv = sh.getIntFromCell(xSheet, Position.from(aktivSpalte, z));
            if (aktiv == 1) {
                meldungen.addTeamWennNichtVorhanden(Team.from(nr));
            }
        }
        return meldungen;
    }

    Map<Integer, String> leseTeamNamenMap() throws GenerateException {
        XSpreadsheet xSheet = sheet.getXSpreadSheet();
        SheetHelper sh = sheet.getSheetHelper();
        int letzte = letzteZeileMitDaten(xSheet);
        boolean teamnameAktiv = konfigurationSheet.isMeldeListeTeamnameAnzeigen();

        Map<Integer, String> map = new HashMap<>();
        for (int z = ERSTE_DATEN_ZEILE_OVERRIDE; z <= letzte; z++) {
            int nr = sh.getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), z));
            if (nr <= 0) {
                continue;
            }
            map.put(nr, leseAnzeigename(sh, xSheet, z, teamnameAktiv));
        }
        return map;
    }

    private String leseAnzeigename(SheetHelper sh, XSpreadsheet xSheet, int zeile, boolean teamnameAktiv)
            throws GenerateException {
        if (teamnameAktiv) {
            String tn = sh.getTextFromCell(xSheet, Position.from(getTeamnameSpalte(), zeile));
            return tn != null ? tn.strip() : "";
        }
        var sb = new StringBuilder();
        for (int s = 0; s < Formation.TRIPLETTE.getAnzSpieler(); s++) {
            String vn = sh.getTextFromCell(xSheet, Position.from(getVornameSpalte(s), zeile));
            String nn = sh.getTextFromCell(xSheet, Position.from(getNachnameSpalte(s), zeile));
            vn = vn != null ? vn.strip() : "";
            nn = nn != null ? nn.strip() : "";
            if (vn.isEmpty() && nn.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(" / ");
            }
            sb.append(vn).append(" ").append(nn);
        }
        return sb.toString().strip();
    }

    // ---------------------------------------------------------------
    // IMitSpielerSpalte-Methoden
    // ---------------------------------------------------------------

    int getErsteDatenZiele() {
        return ERSTE_DATEN_ZEILE_OVERRIDE;
    }

    int getLetzteDatenZeileUseMin() throws GenerateException {
        int minZeile = ERSTE_DATEN_ZEILE_OVERRIDE + MIN_ANZAHL_MELDUNGEN_ZEILEN - 1;
        int actualZeile = letzteZeileMitDaten(sheet.getXSpreadSheet()) + 10;
        return Math.max(minZeile, actualZeile);
    }

    int getLetzteMitDatenZeileInSpielerNrSpalte() throws GenerateException {
        return letzteZeileMitDaten(sheet.getXSpreadSheet());
    }

    int naechsteFreieDatenZeileInSpielerNrSpalte() throws GenerateException {
        return letzteZeileMitDaten(sheet.getXSpreadSheet()) + 1;
    }

    int letzteZeileMitSpielerName() throws GenerateException {
        return letzteZeileMitDaten(sheet.getXSpreadSheet());
    }

    int getSpielerZeileNr(int spielerNr) throws GenerateException {
        XSpreadsheet xSheet = sheet.getXSpreadSheet();
        SheetHelper sh = sheet.getSheetHelper();
        int letzte = letzteZeileMitDaten(xSheet);
        for (int z = ERSTE_DATEN_ZEILE_OVERRIDE; z <= letzte; z++) {
            int nr = sh.getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), z));
            if (nr == spielerNr) {
                return z;
            }
        }
        return -1;
    }

    List<Integer> getSpielerNrList() throws GenerateException {
        XSpreadsheet xSheet = sheet.getXSpreadSheet();
        int letzte = letzteZeileMitDaten(xSheet);
        List<Integer> liste = new ArrayList<>();
        if (letzte < ERSTE_DATEN_ZEILE_OVERRIDE) {
            return liste;
        }
        RangePosition nrRange = RangePosition.from(getTeamNrSpalte(), ERSTE_DATEN_ZEILE_OVERRIDE,
                getTeamNrSpalte(), letzte);
        RangeData data = RangeHelper.from(xSheet, sheet.getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), nrRange)
                .getDataFromRange();
        for (RowData row : data) {
            int nr = row.get(0).getIntVal(-1);
            if (nr > 0) {
                liste.add(nr);
            }
        }
        Collections.sort(liste);
        return liste;
    }

    List<String> getSpielerNamenList() throws GenerateException {
        XSpreadsheet xSheet = sheet.getXSpreadSheet();
        SheetHelper sh = sheet.getSheetHelper();
        int letzte = letzteZeileMitDaten(xSheet);
        boolean teamnameAktiv = konfigurationSheet.isMeldeListeTeamnameAnzeigen();
        List<String> liste = new ArrayList<>();
        for (int z = ERSTE_DATEN_ZEILE_OVERRIDE; z <= letzte; z++) {
            String name = leseAnzeigename(sh, xSheet, z, teamnameAktiv);
            if (!name.isBlank()) {
                liste.add(name);
            }
        }
        Collections.sort(liste);
        return liste;
    }

    // ---------------------------------------------------------------
    // Formel für Spielplan-Teamnamen
    // ---------------------------------------------------------------

    String formulaSverweisSpielernamen(String nrAdresse) {
        return MeldeListeHelper.teamNameFormel(nrAdresse,
                konfigurationSheet.isMeldeListeTeamnameAnzeigen(),
                Formation.TRIPLETTE,
                konfigurationSheet.isMeldeListeVereinsnameAnzeigen());
    }

    int getSpielerNameErsteSpalte() {
        int tnSpalte = getTeamnameSpalte();
        return tnSpalte >= 0 ? tnSpalte : getVornameSpalte(0);
    }

    int letzteSpielTagSpalte() {
        return getAktivSpalte() + 1;
    }

    // ---------------------------------------------------------------
    // Hilfsmethoden
    // ---------------------------------------------------------------

    private int letzteZeileMitDaten(XSpreadsheet xSheet) throws GenerateException {
        int letzte = ERSTE_DATEN_ZEILE_OVERRIDE - 1;
        int maxZeile = ERSTE_DATEN_ZEILE_OVERRIDE + MeldungenSpalte.MAX_ANZ_MELDUNGEN;
        RangePosition range = RangePosition.from(getVornameSpalte(0), ERSTE_DATEN_ZEILE_OVERRIDE,
                getVornameSpalte(0), maxZeile);
        RangeData data = RangeHelper.from(xSheet, sheet.getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), range)
                .getDataFromRange();
        int zeile = ERSTE_DATEN_ZEILE_OVERRIDE;
        for (RowData row : data) {
            if (!row.isEmpty()) {
                String vorname = row.get(0).getStringVal();
                if (vorname != null && !vorname.isBlank()) {
                    letzte = zeile;
                }
            }
            zeile++;
        }
        return letzte;
    }
}
