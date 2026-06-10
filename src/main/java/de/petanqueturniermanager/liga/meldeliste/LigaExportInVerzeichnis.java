package de.petanqueturniermanager.liga.meldeliste;

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
import de.petanqueturniermanager.helper.sheet.io.PdfExport;
import de.petanqueturniermanager.helper.upload.AbstractExportInVerzeichnis;
import de.petanqueturniermanager.helper.upload.ExportErgebnis;
import de.petanqueturniermanager.helper.upload.ExportHtmlSeite;
import de.petanqueturniermanager.liga.konfiguration.LigaKonfigurationSheet;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteSheet;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;

public class LigaExportInVerzeichnis extends AbstractExportInVerzeichnis {

    public LigaExportInVerzeichnis(WorkingSpreadsheet ws, Path zielVerzeichnis) {
        super(ws, TurnierSystem.LIGA, "Liga Export Verzeichnis", zielVerzeichnis);
    }

    @Override
    protected ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException {
        var ws = getWorkingSpreadsheet();
        new LigaMeldeListeSheetUpdate(ws).upDateSheet();
        processBox().info(I18n.get("export.info.pdf"));

        var ligaSpielPlanSheet = new LigaSpielPlanSheet(ws);
        var ligaRanglisteSheet = new LigaRanglisteSheet(ws);
        var konfiguration = new LigaKonfigurationSheet(ws);

        String turnierlogoUrl = StringUtils.strip(konfiguration.getTurnierlogoUrl());
        String gruppenname = StringUtils.strip(konfiguration.getGruppenname());

        if (StringUtils.isEmpty(turnierlogoUrl)) {
            processBox().info(I18n.get("export.warnung.turnierlogo.fehlt"));
        } else {
            processBox().info(I18n.get("export.info.turnierlogo", turnierlogoUrl));
        }

        String meldelisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_LIGA_MELDELISTE, SheetNamen.meldeliste());
        String spielplanSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_LIGA_SPIELPLAN, LigaSpielPlanSheet.sheetName());
        String ranglisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE, SheetNamen.rangliste());
        String direktvergleichSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_LIGA_DIREKTVERGLEICH, SheetNamen.direktvergleich());

        Path pdfSpielplan = exportierePdfWennTabelleVorhanden(spielplanSheetName,
                () -> PdfExport.from(ws)
                        .sheetName(spielplanSheetName)
                        .range(ligaSpielPlanSheet.printBereichRangePosition())
                        .prefix1(spielplanSheetName)
                        .zielVerzeichnis(zielVerzeichnis)
                        .doExport());

        Path pdfRangliste = exportierePdfWennTabelleVorhanden(ranglisteSheetName,
                () -> PdfExport.from(ws)
                        .sheetName(ranglisteSheetName)
                        .range(ligaRanglisteSheet.printBereichRangePosition())
                        .prefix1(ranglisteSheetName)
                        .zielVerzeichnis(zielVerzeichnis)
                        .doExport());

        processBox().info(I18n.get("export.info.html"));
        List<ExportHtmlSeite.Section> sections = htmlSections(
                meldelisteSheetName, spielplanSheetName, buildPdfUrl(pdfSpielplan),
                ranglisteSheetName, buildPdfUrl(pdfRangliste), direktvergleichSheetName,
                konfiguration.isMeldelisteExportieren());
        Path htmlDatei = exportiereHtml(zielVerzeichnis, "Liga.html",
                StringUtils.defaultIfBlank(gruppenname, TurnierSystem.LIGA.getBezeichnung()),
                turnierlogoUrl, sections);

        List<Path> exportierteDateien = new ArrayList<>();
        if (pdfSpielplan != null) {
            exportierteDateien.add(pdfSpielplan);
        }
        if (pdfRangliste != null) {
            exportierteDateien.add(pdfRangliste);
        }
        exportierteDateien.add(htmlDatei);
        return new ExportErgebnis(exportierteDateien);
    }

    public static List<ExportHtmlSeite.Section> htmlSections(
            String meldelisteSheetName, String spielplanSheetName, String spielplanPdfUrl,
            String ranglisteSheetName, String ranglistePdfUrl, String direktvergleichSheetName,
            boolean meldelisteExportieren) {
        List<ExportHtmlSeite.Section> sections = new ArrayList<>();
        if (meldelisteExportieren) {
            sections.add(new ExportHtmlSeite.Section("meldeliste", I18n.get("export.liga.nav.meldeliste"),
                    meldelisteSheetName, null));
        }
        sections.add(new ExportHtmlSeite.Section("spielplan", I18n.get("export.liga.nav.spielplan"),
                spielplanSheetName, spielplanPdfUrl));
        sections.add(new ExportHtmlSeite.Section("rangliste", I18n.get("export.liga.nav.rangliste"),
                ranglisteSheetName, ranglistePdfUrl));
        sections.add(new ExportHtmlSeite.Section("direktvergleich", I18n.get("export.liga.nav.direktvergleich"),
                direktvergleichSheetName, null));
        return sections;
    }
}
