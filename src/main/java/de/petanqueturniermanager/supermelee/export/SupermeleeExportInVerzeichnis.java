/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.export;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.sun.star.container.XNamed;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.rangliste.SignaturQuellen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheetsync.EingabeSignatur;
import de.petanqueturniermanager.helper.upload.AbstractExportInVerzeichnis;
import de.petanqueturniermanager.helper.upload.ExportErgebnis;
import de.petanqueturniermanager.helper.upload.ExportFormat;
import de.petanqueturniermanager.helper.upload.ExportHtmlSeite;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.endrangliste.EndranglisteSheetUpdate;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheetUpdate;

public class SupermeleeExportInVerzeichnis extends AbstractExportInVerzeichnis {

    public SupermeleeExportInVerzeichnis(WorkingSpreadsheet ws, Path zielVerzeichnis, ExportFormat format) {
        super(ws, TurnierSystem.SUPERMELEE, "Supermelee Export Verzeichnis", zielVerzeichnis, format);
    }

    @Override
    protected ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException {
        var ws = getWorkingSpreadsheet();
        var xDoc = ws.getWorkingSpreadsheetDocument();
        var konfiguration = new SuperMeleeKonfigurationSheet(ws);
        var spieltagRanglisteSheet = new SpieltagRanglisteSheet(ws, SpielTagNr.from(1));
        int anzahlSpieltage = spieltagRanglisteSheet.countNumberOfRanglisten();
        aktualisiereExportSheetsWennDirty(ws, anzahlSpieltage);

        String turnierlogoUrl = StringUtils.strip(konfiguration.getTurnierlogoUrl());
        String turniername = StringUtils.defaultIfBlank(StringUtils.strip(konfiguration.getKopfZeileMitte()),
                TurnierSystem.SUPERMELEE.getBezeichnung());

        List<String> spieltagSheetNamen = new ArrayList<>();
        List<String> spieltagSchluessel = new ArrayList<>();
        List<String> spieltagTitel = new ArrayList<>();
        for (int nr = 1; nr <= anzahlSpieltage; nr++) {
            var schluessel = SheetMetadataHelper.schluesselSpieltagRangliste(nr);
            var xSheet = SheetMetadataHelper.findeSheetUndHeile(xDoc, schluessel, SheetNamen.spieltagRangliste(nr));
            if (xSheet == null) {
                continue;
            }
            spieltagSheetNamen.add(Lo.qi(XNamed.class, xSheet).getName());
            spieltagSchluessel.add(schluessel);
            spieltagTitel.add(I18n.get("export.supermelee.spieltag", nr));
        }

        var endranglisteSchluessel = SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_ENDRANGLISTE;
        var xEndrangliste = SheetMetadataHelper.findeSheetUndHeile(xDoc, endranglisteSchluessel,
                SheetNamen.endrangliste());
        String endranglisteSheetName = xEndrangliste != null ? Lo.qi(XNamed.class, xEndrangliste).getName() : null;

        if (StringUtils.isEmpty(turnierlogoUrl)) {
            processBox().info(I18n.get("export.warnung.turnierlogo.fehlt"));
        } else {
            processBox().info(I18n.get("export.info.turnierlogo", turnierlogoUrl));
        }

        if (getFormat().istEinDokument()) {
            List<ExportHtmlSeite.Section> sections = sections(endranglisteSchluessel, endranglisteSheetName, null,
                    spieltagSchluessel, spieltagSheetNamen, spieltagTitel, null);
            processBox().info(I18n.get("export.info.ein.dokument", getFormat().anzeigeName()));
            Path dokument = exportiereEinDokument(zielVerzeichnis, "SuperMelee", turniername, turnierlogoUrl,
                    getFormat(), sections);
            List<Path> exportierteDateien = new ArrayList<>();
            if (dokument != null) {
                exportierteDateien.add(dokument);
            }
            return new ExportErgebnis(exportierteDateien);
        }

        processBox().info(I18n.get("export.info.pdf"));
        List<Path> exportierteDateien = new ArrayList<>();
        List<String> spieltagPdfUrls = new ArrayList<>();
        for (int i = 0; i < spieltagSheetNamen.size(); i++) {
            var pdf = exportierePdfAusHtml(spieltagSheetNamen.get(i), spieltagTitel.get(i), zielVerzeichnis);
            if (pdf != null) {
                exportierteDateien.add(pdf);
            }
            spieltagPdfUrls.add(buildPdfUrl(pdf));
        }

        Path pdfEndrangliste = endranglisteSheetName != null
                ? exportierePdfAusHtml(endranglisteSheetName, I18n.get("export.supermelee.nav.endrangliste"), zielVerzeichnis)
                : null;
        if (pdfEndrangliste != null) {
            exportierteDateien.add(pdfEndrangliste);
        }

        processBox().info(I18n.get("export.info.html"));
        List<ExportHtmlSeite.Section> sections = sections(endranglisteSchluessel, endranglisteSheetName,
                buildPdfUrl(pdfEndrangliste), spieltagSchluessel, spieltagSheetNamen, spieltagTitel, spieltagPdfUrls);
        exportiereHtml(zielVerzeichnis, "SuperMelee.html", turniername, turnierlogoUrl, sections)
                .addTo(exportierteDateien);

        return new ExportErgebnis(exportierteDateien);
    }

    private static List<ExportHtmlSeite.Section> sections(String endranglisteSchluessel, String endranglisteSheetName,
            String endranglistePdfUrl, List<String> spieltagSchluessel, List<String> spieltagSheetNamen,
            List<String> spieltagTitel, List<String> spieltagPdfUrls) {
        List<ExportHtmlSeite.Section> sections = new ArrayList<>();
        String endranglisteNavSheetName = endranglisteSheetName != null ? endranglisteSheetName : SheetNamen.endrangliste();
        sections.add(new ExportHtmlSeite.Section(endranglisteSchluessel,
                I18n.get("export.supermelee.nav.endrangliste"), endranglisteNavSheetName, endranglistePdfUrl));
        for (int i = 0; i < spieltagSheetNamen.size(); i++) {
            String pdfUrl = spieltagPdfUrls != null ? spieltagPdfUrls.get(i) : null;
            sections.add(new ExportHtmlSeite.Section(spieltagSchluessel.get(i), spieltagTitel.get(i),
                    spieltagSheetNamen.get(i), pdfUrl));
        }
        return sections;
    }

    private void aktualisiereExportSheetsWennDirty(WorkingSpreadsheet ws, int anzahlSpieltage)
            throws GenerateException {
        for (int nr = 1; nr <= anzahlSpieltage; nr++) {
            int spieltagNr = nr;
            String persistenzSchluessel = "SUPERMELEE_SPIELTAG_" + spieltagNr;
            String sheetSchluessel = SheetMetadataHelper.schluesselSpieltagRangliste(spieltagNr);
            aktualisiereExportSheetWennDirty(persistenzSchluessel,
                    new EingabeSignatur(xDoc -> SignaturQuellen.fuerSupermeleeSpieltag(xDoc, spieltagNr)),
                    exportSheetFehlt(sheetSchluessel),
                    () -> new SpieltagRanglisteSheetUpdate(ws, SpielTagNr.from(spieltagNr)).doRun());
        }

        aktualisiereExportSheetWennDirty(SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_ENDRANGLISTE,
                new EingabeSignatur(SignaturQuellen::fuerSupermeleeEnd),
                () -> new EndranglisteSheetUpdate(ws).doRun());
    }
}
