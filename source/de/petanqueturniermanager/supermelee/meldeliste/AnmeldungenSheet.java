/**
* Erstellung : 20.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;

import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.RangeProperties;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.model.Meldungen;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeSheet;

public class AnmeldungenSheet extends SuperMeleeSheet implements ISheet {
	private static final Logger logger = LogManager.getLogger(AnmeldungenSheet.class);

	public static final String SHEETNAME = "Anmeldungen";
	private static final String SHEET_COLOR = "98e2d7";

	public static final int ERSTE_DATEN_ZEILE = 0;
	public static final int SPIELER_NR_SPALTE = 0; // Spalte A=0
	public static final int SPIELER_NAME_SPALTE = 1; // Spalte A=0
	public static final int MAX_ANZSPIELER_IN_SPALTE = 40;

	private final AbstractSupermeleeMeldeListeSheet meldeliste;
	private SpielTagNr spielTag = null;

	public AnmeldungenSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, "Anmeldungen");
		meldeliste = new MeldeListeSheet_Update(workingSpreadsheet);
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		setSpielTag(getKonfigurationSheet().getAktiveSpieltag());
		meldeliste.setSpielTag(getSpielTag());
		meldeliste.upDateSheet();
		generate();
	}

	public void generate() throws GenerateException {
		meldeliste.setSpielTag(getSpielTag());

		// wenn hier dann immer neu erstellen, force = true
		NewSheet.from(getWorkingSpreadsheet(), getSheetName(getSpielTag())).tabColor(SHEET_COLOR).pos(DefaultSheetPos.SUPERMELEE_WORK).spielTagPageStyle(getSpielTag())
				.forceCreate().hideGrid().setActiv().create();

		// meldeliste nach namen sortieren !
		meldeliste.doSort(meldeliste.getSpielerNameErsteSpalte(), true);

		processBoxinfo("Spieltag " + getSpielTag().getNr() + ". Meldungen einlesen");
		Meldungen alleMeldungen = meldeliste.getAlleMeldungen();
		filleBereichNew(alleMeldungen);

	}

	/**
	 * verwende array helper um der komplette inhalt auf einmal zu erstellen
	 *
	 * @param alleMeldungen
	 * @throws GenerateException
	 */
	private void filleBereichNew(Meldungen alleMeldungen) throws GenerateException {

		if (alleMeldungen.size() < 1) {
			processBoxinfo("Keine Meldungen vorhanden !");
			return; // keine Daten
		}
		// Anzahl blöcke ?
		int anzBloecke = (int) Math.ceil((double) alleMeldungen.size() / MAX_ANZSPIELER_IN_SPALTE);

		RangeData data = new RangeData();
		List<Spieler> spielrList = alleMeldungen.spieler();

		for (int lnCntr = 0; lnCntr < MAX_ANZSPIELER_IN_SPALTE; lnCntr++) {
			RowData zeileData = data.newRow();
			for (int blkCntr = 1; blkCntr <= anzBloecke; blkCntr++) {
				int idx = lnCntr + (blkCntr - 1) * MAX_ANZSPIELER_IN_SPALTE;
				if (idx < alleMeldungen.size()) {
					Spieler spieler = spielrList.get(idx);
					zeileData.newInt(spieler.getNr());
					zeileData.newString("name?");
					// nichte letzte Block ?
					if (blkCntr != anzBloecke) {
						zeileData.newEmpty(); // leere spalte
						zeileData.newEmpty(); // leere spalte
					}
				}
			}
		}
		RangePosition rangePosition = data.getRangePosition(Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE));
		RangeHelper.from(getXSpreadSheet(), rangePosition).setDataInRange(data);

		// spalten formatieren

		// .setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH)
		RangeProperties rangePropNr = RangeProperties.from().setHoriJustify(CellHoriJustify.CENTER).setCharColor(ColorHelper.CHAR_COLOR_SPIELER_NR);
		ColumnProperties columnPropNr = ColumnProperties.from().setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH);

		ColumnProperties columnPropName = ColumnProperties.from().setHoriJustify(CellHoriJustify.CENTER).setWidth(SUPER_MELEE_MELDUNG_NAME_WIDTH);
		StringCellValue nameFormula = StringCellValue.from(getXSpreadSheet(), Position.from(SPIELER_NAME_SPALTE, ERSTE_DATEN_ZEILE)).setShrinkToFit(true)
				.setColumnProperties(columnPropName);

		ColumnProperties columnPropChkBox = ColumnProperties.from().setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH);
		RangeProperties rangePropBorderOnly = RangeProperties.from().setBorder(BorderFactory.from().allThin().toBorder());

		int maxMeldungZeile = 0;
		int letzteSpalte = 0;

		for (int blkCntr = 0; blkCntr < anzBloecke; blkCntr++) {

			int letzteZeile = ERSTE_DATEN_ZEILE + MAX_ANZSPIELER_IN_SPALTE - 1;

			// letzte Block ?
			if (blkCntr + 1 == anzBloecke) {
				// letztZeile berechnen
				letzteZeile = (alleMeldungen.size() - ((anzBloecke - 1) * MAX_ANZSPIELER_IN_SPALTE)) - 1;
			}

			maxMeldungZeile = (maxMeldungZeile < letzteZeile) ? letzteZeile : maxMeldungZeile;

			RangePosition rangePositionBlock = RangePosition.from(SPIELER_NR_SPALTE + (blkCntr * 4), ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE + (blkCntr * 4), letzteZeile);
			Position startNrPos = Position.from(rangePositionBlock.getStart()); // start merken fuer Formula
			{
				RangeHelper.from(getXSpreadSheet(), rangePositionBlock).setRangeProperties(rangePropNr);
				getSheetHelper().setColumnProperties(getXSpreadSheet(), rangePositionBlock.getStartSpalte(), columnPropNr);
			}
			rangePositionBlock.spaltePlusEins();

			// Formula
			{
				String formulaSverweisSpielernamen = meldeliste.formulaSverweisSpielernamen(startNrPos.getAddress()); // A1 geht weil beim fildown automatisch hoch
				nameFormula.setPos((Position) rangePositionBlock.getStart()).setFillAutoDown(letzteZeile).setValue(formulaSverweisSpielernamen);
				getSheetHelper().setFormulaInCell(nameFormula);
			}

			rangePositionBlock.spaltePlusEins();
			// checkBox
			{
				getSheetHelper().setColumnProperties(getXSpreadSheet(), rangePositionBlock.getStartSpalte(), columnPropChkBox); // Width
			}

			// Border
			RangePosition rangePositionBlockAll = RangePosition.from(Position.from(startNrPos), Position.from(rangePositionBlock.getEnde()));
			RangeHelper.from(getXSpreadSheet(), rangePositionBlockAll).setRangeProperties(rangePropBorderOnly);

			// zwischen den blöcke, nur spalte breite
			getSheetHelper().setColumnProperties(getXSpreadSheet(), rangePositionBlock.getEnde().getSpalte() + 1, columnPropNr);

			letzteSpalte = rangePositionBlock.getEnde().getSpalte();
		}

		printBereichDefinieren(maxMeldungZeile, letzteSpalte);
	}

	private void printBereichDefinieren(int letzteZeile, int letzteSpalte) throws GenerateException {
		processBoxinfo("Print-Bereich");
		Position linksOben = Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE);
		Position rechtsUnten = Position.from(letzteSpalte, letzteZeile);
		PrintArea.from(getXSpreadSheet(), getWorkingSpreadsheet()).setPrintArea(RangePosition.from(linksOben, rechtsUnten));
	}

	public String getSheetName(SpielTagNr spieltagNr) {
		checkNotNull(spieltagNr);
		return spieltagNr.getNr() + ". Spieltag " + SHEETNAME;
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return getSheetHelper().findByName(getSheetName(getSpielTag()));
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	public SpielTagNr getSpielTag() {
		checkNotNull(spielTag);
		return spielTag;
	}

	public void setSpielTag(SpielTagNr spielTag) {
		checkNotNull(spielTag);
		ProcessBox.from().spielTag(spielTag);
		this.spielTag = spielTag;
	}

}
