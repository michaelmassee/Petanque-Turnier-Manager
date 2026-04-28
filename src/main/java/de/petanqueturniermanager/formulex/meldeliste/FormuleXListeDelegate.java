/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.meldeliste;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.ConditionOperator;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXPropertiesSpalte;
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
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Delegate für die Formule X Meldeliste.<br>
 * Spaltenstruktur: Nr | [Teamname] | Spieler-Blöcke (Vorname+Nachname[+Vereinsname]) | SP | Aktiv<br>
 * 3 Header-Zeilen analog KO-Meldeliste.
 */
class FormuleXListeDelegate implements MeldeListeKonstanten {

    /** Minimale Anzahl Datenzeilen (immer vorhanden). */
    static final int MIN_ANZAHL_MELDUNGEN_ZEILEN = 32;

    /** Dritte Header-Zeile (Spalten-Namen: Vorname, Nachname, Verein). */
    static final int DRITTE_HEADER_ZEILE = 2;

    /**
     * Erste Daten-Zeile: 3 Header-Zeilen (überschreibt MeldeListeKonstanten.ERSTE_DATEN_ZEILE=2).
     */
    static final int ERSTE_DATEN_ZEILE = 3;

    static final int AKTIV_WERT_NIMMT_TEIL = 1;
    static final int AKTIV_WERT_AUSGESTIEGEN = 2;

    private static final int NR_SPALTE_WIDTH = 800;
    private static final int NAME_SPALTE_WIDTH = 3000;
    private static final int TEAMNAME_SPALTE_WIDTH = 3000;
    private static final int VEREINSNAME_SPALTE_WIDTH = 2500;
    private static final int SP_SPALTE_WIDTH = 800;
    private static final int AKTIV_SPALTE_WIDTH = 700;

    private final ISheet sheet;
    private final FormuleXKonfigurationSheet konfigurationSheet;

    FormuleXListeDelegate(ISheet sheet) {
        this.sheet = checkNotNull(sheet);
        konfigurationSheet = new FormuleXKonfigurationSheet(sheet.getWorkingSpreadsheet());
    }

    FormuleXKonfigurationSheet getKonfigurationSheet() {
        return konfigurationSheet;
    }

    // ---------------------------------------------------------------
    // Spalten-Berechnung (abhängig von Konfiguration)
    // ---------------------------------------------------------------

    /** Spalte für die Team-Nummer (immer Spalte 0). */
    int getTeamNrSpalte() {
        return SPIELER_NR_SPALTE; // = 0
    }

    /** Spalte für den Teamnamen, oder -1 wenn deaktiviert. */
    int getTeamnameSpalte() throws GenerateException {
        return konfigurationSheet.isMeldeListeTeamnameAnzeigen() ? 1 : -1;
    }

    /** Anzahl Spalten pro Spieler: 2 (Vorname+Nachname) oder 3 (+Vereinsname). */
    int getSpaltenProSpieler() throws GenerateException {
        return konfigurationSheet.isMeldeListeVereinsnameAnzeigen() ? 3 : 2;
    }

    /** Index der ersten Spieler-Spalte (Vorname Spieler 1). */
    int getErsterSpielerOffset() throws GenerateException {
        return konfigurationSheet.isMeldeListeTeamnameAnzeigen() ? 2 : 1;
    }

    /** Vorname-Spalte für Spieler spielerIdx (0-basiert). */
    int getVornameSpalte(int spielerIdx) throws GenerateException {
        return getErsterSpielerOffset() + spielerIdx * getSpaltenProSpieler();
    }

    /** Nachname-Spalte für Spieler spielerIdx. */
    int getNachnameSpalte(int spielerIdx) throws GenerateException {
        return getVornameSpalte(spielerIdx) + 1;
    }

