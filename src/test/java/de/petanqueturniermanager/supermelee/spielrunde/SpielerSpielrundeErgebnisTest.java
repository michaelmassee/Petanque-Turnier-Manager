package de.petanqueturniermanager.supermelee.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

public class SpielerSpielrundeErgebnisTest {

	@Test
	public void testFromSpielRundeNrIntPositionIntSpielRundeTeamB() throws Exception {
		SpielerSpielrundeErgebnis result = newTestSpielerSpielrundeErgebnisTeamB();
		validateSpielrundeErg(result);
	}

	@Test
	public void testFromSpielRundeNrIntPositionIntSpielRundeTeamA() throws Exception {

		SpielRundeNr spielrunde = SpielRundeNr.from(8);
		int spielerNr = 29;
		Position positionSpielerNr = Position.from(8, 32);
		int ersteSpalteErgebnisse = 12;
		SpielRundeTeam spielRundeTeam = SpielRundeTeam.A;
		SpielerSpielrundeErgebnis result = SpielerSpielrundeErgebnis.from(spielrunde, spielerNr, positionSpielerNr, ersteSpalteErgebnisse, spielRundeTeam);

		assertThat(result.getPositionMinusPunkte()).isNotNull(); // Rechter spalte
		assertThat(result.getPositionMinusPunkte().getSpalte()).isEqualTo(13);
		assertThat(result.getPositionMinusPunkte().getZeile()).isEqualTo(32);

		assertThat(result.getPositionPlusPunkte()).isNotNull(); // Rechter spalte
		assertThat(result.getPositionPlusPunkte().getSpalte()).isEqualTo(12);
		assertThat(result.getPositionPlusPunkte().getZeile()).isEqualTo(32);
	}

	@Test
	public void testFromSpielerSpielrundeErgebnis() throws Exception {
		SpielerSpielrundeErgebnis orginal = newTestSpielerSpielrundeErgebnisTeamB();
		SpielerSpielrundeErgebnis copy = SpielerSpielrundeErgebnis.from(orginal);

		validateSpielrundeErg(copy);
	}

	private SpielerSpielrundeErgebnis newTestSpielerSpielrundeErgebnisTeamB() {
		SpielRundeNr spielrunde = SpielRundeNr.from(3);
		int spielerNr = 22;
		Position positionSpielerNr = Position.from(8, 20);
		int ersteSpalteErgebnisse = 5;
		SpielRundeTeam spielRundeTeam = SpielRundeTeam.B;
		return SpielerSpielrundeErgebnis.from(spielrunde, spielerNr, positionSpielerNr, ersteSpalteErgebnisse, spielRundeTeam);
	}

	private void validateSpielrundeErg(SpielerSpielrundeErgebnis spielerSpielrundeErgebnis) {
		assertThat(spielerSpielrundeErgebnis.getPositionMinusPunkte()).isNotNull(); // Linker spalte
		assertThat(spielerSpielrundeErgebnis.getPositionMinusPunkte().getSpalte()).isEqualTo(5);
		assertThat(spielerSpielrundeErgebnis.getPositionMinusPunkte().getZeile()).isEqualTo(20);

		assertThat(spielerSpielrundeErgebnis.getPositionPlusPunkte()).isNotNull(); // Rechter spalte
		assertThat(spielerSpielrundeErgebnis.getPositionPlusPunkte().getSpalte()).isEqualTo(6);
		assertThat(spielerSpielrundeErgebnis.getPositionPlusPunkte().getZeile()).isEqualTo(20);

		assertThat(spielerSpielrundeErgebnis.getPositionSpielerNr()).isNotNull();
		assertThat(spielerSpielrundeErgebnis.getPositionSpielerNr().getSpalte()).isEqualTo(8);
		assertThat(spielerSpielrundeErgebnis.getPositionSpielerNr().getZeile()).isEqualTo(20);

		assertThat(spielerSpielrundeErgebnis.getSpielerNr()).isEqualTo(22);
		assertThat(spielerSpielrundeErgebnis.getSpielrunde().getNr()).isEqualTo(3);
		assertThat(spielerSpielrundeErgebnis.getSpielRundeTeam()).isEqualTo(SpielRundeTeam.B);
	}

}
