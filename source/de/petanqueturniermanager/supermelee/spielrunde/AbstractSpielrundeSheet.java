/**
* Erstellung : 29.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.spielrunde;

import static com.google.common.base.Preconditions.*;
import static de.petanqueturniermanager.helper.cellvalue.CellProperties.*;

import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.awt.FontWeight;
import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.ConditionOperator;
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
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellstyle.FehlerStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.cellvalue.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.SpielerSpalte;
import de.petanqueturniermanager.konfiguration.KonfigurationSheet;
import de.petanqueturniermanager.model.Meldungen;
import de.petanqueturniermanager.model.SpielRunde;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.meldeliste.AbstractSupermeleeMeldeListeSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;

public abstract class AbstractSpielrundeSheet extends SheetRunner implements ISheet {

	private static final Logger logger = LogManager.getLogger(AbstractSpielrundeSheet.class);

	public static final String PREFIX_SHEET_NAMEN = "Spielrunde";
	public static final int ERSTE_DATEN_ZEILE = 2; // Zeile 3
	public static final int ERSTE_HEADER_ZEILE = ERSTE_DATEN_ZEILE - 2;
	public static final int ZWEITE_HEADER_ZEILE = ERSTE_HEADER_ZEILE + 1;

	public static final int ERSTE_SPALTE_RUNDESPIELPLAN = 1; // spalte B
	public static final int NUMMER_SPALTE_RUNDESPIELPLAN = ERSTE_SPALTE_RUNDESPIELPLAN - 1; // spalte A
	public static final int ERSTE_SPALTE_ERGEBNISSE = ERSTE_SPALTE_RUNDESPIELPLAN + 6;
	public static final int EINGABE_VALIDIERUNG_SPALTE = ERSTE_SPALTE_ERGEBNISSE + 2; // rechts neben die 2 egebnis spalten
	public static final int ERSTE_SPIELERNR_SPALTE = 11; // spalte L + 5 Spalten
	public static final int ERSTE_SPALTE_VERTIKALE_ERGEBNISSE = ERSTE_SPIELERNR_SPALTE + 7; // rechts neben spielrnr Block +1

	public static final int LETZTE_SPALTE = ERSTE_SPIELERNR_SPALTE + 5;

	private final AbstractSupermeleeMeldeListeSheet meldeListe;
	private final KonfigurationSheet konfigurationSheet;
	private SpielTagNr spielTag = null;
	private SpielRundeNr spielRundeNr = null;

	public AbstractSpielrundeSheet(XComponentContext xContext) {
		super(xContext);
		this.konfigurationSheet = newKonfigurationSheet(xContext);
		this.meldeListe = initMeldeListeSheet(xContext);
	}

	@VisibleForTesting
	KonfigurationSheet newKonfigurationSheet(XComponentContext xContext) {
		return new KonfigurationSheet(xContext);
	}

	@VisibleForTesting
	AbstractSupermeleeMeldeListeSheet initMeldeListeSheet(XComponentContext xContext) {
		return new MeldeListeSheet_Update(xContext);
	}

	public AbstractSupermeleeMeldeListeSheet getMeldeListe() throws GenerateException {
		this.meldeListe.setSpielTag(this.getSpielTag());
		return this.meldeListe;
	}

	@Override
	public XSpreadsheet getSheet() throws GenerateException {
		return getSpielRundeSheet(getSpielTag(), getSpielRundeNr());
	}

	public XSpreadsheet getSpielRundeSheet(SpielTagNr spieltag, SpielRundeNr spielrunde) throws GenerateException {
		return getSheetHelper().newIfNotExist(getSheetName(spieltag, spielrunde), DefaultSheetPos.SUPERMELEE_WORK);
	}

	protected KonfigurationSheet getKonfigurationSheet() {
		return this.konfigurationSheet;
	}

	public String getSheetName(SpielTagNr spieltag, SpielRundeNr spielrunde) throws GenerateException {
		return spieltag.getNr() + "." + spielrunde.getNr() + ". " + PREFIX_SHEET_NAMEN;
	}

	protected final boolean canStart(Meldungen meldungen) throws GenerateException {
		if (getSpielRundeNr().getNr() < 1) {
			this.getSheetHelper().setActiveSheet(getMeldeListe().getSheet());
			this.newErrMsgBox().showOk("Aktuelle Spielrunde Fehler", "Ungültige Spielrunde in der Meldeliste '" + getSpielRundeNr().getNr() + "'");
			return false;
		}

		if (meldungen.size() < 6) {
			this.getSheetHelper().setActiveSheet(getMeldeListe().getSheet());
			this.newErrMsgBox().showOk("Aktuelle Spielrunde Fehler", "Ungültige anzahl von Meldungen '" + meldungen.size() + "' ,kleiner als 6.");
			return false;
		}
		return true;
	}

	/**
	 * 3 spalten vertikale ergebnisse fuer die ragesrangliste
	 *
	 * @throws GenerateException
	 */

	protected void vertikaleErgbnisseFormulEinfuegen(SpielRunde spielRunde) throws GenerateException {
		checkArgument(spielRunde.getNr() == getSpielRundeNr().getNr());
		XSpreadsheet sheet = getSpielRundeSheet(getSpielTag(), getSpielRundeNr());
		Position posSpielrNr = Position.from(ERSTE_SPIELERNR_SPALTE, ERSTE_DATEN_ZEILE);
		Position posSpielrNrFormula = Position.from(ERSTE_SPALTE_VERTIKALE_ERGEBNISSE, ERSTE_DATEN_ZEILE);
		StringCellValue spielrNrFormula = StringCellValue.from(sheet, posSpielrNrFormula);
		Position ergebnisPosA = Position.from(ERSTE_SPALTE_ERGEBNISSE, ERSTE_DATEN_ZEILE);
		Position ergebnisPosB = Position.from(ERSTE_SPALTE_ERGEBNISSE + 1, ERSTE_DATEN_ZEILE);

		// -----------
		// header
		// -----------
		Position ersteHeaderZeile = Position.from(ERSTE_SPALTE_VERTIKALE_ERGEBNISSE, ZWEITE_HEADER_ZEILE);
		CellProperties columnProperties = CellProperties.from().setWidth(SpielerSpalte.DEFAULT_SPALTE_NUMBER_WIDTH).setHoriJustify(CellHoriJustify.CENTER)
				.setVertJustify(CellVertJustify2.CENTER);
		StringCellValue headerText = StringCellValue.from(sheet, ersteHeaderZeile).addColumnProperties(columnProperties);
		this.getSheetHelper().setTextInCell(headerText.setValue("Nr"));
		this.getSheetHelper().setTextInCell(headerText.setValue("+").spaltePlusEins());
		this.getSheetHelper().setTextInCell(headerText.setValue("-").spaltePlusEins());

		List<Team> teams = spielRunde.teams();

		for (int teamCntr = 0; teamCntr < teams.size(); teamCntr++) {
			if ((teamCntr & 1) == 0) {
				// Team A
				for (int spielrNr = 1; spielrNr <= 3; spielrNr++) {
					vertikaleErgbnisseEinezeileEinfuegen(posSpielrNr, spielrNrFormula, ergebnisPosA, ergebnisPosB);
				}
			} else {
				// Team B
				for (int spielrNr = 1; spielrNr <= 3; spielrNr++) {
					vertikaleErgbnisseEinezeileEinfuegen(posSpielrNr, spielrNrFormula, ergebnisPosB, ergebnisPosA);
				}
				posSpielrNr.zeilePlusEins().spalte(ERSTE_SPIELERNR_SPALTE);
				ergebnisPosA.zeilePlusEins();
				ergebnisPosB.zeilePlusEins();
			}
		}
	}

	private void vertikaleErgbnisseEinezeileEinfuegen(Position posSpielrNr, StringCellValue spielrNrFormula, Position ergebnisPosA, Position ergebnisPosB)
			throws GenerateException {
		this.getSheetHelper().setFormulaInCell(spielrNrFormula.setValue(posSpielrNr.getAddressWith$()));
		StringCellValue plusSpalteFormula = StringCellValue.from(spielrNrFormula).spaltePlusEins();
		StringCellValue minusSpalteFormula = StringCellValue.from(plusSpalteFormula).spaltePlusEins();
		// + spalte
		// =WENN(M6>0;A4;"")
		plusSpalteFormula.setValue("IF(" + spielrNrFormula.getPos().getAddress() + ">0;" + ergebnisPosA.getAddressWith$() + ";\"\")");
		// - spalte
		minusSpalteFormula.setValue("IF(" + spielrNrFormula.getPos().getAddress() + ">0;" + ergebnisPosB.getAddressWith$() + ";\"\")");
		this.getSheetHelper().setFormulaInCell(plusSpalteFormula);
		this.getSheetHelper().setFormulaInCell(minusSpalteFormula);
		posSpielrNr.spaltePlusEins();
		spielrNrFormula.zeilePlusEins();
	}

	/**
	 * enweder einfach ein laufende nummer, oder jenachdem was in der konfig steht die Spielbahnnummer
	 *
	 * @throws GenerateException
	 */
	private void datenErsteSpalte() throws GenerateException {
		XSpreadsheet sheet = getSpielRundeSheet(getSpielTag(), getSpielRundeNr());
		String spielrundeSpielbahn = this.getKonfigurationSheet().getSpielrundeSpielbahn();
		Position letzteZeile = letzteZeile();

		// header
		// -------------------------
		// spalte paarungen Nr oder Spielbahn-Nummer
		// -------------------------
		CellProperties columnProperties = CellProperties.from().setVertJustify(CellVertJustify2.CENTER).setHoriJustify(CellHoriJustify.CENTER);
		if (StringUtils.isBlank(spielrundeSpielbahn) || StringUtils.equalsIgnoreCase("X", spielrundeSpielbahn)) {
			columnProperties.setWidth(500); // Paarungen cntr
			getSheetHelper().setColumnProperties(sheet, NUMMER_SPALTE_RUNDESPIELPLAN, columnProperties);
		} else {
			// Spielbahn Spalte header
			columnProperties.setWidth(900); // Paarungen cntr
			Position posErsteHeaderZelle = Position.from(NUMMER_SPALTE_RUNDESPIELPLAN, ERSTE_HEADER_ZEILE);
			Integer headerColor = this.getKonfigurationSheet().getSpielRundeHeaderFarbe();
			StringCellValue headerValue = StringCellValue.from(sheet, posErsteHeaderZelle).setRotateAngle(27000).setVertJustify(CellVertJustify2.CENTER)
					.setBorder(BorderFactory.from().allThin().toBorder()).setCellBackColor(headerColor).setCharHeight(14).setColumnProperties(columnProperties)
					.setEndPosMergeZeilePlus(1).setValue("Bahn").setComment("Spielbahn");
			getSheetHelper().setTextInCell(headerValue);

			RangePosition nbrRange = RangePosition.from(posErsteHeaderZelle, letzteZeile.spalte(NUMMER_SPALTE_RUNDESPIELPLAN));
			getSheetHelper().setPropertiesInRange(sheet, nbrRange, CellProperties.from().setCharHeight(16));
		}

		// Daten

		Position posErsteDatenZelle = Position.from(NUMMER_SPALTE_RUNDESPIELPLAN, ERSTE_DATEN_ZEILE);
		StringCellValue formulaCellValue = StringCellValue.from(sheet, posErsteDatenZelle);
		if (StringUtils.isBlank(spielrundeSpielbahn) || StringUtils.equalsIgnoreCase("X", spielrundeSpielbahn) || StringUtils.equalsIgnoreCase("N", spielrundeSpielbahn)) {
			formulaCellValue.setValue("=ROW()-" + ERSTE_DATEN_ZEILE).setFillAutoDown(letzteZeile.getZeile());
			getSheetHelper().setFormulaInCell(formulaCellValue);
		}
	}

	protected void spielerNummerEinfuegen(SpielRunde spielRunde) throws GenerateException {
		checkArgument(spielRunde.getNr() == getSpielRundeNr().getNr());

		HashSet<Integer> spielrNr = new HashSet<>();

		XSpreadsheet sheet = getSpielRundeSheet(getSpielTag(), getSpielRundeNr());

		Position posErsteSpielrNr = Position.from(ERSTE_SPIELERNR_SPALTE, ERSTE_DATEN_ZEILE - 1);

		NumberCellValue numberCellValue = NumberCellValue.from(sheet, posErsteSpielrNr, 999).addCellProperty(VERT_JUSTIFY, CellVertJustify2.CENTER);

		StringCellValue validateCellVal = StringCellValue.from(numberCellValue).spalte(EINGABE_VALIDIERUNG_SPALTE).setCharColor(ColorHelper.CHAR_COLOR_RED)
				.setCharWeight(FontWeight.BOLD).setCharHeight(14).setHoriJustify(CellHoriJustify.CENTER);

		List<Team> teams = spielRunde.teams();
		for (int teamCntr = 0; teamCntr < teams.size(); teamCntr++) {
			if ((teamCntr & 1) == 0) {
				// Team A
				numberCellValue.zeilePlusEins();
				numberCellValue.getPos().spalte(ERSTE_SPIELERNR_SPALTE);

				// paarung counter Spalte vor spielernr
				StringCellValue formulaCellValue = StringCellValue.from(numberCellValue).spalte(ERSTE_SPIELERNR_SPALTE - 1).setValue("=ROW()-" + ERSTE_DATEN_ZEILE);
				getSheetHelper().setFormulaInCell(formulaCellValue);

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
				verweisAufSpielerNamenEinfuegen(numberCellValue.getPos());
				numberCellValue.spaltePlusEins();
				spielrNr.add(spieler.getNr());
			}

			// bei doublette, in triplette leere Zelle formula einfuegen
			if (teams.get(teamCntr).spieler().size() == 2) {
				verweisAufSpielerNamenEinfuegen(numberCellValue.getPos());
			}

		}

		// conditional formatierung doppelte spieler nr
		FehlerStyle fehlerStyle = new FehlerStyle();
		RangePosition datenRange = RangePosition.from(posErsteSpielrNr, numberCellValue.getPos());
		// UND(ZÄHLENWENN($S:$S;INDIREKT(ADRESSE(ZEILE();SPALTE())))>1;INDIREKT(ADRESSE(ZEILE();SPALTE()))<>"")
		Position posSpalteSpielrNr = Position.from(ERSTE_SPALTE_VERTIKALE_ERGEBNISSE, ERSTE_DATEN_ZEILE);
		String conditionfindDoppelt = "COUNTIF(" + posSpalteSpielrNr.getSpalteAddressWith$() + ";" + ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")>1";
		String conditionNotEmpty = ConditionalFormatHelper.FORMULA_CURRENT_CELL + "<>\"\"";
		String formulaFindDoppelteSpielrNr = "AND(" + conditionfindDoppelt + ";" + conditionNotEmpty + ")";
		ConditionalFormatHelper.from(this, datenRange).clear().formula1(formulaFindDoppelteSpielrNr).operator(ConditionOperator.FORMULA).style(fehlerStyle).apply();
	}

	private void headerPaarungen(XSpreadsheet sheet, SpielRunde spielRunde) throws GenerateException {
		checkArgument(spielRunde.getNr() == getSpielRundeNr().getNr());

		// erste Header
		// -------------------------
		SpielTagNr spieltag = getSpielTag();
		Position ersteHeaderZeile = Position.from(ERSTE_SPALTE_RUNDESPIELPLAN, ERSTE_HEADER_ZEILE);
		Position ersteHeaderZeileMerge = Position.from(ersteHeaderZeile).spalte(ERSTE_SPALTE_ERGEBNISSE - 1);

		// back color
		Integer headerFarbe = this.getKonfigurationSheet().getSpielRundeHeaderFarbe();
		// CellBackColor

		StringCellValue headerVal = StringCellValue.from(sheet, ersteHeaderZeile, "Spieltag " + spieltag.getNr() + " Spielrunde " + spielRunde.getNr())
				.addCellProperty(CHAR_WEIGHT, FontWeight.BOLD).setEndPosMerge(ersteHeaderZeileMerge).addCellProperty(HORI_JUSTIFY, CellHoriJustify.CENTER)
				.addCellProperty(TABLE_BORDER2, BorderFactory.from().allThin().toBorder()).setCharHeight(13).setVertJustify(CellVertJustify2.CENTER).setCellBackColor(headerFarbe);
		getSheetHelper().setTextInCell(headerVal);

		// header spielernamen
		// -------------------------
		Position posSpielerNamen = Position.from(ERSTE_SPALTE_RUNDESPIELPLAN, ZWEITE_HEADER_ZEILE);
		Position posSpielerNamenMerge = Position.from(posSpielerNamen).spaltePlus(2);
		headerVal.setValue("Team 1").setPos(posSpielerNamen).setEndPosMerge(posSpielerNamenMerge)
				// rechts Doppelte Linie
				.addCellProperty(TABLE_BORDER2, BorderFactory.from().allThin().doubleLn().forRight().toBorder());
		getSheetHelper().setTextInCell(headerVal);
		headerVal.setValue("Team 2").setPos(posSpielerNamen.spaltePlus(3)).setEndPosMerge(posSpielerNamenMerge.spaltePlus(3)).addCellProperty(TABLE_BORDER2,
				BorderFactory.from().allThin().toBorder());
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

	private void headerSpielerNr(XSpreadsheet sheet) throws GenerateException {
		Position pos = Position.from(ERSTE_SPIELERNR_SPALTE - 1, ERSTE_DATEN_ZEILE - 1);
		StringCellValue headerCelVal = StringCellValue.from(sheet, Position.from(pos), "#").setColumnWidth(800).setSpalteHoriJustify(CellHoriJustify.CENTER);
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

	protected void verweisAufSpielerNamenEinfuegen(Position spielerNrPos) throws GenerateException {
		int anzSpaltenDiv = ERSTE_SPIELERNR_SPALTE - ERSTE_SPALTE_RUNDESPIELPLAN;
		String spielerNrAddress = spielerNrPos.getAddress();
		String formulaVerweis = "IFNA(" + this.getMeldeListe().formulaSverweisSpielernamen(spielerNrAddress) + ";\"\")";

		StringCellValue val = StringCellValue.from(getSpielRundeSheet(getSpielTag(), getSpielRundeNr()), Position.from(spielerNrPos).spaltePlus(-anzSpaltenDiv), formulaVerweis)
				.setVertJustify(CellVertJustify2.CENTER).setShrinkToFit(true).setCharHeight(12);
		getSheetHelper().setFormulaInCell(val);
	}

	protected void neueSpielrunde(Meldungen meldungen, int aktuelleSpielrunde) throws GenerateException {
		neueSpielrunde(meldungen, aktuelleSpielrunde, false);
	}

	protected void neueSpielrunde(Meldungen meldungen, int neueSpielrundeNr, boolean force) throws GenerateException {
		checkNotNull(meldungen);
		setSpielRundeNr(SpielRundeNr.from(neueSpielrundeNr));

		if (meldungen.spieler().size() < 4) {
			throw new GenerateException("Fehler beim erstellen von Spielrunde. Kann für Spieltag " + getSpielTag().getNr() + " die Spielrunde " + neueSpielrundeNr
					+ " nicht Auslosen. Anzahl Spieler < 4. Aktive Spieler = " + meldungen.spieler().size());
		}

		XSpreadsheet sheet = getSheetHelper().findByName(getSheetName(getSpielTag(), getSpielRundeNr()));
		if (sheet != null) {
			getSheetHelper().setActiveSheet(sheet);
			MessageBox msgbox = MessageBox.from(this.getxContext(), MessageBoxTypeEnum.WARN_YES_NO).forceOk(force).caption("Spielrunde");
			msgbox.message("Spieltag " + this.getSpielTag().getNr() + "\r\nSpielrunde " + neueSpielrundeNr + "\r\nist bereits vorhanden.\r\nLöschen und neu erstellen ?");
			if (MessageBoxResult.NO == msgbox.show()) {
				return;
			}
			// loeschen
			getSheetHelper().setActiveSheet(this.meldeListe.getSheet());
			getSheetHelper().removeSheet(getSheetName(getSpielTag(), getSpielRundeNr()));
		} else {
			String msg = "Erstelle für Spieltag " + this.getSpielTag().getNr() + "\r\nSpielrunde " + neueSpielrundeNr + "\r\neine neue Spielrunde";
			MessageBoxResult msgBoxRslt = MessageBox.from(this.getxContext(), MessageBoxTypeEnum.QUESTION_OK_CANCEL).forceOk(force).caption("Neue Spielrunde").message(msg).show();
			if (MessageBoxResult.CANCEL == msgBoxRslt) {
				return;
			}
		}

		boolean doubletteRunde = false;
		// abfrage nur doublette runde ?
		boolean isKannNurDoublette = this.meldeListe.isKannNurDoublette(getSpielTag());
		if (isKannNurDoublette) {
			MessageBox msgbox = MessageBox.from(this.getxContext(), MessageBoxTypeEnum.QUESTION_YES_NO).forceOk(force).caption("Spielrunde Doublette");
			msgbox.message("Für Spieltag " + this.getSpielTag().getNr() + "\r\nSpielrunde " + neueSpielrundeNr + "\r\nnur Doublette Paarungen auslosen ?");
			if (MessageBoxResult.YES == msgbox.show()) {
				doubletteRunde = true;
			}
		}
		if (force && isKannNurDoublette) {
			doubletteRunde = true;
		}

		sheet = getSpielRundeSheet(getSpielTag(), getSpielRundeNr());
		getSheetHelper().setActiveSheet(sheet);
		TripletteDoublPaarungen paarungen = new TripletteDoublPaarungen();
		try {
			SpielRunde spielRundeSheet = paarungen.neueSpielrunde(neueSpielrundeNr, meldungen, doubletteRunde);
			spielRundeSheet.validateSpielerTeam(null);
			headerPaarungen(sheet, spielRundeSheet);
			headerSpielerNr(sheet);
			spielerNummerEinfuegen(spielRundeSheet);
			vertikaleErgbnisseFormulEinfuegen(spielRundeSheet);
			datenErsteSpalte();
			datenformatieren(sheet);
			spielrundeProperties(sheet, doubletteRunde);
			this.getKonfigurationSheet().setAktiveSpielRunde(neueSpielrundeNr);
			wennNurDoubletteRundeDannSpaltenAusblenden(sheet, doubletteRunde);
		} catch (AlgorithmenException e) {
			getLogger().error(e.getMessage(), e);
			getSheetHelper().setActiveSheet(getMeldeListe().getSheet());
			getSheetHelper().removeSheet(getSheetName(getSpielTag(), getSpielRundeNr()));
			this.newErrMsgBox().showOk("Fehler beim Auslosen", e.getMessage());
			throw new RuntimeException(e); // komplett raus
		}
	}

	private void wennNurDoubletteRundeDannSpaltenAusblenden(XSpreadsheet sheet, boolean doubletteRunde) throws GenerateException {
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
	private void spielrundeProperties(XSpreadsheet sheet, boolean doubletteSpielRunde) throws GenerateException {
		Position datenEnd = letzteZeile();
		StringCellValue propName = StringCellValue.from(sheet, Position.from(ERSTE_SPIELERNR_SPALTE - 1, datenEnd.getZeile()));
		propName.zeilePlus(2).setHoriJustify(CellHoriJustify.RIGHT);

		NumberCellValue propVal = NumberCellValue.from(propName).spaltePlus(3).setHoriJustify(CellHoriJustify.CENTER);

		// "Aktiv"
		int anzAktiv = this.meldeListe.getAnzahlAktiveSpieler(getSpielTag());
		getSheetHelper().setTextInCell(propName.setEndPosMergeSpaltePlus(2).setValue("Aktiv"));
		getSheetHelper().setValInCell(propVal.setValue((double) anzAktiv));

		int anzAusg = this.meldeListe.getAusgestiegenSpieler(getSpielTag());
		getSheetHelper().setTextInCell(propName.zeilePlusEins().setEndPosMergeSpaltePlus(2).setValue("Ausgestiegen"));
		getSheetHelper().setValInCell(propVal.zeilePlusEins().setValue((double) anzAusg));

		getSheetHelper().setTextInCell(propName.zeilePlusEins().setEndPosMergeSpaltePlus(2).setValue("Doublette").setComment("Doublette Spielrunde"));
		getSheetHelper().setTextInCell(StringCellValue.from(propVal).zeilePlusEins().setValue((doubletteSpielRunde ? "J" : "")));
	}

	private void datenformatieren(XSpreadsheet sheet) throws GenerateException {
		// gitter
		Position datenStart = Position.from(NUMMER_SPALTE_RUNDESPIELPLAN, ERSTE_DATEN_ZEILE);
		Position datenEnd = letzteZeile();

		// bis zur mitte mit normal gitter
		RangePosition datenRangeErsteHaelfte = RangePosition.from(datenStart, Position.from(ERSTE_SPALTE_RUNDESPIELPLAN + 2, datenEnd.getZeile()));
		TableBorder2 border = BorderFactory.from().allThin().boldLn().forTop().toBorder();
		getSheetHelper().setPropertyInRange(sheet, datenRangeErsteHaelfte, TABLE_BORDER2, border);

		// zweite haelfte doppelte linie links
		RangePosition datenRangeZweiteHaelfte = RangePosition.from(Position.from(ERSTE_SPALTE_RUNDESPIELPLAN + 3, ERSTE_DATEN_ZEILE), datenEnd);
		border = BorderFactory.from().allThin().boldLn().forTop().doubleLn().forLeft().toBorder();
		getSheetHelper().setPropertyInRange(sheet, datenRangeZweiteHaelfte, TABLE_BORDER2, border);

		// zeile höhe
		// currcell.getRows().Height = 750
		for (int zeileCntr = ERSTE_HEADER_ZEILE; zeileCntr <= datenEnd.getZeile(); zeileCntr++) {
			getSheetHelper().setRowProperty(sheet, zeileCntr, HEIGHT, 800);
		}

		// Ergebnis Zellen
		RangePosition ergbenissRange = RangePosition.from(Position.from(ERSTE_SPALTE_ERGEBNISSE, ERSTE_DATEN_ZEILE), Position.from(datenEnd));
		getSheetHelper().setPropertyInRange(sheet, ergbenissRange, CHAR_HEIGHT, 16);
		getSheetHelper().setPropertyInRange(sheet, ergbenissRange, CHAR_WEIGHT, FontWeight.BOLD);

		// gerade / ungrade hintergrund farbe
		// CellBackColor
		RangePosition datenRangeOhneErsteSpalteOhneErgebniss = RangePosition.from(Position.from(ERSTE_SPALTE_RUNDESPIELPLAN, ERSTE_DATEN_ZEILE),
				datenEnd.spalte(ERSTE_SPALTE_ERGEBNISSE - 1));

		Integer geradeColor = this.getKonfigurationSheet().getSpielRundeHintergrundFarbeGerade();
		Integer unGeradeColor = this.getKonfigurationSheet().getSpielRundeHintergrundFarbeUnGerade();
		SpielrundeHintergrundFarbeGeradeStyle spielrundeHintergrundFarbeGeradeStyle = new SpielrundeHintergrundFarbeGeradeStyle(geradeColor);
		SpielrundeHintergrundFarbeUnGeradeStyle spielrundeHintergrundFarbeUnGeradeStyle = new SpielrundeHintergrundFarbeUnGeradeStyle(unGeradeColor);

		ConditionalFormatHelper.from(this, datenRangeOhneErsteSpalteOhneErgebniss).clear().formulaIsEvenRow().operator(ConditionOperator.FORMULA)
				.style(spielrundeHintergrundFarbeGeradeStyle).apply();
		ConditionalFormatHelper.from(this, datenRangeOhneErsteSpalteOhneErgebniss).formulaIsOddRow().operator(ConditionOperator.FORMULA)
				.style(spielrundeHintergrundFarbeUnGeradeStyle).apply();

		// erste Spalte, mit prüfung auf doppelte nummer
		Position posNrSpalte = Position.from(NUMMER_SPALTE_RUNDESPIELPLAN, ERSTE_DATEN_ZEILE);
		RangePosition datenRangeErsteSpalte = RangePosition.from(posNrSpalte, datenEnd.spalte(NUMMER_SPALTE_RUNDESPIELPLAN));

		FehlerStyle fehlerStyle = new FehlerStyle();
		String conditionfindDoppelt = "COUNTIF(" + posNrSpalte.getSpalteAddressWith$() + ";" + ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")>1";
		String conditionNotEmpty = ConditionalFormatHelper.FORMULA_CURRENT_CELL + "<>\"\"";
		String formulaFindDoppelteSpielrNr = "AND(" + conditionfindDoppelt + ";" + conditionNotEmpty + ")";
		ConditionalFormatHelper.from(this, datenRangeErsteSpalte).clear().formula1(formulaFindDoppelteSpielrNr).operator(ConditionOperator.FORMULA).style(fehlerStyle).apply();

		ConditionalFormatHelper.from(this, datenRangeErsteSpalte).formulaIsEvenRow().operator(ConditionOperator.FORMULA).style(spielrundeHintergrundFarbeGeradeStyle).apply();
		ConditionalFormatHelper.from(this, datenRangeErsteSpalte).formulaIsOddRow().operator(ConditionOperator.FORMULA).style(spielrundeHintergrundFarbeUnGeradeStyle).apply();

		// ergebniss spalten mit prüfung auf >=0 <=13
		ConditionalFormatHelper.from(this, ergbenissRange).clear().formula1("0").formula2("13").operator(ConditionOperator.NOT_BETWEEN).styleIsFehler().apply();
		// test if Text mit FORMULA
		String formula = "ISTEXT(" + ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")";
		ConditionalFormatHelper.from(this, ergbenissRange).formula1(formula).operator(ConditionOperator.FORMULA).styleIsFehler().apply();
		ConditionalFormatHelper.from(this, ergbenissRange).formulaIsEvenRow().operator(ConditionOperator.FORMULA).style(spielrundeHintergrundFarbeGeradeStyle).apply();
		ConditionalFormatHelper.from(this, ergbenissRange).formulaIsOddRow().operator(ConditionOperator.FORMULA).style(spielrundeHintergrundFarbeUnGeradeStyle).apply();
	}

	protected Position letzteZeile() throws GenerateException {
		XSpreadsheet sheet = getSpielRundeSheet(getSpielTag(), getSpielRundeNr());
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

	protected void clearSheet() throws GenerateException {
		XSpreadsheet xSheet = getSpielRundeSheet(getSpielTag(), getSpielRundeNr());
		Position letzteZeile = letzteZeile();

		if (letzteZeile == null) {
			return; // keine Daten
		}

		RangePosition rangPos = RangePosition.from(NUMMER_SPALTE_RUNDESPIELPLAN, ERSTE_DATEN_ZEILE, LETZTE_SPALTE, letzteZeile.getZeile());
		getSheetHelper().clearRange(xSheet, rangPos);
	}

	public SpielTagNr getSpielTag() throws GenerateException {
		checkNotNull(this.spielTag);
		return this.spielTag;
	}

	public void setSpielTag(SpielTagNr spielTag) {
		checkNotNull(spielTag);
		this.spielTag = spielTag;
	}

	public SpielRundeNr getSpielRundeNr() throws GenerateException {
		checkNotNull(this.spielRundeNr);
		return this.spielRundeNr;
	}

	public void setSpielRundeNr(SpielRundeNr spielrunde) {
		checkNotNull(spielrunde);
		this.spielRundeNr = spielrunde;
	}
}