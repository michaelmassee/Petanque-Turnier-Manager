/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.helper.position.Position;

/**
 * Zwei gleichnamige Teams im Halbfinale: Die Auflösung folgt den Punkten bis zur UUID der Setzliste, nicht dem
 * angezeigten Namen.
 */
class KoWeitergabeFormelTest {

	private static final String UUID_A = "11111111-0000-4000-8000-00000000000a";
	private static final String UUID_B = "11111111-0000-4000-8000-00000000000b";
	private static final String UUID_C = "11111111-0000-4000-8000-00000000000c";
	private static final String UUID_D = "11111111-0000-4000-8000-00000000000d";

	/** Runde 1 in Spalte B (Team) / C (Punkte), Zeilen 5–8; Runde 2 in E/F, Zeilen 5–6; Sieger in H5. */
	private static final Position SIEGER_R2_A = Position.from(4, 4);
	private static final Position SIEGER_R2_B = Position.from(4, 5);
	private static final Position TURNIERSIEGER = Position.from(7, 4);
	private static final Position VERLIERER = Position.from(7, 6);

	private final Map<Position, String> formeln = new HashMap<>();
	private final Map<Position, Integer> punkte = new HashMap<>();
	private final KoWeitergabeFormel.Zellen zellen = new KoWeitergabeFormel.Zellen() {
		@Override
		public String formel(Position position) {
			return formeln.get(position);
		}

		@Override
		public Optional<Integer> punkte(Position position) {
			return Optional.ofNullable(punkte.get(position));
		}
	};

	@BeforeEach
	void baumAufbauen() {
		formeln.put(Position.from(1, 4), setzliste(UUID_A));
		formeln.put(Position.from(1, 5), setzliste(UUID_B));
		formeln.put(Position.from(1, 6), setzliste(UUID_C));
		formeln.put(Position.from(1, 7), setzliste(UUID_D));
		formeln.put(SIEGER_R2_A, weitergabe(">", "<", "C5", "C6", "B5", "B6"));
		formeln.put(SIEGER_R2_B, weitergabe(">", "<", "C7", "C8", "B7", "B8"));
		formeln.put(TURNIERSIEGER, weitergabe(">", "<", "F5", "F6", "E5", "E6"));
		formeln.put(VERLIERER, weitergabe("<", ">", "F5", "F6", "E5", "E6"));
	}

	@Test
	void folgtDenPunktenBisZurSetzliste() {
		ergebnis(Position.from(2, 4), 7, 13);
		ergebnis(Position.from(2, 6), 13, 2);
		ergebnis(Position.from(5, 4), 13, 11);

		assertThat(KoWeitergabeFormel.uuid(zellen, SIEGER_R2_A)).hasValue(UUID_B);
		assertThat(KoWeitergabeFormel.uuid(zellen, SIEGER_R2_B)).hasValue(UUID_C);
		assertThat(KoWeitergabeFormel.uuid(zellen, TURNIERSIEGER)).as("zwei Ebenen").hasValue(UUID_B);
		assertThat(KoWeitergabeFormel.uuid(zellen, VERLIERER)).as("vertauschte Vergleiche").hasValue(UUID_C);
	}

	@Test
	void ohneErgebnisOderBeiGleichstandKeinTeam() {
		ergebnis(Position.from(2, 4), 13, 5);
		assertThat(KoWeitergabeFormel.uuid(zellen, TURNIERSIEGER)).as("Halbfinale B offen").isEmpty();

		ergebnis(Position.from(2, 6), 13, 2);
		ergebnis(Position.from(5, 4), 9, 9);
		assertThat(KoWeitergabeFormel.uuid(zellen, TURNIERSIEGER)).as("Gleichstand").isEmpty();
	}

	@Test
	void setzlistenZelleLiefertIhreUuid() {
		assertThat(KoWeitergabeFormel.uuid(zellen, Position.from(1, 6))).hasValue(UUID_C);
	}

	@Test
	void unbekannteFormelLiefertNichts() {
		formeln.put(SIEGER_R2_A, "=\"Anna\"");
		assertThat(KoWeitergabeFormel.uuid(zellen, SIEGER_R2_A)).isEmpty();
		assertThat(KoWeitergabeFormel.uuid(zellen, Position.from(9, 9))).as("leere Zelle").isEmpty();
	}

	@Test
	void zirkelbezugBrichtAb() {
		formeln.put(SIEGER_R2_A, weitergabe(">", "<", "C5", "C6", "E5", "E5"));
		ergebnis(Position.from(2, 4), 13, 5);
		assertThat(KoWeitergabeFormel.uuid(zellen, SIEGER_R2_A)).isEmpty();
	}

	private void ergebnis(Position punkteA, int a, int b) {
		punkte.put(punkteA, a);
		punkte.put(Position.from(punkteA.getSpalte(), punkteA.getZeile() + 1), b);
	}

	private static String setzliste(String uuid) {
		return "=PTM.ALG.UUID.TEAMANZEIGE(0;1;0;\"" + uuid + "\";$'Meldeliste'.$A$1:$C$999;0)";
	}

	/** Wie {@code KoTurnierbaumSheet}: Bedingung, dann zwei verschachtelte Vergleiche. */
	private static String weitergabe(String erster, String zweiter, String a, String b, String teamA, String teamB) {
		String sa = "$" + a.charAt(0) + "$" + a.substring(1);
		String sb = "$" + b.charAt(0) + "$" + b.substring(1);
		String ta = "$" + teamA.charAt(0) + "$" + teamA.substring(1);
		String tb = "$" + teamB.charAt(0) + "$" + teamB.substring(1);
		return "=IF(AND(ISNUMBER(" + sa + ");" + sa + "<>\"\";ISNUMBER(" + sb + ");" + sb + "<>\"\");IF(" + sa + erster
				+ sb + ";" + ta + ";IF(" + sa + zweiter + sb + ";" + tb + ";\"?\"));\"\")";
	}
}
