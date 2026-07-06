package de.petanqueturniermanager.liga.meldeliste;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.rangliste.SignaturQuellen;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheetsync.EingabeSignatur;
import de.petanqueturniermanager.helper.sheetsync.SheetSyncSignaturStore;
import de.petanqueturniermanager.helper.sheetsync.SignaturErgebnis;
import de.petanqueturniermanager.helper.upload.AbstractExportInVerzeichnis;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteDirektvergleichSheet;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteSheetUpdate;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;
import de.petanqueturniermanager.liga.spielplan.LigaTermineProTeilnehmerSheet;
import de.petanqueturniermanager.helper.upload.ExportErgebnis;
import de.petanqueturniermanager.helper.upload.ExportHtmlSeite;
import de.petanqueturniermanager.liga.konfiguration.LigaKonfigurationSheet;
import de.petanqueturniermanager.model.TeamMeldungen;

public class LigaExportInVerzeichnis extends AbstractExportInVerzeichnis {

    private static final Logger logger = LogManager.getLogger(LigaExportInVerzeichnis.class);
    private static final String EXPORT_UPDATE_GRUND = "exportBeforeBuild";

    public LigaExportInVerzeichnis(WorkingSpreadsheet ws, Path zielVerzeichnis) {
        super(ws, TurnierSystem.LIGA, "Liga Export Verzeichnis", zielVerzeichnis);
    }

