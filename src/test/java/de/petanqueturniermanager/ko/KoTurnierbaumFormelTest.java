/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Issue #23: Die Weitergabe-Formeln des KO-Turnierbaums dürfen nur bei feststehenden Gegnern greifen.
 */
class KoTurnierbaumFormelTest {

	private static final String SCORE_A = "$C$5";
	private static final String SCORE_B = "$C$6";
	private static final String TEAM_A = "$B$5";
	private static final String TEAM_B = "$B$6";

	@Test
	void gueltigesSpielVerlangtBeideTeams() {
		String formel = KoTurnierbaumSheet.spielGueltigFormel(SCORE_A, SCORE_B, TEAM_A, TEAM_B);

		assertThat(formel)
				.isEqualTo("AND(ISNUMBER($C$5);$C$5<>\"\";ISNUMBER($C$6);$C$6<>\"\";"
						+ "$B$5<>\"\";$B$5<>\"?\";$B$6<>\"\";$B$6<>\"?\")");
	}

	@Test
	void siegerFormelReichtHoeherePunktzahlWeiter() {
		String formel = KoTurnierbaumSheet.weitergabeFormel(SCORE_A, SCORE_B, TEAM_A, TEAM_B,
				KoTurnierbaumSheet.Weitergabe.SIEGER);

		assertThat(formel)
				.startsWith("IF(" + KoTurnierbaumSheet.spielGueltigFormel(SCORE_A, SCORE_B, TEAM_A, TEAM_B) + ";")
				.contains("IF($C$5>$C$6;$B$5;IF($C$5<$C$6;$B$6;\"?\"))")
				.endsWith(";\"\")");
	}

	@Test
	void verliererFormelReichtNiedrigerePunktzahlWeiter() {
		String formel = KoTurnierbaumSheet.weitergabeFormel(SCORE_A, SCORE_B, TEAM_A, TEAM_B,
				KoTurnierbaumSheet.Weitergabe.VERLIERER);

		assertThat(formel).contains("IF($C$5>$C$6;$B$6;IF($C$5<$C$6;$B$5;\"?\"))");
	}

	@Test
	void ergebnisOhneGegnerIstFehler() {
		String formel = KoTurnierbaumSheet.ergebnisOhneGegnerFormel(SCORE_A, SCORE_B, TEAM_A, TEAM_B);

		assertThat(formel).isEqualTo("AND(OR($C$5<>\"\";$C$6<>\"\");"
				+ "NOT(AND($B$5<>\"\";$B$5<>\"?\";$B$6<>\"\";$B$6<>\"?\")))");
	}
}
