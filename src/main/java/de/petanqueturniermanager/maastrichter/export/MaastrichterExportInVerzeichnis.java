/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.export;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.upload.AbstractExportInVerzeichnis;
import de.petanqueturniermanager.helper.upload.ExportErgebnis;
import de.petanqueturniermanager.helper.upload.ExportHtmlSeite;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;

public class MaastrichterExportInVerzeichnis extends AbstractExportInVerzeichnis {

    public MaastrichterExportInVerzeichnis(WorkingSpreadsheet ws, Path zielVerzeichnis) {
        super(ws, TurnierSystem.MAASTRICHTER, "Maastrichter Export Verzeichnis", zielVerzeichnis);
    }

    @Override
    protected ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException {
        processBox().info(I18n.get("export.info.pdf"));

        var ws = getWorkingSpreadsheet();
        var konfiguration = new MaastrichterKonfigurationSheet(ws);

        String baseDownloadUrl = StringUtils.strip(konfiguration.getDownloadUrl());

        if (StringUtils.isNotEmpty(baseDownloadUrl)) {
            processBox().info(I18n.get("export.info.download.url", baseDownloadUrl));
        }

        List<Path> exportierteDateien = new ArrayList<>();
        List<ExportHtmlSeite.Section> sections = new ArrayList<>();
        sections.add(new ExportHtmlSeite.Section("meldeliste", SheetNamen.meldeliste(), SheetNamen.meldeliste(), null));

        String ranglisteSheetName = SheetNamen.maastrichterVorrundenRangliste();
        Path pdfRangliste = exportierePdfWennTabelleVorhanden(ranglisteSheetName, zielVerzeichnis);
        if (pdfRangliste != null) {
            exportierteDateien.add(pdfRangliste);
        }
        sections.add(new ExportHtmlSeite.Section("vorrunden-rangliste", ranglisteSheetName, ranglisteSheetName,
                buildPdfUrl(baseDownloadUrl, pdfRangliste)));

        for (String sheetName : vorhandeneBuchstabenSheets(SheetNamen::koFinaleGruppe)) {
            Path pdf = exportierePdfWennTabelleVorhanden(sheetName, zielVerzeichnis);
            if (pdf != null) {
                exportierteDateien.add(pdf);
            }
            sections.add(new ExportHtmlSeite.Section("ko-" + sheetName, sheetName, sheetName,
                    buildPdfUrl(baseDownloadUrl, pdf)));
        }

        processBox().info(I18n.get("export.info.html"));
        exportierteDateien.add(exportiereHtml(zielVerzeichnis, "Maastrichter.html",
                StringUtils.defaultIfBlank(StringUtils.strip(konfiguration.getKopfZeileMitte()),
                        TurnierSystem.MAASTRICHTER.getBezeichnung()),
                StringUtils.strip(konfiguration.getTurnierlogoUrl()), sections));

        return new ExportErgebnis(exportierteDateien);
    }
}
