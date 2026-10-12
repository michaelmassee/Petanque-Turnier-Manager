/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.basesheet.meldeliste.NrZellFormel.Art;

class NrZellFormelTest {

	private static final String UUID_LOKAL = "0b8c1f8e-3c55-4b7a-9d1e-2f6a7c9e1a01";
	private static final String UUID_ONLINE = "7f3e2d1c-0b9a-4876-a5d4-c3b2a1908f7e";

	@Test
	void teamOhneOnlineId() {
		assertThat(NrZellFormel.team(UUID_LOKAL).formel(7)).isEqualTo("PTM.ALG.TNRUUID(7;\"" + UUID_LOKAL + "\")");
	}

	@Test
	void spielerMitOnlineIdHinUndZurueck() {
		String formel = NrZellFormel.spieler(UUID_LOKAL).mitOnlineId(Optional.of(UUID_ONLINE)).formel(3);
		assertThat(formel).isEqualTo("PTM.ALG.SPLRUUID(3;\"" + UUID_LOKAL + "\";\"" + UUID_ONLINE + "\")");

		NrZellFormel gelesen = NrZellFormel.lese(formel, Art.TEAM).orElseThrow();
		assertThat(gelesen.art()).isEqualTo(Art.SPIELER);
		assertThat(gelesen.uuid()).isEqualTo(UUID_LOKAL);
		assertThat(gelesen.onlineId()).contains(UUID_ONLINE);
	}

	@Test
	void programmatischerFunktionsnameWirdErkannt() {
		String formel = "de.petanqueturniermanager.addin.GlobalAddIn.ptmsplruuid(3;\"" + UUID_LOKAL + "\";\"r1\")";
		NrZellFormel gelesen = NrZellFormel.lese(formel, Art.TEAM).orElseThrow();
		assertThat(gelesen.art()).isEqualTo(Art.SPIELER);
		assertThat(gelesen.onlineId()).contains("r1");
	}

	@Test
	void unbekannterNameNimmtStandardArt() {
		assertThat(NrZellFormel.lese("X(1;\"" + UUID_LOKAL + "\")", Art.SPIELER).orElseThrow().art())
				.isEqualTo(Art.SPIELER);
	}

	@Test
	void umnummerierenBehaeltOnlineId() {
		String alt = "PTM.ALG.TNRUUID(3;\"" + UUID_LOKAL + "\";\"r1\")";
		assertThat(NrZellFormel.lese(alt, Art.TEAM).orElseThrow().formel(12))
				.isEqualTo("PTM.ALG.TNRUUID(12;\"" + UUID_LOKAL + "\";\"r1\")");
	}

	@Test
	void leereOnlineIdEntfaellt() {
		assertThat(NrZellFormel.team(UUID_LOKAL).mitOnlineId(Optional.of(" ")).onlineId()).isEmpty();
		assertThat(NrZellFormel.onlineIdAus("PTM.ALG.TNRUUID(3;\"" + UUID_LOKAL + "\";\"\")")).isEmpty();
	}

	@Test
	void ohneUuidKeinInhalt() {
		assertThat(NrZellFormel.lese("7", Art.TEAM)).isEmpty();
		assertThat(NrZellFormel.lese("", Art.TEAM)).isEmpty();
		assertThat(NrZellFormel.lese(null, Art.TEAM)).isEmpty();
	}
}
