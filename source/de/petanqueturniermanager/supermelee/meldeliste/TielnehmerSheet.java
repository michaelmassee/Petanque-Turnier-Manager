/**
* Erstellung : 20.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

import static com.google.common.base.Preconditions.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.SpielerSpalte;
import de.petanqueturniermanager.konfiguration.KonfigurationSheet;
import de.petanqueturniermanager.model.Meldungen;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.supermelee.SpielTagNr;

public class TielnehmerSheet extends SheetRunner implements ISheet {

	private static final Logger logger = LogManager.getLogger(TielnehmerSheet.class);

	public static final String SHEETNAME = "Teilnehmer";

	public static final int ERSTE_DATEN_ZEILE = 0;
	public static final int SPIELER_NR_SPALTE = 0; // Spalte A=0
	public static final int SPIELER_NAME_SPALTE = 1; // Spalte A=0
	public static final int ANZAHL_SPALTEN = 3; // nr + name + leer

	private static final String SHEET_COLOR = "6542f4";

	private final AbstractSupermeleeMeldeListeSheet meldeliste;
	private final KonfigurationSheet konfigurationSheet;
	private SpielTagNr spielTagNr = null;

	public TielnehmerSheet(XComponentContext xContext) {
		super(xContext);
		this.meldeliste = new MeldeListeSheet_Update(xContext);
		this.konfigurationSheet = new KonfigurationSheet(xContext);
	}

	@Override
	public XSpreadsheet getSheet() throws GenerateException {
		return this.getSheetHelper().newIfNotExist(getSheetName(getSpielTagNr()), DefaultSheetPos.SUPERMELEE_WORK, SHEET_COLOR);
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		this.setSpielTagNr(this.konfigurationSheet.getAktiveSpieltag());
		generate();
	}

	public void generate() throws GenerateException {
		this.meldeliste.setSpielTag(this.getSpielTagNr());
		getSheetHelper().removeSheet(getSheetName(this.getSpielTagNr()));
		XSpreadsheet sheet = getSheet();
		getSheetHelper().setActiveSheet(sheet);

		// meldeliste nach namen sortieren !
		this.meldeliste.doSort(this.meldeliste.getSpielerNameErsteSpalte(), true);

		Meldungen aktiveUndAusgesetztMeldungen = this.meldeliste.getAktiveUndAusgesetztMeldungen();

		CellProperties celPropNr = CellProperties.from().setHoriJustify(CellHoriJustify.CENTER).setWidth(SpielerSpalte.DEFAULT_SPALTE_NUMBER_WIDTH);
		NumberCellValue spierNrVal = NumberCellValue.from(getSheet(), Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE)).setBorder(BorderFactory.from().allThin().toBorder())
				.setCharColor(ColorHelper.CHAR_COLOR_SPIELER_NR);

		CellProperties celPropName = CellProperties.from().setHoriJustify(CellHoriJustify.CENTER).setWidth(SpielerSpalte.DEFAULT_SPIELER_NAME_WIDTH);

		StringCellValue nameFormula = StringCellValue.from(getSheet(), Position.from(SPIELER_NAME_SPALTE, ERSTE_DATEN_ZEILE)).setBorder(BorderFactory.from().allThin().toBorder())
				.setShrinkToFit(true);

		int anzSpielerinSpalte = 40;
		int lfndNr = 1;
		spalteFormat(spierNrVal, celPropNr, nameFormula, celPropName);

		for (Spieler spieler : aktiveUndAusgesetztMeldungen.getSpielerList()) {

			spierNrVal.setValue((double) spieler.getNr());
			nameFormula.setValue(this.meldeliste.formulaSverweisSpielernamen(spierNrVal.getPos().getAddress()));

			this.getSheetHelper().setValInCell(spierNrVal);
			this.getSheetHelper().setFormulaInCell(nameFormula);

			spierNrVal.zeilePlusEins();
			nameFormula.zeilePlusEins();

			if ((lfndNr / anzSpielerinSpalte) * anzSpielerinSpalte == lfndNr) {
				spierNrVal.spalte((lfndNr / anzSpielerinSpalte) * ANZAHL_SPALTEN).zeile(ERSTE_DATEN_ZEILE);
				nameFormula.spalte(spierNrVal.getPos().getSpalte() + 1).zeile(ERSTE_DATEN_ZEILE);
				spalteFormat(spierNrVal, celPropNr, nameFormula, celPropName);
			}
			lfndNr++;
		}

	}

	private void spalteFormat(NumberCellValue nrVal, CellProperties celPropNr, StringCellValue nameVal, CellProperties celPropName) throws GenerateException {
		this.getSheetHelper().setColumnProperties(getSheet(), nrVal.getPos().getSpalte(), celPropNr);
		this.getSheetHelper().setColumnProperties(getSheet(), nameVal.getPos().getSpalte(), celPropName);
		// leere spalte breite
		this.getSheetHelper().setColumnProperties(getSheet(), nameVal.getPos().getSpalte() + 1, celPropNr);
	}

	public String getSheetName(SpielTagNr spieltagNr) throws GenerateException {
		return spieltagNr.getNr() + ". Spieltag " + SHEETNAME;
	}

	public SpielTagNr getSpielTagNr() throws GenerateException {
		checkNotNull(this.spielTagNr, "spielTagNr == null");
		return this.spielTagNr;
	}

	public void setSpielTagNr(SpielTagNr spielTag) {
		checkNotNull(spielTag, "spielTagNr == null");
		this.spielTagNr = spielTag;
	}

}