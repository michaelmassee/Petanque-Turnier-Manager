package de.petanqueturniermanager.helper.pagestyle;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.junit.jupiter.api.Test;

public class HeaderFooterTokenResolverTest {

	@Test
	public void textOhnePlatzhalterBleibtUnveraendert() {
		String text = "* Pétanque-Turnier-Manager *";
		assertThat(HeaderFooterTokenResolver.resolve(text, "Meldeliste", "turnier.ods")).isEqualTo(text);
	}

	@Test
	public void tabelleWirdDurchEchtenSheetNamenErsetzt() {
		String ergebnis = HeaderFooterTokenResolver.resolve("Blatt: {TABELLE}", "Spielrunde 3", "turnier.ods");
		assertThat(ergebnis).isEqualTo("Blatt: Spielrunde 3");
	}

	@Test
	public void dateinameWirdErsetzt() {
		String ergebnis = HeaderFooterTokenResolver.resolve("{DATEINAME}", "Meldeliste", "turnier.ods");
		assertThat(ergebnis).isEqualTo("turnier.ods");
	}

	@Test
	public void seiteUndSeitenWerdenAufDerWebseiteAusgeblendet() {
		String ergebnis = HeaderFooterTokenResolver.resolve("Seite {SEITE} von {SEITEN}", "Meldeliste", "turnier.ods");
		assertThat(ergebnis).isEqualTo("Seite  von ");
	}

	@Test
	public void datumWirdAufAktuellesDatumAufgeloest() {
		String erwartet = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(LocalDate.now());
		String ergebnis = HeaderFooterTokenResolver.resolve("{DATUM}", "Meldeliste", "turnier.ods");
		assertThat(ergebnis).isEqualTo(erwartet);
	}

	@Test
	public void nullTabellenameUndDateinameErgebenLeerenString() {
		String ergebnis = HeaderFooterTokenResolver.resolve("{TABELLE}-{DATEINAME}", null, null);
		assertThat(ergebnis).isEqualTo("-");
	}
}
