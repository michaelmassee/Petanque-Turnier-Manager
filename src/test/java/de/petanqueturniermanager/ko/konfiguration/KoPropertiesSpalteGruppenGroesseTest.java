/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko.konfiguration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests für {@link KoPropertiesSpalte#normalisiereGruppenGroesse(int)} und
 * {@link KoPropertiesSpalte#normalisiereGruppenGroesse(String)}. Erwartung:
 * Eingaben werden auf die nächst-höhere Zweierpotenz aus {4,8,16,32,64,128,256}
 * gesnapped; Default ist 16 für ungültige/leere Werte; Werte über 256 werden gekappt.
 */
public class KoPropertiesSpalteGruppenGroesseTest {

	@Test
	public void erlaubteWertePassierenUnverändert() {
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(4)).isEqualTo(4);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(8)).isEqualTo(8);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(16)).isEqualTo(16);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(32)).isEqualTo(32);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(64)).isEqualTo(64);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(128)).isEqualTo(128);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(256)).isEqualTo(256);
	}

	@Test
	public void zwischenwerteSnapAufNächstHöhere() {
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(1)).isEqualTo(4);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(2)).isEqualTo(4);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(3)).isEqualTo(4);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(5)).isEqualTo(8);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(12)).isEqualTo(16);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(33)).isEqualTo(64);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(100)).isEqualTo(128);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(200)).isEqualTo(256);
	}

	@Test
	public void werteÜber256WerdenGekappt() {
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(257)).isEqualTo(256);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(1024)).isEqualTo(256);
	}

	@Test
	public void nullOderNegativErgibtDefault() {
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(0)).isEqualTo(16);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(-1)).isEqualTo(16);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(-99)).isEqualTo(16);
	}

	@Test
	public void stringValideZahl() {
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse("16")).isEqualTo(16);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse("32")).isEqualTo(32);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(" 12 ")).isEqualTo(16);
	}

	@Test
	public void stringFloatRepräsentation() {
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse("16.0")).isEqualTo(16);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse("12.0")).isEqualTo(16);
	}

	@Test
	public void stringLeerOderUngültigErgibtDefault() {
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse((String) null)).isEqualTo(16);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse("")).isEqualTo(16);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse("   ")).isEqualTo(16);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse("foo")).isEqualTo(16);
	}
}
