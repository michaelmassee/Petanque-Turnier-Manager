/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.export;

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
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;
import de.petanqueturniermanager.poule.rangliste.PouleVorrundenRanglisteSheetUpdate;

public class PouleExportInVerzeichnis extends AbstractExportInVerzeichnis {

    public PouleExportInVerzeichnis(WorkingSpreadsheet ws, Path zielVerzeichnis, ExportFormat format) {
        super(ws, TurnierSystem.POULE, "Poule Export Verzeichnis", zielVerzeichnis, format);
    }

    @Override
    protected ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException {
        var ws = getWorkingSpreadsheet();
        var konfiguration = new PouleKonfigurationSheet(ws);
        aktualisiereExportSheetWennDirty(SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDEN_RANGLISTE,
                new EingabeSignatur(SignaturQuellen::fuerPoule),
                () -> new PouleVorrundenRanglisteSheetUpdate(ws).doRun());

        String meldelisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_POULE_MELDELISTE, SheetNamen.meldeliste());
        String teilnehmerlisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_TEILNEHMER, SheetNamen.pouleTeilnehmer());
        String ranglisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDEN_RANGLISTE, SheetNamen.pouleVorrundenRangliste());
        boolean meldelisteExportieren = konfiguration.isMeldelisteExportieren();
        boolean teilnehmerlisteExportieren = konfiguration.isTeilnehmerlisteExportieren();
        boolean spielrundenExportieren = konfiguration.isSpielrundenExportieren();
        var spielplaene = spielrundenExportieren
                ? rundenSheetEintraegePerSchluessel(SheetMetadataHelper.SCHLUESSEL_POULE_SPIELPLAN_PREFIX, SheetNamen::pouleSpielplan)
                : List.<RundenSheetEintrag>of();
        var koSheets = spielrundenExportieren
                ? buchstabenSheetEintraegePerSchluessel(SheetMetadataHelper::schluesselPouleKo, SheetNamen::koFinaleGruppe)
                : List.<SheetEintrag>of();
        boolean abschlussSheetExportieren = konfiguration.isAbschlussSheetExportieren();
        String abschlussSheetName = StringUtils.strip(konfiguration.getAbschlussSheetName());
        String titel = StringUtils.defaultIfBlank(StringUtils.strip(konfiguration.getKopfZeileMitte()),
                TurnierSystem.POULE.getBezeichnung());
        String turnierlogoUrl = StringUtils.strip(konfiguration.getTurnierlogoUrl());

        if (getFormat().istEinDokument()) {
            List<ExportHtmlSeite.Section> sections = sectionsMitOptionalerMeldelisteUndRangliste(
                    meldelisteSheetName, meldelisteExportieren, teilnehmerlisteSheetName, teilnehmerlisteExportieren,
                    I18n.get("export.nav.poule.vorrunden.rangliste"), ranglisteSheetName, null);
            for (var plan : spielplaene) {
                sections.add(new ExportHtmlSeite.Section("spielplan-" + plan.rundeNr(), plan.sheetName(),
                        plan.sheetName(), null));
            }
            for (var ko : koSheets) {
                sections.add(new ExportHtmlSeite.Section("ko-" + ko.buchstabe(),
                        I18n.get("export.ko.nav.turnierbaum.gruppe", ko.buchstabe()), ko.sheetName(), null));
            }
            if (abschlussSheetExportieren && StringUtils.isNotBlank(abschlussSheetName)) {
                var abschluss = renderiereAbschlussSheetAlsBild(abschlussSheetName, zielVerzeichnis);
                if (abschluss != null) {
                    sections.add(new ExportHtmlSeite.Section("abschluss-sheet", abschlussSheetName,
                            null, null, abschluss.png()));
                }
            }
            processBox().info(I18n.get("export.info.ein.dokument", getFormat().anzeigeName()));
            Path dokument = exportiereEinDokument(zielVerzeichnis, "Poule", titel, turnierlogoUrl, getFormat(), sections);
            List<Path> exportierteDateien = new ArrayList<>();
            if (dokument != null) {
                exportierteDateien.add(dokument);
            }
            return new ExportErgebnis(exportierteDateien);
        }

        processBox().info(I18n.get("export.info.pdf"));
        List<Path> exportierteDateien = new ArrayList<>();

        Path pdfRangliste = exportierePdfAusHtml(ranglisteSheetName, I18n.get("export.nav.poule.vorrunden.rangliste"),
                titel, turnierlogoUrl, zielVerzeichnis);
        if (pdfRangliste != null) {
            exportierteDateien.add(pdfRangliste);
        }

        processBox().info(I18n.get("export.info.html"));
        List<ExportHtmlSeite.Section> sections = sectionsMitOptionalerMeldelisteUndRangliste(
                meldelisteSheetName, meldelisteExportieren, teilnehmerlisteSheetName, teilnehmerlisteExportieren,
                I18n.get("export.nav.poule.vorrunden.rangliste"), ranglisteSheetName, buildPdfUrl(pdfRangliste));
        for (var plan : spielplaene) {
            Path pdf = exportierePdfAusHtml(plan.sheetName(), plan.sheetName(), titel, turnierlogoUrl, zielVerzeichnis);
            if (pdf != null) {
                exportierteDateien.add(pdf);
            }
            sections.add(new ExportHtmlSeite.Section("spielplan-" + plan.rundeNr(), plan.sheetName(),
                    plan.sheetName(), buildPdfUrl(pdf)));
        }
        for (var ko : koSheets) {
            var koTitel = I18n.get("export.ko.nav.turnierbaum.gruppe", ko.buchstabe());
            Path pdf = exportierePdfAusHtml(ko.sheetName(), koTitel, titel, turnierlogoUrl, zielVerzeichnis);
            if (pdf != null) {
                exportierteDateien.add(pdf);
            }
            sections.add(new ExportHtmlSeite.Section("ko-" + ko.buchstabe(), koTitel, ko.sheetName(), buildPdfUrl(pdf)));
        }
        if (abschlussSheetExportieren && StringUtils.isNotBlank(abschlussSheetName)) {
            var abschluss = renderiereAbschlussSheetAlsBild(abschlussSheetName, zielVerzeichnis);
            if (abschluss != null) {
                sections.add(new ExportHtmlSeite.Section("abschluss-sheet", abschlussSheetName,
                        null, buildPdfUrl(abschluss.pdf()), abschluss.png()));
                exportierteDateien.add(abschluss.png());
                exportierteDateien.add(abschluss.pdf());
            }
        }
        exportiereHtmlMitMeldelisteDruckbereich(meldelisteExportieren, meldelisteSheetName,
                zielVerzeichnis, "Poule.html", titel, turnierlogoUrl, sections)
                .addTo(exportierteDateien);

        return new ExportErgebnis(exportierteDateien);
    }
}
