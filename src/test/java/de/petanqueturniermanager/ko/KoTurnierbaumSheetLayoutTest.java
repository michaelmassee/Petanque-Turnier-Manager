package de.petanqueturniermanager.ko;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KoTurnierbaumSheetLayoutTest {

	@Test
	void vierzehnTeamsNutzenAchterHauptfeldMitHoehererCadrageSpreizung() {
		int bracketGroesse = KoTurnierbaumSheet.berechneBracketGroesse(14);

		assertThat(bracketGroesse).isEqualTo(8);
		assertThat(KoTurnierbaumSheet.berechneRunde1MatchZeilenAbstand(14, bracketGroesse))
				.isEqualTo(6);
	}

	@Test
	void zehnTeamsBehaltenKompakteCadrageSpreizung() {
		int bracketGroesse = KoTurnierbaumSheet.berechneBracketGroesse(10);

		assertThat(bracketGroesse).isEqualTo(8);
		assertThat(KoTurnierbaumSheet.berechneRunde1MatchZeilenAbstand(10, bracketGroesse))
				.isEqualTo(3);
	}
}
