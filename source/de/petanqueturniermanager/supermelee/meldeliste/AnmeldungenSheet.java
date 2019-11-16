/**
* Erstellung : 20.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

import static com.google.common.base.Preconditions.checkNotNull;

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
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
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
		generate();
	}

	public void generate() throws GenerateException {

		meldeliste.setSpielTag(getSpielTag());

		// wenn hier dann immer neu erstellen, force = true
		NewSheet.from(getWorkingSpreadsheet(), getSheetName(getSpielTag())).tabColor(SHEET_COLOR).pos(DefaultSheetPos.SUPERMELEE_WORK).spielTagPageStyle(getSpielTag())
				.forceCreate().setActiv().create();

		// meldeliste nach namen sortieren !
		meldeliste.doSort(meldeliste.getSpielerNameErsteSpalte(), true);

		processBoxinfo("Spieltag " + getSpielTag().getNr() + ". Meldungen einlesen");
		Meldungen alleMeldungen = meldeliste.getAlleMeldungen();

		ColumnProperties celPropNr = ColumnProperties.from().setHoriJustify(CellHoriJustify.CENTER).setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH);
		NumberCellValue spierNrVal = NumberCellValue.from(getSheet(), Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE)).setBorder(BorderFactory.from().allThin().toBorder())
				.setCharColor(ColorHelper.CHAR_COLOR_SPIELER_NR);

		ColumnProperties celPropName = ColumnProperties.from().setHoriJustify(CellHoriJustify.CENTER).setWidth(MeldungenSpalte.DEFAULT_SPIELER_NAME_WIDTH);

		StringCellValue nameFormula = StringCellValue.from(getSheet(), Position.from(SPIELER_NAME_SPALTE, ERSTE_DATEN_ZEILE)).setBorder(BorderFactory.from().allThin().toBorder())
				.setShrinkToFit(true);

		StringCellValue chkBox = StringCellValue.from(getSheet(), Position.from(SPIELER_NAME_SPALTE + 1, ERSTE_DATEN_ZEILE)).setBorder(BorderFactory.from().allThin().toBorder())
				.setValue(" ");

		int maxAnzSpielerInSpalte = 0;
		int spielerCntr = 1;
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

			if ((spielerCntr / MAX_ANZSPIELER_IN_SPALTE) * MAX_ANZSPIELER_IN_SPALTE == spielerCntr) {
				spierNrVal.spalte((spielerCntr / MAX_ANZSPIELER_IN_SPALTE) * 4).zeile(ERSTE_DATEN_ZEILE);
				nameFormula.spalte(spierNrVal.getPos().getSpalte() + 1).zeile(ERSTE_DATEN_ZEILE);
				chkBox.spalte(spierNrVal.getPos().getSpalte() + 2).zeile(ERSTE_DATEN_ZEILE);
				spalteFormat(spierNrVal, celPropNr, nameFormula, celPropName, chkBox);
			}
			spielerCntr++;
			if (maxAnzSpielerInSpalte < MAX_ANZSPIELER_IN_SPALTE) {
				maxAnzSpielerInSpalte++;
			}
		}

		printBereichDefinieren(maxAnzSpielerInSpalte - 1, nameFormula.getPos().getSpalte() + 1);
	}

	private void printBereichDefinieren(int letzteZeile, int letzteSpalte) throws GenerateException {
		processBoxinfo("Print-Bereich");
		Position linksOben = Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE);
		Position rechtsUnten = Position.from(letzteSpalte, letzteZeile);
		PrintArea.from(getSheet()).setPrintArea(RangePosition.from(linksOben, rechtsUnten));
	}

	private void spalteFormat(NumberCellValue nrVal, ColumnProperties celPropNr, StringCellValue nameVal, ColumnProperties celPropName, StringCellValue chkBox)
			throws GenerateException {
		getSheetHelper().setColumnProperties(getSheet(), nrVal.getPos().getSpalte(), celPropNr);
		getSheetHelper().setColumnProperties(getSheet(), nameVal.getPos().getSpalte(), celPropName);
		getSheetHelper().setColumnProperties(getSheet(), chkBox.getPos().getSpalte(), celPropNr);
		// leere spalte breite
		getSheetHelper().setColumnProperties(getSheet(), chkBox.getPos().getSpalte() + 1, celPropNr);
	}

	public String getSheetName(SpielTagNr spieltagNr) {
		checkNotNull(spieltagNr);
		return spieltagNr.getNr() + ". Spieltag " + SHEETNAME;
	}

	@Override
	public XSpreadsheet getSheet() throws GenerateException {
		return getSheetHelper().findByName(getSheetName(getSpielTag()));
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
