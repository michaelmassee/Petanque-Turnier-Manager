package de.petanqueturniermanager.webserver;

import java.util.Locale;

/**
 * Zentrale Ermittlung des HTTP-{@code Content-Type} anhand der Dateiendung für die
 * eingebetteten Webserver-Instanzen. Ersetzt die zuvor in mehreren Server-Klassen
 * ({@code CompositeViewInstanz}, {@code WebserverRegieServerInstanz},
 * {@code TurnierStartseiteWebServerInstanz}) divergent duplizierte
 * {@code ermittleContentType}-Logik durch eine case-insensitive Obermenge.
 */
final class WebContentType {

	/** Content-Type für HTML-Antworten (UTF-8). */
	static final String HTML = "text/html; charset=UTF-8";

	private WebContentType() {
		// Utility-Klasse — keine Instanzen.
	}

	/**
	 * Liefert den Content-Type zur Dateiendung von {@code dateiname}. Groß-/Kleinschreibung
	 * der Endung wird ignoriert; für unbekannte Endungen wird {@code application/octet-stream}
	 * zurückgegeben.
	 *
	 * @param dateiname Dateiname oder Pfad, z. B. {@code "logo.PNG"} oder {@code "/assets/app.js"}
	 * @return zugehöriger Content-Type, nie {@code null}
	 */
	static String fuerDateiname(String dateiname) {
		int punkt = dateiname.lastIndexOf('.');
		String endung = punkt >= 0 ? dateiname.substring(punkt + 1).toLowerCase(Locale.ROOT) : "";
		return switch (endung) {
			case "js" -> "text/javascript; charset=UTF-8";
			case "css" -> "text/css; charset=UTF-8";
			case "html" -> HTML;
			case "svg" -> "image/svg+xml";
			case "png" -> "image/png";
			case "jpg", "jpeg" -> "image/jpeg";
			case "gif" -> "image/gif";
			case "webp" -> "image/webp";
			case "ico" -> "image/x-icon";
			case "wav" -> "audio/wav";
			case "pdf" -> "application/pdf";
			case "txt" -> "text/plain; charset=UTF-8";
			default -> "application/octet-stream";
		};
	}
}
