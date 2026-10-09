/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TeamAnzeigeTest {

	@Test
	void teamnameOhneTeamnameSpalteFaelltAufSpielernamenZurueck() {
		assertThat(TeamAnzeige.NAME.effektiv(false)).isEqualTo(TeamAnzeige.SPIELERNAMEN);
		assertThat(TeamAnzeige.NAME.effektiv(true)).isEqualTo(TeamAnzeige.NAME);
	}

	@Test
	void nummerUndSpielernamenBleibenUnabhaengigVonDerTeamnameSpalte() {
		for (boolean teamnameAktiv : new boolean[] { true, false }) {
			assertThat(TeamAnzeige.NR.effektiv(teamnameAktiv)).isEqualTo(TeamAnzeige.NR);
			assertThat(TeamAnzeige.SPIELERNAMEN.effektiv(teamnameAktiv)).isEqualTo(TeamAnzeige.SPIELERNAMEN);
		}
	}

	@Test
	void dialogIndexIstUmkehrbar() {
		for (TeamAnzeige anzeige : TeamAnzeige.values()) {
			assertThat(TeamAnzeige.ausDialogIndex(anzeige.dialogIndex())).isEqualTo(anzeige);
		}
	}

	@Test
	void dialogReihenfolgeEntsprichtDenAuswahllisten() {
		assertThat(TeamAnzeige.NR.dialogIndex()).isZero();
		assertThat(TeamAnzeige.SPIELERNAMEN.dialogIndex()).isEqualTo((short) 1);
		assertThat(TeamAnzeige.NAME.dialogIndex()).isEqualTo((short) 2);
	}

	@Test
	void unbekannterDialogIndexErgibtNummer() {
		assertThat(TeamAnzeige.ausDialogIndex((short) -1)).isEqualTo(TeamAnzeige.NR);
		assertThat(TeamAnzeige.ausDialogIndex((short) 3)).isEqualTo(TeamAnzeige.NR);
	}

	@Test
	void jedeAnzeigeHatEigeneKopfzeile() {
		assertThat(TeamAnzeigeHelper.headerI18nKey(TeamAnzeige.NR)).isEqualTo("column.header.nr");
		assertThat(TeamAnzeigeHelper.headerI18nKey(TeamAnzeige.SPIELERNAMEN)).isEqualTo("column.header.spieler");
		assertThat(TeamAnzeigeHelper.headerI18nKey(TeamAnzeige.NAME)).isEqualTo("column.header.teamname");
	}

	@Test
	void nummernAnzeigeVerwendetDieNummernzelleDirekt() {
		assertThat(TeamAnzeigeHelper.formel("A3", TeamAnzeige.NR, true, Formation.DOUBLETTE, false)).isEqualTo("A3");
	}

	@Test
	void spielernamenBeruecksichtigenDieTeamnameSpalte() {
		assertThat(TeamAnzeigeHelper.formel("A3", TeamAnzeige.SPIELERNAMEN, true, Formation.DOUBLETTE, false))
				.isEqualTo(MeldeListeHelper.spielerNamenFormel("A3", true, Formation.DOUBLETTE, false));
		assertThat(TeamAnzeigeHelper.formel("A3", TeamAnzeige.SPIELERNAMEN, false, Formation.DOUBLETTE, false))
				.isEqualTo(MeldeListeHelper.spielerNamenFormel("A3", false, Formation.DOUBLETTE, false));
	}

	@Test
	void teamnameOhneTeamnameSpalteZeigtSpielernamen() {
		assertThat(TeamAnzeigeHelper.formel("A3", TeamAnzeige.NAME, false, Formation.DOUBLETTE, false))
				.isEqualTo(MeldeListeHelper.spielerNamenFormel("A3", false, Formation.DOUBLETTE, false));
		assertThat(TeamAnzeigeHelper.formel("A3", TeamAnzeige.NAME, true, Formation.DOUBLETTE, false))
				.isEqualTo(MeldeListeHelper.teamNameFormel("A3", true, Formation.DOUBLETTE, false));
	}
}
