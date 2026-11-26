/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ki;

import java.util.List;
import java.util.function.Predicate;

import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Legt die von der KI vorgeschlagenen Zusatzblätter an und füllt sie. Vom Plugin verwaltete Blätter (Meldeliste,
 * Spielrunden, Ranglisten …) werden dabei nie ersetzt: Heißt ein Vorschlag wie ein solches Blatt, bekommt das
 * KI-Blatt einen freien Ersatznamen. Bei einem anderen vorhandenen Blatt fragt {@link NewSheet} nach; lehnt der
 * Anwender ab, wird nichts geschrieben.
 */
final class KiCustomSheetsRunner extends SheetRunner implements ISheet {

    private final List<CustomSheetDaten> sheets;
    private XSpreadsheet aktuellesSheet;

    KiCustomSheetsRunner(WorkingSpreadsheet workingSpreadsheet, List<CustomSheetDaten> sheets) {
        super(workingSpreadsheet, TurnierSystem.KEIN, "KI Zusatz-Sheets");
        this.sheets = List.copyOf(sheets);
    }

    @Override
    protected void doRun() throws GenerateException {
        for (CustomSheetDaten sheetDaten : sheets) {
            SheetHelper sheetHelper = getSheetHelper();
            String name = zielName(sheetDaten.name(), kandidat -> istVomPluginVerwaltet(sheetHelper, kandidat),
                    kandidat -> sheetHelper.findByName(kandidat) != null);
            if (!name.equals(sheetDaten.name())) {
                getLogger().info("KI: Blatt '{}' wird vom Plugin verwaltet, KI-Daten gehen nach '{}'",
                        sheetDaten.name(), name);
                processBox().info(I18n.get("ki.zusatzblatt.umbenannt", sheetDaten.name(), name));
            }
            NewSheet neuesSheet = NewSheet.from(this, name, "KI_" + name)
                    .newIfExist()
                    .showGrid()
                    .setActiv()
                    .create();
            // Nur in ein gerade neu angelegtes Blatt schreiben: Lehnt der Anwender die Neuerstellung eines
            // vorhandenen Blatts ab (z. B. „Meldeliste“), bleibt dessen Inhalt unangetastet.
            if (!neuesSheet.isDidCreate()) {
                getLogger().info("KI: Blatt '{}' nicht neu erstellt, KI-Daten werden nicht geschrieben", name);
                continue;
            }
            aktuellesSheet = neuesSheet.getSheet();
            schreibe(sheetDaten);
        }
    }

    /**
     * Name des KI-Blatts: der Vorschlag, außer ein vom Plugin verwaltetes Blatt heißt so – dann der erste freie
     * Ersatzname „Vorschlag (KI)“, „Vorschlag (KI 2)“, …
     *
     * @param istVerwaltet ob das Blatt dieses Namens vom Plugin verwaltet wird
     * @param existiert    ob es ein Blatt dieses Namens gibt
     */
    static String zielName(String vorschlag, Predicate<String> istVerwaltet, Predicate<String> existiert) {
        if (!istVerwaltet.test(vorschlag)) {
            return vorschlag;
        }
        String kandidat = I18n.get("ki.zusatzblatt.ersatzname", vorschlag);
        for (int nummer = 2; existiert.test(kandidat); nummer++) {
            kandidat = I18n.get("ki.zusatzblatt.ersatzname.nummer", vorschlag, nummer);
        }
        return kandidat;
    }

    /** Vom Plugin verwaltet: Das Blatt trägt einen PTM-Metadaten-Schlüssel (überlebt Umbenennungen). */
    private boolean istVomPluginVerwaltet(SheetHelper sheetHelper, String name) {
        XSpreadsheet vorhanden = sheetHelper.findByName(name);
        return vorhanden != null && SheetMetadataHelper.hatPraefixSchluessel(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), vorhanden, SheetMetadataHelper.PRAEFIX_PTM);
    }

    @Override
    protected IKonfigurationSheet getKonfigurationSheet() {
        return null;
    }

    private void schreibe(CustomSheetDaten sheetDaten) throws GenerateException {
        RangeData rangeData = new RangeData();
        for (List<String> row : sheetDaten.rows()) {
            RowData rowData = new RowData();
            for (String value : row) {
                rowData.newString(value);
            }
            rangeData.add(rowData);
        }
        if (rangeData.isEmpty()) {
            return;
        }
        Position startPos = Position.from(0, 0);
        RangeHelper.from(this, rangeData.getRangePosition(startPos)).setDataInRange(rangeData);
    }

    @Override
    public XSpreadsheet getXSpreadSheet() {
        return aktuellesSheet;
    }

    @Override
    public Logger getLogger() {
        return super.getLogger();
    }

    @Override
    public TurnierSheet getTurnierSheet() throws GenerateException {
        return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
    }
}
