/**
* Erstellung : 14.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.endrangliste;

import static com.google.common.base.Preconditions.*;

import com.sun.star.table.CellHoriJustify;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.rangliste.AbstractRanglisteFormatter;
import de.petanqueturniermanager.helper.sheet.SpielerSpalte;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;
import de.petanqueturniermanager.konfiguration.IPropertiesSpalte;

public class EndRanglisteFormatter extends AbstractRanglisteFormatter {

	private final WeakRefHelper<IEndRangliste> ranglisteWkRef;
	private final int anzSpaltenInSpieltag;
	private final int ersteSpielTagSpalte;

	public EndRanglisteFormatter(XComponentContext xContext, IEndRangliste rangliste, int anzSpaltenInSpieltag,
			SpielerSpalte spielerSpalte, int ersteSpielTagSpalte, IPropertiesSpalte propertiesSpalte) {
		super(xContext, spielerSpalte, propertiesSpalte, rangliste);
		checkNotNull(rangliste);
		checkNotNull(xContext);
		this.ranglisteWkRef = new WeakRefHelper<IEndRangliste>(rangliste);
		this.anzSpaltenInSpieltag = anzSpaltenInSpieltag;
		this.ersteSpielTagSpalte = ersteSpielTagSpalte;
	}

	public void updateHeader() throws GenerateException {
		IEndRangliste rangliste = this.ranglisteWkRef.getObject();
		int anzSpieltagen = rangliste.getAnzahlSpieltage();
		if (anzSpieltagen < 1) {
			return;
		}
		// -------------------------
		// spieltag spalten
		// -------------------------
		StringCellValue spieltagheader = StringCellValue.from(getSheet(), Position.from(ERSTE_KOPFDATEN_ZEILE, 0))
				.setCellBackColor(getHeaderFarbe())
				.setBorder(BorderFactory.from().allThin().boldLn().forLeft().forTop().forRight().toBorder())
				.setHoriJustify(CellHoriJustify.CENTER);
		for (int spielTag = 1; spielTag <= anzSpieltagen; spielTag++) {
			int ersteSpalteSpieltagBlock = this.ersteSpielTagSpalte + ((spielTag - 1) * this.anzSpaltenInSpieltag);
			// ERSTE_KOPFDATEN_ZEILE
			spieltagheader.spalte(ersteSpalteSpieltagBlock).setValue(spielTag + ". Spieltag")
					.setEndPosMergeSpaltePlus(this.anzSpaltenInSpieltag - 1);
			getSheetHelper().setTextInCell(spieltagheader);
			formatZweiteZeileSpielTagSpalten(ersteSpalteSpieltagBlock); // ZWEITE_KOPFDATEN_ZEILE
			formatDritteZeileSpielTagSpalten(ersteSpalteSpieltagBlock);
		}

		// -------------------------
		// endsumme spalten
		// -------------------------
		formatEndSummen(rangliste.getErsteSummeSpalte());
		// -------------------------
	}

	public void formatDaten() throws GenerateException {
		IEndRangliste rangliste = this.ranglisteWkRef.getObject();
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

		getSheetHelper().setTextInCell(stringVal.zeilePlusEins()
				.setValue("Aus der Endranglistenwertung entfallen eine einmalige Nichtteilnahme bzw. das"));
		getSheetHelper().setTextInCell(stringVal.zeilePlusEins()
				.setValue("schlechteste Tagesergebnis wenn an allen Spieltagen teilgenommen wurde"));

		return stringVal;
	}

}
