package de.petanqueturniermanager.helper.upload;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

public record UploadKonfiguration(UploadProtokoll protokoll, String host, int port, String benutzer,
        String remotePfad) {

    public static int portOderStandard(String port, UploadProtokoll protokoll) {
        UploadProtokoll effektivesProtokoll = protokoll != null ? protokoll : UploadProtokoll.FTP;
        if (StringUtils.isBlank(port)) {
            return effektivesProtokoll.standardPort();
        }
        int portNummer = NumberUtils.toInt(port.trim(), -1);
        return portNummer > 0 ? portNummer : effektivesProtokoll.standardPort();
    }

    public boolean istVollstaendig() {
        return protokoll != null
                && StringUtils.isNotBlank(host)
                && StringUtils.isNotBlank(benutzer)
                && StringUtils.isNotBlank(remotePfad);
    }
}
