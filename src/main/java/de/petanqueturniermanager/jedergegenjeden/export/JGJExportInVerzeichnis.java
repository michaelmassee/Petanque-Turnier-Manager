/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.jedergegenjeden.export;

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
import de.petanqueturniermanager.helper.upload.AbstractExportInVerzeichnis;
import de.petanqueturniermanager.helper.upload.ExportErgebnis;
import de.petanqueturniermanager.helper.upload.ExportHtmlSeite;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet;

public class JGJExportInVerzeichnis extends AbstractExportInVerzeichnis {

    public JGJExportInVerzeichnis(WorkingSpreadsheet ws, Path zielVerzeichnis) {
        super(ws, TurnierSystem.JGJ, "JGJ Export Verzeichnis", zielVerzeichnis);
    }

    @Override
    protected ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException {
        processBox().info(I18n.get("export.info.pdf"));

        var ws = getWorkingSpreadsheet();
        var konfiguration = new JGJKonfigurationSheet(ws);

        List<Path> exportierteDateien = new ArrayList<>();

        String meldelisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_JGJ_MELDELISTE, SheetNamen.meldeliste());
        String spielplanSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_JGJ_SPIELPLAN, JGJSpielPlanSheet.sheetName());
        String ranglisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_JGJ_RANGLISTE, SheetNamen.rangliste());
        String direktvergleichSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_JGJ_DIREKTVERGLEICH, SheetNamen.direktvergleich());

        Path pdfRangliste = exportierePdfWennTabelleVorhanden(ranglisteSheetName, zielVerzeichnis);
        if (pdfRangliste != null) {
            exportierteDateien.add(pdfRangliste);
        }

        Path pdfDirektvergleich = exportierePdfWennTabelleVorhanden(direktvergleichSheetName, zielVerzeichnis);
        if (pdfDirektvergleich != null) {
            exportierteDateien.add(pdfDirektvergleich);
        }

        processBox().info(I18n.get("export.info.html"));
        List<ExportHtmlSeite.Section> sections = new ArrayList<>();
        if (konfiguration.isMeldelisteExportieren()) {
            sections.add(new ExportHtmlSeite.Section("meldeliste", I18n.get("export.nav.meldeliste"), meldelisteSheetName, null));
        }
        sections.add(new ExportHtmlSeite.Section("spielplan", I18n.get("export.nav.spielplan"), spielplanSheetName, null));
        sections.add(new ExportHtmlSeite.Section("rangliste", I18n.get("export.nav.rangliste"), ranglisteSheetName,
                buildPdfUrl(pdfRangliste)));
        sections.add(new ExportHtmlSeite.Section("direktvergleich", I18n.get("export.nav.direktvergleich"), direktvergleichSheetName,
                buildPdfUrl(pdfDirektvergleich)));
        exportierteDateien.add(exportiereHtml(zielVerzeichnis, "JederGegenJeden.html",
                StringUtils.defaultIfBlank(StringUtils.strip(konfiguration.getKopfZeileMitte()),
                        TurnierSystem.JGJ.getBezeichnung()),
                StringUtils.strip(konfiguration.getTurnierlogoUrl()), sections));

        return new ExportErgebnis(exportierteDateien);
    }
}
