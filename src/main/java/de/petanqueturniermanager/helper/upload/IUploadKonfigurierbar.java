package de.petanqueturniermanager.helper.upload;

public interface IUploadKonfigurierbar {

    UploadProtokoll getUploadProtokoll();

    String getUploadHost();

    int getUploadPort();

    String getUploadBenutzer();

    String getUploadVerzeichnis();
}
