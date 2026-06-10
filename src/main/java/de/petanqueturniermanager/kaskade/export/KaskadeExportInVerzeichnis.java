/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.kaskade.export;

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
import de.petanqueturniermanager.kaskade.konfiguration.KaskadeKonfigurationSheet;

public class KaskadeExportInVerzeichnis extends AbstractExportInVerzeichnis {

    public KaskadeExportInVerzeichnis(WorkingSpreadsheet ws, Path zielVerzeichnis) {
        super(ws, TurnierSystem.KASKADE, "Kaskade Export Verzeichnis", zielVerzeichnis);
    }

    @Override
    protected ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException {
        processBox().info(I18n.get("export.info.pdf"));

        var ws = getWorkingSpreadsheet();
        var konfiguration = new KaskadeKonfigurationSheet(ws);

        List<Path> exportierteDateien = new ArrayList<>();
        List<ExportHtmlSeite.Section> sections = new ArrayList<>();

        String meldelisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_KASKADE_MELDELISTE, SheetNamen.meldeliste());
        sections.add(new ExportHtmlSeite.Section("meldeliste", I18n.get("export.nav.meldeliste"), meldelisteSheetName, null));

        String ranglisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_KASKADE_GRUPPENRANGLISTE, SheetNamen.kaskadeGruppenrangliste());
        Path pdfRangliste = exportierePdfWennTabelleVorhanden(ranglisteSheetName, zielVerzeichnis);
        if (pdfRangliste != null) {
            exportierteDateien.add(pdfRangliste);
        }
        sections.add(new ExportHtmlSeite.Section("gruppenrangliste",
                I18n.get("export.kaskade.nav.gruppenrangliste"), ranglisteSheetName,
                buildPdfUrl(pdfRangliste)));

        for (var eintrag : buchstabenSheetEintraegePerSchluessel(
                SheetMetadataHelper::schluesselKaskadenFeld, SheetNamen::kaskadenFeld)) {
            Path pdf = exportierePdfWennTabelleVorhanden(eintrag.sheetName(), zielVerzeichnis);
            if (pdf != null) {
                exportierteDateien.add(pdf);
            }
            sections.add(new ExportHtmlSeite.Section("feld-" + eintrag.buchstabe(),
                    I18n.get("export.kaskade.nav.feld", eintrag.buchstabe()),
                    eintrag.sheetName(), buildPdfUrl(pdf)));
        }

        processBox().info(I18n.get("export.info.html"));
        exportierteDateien.add(exportiereHtml(zielVerzeichnis, "KaskadenKO.html",
                StringUtils.defaultIfBlank(StringUtils.strip(konfiguration.getKopfZeileMitte()),
                        TurnierSystem.KASKADE.getBezeichnung()),
                StringUtils.strip(konfiguration.getTurnierlogoUrl()), sections));

        return new ExportErgebnis(exportierteDateien);
    }
}
