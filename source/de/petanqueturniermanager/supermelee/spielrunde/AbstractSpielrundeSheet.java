/**
* Erstellung : 29.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.spielrunde;

import static com.google.common.base.Preconditions.*;
import static de.petanqueturniermanager.helper.cellvalue.CellProperties.*;

import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.awt.FontWeight;
import com.sun.star.awt.MessageBoxResults;
import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.table.TableBorder2;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.TripletteDoublPaarungen;
import de.petanqueturniermanager.exception.AlgorithmenException;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.msgbox.ErrorMessageBox;
import de.petanqueturniermanager.helper.msgbox.QuestionBox;
import de.petanqueturniermanager.helper.msgbox.WarningBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.konfiguration.KonfigurationSheet;
import de.petanqueturniermanager.model.Meldungen;
import de.petanqueturniermanager.model.SpielRunde;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.meldeliste.AbstractMeldeListeSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;

public abstract class AbstractSpielrundeSheet extends SheetRunner {

	private static final Logger logger = LogManager.getLogger(AbstractSpielrundeSheet.class);

	public static final String PREFIX_SHEET_NAMEN = "Spielrunde";
	public static final int ERSTE_DATEN_ZEILE = 2; // Zeile 3
	public static final int ERSTE_HEADER_ZEILE = ERSTE_DATEN_ZEILE - 2;
	public static final int ZWEITE_HEADER_ZEILE = ERSTE_HEADER_ZEILE + 1;

	public static final int ERSTE_SPALTE_RUNDESPIELPLAN = 1; // spalte B
	public static final int NUMMER_SPALTE_RUNDESPIELPLAN = ERSTE_SPALTE_RUNDESPIELPLAN - 1; // spalte A
	public static final int ERSTE_SPALTE_ERGEBNISSE = ERSTE_SPALTE_RUNDESPIELPLAN + 6;
	public static final int VALIDIERUNG_SPALTE = ERSTE_SPALTE_ERGEBNISSE + 2; // rechts neben die 2 egebnis spalten

	public static final int ERSTE_SPIELERNR_SPALTE = 11; // spalte L + 5 Spalten
	public static final int LETZTE_SPALTE = ERSTE_SPIELERNR_SPALTE + 5;

	private final AbstractMeldeListeSheet meldeListe;
	private final KonfigurationSheet konfigurationSheet;
	private final ErrorMessageBox errMsg;
	private SpielTagNr spielTag = null;

	public AbstractSpielrundeSheet(XComponentContext xContext) {
		super(xContext);
		this.konfigurationSheet = newKonfigurationSheet(xContext);
		this.meldeListe = initMeldeListeSheet(xContext);
		this.errMsg = new ErrorMessageBox(getxContext());
	}

	@VisibleForTesting
	KonfigurationSheet newKonfigurationSheet(XComponentContext xContext) {
		return new KonfigurationSheet(xContext);
	}

	@VisibleForTesting
	AbstractMeldeListeSheet initMeldeListeSheet(XComponentContext xContext) {
		return new MeldeListeSheet_Update(xContext);
	}

	public AbstractMeldeListeSheet getMeldeListe() throws GenerateException {
		this.meldeListe.setSpielTag(this.getSpielTag());
		return this.meldeListe;
	}

	public XSpreadsheet getSpielRundeSheet(SpielTagNr spieltag, int spielrunde) throws GenerateException {
		return getSheetHelper().newIfNotExist(getSheetName(spieltag, spielrunde), DefaultSheetPos.SUPERMELEE_WORK);
	}

	protected KonfigurationSheet getKonfigurationSheet() {
		return this.konfigurationSheet;
	}

	public String getSheetName(SpielTagNr spieltag, int spielrunde) throws GenerateException {
		return spieltag.getNr() + "." + spielrunde + ". " + PREFIX_SHEET_NAMEN;
	}

	protected final boolean canStart(Meldungen meldungen, int aktuelleSpielrunde) throws GenerateException {
		if (aktuelleSpielrunde < 1) {
			this.getSheetHelper().setActiveSheet(getMeldeListe().getSheet());
			this.errMsg.showOk("Aktuelle Spielrunde Fehler",
					"Ungültige Spielrunde in der Meldeliste '" + aktuelleSpielrunde + "'");
			return false;
		}

		if (meldungen.size() < 6) {
			this.getSheetHelper().setActiveSheet(getMeldeListe().getSheet());
			this.errMsg.showOk("Aktuelle Spielrunde Fehler",
					"Ungültige anzahl von Meldungen '" + meldungen.size() + "' ,kleiner als 6.");
			return false;
		}
		return true;
	}

	protected void spielerNummerEinfuegen(SpielRunde spielRunde) throws GenerateException {

		HashSet<Integer> spielrNr = new HashSet<>();

		XSpreadsheet sheet = getSpielRundeSheet(getSpielTag(), spielRunde.getNr());

		Position pos = Position.from(ERSTE_SPIELERNR_SPALTE, ERSTE_DATEN_ZEILE - 1);

		NumberCellValue numberCellValue = NumberCellValue.from(sheet, pos, 999).addCellProperty(VERT_JUSTIFY,
				CellVertJustify2.CENTER);

		StringCellValue validateCellVal = StringCellValue.from(numberCellValue).spalte(VALIDIERUNG_SPALTE)
				.setCharColor(ColorHelper.CHAR_COLOR_RED).setCharWeight(FontWeight.BOLD).setCharHeight(14)
				.setHoriJustify(CellHoriJustify.CENTER);

		List<Team> teams = spielRunde.teams();
		for (int teamCntr = 0; teamCntr < teams.size(); teamCntr++) {
			if ((teamCntr & 1) == 0) {
				// Team A
				numberCellValue.zeilePlusEins();
				numberCellValue.getPos().spalte(ERSTE_SPIELERNR_SPALTE);

				// paarung counter Spalte vor spielernr
				StringCellValue formulaCellValue = StringCellValue.from(numberCellValue)
						.spalte(ERSTE_SPIELERNR_SPALTE - 1).setValue("=ROW()-" + ERSTE_DATEN_ZEILE);
				getSheetHelper().setFormulaInCell(formulaCellValue);
				// paarung counter erste spalte
				getSheetHelper().setFormulaInCell(formulaCellValue.spalte(ERSTE_SPALTE_RUNDESPIELPLAN - 1));

				// Validierung für die eingabe der Ergbnisse
				String ergA = Position.from(numberCellValue.getPos()).spalte(ERSTE_SPALTE_ERGEBNISSE).getAddress();
				String ergB = Position.from(numberCellValue.getPos()).spalte(ERSTE_SPALTE_ERGEBNISSE + 1).getAddress();

				// @formatter:off
			    String valFormula = "IF(" +
			    		"OR(" +
						"AND(" + // AND 1
			    		"ISBLANK(" + ergA + ");" +
			    		"ISBLANK(" + ergB + ")" +
						")" + // end AND 1
						";" + // trenner OR
						"AND(" + // AND 2
						ergA + "< 14;" +
						ergB + "< 14;" +
						ergA + ">-1;" +
						ergB + ">-1;" +
						ergA + "<>"+ ergB +
						")" + // end AND 2
						")" + // end OR
						";\"\";\"FEHLER\")";
				// @formatter:on
				validateCellVal.setValue(valFormula).zeile(numberCellValue.getPos().getZeile());
				getSheetHelper().setFormulaInCell(validateCellVal);
			} else {
				// Team B
				numberCellValue.getPos().spalte(ERSTE_SPIELERNR_SPALTE + 3);
			}
			for (Spieler spieler : teams.get(teamCntr).spieler()) {

				// doppelte nummer prüfen !!
				if (spielrNr.contains(spieler.getNr())) {
					logger.error("Doppelte Spieler in Runde ");
					logger.error("Spieler:" + spieler);
					logger.error("Runde:" + spielRunde);

					throw new RuntimeException("Doppelte Spieler in Runde " + spielRunde + " Spieler " + spieler);
				}

				getSheetHelper().setValInCell(numberCellValue.setValue((double) spieler.getNr()));
				verweisAufSpielerNamenEinfuegen(numberCellValue.getPos(), spielRunde.getNr());
				numberCellValue.spaltePlusEins();
				spielrNr.add(spieler.getNr());
			}
		}
	}

	private void headerPaarungen(XSpreadsheet sheet, SpielRunde spielRunde) throws GenerateException {

		// erste Header
		// -------------------------
		SpielTagNr spieltag = getSpielTag();
		Position ersteHeaderZeile = Position.from(ERSTE_SPALTE_RUNDESPIELPLAN, ERSTE_HEADER_ZEILE);
		Position ersteHeaderZeileMerge = Position.from(ersteHeaderZeile).spalte(ERSTE_SPALTE_ERGEBNISSE - 1);

		// back color
		Integer headerFarbe = this.getKonfigurationSheet().getSpielRundeHeaderFarbe();
		// CellBackColor

		StringCellValue headerVal = StringCellValue
				.from(sheet, ersteHeaderZeile, "Spieltag " + spieltag.getNr() + " Spielrunde " + spielRunde.getNr())
				.addCellProperty(CHAR_WEIGHT, FontWeight.BOLD).setEndPosMerge(ersteHeaderZeileMerge)
				.addCellProperty(HORI_JUSTIFY, CellHoriJustify.CENTER)
				.addCellProperty(TABLE_BORDER2, BorderFactory.from().allThin().toBorder()).setCharHeight(13)
				.setVertJustify(CellVertJustify2.CENTER).setCellBackColor(headerFarbe);
		getSheetHelper().setTextInCell(headerVal);
		// -------------------------

		// spalte paarungen Nr
		// -------------------------
		Position posPaarungNr = Position.from(NUMMER_SPALTE_RUNDESPIELPLAN, ERSTE_DATEN_ZEILE);
		getSheetHelper().setColumnWidth(sheet, posPaarungNr, 500); // Paarungen cntr
		getSheetHelper().setColumnCellHoriJustify(sheet, posPaarungNr, CellHoriJustify.CENTER);

		// header spielernamen
		// -------------------------
		Position posSpielerNamen = Position.from(ERSTE_SPALTE_RUNDESPIELPLAN, ZWEITE_HEADER_ZEILE);
		Position posSpielerNamenMerge = Position.from(posSpielerNamen).spaltePlus(2);
		headerVal.setValue("Team 1").setPos(posSpielerNamen).setEndPosMerge(posSpielerNamenMerge)
				// rechts Doppelte Linie
				.addCellProperty(TABLE_BORDER2, BorderFactory.from().allThin().doubleLn().forRight().toBorder());
		getSheetHelper().setTextInCell(headerVal);
		headerVal.setValue("Team 2").setPos(posSpielerNamen.spaltePlus(3))
				.setEndPosMerge(posSpielerNamenMerge.spaltePlus(3))
				.addCellProperty(TABLE_BORDER2, BorderFactory.from().allThin().toBorder());
		getSheetHelper().setTextInCell(headerVal);

		//
		// spalten spielernamen
		// -------------------------
		Position posSpielerSpalte = Position.from(ERSTE_SPALTE_RUNDESPIELPLAN, ERSTE_DATEN_ZEILE - 1);
		for (int i = 1; i <= 6; i++) {
			getSheetHelper().setColumnWidth(sheet, posSpielerSpalte, 4000); // SpielerNamen
			getSheetHelper().setColumnCellHoriJustify(sheet, posSpielerSpalte, CellHoriJustify.CENTER);
			posSpielerSpalte.spaltePlusEins();
		}
		// -------------------------

		// spielErgebnisse
		// header
		Position ergebnis = Position.from(ERSTE_SPALTE_ERGEBNISSE, ERSTE_DATEN_ZEILE - 1);
		headerVal.setValue("Ergebnis").setPos(ergebnis).setEndPosMerge(Position.from(ergebnis).spaltePlusEins());
		getSheetHelper().setTextInCell(headerVal);

		// spielErgebnisse
		// Spalten
		for (int i = 1; i <= 2; i++) {
			XPropertySet xpropSet = getSheetHelper().setColumnWidth(sheet, ergebnis, 1800);
			getSheetHelper().setProperty(xpropSet, HORI_JUSTIFY, CellHoriJustify.CENTER);
			getSheetHelper().setProperty(xpropSet, VERT_JUSTIFY, CellVertJustify2.CENTER);
			getSheetHelper().setProperty(xpropSet, CHAR_WEIGHT, FontWeight.BOLD);
			ergebnis.spaltePlusEins();
		}
		// -------------------------
	}

	private void headerSpielerNr(XSpreadsheet sheet) {
		Position pos = Position.from(ERSTE_SPIELERNR_SPALTE - 1, ERSTE_DATEN_ZEILE - 1);
		StringCellValue headerCelVal = StringCellValue.from(sheet, Position.from(pos), "#").setColumnWidth(800)
				.setSpalteHoriJustify(CellHoriJustify.CENTER);
		getSheetHelper().setTextInCell(headerCelVal);
		headerCelVal.spaltePlusEins();

		for (int a = 1; a <= 3; a++) {
			getSheetHelper().setTextInCell(headerCelVal.setValue("A" + a));
			headerCelVal.spaltePlusEins();
		}
		for (int b = 1; b <= 3; b++) {
			getSheetHelper().setTextInCell(headerCelVal.setValue("B" + b));
			headerCelVal.spaltePlusEins();
		}
	}

	protected void verweisAufSpielerNamenEinfuegen(Position spielerNrPos, int spielrunde) throws GenerateException {
		int anzSpaltenDiv = ERSTE_SPIELERNR_SPALTE - ERSTE_SPALTE_RUNDESPIELPLAN;
		String spielerNrAddress = getSheetHelper().getAddressFromColumnRow(spielerNrPos);
		String formulaVerweis = this.getMeldeListe().formulaSverweisSpielernamen(spielerNrAddress);

		StringCellValue val = StringCellValue
				.from(getSpielRundeSheet(getSpielTag(), spielrunde),
						Position.from(spielerNrPos).spaltePlus(-anzSpaltenDiv), formulaVerweis)
				.setVertJustify(CellVertJustify2.CENTER).setShrinkToFit(true).setCharHeight(12);
		getSheetHelper().setFormulaInCell(val);
	}

	protected void neueSpielrunde(Meldungen meldungen, int aktuelleSpielrunde) throws GenerateException {
		neueSpielrunde(meldungen, aktuelleSpielrunde, false);
	}

	protected void neueSpielrunde(Meldungen meldungen, int neueSpielrundeNr, boolean force) throws GenerateException {
		checkNotNull(meldungen);

		if (meldungen.spieler().size() < 4) {
			this.errMsg.showOk("Fehler beim erstellen von Spielrunde",
					"Kann für Spieltag " + getSpielTag().getNr() + " die Spielrunde " + neueSpielrundeNr
							+ " nicht Auslosen. Anzahl Spieler < 4. Aktive Spieler = " + meldungen.spieler().size());
			return;
		}

		XSpreadsheet sheet = getSheetHelper().findByName(getSheetName(getSpielTag(), neueSpielrundeNr));
		if (sheet != null && !force) {
			getSheetHelper().setActiveSheet(sheet);
			WarningBox errBox = new WarningBox(getxContext());
			short result = errBox.showYesNo("Spielrunde",
					"Spielrunde\r\n'" + getSheetName(getSpielTag(), neueSpielrundeNr)
							+ "'\r\nist bereits vorhanden.\r\nLöschen und neu erstellen ?");

			if (result != MessageBoxResults.YES) {
				return;
			}
			// loeschen
			getSheetHelper().removeSheet(getSheetName(getSpielTag(), neueSpielrundeNr));
		}

		boolean doubletteRunde = false;
		// abfrage nur doublette runde ?
		boolean isKannNurDoublette = this.meldeListe.isKannNurDoublette(getSpielTag());
		if (!force && isKannNurDoublette) {
			QuestionBox questionBox = new QuestionBox(getxContext());
			short result = questionBox.showYesNo("Spielrunde Doublette", "Neue Spielrunde "
					+ getSheetName(getSpielTag(), neueSpielrundeNr) + "\r\nnur Doublette Paarungen auslosen ?");

			if (result == MessageBoxResults.YES) {
				doubletteRunde = true;
			}
		}
		if (force && isKannNurDoublette) {
			doubletteRunde = true;
		}

		sheet = getSpielRundeSheet(getSpielTag(), neueSpielrundeNr);
		getSheetHelper().setActiveSheet(sheet);
		TripletteDoublPaarungen paarungen = new TripletteDoublPaarungen();
		try {
			SpielRunde spielRundeSheet = paarungen.neueSpielrunde(neueSpielrundeNr, meldungen, doubletteRunde);
			spielRundeSheet.validateSpielerTeam(null);
			spielerNummerEinfuegen(spielRundeSheet);
			headerPaarungen(sheet, spielRundeSheet);
			headerSpielerNr(sheet);
			datenformatieren(sheet, neueSpielrundeNr);
			spielrundeProperties(sheet, neueSpielrundeNr, doubletteRunde);
			this.getKonfigurationSheet().setAktiveSpielRunde(neueSpielrundeNr);
			wennNurDoubletteRundeDannSpaltenAusblenden(sheet, doubletteRunde);
		} catch (AlgorithmenException e) {
			getLogger().error(e.getMessage(), e);
			getSheetHelper().setActiveSheet(getMeldeListe().getSheet());
			getSheetHelper().removeSheet(getSheetName(getSpielTag(), neueSpielrundeNr));
			this.errMsg.showOk("Fehler beim Auslosen", e.getMessage());
			throw new RuntimeException(e); // komplett raus
		}
	}

	private void wennNurDoubletteRundeDannSpaltenAusblenden(XSpreadsheet sheet, boolean doubletteRunde) {
		if (doubletteRunde) {
			// 3e Spalte Team 1
			getSheetHelper().setColumnProperty(sheet, ERSTE_SPALTE_RUNDESPIELPLAN + 2, "IsVisible", false);
			// 3e Spalte Team 2
			getSheetHelper().setColumnProperty(sheet, ERSTE_SPALTE_RUNDESPIELPLAN + 5, "IsVisible", false);
		}
	}

	/**
	 * die für diesen Spielrunde properties ausgeben<br>
	 * position rechts unter den block mit spieler nummer
	 *
	 * @param sheet
	 * @throws GenerateException
	 */
	private void spielrundeProperties(XSpreadsheet sheet, int aktuelleSpielrunde, boolean doubletteSpielRunde)
			throws GenerateException {
		Position datenEnd = letzteZeile(aktuelleSpielrunde);
		StringCellValue propName = StringCellValue.from(sheet,
				Position.from(ERSTE_SPIELERNR_SPALTE - 1, datenEnd.getZeile()));
		propName.zeilePlus(2).setHoriJustify(CellHoriJustify.RIGHT);

		NumberCellValue propVal = NumberCellValue.from(propName).spaltePlus(3).setHoriJustify(CellHoriJustify.CENTER);

		SpielTagNr spieltag = this.getKonfigurationSheet().getAktiveSpieltag();

		// "Aktiv"
		int anzAktiv = this.meldeListe.getAnzahlAktiveSpieler(spieltag);
		getSheetHelper().setTextInCell(propName.setEndPosMergeSpaltePlus(2).setValue("Aktiv"));
		getSheetHelper().setValInCell(propVal.setValue((double) anzAktiv));

		int anzAusg = this.meldeListe.getAusgestiegenSpieler(spieltag);
		getSheetHelper().setTextInCell(propName.zeilePlusEins().setEndPosMergeSpaltePlus(2).setValue("Ausgestiegen"));
		getSheetHelper().setValInCell(propVal.zeilePlusEins().setValue((double) anzAusg));

		getSheetHelper().setTextInCell(propName.zeilePlusEins().setEndPosMergeSpaltePlus(2).setValue("Doublette")
				.setComment("Doublette Spielrunde"));
		getSheetHelper().setTextInCell(
				StringCellValue.from(propVal).zeilePlusEins().setValue((doubletteSpielRunde ? "J" : "")));
	}

	private void datenformatieren(XSpreadsheet sheet, int aktuelleSpielrunde) throws GenerateException {
		// gitter
		Position datenStart = Position.from(NUMMER_SPALTE_RUNDESPIELPLAN, ERSTE_DATEN_ZEILE);
		Position datenEnd = letzteZeile(aktuelleSpielrunde);

		// bis zur mitte mit normal gitter
		RangePosition datenRange = RangePosition.from(datenStart,
				Position.from(ERSTE_SPALTE_RUNDESPIELPLAN + 2, datenEnd.getZeile()));
		TableBorder2 border = BorderFactory.from().allThin().boldLn().forTop().toBorder();
		getSheetHelper().setPropertyInRange(sheet, datenRange, TABLE_BORDER2, border);

		// zweite haelfte doppelte linie links
		datenRange = RangePosition.from(Position.from(ERSTE_SPALTE_RUNDESPIELPLAN + 3, ERSTE_DATEN_ZEILE), datenEnd);
		border = BorderFactory.from().allThin().boldLn().forTop().doubleLn().forLeft().toBorder();
		getSheetHelper().setPropertyInRange(sheet, datenRange, TABLE_BORDER2, border);

		// zeile höhe
		// currcell.getRows().Height = 750
		for (int zeileCntr = ERSTE_HEADER_ZEILE; zeileCntr <= datenEnd.getZeile(); zeileCntr++) {
			getSheetHelper().setRowProperty(sheet, zeileCntr, HEIGHT, 800);
		}

		// Ergebnis Zellen
		datenRange = RangePosition.from(Position.from(ERSTE_SPALTE_ERGEBNISSE, ERSTE_DATEN_ZEILE),
				Position.from(datenEnd));
		getSheetHelper().setPropertyInRange(sheet, datenRange, CHAR_HEIGHT, 16);
		getSheetHelper().setPropertyInRange(sheet, datenRange, CHAR_WEIGHT, FontWeight.BOLD);

		// gerade / ungrade hintergrund farbe
		// CellBackColor
		Integer geradeColor = this.getKonfigurationSheet().getSpielRundeHintergrundFarbeGerade();
		Integer unGeradeColor = this.getKonfigurationSheet().getSpielRundeHintergrundFarbeUnGerade();

		for (int zeileCntr = ERSTE_DATEN_ZEILE; zeileCntr <= datenEnd.getZeile(); zeileCntr++) {
			datenRange = RangePosition.from(NUMMER_SPALTE_RUNDESPIELPLAN, zeileCntr, ERSTE_SPALTE_ERGEBNISSE + 1,
					zeileCntr);
			if ((zeileCntr & 1) == 0) {
				if (unGeradeColor != null) {
					getSheetHelper().setPropertyInRange(sheet, datenRange, CELL_BACK_COLOR, unGeradeColor);
				}
			} else {
				if (geradeColor != null) {
					getSheetHelper().setPropertyInRange(sheet, datenRange, CELL_BACK_COLOR, geradeColor);
				}
			}
		}
	}

	protected Position letzteZeile(int aktuelleSpielrunde) throws GenerateException {
		if (aktuelleSpielrunde < 1) {
			return null;
		}
		XSpreadsheet sheet = getSpielRundeSheet(getSpielTag(), aktuelleSpielrunde);
		Position pos = Position.from(ERSTE_SPIELERNR_SPALTE, ERSTE_DATEN_ZEILE);

		if (getSheetHelper().getIntFromCell(sheet, pos) == -1) {
			return null; // Keine Daten
		}

		int maxCntr = 999;
		while (maxCntr-- > 0) {
			int spielrNr = getSheetHelper().getIntFromCell(sheet, pos);
			if (spielrNr < 1) {
				pos.zeilePlus(-1);
				break;
			}
			pos.zeilePlusEins();
		}
		return pos.spalte(ERSTE_SPALTE_ERGEBNISSE + 1);
	}

	/**
	 * @throws GenerateException
	 */

	protected void clearSheet(int aktuelleSpielrunde) throws GenerateException {
		XSpreadsheet xSheet = getSpielRundeSheet(getSpielTag(), aktuelleSpielrunde);
		Position letzteZeile = letzteZeile(aktuelleSpielrunde);

		if (letzteZeile == null) {
			return; // keine Daten
		}

		RangePosition rangPos = RangePosition.from(NUMMER_SPALTE_RUNDESPIELPLAN, ERSTE_DATEN_ZEILE, LETZTE_SPALTE,
				letzteZeile.getZeile());
		getSheetHelper().clearRange(xSheet, rangPos);
	}

	public SpielTagNr getSpielTag() throws GenerateException {
		return this.spielTag;
	}

	public void setSpielTag(SpielTagNr spielTag) {
		this.spielTag = spielTag;
	}

}