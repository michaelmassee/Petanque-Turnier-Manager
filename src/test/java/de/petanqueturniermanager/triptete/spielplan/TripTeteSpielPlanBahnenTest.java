/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.triptete.spielplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamPaarung;

class TripTeteSpielPlanBahnenTest {

	@Test
	void freispielBekommtKeineBahn() {
		List<List<TeamPaarung>> spielPlan = List.of(List.of(
				new TeamPaarung(Team.from(1), Team.from(2)),
				new TeamPaarung(Team.from(3)),
				new TeamPaarung(Team.from(4), Team.from(5))));

		RangeData daten = TripTeteSpielPlanSheet.bahnenDaten(spielPlan);

		assertThat(daten).hasSize(3);
		assertThat(werte(daten.get(0))).containsExactly(1, 1, 2);
		assertThat(werte(daten.get(1))).containsExactly("", "", "");
		assertThat(werte(daten.get(2))).containsExactly(3, 3, 4);
	}

	@Test
	void bahnnummernBeginnenJedeRundeNeu() {
		List<List<TeamPaarung>> spielPlan = List.of(
				List.of(new TeamPaarung(Team.from(1)), new TeamPaarung(Team.from(2), Team.from(3))),
				List.of(new TeamPaarung(Team.from(1), Team.from(3)), new TeamPaarung(Team.from(2))));

		RangeData daten = TripTeteSpielPlanSheet.bahnenDaten(spielPlan);

		assertThat(werte(daten.get(0))).containsExactly("", "", "");
		assertThat(werte(daten.get(1))).containsExactly(1, 1, 2);
		assertThat(werte(daten.get(2))).containsExactly(1, 1, 2);
		assertThat(werte(daten.get(3))).containsExactly("", "", "");
	}

	private static List<Object> werte(RowData row) {
		return row.stream().map(CellData::getData).toList();
	}
}
