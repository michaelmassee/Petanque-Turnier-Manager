/*
 * Erstellung : 14.04.2018 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.endrangliste;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.IS_TEXT_WRAPPED;

import java.util.ArrayList;
import java.util.List;

import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.AbstractSuperMeleeRanglisteFormatter;
import de.petanqueturniermanager.supermelee.konfiguration.ISuperMeleePropertiesSpalte;
import de.petanqueturniermanager.supermelee.konfiguration.SuprMleEndranglisteSortMode;

public class EndRanglisteFormatter extends AbstractSuperMeleeRanglisteFormatter {

	private final IEndRangliste rangliste;
	private final int anzSpaltenInSpieltag;
	private final int ersteSpielTagSpalte;

	public EndRanglisteFormatter(IEndRangliste rangliste, int anzSpaltenInSpieltag,
			MeldungenSpalte<SpielerMeldungen, Spieler> spielerSpalte, int ersteSpielTagSpalte,
			ISuperMeleePropertiesSpalte propertiesSpalte) {
		super(spielerSpalte, propertiesSpalte, rangliste);
		this.rangliste = checkNotNull(rangliste);
		this.anzSpaltenInSpieltag = anzSpaltenInSpieltag;
		this.ersteSpielTagSpalte = ersteSpielTagSpalte;
	}

	public void updateHeader() throws GenerateException {
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
				.setHoriJustify(CellHoriJustify.CENTER)
				.setShrinkToFit(true);
		for (int spielTag = 1; spielTag <= anzSpieltagen; spielTag++) {
			int ersteSpalteSpieltagBlock = ersteSpielTagSpalte + ((spielTag - 1) * anzSpaltenInSpieltag);
			// ERSTE_KOPFDATEN_ZEILE
			spieltagheader.spalte(ersteSpalteSpieltagBlock).setValue(spielTag + ". Spieltag")
					.setEndPosMergeSpaltePlus(anzSpaltenInSpieltag - 1);
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
		int anzSpielTage = rangliste.getAnzahlSpieltage();
		if (anzSpielTage < 1) {
			return;
		}
		int ersteDatenZeile = getSpielerSpalte().getErsteDatenZiele();
		int letzteDatenZeile = getSpielerSpalte().getLetzteMitDatenZeileInSpielerNrSpalte();
		if (letzteDatenZeile < ersteDatenZeile) {
			return;
		}

		// Statt 2 Range-Calls pro Spieltag: alle gleichartigen Sub-Blöcke je Border-Typ
		// in einem einzigen Multi-Range-Call setzen.
		List<RangePosition> spieleBloecke = new ArrayList<>();
		List<RangePosition> punkteBloecke = new ArrayList<>();
		for (int spielTag = 1; spielTag <= anzSpielTage; spielTag++) {
			int ersteSpalteSpieltagBlock = ersteSpielTagSpalte + ((spielTag - 1) * anzSpaltenInSpieltag);
			sammelSpieleUndPunkteBloecke(ersteSpalteSpieltagBlock, ersteDatenZeile, letzteDatenZeile,
					spieleBloecke, punkteBloecke);
		}
		// Endsumme-Block (gleiches Layout)
		sammelSpieleUndPunkteBloecke(rangliste.getErsteSummeSpalte(), ersteDatenZeile, letzteDatenZeile,
				spieleBloecke, punkteBloecke);

		TableBorder2 spieleBorder = BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder();
		TableBorder2 punkteBorder = BorderFactory.from().allThin().boldLn().forTop().forRight()
				.doubleLn().forLeft().toBorder();
		getSheetHelper().setPropertyInMultipleRanges(rangliste, spieleBloecke, ICommonProperties.TABLE_BORDER2,
				spieleBorder);
		getSheetHelper().setPropertyInMultipleRanges(rangliste, punkteBloecke, ICommonProperties.TABLE_BORDER2,
				punkteBorder);
	}

	private void sammelSpieleUndPunkteBloecke(int ersteSummeSpalte, int ersteDatenZeile, int letzteDatenZeile,
			List<RangePosition> spieleBloecke, List<RangePosition> punkteBloecke) {
		// Spiele-Block: +,-,Δ (3 Spalten ab ersteSummeSpalte)
		spieleBloecke.add(RangePosition.from(ersteSummeSpalte, ersteDatenZeile,
				ersteSummeSpalte + 2, letzteDatenZeile));
		// Punkte-Block: +,-,Δ (3 Spalten direkt anschließend)
		punkteBloecke.add(RangePosition.from(ersteSummeSpalte + 3, ersteDatenZeile,
				ersteSummeSpalte + 5, letzteDatenZeile));
	}

	@Override
	public StringCellValue addFooter() throws GenerateException {
		StringCellValue stringVal = super.addFooter();

		getSheetHelper().setStringValueInCell(stringVal.zeilePlusEins()
				.setEndPosMergeSpaltePlus(getLetzteSpalte())
				.addCellProperty(IS_TEXT_WRAPPED, Boolean.TRUE)
				.addRowProperty("OptimalHeight", Boolean.TRUE)
				.setValue("Aus der Endranglistenwertung entfallen eine einmalige Nichtteilnahme bzw. das"));
		getSheetHelper().setStringValueInCell(stringVal.zeilePlusEins()
				.setEndPosMergeSpaltePlus(getLetzteSpalte())
				.addCellProperty(IS_TEXT_WRAPPED, Boolean.TRUE)
				.addRowProperty("OptimalHeight", Boolean.TRUE)
				.setValue("schlechteste Tagesergebnis wenn an allen Spieltagen teilgenommen wurde"));

		return stringVal;
	}

	@Override
	protected SuprMleEndranglisteSortMode getSuprMleEndranglisteSortMode() {
		return getPropertiesSpalte().getSuprMleEndranglisteSortMode();
	}

}
