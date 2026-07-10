package de.petanqueturniermanager.helper.upload;

import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jcraft.jsch.UserInfo;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;

/**
 * Fragt bei unbekanntem SFTP-Host-Key per Dialog nach.
 * <p>
 * {@code aufMainThread} MUSS angeben, ob der Aufrufer (also letztlich
 * {@code SftpUploadService.verbinde()}) bereits synchron auf dem LO-Main-Thread läuft
 * (z. B. Dialog-Button-Klick) oder auf einem Worker-Thread (z. B. {@code SheetRunner}).
 * Auf dem Main-Thread wird der Dialog direkt gezeigt; auf einem Worker-Thread wird per
 * {@link LoMainThread#post} marshallt. Ein Post+{@code future.get()} während der Aufrufer
 * selbst schon auf dem Main-Thread ist, wäre ein garantierter Deadlock.
 * <p>
 * {@code abgebrochen} wird sowohl vor dem Marshalling als auch erneut innerhalb des geposteten
 * Main-Thread-Runnables geprüft, damit keine Rückfrage/Meldung mehr erscheint, wenn der
 * aufrufende Dialog zwischenzeitlich geschlossen wurde (z. B. {@code FtpServerDetailDialog}).
 */
final class SftpHostKeyUserInfo implements UserInfo {

    private static final Logger logger = LogManager.getLogger(SftpHostKeyUserInfo.class);

    private final XComponentContext xContext;
    private final boolean aufMainThread;
    private final BooleanSupplier abgebrochen;
    private String passwort;

    SftpHostKeyUserInfo(XComponentContext xContext, boolean aufMainThread, BooleanSupplier abgebrochen) {
        this.xContext = xContext;
        this.aufMainThread = aufMainThread;
        this.abgebrochen = abgebrochen;
    }

    void setPasswort(String passwort) {
        this.passwort = passwort;
    }

    @Override
    public String getPassphrase() {
        return null;
    }

    @Override
    public String getPassword() {
        return passwort;
    }

    @Override
    public boolean promptPassword(String message) {
        return passwort != null && !passwort.isEmpty();
    }

    @Override
    public boolean promptPassphrase(String message) {
        return false;
    }

    @Override
    public boolean promptYesNo(String message) {
        if (abgebrochen.getAsBoolean()) {
            return false;
        }
        if (aufMainThread) {
            return zeigeJaNeinDialog(message);
        }
        var future = new CompletableFuture<Boolean>();
        LoMainThread.post(xContext,
                () -> future.complete(!abgebrochen.getAsBoolean() && zeigeJaNeinDialog(message)));
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            logger.warn("SFTP-Host-Key-Rückfrage konnte nicht beantwortet werden", e);
            return false;
        }
    }

    private boolean zeigeJaNeinDialog(String message) {
        try {
            MessageBoxResult result = MessageBox.from(xContext, MessageBoxTypeEnum.WARN_YES_NO)
                    .caption(I18n.get("upload.sftp.hostkey.dialog.titel"))
                    .message(I18n.get("upload.sftp.hostkey.dialog.frage", message))
                    .show();
            return result == MessageBoxResult.YES;
        } catch (Exception e) {
            logger.warn("SFTP-Host-Key-Rückfrage fehlgeschlagen", e);
            return false;
        }
    }

    @Override
    public void showMessage(String message) {
        Runnable zeigen = () -> {
            if (abgebrochen.getAsBoolean()) {
                return;
            }
            MessageBox.from(xContext, MessageBoxTypeEnum.WARN_OK)
                    .caption(I18n.get("upload.sftp.hostkey.dialog.titel"))
                    .message(message)
                    .show();
        };
        if (abgebrochen.getAsBoolean()) {
            return;
        }
        if (aufMainThread) {
            zeigen.run();
        } else {
            LoMainThread.post(xContext, zeigen);
        }
    }
}
