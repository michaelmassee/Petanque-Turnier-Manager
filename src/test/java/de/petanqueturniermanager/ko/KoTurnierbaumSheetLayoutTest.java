package de.petanqueturniermanager.ko;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KoTurnierbaumSheetLayoutTest {

	@Test
	void vierzehnTeamsSpreizenMaximalWeilEinMatchBeidseitigCadrageHat() {
		int bracketGroesse = KoTurnierbaumSheet.berechneBracketGroesse(14);

		assertThat(bracketGroesse).isEqualTo(8);
		assertThat(KoTurnierbaumSheet.berechneRunde1MatchZeilenAbstand(14, bracketGroesse))
				.isEqualTo(6);
		assertThat(KoTurnierbaumSheet.berechneRunde1SlotAbstand(14, bracketGroesse))
				.isEqualTo(3);
	}

	@Test
	void fuenfzehnTeamsSpreizenMaximalWeilMatchesBeidseitigCadrageHaben() {
		int bracketGroesse = KoTurnierbaumSheet.berechneBracketGroesse(15);

		assertThat(bracketGroesse).isEqualTo(8);
		assertThat(KoTurnierbaumSheet.berechneRunde1MatchZeilenAbstand(15, bracketGroesse))
				.isEqualTo(6);
		assertThat(KoTurnierbaumSheet.berechneRunde1SlotAbstand(15, bracketGroesse))
				.isEqualTo(3);
	}

	@Test
	void siebenTeamsSpreizenMaximalWeilEinMatchBeidseitigCadrageHat() {
		int bracketGroesse = KoTurnierbaumSheet.berechneBracketGroesse(7);

		assertThat(bracketGroesse).isEqualTo(4);
		assertThat(KoTurnierbaumSheet.berechneRunde1MatchZeilenAbstand(7, bracketGroesse))
				.isEqualTo(6);
		assertThat(KoTurnierbaumSheet.berechneRunde1SlotAbstand(7, bracketGroesse))
				.isEqualTo(3);
	}

	@Test
	void zwoelfTeamsSpreizenWegenUnteremCadrageSlot() {
		int bracketGroesse = KoTurnierbaumSheet.berechneBracketGroesse(12);

		assertThat(bracketGroesse).isEqualTo(8);
		assertThat(KoTurnierbaumSheet.berechneRunde1MatchZeilenAbstand(12, bracketGroesse))
				.isEqualTo(5);
		assertThat(KoTurnierbaumSheet.berechneRunde1SlotAbstand(12, bracketGroesse))
				.isEqualTo(2);
	}

	@Test
	void zehnTeamsSpreizenWegenUnteremCadrageSlot() {
		int bracketGroesse = KoTurnierbaumSheet.berechneBracketGroesse(10);

		assertThat(bracketGroesse).isEqualTo(8);
		assertThat(KoTurnierbaumSheet.berechneRunde1MatchZeilenAbstand(10, bracketGroesse))
				.isEqualTo(5);
		assertThat(KoTurnierbaumSheet.berechneRunde1SlotAbstand(10, bracketGroesse))
				.isEqualTo(2);
	}

	@Test
	void neunTeamsSpreizenWegenUnteremCadrageSlot() {
		int bracketGroesse = KoTurnierbaumSheet.berechneBracketGroesse(9);

		assertThat(bracketGroesse).isEqualTo(8);
		assertThat(KoTurnierbaumSheet.berechneRunde1MatchZeilenAbstand(9, bracketGroesse))
				.isEqualTo(5);
		assertThat(KoTurnierbaumSheet.berechneRunde1SlotAbstand(9, bracketGroesse))
				.isEqualTo(2);
	}

	@Test
	void achtTeamsBleibenKompaktOhneCadrage() {
		int bracketGroesse = KoTurnierbaumSheet.berechneBracketGroesse(8);

		assertThat(bracketGroesse).isEqualTo(8);
		assertThat(KoTurnierbaumSheet.berechneRunde1MatchZeilenAbstand(8, bracketGroesse))
				.isEqualTo(3);
		assertThat(KoTurnierbaumSheet.berechneRunde1SlotAbstand(8, bracketGroesse))
				.isEqualTo(1);
	}
}
