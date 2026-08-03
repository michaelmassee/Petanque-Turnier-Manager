/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.pagestyle;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

/**
 * Löst Platzhalter-Tokens (siehe {@link HeaderFooterPlatzhalter}) in einem Kopf-/Fußzeilen-
 * Konfigurationstext direkt zu echten, aktuellen String-Werten auf.
 *
 * <p>Wird von der Live-Webserver-Ansicht ({@code TabellenMapper}) verwendet, da
 * {@code XHeaderFooterContent.getXXXText().getString()} bei UNO-Textfeldern nur LO-interne
 * Dummy-Werte liefert (siehe {@code ScHeaderFooterTextObj::FillDummyFieldData}: "???"/"1"/"99"),
 * solange kein echter Druckvorgang läuft. Der native Calc-PageStyle selbst wird davon nicht
 * berührt – dort übernehmen echte UNO-Textfelder (siehe {@link PageStyleDef}) die Auflösung
 * beim Drucken/PDF-Export.
 *
 * <p>{@code SEITE}/{@code SEITEN} ergeben auf der fortlaufenden Web-Ansicht ohne Seitenumbrüche
 * fachlich keinen sinnvollen Wert und werden daher durch einen leeren String ersetzt.
 */
public final class HeaderFooterTokenResolver {

	private HeaderFooterTokenResolver() {
	}

	public static String resolve(String text, String tabellenName, String dateiname) {
		if (!HeaderFooterPlatzhalter.enthaeltPlatzhalter(text)) {
			return text;
		}

		StringBuilder ergebnis = new StringBuilder();
		List<HeaderFooterTextSegment> segmente = HeaderFooterTokenParser.parse(text);
		for (HeaderFooterTextSegment segment : segmente) {
			switch (segment) {
			case HeaderFooterTextSegment.Literal literal -> ergebnis.append(literal.text());
			case HeaderFooterTextSegment.Platzhalter platzhalterSegment ->
					ergebnis.append(aufloesen(platzhalterSegment.platzhalter(), tabellenName, dateiname));
			}
		}
		return ergebnis.toString();
	}

	private static String aufloesen(HeaderFooterPlatzhalter platzhalter, String tabellenName, String dateiname) {
		return switch (platzhalter) {
		case DATUM -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(LocalDate.now());
		case UHRZEIT -> DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(LocalTime.now());
		case TABELLE -> tabellenName == null ? "" : tabellenName;
		case DATEINAME -> dateiname == null ? "" : dateiname;
		// Auf der fortlaufenden Web-Ansicht ohne Seitenumbrüche fachlich nicht sinnvoll auflösbar
		case SEITE, SEITEN -> "";
		};
	}
}
