package de.petanqueturniermanager.algorithmen.schweizer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class FreilosRundenTextTest {

	@Test
	void ohneFreilosIstDerTextLeer() {
		assertThat(FreilosRundenText.alsText(List.of())).isEmpty();
	}

	@Test
	void einFreilosZeigtNurDieRunde() {
		assertThat(FreilosRundenText.alsText(List.of(2))).isEqualTo("2");
	}

	@Test
	void mehrereFreiloseWerdenMitKommaGetrennt() {
		assertThat(FreilosRundenText.alsText(List.of(1, 3, 6))).isEqualTo("1, 3, 6");
	}

	@Test
	void teamErgebnisOhneAngabeHatKeineFreilosRunden() {
		var ergebnis = new SchweizerTeamErgebnis(1, 2, 5, 26, List.of(2, 3), List.of(), 0);

		assertThat(ergebnis.freilosInRunden()).isEmpty();
	}
}
