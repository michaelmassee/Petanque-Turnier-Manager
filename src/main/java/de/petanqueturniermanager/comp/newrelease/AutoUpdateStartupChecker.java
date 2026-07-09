/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.LoMainThread;

/**
 * Prüft nach jeder Statusänderung von {@link ReleaseUpdateService}, ob der
 * automatische Update-Dialog beim Startup gezeigt werden soll, und zeigt ihn
 * bei Bedarf genau einmal pro LO-Session an.
 *
 * <p>Wird in {@code PetanqueTurnierMngrSingleton.init()} als Statuslistener bei
 * {@link ReleaseUpdateService#addStatusListener(Runnable)} registriert. Der
 * Listener feuert auf dem Hintergrund-Executor-Thread von {@code ReleaseUpdateService}
 * – jegliche UI-Arbeit wird per {@link LoMainThread#post} auf den LO-Main-Thread
 * marshallt (siehe CLAUDE.md Threading-Regeln).
 */
public final class AutoUpdateStartupChecker {

    private static final Logger logger = LogManager.getLogger(AutoUpdateStartupChecker.class);

    private static final AtomicBoolean bereitsGezeigt = new AtomicBoolean(false);

    private final XComponentContext context;

    public AutoUpdateStartupChecker(XComponentContext context) {
        this.context = context;
    }

    /** Test-Hook: erlaubt es, den "bereits gezeigt"-Zustand zwischen Tests zurückzusetzen. */
    static void resetFuerTest() {
        bereitsGezeigt.set(false);
    }

    /**
     * Prüft die Anzeige-Bedingungen. Läuft auf dem aufrufenden Thread (i. d. R.
     * der {@code ptm-release-check}-Hintergrund-Thread von {@link ReleaseUpdateService}).
     */
    public void pruefeUndZeige() {
        if (!sollDialogGezeigtWerden()) {
            return;
        }
        if (!bereitsGezeigt.compareAndSet(false, true)) {
            return;
        }
        LoMainThread.post(context, this::zeigeDialogAufMainThread);
    }

    boolean sollDialogGezeigtWerden() {
        var service = releaseUpdateServiceOderNull();
        if (service == null || !service.isUpdateVerfuegbar()) {
            return false;
        }
        if (!GlobalProperties.get().isAutoUpdateDialogBeimStartAktiv()) {
            return false;
        }
        var neuesteVersion = service.getNeuesteVersionTag().orElse(null);
        if (neuesteVersion == null) {
            return false;
        }
        return GlobalProperties.get().getUpdateSkipVersion()
                .map(skip -> !skip.equals(neuesteVersion))
                .orElse(true);
    }

    private static @Nullable ReleaseUpdateService releaseUpdateServiceOderNull() {
        try {
            return ReleaseUpdateService.get();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private void zeigeDialogAufMainThread() {
        var service = releaseUpdateServiceOderNull();
        if (service == null || !service.isUpdateVerfuegbar()) {
            return;
        }
        var release = service.getAktuellesRelease().orElse(null);
        if (release == null) {
            return;
        }
        var installierteVersion = service.getInstallierteVersion().orElse("?");

        var ws = new WorkingSpreadsheet(context);
        UpdateVerfuegbarDialog.Aktion aktion;
        try {
            aktion = UpdateVerfuegbarDialog.zeigen(context, release, installierteVersion, ws.getContainerWindowPeer());
        } catch (Exception e) {
            logger.error("Fehler beim Anzeigen des Update-Verfügbar-Dialogs", e);
            return;
        }
        switch (aktion) {
            case UPDATE -> new DirectUpdate(ws).start();
            case NICHT_MEHR_FUER_VERSION -> GlobalProperties.get().setUpdateSkipVersion(release.tagName());
            case ABBRUCH -> { /* keine Aktion */ }
        }
    }
}
