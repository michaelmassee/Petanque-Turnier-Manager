package de.petanqueturniermanager.helper.upload;

import java.io.IOException;

@SuppressWarnings("serial")
class AnmeldeFehlgeschlagenException extends IOException {

    AnmeldeFehlgeschlagenException(String message) {
        super(message);
    }

    AnmeldeFehlgeschlagenException(String message, Throwable cause) {
        super(message, cause);
    }
}
