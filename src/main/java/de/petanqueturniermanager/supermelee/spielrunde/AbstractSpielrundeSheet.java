/**
 * Erstellung : 29.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.spielrunde;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.CHAR_HEIGHT;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.CHAR_WEIGHT;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.HEIGHT;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.HORI_JUSTIFY;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.TABLE_BORDER2;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.VERT_JUSTIFY;

import java.util.ArrayList;
import java.util.Collections;
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

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.SuperMeleePaarungen;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.AlgorithmenException;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellstyle.FehlerStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.MeleeSpielRunde;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.SuperMeleeTeamRechner;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeSheet;
import de.petanqueturniermanager.supermelee.meldeliste.AbstractSupermeleeMeldeListeSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;

public abstract class AbstractSpielrundeSheet extends SuperMeleeSheet implements ISheet {

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
	public static final int PAARUNG_CNTR_SPALTE = ERSTE_SPIELERNR_SPALTE - 1; // Paarungen nr
	public static final int ERSTE_SPALTE_VERTIKALE_ERGEBNISSE = ERSTE_SPIELERNR_SPALTE + 7; // rechts neben spielrnr Block +1
	public static final int SPALTE_VERTIKALE_ERGEBNISSE_PLUS = ERSTE_SPALTE_VERTIKALE_ERGEBNISSE + 1;
	public static final int SPALTE_VERTIKALE_ERGEBNISSE_MINUS = ERSTE_SPALTE_VERTIKALE_ERGEBNISSE + 2;
	public static final int SPALTE_VERTIKALE_ERGEBNISSE_AB = ERSTE_SPALTE_VERTIKALE_ERGEBNISSE + 3;
	public static final int SPALTE_VERTIKALE_ERGEBNISSE_BA_NR = ERSTE_SPALTE_VERTIKALE_ERGEBNISSE + 4; // Bahn Nr

	public static final int LETZTE_SPALTE = ERSTE_SPIELERNR_SPALTE + 5;

	private final AbstractSupermeleeMeldeListeSheet meldeListe;

	private SpielTagNr spielTag = null;
	private SpielRundeNr spielRundeNr = null;
	private boolean forceOk = false; // wird fuer Test verwendet

	public AbstractSpielrundeSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, "Spielrunde");
		meldeListe = initMeldeListeSheet(workingSpreadsheet);
	}

	@VisibleForTesting
	AbstractSupermeleeMeldeListeSheet initMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new MeldeListeSheet_Update(workingSpreadsheet);
	}

	public AbstractSupermeleeMeldeListeSheet getMeldeListe() {
		meldeListe.setSpielTag(getSpielTag());
		return meldeListe;
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return getSheetHelper().findByName(getSheetName(getSpielTag(), getSpielRundeNr()));
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	public String getSheetName(SpielTagNr spieltag, SpielRundeNr spielrunde) {
		return spieltag.getNr() + "." + spielrunde.getNr() + ". " + PREFIX_SHEET_NAMEN;
	}

	protected final boolean canStart(SpielerMeldungen meldungen) throws GenerateException {
		if (getSpielRundeNr().getNr() < 1) {
			getSheetHelper().setActiveSheet(getMeldeListe().getXSpreadSheet());

			String errorMsg = "Ungültige Spielrunde in der Meldeliste '" + getSpielRundeNr().getNr() + "'";
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK).caption("Aktuelle Spielrunde Fehler")
					.message(errorMsg).show();
			return false;
		}

		if (meldungen.size() < 6) {
			getSheetHelper().setActiveSheet(getMeldeListe().getXSpreadSheet());
			String errorMsg = "Ungültige Anzahl '" + meldungen.size() + "' von Aktive Meldungen vorhanden."
					+ "\r\nFür Spieltag " + getSpielTag().getNr() + " mindestens 6 Meldungen aktivieren.";
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK).caption("Aktuelle Spielrunde Fehler")
					.message(errorMsg).show();
			return false;
		}
		return true;
	}

	/**
	 * 3 spalten vertikale ergebnisse fuer die ragesrangliste
	 *
	 * @throws GenerateException
	 */

	protected void vertikaleErgbnisseFormulaEinfuegen(MeleeSpielRunde spielRunde) throws GenerateException {

		processBoxinfo("Vertikal Ergbnisspalten");

		checkArgument(spielRunde.getNr() == getSpielRundeNr().getNr());
		XSpreadsheet sheet = getXSpreadSheet();
		Position posSpielrNr = Position.from(ERSTE_SPIELERNR_SPALTE, ERSTE_DATEN_ZEILE);
		Position posSpielrNrFormula = Position.from(ERSTE_SPALTE_VERTIKALE_ERGEBNISSE, ERSTE_DATEN_ZEILE);
		StringCellValue spielrNrFormula = StringCellValue.from(sheet, posSpielrNrFormula);
		Position ergebnisPosA = Position.from(ERSTE_SPALTE_ERGEBNISSE, ERSTE_DATEN_ZEILE);
		Position ergebnisPosB = Position.from(ERSTE_SPALTE_ERGEBNISSE + 1, ERSTE_DATEN_ZEILE);

		List<Team> teams = spielRunde.teams();

		// TODO Data Array verwenden
		for (int teamCntr = 0; teamCntr < teams.size(); teamCntr++) {
			if ((teamCntr & 1) == 0) {
				// Team A
				for (int spielrNr = 1; spielrNr <= 3; spielrNr++) {
					vertikaleErgbnisseEinezeileEinfuegen(posSpielrNr, spielrNrFormula);
				}
			} else {
				// Team B
				for (int spielrNr = 1; spielrNr <= 3; spielrNr++) {
					vertikaleErgbnisseEinezeileEinfuegen(posSpielrNr, spielrNrFormula);
				}
				posSpielrNr.zeilePlusEins().spalte(ERSTE_SPIELERNR_SPALTE);
				ergebnisPosA.zeilePlusEins();
				ergebnisPosB.zeilePlusEins();
			}
		}

		// filldown
		int letzteZeile = (ERSTE_DATEN_ZEILE + (teams.size() * 3)) - 1;
		// + punkte
		// (ZEILE()+3)/6) und (ZEILE())/3) = weil erste daten zeile = 3
		// =WENN(INDIREKT(ADRESSE(ZEILE();19;8;1))>0;INDIREKT(ADRESSE(((ZEILE()+3)/6)+2;WENN(ISTUNGERADE(ABRUNDEN((ZEILE())/3)+2);8;9);8;1));"")
		String plusminusFormula = "IF(INDIRECT(ADDRESS(ROW();%d;4;1))>0;INDIRECT(ADDRESS(((ROW( )+3)/6)+2;IF(ISODD(ROUNDDOWN((ROW())/3)+2);%d;%d);4;1));\"\")";
		String plusPunkteFormula = String.format(plusminusFormula,
				// erste spalte = 1 nicht 0
				ERSTE_SPALTE_VERTIKALE_ERGEBNISSE + 1, // spielernr
				ERSTE_SPALTE_ERGEBNISSE + 1, // Team A
				ERSTE_SPALTE_ERGEBNISSE + 2); // Team B
		//@formatter:off
		StringCellValue plusSpalteFormula = StringCellValue.from(getXSpreadSheet())
				.zeile(ERSTE_DATEN_ZEILE)
				.spalte(SPALTE_VERTIKALE_ERGEBNISSE_PLUS)
				.setValue(plusPunkteFormula)
				.setFillAutoDown(letzteZeile);
		//@formatter:on
		getSheetHelper().setFormulaInCell(plusSpalteFormula);

		// - punkte
		String minusPunkteFormula = String.format(plusminusFormula,
				// erste spalte = 1 nicht 0
				ERSTE_SPALTE_VERTIKALE_ERGEBNISSE + 1, // spielernr
				ERSTE_SPALTE_ERGEBNISSE + 2, // Team B
				ERSTE_SPALTE_ERGEBNISSE + 1); // Team A
		//@formatter:off
		StringCellValue minusSpalteFormula = StringCellValue.from(getXSpreadSheet())
				.zeile(ERSTE_DATEN_ZEILE)
				.spalte(SPALTE_VERTIKALE_ERGEBNISSE_MINUS)
				.setValue(minusPunkteFormula)
				.setFillAutoDown(letzteZeile);
		//@formatter:on
		getSheetHelper().setFormulaInCell(minusSpalteFormula);

		// Team
		// =IF(ISODD(ROUNDDOWN((ROW())/3)+2);"A";"B")
		//@formatter:off
		StringCellValue teamSpalteFormula = StringCellValue.from(getXSpreadSheet())
				.zeile(ERSTE_DATEN_ZEILE)
				.spalte(SPALTE_VERTIKALE_ERGEBNISSE_AB)
				.setValue("IF(ISODD(ROUNDDOWN((ROW())/3)+2);\"A\";\"B\")")
				.setFillAutoDown(letzteZeile);
		//@formatter:on
		getSheetHelper().setFormulaInCell(teamSpalteFormula);

		// Bahn
		// =INDIREKT( ADRESSE( ABRUNDEN((ZEILE( )+3) /6)+2;1;8;1))
		//@formatter:off
		StringCellValue bahnSpalteFormula = StringCellValue.from(getXSpreadSheet())
				.zeile(ERSTE_DATEN_ZEILE)
				.spalte(SPALTE_VERTIKALE_ERGEBNISSE_BA_NR)
				.setValue("INDIRECT( ADDRESS(ROUNDDOWN((ROW( )+3) /6)+2;" + (NUMMER_SPALTE_RUNDESPIELPLAN +1) + ";4;1))")
				.setFillAutoDown(letzteZeile);
		//@formatter:on
		getSheetHelper().setFormulaInCell(bahnSpalteFormula);

		// -----------
		// !Achtung wegen filldown bug, erst jetzt ggf. Spalten ausblenden
		// -----------
		// header
		// -----------
		Position ersteHeaderZeile = Position.from(ERSTE_SPALTE_VERTIKALE_ERGEBNISSE, ZWEITE_HEADER_ZEILE);
		ColumnProperties columnProperties = ColumnProperties.from()
				.setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH).setHoriJustify(CellHoriJustify.CENTER)
				.setVertJustify(CellVertJustify2.CENTER).isVisible(getKonfigurationSheet().zeigeArbeitsSpalten());

		StringCellValue headerText = StringCellValue.from(sheet, ersteHeaderZeile)
				.addColumnProperties(columnProperties);
		getSheetHelper().setStringValueInCell(headerText.setValue("Nr"));
		getSheetHelper().setStringValueInCell(
				headerText.setValue("+").spalte(SPALTE_VERTIKALE_ERGEBNISSE_PLUS).setComment("Plus Punkte"));
		getSheetHelper().setStringValueInCell(
				headerText.setValue("-").spalte(SPALTE_VERTIKALE_ERGEBNISSE_MINUS).setComment("Minus Punkte"));
		getSheetHelper().setStringValueInCell(
				headerText.setValue("Tm").spalte(SPALTE_VERTIKALE_ERGEBNISSE_AB).setComment("Mannschaft")); // Team A/B
		getSheetHelper().setStringValueInCell(
				headerText.setValue("Ba").spalte(SPALTE_VERTIKALE_ERGEBNISSE_BA_NR).setComment("Spielbahn Nr.")); // Bahn Nr

	}

	private void vertikaleErgbnisseEinezeileEinfuegen(Position posSpielrNr, StringCellValue spielrNrFormula)
			throws GenerateException {
		getSheetHelper().setFormulaInCell(spielrNrFormula.setValue(posSpielrNr.getAddressWith$()));
		// naechste spielernr
		posSpielrNr.spaltePlusEins();
		spielrNrFormula.zeilePlusEins();
	}

	/**
	 * enweder einfach ein laufende nummer, oder jenachdem was in der konfig steht die Spielbahnnummer<br>
	 * property getSpielrundeSpielbahn<br>
	 * X = nur ein laufende paarungen nummer<br>
	 * L = Spielbahn -> leere Spalte<br>
	 * N = Spielbahn -> durchnumeriert<br>
	 * R = Spielbahn -> random<br>
	 *
	 * @throws GenerateException
	 */
	private void datenErsteSpalte() throws GenerateException {

		processBoxinfo("Erste Spalte Daten einfügen");

		XSpreadsheet sheet = getXSpreadSheet();
		String spielrundeSpielbahn = getKonfigurationSheet().getSpielrundeSpielbahn();
		Position letzteZeile = letzteSpielrNrPosition();

		// header
		// -------------------------
		// spalte paarungen Nr oder Spielbahn-Nummer
		// -------------------------
		ColumnProperties columnProperties = ColumnProperties.from().setVertJustify(CellVertJustify2.CENTER)
				.setHoriJustify(CellHoriJustify.CENTER);
		if (StringUtils.isBlank(spielrundeSpielbahn) || StringUtils.equalsIgnoreCase("X", spielrundeSpielbahn)) {
			columnProperties.setWidth(500); // Paarungen cntr
			getSheetHelper().setColumnProperties(sheet, NUMMER_SPALTE_RUNDESPIELPLAN, columnProperties);
		} else {
			// Spielbahn Spalte header
			columnProperties.setWidth(900); // Paarungen cntr
			Position posErsteHeaderZelle = Position.from(NUMMER_SPALTE_RUNDESPIELPLAN, ERSTE_HEADER_ZEILE);
			Integer headerColor = getKonfigurationSheet().getSpielRundeHeaderFarbe();
			StringCellValue headerValue = StringCellValue.from(sheet, posErsteHeaderZelle).setRotateAngle(27000)
					.setVertJustify(CellVertJustify2.CENTER).setBorder(BorderFactory.from().allThin().toBorder())
					.setCellBackColor(headerColor).setCharHeight(14).setColumnProperties(columnProperties)
					.setEndPosMergeZeilePlus(1).setValue("Bahn").setComment("Spielbahn");
			getSheetHelper().setStringValueInCell(headerValue);

			RangePosition nbrRange = RangePosition.from(posErsteHeaderZelle,
					letzteZeile.spalte(NUMMER_SPALTE_RUNDESPIELPLAN));
			getSheetHelper().setPropertiesInRange(sheet, nbrRange, CellProperties.from().setCharHeight(16));
		}

		// Daten
		Position posErsteDatenZelle = Position.from(NUMMER_SPALTE_RUNDESPIELPLAN, ERSTE_DATEN_ZEILE);
		if (StringUtils.isBlank(spielrundeSpielbahn) || StringUtils.equalsIgnoreCase("X", spielrundeSpielbahn)
				|| StringUtils.equalsIgnoreCase("N", spielrundeSpielbahn)) {
			StringCellValue formulaCellValue = StringCellValue.from(sheet, posErsteDatenZelle);
			formulaCellValue.setValue("=ROW()-" + ERSTE_DATEN_ZEILE).setFillAutoDown(letzteZeile.getZeile());
			getSheetHelper().setFormulaInCell(formulaCellValue);
		} else if (StringUtils.startsWithIgnoreCase(spielrundeSpielbahn, "R")) {
			// Rx = Spielbahn -> random x = optional = max anzahl von Spielbahnen
			// anzahl paarungen ?
			int anzPaarungen = letzteZeile.getZeile() - ERSTE_DATEN_ZEILE + 1;
			int letzteBahnNr = anzPaarungen;

			// ist eine letzte bahnummer vorhanden ?
			if (spielrundeSpielbahn.length() > 1) {
				try {
					letzteBahnNr = Integer.parseInt(spielrundeSpielbahn.substring(1).trim());
				} catch (NumberFormatException | NullPointerException nfe) {
					// just ignore when no number found
				}
			}

			ArrayList<Integer> bahnnummern = new ArrayList<>();
			// fill
			for (int i = 1; i <= anzPaarungen; i++) {
				if (i <= letzteBahnNr) {
					bahnnummern.add(i);
				} else {
					bahnnummern.add(0); // platzhalter = spielpaarungen ohne bahnnummer
				}
			}
			// mishen
			Collections.shuffle(bahnnummern);
			StringCellValue stringCellValue = StringCellValue.from(sheet, posErsteDatenZelle);
			for (Integer bahnnr : bahnnummern) {
				if (bahnnr > 0) { // es kann sein das wir lücken haben, = teampaarungen ohne bahnnummer
					stringCellValue.setValue(bahnnr);
					getSheetHelper().setStringValueInCell(stringCellValue);
				}
				stringCellValue.zeilePlusEins();
			}
		}
	}

	/**
	 * bereich rechts neben der tabelle
	 *
	 * @param spielRunde
	 * @throws GenerateException
	 */

	protected void spielerNummerEinfuegen(MeleeSpielRunde spielRunde) throws GenerateException {

		processBoxinfo("Spieler Nummer einfügen");

		checkArgument(spielRunde.getNr() == getSpielRundeNr().getNr());

		HashSet<Integer> spielrNr = new HashSet<>();

		XSpreadsheet sheet = getXSpreadSheet();

		Position posErsteSpielrNr = Position.from(ERSTE_SPIELERNR_SPALTE, ERSTE_DATEN_ZEILE - 1);

		NumberCellValue numberCellValue = NumberCellValue
				.from(sheet, posErsteSpielrNr, MeldungenSpalte.MAX_ANZ_MELDUNGEN)
				.addCellProperty(VERT_JUSTIFY, CellVertJustify2.CENTER);

		StringCellValue validateCellVal = StringCellValue.from(numberCellValue).spalte(EINGABE_VALIDIERUNG_SPALTE)
				.setCharColor(ColorHelper.CHAR_COLOR_RED).setCharWeight(FontWeight.BOLD).setCharHeight(14)
				.setHoriJustify(CellHoriJustify.CENTER);

		List<Team> teams = spielRunde.teams();
		for (int teamCntr = 0; teamCntr < teams.size(); teamCntr++) {
			if ((teamCntr & 1) == 0) {
				// Team A
				numberCellValue.zeilePlusEins();
				numberCellValue.getPos().spalte(ERSTE_SPIELERNR_SPALTE);

				// paarung counter Spalte vor spielernr
				StringCellValue formulaCellValue = StringCellValue.from(numberCellValue).spalte(PAARUNG_CNTR_SPALTE)
						.setValue("=ROW()-" + ERSTE_DATEN_ZEILE);
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

				getSheetHelper().setNumberValueInCell(numberCellValue.setValue((double) spieler.getNr()));
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
		RangePosition datenRange = RangePosition.from(Position.from(ERSTE_SPIELERNR_SPALTE, ERSTE_DATEN_ZEILE),
				Position.from(ERSTE_SPIELERNR_SPALTE + 5, numberCellValue.getPos().getZeile()));
		// UND(ZÄHLENWENN($S:$S;INDIREKT(ADRESSE(ZEILE();SPALTE())))>1;INDIREKT(ADRESSE(ZEILE();SPALTE()))<>"")
		Position posSpalteSpielrNr = Position.from(ERSTE_SPALTE_VERTIKALE_ERGEBNISSE, ERSTE_DATEN_ZEILE);
		String conditionfindDoppelt = "COUNTIF(" + posSpalteSpielrNr.getSpalteAddressWith$() + ";"
				+ ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")>1";
		String conditionNotEmpty = ConditionalFormatHelper.FORMULA_CURRENT_CELL + "<>\"\"";
		String formulaFindDoppelteSpielrNr = "AND(" + conditionfindDoppelt + ";" + conditionNotEmpty + ")";
		ConditionalFormatHelper.from(this, datenRange).clear().formula1(formulaFindDoppelteSpielrNr)
				.operator(ConditionOperator.FORMULA).style(fehlerStyle).applyAndDoReset();
	}

	private void headerPaarungen(XSpreadsheet sheet, MeleeSpielRunde spielRunde) throws GenerateException {

		processBoxinfo("Header für Spielpaarungen");

		checkArgument(spielRunde.getNr() == getSpielRundeNr().getNr());

		// erste Header
		// -------------------------
		SpielTagNr spieltag = getSpielTag();
		Position ersteHeaderZeile = Position.from(ERSTE_SPALTE_RUNDESPIELPLAN, ERSTE_HEADER_ZEILE);
		Position ersteHeaderZeileMerge = Position.from(ersteHeaderZeile).spalte(ERSTE_SPALTE_ERGEBNISSE - 1);

		// back color
		Integer headerFarbe = getKonfigurationSheet().getSpielRundeHeaderFarbe();

		String ersteHeader = "Spielrunde " + spielRunde.getNr();
		if (getKonfigurationSheet().getSpielrunde1Header()) { // spieltag in header ?
			ersteHeader = spieltag.getNr() + ". Spieltag - " + ersteHeader;
		}

		StringCellValue headerVal = StringCellValue.from(sheet, ersteHeaderZeile, ersteHeader)
				.addCellProperty(CHAR_WEIGHT, FontWeight.BOLD).setEndPosMerge(ersteHeaderZeileMerge)
				.addCellProperty(HORI_JUSTIFY, CellHoriJustify.CENTER)
				.addCellProperty(TABLE_BORDER2, BorderFactory.from().allThin().toBorder()).setCharHeight(13)
				.setVertJustify(CellVertJustify2.CENTER).setCellBackColor(headerFarbe);
		getSheetHelper().setStringValueInCell(headerVal);

		// header spielernamen
		// -------------------------
		Position posSpielerNamen = Position.from(ERSTE_SPALTE_RUNDESPIELPLAN, ZWEITE_HEADER_ZEILE);
		Position posSpielerNamenMerge = Position.from(posSpielerNamen).spaltePlus(2);
		headerVal.setValue("Mannschaft A").setPos(posSpielerNamen).setEndPosMerge(posSpielerNamenMerge)
				// rechts Doppelte Linie
				.addCellProperty(TABLE_BORDER2, BorderFactory.from().allThin().doubleLn().forRight().toBorder());
		getSheetHelper().setStringValueInCell(headerVal);
		headerVal.setValue("Mannschaft B").setPos(posSpielerNamen.spaltePlus(3))
				.setEndPosMerge(posSpielerNamenMerge.spaltePlus(3))
				.addCellProperty(TABLE_BORDER2, BorderFactory.from().allThin().toBorder());
		getSheetHelper().setStringValueInCell(headerVal);

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
		getSheetHelper().setStringValueInCell(headerVal);

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
		processBoxinfo("Header Spieler Nummer ");

		Position pos = Position.from(ERSTE_SPIELERNR_SPALTE - 1, ERSTE_DATEN_ZEILE - 1);
		ColumnProperties columnProperties = ColumnProperties.from().setWidth(800).setHoriJustify(CellHoriJustify.CENTER)
				.isVisible(getKonfigurationSheet().zeigeArbeitsSpalten());
		StringCellValue headerCelVal = StringCellValue.from(sheet, Position.from(pos), "#")
				.addColumnProperties(columnProperties);
		getSheetHelper().setStringValueInCell(headerCelVal);
		headerCelVal.spaltePlusEins();

		for (int a = 1; a <= 3; a++) {
			getSheetHelper().setStringValueInCell(headerCelVal.setValue("A" + a));
			headerCelVal.spaltePlusEins();
		}
		for (int b = 1; b <= 3; b++) {
			getSheetHelper().setStringValueInCell(headerCelVal.setValue("B" + b));
			headerCelVal.spaltePlusEins();
		}
	}

	protected void verweisAufSpielerNamenEinfuegen(Position spielerNrPos) throws GenerateException {
		int anzSpaltenDiv = ERSTE_SPIELERNR_SPALTE - ERSTE_SPALTE_RUNDESPIELPLAN;
		String spielerNrAddress = spielerNrPos.getAddress();
		String formulaVerweis = "IFNA(" + getMeldeListe().formulaSverweisSpielernamen(spielerNrAddress) + ";\"\")";

		StringCellValue val = StringCellValue
				.from(getXSpreadSheet(), Position.from(spielerNrPos).spaltePlus(-anzSpaltenDiv), formulaVerweis)
				.setVertJustify(CellVertJustify2.CENTER).setShrinkToFit(true).setCharHeight(12);
		getSheetHelper().setFormulaInCell(val);
	}

	protected boolean neueSpielrunde(SpielerMeldungen meldungen, SpielRundeNr neueSpielrundeNr)
			throws GenerateException {
		return neueSpielrunde(meldungen, neueSpielrundeNr, isForceOk());
	}

	protected boolean neueSpielrunde(SpielerMeldungen meldungen, SpielRundeNr neueSpielrundeNr, boolean force)
			throws GenerateException {
		checkNotNull(meldungen);

		processBoxinfo("Neue Spielrunde " + neueSpielrundeNr.getNr() + " für Spieltag " + getSpielTag().getNr());
		processBoxinfo(meldungen.size() + " Meldungen");

		SuperMeleeMode superMeleeMode = getKonfigurationSheet().getSuperMeleeMode();
		SuperMeleeTeamRechner superMeleeTeamRechner = new SuperMeleeTeamRechner(meldungen.spieler().size(),
				superMeleeMode);

		if (!superMeleeTeamRechner.valideAnzahlSpieler()) {
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK).caption("Neue Spielrunde").message(
					superMeleeTeamRechner.getAnzSpieler() + " Meldungen, ist eine ungültige Anzahl. Meldeliste Prüfen")
					.show();
			return false;
		}

		setSpielRundeNr(neueSpielrundeNr);

		if (meldungen.spieler().size() < 4) {
			throw new GenerateException("Fehler beim erstellen von Spielrunde. Kann für Spieltag "
					+ getSpielTag().getNr() + " die Spielrunde " + neueSpielrundeNr.getNr()
					+ " nicht Auslosen. Anzahl Spieler < 4. Aktive Spieler = " + meldungen.spieler().size());
		}
		// -------------------------------
		if (getSheetHelper().findByName(getSheetName(getSpielTag(), getSpielRundeNr())) != null) {
			String msg = "Erstelle für Spieltag " + getSpielTag().getNr() + "\r\nSpielrunde " + neueSpielrundeNr.getNr()
					+ "\r\neine neue Spielrunde";
			MessageBoxResult msgBoxRslt = MessageBox.from(getxContext(), MessageBoxTypeEnum.QUESTION_OK_CANCEL)
					.forceOk(force).caption("Neue Spielrunde").message(msg).show();
			if (MessageBoxResult.CANCEL == msgBoxRslt) {
				ProcessBox.from().info("Abbruch vom Benutzer, Spielrunde wurde nicht erstellt");
				return false;
			}
		}
		// wenn hier dann neu erstellen
		if (!NewSheet.from(this, getSheetName(getSpielTag(), getSpielRundeNr())).pos(DefaultSheetPos.SUPERMELEE_WORK)
				.spielTagPageStyle(getSpielTag()).setForceCreate(force).setActiv().hideGrid().create().isDidCreate()) {
			ProcessBox.from().info("Abbruch vom Benutzer, Spielrunde wurde nicht erstellt");
			return false;
		}

		// neue Spielrunde speichern, sheet vorhanden
		getKonfigurationSheet().setAktiveSpielRunde(getSpielRundeNr());

		// Triplette oder Doublette Mode ?

		// -------------------------------
		boolean doubletteRunde = false; // nur doublettes vorhanden
		boolean tripletteRunde = false; // nur triplettes vorhanden
		boolean isKannNurDoublette = false;
		if (getKonfigurationSheet().getGleichePaarungenAktiv()) { // Sachverhalt beachten ? default false
			isKannNurDoublette = superMeleeTeamRechner.isNurDoubletteMoeglich();
		}
		// abfrage nur doublette runde ?
		if (superMeleeMode == SuperMeleeMode.Triplette && isKannNurDoublette) {
			MessageBox msgbox = MessageBox.from(getxContext(), MessageBoxTypeEnum.QUESTION_YES_NO).forceOk(force)
					.caption("Spielrunde Doublette");
			msgbox.message("Für Spieltag " + getSpielTag().getNr() + "\r\nSpielrunde " + neueSpielrundeNr.getNr()
					+ "\r\nnur Doublette Paarungen auslosen ?");
			if (MessageBoxResult.YES == msgbox.show()) {
				doubletteRunde = true;
			}
		} else if (superMeleeMode == SuperMeleeMode.Doublette && isKannNurDoublette) {
			doubletteRunde = true; // nur doublettes
		}

		// testdaten
		if (force && isKannNurDoublette) {
			doubletteRunde = true;
		}

		SuperMeleePaarungen paarungen = new SuperMeleePaarungen();
		try {
			MeleeSpielRunde spielRundeSheet;
			if (superMeleeMode == SuperMeleeMode.Triplette) {
				spielRundeSheet = paarungen.neueSpielrundeTripletteMode(neueSpielrundeNr.getNr(), meldungen,
						doubletteRunde);
			} else {
				spielRundeSheet = paarungen.neueSpielrundeDoubletteMode(neueSpielrundeNr.getNr(), meldungen,
						tripletteRunde);
			}

			spielRundeSheet.validateSpielerTeam(null);
			headerPaarungen(getXSpreadSheet(), spielRundeSheet);
			headerSpielerNr(getXSpreadSheet());
			spielerNummerEinfuegen(spielRundeSheet);
			vertikaleErgbnisseFormulaEinfuegen(spielRundeSheet);
			datenErsteSpalte();
			datenformatieren(getXSpreadSheet());
			spielrundeProperties(getXSpreadSheet());
			wennNurDoubletteRundeDannSpaltenAusblenden(getXSpreadSheet(), doubletteRunde);
			// TODO
			// int anzZeilen = spielTagInfosEinfuegen();
			printBereichDefinieren(getXSpreadSheet());
			// Spielrundeplan, ! nur hier instance erstellen
			if (getKonfigurationSheet().getSpielrundePlan()) {
				new SpielrundePlan(getWorkingSpreadsheet()).generate();
			}

		} catch (AlgorithmenException e) {
			getLogger().error(e.getMessage(), e);
			getSheetHelper().setActiveSheet(getMeldeListe().getXSpreadSheet());
			getSheetHelper().removeSheet(getSheetName(getSpielTag(), getSpielRundeNr()));
			getKonfigurationSheet().setAktiveSpielRunde(SpielRundeNr.from(getSpielRundeNr().getNr() - 1));
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK).caption("Fehler beim Auslosen")
					.message(e.getMessage()).show();
			throw new RuntimeException(e); // komplett raus
		}
		return true;
	}

	private void printBereichDefinieren(XSpreadsheet sheet) throws GenerateException {
		processBoxinfo("Print-Bereich");
		Position letzteZeile = letzteSpielrNrPosition();
		PrintArea.from(sheet, getWorkingSpreadsheet())
				.setPrintArea(RangePosition.from(NUMMER_SPALTE_RUNDESPIELPLAN, ERSTE_HEADER_ZEILE, letzteZeile));
	}

	private void wennNurDoubletteRundeDannSpaltenAusblenden(XSpreadsheet sheet, boolean doubletteRunde)
			throws GenerateException {
		if (doubletteRunde) {

			processBoxinfo("Nur Doublette Spielrunde, leere Spalten ausblenden");

			// 3e Spalte Team 1
			getSheetHelper().setColumnProperty(sheet, ERSTE_SPALTE_RUNDESPIELPLAN + 2, "IsVisible", false);
			// 3e Spalte Team 2
			getSheetHelper().setColumnProperty(sheet, ERSTE_SPALTE_RUNDESPIELPLAN + 5, "IsVisible", false);
		}
	}

	/**
	 * die für diesen Spielrunde properties ausgeben<br>
	 * Position Links unter Spielsheet
	 *
	 * @param sheet
	 * @throws GenerateException
	 */
	private void spielrundeProperties(XSpreadsheet sheet) throws GenerateException {

		processBoxinfo("Spielrunde Properties einfügen");

		Position datenEnd = letzteSpielrNrPosition();

		CellProperties cellPropBez = CellProperties.from().margin(150).setHoriJustify(CellHoriJustify.RIGHT)
				.setVertJustify(CellVertJustify2.CENTER).setBorder(BorderFactory.from().allThin().toBorder());

		StringCellValue propName = StringCellValue
				.from(sheet, Position.from(NUMMER_SPALTE_RUNDESPIELPLAN + 1, datenEnd.getZeile() + 1))
				.setCellProperties(cellPropBez);
		propName.zeilePlus(2);

		NumberCellValue propVal = NumberCellValue.from(propName).spaltePlusEins().setHoriJustify(CellHoriJustify.LEFT)
				.setBorder(BorderFactory.from().allThin().toBorder());

		// "Aktiv"
		int anzAktiv = meldeListe.getAnzahlAktiveSpieler(getSpielTag());
		getSheetHelper().setStringValueInCell(propName.setValue("Aktiv :").setComment("Anzahl Spieler in diese Runde"));
		getSheetHelper().setNumberValueInCell(propVal.setValue((double) anzAktiv));

		int anzAusg = meldeListe.getAusgestiegenSpieler(getSpielTag());
		getSheetHelper().setStringValueInCell(propName.zeilePlusEins().setValue("Ausgestiegen :")
				.setComment("Anzahl Spieler die nicht in diese Runde Mitspielen"));
		getSheetHelper().setNumberValueInCell(propVal.zeilePlusEins().setValue((double) anzAusg));

		SuperMeleeMode superMeleeMode = getKonfigurationSheet().getSuperMeleeMode();

		getSheetHelper()
				.setStringValueInCell(propName.zeilePlusEins().setValue("Modus :").setComment("Supermêlée Modus"));
		getSheetHelper()
				.setStringValueInCell(StringCellValue.from(propVal).zeilePlusEins().setValue(superMeleeMode.name()));
	}

	private void datenformatieren(XSpreadsheet sheet) throws GenerateException {

		processBoxinfo("Daten Formatieren");

		// gitter
		Position datenStart = Position.from(NUMMER_SPALTE_RUNDESPIELPLAN, ERSTE_DATEN_ZEILE);
		Position datenEnd = letzteSpielrNrPosition();

		// bis zur mitte mit normal gitter
		RangePosition datenRangeErsteHaelfte = RangePosition.from(datenStart,
				Position.from(ERSTE_SPALTE_RUNDESPIELPLAN + 2, datenEnd.getZeile()));
		TableBorder2 border = BorderFactory.from().allThin().boldLn().forTop().toBorder();
		getSheetHelper().setPropertyInRange(sheet, datenRangeErsteHaelfte, TABLE_BORDER2, border);

		// zweite haelfte doppelte linie links
		RangePosition datenRangeZweiteHaelfte = RangePosition
				.from(Position.from(ERSTE_SPALTE_RUNDESPIELPLAN + 3, ERSTE_DATEN_ZEILE), datenEnd);
		border = BorderFactory.from().allThin().boldLn().forTop().doubleLn().forLeft().toBorder();
		getSheetHelper().setPropertyInRange(sheet, datenRangeZweiteHaelfte, TABLE_BORDER2, border);

		// zeile höhe
		// currcell.getRows().Height = 750
		for (int zeileCntr = ERSTE_HEADER_ZEILE; zeileCntr <= datenEnd.getZeile(); zeileCntr++) {
			getSheetHelper().setRowProperty(sheet, zeileCntr, HEIGHT, 800);
		}

		// Ergebnis Zellen
		RangePosition ergbenissRange = RangePosition.from(Position.from(ERSTE_SPALTE_ERGEBNISSE, ERSTE_DATEN_ZEILE),
				Position.from(datenEnd));
		getSheetHelper().setPropertyInRange(sheet, ergbenissRange, CHAR_HEIGHT, 16);
		getSheetHelper().setPropertyInRange(sheet, ergbenissRange, CHAR_WEIGHT, FontWeight.BOLD);

		// gerade / ungrade hintergrund farbe
		// CellBackColor
		RangePosition datenRangeOhneErsteSpalteOhneErgebnis = RangePosition.from(
				Position.from(ERSTE_SPALTE_RUNDESPIELPLAN, ERSTE_DATEN_ZEILE),
				datenEnd.spalte(ERSTE_SPALTE_ERGEBNISSE - 1));

		Integer geradeColor = getKonfigurationSheet().getSpielRundeHintergrundFarbeGerade();
		Integer unGeradeColor = getKonfigurationSheet().getSpielRundeHintergrundFarbeUnGerade();
		SpielrundeHintergrundFarbeGeradeStyle spielrundeHintergrundFarbeGeradeStyle = new SpielrundeHintergrundFarbeGeradeStyle(
				geradeColor);
		SpielrundeHintergrundFarbeUnGeradeStyle spielrundeHintergrundFarbeUnGeradeStyle = new SpielrundeHintergrundFarbeUnGeradeStyle(
				unGeradeColor);

		ConditionalFormatHelper.from(this, datenRangeOhneErsteSpalteOhneErgebnis).clear().formulaIsEvenRow()
				.style(spielrundeHintergrundFarbeGeradeStyle).applyAndDoReset();
		ConditionalFormatHelper.from(this, datenRangeOhneErsteSpalteOhneErgebnis).formulaIsOddRow()
				.operator(ConditionOperator.FORMULA).style(spielrundeHintergrundFarbeUnGeradeStyle).applyAndDoReset();

		// erste Spalte, mit prüfung auf doppelte nummer
		Position posNrSpalte = Position.from(NUMMER_SPALTE_RUNDESPIELPLAN, ERSTE_DATEN_ZEILE);
		RangePosition datenRangeErsteSpalte = RangePosition.from(posNrSpalte,
				datenEnd.spalte(NUMMER_SPALTE_RUNDESPIELPLAN));

		FehlerStyle fehlerStyle = new FehlerStyle();
		String conditionfindDoppelt = "COUNTIF(" + posNrSpalte.getSpalteAddressWith$() + ";"
				+ ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")>1";
		String conditionNotEmpty = ConditionalFormatHelper.FORMULA_CURRENT_CELL + "<>\"\"";
		String formulaFindDoppelteSpielrNr = "AND(" + conditionfindDoppelt + ";" + conditionNotEmpty + ")";
		ConditionalFormatHelper.from(this, datenRangeErsteSpalte).clear().formula1(formulaFindDoppelteSpielrNr)
				.operator(ConditionOperator.FORMULA).style(fehlerStyle).applyAndDoReset();

		ConditionalFormatHelper.from(this, datenRangeErsteSpalte).formulaIsEvenRow()
				.style(spielrundeHintergrundFarbeGeradeStyle).applyAndDoReset();
		ConditionalFormatHelper.from(this, datenRangeErsteSpalte).formulaIsOddRow()
				.style(spielrundeHintergrundFarbeUnGeradeStyle).applyAndDoReset();

		// ergebniss spalten mit prüfung auf >=0 <=13
		ConditionalFormatHelper.from(this, ergbenissRange).clear().formula1("0").formula2("13")
				.operator(ConditionOperator.NOT_BETWEEN).styleIsFehler().applyAndDoReset();
		// test if Text mit FORMULA
		String formula = "ISTEXT(" + ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")";
		ConditionalFormatHelper.from(this, ergbenissRange).formula1(formula).operator(ConditionOperator.FORMULA)
				.styleIsFehler().applyAndDoReset();
		ConditionalFormatHelper.from(this, ergbenissRange).formulaIsEvenRow()
				.style(spielrundeHintergrundFarbeGeradeStyle).applyAndDoReset();
		ConditionalFormatHelper.from(this, ergbenissRange).formulaIsOddRow()
				.style(spielrundeHintergrundFarbeUnGeradeStyle).applyAndDoReset();
	}

	/**
	 * rechts unten, letzte ergebnis zelle
	 *
	 * @return
	 * @throws GenerateException
	 */

	protected Position letzteSpielrNrPosition() throws GenerateException {
		XSpreadsheet sheet = getXSpreadSheet();
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
	 * in der meldungen liste alle spieler die liste warimTeammit fuellen.<br>
	 * ggf vergangene Spieltage komplet einlesen
	 *
	 * @param aktiveMeldungen liste der Aktive Meldungen
	 * @param bisSpielrunde bis zu diese spielrunde
	 * @param abSpielrunde ab diese spielrunde = default = 1
	 * @throws GenerateException
	 */
	protected void gespieltenRundenEinlesen(SpielerMeldungen aktiveMeldungen, int abSpielrunde, int bisSpielrunde)
			throws GenerateException {
		SpielTagNr aktuelleSpielTag = getSpielTag();

		Integer maxAnzGespielteSpieltage = getMaxAnzGespielteSpieltage();
		int bisVergangeneSpieltag = aktuelleSpielTag.getNr() - 1 - maxAnzGespielteSpieltage;
		if (bisVergangeneSpieltag < 0) {
			bisVergangeneSpieltag = 0;
		}

		for (int vergangeneSpieltag = aktuelleSpielTag.getNr()
				- 1; vergangeneSpieltag > bisVergangeneSpieltag; vergangeneSpieltag--) {
			gespieltenRundenEinlesen(aktiveMeldungen, SpielTagNr.from(vergangeneSpieltag), 1, 999);
		}
		gespieltenRundenEinlesen(aktiveMeldungen, getSpielTag(), abSpielrunde, bisSpielrunde);
	}

	/**
	 * 
	 * @return anzahl spieltage die bei der neu auslosung eingelesen wird (hat zusammen
	 * @throws GenerateException
	 */
	public Integer getMaxAnzGespielteSpieltage() throws GenerateException {
		return getKonfigurationSheet().getMaxAnzGespielteSpieltage();
	}

	/**
	 * @param aktiveMeldungen liste der Aktive Meldungen
	 * @param spielTagNr
	 * @param abSpielrunde
	 * @param bisSpielrunde
	 * @throws GenerateException
	 */

	protected void gespieltenRundenEinlesen(SpielerMeldungen aktiveMeldungen, SpielTagNr spielTagNr, int abSpielrunde,
			int bisSpielrunde) throws GenerateException {
		int spielrunde = 1;

		if (bisSpielrunde < abSpielrunde || bisSpielrunde < 1) {
			return;
		}

		if (abSpielrunde > 1) {
			spielrunde = abSpielrunde;
		}

		processBoxinfo("Meldungen von gespielten Runden einlesen. Spieltag:" + spielTagNr.getNr() + " Von Runde:"
				+ spielrunde + " Bis Runde:" + bisSpielrunde);

		for (; spielrunde <= bisSpielrunde; spielrunde++) {
			SheetRunner.testDoCancelTask();

			// XSpreadsheet sheet = getSpielRundeSheet(getSpielTag(), SpielRundeNr.from(spielrunde));
			XSpreadsheet sheet = getSheetHelper().findByName(getSheetName(spielTagNr, SpielRundeNr.from(spielrunde)));

			if (sheet == null) {
				continue;
			}
			Position pospielerNr = Position.from(ERSTE_SPIELERNR_SPALTE, ERSTE_DATEN_ZEILE);

			boolean zeileIstLeer = false;
			int maxcntr = 999; // sollte nicht vorkommen, endlos schleife vermeiden in fehlerfall
			while (!zeileIstLeer && maxcntr > 0) {
				maxcntr--;
				for (int teamCntr = 1; teamCntr <= 2; teamCntr++) { // Team A & B
					Team team = Team.from(1); // dummy team verwenden um Spieler gegenseitig ein zu tragen
					// 3 spalten
					for (int spielerCntr = 1; spielerCntr <= 3; spielerCntr++) {
						pospielerNr.spalte(ERSTE_SPIELERNR_SPALTE + ((teamCntr - 1) * 3) + spielerCntr - 1);
						int spielerNr = getSheetHelper().getIntFromCell(sheet, pospielerNr); // Spieler aus Rundeliste
						if (spielerNr > 0) {
							Spieler spieler = aktiveMeldungen.findSpielerByNr(spielerNr);
							if (spieler != null) { // ist dann der fall wenn der spieler Ausgestiegen ist
								try {
									team.addSpielerWennNichtVorhanden(spieler); // im gleichen Team = wird gegenseitig eingetragen
								} catch (AlgorithmenException e) {
									logger.error(e.getMessage(), e);
									throw new GenerateException(
											"Fehler beim einlesen der gespielten Runden. siehe log datei für details");
								}
							}
						}
					}
				}
				// Spalte Paarungen Cntr Prüfen
				pospielerNr.zeilePlusEins().spalte(PAARUNG_CNTR_SPALTE);
				if (getSheetHelper().getIntFromCell(sheet, pospielerNr) == -1) {
					// keine paarungen mehr vorhanden
					zeileIstLeer = true;
				}
			}
		}
	}

	/**
	 * @throws GenerateException
	 */

	protected void clearSheet() throws GenerateException {
		Position letzteZeile = letzteSpielrNrPosition();

		if (letzteZeile == null) {
			return; // keine Daten
		}
		RangePosition rangPos = RangePosition.from(NUMMER_SPALTE_RUNDESPIELPLAN, ERSTE_DATEN_ZEILE, LETZTE_SPALTE,
				letzteZeile.getZeile());
		RangeHelper.from(this, rangPos).clearRange();
	}

	public SpielTagNr getSpielTag() {
		checkNotNull(spielTag);
		return spielTag;
	}

	public void setSpielTag(SpielTagNr spielTag) {
		checkNotNull(spielTag);
		this.spielTag = spielTag;
	}

	/**
	 * nicht die globale Aktive spielnummer
	 *
	 * @return
	 */

	public SpielRundeNr getSpielRundeNr() {
		checkNotNull(spielRundeNr);
		return spielRundeNr;
	}

	/**
	 * nicht die global aktive Spielrunde
	 *
	 * @param spielrunde
	 */

	public void setSpielRundeNr(SpielRundeNr spielrunde) {
		checkNotNull(spielrunde);
		spielRundeNr = spielrunde;
	}

	public boolean isForceOk() {
		return forceOk;
	}

	public void setForceOk(boolean forceOk) {
		this.forceOk = forceOk;
	}
}