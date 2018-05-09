/**
* Erstellung : 22.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

import static com.google.common.base.Preconditions.*;
import static de.petanqueturniermanager.helper.cellvalue.CellProperties.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.beans.PropertyValue;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.table.TableSortField;
import com.sun.star.table.XCellRange;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XSortable;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.msgbox.ErrorMessageBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.IMitSpielerSpalte;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SpielerSpalte;
import de.petanqueturniermanager.konfiguration.KonfigurationSheet;
import de.petanqueturniermanager.model.Meldungen;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.SupermeleeTeamPaarungenSheet;

abstract public class AbstractMeldeListeSheet extends SheetRunner
		implements IMeldeliste, Runnable, ISheet, IMitSpielerSpalte {
	private static final String SPIELTAG_HEADER_STR = "Spieltag";

	public static final int SPALTE_FORMATION = 0; // siehe enum #Formation Spalte 0
	public static final int ZEILE_FORMATION = 0; // Zeile 0

	public static final int ERSTE_DATEN_ZEILE = 2; // Zeile 3
	public static final int SPIELER_NR_SPALTE = 0; // Spalte A=0
	public static final int HEADER_ZEILE = ERSTE_DATEN_ZEILE - 1; // Zeile 2

	public static final int SUMMEN_SPALTE_OFFSET = 2; // 2 Spalten weiter zur letzte Spieltag
	public static final int SUMMEN_ERSTE_ZEILE = ERSTE_DATEN_ZEILE; // Zeile 3
	public static final int SUMMEN_AKTIVE_ZEILE = SUMMEN_ERSTE_ZEILE; // Zeile 6
	public static final int SUMMEN_INAKTIVE_ZEILE = SUMMEN_ERSTE_ZEILE + 1;
	public static final int SUMMEN_AUSGESTIEGENE_ZEILE = SUMMEN_ERSTE_ZEILE + 2; // Zeile 8
	public static final int SUMMEN_KANN_DOUBLETTE_ZEILE = SUMMEN_ERSTE_ZEILE + 7; // Zeile 10

	public static final int ERSTE_ZEILE_PROPERTIES = 12; // Zeile 13

	public static final String SHEETNAME = "Meldeliste";
	public static final short SHEET_POS = (short) 0;
	public static final String SHEET_COLOR = "2544dd";

	private final ErrorMessageBox errMsgBox;
	private final SheetHelper sheetHelper;
	private final SpielerSpalte spielerSpalte;
	private final SupermeleeTeamPaarungenSheet supermeleeTeamPaarungen;
	private final KonfigurationSheet konfigurationSheet;
	private SpielTagNr spielTag = null;

	public AbstractMeldeListeSheet(XComponentContext xContext) {
		super(xContext);
		this.konfigurationSheet = newKonfigurationSheet(xContext);
		this.sheetHelper = new SheetHelper(xContext);
		this.spielerSpalte = new SpielerSpalte(xContext, ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE, this, this,
				Formation.MELEE);
		this.supermeleeTeamPaarungen = new SupermeleeTeamPaarungenSheet(xContext);
		this.errMsgBox = new ErrorMessageBox(xContext);
	}

	@VisibleForTesting
	KonfigurationSheet newKonfigurationSheet(XComponentContext xContext) {
		return new KonfigurationSheet(xContext);
	}

	/**
	 * anzahl header zählen
	 *
	 * @return
	 * @throws GenerateException
	 */
	public int countAnzSpieltage() throws GenerateException {
		int anzSpieltage = 0;
		int ersteSpieltagspalteSpalte = ersteSpieltagspalteSpalte();
		Position posHeader = Position.from(ersteSpieltagspalteSpalte, HEADER_ZEILE);

		for (int spaltecntr = 0; spaltecntr < 90; spaltecntr++) {
			String header = this.getSheetHelper().getTextFromCell(this.getSheet(), posHeader);

			if (header != null && header.contains(SPIELTAG_HEADER_STR)) {
				anzSpieltage++;
			}
			posHeader.spaltePlusEins();
		}
		return anzSpieltage;
	}

	public Formation getFormation() throws GenerateException {
		return this.getKonfigurationSheet().getFormation();
	}

	@Override
	public XSpreadsheet getSheet() {
		return this.sheetHelper.newIfNotExist(SHEETNAME, SHEET_POS, SHEET_COLOR);
	}

	public void show() {
		this.sheetHelper.setActiveSheet(getSheet());
	}

	public void upDateSheet() throws GenerateException {

		if (isErrorInSheet()) {
			return;
		}

		XSpreadsheet sheet = getSheet();
		this.sheetHelper.setActiveSheet(sheet);
		this.sheetHelper.setValInCell(sheet, SPALTE_FORMATION, ZEILE_FORMATION, getFormation().getId());
		this.sheetHelper.setTextInCell(sheet, SPALTE_FORMATION + 1, ZEILE_FORMATION, getFormation().getBezeichnung());
		// Header einfuegen
		// ------
		int hederBackColor = this.getKonfigurationSheet().getRanglisteHeaderFarbe();
		this.spielerSpalte.insertHeaderInSheet(hederBackColor);
		if (nichtZusammenSpielenSpalte() > -1) {

			StringCellValue bezCelVal = StringCellValue
					.from(sheet, nichtZusammenSpielenSpalte(), HEADER_ZEILE, "SetzPos")
					.setSpalteHoriJustify(CellHoriJustify.CENTER)
					.setComment("1 = Setzposition, Diesen Spieler werden nicht zusammen im gleichen Team gelost.")
					.setColumnWidth(800).setCellBackColor(hederBackColor)
					.setBorder(BorderFactory.from().allThin().toBorder());
			this.sheetHelper.setTextInCell(bezCelVal);
		}

		formatSpielTagSpalte(getSpielTag());

		// eventuelle luecken in spiele namen nach unten sortieren
		lueckenEntfernen();
		updateSpielerNr();

		doSort(this.spielerSpalte.getSpielerNameSpalte(), true); // nach namen sortieren
		updateSpieltageSummenSpalten();
		this.spielerSpalte.formatDaten();
		this.formatDaten();
	}

	protected void formatSpielTagSpalte(SpielTagNr spieltag) throws GenerateException {
		checkNotNull(spieltag);
		XSpreadsheet sheet = getSheet();
		int hederBackColor = this.getKonfigurationSheet().getRanglisteHeaderFarbe();
		StringCellValue bezCelSpieltagVal = StringCellValue
				.from(sheet, spieltagSpalte(spieltag), HEADER_ZEILE, spielTagHeader(spieltag))
				.setSpalteHoriJustify(CellHoriJustify.CENTER).setComment("1 = Aktiv, 2 = Ausgestiegen, leer = InAktiv")
				.setColumnWidth(2000).setCellBackColor(hederBackColor)
				.setBorder(BorderFactory.from().allThin().toBorder());

		// Spieltag header
		bezCelSpieltagVal.setValue(spielTagHeader(spieltag));
		this.sheetHelper.setTextInCell(bezCelSpieltagVal);
	}

	void formatDaten() throws GenerateException {
		int letzteDatenZeile = this.spielerSpalte.letzteDatenZeile();
		if (letzteDatenZeile < ERSTE_DATEN_ZEILE) {
			// keine Daten
			return;
		}

		RangePosition datenRange = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, letzteSpielTagSpalte(),
				letzteDatenZeile);

		this.sheetHelper.setPropertiesInRange(getSheet(), datenRange,
				CellProperties.from().setVertJustify(CellVertJustify2.CENTER)
						.setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
						.setCharColor(ColorHelper.CHAR_COLOR_BLACK).setCellBackColor(-1).setShrinkToFit(true));

		// gerade / ungrade hintergrund farbe
		// CellBackColor
		Integer geradeColor = this.getKonfigurationSheet().getRanglisteHintergrundFarbeGerade();
		Integer unGeradeColor = this.getKonfigurationSheet().getRanglisteHintergrundFarbeUnGerade();

		int letzteSpielTagSpalte = letzteSpielTagSpalte();

		for (int zeileCntr = datenRange.getStartZeile(); zeileCntr <= letzteDatenZeile; zeileCntr++) {
			RangePosition datenRangeLine = RangePosition.from(0, zeileCntr, letzteSpielTagSpalte, zeileCntr);
			if ((zeileCntr & 1) == 0) {
				if (unGeradeColor != null) {
					this.sheetHelper.setPropertyInRange(getSheet(), datenRangeLine, CELL_BACK_COLOR, unGeradeColor);
				}
			} else {
				if (geradeColor != null) {
					this.sheetHelper.setPropertyInRange(getSheet(), datenRangeLine, CELL_BACK_COLOR, geradeColor);
				}
			}
		}

	}

	/**
	 * @param spieltag = 1 bis x
	 * @return
	 * @throws GenerateException
	 */
	public String spielTagHeader(SpielTagNr spieltag) throws GenerateException {
		return spieltag.getNr() + ". " + SPIELTAG_HEADER_STR;
	}

	/**
	 * diese Spalte nur beim supermelee
	 *
	 * @return -1 wenn nicht vorhanden
	 * @throws GenerateException
	 */
	public int nichtZusammenSpielenSpalte() throws GenerateException {
		if (getFormation() == Formation.MELEE) {
			return 2; // Spalte C
		}
		return -1;
	}

	public int letzteSpielTagSpalte() throws GenerateException {
		int anzSpieltage = countAnzSpieltage();
		return ersteSpieltagspalteSpalte() + (anzSpieltage - 1);
	}

	public int ersteSpieltagspalteSpalte() throws GenerateException {
		if (nichtZusammenSpielenSpalte() > -1) {
			return nichtZusammenSpielenSpalte() + 1;
		}
		return SPIELER_NR_SPALTE + this.spielerSpalte.anzahlSpielerNamenSpalten();
	}

	public int aktuelleSpieltagSpalte() throws GenerateException {
		return spieltagSpalte(this.getKonfigurationSheet().getAktiveSpieltag());
	}

	public int spieltagSpalte(SpielTagNr spieltag) throws GenerateException {
		return ersteSpieltagspalteSpalte() + spieltag.getNr() - 1;
	}

	public int ersteSummeSpalte() throws GenerateException {
		return letzteSpielTagSpalte() + SUMMEN_SPALTE_OFFSET;
	}

	@Override
	public String formulaSverweisSpielernamen(String spielrNrAdresse) {
		String ersteZelleAddress = Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE).getAddressWith$();
		String letzteZelleAddress = Position.from(this.spielerSpalte.getSpielerNameSpalte(), 999).getAddressWith$();
		return "=VLOOKUP(" + spielrNrAdresse + ";$'" + SHEETNAME + "'." + ersteZelleAddress + ":" + letzteZelleAddress
				+ ";2;0)";
	}

	public void lueckenEntfernen() throws GenerateException {
		doSort(this.spielerSpalte.getSpielerNameSpalte(), true); // alle zeilen ohne namen nach unten sortieren, egal ob
		// daten oder nicht
		int letzteNrZeile = this.spielerSpalte.neachsteFreieDatenZeile();
		if (letzteNrZeile < ERSTE_DATEN_ZEILE) { // daten vorhanden ?
			return; // keine Daten
		}
		XSpreadsheet xSheet = getSheet();

		for (int spielerNrZeilecntr = letzteNrZeile; spielerNrZeilecntr >= ERSTE_DATEN_ZEILE; spielerNrZeilecntr--) {
			String spielerNamen = this.sheetHelper.getTextFromCell(xSheet,
					Position.from(this.spielerSpalte.getSpielerNameSpalte(), spielerNrZeilecntr));
			// Achtung alle durchgehen weil eventuell lücken in der nr spalte!
			if (StringUtils.isBlank(spielerNamen)) { // null oder leer oder leerzeichen
				// nr ohne spieler namen entfernen
				this.sheetHelper.setTextInCell(xSheet, SPIELER_NR_SPALTE, spielerNrZeilecntr, "");
			}
		}

	}

	public int getSpielerNameSpalte() {
		return this.spielerSpalte.getSpielerNameSpalte();
	}

	public boolean isErrorInSheet() throws GenerateException {
		XSpreadsheet xSheet = getSheet();

		int letzteSpielZeile = this.spielerSpalte.letzteZeileMitSpielerName();
		if (letzteSpielZeile <= ERSTE_DATEN_ZEILE) { // daten vorhanden ?
			return false; // keine Daten
		}

		doSort(SPIELER_NR_SPALTE, false); // hoechste nummer oben, ohne nummer nach unten

		// doppelte spieler Nummer entfernen !?!?!
		HashSet<Integer> spielrNrInSheet = new HashSet<>();
		int spielrNr;
		NumberCellValue celVal = NumberCellValue.from(xSheet, Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE))
				.setCharColor(ColorHelper.CHAR_COLOR_RED);
		for (int spielerZeilecntr = ERSTE_DATEN_ZEILE; spielerZeilecntr <= letzteSpielZeile; spielerZeilecntr++) {
			spielrNr = this.sheetHelper.getIntFromCell(xSheet, Position.from(SPIELER_NR_SPALTE, spielerZeilecntr));
			if (spielrNr == -1) {
				// keine nummer vorhanden
				continue;
			}
			if (spielrNrInSheet.contains(spielrNr)) {
				// RED Color
				this.sheetHelper.setValInCell(celVal.setValue((double) spielrNr).zeile(spielerZeilecntr));
				this.errMsgBox.showOk("Fehler", "Meldeliste wurde nicht Aktualisiert.\r\nSpieler Nr. " + spielrNr
						+ " ist doppelt in der Meldliste !!!");
				return true;
			} else {
				spielrNrInSheet.add(spielrNr);
			}
		}
		return false;
	}

	public void updateSpielerNr() throws GenerateException {
		int letzteSpielZeile = this.spielerSpalte.letzteZeileMitSpielerName();
		if (letzteSpielZeile < ERSTE_DATEN_ZEILE) { // daten vorhanden ?
			return; // keine Daten
		}
		XSpreadsheet xSheet = getSheet();
		doSort(SPIELER_NR_SPALTE, false); // hoechste nummer oben, ohne nummer nach unten

		int letzteSpielerNr = 0;
		int spielrNr = this.sheetHelper.getIntFromCell(xSheet, Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE));
		if (spielrNr > -1) {
			letzteSpielerNr = spielrNr;
		}
		// spieler nach Alphabet sortieren
		doSort(this.spielerSpalte.getSpielerNameSpalte(), true);

		// lücken füllen
		NumberCellValue celVal = NumberCellValue.from(xSheet, Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE));
		for (int spielerZeilecntr = ERSTE_DATEN_ZEILE; spielerZeilecntr <= letzteSpielZeile; spielerZeilecntr++) {
			spielrNr = this.sheetHelper.getIntFromCell(xSheet, Position.from(SPIELER_NR_SPALTE, spielerZeilecntr));
			if (spielrNr == -1) {
				this.sheetHelper.setValInCell(celVal.setValue((double) ++letzteSpielerNr).zeile(spielerZeilecntr));
			}
		}
	}

	/**
	 * alle sortierbare daten, ohne header !
	 *
	 * @return
	 * @throws GenerateException
	 */
	private XCellRange getxCellRangeAlleDaten() throws GenerateException {
		XSpreadsheet xSheet = getSheet();
		XCellRange xCellRange = null;
		try {
			int letzteSpielZeile = this.spielerSpalte.letzteZeileMitSpielerName();
			if (letzteSpielZeile > ERSTE_DATEN_ZEILE) { // daten vorhanden ?
				// (column, row, column, row)
				xCellRange = xSheet.getCellRangeByPosition(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, letzteSpielTagSpalte(),
						letzteSpielZeile);
			}
		} catch (IndexOutOfBoundsException e) {
			getLogger().error(e.getMessage(), e);
			return null;
		}
		return xCellRange;
	}

	public void doSort(int spalteNr, boolean isAscending) throws GenerateException {

		XCellRange xCellRange = getxCellRangeAlleDaten();

		if (xCellRange == null) {
			return;
		}

		XSortable xSortable = UnoRuntime.queryInterface(XSortable.class, xCellRange);

		TableSortField[] aSortFields = new TableSortField[1];
		TableSortField field1 = new TableSortField();
		field1.Field = spalteNr; // 0 = erste spalte, nur eine Spalte sortieren
		field1.IsAscending = isAscending;
		// Note – The FieldType member, that is used to select textual or numeric sorting in
		// text documents is ignored in the spreadsheet application. In a spreadsheet, a cell
		// always has a known type of text or value, which is used for sorting, with numbers
		// sorted before text cells.
		aSortFields[0] = field1;

		PropertyValue[] aSortDesc = new PropertyValue[2];
		PropertyValue propVal = new PropertyValue();
		propVal.Name = "SortFields";
		propVal.Value = aSortFields;
		aSortDesc[0] = propVal;

		// specifies if cell formats are moved with the contents they belong to.
		propVal = new PropertyValue();
		propVal.Name = "BindFormatsToContent";
		propVal.Value = false;
		aSortDesc[1] = propVal;

		xSortable.sort(aSortDesc);
	}

	public void updateSpieltageSummenSpalten() throws GenerateException {

		if (this.spielerSpalte.letzteDatenZeile() < ERSTE_DATEN_ZEILE) {
			return; // keine daten
		}

		XSpreadsheet sheet = getSheet();

		int anzSpieltage = countAnzSpieltage();

		RangePosition cleanUpRange = RangePosition.from(ersteSummeSpalte() - 1, 0,
				ersteSummeSpalte() + anzSpieltage + 10, 999);
		getSheetHelper().clearRange(sheet, cleanUpRange);

		Position posBezeichnug = Position.from(ersteSummeSpalte(), SUMMEN_ERSTE_ZEILE - 1);

		StringCellValue bezCelVal = StringCellValue.from(sheet, posBezeichnug, "")
				.setSpalteHoriJustify(CellHoriJustify.RIGHT).setComment(null).setColumnWidth(3000)
				.removeCellBackColor();
		this.sheetHelper.setTextInCell(bezCelVal);

		bezCelVal.setComment(null).setValue("Aktiv").zeile(SUMMEN_AKTIVE_ZEILE);
		this.sheetHelper.setTextInCell(bezCelVal);

		bezCelVal.setComment(null).setValue("InAktiv").zeile(SUMMEN_INAKTIVE_ZEILE);
		this.sheetHelper.setTextInCell(bezCelVal);

		bezCelVal.setComment("Spieler mit \"2\" im Spieltag").setValue("Ausgestiegen")
				.zeile(SUMMEN_AUSGESTIEGENE_ZEILE);
		this.sheetHelper.setTextInCell(bezCelVal);

		bezCelVal.setComment("Aktive + Ausgestiegen").setValue("Anz. Spieler").zeilePlusEins();
		this.sheetHelper.setTextInCell(bezCelVal);

		bezCelVal.setComment("Aktive + Inaktiv + Ausgestiegen").setValue("Summe").zeilePlusEins();
		this.sheetHelper.setTextInCell(bezCelVal);

		bezCelVal.setComment("Doublette Teams").setValue("∑x2").zeilePlusEins();
		this.sheetHelper.setTextInCell(bezCelVal);

		bezCelVal.setComment("Triplette Teams").setValue("∑x3").zeilePlusEins();
		this.sheetHelper.setTextInCell(bezCelVal);

		bezCelVal.setComment("Kann Doublette gespielt werden").setValue("Doublette").zeilePlusEins();
		this.sheetHelper.setTextInCell(bezCelVal.zeile(SUMMEN_KANN_DOUBLETTE_ZEILE));

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {

			SpielTagNr spielTagNr = new SpielTagNr(spieltagCntr);

			Position posSpieltagWerte = Position.from(ersteSummeSpalte() + spieltagCntr, SUMMEN_ERSTE_ZEILE - 1);

			// Header
			this.sheetHelper.setColumnWidthAndHoriJustifyCenter(sheet, posSpieltagWerte, 1000, "Tag " + spieltagCntr);

			// Summe Aktive Spieler "=ZÄHLENWENN(D3:D102;1)"
			this.sheetHelper.setFormulaInCell(sheet, posSpieltagWerte.zeile(SUMMEN_AKTIVE_ZEILE),
					"=" + formulaCountSpieler(spielTagNr, "1"));

			// Summe inAktive Spieler "=ZÄHLENWENN(D3:D102;0) + ZÄHLENWENN(D3:D102;"")"
			this.sheetHelper.setFormulaInCell(sheet, posSpieltagWerte.zeile(SUMMEN_INAKTIVE_ZEILE),
					"=" + formulaCountSpieler(spielTagNr, "0") + " + " + formulaCountSpieler(spielTagNr, "\"\""));

			// Ausgestiegen =ZÄHLENWENN(D3:D102;2)
			this.sheetHelper.setFormulaInCell(sheet, posSpieltagWerte.zeile(SUMMEN_AUSGESTIEGENE_ZEILE),
					"=" + formulaCountSpieler(spielTagNr, "2"));
			// -----------------------------------
			// Aktiv + Ausgestiegen
			Position anzahlAktiveSpielerPosition = getAnzahlAktiveSpielerPosition(spielTagNr);
			String aktivZelle = this.sheetHelper.getAddressFromColumnRow(anzahlAktiveSpielerPosition);
			String ausgestiegenZelle = this.sheetHelper
					.getAddressFromColumnRow(getAusgestiegenSpielerPosition(spielTagNr));
			this.sheetHelper.setFormulaInCell(sheet, posSpieltagWerte.zeilePlusEins(),
					"=" + aktivZelle + "+" + ausgestiegenZelle);
			// -----------------------------------
			// =K7+K8+K9
			// Aktiv + Ausgestiegen + inaktive
			String inAktivZelle = this.sheetHelper.getAddressFromColumnRow(anzahlAktiveSpielerPosition.zeilePlusEins());
			this.sheetHelper.setFormulaInCell(sheet, posSpieltagWerte.zeilePlusEins(),
					"=" + aktivZelle + "+" + inAktivZelle + "+" + ausgestiegenZelle);
			// -----------------------------------
			String anzSpielerAddr = this.sheetHelper
					.getAddressFromColumnRow(getAnzahlAktiveSpielerPosition(spielTagNr));
			String formulaSverweisAnzDoublette = this.supermeleeTeamPaarungen
					.formulaSverweisAnzDoublette(anzSpielerAddr);
			this.sheetHelper.setFormulaInCell(sheet, posSpieltagWerte.zeilePlusEins(), formulaSverweisAnzDoublette);

			String formulaSverweisAnzTriplette = this.supermeleeTeamPaarungen
					.formulaSverweisAnzTriplette(anzSpielerAddr);
			this.sheetHelper.setFormulaInCell(sheet, posSpieltagWerte.zeilePlusEins(), formulaSverweisAnzTriplette);

			String formulaSverweisNurDoublette = this.supermeleeTeamPaarungen
					.formulaSverweisNurDoublette(anzSpielerAddr);
			this.sheetHelper.setFormulaInCell(sheet, posSpieltagWerte.zeile(SUMMEN_KANN_DOUBLETTE_ZEILE),
					formulaSverweisNurDoublette);
		}
	}

	// ---------------------------------------------
	public int getAnzahlAktiveSpieler(SpielTagNr Spieltag) throws GenerateException {
		return this.sheetHelper.getIntFromCell(getSheet(), getAnzahlAktiveSpielerPosition(Spieltag));
	}

	public Position getAnzahlAktiveSpielerPosition(SpielTagNr spieltag) throws GenerateException {
		return Position.from(ersteSummeSpalte() + spieltag.getNr(), SUMMEN_AKTIVE_ZEILE);
	}

	// ---------------------------------------------
	public int getAnzahlInAktiveSpieler(SpielTagNr spieltag) throws GenerateException {
		return this.sheetHelper.getIntFromCell(getSheet(), getAnzahlInAktiveSpielerPosition(spieltag));
	}

	public Position getAnzahlInAktiveSpielerPosition(SpielTagNr spieltag) throws GenerateException {
		return Position.from(ersteSummeSpalte() + spieltag.getNr(), SUMMEN_INAKTIVE_ZEILE);
	}

	// ---------------------------------------------
	public int getAusgestiegenSpieler(SpielTagNr spieltag) throws GenerateException {
		return this.sheetHelper.getIntFromCell(getSheet(), getAusgestiegenSpielerPosition(spieltag));
	}

	public Position getAusgestiegenSpielerPosition(SpielTagNr spieltag) throws GenerateException {
		return Position.from(ersteSummeSpalte() + spieltag.getNr(), SUMMEN_AUSGESTIEGENE_ZEILE);
	}
	// ---------------------------------------------

	public Boolean isKannNurDoublette(SpielTagNr Spieltag) throws GenerateException {
		return StringUtils
				.isNotBlank(this.sheetHelper.getTextFromCell(getSheet(), getKannNurDoublettePosition(Spieltag)));
	}

	public Position getKannNurDoublettePosition(SpielTagNr Spieltag) throws GenerateException {
		return Position.from(ersteSummeSpalte() + Spieltag.getNr(), SUMMEN_KANN_DOUBLETTE_ZEILE);
	}
	// ---------------------------------------------

	/**
	 *
	 * @param spieltag 1 = erste spieltag
	 * @param status = 1,2
	 * @return "=ZÄHLENWENN(D3:D102;1)"
	 * @throws GenerateException
	 */
	private String formulaCountSpieler(SpielTagNr spieltag, String status) throws GenerateException {
		int spieltagSpalte = spieltagSpalte(spieltag);
		int letzteZeile = this.spielerSpalte.letzteDatenZeile();

		if (letzteZeile < ERSTE_DATEN_ZEILE) {
			return "";
		}

		String ersteZelle = this.sheetHelper.getAddressFromColumnRow(Position.from(spieltagSpalte, ERSTE_DATEN_ZEILE));
		String letzteZelle = this.sheetHelper.getAddressFromColumnRow(Position.from(spieltagSpalte, letzteZeile));

		return "COUNTIF(" + ersteZelle + ":" + letzteZelle + ";" + status + ")";
	}

	public Meldungen getMeldungen(SpielTagNr spieltag, List<SpielrundeGespielt> spielrundeGespielt)
			throws GenerateException {
		checkNotNull(spieltag);
		Meldungen meldung = new Meldungen();
		int letzteZeile = this.spielerSpalte.letzteDatenZeile();

		if (letzteZeile >= ERSTE_DATEN_ZEILE) {
			// daten vorhanden
			int nichtZusammenSpielenSpalte = nichtZusammenSpielenSpalte();
			int spieltagSpalte = spieltagSpalte(spieltag);

			Position posSpieltag = Position.from(spieltagSpalte, ERSTE_DATEN_ZEILE);
			XSpreadsheet sheet = getSheet();

			for (int spielerZeile = ERSTE_DATEN_ZEILE; spielerZeile <= letzteZeile; spielerZeile++) {

				int isAktiv = this.sheetHelper.getIntFromCell(sheet, posSpieltag.zeile(spielerZeile));
				SpielrundeGespielt status = SpielrundeGespielt.findById(isAktiv);

				if (spielrundeGespielt.contains(status)) {
					int spielerNr = this.sheetHelper.getIntFromCell(sheet,
							Position.from(posSpieltag).spalte(SPIELER_NR_SPALTE));
					if (spielerNr > 0) {
						Spieler spieler = new Spieler(spielerNr);

						if (nichtZusammenSpielenSpalte > -1) {
							int nichtzusammen = this.sheetHelper.getIntFromCell(sheet,
									Position.from(posSpieltag).spalte(nichtZusammenSpielenSpalte));
							if (nichtzusammen == 1) {
								spieler.setSetzPos(1);
							}
						}
						meldung.addSpielerWennNichtVorhanden(spieler);
					}
				}
			}
		}
		return meldung;
	}

	@Override
	public int getSpielerZeileNr(int spielerNr) throws GenerateException {
		return this.spielerSpalte.getSpielerZeileNr(spielerNr);
	}

	@Override
	public List<String> getSpielerNamenList() throws GenerateException {
		doSort(this.getSpielerNameSpalte(), true); // nach namen sortieren
		return this.spielerSpalte.getSpielerNamenList();
	}

	@Override
	public List<Integer> getSpielerNrList() throws GenerateException {
		doSort(this.getSpielerNameSpalte(), true); // nach namen sortieren
		return this.spielerSpalte.getSpielerNrList();
	}

	@Override
	public int neachsteFreieDatenZeile() throws GenerateException {
		return this.spielerSpalte.neachsteFreieDatenZeile();
	}

	@Override
	public void spielerEinfuegenWennNichtVorhanden(int spielerNr) throws GenerateException {
		this.spielerSpalte.spielerEinfuegenWennNichtVorhanden(spielerNr);
	}

	@Override
	public int letzteDatenZeile() throws GenerateException {
		return this.spielerSpalte.letzteDatenZeile();
	}

	@Override
	public int getErsteDatenZiele() {
		return this.spielerSpalte.getErsteDatenZiele();
	}

	protected KonfigurationSheet getKonfigurationSheet() {
		return this.konfigurationSheet;
	}

	public SpielTagNr getSpielTag() throws GenerateException {
		return this.spielTag;
	}

	public void setSpielTag(SpielTagNr spielTag) {
		this.spielTag = spielTag;
	}

	@Override
	public Meldungen getAktiveUndAusgesetztMeldungen() throws GenerateException {
		return getMeldungen(getSpielTag(), Arrays.asList(SpielrundeGespielt.JA, SpielrundeGespielt.AUSGESETZT));
	}

	@Override
	public Meldungen getAktiveMeldungen() throws GenerateException {
		return getMeldungen(getSpielTag(), Arrays.asList(SpielrundeGespielt.JA));
	}

	// @Override
	// public Meldungen getAktiveUndAusgesetztMeldungenAktuellenSpielTag() throws GenerateException {
	// return getMeldungen(this.getKonfigurationSheet().getAktiveSpieltag(),
	// Arrays.asList(SpielrundeGespielt.JA, SpielrundeGespielt.AUSGESETZT));
	// }
	//
	// public Meldungen getAktiveMeldungenAktuellenSpielTag() throws GenerateException {
	// return getMeldungen(this.getKonfigurationSheet().getAktiveSpieltag(), Arrays.asList(SpielrundeGespielt.JA));
	// }

}
