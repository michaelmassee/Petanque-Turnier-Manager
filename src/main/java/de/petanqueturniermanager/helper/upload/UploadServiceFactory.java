package de.petanqueturniermanager.helper.upload;

public final class UploadServiceFactory {

    private UploadServiceFactory() {
    }

    public static IUploadService erstelle(UploadKonfiguration konfiguration) {
        return switch (konfiguration.protokoll()) {
            case FTP -> new FtpUploadService(konfiguration);
            case SFTP -> new SftpUploadService(konfiguration);
        };
    }
}
