/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

/**
 * Beschreibt ein einzelnes Release-Asset (Datei) eines GitHub-Releases.
 *
 * @param name Dateiname des Assets, z.B. {@code PetanqueTurnierManager-1.2.3.oxt}.
 * @param downloadUrl Direkt-Download-URL ({@code browser_download_url}) des Assets.
 */
public record AssetInfo(String name, String downloadUrl) {
}
