/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.siegergeld;

import java.util.List;

import de.petanqueturniermanager.exception.GenerateException;

interface SiegergeldQuelle {
	List<SiegergeldEintrag> leseTop(int maxPlatz) throws GenerateException;

	default List<SiegergeldEintrag> leseTop3() throws GenerateException {
		return leseTop(3);
	}

	default List<SiegergeldEintrag> allgemeineEintraege() throws GenerateException {
		return SiegergeldAllgemeineEintraege.einzelgruppe(3);
	}

	int teilnehmerAnzahl() throws GenerateException;
}
