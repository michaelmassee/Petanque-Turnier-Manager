/**
* Erstellung : 14.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.rangliste;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.TABLE_BORDER2;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;
import de.petanqueturniermanager.supermelee.konfiguration.ISuperMeleePropertiesSpalte;

public class RanglisteFormatter extends AbstractRanglisteFormatter {

	private final WeakRefHelper<ISpielTagRangliste> ranglisteWkRef;
	private final int anzSpaltenInSpielrunde;
	private final int ersteSpielRundeSpalte;

	public RanglisteFormatter(ISpielTagRangliste rangliste, int anzSpaltenInSpielrunde, MeldungenSpalte spielerSpalte, int ersteSpielRundeSpalte,
			ISuperMeleePropertiesSpalte propertiesSpalte) {
		super(spielerSpalte, propertiesSpalte, rangliste);
		checkNotNull(rangliste);
		ranglisteWkRef = new WeakRefHelper<>(rangliste);
		this.anzSpaltenInSpielrunde = anzSpaltenInSpielrunde;
		this.ersteSpielRundeSpalte = ersteSpielRundeSpalte;
	}

	public void updateHeader() throws GenerateException {

		ranglisteWkRef.get().processBoxinfo("Formatiere Header");

		ISpielTagRangliste rangliste = ranglisteWkRef.get();
		int anzRunden = rangliste.getAnzahlRunden();
		if (anzRunden < 1) {
			return;
		}
		XSpreadsheet sheet = rangliste.getXSpreadSheet();
		// -------------------------
		// spielrunden spalten
		// -------------------------
		ColumnProperties columnProperties = ColumnProperties.from().setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH).setHoriJustify(CellHoriJustify.CENTER);
		StringCellValue headerPlus = StringCellValue.from(sheet, Position.from(ersteSpielRundeSpalte, DRITTE_KOPFDATEN_ZEILE), "+").addColumnProperties(columnProperties)
				.setCellBackColor(getHeaderFarbe());
		StringCellValue headerMinus = StringCellValue.from(headerPlus).setValue("-");

		headerPlus.setBorder(borderThinLeftBold());

		for (int spielRunde = 1; spielRunde <= anzRunden; spielRunde++) {
			int plusSpalte = ersteSpielRundeSpalte + ((spielRunde - 1) * 2);
			getSheetHelper().setStringValueInCell(headerPlus.spalte(plusSpalte).setComment("Spielrunde " + spielRunde + " Punkte +"));
			getSheetHelper().setStringValueInCell(headerMinus.spalte(plusSpalte + 1).setComment("Spielrunde " + spielRunde + " Punkte -"));

			// Runden Counter
			StringCellValue headerRndCounter = StringCellValue.from(headerPlus).setValue(spielRunde + ". Rnd").zeile(ZWEITE_KOPFDATEN_ZEILE).setEndPosMergeSpaltePlus(1)
					.setComment(null);
			getSheetHelper().setStringValueInCell(headerRndCounter);
		}
		// -------------------------
		// summen spalten
		// -------------------------
		formatEndSummen(rangliste.getErsteSummeSpalte());
	}

	public void formatDaten() throws GenerateException {

		ranglisteWkRef.get().processBoxinfo("Formatiere Daten");

		ISpielTagRangliste rangliste = ranglisteWkRef.get();
		MeldungenSpalte spielerSpalte = getSpielerSpalteWkRef().get();

		int anzRunden = rangliste.getAnzahlRunden();
		if (anzRunden < 1) {
			return;
		}

		int ersteDatenZeile = spielerSpalte.getErsteDatenZiele();
		int letzteDatenZeile = spielerSpalte.getLetzteDatenZeile();
		TableBorder2 border = BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder();

		for (int spielRunde = 1; spielRunde <= anzRunden; spielRunde++) {
			Position posPunktePlusStart = Position.from(ersteSpielRundeSpalte + ((spielRunde - 1) * anzSpaltenInSpielrunde), ersteDatenZeile);
			Position posPunkteMinusEnd = Position.from(posPunktePlusStart).spaltePlusEins().zeile(letzteDatenZeile);
			RangePosition datenRange = RangePosition.from(posPunktePlusStart, posPunkteMinusEnd);
			getSheetHelper().setPropertyInRange(rangliste.getXSpreadSheet(), datenRange, TABLE_BORDER2, border);
		}

		formatDatenSpielTagSpalten(rangliste.getErsteSummeSpalte());
	}

	@Override
	public StringCellValue addFooter() throws GenerateException {

		ranglisteWkRef.get().processBoxinfo("Fußzeile einfügen");

		StringCellValue stringVal = super.addFooter();

		ISuperMeleePropertiesSpalte propertiesSpalte = getPropertiesSpaltewkRef().get();

		int nichtgespieltPlus = propertiesSpalte.getNichtGespielteRundePlus();
		int nichtgespieltMinus = propertiesSpalte.getNichtGespielteRundeMinus();
		getSheetHelper().setStringValueInCell(stringVal.zeilePlusEins().setValue("Nicht gespielten Runden werden mit " + nichtgespieltPlus + ":" + nichtgespieltMinus + " gewertet"));

		return stringVal;
	}

}
