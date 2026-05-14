/**
 * Plugin-Versionserkennung und GitHub-Update-Check.
 *
 * <p>
 * Komponenten:
 * <ul>
 *   <li>{@link de.petanqueturniermanager.comp.newrelease.InstallierteVersion} – installierte
 *       Plugin-Version aus dem LibreOffice-Extension-Manager.</li>
 *   <li>{@link de.petanqueturniermanager.comp.newrelease.GithubReleaseClient} – HTTP-Zugriff
 *       auf die GitHub-Release-API (mit Timeouts).</li>
 *   <li>{@link de.petanqueturniermanager.comp.newrelease.ReleaseCache} – persistenter
 *       JSON-Cache der zuletzt abgerufenen Release-Info.</li>
 *   <li>{@link de.petanqueturniermanager.comp.newrelease.VersionVergleicher} – reine
 *       Vergleichslogik inklusive Pre-Release-Behandlung.</li>
 *   <li>{@link de.petanqueturniermanager.comp.newrelease.ReleaseUpdateService} – Singleton,
 *       der Cache+Client+Vergleicher orchestriert und Statuslistener verteilt.</li>
 * </ul>
 */
@org.jspecify.annotations.NullMarked
package de.petanqueturniermanager.comp.newrelease;
