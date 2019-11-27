/**
* Erstellung : 26.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.RangeProperties;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeSheet;

public class SupermeleeTeamPaarungenSheet extends SuperMeleeSheet implements ISheet {
	private static final Logger logger = LogManager.getLogger(SupermeleeTeamPaarungenSheet.class);

	public static final String SHEETNAME = "Supermêlée Teams";
	public static final int ERSTE_DATEN_ZEILE = 1;
	public static final int ANZ_SPIELER_SPALTE = 0;
	public static final int ANZ_DOUBLETTE_SPALTE = 1;
	public static final int ANZ_TRIPLETTE_SPALTE = 2;
	public static final int NUR_DOUBLETTE_SPALTE = 3;
	public static final int NUR_DOUBLETTE_ANZ_DOUBL_SPALTE = 4;
	public static final int NICHT_VALIDE_ANZAHL_SPIELER_SPALTE = 5;

	// Doublette / Triplette
	public static final int DOUBL_MODE_ANZ_DOUBLETTE_SPALTE = 6;
	public static final int DOUBL_MODE_ANZ_TRIPLETTE_SPALTE = 7;
	public static final int DOUBL_MODE_NUR_TRIPLETTE_SPALTE = 8;
	public static final int DOUBL_MODE_NUR_TRIPLETTE_ANZ_TRIPL_SPALTE = 9;

	private static final String SHEET_COLOR = "f4ca46";

	public SupermeleeTeamPaarungenSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
		if (NewSheet.from(getWorkingSpreadsheet(), SHEETNAME).hideGrid().pos(DefaultSheetPos.SUPERMELEE_TEAMS).tabColor(SHEET_COLOR).useIfExist().create().isDidCreate()) {
			try {
				initSheet();
			} catch (GenerateException e) {
				logger.fatal(e);
			}
		}
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		XSpreadsheet sheet = getSheetHelper().findByName(SHEETNAME);
		return sheet;
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	private void initSheet() throws GenerateException {
		XSpreadsheet sheet = getXSpreadSheet();
		processBoxinfo("Erstelle " + SHEETNAME);

		// Header
		// --------------------------------------------
		Position pos = Position.from(ANZ_SPIELER_SPALTE, ERSTE_DATEN_ZEILE - 1);
		int spalteBreite = 1000;
		ColumnProperties columnProperties = ColumnProperties.from().setWidth(spalteBreite).setHoriJustify(CellHoriJustify.CENTER);
		StringCellValue headerVal = StringCellValue.from(sheet, pos, "#").setComment("Anzahl Spieler").setColumnProperties(columnProperties);
		getSheetHelper().setStringValueInCell(headerVal);
		getSheetHelper().setStringValueInCell(headerVal.spaltePlusEins().setValue("∑x2").setComment("Tripl/Doubl\r\nDoublette Teams"));
		getSheetHelper().setStringValueInCell(headerVal.spaltePlusEins().setValue("∑x3").setComment("Tripl/Doubl\r\nTriplette Teams"));
		getSheetHelper().setStringValueInCell(headerVal.spaltePlusEins().setValue("Doubl").setComment("x= mit dieser Anzahl von Spieler kann nur Doublette gespielt werden"));
		getSheetHelper().setStringValueInCell(headerVal.spaltePlusEins().setValue("∑x2").setComment("Wenn nur Doublette gespielt wird, anzahl Teams."));
		getSheetHelper().setStringValueInCell(headerVal.spaltePlusEins().setValue("Ung").setComment("x= Dieser Anzahl an Spieler ist ungültig.\r\nKeine Kombinationen möglich"));
		getSheetHelper().setStringValueInCell(headerVal.spaltePlusEins().setValue("∑x2").setComment("Doubl/Tripl\r\nDoublette Teams"));
		getSheetHelper().setStringValueInCell(headerVal.spaltePlusEins().setValue("∑x3").setComment("Doubl/Tripl\r\nTriplette Teams"));
		getSheetHelper().setStringValueInCell(headerVal.spaltePlusEins().setValue("Tripl").setComment("x= mit dieser Anzahl von Spieler kann nur Triplette gespielt werden"));
		getSheetHelper().setStringValueInCell(headerVal.spaltePlusEins().setValue("∑x3").setComment("Wenn nur Triplette gespielt wird, anzahl Teams."));

		TableBorder2 border = BorderFactory.from().allThin().toBorder();
		RangePosition rangePosHeader = RangePosition.from(0, 0, 9, 0); // 10 spalten
		RangeProperties rangeHeaderProp = RangeProperties.from().setBorder(border).setCharWeight(FontWeight.BOLD);
		RangeHelper.from(getXSpreadSheet(), rangePosHeader).setRangeProperties(rangeHeaderProp);

		// --------------------------------------------

		// Daten zusammenbauen
		RangeData rangeData = new RangeData();
		// for (int anSpielerCntr = 4; anSpielerCntr < 101; anSpielerCntr++) {
		for (int anSpielerCntr = 4; anSpielerCntr < 101; anSpielerCntr++) {
			RowData row = rangeData.newRow();
			{
				SuperMeleeTeamRechner teamRechnerTriplette = new SuperMeleeTeamRechner(anSpielerCntr, SuperMeleeMode.Triplette);
				row.newInt(teamRechnerTriplette.getAnzSpieler());
				row.newInt(teamRechnerTriplette.getAnzDoublette());
				row.newInt(teamRechnerTriplette.getAnzTriplette());
				row.newString(teamRechnerTriplette.isNurDoubletteMoeglich() ? "X" : "");
				row.newInt(teamRechnerTriplette.getAnzahlDoubletteWennNurDoublette());
				row.newString(teamRechnerTriplette.valideAnzahlSpieler() ? "" : "X");
			}
			{
				SuperMeleeTeamRechner teamRechnerDoublette = new SuperMeleeTeamRechner(anSpielerCntr, SuperMeleeMode.Doublette);
				row.newInt(teamRechnerDoublette.getAnzDoublette());
				row.newInt(teamRechnerDoublette.getAnzTriplette());
				row.newString(teamRechnerDoublette.isNurTripletteMoeglich() ? "X" : "");
				row.newInt(teamRechnerDoublette.getAnzahlTripletteWennNurTriplette());
			}
		}

		RangeProperties rangeProp = RangeProperties.from().setBorder(border);
		RangePosition rangePosAlldata = RangePosition.from(0, 1, 9, rangeData.size()); // 10 spalten
		RangeHelper.from(getXSpreadSheet(), rangePosAlldata).setDataInRange(rangeData).setRangeProperties(rangeProp);
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
	}

	public String formulaSverweisAnzDoublette(String adresseAnzSpieler) throws GenerateException {
		return formulaSverweis(adresseAnzSpieler, ANZ_DOUBLETTE_SPALTE);
	}

	public String formulaSverweisAnzTriplette(String adresseAnzSpieler) throws GenerateException {
		return formulaSverweis(adresseAnzSpieler, ANZ_TRIPLETTE_SPALTE);
	}

	public String formulaSverweisNurDoublette(String adresseAnzSpieler) throws GenerateException {
		return formulaSverweis(adresseAnzSpieler, NUR_DOUBLETTE_SPALTE);
	}

	// -----------------------------------------------------------------------------------------------------------
	public String formulaSverweisDoubletteModeAnzDoublette(String adresseAnzSpieler) throws GenerateException {
		return formulaSverweis(adresseAnzSpieler, DOUBL_MODE_ANZ_DOUBLETTE_SPALTE);
	}

	public String formulaSverweisAnzDoubletteModeAnzTriplette(String adresseAnzSpieler) throws GenerateException {
		return formulaSverweis(adresseAnzSpieler, DOUBL_MODE_ANZ_TRIPLETTE_SPALTE);
	}

	public String formulaSverweisDoubletteModeNurTriplette(String adresseAnzSpieler) throws GenerateException {
		return formulaSverweis(adresseAnzSpieler, DOUBL_MODE_NUR_TRIPLETTE_SPALTE);
	}

	public String formulaSverweis(String adresseAnzSpieler, int spalte) throws GenerateException {
		// sheet vorhanden ?
		getXSpreadSheet(); // dummy aufruf

		String ersteZelleAddress = Position.from(ANZ_SPIELER_SPALTE, ERSTE_DATEN_ZEILE).getAddressWith$();
		String letzteZelleAddress = Position.from(spalte, ERSTE_DATEN_ZEILE + 999).getAddressWith$();

		// suchkriterium;matrix;index;sortiert
		return "=VLOOKUP(" + adresseAnzSpieler + ";$'" + SHEETNAME + "'." + ersteZelleAddress + ":" + letzteZelleAddress + ";" + (spalte + 1) + ";0)";
	}

}
