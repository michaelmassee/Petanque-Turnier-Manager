/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.finalrunde;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerNamenLeser;
import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerNamenLeser.TeilnehmerNamen;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
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
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetUpdate;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterGruppenSpalteHelper;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterVorrundenRanglisteSheetUpdate;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;

/**
 * Kompakte Übersicht "Nr, Name, Gruppe" für die im Rahmen der Maastrichter-Finalrunde
 * (KO-Runde) vergebenen Final-Gruppen. Sortiert nach Team-Nr, die Gruppe-Zelle wird pro
 * Gruppenbuchstabe farblich unterschiedlich hervorgehoben.
 */
public class MaastrichterGruppenUebersichtSheet extends SheetRunner implements ISheet {

    private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;
    private static final int ERSTE_DATEN_ZEILE = 1;
    private static final int HEADER_ZEILE = 0;
    private static final int SPALTE_NR = 0;
    private static final int SPALTE_NAME = 1;
    private static final int SPALTE_GRUPPE = 2;

    /** Feste Breite der Gruppe-Spalte (1/100 mm) – Inhalt ist nur ein einzelner Buchstabe. */
    private static final int SPALTE_GRUPPE_BREITE = 700;

    /** Zyklische Schriftfarben-Palette für den Gruppenbuchstaben, nach Gruppenbuchstabe-Index vergeben (A=Grün, B=Rot, …). */
    private static final Integer[] GRUPPEN_FARBEN = {
            ColorHelper.CHAR_COLOR_GREEN, ColorHelper.CHAR_COLOR_RED, Integer.valueOf("0066cc", 16),
            ColorHelper.CHAR_COLOR_ORANGE, Integer.valueOf("7030a0", 16), Integer.valueOf("008080", 16),
            Integer.valueOf("8b4513", 16), Integer.valueOf("696969", 16)
    };

    private final MaastrichterKonfigurationSheet konfigurationSheet;
    private final MaastrichterMeldeListeSheetUpdate meldeliste;

    public MaastrichterGruppenUebersichtSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.MAASTRICHTER, "Maastrichter-Gruppen-Übersicht");
        konfigurationSheet = new MaastrichterKonfigurationSheet(workingSpreadsheet);
        meldeliste = new MaastrichterMeldeListeSheetUpdate(workingSpreadsheet);
    }

    @Override
    protected MaastrichterKonfigurationSheet getKonfigurationSheet() {
        return konfigurationSheet;
    }

    @Override
    public XSpreadsheet getXSpreadSheet() throws GenerateException {
        return SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_GRUPPEN_UEBERSICHT,
                SheetNamen.maastrichterGruppenUebersicht());
    }

    @Override
    public final TurnierSheet getTurnierSheet() throws GenerateException {
        return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
    }

    @Override
    protected void doRun() throws GenerateException {
        generate();
    }

    public void generate() throws GenerateException {
        NewSheet.from(this, SheetNamen.maastrichterGruppenUebersicht(),
                SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_GRUPPEN_UEBERSICHT)
                .tabColor(konfigurationSheet.getTeilnehmerTabFarbe()).pos(DefaultSheetPos.MAASTRICHTER_WORK)
                .forceCreate().hideGrid().setActiv().create();
        befuelleGruppenUebersicht();
    }

    private record GruppenEintrag(int nr, String name, String gruppe) {
    }

    private void befuelleGruppenUebersicht() throws GenerateException {
        processBoxinfo("processbox.maastrichter.gruppen.uebersicht.einlesen");
        TeamMeldungen aktiveMeldungen = meldeliste.getAktiveMeldungen();
        boolean teamnameAktiv = konfigurationSheet.isMeldeListeTeamnameAnzeigen();

        List<GruppenEintrag> eintraege = new ArrayList<>(aktiveMeldungen.size());
        if (aktiveMeldungen.size() > 0) {
            TeilnehmerNamen namen = TeilnehmerNamenLeser.from(meldeliste, MELDELISTE_ERSTE_DATEN_ZEILE,
                    konfigurationSheet.getMeldeListeFormation(), teamnameAktiv,
                    konfigurationSheet.isMeldeListeVereinsnameAnzeigen()).lesen();
            Map<Integer, String> spielerNamen = namen.spielerNamen();
            Map<Integer, String> teamnamen = namen.teamnamen();
            Map<Integer, String> teamNrZuGruppe = MaastrichterGruppenSpalteHelper
                    .lesePreservedGruppen(new MaastrichterVorrundenRanglisteSheetUpdate(getWorkingSpreadsheet()));

            for (Team team : aktiveMeldungen.getTeamList()) {
                int nr = team.getNr();
                String name = teamnameAktiv ? teamnamen.getOrDefault(nr, "") : spielerNamen.getOrDefault(nr, "");
                eintraege.add(new GruppenEintrag(nr, name, teamNrZuGruppe.getOrDefault(nr, "")));
            }
        }

        processBoxinfo("processbox.maastrichter.gruppen.uebersicht.einfuegen", eintraege.size());

        XSpreadsheet sheet = getXSpreadSheet();
        headerSchreiben(sheet);
        if (eintraege.isEmpty()) {
            return;
        }
        datenSchreiben(sheet, eintraege);
        int letzteZeile = ERSTE_DATEN_ZEILE + eintraege.size() - 1;
        getSheetHelper().setOptimaleBreiteUndHoeheAlles(sheet, HEADER_ZEILE, letzteZeile, SPALTE_NR, SPALTE_NAME);
        getSheetHelper().setColumnWidth(sheet, SPALTE_GRUPPE, SPALTE_GRUPPE_BREITE);
    }

    private void headerSchreiben(XSpreadsheet sheet) throws GenerateException {
        var headerFarbe = konfigurationSheet.getMeldeListeHeaderFarbe();
        var headerBorder = BorderFactory.from().allThin().boldLn().forBottom().toBorder();
        schreibeHeaderZelle(sheet, SPALTE_NR, "column.header.nr", headerFarbe, headerBorder);
        schreibeHeaderZelle(sheet, SPALTE_NAME, "column.header.name", headerFarbe, headerBorder);
        schreibeHeaderZelle(sheet, SPALTE_GRUPPE, "column.header.gruppe.kurz", headerFarbe, headerBorder);
    }

    private void schreibeHeaderZelle(XSpreadsheet sheet, int spalte, String i18nKey, Integer headerFarbe,
            TableBorder2 headerBorder) throws GenerateException {
        getSheetHelper().setStringValueInCell(StringCellValue
                .from(sheet, Position.from(spalte, HEADER_ZEILE), I18n.get(i18nKey))
                .setBorder(headerBorder)
                .setCellBackColor(headerFarbe)
                .setCharWeight(FontWeight.BOLD)
                .centerJustify());
    }

    private void datenSchreiben(XSpreadsheet sheet, List<GruppenEintrag> eintraege) throws GenerateException {
        RangeData rangeData = new RangeData();
        for (GruppenEintrag eintrag : eintraege) {
            RowData row = rangeData.addNewRow();
            row.newInt(eintrag.nr());
            row.newString(eintrag.name());
            row.newString(eintrag.gruppe());
        }
        RangeHelper.from(this, rangeData.getRangePosition(Position.from(SPALTE_NR, ERSTE_DATEN_ZEILE)))
                .setDataInRange(rangeData);

        int letzteZeile = ERSTE_DATEN_ZEILE + eintraege.size() - 1;
        getSheetHelper().setPropertiesInRange(sheet,
                RangePosition.from(SPALTE_NR, ERSTE_DATEN_ZEILE, SPALTE_GRUPPE, letzteZeile),
                CellProperties.from().setAllThinBorder());
        SheetHelper.faerbeZeilenAbwechselnd(this,
                RangePosition.from(SPALTE_NR, ERSTE_DATEN_ZEILE, SPALTE_GRUPPE, letzteZeile),
                konfigurationSheet.getMeldeListeHintergrundFarbeGerade(),
                konfigurationSheet.getMeldeListeHintergrundFarbeUnGerade());

        gruppenFarbenSetzen(sheet, eintraege);
    }

    /**
     * Färbt die Schrift des Gruppenbuchstabens je Zeilenblock passend zur Buchstabe→Farbe-Zuordnung
     * (A=Grün, B=Rot, …). Der Zebra-Hintergrund bleibt unangetastet. Zusammenhängende Zeilen
     * derselben Gruppe werden in einem Aufruf eingefärbt.
     */
    private void gruppenFarbenSetzen(XSpreadsheet sheet, List<GruppenEintrag> eintraege) throws GenerateException {
        int blockStart = 0;
        for (int i = 1; i <= eintraege.size(); i++) {
            boolean blockEndeErreicht = i == eintraege.size()
                    || !eintraege.get(i).gruppe().equals(eintraege.get(blockStart).gruppe());
            if (blockEndeErreicht) {
                String gruppe = eintraege.get(blockStart).gruppe();
                if (!gruppe.isEmpty()) {
                    getSheetHelper().setPropertiesInRange(sheet,
                            RangePosition.from(SPALTE_GRUPPE, ERSTE_DATEN_ZEILE + blockStart,
                                    SPALTE_GRUPPE, ERSTE_DATEN_ZEILE + i - 1),
                            CellProperties.from().setCharColor(gruppenBuchstabeFarbe(gruppe)).centerJustify()
                                    .setCharWeight(FontWeight.BOLD));
                }
                blockStart = i;
            }
        }
    }

    /** Ermittelt die feste Palettenfarbe für einen Gruppenbuchstaben (A=Grün, B=Rot, C=Blau, …). */
    private Integer gruppenBuchstabeFarbe(String gruppe) {
        int index = Character.toUpperCase(gruppe.charAt(0)) - 'A';
        return GRUPPEN_FARBEN[Math.floorMod(index, GRUPPEN_FARBEN.length)];
    }
}
