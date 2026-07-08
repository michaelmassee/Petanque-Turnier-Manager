package de.petanqueturniermanager.helper.upload;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface IUploadService {

    int hochladen(List<Path> dateien, String passwort) throws IOException;

    /**
     * Baut nur die Verbindung auf und meldet sich an (kein Upload). Wirft eine
     * {@link IOException} mit einer für den Nutzer verständlichen Meldung, wenn Host, Port,
     * Benutzer oder Passwort nicht funktionieren.
     */
    void testeVerbindung(String passwort) throws IOException;
}
