/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import static de.petanqueturniermanager.basesheet.meldeliste.MeleeNeuAuslosungPlan.namensSchluessel;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.basesheet.meldeliste.MeleeNeuAuslosungPlan.Belegung;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeNeuAuslosungPlan.Bereich;

/**
 * Testet die Planung des Neu-Auslosens: Erkennen der bisherigen Mêlée-Teams in der Meldeliste,
 * Zuordnung der neuen Teams zu Zeilen und das Zusammenfassen zu Schreibblöcken.
 */
class MeleeNeuAuslosungPlanTest {

	private static final String ANNA = namensSchluessel("Anna", "A");
	private static final String BERT = namensSchluessel("Bert", "B");
	private static final String CARL = namensSchluessel("Carl", "C");
	private static final String DORA = namensSchluessel("Dora", "D");
	private static final String HAND = namensSchluessel("Hand", "Gemeldet");

	@Test
	void namensSchluesselIgnoriertLeerzeichenUndGrossKlein() {
		assertThat(namensSchluessel(" Anna ", "A")).isEqualTo(namensSchluessel("anna", "a"));
		assertThat(namensSchluessel("Anna", "A")).isNotEqualTo(namensSchluessel("AnnaA", ""));
	}

	@Test
	void meleeTeamZeilenErkenntNurTeamsAusUebernommenenSpielern() {
		List<Set<String>> spielerJeZeile = List.of(
				Set.of(ANNA, BERT),
				Set.of(HAND, CARL),
				Set.of(),
				Set.of(CARL, DORA));

		assertThat(MeleeNeuAuslosungPlan.meleeTeamZeilen(spielerJeZeile, Set.of(ANNA, BERT, CARL, DORA)))
				.as("gemischtes Team mit von Hand gemeldetem Spieler und Leerzeile zählen nicht")
				.containsExactly(0, 3);
	}

	@Test
	void zeilenZuordnungHaengtWeitereTeamsHintenAn() {
		assertThat(MeleeNeuAuslosungPlan.zeilenZuordnung(List.of(3, 5), 3, 9))
				.containsExactly(new Belegung(3, 0), new Belegung(5, 1), new Belegung(9, 2));
	}

	@Test
	void zeilenZuordnungLeertUeberzaehligeBisherigeZeilen() {
		List<Belegung> belegungen = MeleeNeuAuslosungPlan.zeilenZuordnung(List.of(3, 4, 5), 2, 9);

		assertThat(belegungen).containsExactly(new Belegung(3, 0), new Belegung(4, 1),
				new Belegung(5, Belegung.LEEREN));
		assertThat(belegungen.get(2).istLeeren()).isTrue();
	}

	@Test
	void zusammenhaengendeBereicheFasstLueckenloseZeilenZusammen() {
		List<Belegung> belegungen = List.of(new Belegung(3, 0), new Belegung(4, 1), new Belegung(6, 2),
				new Belegung(9, 3), new Belegung(10, 4));

		assertThat(MeleeNeuAuslosungPlan.zusammenhaengendeBereiche(belegungen))
				.containsExactly(new Bereich(3, 4), new Bereich(6, 6), new Bereich(9, 10));
	}

	@Test
	void zusammenhaengendeBereicheOhneBelegungIstLeer() {
		assertThat(MeleeNeuAuslosungPlan.zusammenhaengendeBereiche(List.of())).isEmpty();
	}
}
