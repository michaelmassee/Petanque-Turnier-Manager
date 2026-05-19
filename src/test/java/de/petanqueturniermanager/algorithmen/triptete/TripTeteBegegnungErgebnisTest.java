/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.triptete;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.model.SpielErgebnis;
import de.petanqueturniermanager.model.Team;

public class TripTeteBegegnungErgebnisTest {

	@Test
	public void beispielAusDokuTeamAGewinnt2zu1() {
		// Doku §5 Beispiel 1: Triplette 9:13 (B), Doublette 13:6 (A), Tête 13:11 (A) → A gewinnt 2:1
		TripTeteBegegnungErgebnis erg = new TripTeteBegegnungErgebnis(Team.from(1), Team.from(2));
		erg.setPartieErgebnis(TripTetePartie.TRIPLETTE, new SpielErgebnis(9, 13));
		erg.setPartieErgebnis(TripTetePartie.DOUBLETTE, new SpielErgebnis(13, 6));
		erg.setPartieErgebnis(TripTetePartie.TETE, new SpielErgebnis(13, 11));

		assertThat(erg.istVollstaendig()).isTrue();
		assertThat(erg.begegnungPunkteA()).isEqualTo(2);
		assertThat(erg.begegnungPunkteB()).isEqualTo(1);
		assertThat(erg.siegA()).isTrue();
		assertThat(erg.siegB()).isFalse();
		assertThat(erg.unentschieden()).isFalse();
		assertThat(erg.kugelnFuerA()).isEqualTo(9 + 13 + 13);
		assertThat(erg.kugelnGegenA()).isEqualTo(13 + 6 + 11);
		assertThat(erg.kugelDiffA()).isEqualTo(5);
		assertThat(erg.sieger()).isPresent().get().extracting(Team::getNr).isEqualTo(1);
	}

	@Test
	public void unvollstaendigeBegegnungHatKeinenSieger() {
		TripTeteBegegnungErgebnis erg = new TripTeteBegegnungErgebnis(Team.from(1), Team.from(2));
		erg.setPartieErgebnis(TripTetePartie.TRIPLETTE, new SpielErgebnis(13, 9));

		assertThat(erg.istVollstaendig()).isFalse();
		assertThat(erg.siegA()).isFalse();
		assertThat(erg.siegB()).isFalse();
		assertThat(erg.sieger()).isEmpty();
		assertThat(erg.begegnungPunkteA()).isEqualTo(1);
		assertThat(erg.kugelDiffA()).isEqualTo(4);
	}

	@Test
	public void teamBGewinntKlar3zu0() {
		TripTeteBegegnungErgebnis erg = new TripTeteBegegnungErgebnis(Team.from(3), Team.from(4));
		erg.setPartieErgebnis(TripTetePartie.TRIPLETTE, new SpielErgebnis(5, 13));
		erg.setPartieErgebnis(TripTetePartie.DOUBLETTE, new SpielErgebnis(7, 13));
		erg.setPartieErgebnis(TripTetePartie.TETE, new SpielErgebnis(11, 13));

		assertThat(erg.siegB()).isTrue();
		assertThat(erg.begegnungPunkteB()).isEqualTo(3);
		assertThat(erg.kugelDiffA()).isEqualTo(-16);
		assertThat(erg.sieger()).isPresent().get().extracting(Team::getNr).isEqualTo(4);
	}

	@Test
	public void partieErgebnisAbrufbar() {
		TripTeteBegegnungErgebnis erg = new TripTeteBegegnungErgebnis(Team.from(1), Team.from(2));
		SpielErgebnis triplette = new SpielErgebnis(13, 9);
		erg.setPartieErgebnis(TripTetePartie.TRIPLETTE, triplette);

		assertThat(erg.getPartieErgebnis(TripTetePartie.TRIPLETTE)).contains(triplette);
		assertThat(erg.getPartieErgebnis(TripTetePartie.DOUBLETTE)).isEmpty();
	}
}
