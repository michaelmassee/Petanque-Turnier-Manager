/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.schweizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.schweizer.meldeliste.TeamAnzeigeIndex;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;

class SchweizerTeamNrAufloeserTest {

	private static final int TECHNISCH_A_IDX = SchweizerAbstractSpielrundeSheet.TECHNISCHE_TEAM_A_NR_SPALTE
			- SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE;

	private final AtomicInteger anzIndexLaden = new AtomicInteger();

	@Test
	void sichtbareNummerHatVorrangVorTechnischerNummer() throws GenerateException {
		// Nummern-Anzeige mit manueller Korrektur: sichtbar 7, versteckt noch 3
		RowData zeile = zeile(7.0, 8.0, 3, 4);

		var aufloeser = aufloeser(new TeamAnzeigeIndex());

		assertThat(aufloeser.teamA(zeile)).isEqualTo(7);
		assertThat(aufloeser.teamB(zeile)).isEqualTo(8);
		assertThat(anzIndexLaden).hasValue(0);
	}

	@Test
	void namensanzeigeNutztTechnischeNummer() throws GenerateException {
		RowData zeile = zeile("Anna / Berta", "Carl / Dora", 3, 4);

		var aufloeser = aufloeser(new TeamAnzeigeIndex());

		assertThat(aufloeser.teamA(zeile)).isEqualTo(3);
		assertThat(aufloeser.teamB(zeile)).isEqualTo(4);
		assertThat(anzIndexLaden).hasValue(0);
	}

	@Test
	void alteDateiOhneTechnischeSpaltenLoestUeberMeldelisteAuf() throws GenerateException {
		TeamAnzeigeIndex index = new TeamAnzeigeIndex();
		index.hinzufuegen("Anna / Berta", 3);
		index.hinzufuegen("Carl / Dora", 4);
		RowData zeile = zeileOhneTechnischeSpalten("Anna / Berta", "Carl / Dora");

		var aufloeser = aufloeser(index);

		assertThat(aufloeser.teamA(zeile)).isEqualTo(3);
		assertThat(aufloeser.teamB(zeile)).isEqualTo(4);
		assertThat(anzIndexLaden).as("Meldeliste nur einmal lesen").hasValue(1);
	}

	@Test
	void leererGegnerIstFreilos() throws GenerateException {
		RowData zeile = zeile("Anna / Berta", "", 3, 0);

		assertThat(aufloeser(new TeamAnzeigeIndex()).teamB(zeile)).isZero();
	}

	@Test
	void leereZeileIstDatenende() throws GenerateException {
		assertThat(aufloeser(new TeamAnzeigeIndex()).teamA(zeile("", "", "", ""))).isZero();
	}

	@Test
	void mehrdeutigerGegnerWirdNichtAlsFreilosGewertet() {
		TeamAnzeigeIndex index = new TeamAnzeigeIndex();
		index.hinzufuegen("Anna", 3);
		index.hinzufuegen("Anna", 5);
		RowData zeile = zeileOhneTechnischeSpalten("Carl", "Anna");

		assertThatThrownBy(() -> aufloeser(index).teamB(zeile)).isInstanceOf(GenerateException.class);
	}

	@Test
	void unbekannteKennungWirdNichtAlsDatenendeGewertet() {
		RowData zeile = zeileOhneTechnischeSpalten("Gelöschtes Team", "");

		assertThatThrownBy(() -> aufloeser(new TeamAnzeigeIndex()).teamA(zeile))
				.isInstanceOf(GenerateException.class);
	}

	private SchweizerTeamNrAufloeser aufloeser(TeamAnzeigeIndex index) {
		return new SchweizerTeamNrAufloeser(() -> {
			anzIndexLaden.incrementAndGet();
			return index;
		});
	}

	/** Zeile ab TEAM_A_SPALTE bis einschließlich der technischen Spalten. */
	private static RowData zeile(Object teamA, Object teamB, Object technischA, Object technischB) {
		RowData zeile = zeileOhneTechnischeSpalten(teamA, teamB);
		while (zeile.size() < TECHNISCH_A_IDX) {
			zeile.add(new CellData(""));
		}
		zeile.add(new CellData(technischA));
		zeile.add(new CellData(technischB));
		return zeile;
	}

	private static RowData zeileOhneTechnischeSpalten(Object teamA, Object teamB) {
		RowData zeile = new RowData();
		zeile.add(new CellData(teamA));
		zeile.add(new CellData(teamB));
		return zeile;
	}
}
