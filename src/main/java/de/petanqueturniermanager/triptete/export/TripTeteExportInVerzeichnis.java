/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.triptete.export;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.io.PdfExport;
import de.petanqueturniermanager.helper.upload.AbstractExportInVerzeichnis;
import de.petanqueturniermanager.helper.upload.ExportErgebnis;
import de.petanqueturniermanager.helper.upload.ExportHtmlSeite;
import de.petanqueturniermanager.triptete.konfiguration.TripTeteKonfigurationSheet;
import de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet;

public class TripTeteExportInVerzeichnis extends AbstractExportInVerzeichnis {

    public TripTeteExportInVerzeichnis(WorkingSpreadsheet ws, Path zielVerzeichnis) {
        super(ws, TurnierSystem.TRIPTETE, "TripTete Export Verzeichnis", zielVerzeichnis);
    }

    @Override
    protected ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException {
        processBox().info(I18n.get("export.info.pdf"));

        var ws = getWorkingSpreadsheet();
        var konfiguration = new TripTeteKonfigurationSheet(ws);
        var spielplan = new TripTeteSpielPlanSheet(ws);

        List<Path> exportierteDateien = new ArrayList<>();

        String meldelisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_TRIPTETE_MELDELISTE, SheetNamen.meldeliste());
        String spielplanSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_TRIPTETE_SPIELPLAN, TripTeteSpielPlanSheet.sheetName());
        String ranglisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_TRIPTETE_RANGLISTE, SheetNamen.rangliste());

        Path pdfSpielplan = exportierePdfWennTabelleVorhanden(spielplanSheetName,
                () -> PdfExport.from(ws)
                        .sheetName(spielplanSheetName)
                        .range(spielplan.printBereichRangePosition())
                        .prefix1(spielplanSheetName)
                        .zielVerzeichnis(zielVerzeichnis)
                        .doExport());
        if (pdfSpielplan != null) {
            exportierteDateien.add(pdfSpielplan);
        }

        Path pdfRangliste = exportierePdfWennTabelleVorhanden(ranglisteSheetName, zielVerzeichnis);
        if (pdfRangliste != null) {
            exportierteDateien.add(pdfRangliste);
        }

        processBox().info(I18n.get("export.info.html"));
        List<ExportHtmlSeite.Section> sections = List.of(
                new ExportHtmlSeite.Section("meldeliste", I18n.get("export.nav.meldeliste"), meldelisteSheetName, null),
                new ExportHtmlSeite.Section("spielplan", I18n.get("export.nav.spielplan"), spielplanSheetName,
                        buildPdfUrl(null, pdfSpielplan)),
                new ExportHtmlSeite.Section("rangliste", I18n.get("export.nav.rangliste"), ranglisteSheetName,
                        buildPdfUrl(null, pdfRangliste)));
        exportierteDateien.add(exportiereHtml(zielVerzeichnis, "TripTete.html",
                StringUtils.defaultIfBlank(StringUtils.strip(konfiguration.getKopfZeileMitte()),
                        TurnierSystem.TRIPTETE.getBezeichnung()),
                StringUtils.strip(konfiguration.getTurnierlogoUrl()), sections));

        return new ExportErgebnis(exportierteDateien);
    }
}
