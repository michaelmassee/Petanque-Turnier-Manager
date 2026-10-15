package de.petanqueturniermanager.liga.meldeliste;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.upload.AbstractExportInVerzeichnis;
import de.petanqueturniermanager.helper.upload.ExportFormat;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;
import de.petanqueturniermanager.liga.spielplan.LigaTermineProTeilnehmerSheet;
import de.petanqueturniermanager.helper.upload.ExportErgebnis;
import de.petanqueturniermanager.helper.upload.ExportHtmlSeite;
import de.petanqueturniermanager.liga.konfiguration.LigaKonfigurationSheet;

public class LigaExportInVerzeichnis extends AbstractExportInVerzeichnis {

    public LigaExportInVerzeichnis(WorkingSpreadsheet ws, Path zielVerzeichnis, ExportFormat format) {
        super(ws, TurnierSystem.LIGA, "Liga Export Verzeichnis", zielVerzeichnis, format);
    }

    @Override
    protected ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException {
        var ws = getWorkingSpreadsheet();
        var konfiguration = new LigaKonfigurationSheet(ws);

        String turnierlogoUrl = StringUtils.strip(konfiguration.getTurnierlogoUrl());
        String gruppenname = StringUtils.strip(konfiguration.getGruppenname());
        String titel = StringUtils.defaultIfBlank(gruppenname, TurnierSystem.LIGA.getBezeichnung());

        String meldelisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_LIGA_MELDELISTE, SheetNamen.meldeliste());
        String spielplanSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_LIGA_SPIELPLAN, LigaSpielPlanSheet.sheetName());
        List<String> termineSheetNames = termineSheetNames();
        String ranglisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE, SheetNamen.rangliste());
        String direktvergleichSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_LIGA_DIREKTVERGLEICH, SheetNamen.direktvergleich());
        boolean meldelisteExportieren = konfiguration.isMeldelisteExportieren();
        boolean abschlussSheetExportieren = konfiguration.isAbschlussSheetExportieren();
        String abschlussSheetName = StringUtils.strip(konfiguration.getAbschlussSheetName());

        if (StringUtils.isEmpty(turnierlogoUrl)) {
            processBox().info(I18n.get("export.warnung.turnierlogo.fehlt"));
        } else {
            processBox().info(I18n.get("export.info.turnierlogo", turnierlogoUrl));
        }

        if (getFormat().istEinDokument()) {
            List<ExportHtmlSeite.Section> sections = htmlSections(meldelisteSheetName, spielplanSheetName, null,
                    termineSheetNames, ranglisteSheetName, null, direktvergleichSheetName, meldelisteExportieren);
            if (abschlussSheetExportieren && StringUtils.isNotBlank(abschlussSheetName)) {
                var abschluss = renderiereAbschlussSheetAlsBild(abschlussSheetName, zielVerzeichnis);
                if (abschluss != null) {
                    sections.add(new ExportHtmlSeite.Section("abschluss-sheet", abschlussSheetName,
                            null, null, abschluss.png()));
                }
            }
            processBox().info(I18n.get("export.info.ein.dokument", getFormat().anzeigeName()));
            Path dokument = exportiereEinDokument(zielVerzeichnis, "Liga", titel, turnierlogoUrl, getFormat(), sections);
            List<Path> exportierteDateien = new ArrayList<>();
            if (dokument != null) {
                exportierteDateien.add(dokument);
            }
            return new ExportErgebnis(exportierteDateien);
        }

        processBox().info(I18n.get("export.info.pdf"));
        Path pdfSpielplan = exportierePdfAusHtml(spielplanSheetName, I18n.get("export.liga.nav.spielplan"),
                titel, turnierlogoUrl, zielVerzeichnis);

        Path pdfRangliste = exportierePdfAusHtml(ranglisteSheetName, I18n.get("export.liga.nav.rangliste"),
                titel, turnierlogoUrl, zielVerzeichnis);
        List<TerminExportEintrag> termine = exportiereTerminlistenPdf(termineSheetNames, titel, turnierlogoUrl, zielVerzeichnis);

        processBox().info(I18n.get("export.info.html"));
        List<ExportHtmlSeite.Section> sections = htmlSectionsMitTerminPdf(
                meldelisteSheetName, spielplanSheetName, buildPdfUrl(pdfSpielplan), termine,
                ranglisteSheetName, buildPdfUrl(pdfRangliste), direktvergleichSheetName,
                meldelisteExportieren);

        AbschlussSheetErgebnis abschluss = null;
        if (abschlussSheetExportieren && StringUtils.isNotBlank(abschlussSheetName)) {
            abschluss = renderiereAbschlussSheetAlsBild(abschlussSheetName, zielVerzeichnis);
            if (abschluss != null) {
                sections.add(new ExportHtmlSeite.Section("abschluss-sheet", abschlussSheetName,
                        null, buildPdfUrl(abschluss.pdf()), abschluss.png()));
            }
        }

        var htmlExport = exportiereHtmlMitMeldelisteDruckbereich(meldelisteExportieren, meldelisteSheetName,
                zielVerzeichnis, "Liga.html", titel, turnierlogoUrl, sections);

        List<Path> exportierteDateien = new ArrayList<>();
        if (pdfSpielplan != null) {
            exportierteDateien.add(pdfSpielplan);
        }
        if (pdfRangliste != null) {
            exportierteDateien.add(pdfRangliste);
        }
        termine.stream()
                .map(TerminExportEintrag::pdf)
                .filter(pdf -> pdf != null)
                .forEach(exportierteDateien::add);
        if (abschluss != null) {
            exportierteDateien.add(abschluss.png());
            exportierteDateien.add(abschluss.pdf());
        }
        htmlExport.addTo(exportierteDateien);
        return new ExportErgebnis(exportierteDateien);
    }

    private List<TerminExportEintrag> exportiereTerminlistenPdf(List<String> termineSheetNames, String titel,
            String turnierlogoUrl, Path zielVerzeichnis) throws GenerateException {
        List<TerminExportEintrag> termine = new ArrayList<>();
        for (String sheetName : termineSheetNames) {
            Path pdf = exportierePdfAusHtml(sheetName, sheetName, titel, turnierlogoUrl, zielVerzeichnis);
            termine.add(new TerminExportEintrag(sheetName, buildPdfUrl(pdf), pdf));
        }
        return termine;
    }

    public static List<ExportHtmlSeite.Section> htmlSections(
            String meldelisteSheetName, String spielplanSheetName, String spielplanPdfUrl,
            List<String> termineSheetNames, String ranglisteSheetName, String ranglistePdfUrl, String direktvergleichSheetName,
            boolean meldelisteExportieren) {
        return htmlSectionsMitTerminPdf(meldelisteSheetName, spielplanSheetName, spielplanPdfUrl,
                termineSheetNames.stream()
                        .map(sheetName -> new TerminExportEintrag(sheetName, null, null))
                        .toList(),
                ranglisteSheetName, ranglistePdfUrl, direktvergleichSheetName, meldelisteExportieren);
    }

    public static List<ExportHtmlSeite.Section> htmlSectionsMitTerminPdf(
            String meldelisteSheetName, String spielplanSheetName, String spielplanPdfUrl,
            List<TerminExportEintrag> termine, String ranglisteSheetName, String ranglistePdfUrl,
            String direktvergleichSheetName, boolean meldelisteExportieren) {
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
        for (int i = 0; i < termine.size(); i++) {
            TerminExportEintrag termin = termine.get(i);
            sections.add(new ExportHtmlSeite.Section("termine-" + (i + 1), termin.sheetName(),
                    termin.sheetName(), termin.pdfUrl()));
        }
        return sections;
    }

    private List<String> termineSheetNames() throws GenerateException {
        var doc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
        List<TerminSheetEintrag> eintraege = new ArrayList<>();
        for (String schluessel : SheetMetadataHelper.getSchluesselMitPrefix(doc,
                SheetMetadataHelper.SCHLUESSEL_LIGA_TERMINE_PRO_TEILNEHMER_PREFIX)) {
            if (!schluessel.endsWith(SheetMetadataHelper.SCHLUESSEL_SUFFIX)) {
                continue;
            }
            int teamNr = teamNrAusSchluessel(schluessel);
            if (teamNr <= 0) {
                continue;
            }
            String sheetName = sheetNamePerSchluessel(schluessel, LigaTermineProTeilnehmerSheet.sheetName(teamNr));
            eintraege.add(new TerminSheetEintrag(teamNr, sheetName));
        }
        if (eintraege.isEmpty()) {
            String legacyName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_LIGA_TERMINE_PRO_TEILNEHMER,
                    LigaTermineProTeilnehmerSheet.sheetName());
            if (getSheetHelper().findByName(legacyName) != null) {
                return List.of(legacyName);
            }
        }
        return eintraege.stream()
                .sorted(Comparator.comparingInt(TerminSheetEintrag::teamNr))
                .map(TerminSheetEintrag::sheetName)
                .toList();
    }

    private static int teamNrAusSchluessel(String schluessel) {
        String prefix = SheetMetadataHelper.SCHLUESSEL_LIGA_TERMINE_PRO_TEILNEHMER_PREFIX;
        String nummer = schluessel.substring(prefix.length(), schluessel.length() - SheetMetadataHelper.SCHLUESSEL_SUFFIX.length());
        try {
            return Integer.parseInt(nummer);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private record TerminSheetEintrag(int teamNr, String sheetName) {
    }

    public record TerminExportEintrag(String sheetName, String pdfUrl, Path pdf) {
    }
}
