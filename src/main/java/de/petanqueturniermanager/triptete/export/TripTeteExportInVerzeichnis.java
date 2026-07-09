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
import de.petanqueturniermanager.helper.rangliste.SignaturQuellen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheetsync.EingabeSignatur;
import de.petanqueturniermanager.helper.upload.AbstractExportInVerzeichnis;
import de.petanqueturniermanager.helper.upload.ExportErgebnis;
import de.petanqueturniermanager.helper.upload.ExportFormat;
import de.petanqueturniermanager.helper.upload.ExportHtmlSeite;
import de.petanqueturniermanager.triptete.konfiguration.TripTeteKonfigurationSheet;
import de.petanqueturniermanager.triptete.rangliste.TripTeteRanglisteSheet;
import de.petanqueturniermanager.triptete.rangliste.TripTeteRanglisteSheetUpdate;
import de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet;

public class TripTeteExportInVerzeichnis extends AbstractExportInVerzeichnis {

    public TripTeteExportInVerzeichnis(WorkingSpreadsheet ws, Path zielVerzeichnis, ExportFormat format) {
        super(ws, TurnierSystem.TRIPTETE, "TripTete Export Verzeichnis", zielVerzeichnis, format);
    }

    @Override
    protected ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException {
        var ws = getWorkingSpreadsheet();
        var konfiguration = new TripTeteKonfigurationSheet(ws);
        boolean ranglisteFehlt = exportSheetFehlt(SheetMetadataHelper.SCHLUESSEL_TRIPTETE_RANGLISTE);
        aktualisiereExportSheetWennDirty(SheetMetadataHelper.SCHLUESSEL_TRIPTETE_RANGLISTE,
                new EingabeSignatur(SignaturQuellen::fuerTripTete),
                ranglisteFehlt,
                () -> {
                    if (ranglisteFehlt) {
                        new TripTeteRanglisteSheet(ws).upDateSheet();
                    } else {
                        new TripTeteRanglisteSheetUpdate(ws).doRun();
                    }
                });

        String meldelisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_TRIPTETE_MELDELISTE, SheetNamen.meldeliste());
        String spielplanSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_TRIPTETE_SPIELPLAN, TripTeteSpielPlanSheet.sheetName());
        String ranglisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_TRIPTETE_RANGLISTE, SheetNamen.rangliste());
        boolean meldelisteExportieren = konfiguration.isMeldelisteExportieren();
        String titel = StringUtils.defaultIfBlank(StringUtils.strip(konfiguration.getKopfZeileMitte()),
                TurnierSystem.TRIPTETE.getBezeichnung());

        if (getFormat().istEinDokument()) {
            List<ExportHtmlSeite.Section> sections = sections(meldelisteSheetName, spielplanSheetName,
                    ranglisteSheetName, null, null, meldelisteExportieren);
            processBox().info(I18n.get("export.info.ein.dokument", getFormat().anzeigeName()));
            Path dokument = exportiereEinDokument(zielVerzeichnis, "TripTete", titel, getFormat(), sections);
            List<Path> exportierteDateien = new ArrayList<>();
            if (dokument != null) {
                exportierteDateien.add(dokument);
            }
            return new ExportErgebnis(exportierteDateien);
        }

        processBox().info(I18n.get("export.info.pdf"));
        List<Path> exportierteDateien = new ArrayList<>();

        Path pdfSpielplan = exportierePdfAusHtml(spielplanSheetName, I18n.get("export.nav.spielplan"), zielVerzeichnis);
        if (pdfSpielplan != null) {
            exportierteDateien.add(pdfSpielplan);
        }

        Path pdfRangliste = exportierePdfAusHtml(ranglisteSheetName, I18n.get("export.nav.rangliste"), zielVerzeichnis);
        if (pdfRangliste != null) {
            exportierteDateien.add(pdfRangliste);
        }

        processBox().info(I18n.get("export.info.html"));
        List<ExportHtmlSeite.Section> sections = sections(meldelisteSheetName, spielplanSheetName, ranglisteSheetName,
                buildPdfUrl(pdfSpielplan), buildPdfUrl(pdfRangliste), meldelisteExportieren);
        exportiereHtmlMitMeldelisteDruckbereich(meldelisteExportieren, meldelisteSheetName,
                zielVerzeichnis, "TripTete.html", titel,
                StringUtils.strip(konfiguration.getTurnierlogoUrl()), sections)
                .addTo(exportierteDateien);

        return new ExportErgebnis(exportierteDateien);
    }

    private static List<ExportHtmlSeite.Section> sections(String meldelisteSheetName, String spielplanSheetName,
            String ranglisteSheetName, String spielplanPdfUrl, String ranglistePdfUrl, boolean meldelisteExportieren) {
        List<ExportHtmlSeite.Section> sections = new ArrayList<>();
        if (meldelisteExportieren) {
            sections.add(new ExportHtmlSeite.Section("meldeliste", I18n.get("export.nav.meldeliste"), meldelisteSheetName, null));
        }
        sections.add(new ExportHtmlSeite.Section("spielplan", I18n.get("export.nav.spielplan"), spielplanSheetName,
                spielplanPdfUrl));
        sections.add(new ExportHtmlSeite.Section("rangliste", I18n.get("export.nav.rangliste"), ranglisteSheetName,
                ranglistePdfUrl));
        return sections;
    }
}
