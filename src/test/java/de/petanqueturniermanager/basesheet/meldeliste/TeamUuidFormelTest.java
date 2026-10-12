/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TeamUuidFormelTest {

	private static final String UUID = "3f2b8c1a-9d4e-4f6a-8b7c-1e2d3f4a5b6c";

	@Test
	void findetUuidInNrFormel() {
		assertThat(TeamUuidFormel.findeUuid("=PTM.ALG.TNRUUID(7;\"" + UUID + "\")")).contains(UUID);
	}

	@Test
	void findetUuidUnterProgrammatischemFunktionsnamen() {
		String formel = "=de.petanqueturniermanager.addin.GlobalAddIn.ptmtnruuid(12;\"" + UUID + "\")";
		assertThat(TeamUuidFormel.findeUuid(formel)).contains(UUID);
	}

	@Test
	void liefertUuidKleingeschrieben() {
		assertThat(TeamUuidFormel.findeUuid("=PTM.ALG.TNRUUID(1;\"" + UUID.toUpperCase() + "\")")).contains(UUID);
	}

	@Test
	void reineZahlOderLeereZelleHatKeineUuid() {
		assertThat(TeamUuidFormel.findeUuid("7")).isEmpty();
		assertThat(TeamUuidFormel.findeUuid("")).isEmpty();
		assertThat(TeamUuidFormel.findeUuid(null)).isEmpty();
	}

	@Test
	void findetZeileDerUuidImBereich() {
		String[][] formeln = { { "Nr" }, { "" }, { "=PTM.ALG.TNRUUID(4;\"" + UUID + "\")" }, { "5" } };
		assertThat(TeamUuidFormel.zeileImBereich(UUID, formeln)).hasValue(3);
		assertThat(TeamUuidFormel.zeileImBereich(UUID.toUpperCase(), formeln)).hasValue(3);
	}

	@Test
	void unbekannteOderUngueltigeUuidHatKeineZeile() {
		String[][] formeln = { { "=PTM.ALG.TNRUUID(4;\"" + UUID + "\")" } };
		assertThat(TeamUuidFormel.zeileImBereich("00000000-0000-0000-0000-000000000000", formeln)).isEmpty();
		assertThat(TeamUuidFormel.zeileImBereich("", formeln)).isEmpty();
		assertThat(TeamUuidFormel.zeileImBereich(UUID, null)).isEmpty();
	}

	@Test
	void liefertNrDerZeileMitDerUuid() {
		String[][] formeln = { { "Nr" }, { "=PTM.ALG.TNRUUID(4;\"" + UUID + "\")" } };
		Object[][] werte = { { "Nr" }, { Double.valueOf(4) } };
		assertThat(TeamUuidFormel.nrImBereich(UUID, formeln, werte)).hasValue(4);
	}

	@Test
	void ohneTrefferOderOhneZahlKeineNr() {
		String[][] formeln = { { "=PTM.ALG.TNRUUID(4;\"" + UUID + "\")" } };
		assertThat(TeamUuidFormel.nrImBereich("00000000-0000-0000-0000-000000000000", formeln,
				new Object[][] { { Double.valueOf(4) } })).isEmpty();
		assertThat(TeamUuidFormel.nrImBereich(UUID, formeln, new Object[][] { { "" } })).isEmpty();
		assertThat(TeamUuidFormel.nrImBereich(UUID, formeln, new Object[0][0])).isEmpty();
		assertThat(TeamUuidFormel.nrImBereich(UUID, formeln, null)).isEmpty();
	}

	@Test
	void erkenntGenauEineUuid() {
		assertThat(TeamUuidFormel.istUuid(UUID)).isTrue();
		assertThat(TeamUuidFormel.istUuid(" " + UUID)).isFalse();
		assertThat(TeamUuidFormel.istUuid("keine-uuid")).isFalse();
		assertThat(TeamUuidFormel.istUuid(null)).isFalse();
	}
}
