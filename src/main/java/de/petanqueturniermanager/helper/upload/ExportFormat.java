package de.petanqueturniermanager.helper.upload;

import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Ausgabeformat für den Verzeichnis-Export: entweder das klassische
 * HTML + Einzel-PDFs, oder ein einziges kombiniertes Dokument in einem
 * der unterstützten Formate.
 */
public enum ExportFormat {

    HTML_UND_PDFS("export.format.html.pdfs", null),
    EIN_DOKUMENT_PDF("export.format.ein.dokument.pdf", "pdf"),
    EIN_DOKUMENT_DOCX("export.format.ein.dokument.docx", "docx"),
    EIN_DOKUMENT_ODT("export.format.ein.dokument.odt", "odt"),
    EIN_DOKUMENT_MD("export.format.ein.dokument.md", "md");

    private final String anzeigeKey;
    private final String dateiEndung;

    ExportFormat(String anzeigeKey, String dateiEndung) {
        this.anzeigeKey = anzeigeKey;
        this.dateiEndung = dateiEndung;
    }

    public String anzeigeName() {
        return I18n.get(anzeigeKey);
    }

    public String dateiEndung() {
        return dateiEndung;
    }

    public boolean istEinDokument() {
        return dateiEndung != null;
    }
}
