package de.petanqueturniermanager.liga.spielplan;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.sun.star.container.XNamed;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.RangeProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.RanglisteGeradeUngeradeFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.numberformat.UserNumberFormat;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.liga.konfiguration.LigaKonfigurationSheet;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheetUpdate;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;

public class LigaTermineProTeilnehmerSheet extends SheetRunner implements ISheet {

    private static final String METADATA_SCHLUESSEL =
            SheetMetadataHelper.SCHLUESSEL_LIGA_TERMINE_PRO_TEILNEHMER;
    private static final int HEADER_ZEILE = 0;
    private static final int ERSTE_DATEN_ZEILE = 2;
    private static final int MAX_SPIELPLAN_ZEILE = 999;

    private static final int SPIEL_NR_SPALTE = 0;
    private static final int DATUM_SPALTE = 1;
    private static final int UHRZEIT_SPALTE = 2;
    private static final int ORT_SPALTE = 3;
    private static final int HEIM_GAST_SPALTE = 4;
    private static final int GEGNER_SPALTE = 5;
    private static final int PUNKTE_H_SPALTE = 6;
    private static final int PUNKTE_G_SPALTE = 7;
    private static final int SPIELE_H_SPALTE = 8;
    private static final int SPIELE_G_SPALTE = 9;
    private static final int SPIELPUNKTE_H_SPALTE = 10;
    private static final int SPIELPUNKTE_G_SPALTE = 11;
    private static final int LETZTE_SPALTE = SPIELPUNKTE_G_SPALTE;

    private final LigaKonfigurationSheet konfigurationSheet;
    private final LigaMeldeListeSheetUpdate meldeListe;
    private String aktuellerSheetName;
    private String aktuellerMetadatenSchluessel;

    public LigaTermineProTeilnehmerSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.LIGA, "Termine pro Teilnehmer");
        konfigurationSheet = new LigaKonfigurationSheet(workingSpreadsheet);
        meldeListe = new LigaMeldeListeSheetUpdate(workingSpreadsheet);
    }

    public static String sheetName() {
        return SheetNamen.ligaTermineProTeilnehmer();
    }

    public static String sheetName(int teamNr) {
        return SheetNamen.ligaTermineProTeilnehmer(teamNr);
    }

    @Override
    protected LigaKonfigurationSheet getKonfigurationSheet() {
        return konfigurationSheet;
    }

    @Override
    public XSpreadsheet getXSpreadSheet() throws GenerateException {
        if (aktuellerMetadatenSchluessel == null) {
            throw new GenerateException("Liga-Termine-Sheet wurde ohne Team-Kontext angefordert");
        }
        return SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                aktuellerMetadatenSchluessel, aktuellerSheetName);
    }

    @Override
    public TurnierSheet getTurnierSheet() throws GenerateException {
        return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
    }

    @Override
    protected void doRun() throws GenerateException {
        meldeListe.upDateSheet();
        generate(meldeListe.getAlleMeldungen());
    }

    public void generate(TeamMeldungen meldungen) throws GenerateException {
        entferneBestehendeTerminSheets();
        String spielplanName = spielplanName();
        RangeData spielplanDaten = spielplanDaten();
        List<Team> teams = meldungen.teams().stream().sorted().toList();
        Set<String> verwendeteSheetNamen = new HashSet<>();
        for (Team team : teams) {
            int teamNr = team.getNr();
            aktuellerSheetName = sheetName(teamNr, spielplanDaten, verwendeteSheetNamen);
            aktuellerMetadatenSchluessel = SheetMetadataHelper.schluesselLigaTermineProTeilnehmer(teamNr);

            NewSheet.from(this, aktuellerSheetName, aktuellerMetadatenSchluessel)
                    .pos(DefaultSheetPos.LIGA_TERMINE).tabColor(konfigurationSheet.getSpielPlanHeaderFarbe())
                    .forceCreate().hideGrid().create();

            insertHeaderUndSpalten();
            int letzteDatenZeile = insertTermine(teamNr, spielplanName, spielplanDaten);
            formatieren(letzteDatenZeile);
        }
    }

    private void insertHeaderUndSpalten() throws GenerateException {
        XSpreadsheet sheet = getXSpreadSheet();
        StringCellValue header = StringCellValue.from(sheet, Position.from(SPIEL_NR_SPALTE, HEADER_ZEILE))
                .setCharWeight(com.sun.star.awt.FontWeight.BOLD).centerJustify().setShrinkToFit(true);

        getSheetHelper().setStringValueInCell(
                header.setValue(I18n.get("liga.termine.header.spiel")).setEndPosMergeZeilePlus(1));
        getSheetHelper().setStringValueInCell(
                header.spalte(DATUM_SPALTE).setValue(I18n.get("liga.termine.header.datum")).setEndPosMergeZeilePlus(1));
        getSheetHelper().setStringValueInCell(
                header.spalte(UHRZEIT_SPALTE).setValue(I18n.get("liga.termine.header.uhrzeit")).setEndPosMergeZeilePlus(1));
        getSheetHelper().setStringValueInCell(
                header.spalte(ORT_SPALTE).setValue(I18n.get("liga.termine.header.ort")).setEndPosMergeZeilePlus(1));
        getSheetHelper().setStringValueInCell(
                header.spalte(HEIM_GAST_SPALTE).setValue(I18n.get("liga.termine.header.heim.gast")).setEndPosMergeZeilePlus(1));
        getSheetHelper().setStringValueInCell(
                header.spalte(GEGNER_SPALTE).setValue(I18n.get("liga.termine.header.gegner")).setEndPosMergeZeilePlus(1));
        getSheetHelper().setStringValueInCell(header.setEndPosMerge(null).spalte(PUNKTE_H_SPALTE)
                .setValue(I18n.get("column.header.punkte")).setEndPosMergeSpaltePlus(1));
        getSheetHelper().setStringValueInCell(header.spalte(SPIELE_H_SPALTE).setValue("Siege")
                .setEndPosMergeSpaltePlus(1));
        getSheetHelper().setStringValueInCell(header.spalte(SPIELPUNKTE_H_SPALTE).setValue("SpPunkte")
                .setEndPosMergeSpaltePlus(1));

        header.setEndPosMerge(null).zeile(HEADER_ZEILE + 1);
        getSheetHelper().setStringValueInCell(header.setValue("H").spalte(PUNKTE_H_SPALTE));
        getSheetHelper().setStringValueInCell(header.setValue("G").spalte(PUNKTE_G_SPALTE));
        getSheetHelper().setStringValueInCell(header.setValue("H").spalte(SPIELE_H_SPALTE));
        getSheetHelper().setStringValueInCell(header.setValue("G").spalte(SPIELE_G_SPALTE));
        getSheetHelper().setStringValueInCell(header.setValue("H").spalte(SPIELPUNKTE_H_SPALTE));
        getSheetHelper().setStringValueInCell(header.setValue("G").spalte(SPIELPUNKTE_G_SPALTE));

        getSheetHelper().setColumnProperties(sheet, SPIEL_NR_SPALTE, ColumnProperties.from().setWidth(1400).centerJustify());
        getSheetHelper().setColumnProperties(sheet, DATUM_SPALTE, ColumnProperties.from().setWidth(2000).centerJustify());
        getSheetHelper().setColumnProperties(sheet, UHRZEIT_SPALTE, ColumnProperties.from().setWidth(1600).centerJustify());
        getSheetHelper().setColumnProperties(sheet, ORT_SPALTE, ColumnProperties.from().setWidth(4200));
        getSheetHelper().setColumnProperties(sheet, HEIM_GAST_SPALTE, ColumnProperties.from().setWidth(1300).centerJustify());
        getSheetHelper().setColumnProperties(sheet, GEGNER_SPALTE, ColumnProperties.from().setWidth(5200));
        getSheetHelper().setColumnProperties(sheet, PUNKTE_H_SPALTE, ColumnProperties.from().setWidth(900).centerJustify());
        getSheetHelper().setColumnProperties(sheet, PUNKTE_G_SPALTE, ColumnProperties.from().setWidth(900).centerJustify());
        getSheetHelper().setColumnProperties(sheet, SPIELE_H_SPALTE, ColumnProperties.from().setWidth(900).centerJustify());
        getSheetHelper().setColumnProperties(sheet, SPIELE_G_SPALTE, ColumnProperties.from().setWidth(900).centerJustify());
        getSheetHelper().setColumnProperties(sheet, SPIELPUNKTE_H_SPALTE, ColumnProperties.from().setWidth(1200).centerJustify());
        getSheetHelper().setColumnProperties(sheet, SPIELPUNKTE_G_SPALTE, ColumnProperties.from().setWidth(1200).centerJustify());
    }

    private int insertTermine(int teamNr, String spielplanName, RangeData spielplanDaten) throws GenerateException {
        int zeile = ERSTE_DATEN_ZEILE;
        for (int i = 0; i < spielplanDaten.size(); i++) {
            RowData row = spielplanDaten.get(i);
            String spielNr = row.get(0).getStringVal();
            if (spielNr == null || spielNr.isBlank()) {
                break;
            }
            int teamA = row.get(LigaSpielPlanSheet.TEAM_A_NR_SPALTE).getIntVal(0);
            int teamB = row.get(LigaSpielPlanSheet.TEAM_B_NR_SPALTE).getIntVal(0);
            if (teamA == teamNr || teamB == teamNr) {
                insertTerminZeile(zeile++, spielplanName,
                        LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE + i, teamA == teamNr);
            }
        }
        return zeile - 1;
    }

    private String spielplanName() {
        XSpreadsheet spielplan = SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.SCHLUESSEL_LIGA_SPIELPLAN, LigaSpielPlanSheet.LEGACY_SHEET_NAMEN);
        return Lo.qi(XNamed.class, spielplan).getName();
    }

    private RangeData spielplanDaten() {
        XSpreadsheet spielplan = SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.SCHLUESSEL_LIGA_SPIELPLAN, LigaSpielPlanSheet.LEGACY_SHEET_NAMEN);
        return RangeHelper.from(spielplan, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                RangePosition.from(LigaSpielPlanSheet.SPIEL_NR_SPALTE, LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
                        LigaSpielPlanSheet.TEAM_B_NR_SPALTE, MAX_SPIELPLAN_ZEILE)).getDataFromRange();
    }

    private void insertTerminZeile(int zeile, String spielplanName, int spielplanZeile, boolean heim)
            throws GenerateException {
        XSpreadsheet sheet = getXSpreadSheet();
        getSheetHelper().setFormulaInCell(formel(sheet, SPIEL_NR_SPALTE, zeile,
                ref(spielplanName, LigaSpielPlanSheet.SPIEL_NR_SPALTE, spielplanZeile)));
        getSheetHelper().setFormulaInCell(formel(sheet, DATUM_SPALTE, zeile,
                ref(spielplanName, LigaSpielPlanSheet.DATUM_SPALTE, spielplanZeile)));
        getSheetHelper().setFormulaInCell(formel(sheet, UHRZEIT_SPALTE, zeile,
                ref(spielplanName, LigaSpielPlanSheet.UHRZEIT_SPALTE, spielplanZeile)));
        getSheetHelper().setFormulaInCell(formel(sheet, ORT_SPALTE, zeile,
                leerWennLeer(ref(spielplanName, LigaSpielPlanSheet.ORT_SPALTE, spielplanZeile))));
        getSheetHelper().setStringValueInCell(StringCellValue.from(sheet, HEIM_GAST_SPALTE, zeile,
                heim ? I18n.get("liga.termine.heim") : I18n.get("liga.termine.gast")));
        getSheetHelper().setFormulaInCell(formel(sheet, GEGNER_SPALTE, zeile,
                ref(spielplanName, heim ? LigaSpielPlanSheet.NAME_B_SPALTE : LigaSpielPlanSheet.NAME_A_SPALTE,
                        spielplanZeile)));
        getSheetHelper().setFormulaInCell(formel(sheet, PUNKTE_H_SPALTE, zeile,
                ergebnisRef(spielplanName, LigaSpielPlanSheet.PUNKTE_A_SPALTE, spielplanZeile)));
        getSheetHelper().setFormulaInCell(formel(sheet, PUNKTE_G_SPALTE, zeile,
                ergebnisRef(spielplanName, LigaSpielPlanSheet.PUNKTE_B_SPALTE, spielplanZeile)));
        getSheetHelper().setFormulaInCell(formel(sheet, SPIELE_H_SPALTE, zeile,
                ergebnisRef(spielplanName, LigaSpielPlanSheet.SPIELE_A_SPALTE, spielplanZeile)));
        getSheetHelper().setFormulaInCell(formel(sheet, SPIELE_G_SPALTE, zeile,
                ergebnisRef(spielplanName, LigaSpielPlanSheet.SPIELE_B_SPALTE, spielplanZeile)));
        getSheetHelper().setFormulaInCell(formel(sheet, SPIELPUNKTE_H_SPALTE, zeile,
                ergebnisRef(spielplanName, LigaSpielPlanSheet.SPIELPNKT_A_SPALTE, spielplanZeile)));
        getSheetHelper().setFormulaInCell(formel(sheet, SPIELPUNKTE_G_SPALTE, zeile,
                ergebnisRef(spielplanName, LigaSpielPlanSheet.SPIELPNKT_B_SPALTE, spielplanZeile)));
    }

    private void formatieren(int letzteDatenZeile) throws GenerateException {
        int letzteZeile = Math.max(HEADER_ZEILE, letzteDatenZeile);
        RangePosition gesamt = RangePosition.from(SPIEL_NR_SPALTE, HEADER_ZEILE, LETZTE_SPALTE, letzteZeile);
        RangeHelper.from(this, gesamt).setRangeProperties(RangeProperties.from()
                .setBorder(BorderFactory.from().allThin().toBorder()).setShrinkToFit(true).topMargin(90).bottomMargin(90));
        RangeHelper.from(this, RangePosition.from(SPIEL_NR_SPALTE, HEADER_ZEILE, LETZTE_SPALTE, HEADER_ZEILE + 1))
                .setRangeProperties(RangeProperties.from()
                        .setCellBackColor(konfigurationSheet.getSpielPlanHeaderFarbe()).centerJustify());
        RangeHelper.from(this, RangePosition.from(PUNKTE_H_SPALTE, HEADER_ZEILE, PUNKTE_H_SPALTE, letzteZeile))
                .setRangeProperties(RangeProperties.from()
                        .setBorder(BorderFactory.from().boldLn().forLeft().toBorder()).centerJustify());
        RangeHelper.from(this, RangePosition.from(LETZTE_SPALTE + 1, HEADER_ZEILE, LETZTE_SPALTE + 1, letzteZeile))
                .setRangeProperties(RangeProperties.from()
                        .setBorder(BorderFactory.from().boldLn().forLeft().toBorder()));
        if (letzteDatenZeile >= ERSTE_DATEN_ZEILE) {
            RangePosition daten = RangePosition.from(SPIEL_NR_SPALTE, ERSTE_DATEN_ZEILE, LETZTE_SPALTE, letzteDatenZeile);
            RanglisteGeradeUngeradeFormatHelper.from(this, daten)
                    .geradeFarbe(konfigurationSheet.getSpielPlanHintergrundFarbeGerade())
                    .ungeradeFarbe(konfigurationSheet.getSpielPlanHintergrundFarbeUnGerade())
                    .apply();
            RangeHelper.from(this, RangePosition.from(DATUM_SPALTE, ERSTE_DATEN_ZEILE, DATUM_SPALTE, letzteDatenZeile))
                    .setRangeProperties(RangeProperties.from().numberFormat(UserNumberFormat.DATE_SHORT));
            RangeHelper.from(this, RangePosition.from(UHRZEIT_SPALTE, ERSTE_DATEN_ZEILE, UHRZEIT_SPALTE, letzteDatenZeile))
                    .setRangeProperties(RangeProperties.from().numberFormat(UserNumberFormat.TIME));
        }
        PrintArea.from(getXSpreadSheet(), getWorkingSpreadsheet()).setPrintArea(gesamt);
        SheetFreeze.from(getTurnierSheet()).anzZeilen(2).doFreeze();
    }

    private static String sheetName(int teamNr, RangeData spielplanDaten, Set<String> verwendeteSheetNamen) {
        String teamName = teamName(teamNr, spielplanDaten);
        String basisName = bereinigeSheetName(teamName);
        if (basisName.isBlank()) {
            basisName = sheetName(teamNr);
        }
        String eindeutig = eindeutigerSheetName(basisName, teamNr, verwendeteSheetNamen);
        verwendeteSheetNamen.add(eindeutig);
        return eindeutig;
    }

    private static String teamName(int teamNr, RangeData spielplanDaten) {
        for (RowData row : spielplanDaten) {
            if (row.get(0).getStringVal() == null || row.get(0).getStringVal().isBlank()) {
                break;
            }
            int teamA = row.get(LigaSpielPlanSheet.TEAM_A_NR_SPALTE).getIntVal(0);
            int teamB = row.get(LigaSpielPlanSheet.TEAM_B_NR_SPALTE).getIntVal(0);
            if (teamA == teamNr) {
                return row.get(LigaSpielPlanSheet.NAME_A_SPALTE).getStringVal();
            }
            if (teamB == teamNr) {
                return row.get(LigaSpielPlanSheet.NAME_B_SPALTE).getStringVal();
            }
        }
        return "";
    }

    private static String bereinigeSheetName(String name) {
        if (name == null) {
            return "";
        }
        return name.replaceAll("[\\\\/:?*\\[\\]]", " ").replaceAll("\\s+", " ").strip();
    }

    private static String eindeutigerSheetName(String basisName, int teamNr, Set<String> verwendeteSheetNamen) {
        String name = kuerzeSheetName(basisName, "");
        if (!verwendeteSheetNamen.contains(name)) {
            return name;
        }
        String suffix = " " + teamNr;
        return kuerzeSheetName(basisName, suffix) + suffix;
    }

    private static String kuerzeSheetName(String name, String suffix) {
        int maxLaenge = 31 - suffix.length();
        if (name.length() <= maxLaenge) {
            return name;
        }
        return name.substring(0, maxLaenge).strip();
    }

    private static StringCellValue formel(XSpreadsheet sheet, int spalte, int zeile, String formel) {
        return StringCellValue.from(sheet, spalte, zeile, formel);
    }

    private static String ref(String sheetName, int spalte, int zeile) {
        String escaped = sheetName.replace("'", "''");
        return "$'" + escaped + "'." + Position.from(spalte, zeile).getAddressWith$();
    }

    private static String leerWennLeer(String ref) {
        return "IF(ISBLANK(" + ref + ");\"\";" + ref + ")";
    }

    private static String ergebnisRef(String sheetName, int spalte, int zeile) {
        return leerWennLeer(ref(sheetName, spalte, zeile));
    }

    private void entferneBestehendeTerminSheets() throws GenerateException {
        XSpreadsheetDocument doc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
        for (String schluessel : SheetMetadataHelper.getSchluesselMitPrefix(doc,
                SheetMetadataHelper.SCHLUESSEL_LIGA_TERMINE_PRO_TEILNEHMER_PREFIX)) {
            Optional<XSpreadsheet> sheet = SheetMetadataHelper.findeSheet(doc, schluessel);
            if (sheet.isPresent()) {
                entferneSheet(sheet.get());
            }
        }
        XSpreadsheet legacySheet = SheetMetadataHelper.findeSheetUndHeile(doc, METADATA_SCHLUESSEL,
                SheetNamen.LEGACY_LIGA_TERMINE_PRO_TEILNEHMER);
        if (legacySheet != null) {
            entferneSheet(legacySheet);
        }
    }

    private void entferneSheet(XSpreadsheet sheet) throws GenerateException {
        XNamed named = Lo.qi(XNamed.class, sheet);
        if (named != null) {
            getSheetHelper().removeSheet(named.getName());
        }
    }

}
