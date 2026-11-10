/*
 * Erstellung 10.11.2026 / Michael Massee
 */
package de.petanqueturniermanager.liga.rangliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Nur Begegnungen mit eindeutigem Sieger zählen in der Liga-Rangliste (Issue #27).
 */
class LigaRanglisteEindeutigerSiegerTest {

	@Test
	void gueltigeErgebnisseZaehlen() {
		assertThat(LigaRanglisteSheet.hatEindeutigenSieger(3, 2)).isTrue();
		assertThat(LigaRanglisteSheet.hatEindeutigenSieger(0, 5)).isTrue();
	}

	@Test
	void gleichstandUndNichtGespieltZaehlenNicht() {
		assertThat(LigaRanglisteSheet.hatEindeutigenSieger(3, 3)).isFalse();
		assertThat(LigaRanglisteSheet.hatEindeutigenSieger(0, 0)).isFalse();
	}
}
