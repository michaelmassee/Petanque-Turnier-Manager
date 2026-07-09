/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.kaskaden;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

public class KaskadenFeldBelegungTest {

	@Test
	public void bezeichnerDelegiertAnFeld() {
		KaskadenKoFeldInfo feld = new KaskadenKoFeldInfo("A", "SS", 8, KaskadenKoFeldInfo.cadrageRechnerFuer(8));
		KaskadenFeldBelegung belegung = new KaskadenFeldBelegung(feld, List.of(1, 2, 3, 4, 5, 6, 7, 8));

		assertThat(belegung.bezeichner()).isEqualTo("A");
		assertThat(belegung.feld()).isEqualTo(feld);
		assertThat(belegung.teamNrs()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
	}
}
