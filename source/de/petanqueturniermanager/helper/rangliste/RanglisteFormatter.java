/**
* Erstellung : 14.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.rangliste;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.petanqueturniermanager.helper.cellvalue.CellProperties.TABLE_BORDER2;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.basesheet.konfiguration.IPropertiesSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.SpielerSpalte;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;

public class RanglisteFormatter extends AbstractRanglisteFormatter {

	private final WeakRefHelper<ISpielTagRangliste> ranglisteWkRef;
	private final int anzSpaltenInSpielrunde;
	private final int ersteSpielRundeSpalte;

	public RanglisteFormatter(ISpielTagRangliste rangliste, int anzSpaltenInSpielrunde, SpielerSpalte spielerSpalte, int ersteSpielRundeSpalte,
			IPropertiesSpalte propertiesSpalte) {
		super(spielerSpalte, propertiesSpalte, rangliste);
		checkNotNull(rangliste);
		this.ranglisteWkRef = new WeakRefHelper<ISpielTagRangliste>(rangliste);
		this.anzSpaltenInSpielrunde = anzSpaltenInSpielrunde;
		this.ersteSpielRundeSpalte = ersteSpielRundeSpalte;
	}

	public void updateHeader() throws GenerateException {

		ranglisteWkRef.getObject().processBoxinfo("Formatiere Header");

		ISpielTagRangliste rangliste = this.ranglisteWkRef.getObject();
		int anzRunden = rangliste.getAnzahlRunden();
		if (anzRunden < 1) {
			return;
		}
		XSpreadsheet sheet = rangliste.getSheet();
		// -------------------------
		// spielrunden spalten
		// -------------------------
		CellProperties columnProperties = CellProperties.from().setWidth(SpielerSpalte.DEFAULT_SPALTE_NUMBER_WIDTH).setHoriJustify(CellHoriJustify.CENTER);
		StringCellValue headerPlus = StringCellValue.from(sheet, Position.from(this.ersteSpielRundeSpalte, DRITTE_KOPFDATEN_ZEILE), "+").addColumnProperties(columnProperties)
				.setCellBackColor(getHeaderFarbe());
		StringCellValue headerMinus = StringCellValue.from(headerPlus).setValue("-");

		headerPlus.setBorder(borderThinLeftBold());

		for (int spielRunde = 1; spielRunde <= anzRunden; spielRunde++) {
			int plusSpalte = this.ersteSpielRundeSpalte + ((spielRunde - 1) * 2);
			this.getSheetHelper().setTextInCell(headerPlus.spalte(plusSpalte).setComment("Spielrunde " + spielRunde + " Punkte +"));
			this.getSheetHelper().setTextInCell(headerMinus.spalte(plusSpalte + 1).setComment("Spielrunde " + spielRunde + " Punkte -"));

			// Runden Counter
			StringCellValue headerRndCounter = StringCellValue.from(headerPlus).setValue(spielRunde + ". Rnd").zeile(ZWEITE_KOPFDATEN_ZEILE).setEndPosMergeSpaltePlus(1)
					.setComment(null);
			this.getSheetHelper().setTextInCell(headerRndCounter);
		}
		// -------------------------
		// summen spalten
		// -------------------------
		formatEndSummen(rangliste.getErsteSummeSpalte());
	}

	public void formatDaten() throws GenerateException {

		ranglisteWkRef.getObject().processBoxinfo("Formatiere Daten");

		ISpielTagRangliste rangliste = this.ranglisteWkRef.getObject();
		SpielerSpalte spielerSpalte = this.getSpielerSpalteWkRef().getObject();

		int anzRunden = rangliste.getAnzahlRunden();
		if (anzRunden < 1) {
			return;
		}

		int ersteDatenZeile = spielerSpalte.getErsteDatenZiele();
		int letzteDatenZeile = spielerSpalte.getLetzteDatenZeile();
		TableBorder2 border = BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder();

		for (int spielRunde = 1; spielRunde <= anzRunden; spielRunde++) {
			Position posPunktePlusStart = Position.from(this.ersteSpielRundeSpalte + ((spielRunde - 1) * this.anzSpaltenInSpielrunde), ersteDatenZeile);
			Position posPunkteMinusEnd = Position.from(posPunktePlusStart).spaltePlusEins().zeile(letzteDatenZeile);
			RangePosition datenRange = RangePosition.from(posPunktePlusStart, posPunkteMinusEnd);
			this.getSheetHelper().setPropertyInRange(rangliste.getSheet(), datenRange, TABLE_BORDER2, border);
		}

		formatDatenSpielTagSpalten(rangliste.getErsteSummeSpalte());
	}

	@Override
	public StringCellValue addFooter() throws GenerateException {

		ranglisteWkRef.getObject().processBoxinfo("Fußzeile einfügen");

		StringCellValue stringVal = super.addFooter();

		IPropertiesSpalte propertiesSpalte = this.getPropertiesSpaltewkRef().getObject();

		int nichtgespieltPlus = propertiesSpalte.getNichtGespielteRundePlus();
		int nichtgespieltMinus = propertiesSpalte.getNichtGespielteRundeMinus();
		getSheetHelper().setTextInCell(stringVal.zeilePlusEins().setValue("Nicht gespielten Runden werden mit " + nichtgespieltPlus + ":" + nichtgespieltMinus + " gewertet"));

		return stringVal;
	}

}
