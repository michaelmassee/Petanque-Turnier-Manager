package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.algorithmen.kaskaden.KaskadenKoGruppenRunde;
import de.petanqueturniermanager.algorithmen.kaskaden.KaskadenKoSpielPaar;

public class KaskadenKoGruppenRundeTest {

	@Test
	public void testKonstruktorStartrunde_leererPfad() {
		List<KaskadenKoSpielPaar> spielpaare = List.of(
				new KaskadenKoSpielPaar(1, 1, 4, null, null),
				new KaskadenKoSpielPaar(2, 2, 3, null, null));
		KaskadenKoGruppenRunde runde = new KaskadenKoGruppenRunde("", 4, spielpaare, 0);

		assertThat(runde.pfad()).isEmpty();
		assertThat(runde.anzTeams()).isEqualTo(4);
		assertThat(runde.spielPaare()).hasSize(2);
		assertThat(runde.anzFreilose()).isZero();
	}

	@Test
	public void testInvariante_geradeAnzTeams_ohneFreilos() {
		List<KaskadenKoSpielPaar> spielpaare = List.of(
				new KaskadenKoSpielPaar(1, 1, 4, null, null),
				new KaskadenKoSpielPaar(2, 2, 3, null, null));
		KaskadenKoGruppenRunde runde = new KaskadenKoGruppenRunde("S", 4, spielpaare, 0);

		// Invariante: anzTeams == 2 * spielPaare.size() + anzFreilose
		assertThat(runde.anzTeams())
				.isEqualTo(2 * runde.spielPaare().size() + runde.anzFreilose());
	}

	@Test
	public void testInvariante_ungeradeAnzTeams_mitFreilos() {
		// 5 Teams: 2 Spielpaare + 1 Freilos
		List<KaskadenKoSpielPaar> spielpaare = List.of(
				new KaskadenKoSpielPaar(1, 1, 4, null, null),
				new KaskadenKoSpielPaar(2, 2, 3, null, null));
		KaskadenKoGruppenRunde runde = new KaskadenKoGruppenRunde("V", 5, spielpaare, 1);

		assertThat(runde.anzTeams())
				.isEqualTo(2 * runde.spielPaare().size() + runde.anzFreilose());
		assertThat(runde.anzFreilose()).isEqualTo(1);
	}

	@Test
	public void testLeereSpielpaareListe() {
		KaskadenKoGruppenRunde runde = new KaskadenKoGruppenRunde("VS", 1, List.of(), 1);

		assertThat(runde.spielPaare()).isEmpty();
		assertThat(runde.anzFreilose()).isEqualTo(1);
		assertThat(runde.anzTeams()).isEqualTo(1);
	}

	@Test
	public void testEqualsUndHashCode_gleicheWerte() {
		List<KaskadenKoSpielPaar> spielpaare = List.of(new KaskadenKoSpielPaar(1, 1, 2, null, null));
		KaskadenKoGruppenRunde a = new KaskadenKoGruppenRunde("S", 2, spielpaare, 0);
		KaskadenKoGruppenRunde b = new KaskadenKoGruppenRunde("S", 2, spielpaare, 0);

		assertThat(a).isEqualTo(b);
		assertThat(a.hashCode()).isEqualTo(b.hashCode());
	}

	@Test
	public void testEquals_unterschiedlicherPfad_nichtEqual() {
		List<KaskadenKoSpielPaar> spielpaare = List.of(new KaskadenKoSpielPaar(1, 1, 2, null, null));
		KaskadenKoGruppenRunde a = new KaskadenKoGruppenRunde("S", 2, spielpaare, 0);
		KaskadenKoGruppenRunde b = new KaskadenKoGruppenRunde("V", 2, spielpaare, 0);

		assertThat(a).isNotEqualTo(b);
	}
}
