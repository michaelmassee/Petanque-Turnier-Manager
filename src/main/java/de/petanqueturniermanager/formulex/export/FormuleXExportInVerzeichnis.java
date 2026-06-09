/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.export;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.upload.AbstractExportInVerzeichnis;
import de.petanqueturniermanager.helper.upload.ExportErgebnis;
import de.petanqueturniermanager.helper.upload.ExportHtmlSeite;

public class FormuleXExportInVerzeichnis extends AbstractExportInVerzeichnis {

    public FormuleXExportInVerzeichnis(WorkingSpreadsheet ws, Path zielVerzeichnis) {
        super(ws, TurnierSystem.FORMULEX, "FormuleX Export Verzeichnis", zielVerzeichnis);
    }

    @Override
    protected ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException {
        processBox().info(I18n.get("export.info.pdf"));

        var ws = getWorkingSpreadsheet();
        var konfiguration = new FormuleXKonfigurationSheet(ws);

        String baseDownloadUrl = StringUtils.strip(konfiguration.getDownloadUrl());

        if (StringUtils.isNotEmpty(baseDownloadUrl)) {
            processBox().info(I18n.get("export.info.download.url", baseDownloadUrl));
        }

        List<Path> exportierteDateien = new ArrayList<>();

        String ranglisteSheetName = SheetNamen.formulexRangliste();
        Path pdfRangliste = exportierePdfWennTabelleVorhanden(ranglisteSheetName, zielVerzeichnis);
        if (pdfRangliste != null) {
            exportierteDateien.add(pdfRangliste);
        }

        processBox().info(I18n.get("export.info.html"));
        List<ExportHtmlSeite.Section> sections = List.of(
                new ExportHtmlSeite.Section("meldeliste", SheetNamen.meldeliste(), SheetNamen.meldeliste(), null),
                new ExportHtmlSeite.Section("rangliste", ranglisteSheetName, ranglisteSheetName,
                        buildPdfUrl(baseDownloadUrl, pdfRangliste)));
        exportierteDateien.add(exportiereHtml(zielVerzeichnis, "FormuleX.html",
                StringUtils.defaultIfBlank(StringUtils.strip(konfiguration.getKopfZeileMitte()),
                        TurnierSystem.FORMULEX.getBezeichnung()),
                StringUtils.strip(konfiguration.getTurnierlogoUrl()), sections));

        return new ExportErgebnis(exportierteDateien);
    }
}
