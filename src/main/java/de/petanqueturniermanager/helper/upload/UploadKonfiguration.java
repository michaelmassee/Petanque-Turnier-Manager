package de.petanqueturniermanager.helper.upload;

import org.apache.commons.lang3.StringUtils;

public record UploadKonfiguration(UploadProtokoll protokoll, String host, int port, String benutzer,
        String remotePfad) {

    public boolean istVollstaendig() {
        return protokoll != null
                && StringUtils.isNotBlank(host)
                && StringUtils.isNotBlank(benutzer)
                && StringUtils.isNotBlank(remotePfad);
    }
}