    /** Vereinsname-Spalte für Spieler spielerIdx, oder -1 wenn deaktiviert. */
    int getVereinsnameSpalte(int spielerIdx) throws GenerateException {
        if (!konfigurationSheet.isMeldeListeVereinsnameAnzeigen()) {
            return -1;
        }
        return getVornameSpalte(spielerIdx) + 2;
    }

    /** Letzte Spieler-Datenspalte (0-basiert, ohne SP). */
    int getLetzteDataSpalte() throws GenerateException {
        Formation f = konfigurationSheet.getMeldeListeFormation();
        return getErsterSpielerOffset() + f.getAnzSpieler() * getSpaltenProSpieler() - 1;
    }

    /** Setzposition-Spalte (SP) – direkt nach der letzten Spieler-Spalte. */
    int getSetzPositionSpalte() throws GenerateException {
        return getLetzteDataSpalte() + 1;
    }

    /** Aktiv/Inaktiv-Spalte – direkt nach der SP-Spalte. */
    int getAktivSpalte() throws GenerateException {
        return getSetzPositionSpalte() + 1;
    }

    // ---------------------------------------------------------------
    // Sheet-Aufbau
    // ---------------------------------------------------------------

    void upDateSheet() throws GenerateException {
        sheet.processBoxinfo("processbox.ko.meldeliste.aktualisieren");
        XSpreadsheet xSheet = sheet.getXSpreadSheet();
        TurnierSheet.from(xSheet, sheet.getWorkingSpreadsheet()).setActiv();

        insertHeaderInSheet(konfigurationSheet.getMeldeListeHeaderFarbe());
        formatZeilenfarben();
        formatDatenSpalten();

        SheetFreeze.from(xSheet, sheet.getWorkingSpreadsheet()).anzZeilen(3).doFreeze();

        XSpreadsheet xKonfigSheet = sheet.getSheetHelper().findByName(SheetNamen.formulexKonfiguration());
        if (xKonfigSheet != null) {
            renderKonfigurationsZuZellen(xKonfigSheet);
        }
    }

    /**
     * Schreibt alle Formule X Konfigurationseigenschaften als 2-spaltige Tabelle in das Konfigurationssheet.
     */
    private void renderKonfigurationsZuZellen(XSpreadsheet xKonfigSheet) throws GenerateException {
        int headerFarbe = Integer.parseInt(BasePropertiesSpalte.HEADER_BACK_COLOR.replace("#", ""), 16);

        sheet.getSheetHelper().setStringValueInCell(
                StringCellValue.from(xKonfigSheet, Position.from(0, 0), "Eigenschaft")
                        .setCharWeight(FontWeight.BOLD)
                        .setCellBackColor(headerFarbe));
        sheet.getSheetHelper().setStringValueInCell(
                StringCellValue.from(xKonfigSheet, Position.from(1, 0), "Wert")
                        .setCharWeight(FontWeight.BOLD)
                        .setCellBackColor(headerFarbe));

        List<ConfigProperty<?>> props = FormuleXPropertiesSpalte.KONFIG_PROPERTIES;
        int zeile = 1;
        for (ConfigProperty<?> prop : props) {
            String beschreibung = prop.getDescription() != null ? prop.getDescription() : prop.getKey();
            beschreibung = beschreibung.split("[\r\n]")[0];

            String wert;
            if (prop.getType() == ConfigPropertyType.COLOR) {
                int farbwert = konfigurationSheet.getPropertiesSpalte().readIntProperty(prop.getKey());
                wert = String.format("#%06X", farbwert & 0xFFFFFF);
            } else if (prop.getType() == ConfigPropertyType.INTEGER) {
                wert = String.valueOf(konfigurationSheet.getPropertiesSpalte().readIntProperty(prop.getKey()));
            } else {
                wert = konfigurationSheet.getPropertiesSpalte().readStringProperty(prop.getKey());
            }

            sheet.getSheetHelper().setStringValueInCell(
                    StringCellValue.from(xKonfigSheet, Position.from(0, zeile), beschreibung));
            sheet.getSheetHelper().setStringValueInCell(
                    StringCellValue.from(xKonfigSheet, Position.from(1, zeile), wert));
            zeile++;
        }
    }

