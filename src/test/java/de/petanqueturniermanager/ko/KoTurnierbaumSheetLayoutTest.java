package de.petanqueturniermanager.ko;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KoTurnierbaumSheetLayoutTest {

	@Test
	void vierzehnTeamsNutzenAchterHauptfeldMitKompakterCadrage() {
		int bracketGroesse = KoTurnierbaumSheet.berechneBracketGroesse(14);

		assertThat(bracketGroesse).isEqualTo(8);
		assertThat(KoTurnierbaumSheet.berechneRunde1MatchZeilenAbstand(14, bracketGroesse))
				.isEqualTo(5);
		assertThat(KoTurnierbaumSheet.berechneRunde1SlotAbstand(14, bracketGroesse))
				.isEqualTo(2);
	}

	@Test
	void siebenTeamsSpreizenViererHauptfeldWegenOberemCadrageSlot() {
		int bracketGroesse = KoTurnierbaumSheet.berechneBracketGroesse(7);

		assertThat(bracketGroesse).isEqualTo(4);
		assertThat(KoTurnierbaumSheet.berechneRunde1MatchZeilenAbstand(7, bracketGroesse))
				.isEqualTo(5);
		assertThat(KoTurnierbaumSheet.berechneRunde1SlotAbstand(7, bracketGroesse))
				.isEqualTo(2);
	}

	@Test
	void zwoelfTeamsBleibenKompaktWennCadrageNurAmUnterenSlotHaengt() {
		int bracketGroesse = KoTurnierbaumSheet.berechneBracketGroesse(12);

		assertThat(bracketGroesse).isEqualTo(8);
		assertThat(KoTurnierbaumSheet.berechneRunde1MatchZeilenAbstand(12, bracketGroesse))
				.isEqualTo(3);
		assertThat(KoTurnierbaumSheet.berechneRunde1SlotAbstand(12, bracketGroesse))
				.isEqualTo(1);
	}

	@Test
	void zehnTeamsBleibenKompaktWennCadrageNurAmUnterenSlotHaengt() {
		int bracketGroesse = KoTurnierbaumSheet.berechneBracketGroesse(10);

		assertThat(bracketGroesse).isEqualTo(8);
		assertThat(KoTurnierbaumSheet.berechneRunde1MatchZeilenAbstand(10, bracketGroesse))
				.isEqualTo(3);
		assertThat(KoTurnierbaumSheet.berechneRunde1SlotAbstand(10, bracketGroesse))
				.isEqualTo(1);
	}

	@Test
	void neunTeamsBleibenKompaktWennCadrageNurAmUnterenSlotHaengt() {
		int bracketGroesse = KoTurnierbaumSheet.berechneBracketGroesse(9);

		assertThat(bracketGroesse).isEqualTo(8);
		assertThat(KoTurnierbaumSheet.berechneRunde1MatchZeilenAbstand(9, bracketGroesse))
				.isEqualTo(3);
		assertThat(KoTurnierbaumSheet.berechneRunde1SlotAbstand(9, bracketGroesse))
				.isEqualTo(1);
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
