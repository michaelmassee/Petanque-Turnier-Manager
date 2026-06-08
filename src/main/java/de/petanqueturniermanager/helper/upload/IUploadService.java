package de.petanqueturniermanager.helper.upload;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface IUploadService {

    int hochladen(List<Path> dateien, String passwort) throws IOException;
}
