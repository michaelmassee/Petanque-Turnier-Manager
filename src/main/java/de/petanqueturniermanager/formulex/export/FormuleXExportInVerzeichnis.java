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
import de.petanqueturniermanager.formulex.rangliste.FormuleXRanglisteSheetUpdate;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.rangliste.SignaturQuellen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheetsync.EingabeSignatur;
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
        aktualisiereExportSheetWennDirty(SheetMetadataHelper.SCHLUESSEL_FORMULEX_RANGLISTE,
                new EingabeSignatur(SignaturQuellen::fuerFormuleX),
                () -> new FormuleXRanglisteSheetUpdate(ws).doRun());

        List<Path> exportierteDateien = new ArrayList<>();

        String meldelisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_FORMULEX_MELDELISTE, SheetNamen.meldeliste());
        String ranglisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_FORMULEX_RANGLISTE, SheetNamen.formulexRangliste());

        Path pdfRangliste = exportierePdfAusHtml(ranglisteSheetName, I18n.get("export.nav.formulex.rangliste"), zielVerzeichnis);
        if (pdfRangliste != null) {
            exportierteDateien.add(pdfRangliste);
        }

        processBox().info(I18n.get("export.info.html"));
        boolean meldelisteExportieren = konfiguration.isMeldelisteExportieren();
        List<ExportHtmlSeite.Section> sections = new ArrayList<>();
        if (meldelisteExportieren) {
            sections.add(new ExportHtmlSeite.Section("meldeliste", I18n.get("export.nav.meldeliste"), meldelisteSheetName, null));
        }
        sections.add(new ExportHtmlSeite.Section("rangliste", I18n.get("export.nav.formulex.rangliste"), ranglisteSheetName,
                buildPdfUrl(pdfRangliste)));
        exportiereHtmlMitMeldelisteDruckbereich(meldelisteExportieren, meldelisteSheetName,
                zielVerzeichnis, "FormuleX.html",
                StringUtils.defaultIfBlank(StringUtils.strip(konfiguration.getKopfZeileMitte()),
                        TurnierSystem.FORMULEX.getBezeichnung()),
                StringUtils.strip(konfiguration.getTurnierlogoUrl()), sections)
                .addTo(exportierteDateien);

        return new ExportErgebnis(exportierteDateien);
    }
}
