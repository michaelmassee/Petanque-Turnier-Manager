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
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.upload.AbstractExportInVerzeichnis;
import de.petanqueturniermanager.helper.upload.ExportErgebnis;
import de.petanqueturniermanager.helper.upload.ExportHtmlSeite;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;

public class SupermeleeExportInVerzeichnis extends AbstractExportInVerzeichnis {

    public SupermeleeExportInVerzeichnis(WorkingSpreadsheet ws, Path zielVerzeichnis) {
        super(ws, TurnierSystem.SUPERMELEE, "Supermelee Export Verzeichnis", zielVerzeichnis);
    }

    @Override
    protected ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException {
        processBox().info(I18n.get("export.info.pdf"));

        var ws = getWorkingSpreadsheet();
        var xDoc = ws.getWorkingSpreadsheetDocument();
        var konfiguration = new SuperMeleeKonfigurationSheet(ws);
        var spieltagRanglisteSheet = new SpieltagRanglisteSheet(ws, SpielTagNr.from(1));
        int anzahlSpieltage = spieltagRanglisteSheet.countNumberOfRanglisten();

        String turnierlogoUrl = StringUtils.strip(konfiguration.getTurnierlogoUrl());
        String turniername = StringUtils.strip(konfiguration.getKopfZeileMitte());

        if (StringUtils.isEmpty(turnierlogoUrl)) {
            processBox().info(I18n.get("export.warnung.turnierlogo.fehlt"));
        } else {
            processBox().info(I18n.get("export.info.turnierlogo", turnierlogoUrl));
        }

        List<Path> exportierteDateien = new ArrayList<>();
        List<String> spieltagSheetNamen = new ArrayList<>();
        List<String> spieltagSchluessel = new ArrayList<>();
        List<String> spieltagPdfUrls = new ArrayList<>();
        List<String> spieltagTitel = new ArrayList<>();

        for (int nr = 1; nr <= anzahlSpieltage; nr++) {
            var schluessel = SheetMetadataHelper.schluesselSpieltagRangliste(nr);
            var xSheet = SheetMetadataHelper.findeSheetUndHeile(xDoc, schluessel, SheetNamen.spieltagRangliste(nr));
            if (xSheet == null) {
                continue;
            }
            var sheetName = Lo.qi(XNamed.class, xSheet).getName();
            var titel = I18n.get("export.supermelee.spieltag", nr);
            var pdf = exportierePdfWennTabelleVorhanden(sheetName, zielVerzeichnis);
            if (pdf != null) {
                exportierteDateien.add(pdf);
            }
            spieltagSheetNamen.add(sheetName);
            spieltagSchluessel.add(schluessel);
            spieltagTitel.add(titel);
            spieltagPdfUrls.add(buildPdfUrl(pdf));
        }

        var endranglisteSchluessel = SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_ENDRANGLISTE;
        var xEndrangliste = SheetMetadataHelper.findeSheetUndHeile(xDoc, endranglisteSchluessel,
                SheetNamen.endrangliste());
        String endranglisteSheetName = xEndrangliste != null ? Lo.qi(XNamed.class, xEndrangliste).getName() : null;
        Path pdfEndrangliste = endranglisteSheetName != null
                ? exportierePdfWennTabelleVorhanden(endranglisteSheetName, zielVerzeichnis)
                : null;
        if (pdfEndrangliste != null) {
            exportierteDateien.add(pdfEndrangliste);
        }

        processBox().info(I18n.get("export.info.html"));
        Path htmlDatei = exportiereHtml(zielVerzeichnis, turniername, turnierlogoUrl,
                endranglisteSheetName, pdfEndrangliste, spieltagSheetNamen, spieltagSchluessel,
                spieltagTitel, spieltagPdfUrls);
        exportierteDateien.add(htmlDatei);

        return new ExportErgebnis(exportierteDateien);
    }

    private Path exportiereHtml(Path zielVerzeichnis, String turniername, String logoUrl,
            String endranglisteSheetName, Path pdfEndrangliste,
            List<String> sheetNamen, List<String> schluessel, List<String> titel, List<String> pdfUrls)
            throws GenerateException {
        List<ExportHtmlSeite.Section> sections = new ArrayList<>();
        String endranglisteNavSheetName = endranglisteSheetName != null ? endranglisteSheetName : SheetNamen.endrangliste();
        sections.add(new ExportHtmlSeite.Section(SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_ENDRANGLISTE,
                I18n.get("export.supermelee.nav.endrangliste"),
                endranglisteNavSheetName, buildPdfUrl(pdfEndrangliste)));

        for (int i = 0; i < sheetNamen.size(); i++) {
            sections.add(new ExportHtmlSeite.Section(schluessel.get(i), titel.get(i),
                    sheetNamen.get(i), pdfUrls.get(i)));
        }

        return exportiereHtml(zielVerzeichnis, "SuperMelee.html",
                StringUtils.defaultIfBlank(turniername, TurnierSystem.SUPERMELEE.getBezeichnung()),
                logoUrl, sections);
    }
}
