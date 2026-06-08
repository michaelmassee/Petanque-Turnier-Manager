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
import de.petanqueturniermanager.helper.sheet.io.PdfExport;
import de.petanqueturniermanager.helper.upload.AbstractExportInVerzeichnis;
import de.petanqueturniermanager.helper.upload.ExportErgebnis;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;

public class JGJExportInVerzeichnis extends AbstractExportInVerzeichnis {

    public JGJExportInVerzeichnis(WorkingSpreadsheet ws, Path zielVerzeichnis) {
        super(ws, TurnierSystem.JGJ, "JGJ Export Verzeichnis", zielVerzeichnis);
    }

    @Override
    protected ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException {
        processBox().info(I18n.get("export.info.pdf"));

        var ws = getWorkingSpreadsheet();
        var konfiguration = new JGJKonfigurationSheet(ws);

        String baseDownloadUrl = StringUtils.strip(konfiguration.getDownloadUrl());

        if (StringUtils.isNotEmpty(baseDownloadUrl)) {
            processBox().info(I18n.get("export.info.download.url", baseDownloadUrl));
        }

        List<Path> exportierteDateien = new ArrayList<>();

        String ranglisteSheetName = SheetNamen.rangliste();
        Path pdfRangliste = Path.of(PdfExport.from(ws)
                .sheetName(ranglisteSheetName)
                .prefix1(ranglisteSheetName)
                .zielVerzeichnis(zielVerzeichnis)
                .doExport());
        processBox().info(pdfRangliste.toString());
        exportierteDateien.add(pdfRangliste);

        String direktvergleichSheetName = SheetNamen.direktvergleich();
        Path pdfDirektvergleich = Path.of(PdfExport.from(ws)
                .sheetName(direktvergleichSheetName)
                .prefix1(direktvergleichSheetName)
                .zielVerzeichnis(zielVerzeichnis)
                .doExport());
        processBox().info(pdfDirektvergleich.toString());
        exportierteDateien.add(pdfDirektvergleich);

        return new ExportErgebnis(exportierteDateien);
    }
}
