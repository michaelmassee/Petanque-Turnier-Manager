package de.petanqueturniermanager.jedergegenjeden.rangliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJGesamtranglisteSortModus;

class JGJGesamtranglisteSheetTest {

	@Test
	void fusszeileNenntGruppenplatzWennNachSnakeSortiertWird() {
		assertThat(JGJGesamtranglisteSheet.fusszeilenKey(JGJGesamtranglisteSortModus.GRUPPENPLATZ))
				.isEqualTo("jgj.gesamtrangliste.fusszeile.gruppenplatz");
	}

	@Test
	void fusszeileBleibtBeiAbsoluterSortierungBeiRanglistenKriterien() {
		assertThat(JGJGesamtranglisteSheet.fusszeilenKey(JGJGesamtranglisteSortModus.ABSOLUT))
				.isEqualTo("jgj.rangliste.fusszeile");
	}
}
