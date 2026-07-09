/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.export;

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
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;
import de.petanqueturniermanager.poule.rangliste.PouleVorrundenRanglisteSheetUpdate;

public class PouleExportInVerzeichnis extends AbstractExportInVerzeichnis {

    public PouleExportInVerzeichnis(WorkingSpreadsheet ws, Path zielVerzeichnis, ExportFormat format) {
        super(ws, TurnierSystem.POULE, "Poule Export Verzeichnis", zielVerzeichnis, format);
    }

    @Override
    protected ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException {
        var ws = getWorkingSpreadsheet();
        var konfiguration = new PouleKonfigurationSheet(ws);
        aktualisiereExportSheetWennDirty(SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDEN_RANGLISTE,
                new EingabeSignatur(SignaturQuellen::fuerPoule),
                () -> new PouleVorrundenRanglisteSheetUpdate(ws).doRun());

        String meldelisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_POULE_MELDELISTE, SheetNamen.meldeliste());
        String ranglisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDEN_RANGLISTE, SheetNamen.pouleVorrundenRangliste());
        boolean meldelisteExportieren = konfiguration.isMeldelisteExportieren();
        String titel = StringUtils.defaultIfBlank(StringUtils.strip(konfiguration.getKopfZeileMitte()),
                TurnierSystem.POULE.getBezeichnung());

        if (getFormat().istEinDokument()) {
            List<ExportHtmlSeite.Section> sections = sections(meldelisteSheetName, ranglisteSheetName, null,
                    meldelisteExportieren);
            processBox().info(I18n.get("export.info.ein.dokument", getFormat().anzeigeName()));
            Path dokument = exportiereEinDokument(zielVerzeichnis, "Poule", titel, getFormat(), sections);
            List<Path> exportierteDateien = new ArrayList<>();
            if (dokument != null) {
                exportierteDateien.add(dokument);
            }
            return new ExportErgebnis(exportierteDateien);
        }

        processBox().info(I18n.get("export.info.pdf"));
        List<Path> exportierteDateien = new ArrayList<>();

        Path pdfRangliste = exportierePdfAusHtml(ranglisteSheetName, I18n.get("export.nav.poule.vorrunden.rangliste"), zielVerzeichnis);
        if (pdfRangliste != null) {
            exportierteDateien.add(pdfRangliste);
        }

        processBox().info(I18n.get("export.info.html"));
        List<ExportHtmlSeite.Section> sections = sections(meldelisteSheetName, ranglisteSheetName,
                buildPdfUrl(pdfRangliste), meldelisteExportieren);
        exportiereHtmlMitMeldelisteDruckbereich(meldelisteExportieren, meldelisteSheetName,
                zielVerzeichnis, "Poule.html", titel,
                StringUtils.strip(konfiguration.getTurnierlogoUrl()), sections)
                .addTo(exportierteDateien);

        return new ExportErgebnis(exportierteDateien);
    }

    private static List<ExportHtmlSeite.Section> sections(String meldelisteSheetName, String ranglisteSheetName,
            String ranglistePdfUrl, boolean meldelisteExportieren) {
        List<ExportHtmlSeite.Section> sections = new ArrayList<>();
        if (meldelisteExportieren) {
            sections.add(new ExportHtmlSeite.Section("meldeliste", I18n.get("export.nav.meldeliste"), meldelisteSheetName, null));
        }
        sections.add(new ExportHtmlSeite.Section("rangliste", I18n.get("export.nav.poule.vorrunden.rangliste"), ranglisteSheetName,
                ranglistePdfUrl));
        return sections;
    }
}
