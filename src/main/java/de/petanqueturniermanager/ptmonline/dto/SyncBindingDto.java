package de.petanqueturniermanager.ptmonline.dto;

/**
 * Serverbestätigte exklusive Bindung eines PTM-Dokuments an ein Online-Turnier.
 *
 * @param writeCounter Schreibzähler der Bindung laut Server: 0 bei neuer Bindung, bei der Wiederholung derselben
 *                     Verbindung der aktuelle Stand (Spezifikation P-22)
 */
public record SyncBindingDto(boolean ok, String syncDocumentId, long bindingRevision, long writeCounter) {

    /** Bindung ohne übermittelten Zählerstand (neue Bindung, Zähler 0). */
    public SyncBindingDto(boolean ok, String syncDocumentId, long bindingRevision) {
        this(ok, syncDocumentId, bindingRevision, 0);
    }
}
