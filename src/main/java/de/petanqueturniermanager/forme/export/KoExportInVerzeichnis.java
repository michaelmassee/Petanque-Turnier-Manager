/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.forme.export;

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
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;

public class KoExportInVerzeichnis extends AbstractExportInVerzeichnis {

    public KoExportInVerzeichnis(WorkingSpreadsheet ws, Path zielVerzeichnis) {
        super(ws, TurnierSystem.KO, "Ko Export Verzeichnis", zielVerzeichnis);
    }

    @Override
    protected ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException {
        processBox().info(I18n.get("export.info.pdf"));

        var ws = getWorkingSpreadsheet();
        var konfiguration = new KoKonfigurationSheet(ws);

        String baseDownloadUrl = StringUtils.strip(konfiguration.getDownloadUrl());

        if (StringUtils.isNotEmpty(baseDownloadUrl)) {
            processBox().info(I18n.get("export.info.download.url", baseDownloadUrl));
        }

        List<Path> exportierteDateien = new ArrayList<>();
        List<ExportHtmlSeite.Section> sections = new ArrayList<>();
        sections.add(new ExportHtmlSeite.Section("meldeliste", SheetNamen.meldeliste(), SheetNamen.meldeliste(), null));

        List<String> baumSheets = new ArrayList<>();
        String einzelBaum = SheetNamen.koTurnierbaumEinzel();
        if (getSheetHelper().findByName(einzelBaum) != null) {
            baumSheets.add(einzelBaum);
        }
        baumSheets.addAll(vorhandeneBuchstabenSheets(SheetNamen::koTurnierbaumGruppe));

        for (String sheetName : baumSheets) {
            Path pdf = exportierePdfWennTabelleVorhanden(sheetName, zielVerzeichnis);
            if (pdf != null) {
                exportierteDateien.add(pdf);
            }
            sections.add(new ExportHtmlSeite.Section("ko-" + sheetName, sheetName, sheetName,
                    buildPdfUrl(baseDownloadUrl, pdf)));
        }

        if (baumSheets.isEmpty()) {
            sections.add(new ExportHtmlSeite.Section("ko-turnierbaum", einzelBaum, einzelBaum, null));
        }

        processBox().info(I18n.get("export.info.html"));
        exportierteDateien.add(exportiereHtml(zielVerzeichnis, "KO.html",
                StringUtils.defaultIfBlank(StringUtils.strip(konfiguration.getKopfZeileMitte()),
                        TurnierSystem.KO.getBezeichnung()),
                StringUtils.strip(konfiguration.getTurnierlogoUrl()), sections));

        return new ExportErgebnis(exportierteDateien);
    }
}
