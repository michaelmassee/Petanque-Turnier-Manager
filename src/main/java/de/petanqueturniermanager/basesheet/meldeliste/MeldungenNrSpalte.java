package de.petanqueturniermanager.basesheet.meldeliste;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;
import de.petanqueturniermanager.helper.sheet.search.RangeSearchHelper;

/**
 * Erstellung 25.08.2022 / Michael Massee
 */

public class MeldungenNrSpalte {

	private final int meldungNrSpalteNr;
	private final WeakRefHelper<ISheet> sheet;
	private final int ersteDatenZiele; // Zeile 1 = 0
	private final int maxAnzMeldungen;

	public MeldungenNrSpalte(int meldungNrSpalteNr, ISheet iSheet, int ersteDatenZiele, int maxAnzMeldungen) {
		checkArgument(meldungNrSpalteNr > -1);
		checkArgument(ersteDatenZiele > -1);
		checkArgument(maxAnzMeldungen > 0);
		this.meldungNrSpalteNr = meldungNrSpalteNr;
		this.sheet = new WeakRefHelper<>(checkNotNull(iSheet));
		this.ersteDatenZiele = ersteDatenZiele;
		this.maxAnzMeldungen = maxAnzMeldungen;
	}

	/**
	 * Sucht von unten nach der erste nicht leere zelle - 1<br>
	 * Spalte spielerNr <br>
	 * Achtung wenn bereits footer daten vorhanden, und oder wietere Daten unter der letzte Spielnr<br>
	 * 
	 * @return
	 * @throws GenerateException
	 */
	public int getLetzteMitDatenZeileInSpielerNrSpalte() throws GenerateException {
		return neachsteFreieDatenZeileInSpielerNrSpalte() - 1;
	}

	/**
	 * Sucht von unten nach der erste nicht leere zelle<br>
	 * Spalte spielerNr <br>
	 * Achtung wenn bereits footer daten vorhanden, und oder wietere Daten unter der letzte Spielnr<br>
	 * return ersteDatenzeile wenn kein vorhanden
	 *
	 * @throws GenerateException
	 */
	public int neachsteFreieDatenZeileInSpielerNrSpalte() throws GenerateException {
		Position result = RangeSearchHelper
				.from(getISheet(),
						RangePosition.from(meldungNrSpalteNr, ersteDatenZiele, meldungNrSpalteNr, maxAnzMeldungen))
				.searchLastEmptyInSpalte();
		if (result != null) {
			return result.getZeile();
		}
		return ersteDatenZiele;
	}

	private final XSpreadsheet getXSpreadsheet() throws GenerateException {
		return getISheet().getXSpreadSheet();
	}

	private final ISheet getISheet() {
		return sheet.get();
	}

}
