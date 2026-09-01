/*
* Erstellung : 28.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.exception;

@SuppressWarnings("serial")
public class GenerateException extends Exception {

	private final boolean nurWarnung;

	public GenerateException(String msg) {
		this(msg, false);
	}

	/**
	 * @param nurWarnung {@code true}, wenn diese Exception eine erwartbare Eingabe-Validierung
	 *                   signalisiert (der Anwender muss selbst etwas korrigieren), statt einen
	 *                   echten Plugin-Fehler. Der zentrale Handler
	 *                   ({@code SheetRunner.handleGenerateException}) protokolliert solche Fälle
	 *                   dann nur als Warnung statt als ERROR, zeigt dem Anwender die Meldung aber
	 *                   unverändert an.
	 */
	public GenerateException(String msg, boolean nurWarnung) {
		super(msg);
		this.nurWarnung = nurWarnung;
	}

	public boolean istNurWarnung() {
		return nurWarnung;
	}
}
