/**
* Erstellung : 22.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.meldeliste;

import static com.google.common.base.Preconditions.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.msgbox.ErrorMessageBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SpielerSpalte;
import de.petanqueturniermanager.konfiguration.DocumentPropertiesHelper;
import de.petanqueturniermanager.konfiguration.IPropertiesSpalte;
import de.petanqueturniermanager.konfiguration.PropertiesSpalte;
import de.petanqueturniermanager.model.Meldungen;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.supermelee.SupermeleeTeamPaarungenSheet;
import de.petanqueturniermanager.supermelee.endrangliste.EndranglisteSheet;

public class MeldeListeSheet extends Thread implements IMeldeliste, Runnable, ISheet, IPropertiesSpalte {
	private static final Logger logger = LogManager.getLogger(EndranglisteSheet.class);

	public static final int SPALTE_FORMATION = 0; // siehe enum #Formation Spalte 0
	public static final int ZEILE_FORMATION = 0; // Zeile 0

	public static final int ERSTE_DATEN_ZEILE = 2; // Zeile 3
	public static final int SPIELER_NR_SPALTE = 0; // Spalte A=0

	public static final int SUMMEN_SPALTE_OFFSET = 2; // 2 Spalten weiter zur letzte Spieltag
	public static final int SUMMEN_ERSTE_ZEILE = ERSTE_DATEN_ZEILE; // Zeile 3
	public static final int SUMMEN_AKTIVE_ZEILE = SUMMEN_ERSTE_ZEILE; // Zeile 6
	public static final int SUMMEN_INAKTIVE_ZEILE = SUMMEN_ERSTE_ZEILE + 1;
	public static final int SUMMEN_AUSGESTIEGENE_ZEILE = SUMMEN_ERSTE_ZEILE + 2; // Zeile 8
	public static final int SUMMEN_KANN_DOUBLETTE_ZEILE = SUMMEN_ERSTE_ZEILE + 7; // Zeile 10

	public static final int ERSTE_ZEILE_PROPERTIES = 12; // Zeile 13

	public static final String SHEETNAME = "Meldeliste";

	private final ErrorMessageBox errMsgBox;
	private final DocumentPropertiesHelper properties;
	private final SheetHelper sheetHelper;
	private final SpielerSpalte spielerSpalte;
	private final PropertiesSpalte propertiesSpalte;
	private final SupermeleeTeamPaarungenSheet supermeleeTeamPaarungen;
	private int anzSpieltage = 1;

	public MeldeListeSheet(XComponentContext xContext) {
		checkNotNull(xContext);
		this.properties = new DocumentPropertiesHelper(xContext);
		this.sheetHelper = new SheetHelper(xContext);
		this.spielerSpalte = new SpielerSpalte(xContext, ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE, this, this,
				getFormation());
		this.supermeleeTeamPaarungen = new SupermeleeTeamPaarungenSheet(xContext);
		this.propertiesSpalte = new PropertiesSpalte(xContext, ersteSummeSpalte(), ERSTE_ZEILE_PROPERTIES, this, this);
		this.errMsgBox = new ErrorMessageBox(xContext);
	}

	public Formation getFormation() {
		int formationId = this.properties.getIntProperty(DocumentPropertiesHelper.PROP_NAME_FORMATION);
		if (formationId > -1) {
			return Formation.findById(formationId);
		}
		return null;
	}

	@Override
	public XSpreadsheet getSheet() {
		return this.sheetHelper.newIfNotExist(SHEETNAME, (short) 0, "2544dd");
	}

	@Override
	public void run() {
		if (!SheetRunner.isRunning) {
			SheetRunner.isRunning = true;
			try {
				upDateSheet();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				SheetRunner.isRunning = false;
			}
		}
	}

	public void show() {
		this.sheetHelper.setActiveSheet(getSheet());
	}

	public void upDateSheet() {

		if (isErrorInSheet()) {
			return;
		}

		XSpreadsheet sheet = getSheet();
		this.sheetHelper.setActiveSheet(sheet);
		this.sheetHelper.setValInCell(sheet, SPALTE_FORMATION, ZEILE_FORMATION, getFormation().getId());
		this.sheetHelper.setTextInCell(sheet, SPALTE_FORMATION + 1, ZEILE_FORMATION, getFormation().getBezeichnung());
		// Header einfuegen
		// ------
		int hederBackColor = this.propertiesSpalte.getRanglisteHeaderFarbe();
		this.spielerSpalte.insertHeaderInSheet(hederBackColor);
		if (nichtZusammenSpielenSpalte() > -1) {

			StringCellValue bezCelVal = StringCellValue
					.from(sheet, nichtZusammenSpielenSpalte(), ERSTE_DATEN_ZEILE - 1, "SetzPos")
					.setSpalteHoriJustify(CellHoriJustify.CENTER)
					.setComment("1 = Setzposition, Diesen Spieler werden nicht zusammen im gleichen Team gelost.")
					.setSetColumnWidth(800).setCellBackColor(hederBackColor)
					.setBorder(BorderFactory.from().allThin().toBorder());
			this.sheetHelper.setTextInCell(bezCelVal);
		}

		StringCellValue bezCelSpieltagVal = StringCellValue
				.from(sheet, ersteSpieltagspalteSpalte(), ERSTE_DATEN_ZEILE - 1, spielTagHeader(1))
				.setSpalteHoriJustify(CellHoriJustify.CENTER).setComment("1 = Aktiv, 2 = Ausgestiegen, leer = InAktiv")
				.setSetColumnWidth(2000).setCellBackColor(hederBackColor)
				.setBorder(BorderFactory.from().allThin().toBorder());

		for (int spielTagCntr = 0; spielTagCntr < this.anzSpieltage; spielTagCntr++) {
			bezCelSpieltagVal.setValue(spielTagHeader(spielTagCntr + 1));
			this.sheetHelper.setTextInCell(bezCelSpieltagVal);
			bezCelSpieltagVal.zeilePlusEins();
		}

		// eventuelle luecken in spiele namen nach unten sortieren
		lueckenEntfernen();
		updateSpielerNr();

		doSort(this.spielerSpalte.getSpielerNameSpalte(), true); // nach namen sortieren
		updateSpieltageSummenSpalten();
		this.propertiesSpalte.updateKonfigBlock();

		this.spielerSpalte.formatDaten();
		this.formatDaten();
	}

	void formatDaten() {
		int letzteDatenZeile = this.spielerSpalte.letzteDatenZeile();
		RangePosition datenRange = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, letzteSpielTagSpalte(),
				letzteDatenZeile);

		this.sheetHelper.setPropertiesInRange(getSheet(), datenRange,
				CellProperties.from().setVertJustify(CellVertJustify2.CENTER)
						.setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder()));
	}

	/**
	 * @param spieltag = 1 bis x
	 * @return
	 */
	public String spielTagHeader(int spieltag) {
		if (this.anzSpieltage > 1) {
			return spieltag + ". Spieltag";
		}
		return "Spieltag";
	}

	/**
	 * diese Spalte nur beim supermelee
	 *
	 * @return -1 wenn nicht vorhanden
	 */
	public int nichtZusammenSpielenSpalte() {
		if (getFormation() == Formation.SUPERMELEE) {
			return 2; // Spalte C
		}
		return -1;
	}

	public int letzteSpielTagSpalte() {
		return ersteSpieltagspalteSpalte() + (this.anzSpieltage - 1);
	}

	public int ersteSpieltagspalteSpalte() {
		if (nichtZusammenSpielenSpalte() > -1) {
			return nichtZusammenSpielenSpalte() + 1;
		}
		return SPIELER_NR_SPALTE + this.spielerSpalte.anzahlSpielerNamenSpalten();
	}

	public int aktuelleSpieltagSpalte() {
		return spieltagSpalte(aktuelleSpieltag());
	}

	public int spieltagSpalte(int spieltag) {
		return ersteSpieltagspalteSpalte() + spieltag - 1;
	}

	public int aktuelleSpieltag() {
		return this.propertiesSpalte.getSpieltag();
	}

	public int ersteSummeSpalte() {
		return letzteSpielTagSpalte() + SUMMEN_SPALTE_OFFSET;
	}

	@Override
	public String formulaSverweisSpielernamen(String spielrNrAdresse) {
		String ersteZelleAddress = this.sheetHelper
				.getAddressFromColumnRow(Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE));
		String letzteZelleAddress = this.sheetHelper
				.getAddressFromColumnRow(Position.from(this.spielerSpalte.getSpielerNameSpalte(), 999));
		return "=VLOOKUP(" + spielrNrAdresse + ";$'" + SHEETNAME + "'.$" + ersteZelleAddress + ":$" + letzteZelleAddress
				+ ";2;0)";
	}

	public void lueckenEntfernen() {
		doSort(this.spielerSpalte.getSpielerNameSpalte(), true); // alle zeilen ohne namen nach unten sortieren, egal ob
		// daten oder nicht
		int letzteNrZeile = this.spielerSpalte.neachsteFreieDatenZeile();
		if (letzteNrZeile <= ERSTE_DATEN_ZEILE) { // daten vorhanden ?
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

	public boolean isErrorInSheet() {
		XSpreadsheet xSheet = getSheet();

		int letzteSpielZeile = this.spielerSpalte.letzteZeileMitSpielerName();
		if (letzteSpielZeile <= ERSTE_DATEN_ZEILE) { // daten vorhanden ?
			return false; // keine Daten
		}

		// doppelte spieler Nummer entfernen !?!?!
		HashSet<Integer> spielrNrInSheet = new HashSet<>();
		int spielrNr;
		for (int spielerZeilecntr = ERSTE_DATEN_ZEILE; spielerZeilecntr <= letzteSpielZeile; spielerZeilecntr++) {
			spielrNr = this.sheetHelper.getIntFromCell(xSheet, Position.from(SPIELER_NR_SPALTE, spielerZeilecntr));
			if (spielrNrInSheet.contains(spielrNr)) {
				this.errMsgBox.showOk("Fehler", "Meldeliste wurde nicht Aktualisiert.\r\nSpieler Nr. " + spielrNr
						+ " ist doppelt in der Meldliste !!!");
				return true;
			} else {
				spielrNrInSheet.add(spielrNr);
			}
		}
		return false;
	}

	public void updateSpielerNr() {
		int letzteSpielZeile = this.spielerSpalte.letzteZeileMitSpielerName();
		if (letzteSpielZeile <= ERSTE_DATEN_ZEILE) { // daten vorhanden ?
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
		for (int spielerZeilecntr = ERSTE_DATEN_ZEILE; spielerZeilecntr <= letzteSpielZeile; spielerZeilecntr++) {
			spielrNr = this.sheetHelper.getIntFromCell(xSheet, Position.from(SPIELER_NR_SPALTE, spielerZeilecntr));
			if (spielrNr == -1) {
				this.sheetHelper.setValInCell(xSheet, SPIELER_NR_SPALTE, spielerZeilecntr, ++letzteSpielerNr);
			}
		}
	}

	/**
	 * alle sortierbare daten, ohne header !
	 *
	 * @return
	 */
	private XCellRange getxCellRangeAlleDaten() {
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
			logger.error(e.getMessage(), e);
			return null;
		}
		return xCellRange;
	}

	public void doSort(int spalteNr, boolean isAscending) {

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

	public void updateSpieltageSummenSpalten() {

		if (this.spielerSpalte.letzteDatenZeile() < ERSTE_DATEN_ZEILE) {
			return; // keine daten
		}

		XSpreadsheet sheet = getSheet();

		Position posBezeichnug = Position.from(ersteSummeSpalte(), SUMMEN_ERSTE_ZEILE);

		StringCellValue bezCelVal = StringCellValue.from(sheet, posBezeichnug, "Aktiv")
				.setSpalteHoriJustify(CellHoriJustify.RIGHT).setComment("Spieler mit \"1\" im Spieltag")
				.zeile(SUMMEN_AKTIVE_ZEILE);
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

		for (int spieltagCntr = 1; spieltagCntr <= this.anzSpieltage; spieltagCntr++) {

			Position posSpieltagWerte = Position.from(ersteSummeSpalte() + spieltagCntr, SUMMEN_ERSTE_ZEILE - 1);

			// Header
			this.sheetHelper.setColumnWidthAndHoriJustifyCenter(sheet, posSpieltagWerte, 1000, "Tag " + spieltagCntr);

			// Summe Aktive Spieler "=ZÄHLENWENN(D3:D102;1)"
			this.sheetHelper.setFormulaInCell(sheet, posSpieltagWerte.zeile(SUMMEN_AKTIVE_ZEILE),
					"=" + formulaCountSpieler(spieltagCntr, "1"));

			// Summe inAktive Spieler "=ZÄHLENWENN(D3:D102;0) + ZÄHLENWENN(D3:D102;"")"
			this.sheetHelper.setFormulaInCell(sheet, posSpieltagWerte.zeile(SUMMEN_INAKTIVE_ZEILE),
					"=" + formulaCountSpieler(spieltagCntr, "0") + " + " + formulaCountSpieler(spieltagCntr, "\"\""));

			// Ausgestiegen =ZÄHLENWENN(D3:D102;2)
			this.sheetHelper.setFormulaInCell(sheet, posSpieltagWerte.zeile(SUMMEN_AUSGESTIEGENE_ZEILE),
					"=" + formulaCountSpieler(spieltagCntr, "2"));
			// -----------------------------------
			// Aktiv + Ausgestiegen
			Position anzahlAktiveSpielerPosition = getAnzahlAktiveSpielerPosition(spieltagCntr);
			String aktivZelle = this.sheetHelper.getAddressFromColumnRow(anzahlAktiveSpielerPosition);
			String ausgestiegenZelle = this.sheetHelper
					.getAddressFromColumnRow(getAusgestiegenSpielerPosition(spieltagCntr));
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
					.getAddressFromColumnRow(getAnzahlAktiveSpielerPosition(spieltagCntr));
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
	public int getAnzahlAktiveSpieler(int Spieltag) {
		return this.sheetHelper.getIntFromCell(getSheet(), getAnzahlAktiveSpielerPosition(Spieltag));
	}

	public Position getAnzahlAktiveSpielerPosition(int Spieltag) {
		return Position.from(ersteSummeSpalte() + Spieltag, SUMMEN_AKTIVE_ZEILE);
	}

	// ---------------------------------------------
	public int getAnzahlInAktiveSpieler(int Spieltag) {
		return this.sheetHelper.getIntFromCell(getSheet(), getAnzahlInAktiveSpielerPosition(Spieltag));
	}

	public Position getAnzahlInAktiveSpielerPosition(int Spieltag) {
		return Position.from(ersteSummeSpalte() + Spieltag, SUMMEN_INAKTIVE_ZEILE);
	}

	// ---------------------------------------------
	public int getAusgestiegenSpieler(int Spieltag) {
		return this.sheetHelper.getIntFromCell(getSheet(), getAusgestiegenSpielerPosition(Spieltag));
	}

	public Position getAusgestiegenSpielerPosition(int Spieltag) {
		return Position.from(ersteSummeSpalte() + Spieltag, SUMMEN_AUSGESTIEGENE_ZEILE);
	}
	// ---------------------------------------------

	public Boolean isKannNurDoublette(int Spieltag) {
		return StringUtils
				.isNotBlank(this.sheetHelper.getTextFromCell(getSheet(), getKannNurDoublettePosition(Spieltag)));
	}

	public Position getKannNurDoublettePosition(int Spieltag) {
		return Position.from(ersteSummeSpalte() + Spieltag, SUMMEN_KANN_DOUBLETTE_ZEILE);
	}
	// ---------------------------------------------

	/**
	 *
	 * @param spieltag 1 = erste spieltag
	 * @param status = 1,2
	 * @return "=ZÄHLENWENN(D3:D102;1)"
	 */
	private String formulaCountSpieler(int spieltag, String status) {
		int spieltagSpalte = spieltagSpalte(spieltag);
		int letzteZeile = this.spielerSpalte.letzteDatenZeile();

		if (letzteZeile < ERSTE_DATEN_ZEILE) {
			return "";
		}

		String ersteZelle = this.sheetHelper.getAddressFromColumnRow(Position.from(spieltagSpalte, ERSTE_DATEN_ZEILE));
		String letzteZelle = this.sheetHelper.getAddressFromColumnRow(Position.from(spieltagSpalte, letzteZeile));

		return "COUNTIF(" + ersteZelle + ":" + letzteZelle + ";" + status + ")";
	}

	@Override
	public Meldungen getAktiveUndAusgesetztMeldungenAktuellenSpielTag() {
		return getMeldungen(aktuelleSpieltag(), Arrays.asList(SpielrundeGespielt.JA, SpielrundeGespielt.AUSGESETZT));
	}

	public Meldungen getAktiveMeldungenAktuellenSpielTag() {
		return getMeldungen(aktuelleSpieltag(), Arrays.asList(SpielrundeGespielt.JA));
	}

	public Meldungen getMeldungen(int spieltag, List<SpielrundeGespielt> spielrundeGespielt) {
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
	public int getSpieltag() {
		return this.propertiesSpalte.getSpieltag();
	}

	@Override
	public int getSpielRunde() {
		return this.propertiesSpalte.getSpielRunde();
	}

	@Override
	public void setSpielRunde(int neueSpielrunde) {
		this.propertiesSpalte.setSpielRunde(neueSpielrunde);
	}

	@Override
	public Integer getSpielRundeHintergrundFarbeGerade() {
		return this.propertiesSpalte.getSpielRundeHintergrundFarbeGerade();
	}

	@Override
	public Integer getSpielRundeHintergrundFarbeUnGerade() {
		return this.propertiesSpalte.getSpielRundeHintergrundFarbeUnGerade();
	}

	@Override
	public Integer getSpielRundeHeaderFarbe() {
		return this.propertiesSpalte.getSpielRundeHeaderFarbe();
	}

	@Override
	public Integer getSpielRundeNeuAuslosenAb() {
		return this.propertiesSpalte.getSpielRundeNeuAuslosenAb();
	}

	@Override
	public int getSpielerZeileNr(int spielerNr) {
		return this.spielerSpalte.getSpielerZeileNr(spielerNr);
	}

	@Override
	public Integer getRanglisteHintergrundFarbeGerade() {
		return this.propertiesSpalte.getRanglisteHintergrundFarbeGerade();
	}

	@Override
	public Integer getRanglisteHintergrundFarbeUnGerade() {
		return this.propertiesSpalte.getRanglisteHintergrundFarbeUnGerade();
	}

	@Override
	public Integer getRanglisteHeaderFarbe() {
		return this.propertiesSpalte.getRanglisteHeaderFarbe();

	}
}
