package de.petanqueturniermanager.helper.pagestyle;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

public class HeaderFooterTokenParserTest {

	@Test
	public void reinerLiteralTextOhnePlatzhalter() {
		List<HeaderFooterTextSegment> segmente = HeaderFooterTokenParser.parse("* Pétanque-Turnier-Manager *");

		assertThat(segmente).containsExactly(
				new HeaderFooterTextSegment.Literal("* Pétanque-Turnier-Manager *"));
	}

	@Test
	public void einzelnerPlatzhalterOhneLiteral() {
		List<HeaderFooterTextSegment> segmente = HeaderFooterTokenParser.parse("{DATUM}");

		assertThat(segmente).containsExactly(
				new HeaderFooterTextSegment.Platzhalter(HeaderFooterPlatzhalter.DATUM));
	}

	@Test
	public void gemischterTextMitMehrerenPlatzhaltern() {
		List<HeaderFooterTextSegment> segmente = HeaderFooterTokenParser.parse("Seite {SEITE} von {SEITEN}");

		assertThat(segmente).containsExactly(
				new HeaderFooterTextSegment.Literal("Seite "),
				new HeaderFooterTextSegment.Platzhalter(HeaderFooterPlatzhalter.SEITE),
				new HeaderFooterTextSegment.Literal(" von "),
				new HeaderFooterTextSegment.Platzhalter(HeaderFooterPlatzhalter.SEITEN));
	}

	@Test
	public void unbekannterKlammerausdruckBleibtLiteral() {
		List<HeaderFooterTextSegment> segmente = HeaderFooterTokenParser.parse("{UNBEKANNT} Text");

		assertThat(segmente).containsExactly(
				new HeaderFooterTextSegment.Literal("{UNBEKANNT} Text"));
	}

	@Test
	public void leererTextLiefertLeereListe() {
		assertThat(HeaderFooterTokenParser.parse("")).isEmpty();
		assertThat(HeaderFooterTokenParser.parse(null)).isEmpty();
	}

	@Test
	public void alleSechsPlatzhalterWerdenErkannt() {
		String text = "{DATUM}{UHRZEIT}{SEITE}{SEITEN}{TABELLE}{DATEINAME}";
		List<HeaderFooterTextSegment> segmente = HeaderFooterTokenParser.parse(text);

		assertThat(segmente).containsExactly(
				new HeaderFooterTextSegment.Platzhalter(HeaderFooterPlatzhalter.DATUM),
				new HeaderFooterTextSegment.Platzhalter(HeaderFooterPlatzhalter.UHRZEIT),
				new HeaderFooterTextSegment.Platzhalter(HeaderFooterPlatzhalter.SEITE),
				new HeaderFooterTextSegment.Platzhalter(HeaderFooterPlatzhalter.SEITEN),
				new HeaderFooterTextSegment.Platzhalter(HeaderFooterPlatzhalter.TABELLE),
				new HeaderFooterTextSegment.Platzhalter(HeaderFooterPlatzhalter.DATEINAME));
	}
}
