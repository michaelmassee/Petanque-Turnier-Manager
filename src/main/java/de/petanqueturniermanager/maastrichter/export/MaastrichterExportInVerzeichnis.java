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
import de.petanqueturniermanager.helper.rangliste.SignaturQuellen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheetsync.EingabeSignatur;
import de.petanqueturniermanager.helper.upload.AbstractExportInVerzeichnis;
import de.petanqueturniermanager.helper.upload.ExportErgebnis;
import de.petanqueturniermanager.helper.upload.ExportHtmlSeite;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterVorrundenRanglisteSheetUpdate;

public class MaastrichterExportInVerzeichnis extends AbstractExportInVerzeichnis {

    public MaastrichterExportInVerzeichnis(WorkingSpreadsheet ws, Path zielVerzeichnis) {
        super(ws, TurnierSystem.MAASTRICHTER, "Maastrichter Export Verzeichnis", zielVerzeichnis);
    }

    @Override
    protected ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException {
        processBox().info(I18n.get("export.info.pdf"));

        var ws = getWorkingSpreadsheet();
        var konfiguration = new MaastrichterKonfigurationSheet(ws);
        aktualisiereExportSheetWennDirty(SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX,
                new EingabeSignatur(SignaturQuellen::fuerMaastrichter),
                () -> new MaastrichterVorrundenRanglisteSheetUpdate(ws).doRun());

        List<Path> exportierteDateien = new ArrayList<>();
        List<ExportHtmlSeite.Section> sections = new ArrayList<>();

        String meldelisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_MELDELISTE, SheetNamen.meldeliste());
        boolean meldelisteExportieren = konfiguration.isMeldelisteExportieren();
        if (meldelisteExportieren) {
            sections.add(new ExportHtmlSeite.Section("meldeliste", I18n.get("export.nav.meldeliste"), meldelisteSheetName, null));
        }

        String ranglisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX, SheetNamen.maastrichterVorrundenRangliste());
        Path pdfRangliste = exportierePdfAusHtml(ranglisteSheetName, I18n.get("export.maastrichter.nav.vorrunden.rangliste"), zielVerzeichnis);
        if (pdfRangliste != null) {
            exportierteDateien.add(pdfRangliste);
        }
        sections.add(new ExportHtmlSeite.Section("vorrunden-rangliste",
                I18n.get("export.maastrichter.nav.vorrunden.rangliste"), ranglisteSheetName,
                buildPdfUrl(pdfRangliste)));

        for (var eintrag : buchstabenSheetEintraegePerSchluessel(
                SheetMetadataHelper::schluesselMaastrichterFinalrunde, SheetNamen::koFinaleGruppe)) {
            var finalTitel = I18n.get("export.maastrichter.nav.finalrunde", eintrag.buchstabe());
            Path pdf = exportierePdfAusHtml(eintrag.sheetName(), finalTitel, zielVerzeichnis);
            if (pdf != null) {
                exportierteDateien.add(pdf);
            }
            sections.add(new ExportHtmlSeite.Section("finalrunde-" + eintrag.buchstabe(),
                    finalTitel, eintrag.sheetName(), buildPdfUrl(pdf)));
        }

        processBox().info(I18n.get("export.info.html"));
        exportiereHtmlMitMeldelisteDruckbereich(meldelisteExportieren, meldelisteSheetName,
                zielVerzeichnis, "Maastrichter.html",
                StringUtils.defaultIfBlank(StringUtils.strip(konfiguration.getKopfZeileMitte()),
                        TurnierSystem.MAASTRICHTER.getBezeichnung()),
                StringUtils.strip(konfiguration.getTurnierlogoUrl()), sections)
                .addTo(exportierteDateien);

        return new ExportErgebnis(exportierteDateien);
    }
}
