/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.pagestyle;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Zerlegt einen Kopf-/Fußzeilen-Konfigurationstext (z.B. {@code "Seite {SEITE} von {SEITEN}"})
 * in eine Folge aus Literal- und Platzhalter-Segmenten ({@link HeaderFooterTextSegment}).
 *
 * <p>Unbekannte {@code {...}}-Klammerausdrücke (kein registriertes {@link HeaderFooterPlatzhalter}-Token)
 * werden als normaler Literal-Text behandelt.
 */
public final class HeaderFooterTokenParser {

	private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{[A-ZÄÖÜ]+\\}");

	private HeaderFooterTokenParser() {
	}

	public static List<HeaderFooterTextSegment> parse(String text) {
		List<HeaderFooterTextSegment> segmente = new ArrayList<>();
		if (text == null || text.isEmpty()) {
			return segmente;
		}

		Matcher matcher = TOKEN_PATTERN.matcher(text);
		int letztesEnde = 0;
		StringBuilder literalPuffer = new StringBuilder();

		while (matcher.find()) {
			literalPuffer.append(text, letztesEnde, matcher.start());
			letztesEnde = matcher.end();

			HeaderFooterPlatzhalter platzhalter = HeaderFooterPlatzhalter.vonToken(matcher.group());
			if (platzhalter == null) {
				// unbekannter Klammerausdruck: als Literal behandeln
				literalPuffer.append(matcher.group());
				continue;
			}

			if (literalPuffer.length() > 0) {
				segmente.add(new HeaderFooterTextSegment.Literal(literalPuffer.toString()));
				literalPuffer.setLength(0);
			}
			segmente.add(new HeaderFooterTextSegment.Platzhalter(platzhalter));
		}

		literalPuffer.append(text.substring(letztesEnde));
		if (literalPuffer.length() > 0) {
			segmente.add(new HeaderFooterTextSegment.Literal(literalPuffer.toString()));
		}

		return segmente;
	}
}
