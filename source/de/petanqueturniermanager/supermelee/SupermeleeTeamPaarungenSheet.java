/**
* Erstellung : 26.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;

public class SupermeleeTeamPaarungenSheet extends SheetRunner {
	private static final Logger logger = LogManager.getLogger(SupermeleeTeamPaarungenSheet.class);

	public static final String SHEETNAME = "Supermêlée Teams";
	public static final int ERSTE_DATEN_ZEILE = 1;
	public static final int ANZ_SPIELER_SPALTE = 0;
	public static final int ANZ_DOUBLETTE_SPALTE = 1;
	public static final int ANZ_TRIPLETTE_SPALTE = 2;
	public static final int NUR_DOUBLETTE_SPALTE = 3;
	public static final int NUR_DOUBLETTE_ANZ_DOUBL_SPALTE = 4;
	public static final int NICHT_VALIDE_ANZAHL_SPIELER_SPALTE = 5;

	public SupermeleeTeamPaarungenSheet(XComponentContext xContext) {
		super(xContext);
	}

	public XSpreadsheet getSheet() {
		XSpreadsheet sheet = getSheetHelper().findByName(SHEETNAME);
		if (sheet == null) {
			sheet = getSheetHelper().newIfNotExist(SHEETNAME, (short) 1);
			initSheet(sheet);
		}
		return sheet;
	}

	private void initSheet(XSpreadsheet sheet) {
		// leeren erstellen
		TeamRechner teamRechner;

		getSheetHelper().setTabColor(sheet, "f4ca46");

		// Header
		Position pos = Position.from(ANZ_SPIELER_SPALTE, ERSTE_DATEN_ZEILE - 1);
		int spalteBreite = 1000;

		StringCellValue strVal = StringCellValue.from(sheet, pos, "#").setComment("Anzahl Spieler")
				.setColumnWidth(spalteBreite).setSpalteHoriJustify(CellHoriJustify.CENTER);
		getSheetHelper().setTextInCell(strVal);
		getSheetHelper().setTextInCell(strVal.spaltePlusEins().setValue("∑x2").setComment("Doublette Teams"));
		getSheetHelper().setTextInCell(strVal.spaltePlusEins().setValue("∑x3").setComment("Triplette Teams"));
		getSheetHelper().setTextInCell(strVal.spaltePlusEins().setValue("Doubl")
				.setComment("x= mit dieser Anzahl von Spieler kann nur Doublette gespielt werden"));
		getSheetHelper().setTextInCell(
				strVal.spaltePlusEins().setValue("∑x2").setComment("Wenn nur Doublette gespielt wird, anzahl Teams."));
		getSheetHelper().setTextInCell(strVal.spaltePlusEins().setValue("Ung")
				.setComment("x= Dieser Anzahl an Spieler ist ungültig.\r\nKeine Kombinationen möglich"));

		for (int anSpielerCntr = 4; anSpielerCntr < 101; anSpielerCntr++) {
			teamRechner = new TeamRechner(anSpielerCntr);
			pos = Position.from(ANZ_SPIELER_SPALTE, ERSTE_DATEN_ZEILE + (anSpielerCntr - 4));
			getSheetHelper().setValInCell(sheet, pos, teamRechner.getAnzSpieler());
			getSheetHelper().setValInCell(sheet, pos.spaltePlusEins(), teamRechner.getAnzDoublette());
			getSheetHelper().setValInCell(sheet, pos.spaltePlusEins(), teamRechner.getAnzTriplette());
			getSheetHelper().setTextInCell(sheet, pos.spaltePlusEins(),
					(teamRechner.isNurDoubletteMoeglich() ? "X" : ""));
			getSheetHelper().setTextInCell(sheet, pos.spaltePlusEins(),
					(teamRechner.isNurDoubletteMoeglich() ? "" + teamRechner.getAnzSpieler() / 2 : ""));
			getSheetHelper().setTextInCell(sheet, pos.spaltePlusEins(), (teamRechner.valideAnzahlSpieler() ? "" : "X"));

			if (!teamRechner.valideAnzahlSpieler()) {
				RangePosition rangePos = new RangePosition(Position.from(pos).spalte(0), pos);
				getSheetHelper().setPropertyInRange(sheet, rangePos, "CharColor", ColorHelper.CHAR_COLOR_RED);
				getSheetHelper().setCommentInCell(sheet, pos, "Ungültige Anzahl Spieler = "
						+ teamRechner.getAnzSpieler() + ".\r\nKeine Kombinationen möglich.");
			}
		}
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() {
		// sheet anzeigen
		getSheetHelper().setActiveSheet(getSheet());
	}

	public String formulaSverweisAnzDoublette(String adresseAnzSpieler) {
		return formulaSverweis(adresseAnzSpieler, ANZ_DOUBLETTE_SPALTE);
	}

	public String formulaSverweisAnzTriplette(String adresseAnzSpieler) {
		return formulaSverweis(adresseAnzSpieler, ANZ_TRIPLETTE_SPALTE);
	}

	public String formulaSverweisNurDoublette(String adresseAnzSpieler) {
		return formulaSverweis(adresseAnzSpieler, NUR_DOUBLETTE_SPALTE);
	}

	public String formulaSverweis(String adresseAnzSpieler, int spalte) {
		// sheet vorhanden ?
		getSheet(); // dummy aufruf

		String ersteZelleAddress = Position.from(ANZ_SPIELER_SPALTE, ERSTE_DATEN_ZEILE).getAddressWith$();
		String letzteZelleAddress = Position.from(spalte, ERSTE_DATEN_ZEILE + 999).getAddressWith$();

		// suchkriterium;matrix;index;sortiert
		return "=VLOOKUP(" + adresseAnzSpieler + ";$'" + SHEETNAME + "'." + ersteZelleAddress + ":" + letzteZelleAddress
				+ ";" + (spalte + 1) + ";0)";
	}

}
