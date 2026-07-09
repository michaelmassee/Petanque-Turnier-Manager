/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.forme.export;

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
import de.petanqueturniermanager.helper.upload.ExportFormat;
import de.petanqueturniermanager.helper.upload.ExportHtmlSeite;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;

public class KoExportInVerzeichnis extends AbstractExportInVerzeichnis {

    public KoExportInVerzeichnis(WorkingSpreadsheet ws, Path zielVerzeichnis, ExportFormat format) {
        super(ws, TurnierSystem.KO, "Ko Export Verzeichnis", zielVerzeichnis, format);
    }

    @Override
    protected ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException {
        var ws = getWorkingSpreadsheet();
        var konfiguration = new KoKonfigurationSheet(ws);

        var xDoc = ws.getWorkingSpreadsheetDocument();
        String meldelisteSheetName = sheetNamePerSchluessel(SheetMetadataHelper.SCHLUESSEL_KO_MELDELISTE, SheetNamen.meldeliste());
        boolean meldelisteExportieren = konfiguration.isMeldelisteExportieren();
        String titel = StringUtils.defaultIfBlank(StringUtils.strip(konfiguration.getKopfZeileMitte()),
                TurnierSystem.KO.getBezeichnung());

        var einzelBaumSheet = SheetMetadataHelper.findeSheetUndHeile(xDoc,
                SheetMetadataHelper.schluesselKoTurnierbaum(""), SheetNamen.koTurnierbaumEinzel());
        List<SheetEintrag> gruppenSheets = buchstabenSheetEintraegePerSchluessel(
                SheetMetadataHelper::schluesselKoTurnierbaum, SheetNamen::koTurnierbaumGruppe);
        String einzelName = einzelBaumSheet != null ? Lo.qi(XNamed.class, einzelBaumSheet).getName() : null;

        if (getFormat().istEinDokument()) {
            List<ExportHtmlSeite.Section> sections = new ArrayList<>();
            if (meldelisteExportieren) {
                sections.add(new ExportHtmlSeite.Section("meldeliste", I18n.get("export.nav.meldeliste"), meldelisteSheetName, null));
            }
            if (einzelName != null) {
                sections.add(new ExportHtmlSeite.Section("ko-turnierbaum",
                        I18n.get("export.ko.nav.turnierbaum"), einzelName, null));
            }
            for (var eintrag : gruppenSheets) {
                sections.add(new ExportHtmlSeite.Section("ko-" + eintrag.buchstabe(),
                        I18n.get("export.ko.nav.turnierbaum.gruppe", eintrag.buchstabe()), eintrag.sheetName(), null));
            }
            if (einzelName == null && gruppenSheets.isEmpty()) {
                sections.add(new ExportHtmlSeite.Section("ko-turnierbaum",
                        I18n.get("export.ko.nav.turnierbaum"), SheetNamen.koTurnierbaumEinzel(), null));
            }
            processBox().info(I18n.get("export.info.ein.dokument", getFormat().anzeigeName()));
            Path dokument = exportiereEinDokument(zielVerzeichnis, "KO", titel, getFormat(), sections);
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

        if (einzelName != null) {
            Path pdf = exportierePdfAusHtml(einzelName, I18n.get("export.ko.nav.turnierbaum"), zielVerzeichnis);
            if (pdf != null) {
                exportierteDateien.add(pdf);
            }
            sections.add(new ExportHtmlSeite.Section("ko-turnierbaum",
                    I18n.get("export.ko.nav.turnierbaum"), einzelName, buildPdfUrl(pdf)));
        }

        for (var eintrag : gruppenSheets) {
            Path pdf = exportierePdfAusHtml(eintrag.sheetName(), I18n.get("export.ko.nav.turnierbaum.gruppe", eintrag.buchstabe()), zielVerzeichnis);
            if (pdf != null) {
                exportierteDateien.add(pdf);
            }
            sections.add(new ExportHtmlSeite.Section("ko-" + eintrag.buchstabe(),
                    I18n.get("export.ko.nav.turnierbaum.gruppe", eintrag.buchstabe()),
                    eintrag.sheetName(), buildPdfUrl(pdf)));
        }

        if (einzelName == null && gruppenSheets.isEmpty()) {
            sections.add(new ExportHtmlSeite.Section("ko-turnierbaum",
                    I18n.get("export.ko.nav.turnierbaum"), SheetNamen.koTurnierbaumEinzel(), null));
        }

        processBox().info(I18n.get("export.info.html"));
        exportiereHtmlMitMeldelisteDruckbereich(meldelisteExportieren, meldelisteSheetName,
                zielVerzeichnis, "KO.html", titel,
                StringUtils.strip(konfiguration.getTurnierlogoUrl()), sections)
                .addTo(exportierteDateien);

        return new ExportErgebnis(exportierteDateien);
    }
}
