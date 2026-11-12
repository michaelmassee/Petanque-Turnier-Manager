package de.petanqueturniermanager.algorithmen.schweizer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.algorithmen.schweizer.HerunterlosChronik.Begegnung;

class HerunterlosChronikTest {

	@Test
	void teamMitMehrSiegenGiltAlsHeruntergelost() {
		var chronik = new HerunterlosChronik();

		chronik.vermerkeRunde(2, List.of(new Begegnung(1, 2), new Begegnung(4, 3)), List.of(),
				Map.of(1, 1, 2, 0, 3, 1, 4, 0));

		assertThat(chronik.rundenFuer(1)).containsExactly(2);
		assertThat(chronik.rundenFuer(3)).containsExactly(2);
		assertThat(chronik.rundenFuer(2)).isEmpty();
		assertThat(chronik.rundenFuer(4)).isEmpty();
	}

	@Test
	void gleicheSiegeSindKeinHerunterlosen() {
		var chronik = new HerunterlosChronik();

		chronik.vermerkeRunde(1, List.of(new Begegnung(1, 2)), List.of(), Map.of());
		chronik.vermerkeRunde(2, List.of(new Begegnung(1, 2)), List.of(), Map.of(1, 1, 2, 1));

		assertThat(chronik.rundenFuer(1)).isEmpty();
		assertThat(chronik.rundenFuer(2)).isEmpty();
	}

	@Test
	void sammeltRundenAufsteigend() {
		var chronik = new HerunterlosChronik();

		chronik.vermerkeRunde(2, List.of(new Begegnung(1, 2)), List.of(), Map.of(1, 1));
		chronik.vermerkeRunde(3, List.of(new Begegnung(3, 1)), List.of(), Map.of(1, 1, 3, 2));
		chronik.vermerkeRunde(4, List.of(new Begegnung(5, 1)), List.of(), Map.of(1, 3, 5, 2));

		assertThat(chronik.rundenFuer(1)).containsExactly(2, 4);
		assertThat(chronik.rundenFuer(3)).containsExactly(3);
	}

	@Test
	void freilosZaehltAlsHerunterlosung() {
		var chronik = new HerunterlosChronik();

		chronik.vermerkeRunde(1, List.of(new Begegnung(1, 2)), List.of(3), Map.of());

		assertThat(chronik.rundenFuer(3)).containsExactly(1);
		assertThat(chronik.rundenFuer(1)).isEmpty();
	}

	@Test
	void freilosUndSpaeteresHerunterlosenAufsteigend() {
		var chronik = new HerunterlosChronik();

		chronik.vermerkeRunde(1, List.of(new Begegnung(1, 2)), List.of(3), Map.of());
		chronik.vermerkeRunde(2, List.of(new Begegnung(3, 2)), List.of(), Map.of(1, 1, 3, 1));

		assertThat(chronik.rundenFuer(3)).containsExactly(1, 2);
	}

	@Test
	void rundeZaehltProTeamHoechstensEinmal() {
		var chronik = new HerunterlosChronik();

		chronik.vermerkeRunde(2, List.of(new Begegnung(1, 2)), List.of(1), Map.of(1, 1));

		assertThat(chronik.rundenFuer(1)).containsExactly(2);
	}
}
