package de.petanqueturniermanager.helper.upload;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

public final class UploadServiceFactory {

    private UploadServiceFactory() {
    }

    public static IUploadService erstelle(UploadKonfiguration konfiguration) {
        return switch (konfiguration.protokoll()) {
            case FTP -> new FtpUploadService(konfiguration);
            case SFTP -> new SftpUploadService(konfiguration);
        };
    }

    public static IUploadService erstelle(UploadKonfiguration konfiguration, WorkingSpreadsheet ws) {
        return switch (konfiguration.protokoll()) {
            case FTP -> new FtpUploadService(konfiguration);
            case SFTP -> new SftpUploadService(konfiguration, new SftpHostKeyUserInfo(ws));
        };
    }
}
