/**
* Erstellung : 14.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.endrangliste;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.table.CellHoriJustify;

import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.rangliste.AbstractRanglisteFormatter;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;
import de.petanqueturniermanager.supermelee.konfiguration.ISuperMeleePropertiesSpalte;

public class EndRanglisteFormatter extends AbstractRanglisteFormatter {

	private final WeakRefHelper<IEndRangliste> ranglisteWkRef;
	private final int anzSpaltenInSpieltag;
	private final int ersteSpielTagSpalte;

	public EndRanglisteFormatter(IEndRangliste rangliste, int anzSpaltenInSpieltag, MeldungenSpalte spielerSpalte, int ersteSpielTagSpalte, ISuperMeleePropertiesSpalte propertiesSpalte) {
		super(spielerSpalte, propertiesSpalte, rangliste);
		checkNotNull(rangliste);
		this.ranglisteWkRef = new WeakRefHelper<IEndRangliste>(rangliste);
		this.anzSpaltenInSpieltag = anzSpaltenInSpieltag;
		this.ersteSpielTagSpalte = ersteSpielTagSpalte;
	}

	public void updateHeader() throws GenerateException {
		IEndRangliste rangliste = this.ranglisteWkRef.get();
		int anzSpieltagen = rangliste.getAnzahlSpieltage();
		if (anzSpieltagen < 1) {
			return;
		}
		// -------------------------
		// spieltag spalten
		// -------------------------
		StringCellValue spieltagheader = StringCellValue.from(getSheet(), Position.from(ERSTE_KOPFDATEN_ZEILE, 0)).setCellBackColor(getHeaderFarbe())
				.setBorder(BorderFactory.from().allThin().boldLn().forLeft().forTop().forRight().toBorder()).setHoriJustify(CellHoriJustify.CENTER);
		for (int spielTag = 1; spielTag <= anzSpieltagen; spielTag++) {
			int ersteSpalteSpieltagBlock = this.ersteSpielTagSpalte + ((spielTag - 1) * this.anzSpaltenInSpieltag);
			// ERSTE_KOPFDATEN_ZEILE
			spieltagheader.spalte(ersteSpalteSpieltagBlock).setValue(spielTag + ". Spieltag").setEndPosMergeSpaltePlus(this.anzSpaltenInSpieltag - 1);
			getSheetHelper().setStringValueInCell(spieltagheader);
			formatZweiteZeileSpielTagSpalten(ersteSpalteSpieltagBlock); // ZWEITE_KOPFDATEN_ZEILE
			formatDritteZeileSpielTagSpalten(ersteSpalteSpieltagBlock, MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH);
		}

		// -------------------------
		// endsumme spalten
		// -------------------------
		formatEndSummen(rangliste.getErsteSummeSpalte());
		// -------------------------
	}

	public void formatDaten() throws GenerateException {
		IEndRangliste rangliste = this.ranglisteWkRef.get();
		int anzSpielTage = rangliste.getAnzahlSpieltage();
		if (anzSpielTage < 1) {
			return;
		}
		for (int spielTag = 1; spielTag <= anzSpielTage; spielTag++) {
			int ersteSpalteSpieltagBlock = this.ersteSpielTagSpalte + ((spielTag - 1) * this.anzSpaltenInSpieltag);
			formatDatenSpielTagSpalten(ersteSpalteSpieltagBlock);
		}
		formatDatenSpielTagSpalten(rangliste.getErsteSummeSpalte());

	}

	@Override
	public StringCellValue addFooter() throws GenerateException {
		StringCellValue stringVal = super.addFooter();

		getSheetHelper().setStringValueInCell(stringVal.zeilePlusEins().setValue("Aus der Endranglistenwertung entfallen eine einmalige Nichtteilnahme bzw. das"));
		getSheetHelper().setStringValueInCell(stringVal.zeilePlusEins().setValue("schlechteste Tagesergebnis wenn an allen Spieltagen teilgenommen wurde"));

		return stringVal;
	}

}
