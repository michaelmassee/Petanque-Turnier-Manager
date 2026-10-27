/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamPaarung;

/**
 * Freilos-Paarungen belegen keine Bahn: Ihre Bahnzelle bleibt leer und die übrigen Paarungen werden lückenlos
 * nummeriert.
 */
class BahnNummernFreilosTest {

	@AfterEach
	void resetRandom() {
		RandomSource.reset();
	}

	@Test
	void freilosIndizesFindetPaarungenOhneTeamB() {
		List<TeamPaarung> paarungen = List.of(new TeamPaarung(Team.from(1), Team.from(2)),
				new TeamPaarung(Team.from(3), Team.from(4)), new TeamPaarung(Team.from(5)));

		assertThat(SpielrundeHelper.freilosIndizes(paarungen)).containsExactly(2);
	}

	@Test
	void fortlaufendeNummernUeberspringenFreilos() {
		RangeData daten = SpielrundeHelper.fortlaufendeBahnNummern(4, Set.of(3));

		assertThat(werte(daten)).containsExactly(1, 2, 3, "");
	}

	@Test
	void fortlaufendeNummernOhneFreilosUnveraendert() {
		RangeData daten = SpielrundeHelper.fortlaufendeBahnNummern(3, Set.of());

		assertThat(werte(daten)).containsExactly(1, 2, 3);
	}

	@Test
	void zufaelligeNummernUeberspringenFreilos() {
		RandomSource.setSeed(42L);

		List<Object> werte = werte(SpielrundeHelper.zufaelligeBahnNummern(5, Set.of(4)));

		assertThat(werte).hasSize(5).endsWith("");
		assertThat(werte.subList(0, 4)).containsExactlyInAnyOrder(1, 2, 3, 4);
	}

	@Test
	void durchgangNummernUeberspringenFreilos() {
		RangeData daten = IZeitplanSpielrundeSheet.bahnNummernProDurchgang(List.of(3, 3), Set.of(5));

		assertThat(werte(daten)).containsExactly(1, 2, 3, 1, 2, "");
	}

	private static List<Object> werte(RangeData daten) {
		return daten.stream().map(row -> row.get(0).getData()).toList();
	}
}
