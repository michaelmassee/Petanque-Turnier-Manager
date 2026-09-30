/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.auftrag;

import java.util.Objects;

/**
 * Unveränderlicher, fertig serialisierter Schreibauftrag an PTM-Online (T-09, T-23). Er entsteht im Dokument-Kontext
 * aus einem Snapshot, wird mit dem Dokument gespeichert und erst danach gesendet. Eine Wiederholung – auch nach einem
 * Neustart von LibreOffice – sendet ihn unverändert mit derselben Auftrags-ID und demselben Schreibzähler; PTM-Online
 * liefert dann die gespeicherte Antwort, statt ein zweites Mal zu schreiben.
 *
 * @param auftragsId UUID des Auftrags (Header {@code X-PTM-Request-Id})
 * @param zaehler    Schreibzähler des Dokuments (Header {@code X-PTM-Sync-Counter}), streng steigend
 * @param methode    HTTP-Methode
 * @param pfad       Pfad ab {@code /api/...}
 * @param body       JSON-Nutzlast; Teil des Idempotenz-Hashs und daher nie neu serialisiert
 * @param kontext    JSON mit lokalen Angaben, die das Ergebnis im Dokument braucht (z.&nbsp;B. lokale UUID); wird
 *                   nicht gesendet
 */
public record SyncAuftrag(String auftragsId, long zaehler, AuftragsArt art, String methode, String pfad, String body,
        String kontext) {

    public SyncAuftrag {
        Objects.requireNonNull(auftragsId, "auftragsId");
        Objects.requireNonNull(art, "art");
        Objects.requireNonNull(methode, "methode");
        Objects.requireNonNull(pfad, "pfad");
        body = body == null ? "" : body;
        kontext = kontext == null || kontext.isBlank() ? "{}" : kontext;
        if (zaehler < 1) {
            throw new IllegalArgumentException("Schreibzähler muss positiv sein: " + zaehler);
        }
    }
}
