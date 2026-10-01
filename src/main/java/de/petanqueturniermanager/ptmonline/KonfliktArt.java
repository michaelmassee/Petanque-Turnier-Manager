/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.Locale;

import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Art eines offenen Falls der Konfliktliste (A-29). Ein Ausschlussgrund hält den ersten Rundenstart an
 * (Vorabcheck), ein reiner Hinweis nicht.
 */
public enum KonfliktArt {

    /** Lokal und online gleichnamig erfasst, noch nicht verknüpft (KP-06 a2). */
    MOEGLICH_IDENTISCH(true),
    /** Namen oder Besetzung lokal und online unterschiedlich geändert (KP-16, KP-20). */
    NAMENSKONFLIKT(false),
    /** Online storniert oder auf der Warteliste, lokal von der Auslosung ausgeschlossen (KP-14). */
    ONLINE_AUSGESCHLOSSEN(true),
    /** Verknüpfte Meldung fehlt lokal, ohne dass über die Online-Wirkung entschieden wurde (KP-15). */
    LOKAL_FEHLEND(false),
    /** Nach dem lokalen Turnierstart online eingegangen (KP-05). */
    NACH_START(false),
    /** Dieselbe Person mit Konto in mehreren Anmeldungen (KP-06 b). */
    KONTO_KONFLIKT(true),
    /** Team mit weniger Personen als die Formation (E-20). */
    UNVOLLSTAENDIG(true),
    /** Gleicher Name oder gleiche E-Mail ohne Konto, nur ein Hinweis (KP-06 b'). */
    MOEGLICHE_DUBLETTE(false),
    /** Anmeldung oder Meldung, die nicht übertragen werden konnte. */
    NICHT_UEBERTRAGEN(false);

    private final boolean ausschlussgrund;

    KonfliktArt(boolean ausschlussgrund) {
        this.ausschlussgrund = ausschlussgrund;
    }

    public boolean istAusschlussgrund() {
        return ausschlussgrund;
    }

    public String anzeige() {
        return I18n.get("ptmonline.konflikt.art." + name().toLowerCase(Locale.ROOT));
    }
}
