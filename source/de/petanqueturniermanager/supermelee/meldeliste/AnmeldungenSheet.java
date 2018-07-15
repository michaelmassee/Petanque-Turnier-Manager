/**
* Erstellung : 20.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

import static com.google.common.base.Preconditions.checkNotNull;

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
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.SpielerSpalte;
import de.petanqueturniermanager.konfiguration.KonfigurationSheet;
import de.petanqueturniermanager.model.Meldungen;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.supermelee.SpielTagNr;

public class AnmeldungenSheet extends SheetRunner implements ISheet {
	private static final Logger logger = LogManager.getLogger(AnmeldungenSheet.class);

	public static final String SHEETNAME = "Anmeldungen";
	private static final String SHEET_COLOR = "98e2d7";

	public static final int ERSTE_DATEN_ZEILE = 0;
	public static final int SPIELER_NR_SPALTE = 0; // Spalte A=0
	public static final int SPIELER_NAME_SPALTE = 1; // Spalte A=0

	private final AbstractSupermeleeMeldeListeSheet meldeliste;
	private final KonfigurationSheet konfigurationSheet;
	private SpielTagNr spielTag = null;

	public AnmeldungenSheet(XComponentContext xContext) {
		super(xContext, "Anmeldungen");
		meldeliste = new MeldeListeSheet_Update(xContext);
		konfigurationSheet = new KonfigurationSheet(xContext);
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		setSpielTag(konfigurationSheet.getAktiveSpieltag());
		generate();
	}

	public void generate() throws GenerateException {

		meldeliste.setSpielTag(getSpielTag());

		// wenn hier dann immer neu erstellen, force = true
		NewSheet.from(getxContext(), getSheetName(getSpielTag())).tabColor(SHEET_COLOR).pos(DefaultSheetPos.SUPERMELEE_WORK).spielTagPageStyle(getSpielTag()).setForceCreate(true)
				.setActiv().create();

		// meldeliste nach namen sortieren !
		meldeliste.doSort(meldeliste.getSpielerNameErsteSpalte(), true);

		processBoxinfo("Spieltag " + getSpielTag().getNr() + ". Meldungen einlesen");
		Meldungen alleMeldungen = meldeliste.getAlleMeldungen();

		CellProperties celPropNr = CellProperties.from().setHoriJustify(CellHoriJustify.CENTER).setWidth(SpielerSpalte.DEFAULT_SPALTE_NUMBER_WIDTH);
		NumberCellValue spierNrVal = NumberCellValue.from(getSheet(), Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE)).setBorder(BorderFactory.from().allThin().toBorder())
				.setCharColor(ColorHelper.CHAR_COLOR_SPIELER_NR);

		CellProperties celPropName = CellProperties.from().setHoriJustify(CellHoriJustify.CENTER).setWidth(SpielerSpalte.DEFAULT_SPIELER_NAME_WIDTH);

		StringCellValue nameFormula = StringCellValue.from(getSheet(), Position.from(SPIELER_NAME_SPALTE, ERSTE_DATEN_ZEILE)).setBorder(BorderFactory.from().allThin().toBorder())
				.setShrinkToFit(true);

		StringCellValue chkBox = StringCellValue.from(getSheet(), Position.from(SPIELER_NAME_SPALTE + 1, ERSTE_DATEN_ZEILE)).setBorder(BorderFactory.from().allThin().toBorder())
				.setValue(" ");

		int anzSpielerinSpalte = 40;
		int lfndNr = 1;
		spalteFormat(spierNrVal, celPropNr, nameFormula, celPropName, chkBox);

		processBoxinfo("Spieltag " + getSpielTag().getNr() + ". " + alleMeldungen.size() + " Spieler einfügen");
		for (Spieler spieler : alleMeldungen.getSpielerList()) {

			spierNrVal.setValue((double) spieler.getNr());
			nameFormula.setValue(meldeliste.formulaSverweisSpielernamen(spierNrVal.getPos().getAddress()));

			getSheetHelper().setValInCell(spierNrVal);
			getSheetHelper().setFormulaInCell(nameFormula);
			getSheetHelper().setTextInCell(chkBox);

			spierNrVal.zeilePlusEins();
			nameFormula.zeilePlusEins();
			chkBox.zeilePlusEins();

			if ((lfndNr / anzSpielerinSpalte) * anzSpielerinSpalte == lfndNr) {
				spierNrVal.spalte((lfndNr / anzSpielerinSpalte) * 4).zeile(ERSTE_DATEN_ZEILE);
				nameFormula.spalte(spierNrVal.getPos().getSpalte() + 1).zeile(ERSTE_DATEN_ZEILE);
				chkBox.spalte(spierNrVal.getPos().getSpalte() + 2).zeile(ERSTE_DATEN_ZEILE);
				spalteFormat(spierNrVal, celPropNr, nameFormula, celPropName, chkBox);
			}
			lfndNr++;
		}
	}

	private void spalteFormat(NumberCellValue nrVal, CellProperties celPropNr, StringCellValue nameVal, CellProperties celPropName, StringCellValue chkBox)
			throws GenerateException {
		getSheetHelper().setColumnProperties(getSheet(), nrVal.getPos().getSpalte(), celPropNr);
		getSheetHelper().setColumnProperties(getSheet(), nameVal.getPos().getSpalte(), celPropName);
		getSheetHelper().setColumnProperties(getSheet(), chkBox.getPos().getSpalte(), celPropNr);
		// leere spalte breite
		getSheetHelper().setColumnProperties(getSheet(), chkBox.getPos().getSpalte() + 1, celPropNr);
	}

	public String getSheetName(SpielTagNr spieltagNr) throws GenerateException {
		checkNotNull(spieltagNr);
		return spieltagNr.getNr() + ". Spieltag " + SHEETNAME;
	}

	@Override
	public XSpreadsheet getSheet() throws GenerateException {
		return getSheetHelper().findByName(getSheetName(getSpielTag()));
	}

	public SpielTagNr getSpielTag() throws GenerateException {
		checkNotNull(spielTag);
		return spielTag;
	}

	public void setSpielTag(SpielTagNr spielTag) throws GenerateException {
		checkNotNull(spielTag);
		ProcessBox.from().spielTag(spielTag);
		this.spielTag = spielTag;
	}

}
