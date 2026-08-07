package de.petanqueturniermanager.comp;

/**
 * Fehler beim Importieren von Composite-View-Konfigurationen aus einer JSON-Datei
 * (z. B. ungültiges oder leeres JSON).
 */
public class CompositeViewImportException extends Exception {

	private static final long serialVersionUID = 1L;

	public CompositeViewImportException(String message) {
		super(message);
	}

	public CompositeViewImportException(String message, Throwable cause) {
		super(message, cause);
	}
}
