/*
 * Erstellung : 12.10.2026 / Michael Massee
 **/

package de.petanqueturniermanager.exception;

/**
 * Das Dokument stammt aus einer Plugin-Version, in der die Teamnummer selbst die Identität war (keine UUID in der
 * Nr-Zelle). Solche Dokumente werden nicht umgestellt; die Verarbeitung bricht ab.
 */
@SuppressWarnings("serial")
public class AltesDokumentException extends GenerateException {

	public AltesDokumentException(String msg) {
		super(msg, true);
	}
}
