/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko.konfiguration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests für {@link KoPropertiesSpalte#normalisiereGruppenGroesse(int)},
 * {@link KoPropertiesSpalte#normalisiereGruppenGroesse(String)} und die analogen
 * {@code normalisiereMinLetzteGruppeGroesse}-Methoden. Erwartung: beliebige Werte im Bereich
 * [2, 256] bleiben unverändert (keine Beschränkung auf Zweierpotenzen mehr); Werte außerhalb
 * des Bereichs werden gekappt; Default ist 16 (Gruppengröße) bzw. 4 (Min. letzte Gruppe) für
 * ungültige/leere Werte.
 */
public class KoPropertiesSpalteGruppenGroesseTest {

	@Test
	public void beliebigeWerteImBereichBleibenUnverändert() {
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(2)).isEqualTo(2);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(3)).isEqualTo(3);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(16)).isEqualTo(16);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(19)).isEqualTo(19);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(20)).isEqualTo(20);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(256)).isEqualTo(256);
	}

	@Test
	public void werteUnter2WerdenAufMinimumGekappt() {
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(1)).isEqualTo(2);
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
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse("20")).isEqualTo(20);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse(" 12 ")).isEqualTo(12);
	}

	@Test
	public void stringFloatRepräsentation() {
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse("16.0")).isEqualTo(16);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse("12.0")).isEqualTo(12);
	}

	@Test
	public void stringLeerOderUngültigErgibtDefault() {
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse((String) null)).isEqualTo(16);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse("")).isEqualTo(16);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse("   ")).isEqualTo(16);
		assertThat(KoPropertiesSpalte.normalisiereGruppenGroesse("foo")).isEqualTo(16);
	}

	@Test
	public void minLetzteGruppeBeliebigeWerteImBereichBleibenUnverändert() {
		assertThat(KoPropertiesSpalte.normalisiereMinLetzteGruppeGroesse(2)).isEqualTo(2);
		assertThat(KoPropertiesSpalte.normalisiereMinLetzteGruppeGroesse(7)).isEqualTo(7);
		assertThat(KoPropertiesSpalte.normalisiereMinLetzteGruppeGroesse(256)).isEqualTo(256);
	}

	@Test
	public void minLetzteGruppeWerteAußerhalbBereichWerdenGekappt() {
		assertThat(KoPropertiesSpalte.normalisiereMinLetzteGruppeGroesse(1)).isEqualTo(2);
		assertThat(KoPropertiesSpalte.normalisiereMinLetzteGruppeGroesse(300)).isEqualTo(256);
	}

	@Test
	public void minLetzteGruppeNullOderNegativErgibtDefault() {
		assertThat(KoPropertiesSpalte.normalisiereMinLetzteGruppeGroesse(0)).isEqualTo(4);
		assertThat(KoPropertiesSpalte.normalisiereMinLetzteGruppeGroesse(-1)).isEqualTo(4);
	}

	@Test
	public void minLetzteGruppeStringLeerOderUngültigErgibtDefault() {
		assertThat(KoPropertiesSpalte.normalisiereMinLetzteGruppeGroesse((String) null)).isEqualTo(4);
		assertThat(KoPropertiesSpalte.normalisiereMinLetzteGruppeGroesse("")).isEqualTo(4);
		assertThat(KoPropertiesSpalte.normalisiereMinLetzteGruppeGroesse("foo")).isEqualTo(4);
	}
}
