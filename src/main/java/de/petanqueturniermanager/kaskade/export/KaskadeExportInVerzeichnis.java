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
import de.petanqueturniermanager.helper.upload.ExportFormat;
import de.petanqueturniermanager.helper.upload.ExportHtmlSeite;
import de.petanqueturniermanager.kaskade.konfiguration.KaskadeKonfigurationSheet;
import de.petanqueturniermanager.kaskade.spielrunde.KaskadeGruppenRanglisteSheetUpdate;

public class KaskadeExportInVerzeichnis extends AbstractExportInVerzeichnis {

    public KaskadeExportInVerzeichnis(WorkingSpreadsheet ws, Path zielVerzeichnis, ExportFormat format) {
        super(ws, TurnierSystem.KASKADE, "Kaskade Export Verzeichnis", zielVerzeichnis, format);
    }

    @Override
    protected ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException {
        var ws = getWorkingSpreadsheet();
        var konfiguration = new KaskadeKonfigurationSheet(ws);
        aktualisiereExportSheetWennDirty(SheetMetadataHelper.SCHLUESSEL_KASKADE_GRUPPENRANGLISTE,
                new EingabeSignatur(SignaturQuellen::fuerKaskade),
                () -> new KaskadeGruppenRanglisteSheetUpdate(ws).doRun());

        String meldelisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_KASKADE_MELDELISTE, SheetNamen.meldeliste());
        String teilnehmerlisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_TEILNEHMER, SheetNamen.teilnehmer());
        boolean meldelisteExportieren = konfiguration.isMeldelisteExportieren();
        boolean teilnehmerlisteExportieren = konfiguration.isTeilnehmerlisteExportieren();
        String ranglisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_KASKADE_GRUPPENRANGLISTE, SheetNamen.kaskadeGruppenrangliste());
        String titel = StringUtils.defaultIfBlank(StringUtils.strip(konfiguration.getKopfZeileMitte()),
                TurnierSystem.KASKADE.getBezeichnung());
        var felder = buchstabenSheetEintraegePerSchluessel(
                SheetMetadataHelper::schluesselKaskadenFeld, SheetNamen::kaskadenFeld);
        String turnierlogoUrl = StringUtils.strip(konfiguration.getTurnierlogoUrl());
        boolean abschlussSheetExportieren = konfiguration.isAbschlussSheetExportieren();
        String abschlussSheetName = StringUtils.strip(konfiguration.getAbschlussSheetName());

        if (getFormat().istEinDokument()) {
            List<ExportHtmlSeite.Section> sections = new ArrayList<>();
            if (meldelisteExportieren) {
                sections.add(new ExportHtmlSeite.Section("meldeliste", I18n.get("export.nav.meldeliste"), meldelisteSheetName, null));
            }
            if (teilnehmerlisteExportieren) {
                sections.add(new ExportHtmlSeite.Section("teilnehmerliste", I18n.get("export.nav.teilnehmerliste"), teilnehmerlisteSheetName, null));
            }
            sections.add(new ExportHtmlSeite.Section("gruppenrangliste",
                    I18n.get("export.kaskade.nav.gruppenrangliste"), ranglisteSheetName, null));
            for (var eintrag : felder) {
                sections.add(new ExportHtmlSeite.Section("feld-" + eintrag.buchstabe(),
                        I18n.get("export.kaskade.nav.feld", eintrag.buchstabe()), eintrag.sheetName(), null));
            }
            if (abschlussSheetExportieren && StringUtils.isNotBlank(abschlussSheetName)) {
                var abschluss = renderiereAbschlussSheetAlsBild(abschlussSheetName, zielVerzeichnis);
                if (abschluss != null) {
                    sections.add(new ExportHtmlSeite.Section("abschluss-sheet", I18n.get("export.nav.abschluss.sheet"),
                            null, null, abschluss.png()));
                }
            }
            processBox().info(I18n.get("export.info.ein.dokument", getFormat().anzeigeName()));
            Path dokument = exportiereEinDokument(zielVerzeichnis, "KaskadenKO", titel, turnierlogoUrl,
                    getFormat(), sections);
            List<Path> exportierteDateien = new ArrayList<>();
            if (dokument != null) {
                exportierteDateien.add(dokument);
            }
            return new ExportErgebnis(exportierteDateien);
        }

        processBox().info(I18n.get("export.info.pdf"));
        List<Path> exportierteDateien = new ArrayList<>();
        List<ExportHtmlSeite.Section> sections = new ArrayList<>();

        if (meldelisteExportieren) {
            sections.add(new ExportHtmlSeite.Section("meldeliste", I18n.get("export.nav.meldeliste"), meldelisteSheetName, null));
        }
        if (teilnehmerlisteExportieren) {
            sections.add(new ExportHtmlSeite.Section("teilnehmerliste", I18n.get("export.nav.teilnehmerliste"), teilnehmerlisteSheetName, null));
        }

        Path pdfRangliste = exportierePdfAusHtml(ranglisteSheetName, I18n.get("export.kaskade.nav.gruppenrangliste"), zielVerzeichnis);
        if (pdfRangliste != null) {
            exportierteDateien.add(pdfRangliste);
        }
        sections.add(new ExportHtmlSeite.Section("gruppenrangliste",
                I18n.get("export.kaskade.nav.gruppenrangliste"), ranglisteSheetName,
                buildPdfUrl(pdfRangliste)));

        for (var eintrag : felder) {
            var feldTitel = I18n.get("export.kaskade.nav.feld", eintrag.buchstabe());
            Path pdf = exportierePdfAusHtml(eintrag.sheetName(), feldTitel, zielVerzeichnis);
            if (pdf != null) {
                exportierteDateien.add(pdf);
            }
            sections.add(new ExportHtmlSeite.Section("feld-" + eintrag.buchstabe(),
                    feldTitel, eintrag.sheetName(), buildPdfUrl(pdf)));
        }

        if (abschlussSheetExportieren && StringUtils.isNotBlank(abschlussSheetName)) {
            var abschluss = renderiereAbschlussSheetAlsBild(abschlussSheetName, zielVerzeichnis);
            if (abschluss != null) {
                sections.add(new ExportHtmlSeite.Section("abschluss-sheet", I18n.get("export.nav.abschluss.sheet"),
                        null, buildPdfUrl(abschluss.pdf()), abschluss.png()));
                exportierteDateien.add(abschluss.png());
                exportierteDateien.add(abschluss.pdf());
            }
        }

        processBox().info(I18n.get("export.info.html"));
        exportiereHtmlMitMeldelisteDruckbereich(meldelisteExportieren, meldelisteSheetName,
                zielVerzeichnis, "KaskadenKO.html", titel, turnierlogoUrl, sections)
                .addTo(exportierteDateien);

        return new ExportErgebnis(exportierteDateien);
    }
}
