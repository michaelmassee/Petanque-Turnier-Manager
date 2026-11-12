/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.helper.position.RangePosition;

/**
 * Unit-Tests für die Bereichsberechnung in {@link NichtGewertetFormatHelper}.
 */
class NichtGewertetFormatHelperTest {

	private static final RangePosition TABELLE = RangePosition.from(0, 0, 4, 9);

	@Test
	void schnitt_ueberlappend() {
		assertThat(NichtGewertetFormatHelper.schnitt(TABELLE, RangePosition.from(3, 2, 6, 20)))
				.hasValueSatisfying(bereich -> assertThat(bereich.getAddress()).isEqualTo("D3:E10"));
	}

	@Test
	void schnitt_ausserhalbIstLeer() {
		assertThat(NichtGewertetFormatHelper.schnitt(TABELLE, RangePosition.from(6, 0, 8, 9))).isEmpty();
	}

	@Test
	void freieBereiche_ohneBelegungIstJedeSpalteKomplett() {
		List<RangePosition> frei = NichtGewertetFormatHelper.freieBereiche(TABELLE, List.of());
		assertThat(frei).extracting(RangePosition::getAddress)
				.containsExactly("A1:A10", "B1:B10", "C1:C10", "D1:D10", "E1:E10");
	}

	@Test
	void freieBereiche_sparenBelegteZellenAus() {
		// Ergebnisspalten D/E ab Zeile 3 belegt, Bahnspalte A Zeilen 3..5 belegt
		List<RangePosition> belegt = List.of(RangePosition.from(3, 2, 4, 9), RangePosition.from(0, 2, 0, 4));
		List<RangePosition> frei = NichtGewertetFormatHelper.freieBereiche(TABELLE, belegt);
		assertThat(frei).extracting(RangePosition::getAddress)
				.containsExactly("A1:A2", "A6:A10", "B1:B10", "C1:C10", "D1:D2", "E1:E2");
	}
}
