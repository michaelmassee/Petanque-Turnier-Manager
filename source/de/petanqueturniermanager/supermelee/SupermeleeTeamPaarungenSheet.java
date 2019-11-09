/**
* Erstellung : 26.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellvalue.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
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

	public SupermeleeTeamPaarungenSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	public XSpreadsheet getSheet() throws GenerateException {
		XSpreadsheet sheet = getSheetHelper().findByName(SHEETNAME);
		if (sheet == null) {
			sheet = getSheetHelper().newIfNotExist(SHEETNAME, DefaultSheetPos.SUPERMELEE_TEAMS);
			PageStyleHelper.from(this, PageStyle.PETTURNMNGR).initDefaultFooter().create().applytoSheet();
			TurnierSheet.from(sheet).tabColor("f4ca46").protect();
			initSheet(sheet);
		}
		return sheet;
	}

	private void initSheet(XSpreadsheet sheet) throws GenerateException {
		// leeren erstellen
		processBoxinfo("Erstelle " + SHEETNAME);

		// Header
		Position pos = Position.from(ANZ_SPIELER_SPALTE, ERSTE_DATEN_ZEILE - 1);
		int spalteBreite = 1000;

		CellProperties columnProperties = CellProperties.from().setWidth(spalteBreite).setHoriJustify(CellHoriJustify.CENTER);

		StringCellValue headerVal = StringCellValue.from(sheet, pos, "#").setComment("Anzahl Spieler").setColumnProperties(columnProperties);
		getSheetHelper().setTextInCell(headerVal);
		getSheetHelper().setTextInCell(headerVal.spaltePlusEins().setValue("∑x2").setComment("Tripl/Doubl\r\nDoublette Teams"));
		getSheetHelper().setTextInCell(headerVal.spaltePlusEins().setValue("∑x3").setComment("Tripl/Doubl\r\nTriplette Teams"));
		getSheetHelper().setTextInCell(headerVal.spaltePlusEins().setValue("Doubl").setComment("x= mit dieser Anzahl von Spieler kann nur Doublette gespielt werden"));
		getSheetHelper().setTextInCell(headerVal.spaltePlusEins().setValue("∑x2").setComment("Wenn nur Doublette gespielt wird, anzahl Teams."));
		getSheetHelper().setTextInCell(headerVal.spaltePlusEins().setValue("Ung").setComment("x= Dieser Anzahl an Spieler ist ungültig.\r\nKeine Kombinationen möglich"));
		getSheetHelper().setTextInCell(headerVal.spaltePlusEins().setValue("∑x2").setComment("Doubl/Tripl\r\nDoublette Teams"));
		getSheetHelper().setTextInCell(headerVal.spaltePlusEins().setValue("∑x3").setComment("Doubl/Tripl\r\nTriplette Teams"));
		getSheetHelper().setTextInCell(headerVal.spaltePlusEins().setValue("Doubl").setComment("x= mit dieser Anzahl von Spieler kann nur Triplette gespielt werden"));
		getSheetHelper().setTextInCell(headerVal.spaltePlusEins().setValue("∑x2").setComment("Wenn nur Triplette gespielt wird, anzahl Teams."));

		StringCellValue strDaten = StringCellValue.from(sheet, pos);

		for (int anSpielerCntr = 4; anSpielerCntr < 101; anSpielerCntr++) {
			SuperMeleeTeamRechner teamRechnerTripletteDoublette = new SuperMeleeTeamRechner(anSpielerCntr, SuperMeleeMode.Triplette);
			int zeile = ERSTE_DATEN_ZEILE + (anSpielerCntr - 4);
			pos = Position.from(ANZ_SPIELER_SPALTE, zeile);
			getSheetHelper().setValInCell(sheet, pos, teamRechnerTripletteDoublette.getAnzSpieler());
			getSheetHelper().setValInCell(sheet, pos.spaltePlusEins(), teamRechnerTripletteDoublette.getAnzDoublette());
			getSheetHelper().setValInCell(sheet, pos.spaltePlusEins(), teamRechnerTripletteDoublette.getAnzTriplette());
			strDaten.zeile(pos.getZeile()).spalte(pos.getSpalte());
			getSheetHelper().setTextInCell(strDaten.spaltePlusEins().setValue(teamRechnerTripletteDoublette.isNurDoubletteMoeglich() ? "X" : ""));
			getSheetHelper().setTextInCell(strDaten.spaltePlusEins().setValue(teamRechnerTripletteDoublette.getAnzahlDoubletteWennNurDoublette()));
			getSheetHelper().setTextInCell(strDaten.spaltePlusEins().setValue(teamRechnerTripletteDoublette.valideAnzahlSpieler() ? "" : "X"));

			if (!teamRechnerTripletteDoublette.valideAnzahlSpieler()) {
				RangePosition rangePos = RangePosition.from(Position.from(pos).spalte(0), pos);
				getSheetHelper().setPropertyInRange(sheet, rangePos, "CharColor", ColorHelper.CHAR_COLOR_RED);
				getSheetHelper().setCommentInCell(sheet, pos, "Ungültige Anzahl Spieler = " + teamRechnerTripletteDoublette.getAnzSpieler() + ".\r\nKeine Kombinationen möglich.");
			}

			{ // Doublette / Triplette teams
				SuperMeleeTeamRechner teamRechnerDoubletteTriplette = new SuperMeleeTeamRechner(anSpielerCntr, SuperMeleeMode.Doublette);
				NumberCellValue nmbrVal = NumberCellValue.from(sheet, Position.from(DOUBL_MODE_ANZ_DOUBLETTE_SPALTE, zeile));
				nmbrVal.setValue(teamRechnerDoubletteTriplette.getAnzDoublette());
				getSheetHelper().setValInCell(nmbrVal);
				getSheetHelper().setValInCell(nmbrVal.spalte(DOUBL_MODE_ANZ_TRIPLETTE_SPALTE).setValue(teamRechnerDoubletteTriplette.getAnzTriplette()));
				getSheetHelper().setTextInCell(
						StringCellValue.from(nmbrVal).spalte(DOUBL_MODE_NUR_TRIPLETTE_SPALTE).setValue(teamRechnerDoubletteTriplette.isNurTripletteMoeglich() ? "X" : ""));
				getSheetHelper()
						.setValInCell(nmbrVal.spalte(DOUBL_MODE_NUR_TRIPLETTE_ANZ_TRIPL_SPALTE).setValue(teamRechnerDoubletteTriplette.getAnzahlTripletteWennNurTriplette()));
			}
		}
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		// sheet anzeigen
		getSheetHelper().setActiveSheet(getSheet());
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
		getSheet(); // dummy aufruf

		String ersteZelleAddress = Position.from(ANZ_SPIELER_SPALTE, ERSTE_DATEN_ZEILE).getAddressWith$();
		String letzteZelleAddress = Position.from(spalte, ERSTE_DATEN_ZEILE + 999).getAddressWith$();

		// suchkriterium;matrix;index;sortiert
		return "=VLOOKUP(" + adresseAnzSpieler + ";$'" + SHEETNAME + "'." + ersteZelleAddress + ":" + letzteZelleAddress + ";" + (spalte + 1) + ";0)";
	}

}
