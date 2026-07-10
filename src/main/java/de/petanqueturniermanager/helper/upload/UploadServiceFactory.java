package de.petanqueturniermanager.helper.upload;

import java.util.function.BooleanSupplier;

import com.sun.star.uno.XComponentContext;

public final class UploadServiceFactory {

    private UploadServiceFactory() {
    }

    /**
     * Strikter Modus: verlangt einen bereits bekannten Host-Key in {@code known_hosts},
     * ohne Rückfrage-Möglichkeit. Nur für Kontexte ohne UI-Rückfragemöglichkeit.
     */
    public static IUploadService erstelle(UploadKonfiguration konfiguration) {
        return switch (konfiguration.protokoll()) {
            case FTP -> new FtpUploadService(konfiguration);
            case SFTP -> new SftpUploadService(konfiguration);
        };
    }

    /**
     * Interaktiver Modus: fragt bei unbekanntem Host-Key per Dialog nach und legt
     * {@code known_hosts} bei Bedarf automatisch an.
     *
     * @param aufMainThread {@code true}, wenn der Aufrufer selbst bereits synchron auf dem
     *                      LO-Main-Thread läuft (z. B. Dialog-Button-Klick) — sonst {@code false}
     *                      (Worker-Thread, z. B. {@code SheetRunner}). Siehe {@link SftpHostKeyUserInfo}.
     */
    public static IUploadService erstelle(UploadKonfiguration konfiguration, XComponentContext xContext,
            boolean aufMainThread) {
        return erstelle(konfiguration, xContext, aufMainThread, () -> false);
    }

    /**
     * Wie {@link #erstelle(UploadKonfiguration, XComponentContext, boolean)}, aber mit
     * Abbruch-Signal für Aufrufer mit eigenem UI-Lebenszyklus (z. B. ein Dialog, den der Nutzer
     * während eines laufenden Verbindungstests schließen kann).
     *
     * @param abgebrochen liefert {@code true}, sobald Host-Key-Rückfragen/-Meldungen unterdrückt
     *                    werden sollen, weil der aufrufende UI-Kontext nicht mehr existiert.
     */
    public static IUploadService erstelle(UploadKonfiguration konfiguration, XComponentContext xContext,
            boolean aufMainThread, BooleanSupplier abgebrochen) {
        return switch (konfiguration.protokoll()) {
            case FTP -> new FtpUploadService(konfiguration);
            case SFTP -> new SftpUploadService(konfiguration,
                    new SftpHostKeyUserInfo(xContext, aufMainThread, abgebrochen));
        };
    }
}
