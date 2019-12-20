/**
* Erstellung : 10.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.sheet;

import java.util.List;

import de.petanqueturniermanager.exception.GenerateException;

public interface IMitSpielerSpalte {

	int getErsteDatenZiele();

	int letzteDatenZeile() throws GenerateException;

	int neachsteFreieDatenOhneSpielerNrZeile() throws GenerateException;

	int letzteZeileMitSpielerName() throws GenerateException;

	int getSpielerZeileNr(int spielerNr) throws GenerateException;

	List<String> getSpielerNamenList() throws GenerateException;

	List<Integer> getSpielerNrList() throws GenerateException;

}
