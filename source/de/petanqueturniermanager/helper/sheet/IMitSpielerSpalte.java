/**
* Erstellung : 10.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.sheet;

import java.util.List;

public interface IMitSpielerSpalte {

	int neachsteFreieDatenZeile();

	int getSpielerZeileNr(int spielerNr);

	void spielerEinfuegenWennNichtVorhanden(int spielerNr);

	public int letzteDatenZeile();

	int getErsteDatenZiele();

	List<String> getSpielerNamenList();

}
