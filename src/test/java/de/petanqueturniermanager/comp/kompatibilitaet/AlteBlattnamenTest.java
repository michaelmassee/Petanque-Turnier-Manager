/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.kompatibilitaet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class AlteBlattnamenTest {

	private static final String AKTUELL = "Inschrijflijst";
	private static final List<String> ALT = List.of("Deelnemerslijst");

	@Test
	void altesBlattWirdUmbenannt() {
		assertThat(AlteBlattnamen.zuUmbenennen(AKTUELL, ALT, Set.of("Deelnemerslijst", "Ronde 1")::contains))
				.contains("Deelnemerslijst");
	}

	@Test
	void vorhandenerAktuellerNameVerhindertDieUmbenennung() {
		assertThat(AlteBlattnamen.zuUmbenennen(AKTUELL, ALT, Set.of(AKTUELL, "Deelnemerslijst")::contains)).isEmpty();
	}

	@Test
	void ohneAltesBlattPassiertNichts() {
		assertThat(AlteBlattnamen.zuUmbenennen(AKTUELL, ALT, Set.of("Ronde 1")::contains)).isEmpty();
	}

	@Test
	void aktuellerNameInDerListeDerAltenNamenBleibt() {
		assertThat(AlteBlattnamen.zuUmbenennen("Deelnemerslijst", ALT, Set.of("Deelnemerslijst")::contains))
				.isEmpty();
	}
}
