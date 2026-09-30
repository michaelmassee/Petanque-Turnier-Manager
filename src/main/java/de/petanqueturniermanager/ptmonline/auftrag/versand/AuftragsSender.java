/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.auftrag.versand;

import java.io.IOException;

import de.petanqueturniermanager.ptmonline.auftrag.SyncAuftrag;

/** Überträgt einen Schreibauftrag samt Auftrags-ID und Schreibzähler an PTM-Online. */
@FunctionalInterface
public interface AuftragsSender {

    /**
     * @return die Antwort, auch bei Ablehnung (4xx/5xx)
     * @throws IOException nur, wenn PTM-Online nicht erreichbar war (Netz, Zeitlimit)
     */
    SyncAntwort sende(SyncAuftrag auftrag) throws IOException, InterruptedException;
}
