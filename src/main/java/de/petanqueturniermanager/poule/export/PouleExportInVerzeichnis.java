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
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.upload.AbstractExportInVerzeichnis;
import de.petanqueturniermanager.helper.upload.ExportErgebnis;
import de.petanqueturniermanager.helper.upload.ExportHtmlSeite;
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;

public class PouleExportInVerzeichnis extends AbstractExportInVerzeichnis {

    public PouleExportInVerzeichnis(WorkingSpreadsheet ws, Path zielVerzeichnis) {
        super(ws, TurnierSystem.POULE, "Poule Export Verzeichnis", zielVerzeichnis);
    }

    @Override
    protected ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException {
        processBox().info(I18n.get("export.info.pdf"));

        var ws = getWorkingSpreadsheet();
        var konfiguration = new PouleKonfigurationSheet(ws);

        String baseDownloadUrl = StringUtils.strip(konfiguration.getDownloadUrl());

        if (StringUtils.isNotEmpty(baseDownloadUrl)) {
            processBox().info(I18n.get("export.info.download.url", baseDownloadUrl));
        }

        List<Path> exportierteDateien = new ArrayList<>();

        String meldelisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_POULE_MELDELISTE, SheetNamen.meldeliste());
        String ranglisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDEN_RANGLISTE, SheetNamen.pouleVorrundenRangliste());

        Path pdfRangliste = exportierePdfWennTabelleVorhanden(ranglisteSheetName, zielVerzeichnis);
        if (pdfRangliste != null) {
            exportierteDateien.add(pdfRangliste);
        }

        processBox().info(I18n.get("export.info.html"));
        List<ExportHtmlSeite.Section> sections = List.of(
                new ExportHtmlSeite.Section("meldeliste", I18n.get("export.nav.meldeliste"), meldelisteSheetName, null),
                new ExportHtmlSeite.Section("rangliste", I18n.get("export.nav.poule.vorrunden.rangliste"), ranglisteSheetName,
                        buildPdfUrl(baseDownloadUrl, pdfRangliste)));
        exportierteDateien.add(exportiereHtml(zielVerzeichnis, "Poule.html",
                StringUtils.defaultIfBlank(StringUtils.strip(konfiguration.getKopfZeileMitte()),
                        TurnierSystem.POULE.getBezeichnung()),
                StringUtils.strip(konfiguration.getTurnierlogoUrl()), sections));

        return new ExportErgebnis(exportierteDateien);
    }
}
