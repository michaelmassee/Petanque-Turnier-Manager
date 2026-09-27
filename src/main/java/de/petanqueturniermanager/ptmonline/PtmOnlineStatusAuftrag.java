/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.List;

import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.ptmonline.dto.NeueOnlineAnmeldung;

/**
 * Momentaufnahme des Status-Abgleichs beim Rundenstart, lokal im SheetRunner erstellt und danach im Hintergrund
 * gesendet (siehe {@link PtmOnlineStatusAbgleich}). Enthält alles, was aus dem Dokument gelesen werden muss, damit
 * das Senden das Dokument nicht mehr braucht.
 *
 * @param turnierStarten {@code true} bei der ersten Spielrunde: das Online-Turnier wird gestartet
 * @param eintraege      je lokaler Meldung Teilnahme, Setzposition und ggf. Daten für die Online-Anlage
 */
record PtmOnlineStatusAuftrag(String tournamentId, boolean turnierStarten, List<Eintrag> eintraege) {

    PtmOnlineStatusAuftrag {
        eintraege = List.copyOf(eintraege);
    }

    /**
     * Neuere Momentaufnahme ersetzt eine noch nicht gesendete ältere; ein dort angeforderter Turnierstart bleibt
     * erhalten.
     */
    PtmOnlineStatusAuftrag ersetzt(@Nullable PtmOnlineStatusAuftrag aelter) {
        if (aelter == null || turnierStarten || !aelter.turnierStarten()) {
            return this;
        }
        return new PtmOnlineStatusAuftrag(tournamentId, true, eintraege);
    }

    /**
     * @param lokaleUuid     dauerhafte Kennung der Meldelistenzeile
     * @param seedingPosition lokale Setzposition, {@code null} = keine (löscht die online gepflegte)
     * @param neueAnlage     Daten für die Online-Anlage einer vor Ort erfassten, aktiven Meldung ohne Zuordnung
     */
    record Eintrag(String lokaleUuid, OnlineTeilnahme teilnahme, @Nullable Integer seedingPosition,
            @Nullable NeueAnlage neueAnlage) {
    }

    /**
     * @param nummerFormel Formel der Team-/Spielernummer für das Blatt „PTMOnline Sync“
     */
    record NeueAnlage(NeueOnlineAnmeldung anmeldung, String bezeichnung, String nummerFormel) {
    }
}
