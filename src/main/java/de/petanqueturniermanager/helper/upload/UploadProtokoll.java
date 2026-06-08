package de.petanqueturniermanager.helper.upload;

public enum UploadProtokoll {
    FTP, SFTP;

    public static UploadProtokoll vonString(String wert) {
        if (wert == null || wert.isBlank()) {
            return FTP;
        }
        try {
            return valueOf(wert.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return FTP;
        }
    }
}
