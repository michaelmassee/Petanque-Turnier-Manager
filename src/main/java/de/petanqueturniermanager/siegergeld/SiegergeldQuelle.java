/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.siegergeld;

import java.util.List;

import de.petanqueturniermanager.exception.GenerateException;

interface SiegergeldQuelle {
	List<SiegergeldEintrag> leseTop3() throws GenerateException;

	default List<SiegergeldEintrag> allgemeineEintraege() throws GenerateException {
		return SiegergeldAllgemeineEintraege.einzelgruppe(3);
	}

	int teilnehmerAnzahl() throws GenerateException;
}
