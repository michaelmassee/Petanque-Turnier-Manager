package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class KaskadenKoSpielPaarTest {

	@Test
	public void testKonstruktorUndAccessor() {
		KaskadenKoSpielPaar paar = new KaskadenKoSpielPaar(1, 2, 5, 3, 7);

		assertThat(paar.spielNr()).isEqualTo(1);
		assertThat(paar.positionA()).isEqualTo(2);
		assertThat(paar.positionB()).isEqualTo(5);
		assertThat(paar.zielPositionSieger()).isEqualTo(3);
		assertThat(paar.zielPositionVerlierer()).isEqualTo(7);
	}

	@Test
	public void testZielPositionenNull_zulaessigSolangeNochNichtBefuellt() {
		KaskadenKoSpielPaar paar = new KaskadenKoSpielPaar(1, 1, 2, null, null);

		assertThat(paar.zielPositionSieger()).isNull();
		assertThat(paar.zielPositionVerlierer()).isNull();
	}

	@Test
	public void testEqualsUndHashCode_gleicheWerte() {
		KaskadenKoSpielPaar a = new KaskadenKoSpielPaar(2, 1, 4, 5, 6);
		KaskadenKoSpielPaar b = new KaskadenKoSpielPaar(2, 1, 4, 5, 6);

		assertThat(a).isEqualTo(b);
		assertThat(a.hashCode()).isEqualTo(b.hashCode());
	}

	@Test
	public void testEquals_unterschiedlicheZielPositionen_nichtEqual() {
		KaskadenKoSpielPaar a = new KaskadenKoSpielPaar(1, 1, 2, 3, 4);
		KaskadenKoSpielPaar b = new KaskadenKoSpielPaar(1, 1, 2, null, 4);

		assertThat(a).isNotEqualTo(b);
	}
}
