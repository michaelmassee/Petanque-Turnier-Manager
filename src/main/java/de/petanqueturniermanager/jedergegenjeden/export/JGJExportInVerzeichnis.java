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
import de.petanqueturniermanager.helper.rangliste.SignaturQuellen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheetsync.EingabeSignatur;
import de.petanqueturniermanager.helper.upload.AbstractExportInVerzeichnis;
import de.petanqueturniermanager.helper.upload.ExportErgebnis;
import de.petanqueturniermanager.helper.upload.ExportFormat;
import de.petanqueturniermanager.helper.upload.ExportHtmlSeite;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJGesamtranglisteSheetUpdate;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteDirektvergleichSheet;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteSheetUpdate;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet;

public class JGJExportInVerzeichnis extends AbstractExportInVerzeichnis {

    public JGJExportInVerzeichnis(WorkingSpreadsheet ws, Path zielVerzeichnis, ExportFormat format) {
        super(ws, TurnierSystem.JGJ, "JGJ Export Verzeichnis", zielVerzeichnis, format);
    }

    @Override
    protected ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException {
        var ws = getWorkingSpreadsheet();
        var konfiguration = new JGJKonfigurationSheet(ws);
        boolean abhaengigeAusgabeFehlt = exportSheetFehlt(SheetMetadataHelper.SCHLUESSEL_JGJ_RANGLISTE)
                || exportSheetFehlt(SheetMetadataHelper.SCHLUESSEL_JGJ_DIREKTVERGLEICH);
        aktualisiereExportSheetWennDirty(SheetMetadataHelper.SCHLUESSEL_JGJ_RANGLISTE,
                new EingabeSignatur(SignaturQuellen::fuerJGJ),
                abhaengigeAusgabeFehlt,
                () -> {
                    new JGJRanglisteSheetUpdate(ws).doRun();
                    new JGJGesamtranglisteSheetUpdate(ws).doRun();
                    new JGJRanglisteDirektvergleichSheet(ws).aktualisieren();
                });

        String meldelisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_JGJ_MELDELISTE, SheetNamen.meldeliste());
        String spielplanSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_JGJ_SPIELPLAN, JGJSpielPlanSheet.sheetName());
        String ranglisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_JGJ_RANGLISTE, SheetNamen.rangliste());
        String direktvergleichSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_JGJ_DIREKTVERGLEICH, SheetNamen.direktvergleich());
        boolean meldelisteExportieren = konfiguration.isMeldelisteExportieren();
        String titel = StringUtils.defaultIfBlank(StringUtils.strip(konfiguration.getKopfZeileMitte()),
                TurnierSystem.JGJ.getBezeichnung());
        var finalrunden = buchstabenSheetEintraegePerSchluessel(
                SheetMetadataHelper::schluesselJgjFinalrunde, SheetNamen::koFinaleGruppe);

        if (getFormat().istEinDokument()) {
            List<ExportHtmlSeite.Section> sections = grundSections(meldelisteSheetName, spielplanSheetName,
                    ranglisteSheetName, direktvergleichSheetName, meldelisteExportieren, null, null);
            for (var eintrag : finalrunden) {
                sections.add(new ExportHtmlSeite.Section("finalrunde-" + eintrag.buchstabe(),
                        I18n.get("export.jgj.nav.finalrunde", eintrag.buchstabe()), eintrag.sheetName(), null));
            }
            processBox().info(I18n.get("export.info.ein.dokument", getFormat().anzeigeName()));
            Path dokument = exportiereEinDokument(zielVerzeichnis, "JederGegenJeden", titel, getFormat(), sections);
            List<Path> exportierteDateien = new ArrayList<>();
            if (dokument != null) {
                exportierteDateien.add(dokument);
            }
            return new ExportErgebnis(exportierteDateien);
        }

        processBox().info(I18n.get("export.info.pdf"));
        List<Path> exportierteDateien = new ArrayList<>();

        Path pdfRangliste = exportierePdfAusHtml(ranglisteSheetName, I18n.get("export.nav.rangliste"), zielVerzeichnis);
        if (pdfRangliste != null) {
            exportierteDateien.add(pdfRangliste);
        }

        Path pdfDirektvergleich = exportierePdfAusHtml(direktvergleichSheetName, I18n.get("export.nav.direktvergleich"), zielVerzeichnis);
        if (pdfDirektvergleich != null) {
            exportierteDateien.add(pdfDirektvergleich);
        }

        processBox().info(I18n.get("export.info.html"));
        List<ExportHtmlSeite.Section> sections = grundSections(meldelisteSheetName, spielplanSheetName,
                ranglisteSheetName, direktvergleichSheetName, meldelisteExportieren,
                buildPdfUrl(pdfRangliste), buildPdfUrl(pdfDirektvergleich));
        for (var eintrag : finalrunden) {
            var finalTitel = I18n.get("export.jgj.nav.finalrunde", eintrag.buchstabe());
            Path pdf = exportierePdfAusHtml(eintrag.sheetName(), finalTitel, zielVerzeichnis);
            if (pdf != null) {
                exportierteDateien.add(pdf);
            }
            sections.add(new ExportHtmlSeite.Section("finalrunde-" + eintrag.buchstabe(),
                    finalTitel, eintrag.sheetName(), buildPdfUrl(pdf)));
        }
        exportiereHtmlMitMeldelisteDruckbereich(meldelisteExportieren, meldelisteSheetName,
                zielVerzeichnis, "JederGegenJeden.html", titel,
                StringUtils.strip(konfiguration.getTurnierlogoUrl()), sections)
                .addTo(exportierteDateien);

        return new ExportErgebnis(exportierteDateien);
    }

    private static List<ExportHtmlSeite.Section> grundSections(String meldelisteSheetName, String spielplanSheetName,
            String ranglisteSheetName, String direktvergleichSheetName, boolean meldelisteExportieren,
            String ranglistePdfUrl, String direktvergleichPdfUrl) {
        List<ExportHtmlSeite.Section> sections = new ArrayList<>();
        if (meldelisteExportieren) {
            sections.add(new ExportHtmlSeite.Section("meldeliste", I18n.get("export.nav.meldeliste"), meldelisteSheetName, null));
        }
        sections.add(new ExportHtmlSeite.Section("spielplan", I18n.get("export.nav.spielplan"), spielplanSheetName, null));
        sections.add(new ExportHtmlSeite.Section("rangliste", I18n.get("export.nav.rangliste"), ranglisteSheetName,
                ranglistePdfUrl));
        sections.add(new ExportHtmlSeite.Section("direktvergleich", I18n.get("export.nav.direktvergleich"), direktvergleichSheetName,
                direktvergleichPdfUrl));
        return sections;
    }
}