    @Override
    protected ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException {
        var ws = getWorkingSpreadsheet();
        var meldeliste = new LigaMeldeListeSheetUpdate(ws);
        meldeliste.upDateSheet();
        aktualisiereExportSheetsWennDirty(meldeliste.getAlleMeldungen());
        processBox().info(I18n.get("export.info.pdf"));

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
        List<String> termineSheetNames = termineSheetNames();
        String ranglisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE, SheetNamen.rangliste());
        String direktvergleichSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_LIGA_DIREKTVERGLEICH, SheetNamen.direktvergleich());

        Path pdfSpielplan = exportierePdfAusHtml(spielplanSheetName, I18n.get("export.liga.nav.spielplan"), zielVerzeichnis);

        Path pdfRangliste = exportierePdfAusHtml(ranglisteSheetName, I18n.get("export.liga.nav.rangliste"), zielVerzeichnis);
        List<TerminExportEintrag> termine = exportiereTerminlistenPdf(termineSheetNames, zielVerzeichnis);

        processBox().info(I18n.get("export.info.html"));
        boolean meldelisteExportieren = konfiguration.isMeldelisteExportieren();
        List<ExportHtmlSeite.Section> sections = htmlSectionsMitTerminPdf(
                meldelisteSheetName, spielplanSheetName, buildPdfUrl(pdfSpielplan), termine,
                ranglisteSheetName, buildPdfUrl(pdfRangliste), direktvergleichSheetName,
                meldelisteExportieren);
        var htmlExport = exportiereHtmlMitMeldelisteDruckbereich(meldelisteExportieren, meldelisteSheetName,
                zielVerzeichnis, "Liga.html",
                StringUtils.defaultIfBlank(gruppenname, TurnierSystem.LIGA.getBezeichnung()),
                turnierlogoUrl, sections);

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
        htmlExport.addTo(exportierteDateien);
        return new ExportErgebnis(exportierteDateien);
    }

    private void aktualisiereExportSheetsWennDirty(TeamMeldungen meldungen) throws GenerateException {
        var doc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
        SignaturErgebnis ranglisteErgebnis = new EingabeSignatur(SignaturQuellen::fuerLiga).berechne(doc, 1);
        SignaturErgebnis termineErgebnis = new EingabeSignatur(SignaturQuellen::fuerLigaTermineProTeilnehmer)
                .berechne(doc, 1);
        boolean ausgabenFehlen = exportAusgabenFehlen(meldungen);

        boolean ranglisteDirty = istDirty(SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE, ranglisteErgebnis);
        boolean termineDirty = istDirty(SheetMetadataHelper.SCHLUESSEL_LIGA_TERMINE_PRO_TEILNEHMER, termineErgebnis);
        boolean ranglisteOk = ranglisteErgebnis instanceof SignaturErgebnis.Ok;
        boolean termineOk = termineErgebnis instanceof SignaturErgebnis.Ok;
        if (!ausgabenFehlen && !ranglisteDirty && !termineDirty && ranglisteOk && termineOk) {
            if (ranglisteErgebnis instanceof SignaturErgebnis.Ok) {
                SheetSyncSignaturStore.aktualisiereVerifyZeit(doc, SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE);
            }
            if (termineErgebnis instanceof SignaturErgebnis.Ok) {
                SheetSyncSignaturStore.aktualisiereVerifyZeit(doc,
                        SheetMetadataHelper.SCHLUESSEL_LIGA_TERMINE_PRO_TEILNEHMER);
            }
            logger.debug("Liga-Export: abhaengige Sheets unveraendert, Update uebersprungen");
            return;
        }

        if (ausgabenFehlen || ranglisteDirty || termineDirty) {
            aktualisiereExportSheets(meldungen);
            String grund = ausgabenFehlen ? "exportMissingOutput" : EXPORT_UPDATE_GRUND;
            if (ranglisteErgebnis instanceof SignaturErgebnis.Ok ok) {
                SheetSyncSignaturStore.speichereNachRebuild(doc, SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE,
                        ok.hash(), grund);
            }
            if (termineErgebnis instanceof SignaturErgebnis.Ok ok) {
                SheetSyncSignaturStore.speichereNachRebuild(doc,
                        SheetMetadataHelper.SCHLUESSEL_LIGA_TERMINE_PRO_TEILNEHMER, ok.hash(), grund);
            }
            return;
        }

        logger.warn("Liga-Export: Signatur konnte nicht berechnet werden, abhaengige Updates werden uebersprungen: {}, {}",
                ranglisteErgebnis, termineErgebnis);
    }

    private boolean istDirty(String schluessel, SignaturErgebnis ergebnis) {
        if (ergebnis instanceof SignaturErgebnis.Ok ok) {
            var gespeichert = SheetSyncSignaturStore.ladeHash(getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                    schluessel);
            if (gespeichert.isPresent() && gespeichert.get().equals(ok.hash())) {
                logger.debug("Liga-Export: abhaengige Sheets unveraendert, Update uebersprungen");
                return false;
            }
            return true;
        }
        return false;
    }

    private void aktualisiereExportSheets(TeamMeldungen meldungen) throws GenerateException {
        var ws = getWorkingSpreadsheet();
        processBox().info("Liga Export: Terminlisten, Rangliste und Direktvergleich aktualisieren");
        new LigaTermineProTeilnehmerSheet(ws).generate(meldungen);
        new LigaRanglisteSheetUpdate(ws).doRun();
        new LigaRanglisteDirektvergleichSheet(ws).aktualisieren();
    }

    private boolean exportAusgabenFehlen(TeamMeldungen meldungen) throws GenerateException {
        if (getSheetHelper().findByName(sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE,
                SheetNamen.rangliste())) == null) {
            return true;
        }
        if (getSheetHelper().findByName(sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_LIGA_DIREKTVERGLEICH,
                SheetNamen.direktvergleich())) == null) {
            return true;
        }
        return termineSheetNames().size() < meldungen.teams().size();
    }

    private List<TerminExportEintrag> exportiereTerminlistenPdf(List<String> termineSheetNames, Path zielVerzeichnis)
            throws GenerateException {
        List<TerminExportEintrag> termine = new ArrayList<>();
        for (String sheetName : termineSheetNames) {
            Path pdf = exportierePdfAusHtml(sheetName, sheetName, zielVerzeichnis);
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
