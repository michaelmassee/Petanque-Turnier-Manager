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
import de.petanqueturniermanager.helper.rangliste.SignaturQuellen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheetsync.EingabeSignatur;
import de.petanqueturniermanager.helper.upload.AbstractExportInVerzeichnis;
import de.petanqueturniermanager.helper.upload.ExportErgebnis;
import de.petanqueturniermanager.helper.upload.ExportHtmlSeite;
import de.petanqueturniermanager.kaskade.konfiguration.KaskadeKonfigurationSheet;
import de.petanqueturniermanager.kaskade.spielrunde.KaskadeGruppenRanglisteSheetUpdate;

public class KaskadeExportInVerzeichnis extends AbstractExportInVerzeichnis {

    public KaskadeExportInVerzeichnis(WorkingSpreadsheet ws, Path zielVerzeichnis) {
        super(ws, TurnierSystem.KASKADE, "Kaskade Export Verzeichnis", zielVerzeichnis);
    }

    @Override
    protected ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException {
        processBox().info(I18n.get("export.info.pdf"));

        var ws = getWorkingSpreadsheet();
        var konfiguration = new KaskadeKonfigurationSheet(ws);
        aktualisiereExportSheetWennDirty(SheetMetadataHelper.SCHLUESSEL_KASKADE_GRUPPENRANGLISTE,
                new EingabeSignatur(SignaturQuellen::fuerKaskade),
                () -> new KaskadeGruppenRanglisteSheetUpdate(ws).doRun());

        List<Path> exportierteDateien = new ArrayList<>();
        List<ExportHtmlSeite.Section> sections = new ArrayList<>();

        String meldelisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_KASKADE_MELDELISTE, SheetNamen.meldeliste());
        boolean meldelisteExportieren = konfiguration.isMeldelisteExportieren();
        if (meldelisteExportieren) {
            sections.add(new ExportHtmlSeite.Section("meldeliste", I18n.get("export.nav.meldeliste"), meldelisteSheetName, null));
        }

        String ranglisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_KASKADE_GRUPPENRANGLISTE, SheetNamen.kaskadeGruppenrangliste());
        Path pdfRangliste = exportierePdfAusHtml(ranglisteSheetName, I18n.get("export.kaskade.nav.gruppenrangliste"), zielVerzeichnis);
        if (pdfRangliste != null) {
            exportierteDateien.add(pdfRangliste);
        }
        sections.add(new ExportHtmlSeite.Section("gruppenrangliste",
                I18n.get("export.kaskade.nav.gruppenrangliste"), ranglisteSheetName,
                buildPdfUrl(pdfRangliste)));

        for (var eintrag : buchstabenSheetEintraegePerSchluessel(
                SheetMetadataHelper::schluesselKaskadenFeld, SheetNamen::kaskadenFeld)) {
            var feldTitel = I18n.get("export.kaskade.nav.feld", eintrag.buchstabe());
            Path pdf = exportierePdfAusHtml(eintrag.sheetName(), feldTitel, zielVerzeichnis);
            if (pdf != null) {
                exportierteDateien.add(pdf);
            }
            sections.add(new ExportHtmlSeite.Section("feld-" + eintrag.buchstabe(),
                    feldTitel, eintrag.sheetName(), buildPdfUrl(pdf)));
        }

        processBox().info(I18n.get("export.info.html"));
        exportiereHtmlMitMeldelisteDruckbereich(meldelisteExportieren, meldelisteSheetName,
                zielVerzeichnis, "KaskadenKO.html",
                StringUtils.defaultIfBlank(StringUtils.strip(konfiguration.getKopfZeileMitte()),
                        TurnierSystem.KASKADE.getBezeichnung()),
                StringUtils.strip(konfiguration.getTurnierlogoUrl()), sections)
                .addTo(exportierteDateien);

        return new ExportErgebnis(exportierteDateien);
    }
}
