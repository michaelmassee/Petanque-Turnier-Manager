/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.basesheet.meldeliste.TeamNummernPlan.Aenderung;
import de.petanqueturniermanager.basesheet.meldeliste.TeamNummernPlan.NrZelle;
import de.petanqueturniermanager.basesheet.meldeliste.TeamNummernPlan.Strategie;

class TeamNummernPlanTest {

	private static final String UUID_A = "aaaaaaaa-0000-4000-8000-000000000001";
	private static final String UUID_B = "bbbbbbbb-0000-4000-8000-000000000002";
	private static final String NEU_1 = "11111111-0000-4000-8000-000000000001";
	private static final String NEU_2 = "22222222-0000-4000-8000-000000000002";

	private static Supplier<String> neueUuids() {
		Iterator<String> uuids = List.of(NEU_1, NEU_2).iterator();
		return uuids::next;
	}

	private static NrZelle mitUuid(int nr, String uuid) {
		return new NrZelle("=PTM.ALG.TNRUUID(" + nr + ";\"" + uuid + "\")", nr, true);
	}

	@Test
	void zeileMitNrUndUuidBleibtUnveraendert() {
		List<Aenderung> plan = TeamNummernPlan.berechne(List.of(mitUuid(3, UUID_A)), Strategie.LUECKEN_FUELLEN,
				neueUuids());
		assertThat(plan).isEmpty();
	}

	@Test
	void einzelneMeldungOhneNummerBekommtNummerEins() {
		// Regression Trip-Tête: eine einzelne Meldung darf nicht als "keine Daten" gelten.
		List<Aenderung> plan = TeamNummernPlan.berechne(List.of(new NrZelle("", -1, true)), Strategie.LUECKEN_FUELLEN,
				neueUuids());
		assertThat(plan).extracting(Aenderung::nr).containsExactly(1);
	}

	@Test
	void lueckenFuellenVergibtKleinsteFreieNummer() {
		List<NrZelle> zellen = List.of(mitUuid(1, UUID_A), mitUuid(3, UUID_B), new NrZelle("", -1, true));
		List<Aenderung> plan = TeamNummernPlan.berechne(zellen, Strategie.LUECKEN_FUELLEN, neueUuids());
		assertThat(plan).containsExactly(new Aenderung(2, 2, NEU_1, "PTM.ALG.TNRUUID(2;\"" + NEU_1 + "\")"));
	}

	@Test
	void hochzaehlenVergibtHinterDerHoechstenNummer() {
		List<NrZelle> zellen = List.of(mitUuid(1, UUID_A), mitUuid(3, UUID_B), new NrZelle("", -1, true));
		List<Aenderung> plan = TeamNummernPlan.berechne(zellen, Strategie.HOCHZAEHLEN, neueUuids());
		assertThat(plan).extracting(Aenderung::nr).containsExactly(4);
	}


	@Test
	void zahlOhneUuidBehaeltNrUndBekommtUuid() {
		List<Aenderung> plan = TeamNummernPlan.berechne(List.of(new NrZelle("5", 5, true)), Strategie.LUECKEN_FUELLEN,
				neueUuids());
		assertThat(plan).extracting(Aenderung::nr, Aenderung::uuid).containsExactly(tuple(5, NEU_1));
	}

	@Test
	void uuidOhneNrBehaeltUuidUndBekommtNr() {
		List<NrZelle> zellen = List.of(mitUuid(1, UUID_A), new NrZelle("=PTM.ALG.TNRUUID(0;\"" + UUID_B + "\")", 0, true));
		List<Aenderung> plan = TeamNummernPlan.berechne(zellen, Strategie.LUECKEN_FUELLEN, neueUuids());
		assertThat(plan).extracting(Aenderung::index, Aenderung::nr, Aenderung::uuid).containsExactly(tuple(1, 2, UUID_B));
	}

	@Test
	void kopierteZeileBekommtNeueUuid() {
		List<NrZelle> zellen = List.of(mitUuid(1, UUID_A), mitUuid(1, UUID_A));
		List<Aenderung> plan = TeamNummernPlan.berechne(zellen, Strategie.LUECKEN_FUELLEN, neueUuids());
		assertThat(plan).extracting(Aenderung::index, Aenderung::uuid)
				.containsExactly(tuple(1, NEU_1));
	}


	@Test
	void zeileOhneNamenWirdNichtAngefasst() {
		List<NrZelle> zellen = List.of(new NrZelle("4", 4, false), new NrZelle("", -1, false));
		assertThat(TeamNummernPlan.berechne(zellen, Strategie.LUECKEN_FUELLEN, neueUuids())).isEmpty();
	}
}
