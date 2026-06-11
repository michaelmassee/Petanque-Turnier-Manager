package de.petanqueturniermanager.helper.upload;

public enum UploadProtokoll {
    FTP(21), SFTP(22);

    private final int standardPort;

    UploadProtokoll(int standardPort) {
        this.standardPort = standardPort;
    }

    public int standardPort() {
        return standardPort;
    }

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
