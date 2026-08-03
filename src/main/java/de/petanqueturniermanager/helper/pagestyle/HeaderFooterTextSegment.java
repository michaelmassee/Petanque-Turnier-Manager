/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.pagestyle;

/**
 * Ein Segment eines Kopf-/Fußzeilen-Konfigurationstexts nach dem Parsen durch
 * {@link HeaderFooterTokenParser}: entweder statischer Text oder ein Platzhalter-Token.
 */
public sealed interface HeaderFooterTextSegment {

	record Literal(String text) implements HeaderFooterTextSegment {
	}

	record Platzhalter(HeaderFooterPlatzhalter platzhalter) implements HeaderFooterTextSegment {
	}
}
