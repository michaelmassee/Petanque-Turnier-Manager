/**
* Erstellung : 10.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.sheet;

import java.util.List;

import de.petanqueturniermanager.exception.GenerateException;

public interface IMitSpielerSpalte {

	int neachsteFreieDatenZeile() throws GenerateException;

	int getSpielerZeileNr(int spielerNr) throws GenerateException;

	int letzteDatenZeile() throws GenerateException;

	int getErsteDatenZiele();

	List<String> getSpielerNamenList() throws GenerateException;

	List<Integer> getSpielerNrList() throws GenerateException;

}
