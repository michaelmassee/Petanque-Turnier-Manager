/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.jedergegenjeden.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;

class JGJStartDialogTest {

	@Test
	void ordnetAlleSpielplanAnzeigeEintraegeDerZentralenEnumZu() {
		assertThat(JGJStartDialog.spielplanAnzeige((short) 0)).isEqualTo(TeamAnzeige.NR);
		assertThat(JGJStartDialog.spielplanAnzeige((short) 1)).isEqualTo(TeamAnzeige.SPIELERNAMEN);
		assertThat(JGJStartDialog.spielplanAnzeige((short) 2)).isEqualTo(TeamAnzeige.NAME);
	}
}
