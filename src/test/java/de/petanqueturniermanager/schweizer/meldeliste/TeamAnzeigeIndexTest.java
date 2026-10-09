/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TeamAnzeigeIndexTest {

	@Test
	void eindeutigeKennungLiefertTeamNr() {
		TeamAnzeigeIndex index = new TeamAnzeigeIndex();
		index.hinzufuegen("Anna / Berta", 3);

		assertThat(index.teamNr("Anna / Berta")).isEqualTo(3);
		assertThat(index.istMehrdeutig("Anna / Berta")).isFalse();
	}

	@Test
	void unbekannteKennungLiefertNull() {
		assertThat(new TeamAnzeigeIndex().teamNr("Unbekannt")).isZero();
	}

	@Test
	void gleicheKennungFuerDasselbeTeamBleibtEindeutig() {
		TeamAnzeigeIndex index = new TeamAnzeigeIndex();
		index.hinzufuegen("Anna", 3);
		index.hinzufuegen("Anna", 3);

		assertThat(index.teamNr("Anna")).isEqualTo(3);
		assertThat(index.istMehrdeutig("Anna")).isFalse();
	}

	@Test
	void gleicheKennungVerschiedenerTeamsIstMehrdeutig() {
		TeamAnzeigeIndex index = new TeamAnzeigeIndex();
		index.hinzufuegen("Anna", 3);
		index.hinzufuegen("Anna", 5);
		index.hinzufuegen("Anna", 3);

		assertThat(index.istMehrdeutig("Anna")).isTrue();
		assertThat(index.teamNr("Anna")).isZero();
	}

	@Test
	void leereKennungenUndUngueltigeNummernWerdenIgnoriert() {
		TeamAnzeigeIndex index = new TeamAnzeigeIndex();
		index.hinzufuegen("", 1);
		index.hinzufuegen(" ", 2);
		index.hinzufuegen(null, 3);
		index.hinzufuegen("Anna", 0);

		assertThat(index.teamNr("")).isZero();
		assertThat(index.teamNr("Anna")).isZero();
	}
}
