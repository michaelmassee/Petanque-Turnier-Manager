/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.auftrag;

import java.util.Locale;

import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Art eines Schreibauftrags an PTM-Online (Auftragsmatrix der Spezifikation). Jede Art zählt: sie erhält eine eigene
 * Auftrags-ID und den nächsten Schreibzähler des Dokuments. Verbinden und Übernehmen sind keine Aufträge – sie
 * erzeugen die Bindung erst und setzen den Zähler zurück.
 */
public enum AuftragsArt {

    /** Trennen der Dokumentbindung. */
    TRENNEN,
    /** Übergang des Online-Turniers zu {@code running} beim ersten Rundenstart. */
    START,
    /** Online-Anlage einer lokal erfassten Meldung über ihre lokale UUID. */
    ANMELDUNG_ANLEGEN,
    /** Namenskorrektur einer zugeordneten Meldung aus dem Dokument. */
    ANMELDUNG_AENDERN,
    /** Teilnahme (Check-in) und Setzposition zugeordneter Meldungen. */
    TEILNAHME,
    /** Paarungen und Ergebnisse einer Spielrunde für die Live-Ansicht. */
    RUNDE,
    /** Löschen einer lokal nicht mehr vorhandenen Spielrunde. */
    RUNDE_LOESCHEN,
    /** Ranglisten-Snapshot für die Live-Ansicht. */
    RANGLISTE,
    /** Vollständige Zuordnung der Mêlée-Einzelanmeldungen zu den lokal gemischten Teams (KP-18). */
    MELEE_TEAMS,
    /** Online-Stornierung einer lokal gelöschten, verknüpften Meldung (KP-15). */
    ANMELDUNG_STORNIEREN,
    /** Protokoll der Entscheidungen der Turnierleitung aus der Konfliktliste (A-29). */
    ENTSCHEIDUNGEN;

    /**
     * Noch nicht gesendete Aufträge dieser Art aus der Zeit vor dem lokalen Turnierstart werden beim Start verworfen
     * (KP-05): Anlage und Teilnahme aus der Anmeldephase sind danach überholt, den vollständigen Stand überträgt ein
     * neuer Teilnahme-Auftrag nach {@code running}.
     */
    public boolean wirdBeimStartVerworfen() {
        return this == ANMELDUNG_ANLEGEN || this == ANMELDUNG_AENDERN || this == TEILNAHME;
    }

    public String anzeige() {
        return I18n.get("ptmonline.auftrag.art." + name().toLowerCase(Locale.ROOT));
    }

    /** Nur der Übergang zu {@code running} darf auch bei pausiertem Sync gesendet werden (KP-05, P-58). */
    public boolean inPauseErlaubt() {
        return this == START;
    }
}
