/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

/**
 * Ermittelt die URL, die der Menüpunkt „Release-Seite öffnen" im Standardbrowser
 * öffnen soll.
 */
public final class ReleaseSeiteUrl {

    static final String FALLBACK_URL = "https://github.com/" + ReleaseUpdateService.GITHUB_REPOSITORY + "/releases";

    private ReleaseSeiteUrl() {
    }

    /**
     * Liefert die Seite des zuletzt bekannten Releases ({@link ReleaseInfo#htmlUrl()}),
     * falls vorhanden. Fällt andernfalls (kein Release bekannt, {@link ReleaseUpdateService}
     * noch nicht initialisiert, oder Hintergrund-Fetch noch nicht abgeschlossen) auf die
     * allgemeine GitHub-Releases-Übersicht des Repositories zurück – der Menüpunkt öffnet
     * so immer etwas Sinnvolles.
     */
    public static String ermitteln() {
        try {
            return ReleaseUpdateService.get().getAktuellesRelease()
                    .map(ReleaseInfo::htmlUrl)
                    .filter(url -> url != null && !url.isBlank())
                    .orElse(FALLBACK_URL);
        } catch (IllegalStateException e) {
            return FALLBACK_URL;
        }
    }
}
