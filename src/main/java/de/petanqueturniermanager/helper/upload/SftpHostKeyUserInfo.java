package de.petanqueturniermanager.helper.upload;

import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jcraft.jsch.UserInfo;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;

final class SftpHostKeyUserInfo implements UserInfo {

    private static final Logger logger = LogManager.getLogger(SftpHostKeyUserInfo.class);

    private final WorkingSpreadsheet ws;
    private String passwort;

    SftpHostKeyUserInfo(WorkingSpreadsheet ws) {
        this.ws = ws;
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
        var future = new CompletableFuture<Boolean>();
        LoMainThread.post(ws.getxContext(), () -> {
            try {
                MessageBoxResult result = MessageBox.from(ws, MessageBoxTypeEnum.WARN_YES_NO)
                        .caption(I18n.get("upload.sftp.hostkey.dialog.titel"))
                        .message(I18n.get("upload.sftp.hostkey.dialog.frage", message))
                        .show();
                future.complete(result == MessageBoxResult.YES);
            } catch (Exception e) {
                logger.warn("SFTP-Host-Key-Rückfrage fehlgeschlagen", e);
                future.complete(false);
            }
        });
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

    @Override
    public void showMessage(String message) {
        LoMainThread.post(ws.getxContext(), () -> MessageBox.from(ws, MessageBoxTypeEnum.WARN_OK)
                .caption(I18n.get("upload.sftp.hostkey.dialog.titel"))
                .message(message)
                .show());
    }
}
