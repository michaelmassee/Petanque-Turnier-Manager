/*
 * Erstellung : 12.10.2026 / Michael Massee
 **/

package de.petanqueturniermanager.exception;

/**
 * Das Dokument stammt aus einer Plugin-Version, von der aus keine Migration möglich ist (siehe
 * {@code DokumentKompatibilitaet}). Es wird nicht mehr aktualisiert; die Verarbeitung bricht ab.
 */
@SuppressWarnings("serial")
public class AltesDokumentException extends GenerateException {

	public AltesDokumentException(String msg) {
		super(msg, true);
	}
}