    private void insertHeaderInSheet(int headerColor) throws GenerateException {
        sheet.processBoxinfo("processbox.ko.meldeliste.einfuegen");

        Formation formation = konfigurationSheet.getMeldeListeFormation();
        int anzSpieler = formation.getAnzSpieler();
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
                        .setCharWeight(FontWeight.BOLD)
                        .setComment(I18n.get("ko.meldeliste.comment.startnummer"))
                        .setEndPosMergeZeilePlus(1)
                        .setShrinkToFit(true));

        if (teamnameAktiv) {
            sheet.getSheetHelper().setStringValueInCell(
                    StringCellValue.from(sheet.getXSpreadSheet(), Position.from(1, ZWEITE_HEADER_ZEILE), I18n.get("column.header.teamname"))
                            .addColumnProperties(colPropName.setWidth(TEAMNAME_SPALTE_WIDTH))
                            .setCellBackColor(headerColor)
                            .setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
                            .setVertJustify(CellVertJustify2.CENTER)
                            .setCharWeight(FontWeight.BOLD)
                            .setEndPosMergeZeilePlus(1)
                            .setShrinkToFit(true));
        }

        for (int s = 0; s < anzSpieler; s++) {
            int vornameSpalte = getVornameSpalte(s);
            String spielerTitel = "Spieler " + (s + 1);

            sheet.getSheetHelper().setStringValueInCell(
                    StringCellValue.from(sheet.getXSpreadSheet(), Position.from(vornameSpalte, ZWEITE_HEADER_ZEILE),
                            spielerTitel)
                            .addColumnProperties(colPropName)
                            .setCellBackColor(headerColor)
                            .setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
                            .setVertJustify(CellVertJustify2.CENTER)
                            .setHoriJustify(CellHoriJustify.CENTER)
                            .setCharWeight(FontWeight.BOLD)
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
                                Position.from(getVereinsnameSpalte(s), DRITTE_HEADER_ZEILE), I18n.get("column.header.vereinsname"))
                                .addColumnProperties(colPropName.setWidth(VEREINSNAME_SPALTE_WIDTH))
                                .setCellBackColor(headerColor)
                                .setBorder(BorderFactory.from().allThin().toBorder())
                                .setVertJustify(CellVertJustify2.CENTER)
                                .setShrinkToFit(true));
            }
        }

        ColumnProperties colPropSp = ColumnProperties.from().setWidth(SP_SPALTE_WIDTH)
                .setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER)
                .margin(MeldeListeKonstanten.CELL_MARGIN);
        sheet.getSheetHelper().setStringValueInCell(
                StringCellValue.from(sheet.getXSpreadSheet(), Position.from(getSetzPositionSpalte(), ZWEITE_HEADER_ZEILE),
                        I18n.get("column.header.setzposition"))
                        .addColumnProperties(colPropSp)
                        .setCellBackColor(headerColor)
                        .setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
                        .setVertJustify(CellVertJustify2.CENTER)
                        .setCharWeight(FontWeight.BOLD)
                        .setComment(I18n.get("kaskade.meldeliste.comment.setzposition"))
                        .setRotate90()
                        .setEndPosMergeZeilePlus(1)
                        .setShrinkToFit(true));

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
                        .setCharWeight(FontWeight.BOLD)
                        .setComment(I18n.get("schweizer.meldeliste.comment.aktiv"))
                        .setRotate90()
                        .setEndPosMergeZeilePlus(1)
                        .setShrinkToFit(true));

        sheet.getSheetHelper().setStringValueInCell(
                StringCellValue
                        .from(sheet.getXSpreadSheet(), Position.from(0, ERSTE_HEADER_ZEILE),
                                I18n.get("meldeliste.header.turniersystem", TurnierSystem.FORMULEX.getBezeichnung()))
                        .setEndPosMergeSpaltePlus(2)
                        .setCharWeight(FontWeight.BOLD)
                        .setHoriJustify(CellHoriJustify.LEFT)
                        .setVertJustify(CellVertJustify2.TOP)
                        .setShrinkToFit(true)
                        .setCharColor("00599d"));
    }

    private int getLetzteDatenZeileUseMin() throws GenerateException {
        int minZeile = ERSTE_DATEN_ZEILE + MIN_ANZAHL_MELDUNGEN_ZEILEN - 1;
        int actualZeile = letzteZeileMitDaten(sheet.getXSpreadSheet()) + 10;
        return Math.max(minZeile, actualZeile);
    }

    private void formatDatenSpalten() throws GenerateException {
        sheet.processBoxinfo("processbox.ko.meldeliste.formatieren");
        Formation formation = konfigurationSheet.getMeldeListeFormation();
        int anzSpieler = formation.getAnzSpieler();
        int letzteDatenZeile = getLetzteDatenZeileUseMin();

        MeldungenHintergrundFarbeGeradeStyle farbeGerade = konfigurationSheet.getMeldeListeHintergrundFarbeGeradeStyle();
        MeldungenHintergrundFarbeUnGeradeStyle farbeUngerade = konfigurationSheet
                .getMeldeListeHintergrundFarbeUnGeradeStyle();

        RangePosition nrRange = RangePosition.from(getTeamNrSpalte(), ERSTE_DATEN_ZEILE,
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
            RangePosition teamnameRange = RangePosition.from(1, ERSTE_DATEN_ZEILE, 1, letzteDatenZeile);
            RangeHelper.from(sheet, teamnameRange).setRangeProperties(
                    RangeProperties.from().setBorder(
                            BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder()));
        }

        for (int s = 0; s < anzSpieler; s++) {
            int ersteSpielerSpalte = getVornameSpalte(s);
            int letzteSpielerSpalte = konfigurationSheet.isMeldeListeVereinsnameAnzeigen()
                    ? getVereinsnameSpalte(s)
                    : getNachnameSpalte(s);
            RangePosition spielerRange = RangePosition.from(ersteSpielerSpalte, ERSTE_DATEN_ZEILE,
                    letzteSpielerSpalte, letzteDatenZeile);
            RangeHelper.from(sheet, spielerRange).setRangeProperties(
                    RangeProperties.from().setBorder(
                            BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder()));
        }

        RangePosition spRange = RangePosition.from(getSetzPositionSpalte(), ERSTE_DATEN_ZEILE,
                getSetzPositionSpalte(), letzteDatenZeile);
        RangeHelper.from(sheet, spRange).setRangeProperties(
                RangeProperties.from().setBorder(
                        BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder()));

        ConditionalFormatHelper.from(sheet, spRange).clear()
                .formulaIsText().styleIsFehler().applyAndDoReset()
                .formulaIsEvenRow().style(farbeGerade).applyAndDoReset()
                .formulaIsOddRow().style(farbeUngerade).applyAndDoReset();

        RangePosition aktivRange = RangePosition.from(getAktivSpalte(), ERSTE_DATEN_ZEILE,
                getAktivSpalte(), letzteDatenZeile);
        RangeHelper.from(sheet, aktivRange).setRangeProperties(
                RangeProperties.from().centerJustify()
                        .setBorder(BorderFactory.from().allThin().toBorder()));

        String kondAktivUngueltig = "AND(NOT(ISBLANK(" + ConditionalFormatHelper.FORMULA_CURRENT_CELL + "));"
                + ConditionalFormatHelper.FORMULA_CURRENT_CELL + "<>1;"
                + ConditionalFormatHelper.FORMULA_CURRENT_CELL + "<>2)";
        ConditionalFormatHelper.from(sheet, aktivRange).clear()
                .formula1(kondAktivUngueltig).operator(ConditionOperator.FORMULA)
                .styleIsFehler().applyAndDoReset()
                .formulaIsEvenRow().style(farbeGerade).applyAndDoReset()
                .formulaIsOddRow().style(farbeUngerade).applyAndDoReset();

        EditierbaresZelleFormatHelper.anwenden(sheet, RangePosition.from(1, ERSTE_DATEN_ZEILE, getAktivSpalte(), letzteDatenZeile));
    }

    private void formatZeilenfarben() throws GenerateException {
        MeldungenHintergrundFarbeGeradeStyle farbeGerade = konfigurationSheet.getMeldeListeHintergrundFarbeGeradeStyle();
        MeldungenHintergrundFarbeUnGeradeStyle farbeUngerade = konfigurationSheet
                .getMeldeListeHintergrundFarbeUnGeradeStyle();
        int letzteDatenZeile = getLetzteDatenZeileUseMin();

        RangePosition datenRange = RangePosition.from(getTeamNrSpalte(), ERSTE_DATEN_ZEILE,
                getAktivSpalte(), letzteDatenZeile);
        ConditionalFormatHelper.from(sheet, datenRange).clear().formulaIsEvenRow().style(farbeGerade).applyAndDoReset();
        ConditionalFormatHelper.from(sheet, datenRange).formulaIsOddRow().style(farbeUngerade).applyAndDoReset();
    }

    // ---------------------------------------------------------------
    // Daten einlesen
    // ---------------------------------------------------------------

    /** Liefert alle aktiven Teams aus der Meldeliste, sortiert nach Nr. */
    TeamMeldungen getAktiveMeldungen() throws GenerateException {
        sheet.processBoxinfo("processbox.ko.meldeliste.einlesen");
        XSpreadsheet xSheet = sheet.getXSpreadSheet();
        int vornameSpalte = getVornameSpalte(0);
        int spSpalte = getSetzPositionSpalte();
        int aktivSpalte = getAktivSpalte();
        int letzteZeile = letzteZeileMitDaten(xSheet);

        TeamMeldungen meldungen = new TeamMeldungen();
        for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
            String vorname = sheet.getSheetHelper().getTextFromCell(xSheet, Position.from(vornameSpalte, zeile));
            if (vorname == null || vorname.isEmpty()) {
                continue;
            }
            int nr = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), zeile));
            if (nr <= 0) {
                continue;
            }
            int aktiv = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(aktivSpalte, zeile));
            if (aktiv == AKTIV_WERT_NIMMT_TEIL) {
                int setzPos = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(spSpalte, zeile));
                meldungen.addTeamWennNichtVorhanden(Team.from(nr).setSetzPos(setzPos));
            }
        }
        return meldungen;
    }

    /**
     * Liefert alle aktiven Teams in einer Reihenfolge, bei der Teams mit gleicher SP
     * nicht aufeinandertreffen (Schweizer-Semantik: gleiche SP = nicht in Runde 1 gegeneinander).
     */
    TeamMeldungen getMeldungenSortiertNachSetzposition() throws GenerateException {
        sheet.processBoxinfo("processbox.ko.meldeliste.sortieren");
        XSpreadsheet xSheet = sheet.getXSpreadSheet();
        int vornameSpalte = getVornameSpalte(0);
        int spSpalte = getSetzPositionSpalte();
        int aktivSpalte = getAktivSpalte();
        int letzteZeile = letzteZeileMitDaten(xSheet);

        record TeamZeile(int nr, int sp) {}
        List<TeamZeile> alleTeams = new ArrayList<>();

        for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
            String vorname = sheet.getSheetHelper().getTextFromCell(xSheet, Position.from(vornameSpalte, zeile));
            if (vorname == null || vorname.isEmpty()) {
                continue;
            }
            int nr = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), zeile));
            if (nr <= 0) {
                continue;
            }
            int aktiv = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(aktivSpalte, zeile));
            if (aktiv != AKTIV_WERT_NIMMT_TEIL) {
                continue;
            }
            int sp = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(spSpalte, zeile));
            alleTeams.add(new TeamZeile(nr, sp));
        }

        Map<Integer, List<TeamZeile>> setzGruppen = new LinkedHashMap<>();
        List<TeamZeile> ungesetzte = new ArrayList<>();
        for (TeamZeile t : alleTeams) {
            if (t.sp() > 0) {
                setzGruppen.computeIfAbsent(t.sp(), k -> new ArrayList<>()).add(t);
            } else {
                ungesetzte.add(t);
            }
        }
        setzGruppen.values().forEach(gruppe -> gruppe.sort(Comparator.comparingInt(TeamZeile::nr)));
        ungesetzte.sort(Comparator.comparingInt(TeamZeile::nr));

        List<List<TeamZeile>> eimer = new ArrayList<>(setzGruppen.values());
        eimer.add(ungesetzte);

        List<TeamZeile> ergebnis = new ArrayList<>(alleTeams.size());
        boolean nochElemente = true;
        while (nochElemente) {
            nochElemente = false;
            for (var eimer1 : eimer) {
                if (!eimer1.isEmpty()) {
                    ergebnis.add(eimer1.remove(0));
                    nochElemente = true;
                }
            }
        }

        TeamMeldungen meldungen = new TeamMeldungen();
        for (TeamZeile t : ergebnis) {
            meldungen.addTeamWennNichtVorhanden(Team.from(t.nr()).setSetzPos(t.sp()));
        }
        return meldungen;
    }

    @SuppressWarnings("SameReturnValue")
    String validiereSetzpositionSpalte() {
        return null;
    }

    /**
     * Letzte Zeile mit einem nicht-leeren Vorname (Spieler 1) ab ERSTE_DATEN_ZEILE.
     */
    int letzteZeileMitDaten(XSpreadsheet xSheet) throws GenerateException {
        int vornameSpalte = getVornameSpalte(0);
        int letzte = ERSTE_DATEN_ZEILE - 1;
        int maxZeile = ERSTE_DATEN_ZEILE + 500;
        for (int zeile = ERSTE_DATEN_ZEILE; zeile <= maxZeile; zeile++) {
            String vorname = sheet.getSheetHelper().getTextFromCell(xSheet, Position.from(vornameSpalte, zeile));
            if (vorname != null && !vorname.isEmpty()) {
                letzte = zeile;
            }
        }
        return letzte;
    }

    // ---------------------------------------------------------------
    // Feld-Bereinigung und -Prüfung
    // ---------------------------------------------------------------

    void stringsBesinigen(XSpreadsheet xSheet) throws GenerateException {
        Formation formation = konfigurationSheet.getMeldeListeFormation();
        boolean teamnameAktiv = konfigurationSheet.isMeldeListeTeamnameAnzeigen();
        boolean vereinsnameAktiv = konfigurationSheet.isMeldeListeVereinsnameAnzeigen();
        int letzteZeile = letzteZeileMitDaten(xSheet) + MIN_ANZAHL_MELDUNGEN_ZEILEN;
        for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
            if (teamnameAktiv) {
                bereinigeSpalte(xSheet, getTeamnameSpalte(), zeile);
            }
            for (int s = 0; s < formation.getAnzSpieler(); s++) {
                bereinigeSpalte(xSheet, getVornameSpalte(s), zeile);
                bereinigeSpalte(xSheet, getNachnameSpalte(s), zeile);
                if (vereinsnameAktiv) {
                    bereinigeSpalte(xSheet, getVereinsnameSpalte(s), zeile);
                }
            }
        }
    }

    private void bereinigeSpalte(XSpreadsheet xSheet, int spalte, int zeile) throws GenerateException {
        if (spalte < 0) {
            return;
        }
        String original = sheet.getSheetHelper().getTextFromCell(xSheet, Position.from(spalte, zeile));
        if (original == null || original.isEmpty()) {
            return;
        }
        String bereinigt = original.replaceAll("[\\p{Cntrl}]", "").strip();
        if (!bereinigt.equals(original)) {
            sheet.getSheetHelper().setStringValueInCell(StringCellValue.from(xSheet, Position.from(spalte, zeile), bereinigt));
        }
    }

    void pruefeAufDoppelteTeamNr(XSpreadsheet xSheet) throws GenerateException {
        int letzteZeile = letzteZeileMitDaten(xSheet);
        if (letzteZeile < ERSTE_DATEN_ZEILE) {
            return;
        }
        int vornameSpalte = getVornameSpalte(0);
        Map<Integer, List<Integer>> alleNrn = new LinkedHashMap<>();
        for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
            String vorname = sheet.getSheetHelper().getTextFromCell(xSheet, Position.from(vornameSpalte, zeile));
            if (vorname == null || vorname.isEmpty()) {
                continue;
            }
            int nr = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), zeile));
            if (nr <= 0) {
                continue;
            }
            alleNrn.computeIfAbsent(nr, k -> new ArrayList<>()).add(zeile);
        }
        Map<Integer, List<Integer>> duplikate = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<Integer>> entry : alleNrn.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplikate.put(entry.getKey(), entry.getValue());
            }
        }
        if (duplikate.isEmpty()) {
            return;
        }
        var sb = new StringBuilder("Meldeliste wurde nicht aktualisiert.\nDoppelte Startnummern:");
        for (Map.Entry<Integer, List<Integer>> entry : duplikate.entrySet()) {
            sb.append("\nNr. ").append(entry.getKey()).append(": Zeilen ");
            List<Integer> zeilen = entry.getValue();
            for (int i = 0; i < zeilen.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(zeilen.get(i) + 1);
            }
        }
        throw new GenerateException(sb.toString());
    }

    /** Liest alle Team-Meldungen aus dem Sheet, unabhängig vom Aktiv-Status. */
    TeamMeldungen getAlleMeldungen() throws GenerateException {
        XSpreadsheet xSheet = sheet.getXSpreadSheet();
        int letzteZeile = letzteZeileMitDaten(xSheet);
        TeamMeldungen meldungen = new TeamMeldungen();
        for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
            String vorname = sheet.getSheetHelper().getTextFromCell(xSheet, Position.from(getVornameSpalte(0), zeile));
            if (vorname == null || vorname.isEmpty()) {
                continue;
            }
            int nr = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), zeile));
            if (nr <= 0) {
                continue;
            }
            int setzPos = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(getSetzPositionSpalte(), zeile));
            meldungen.addTeamWennNichtVorhanden(Team.from(nr).setSetzPos(setzPos));
        }
        return meldungen;
    }

    /** Setzt alle Teams mit gültigem Vornamen und Teamnummer auf AKTIV_WERT_NIMMT_TEIL. */
    void alleTeamsAktivieren() throws GenerateException {
        XSpreadsheet xSheet = sheet.getXSpreadSheet();
        int letzteZeile = letzteZeileMitDaten(xSheet);
        for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
            String vorname = sheet.getSheetHelper().getTextFromCell(xSheet, Position.from(getVornameSpalte(0), zeile));
            if (vorname == null || vorname.isEmpty()) {
                continue;
            }
            int nr = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), zeile));
            if (nr <= 0) {
                continue;
            }
            sheet.getSheetHelper().setNumberValueInCell(
                    NumberCellValue.from(xSheet, Position.from(getAktivSpalte(), zeile))
                            .setValue(AKTIV_WERT_NIMMT_TEIL));
        }
    }

    // ---------------------------------------------------------------
    // Weiterleitungs-Methoden
    // ---------------------------------------------------------------

    int getErsteDatenZeile() {
        return ERSTE_DATEN_ZEILE;
    }

    int getNrSpalte() {
        return getTeamNrSpalte();
    }
}
