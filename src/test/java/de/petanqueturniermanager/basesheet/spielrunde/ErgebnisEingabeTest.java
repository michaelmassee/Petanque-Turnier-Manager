/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.helper.sheet.rangedata.CellData;

/** Ein Ergebnis gilt nur als eingetragen, wenn beide Zellen eine Zahl enthalten. */
class ErgebnisEingabeTest {

	/** {@code getDataArray} liefert für leere Zellen einen leeren String, für Zahlen ein Double. */
	private static final CellData LEER = new CellData("");

	private static CellData zahl(double wert) {
		return new CellData(wert);
	}

	@Test
	void beideZahlen_istEingetragen() {
		assertThat(ErgebnisEingabe.istEingetragen(zahl(13), zahl(7))).isTrue();
	}

	@Test
	void nullPunkteGegner_istEingetragen() {
		assertThat(ErgebnisEingabe.istEingetragen(zahl(13), zahl(0))).isTrue();
		assertThat(ErgebnisEingabe.istEingetragen(zahl(0), zahl(13))).isTrue();
	}

	@Test
	void eineSeiteLeer_istNichtEingetragen() {
		assertThat(ErgebnisEingabe.istEingetragen(zahl(13), LEER)).isFalse();
		assertThat(ErgebnisEingabe.istEingetragen(LEER, zahl(13))).isFalse();
	}

	@Test
	void beideLeer_istNichtEingetragen() {
		assertThat(ErgebnisEingabe.istEingetragen(LEER, LEER)).isFalse();
		assertThat(ErgebnisEingabe.istEingetragen(new CellData(null), new CellData(null))).isFalse();
	}

	@Test
	void nullZuNull_istNichtEingetragen() {
		assertThat(ErgebnisEingabe.istEingetragen(zahl(0), zahl(0))).isFalse();
	}

	@Test
	void text_istNichtEingetragen() {
		assertThat(ErgebnisEingabe.istEingetragen(zahl(13), new CellData("x"))).isFalse();
	}
}
