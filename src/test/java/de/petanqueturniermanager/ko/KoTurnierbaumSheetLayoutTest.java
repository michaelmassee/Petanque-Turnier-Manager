package de.petanqueturniermanager.ko;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KoTurnierbaumSheetLayoutTest {

	@Test
	void vierzehnTeamsNutzenAchterHauptfeldMitGespreizterCadrage() {
		int bracketGroesse = KoTurnierbaumSheet.berechneBracketGroesse(14);

		assertThat(bracketGroesse).isEqualTo(8);
		assertThat(KoTurnierbaumSheet.berechneRunde1MatchZeilenAbstand(14, bracketGroesse))
				.isEqualTo(6);
		assertThat(KoTurnierbaumSheet.berechneRunde1SlotAbstand(14, bracketGroesse))
				.isEqualTo(3);
	}

	@Test
	void zehnTeamsSpreizenCadrageSlotsEbenfalls() {
		int bracketGroesse = KoTurnierbaumSheet.berechneBracketGroesse(10);

		assertThat(bracketGroesse).isEqualTo(8);
		// Mit Cadrage werden die Runde-1-Slots gespreizt, damit die Cadrage-Partie fluchtet.
		assertThat(KoTurnierbaumSheet.berechneRunde1MatchZeilenAbstand(10, bracketGroesse))
				.isEqualTo(6);
		assertThat(KoTurnierbaumSheet.berechneRunde1SlotAbstand(10, bracketGroesse))
				.isEqualTo(3);
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
