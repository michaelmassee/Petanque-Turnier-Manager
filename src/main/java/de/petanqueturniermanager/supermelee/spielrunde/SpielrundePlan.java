/**
 * Erstellung 04.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.spielrunde;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;

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
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeSheet;
import de.petanqueturniermanager.supermelee.meldeliste.AbstractSupermeleeMeldeListeSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;

/**
 * eine liste von SpielerNr + Team A/B + Bahnnummer
 *
 * @author Michael Massee
 *
 */
public class SpielrundePlan extends SuperMeleeSheet implements ISheet {
	private static final Logger LOGGER = LogManager.getLogger(SpielrundePlan.class);
	private static final String SHEET_COLOR = "b0f442";

	public static final int HEADER_ZEILE = 0; // Spieltag
	public static final int HEADER_ZEILE_2 = 1; // nr + Team + bahn
	public static final int ERSTE_DATEN_ZEILE = 2;
	public static final int SPIELER_NR_SPALTE = 0; // Spalte A=0
	public static final int SPIELER_NAME_OFFS_SPALTE = 1;
	public static final int SPIELER_BAHN_NR_OFFS_SPALTE = 2;
	public static final int SPIELER_TEAM_OFFS_SPALTE = 3;
	public static final int BLOCK_TRENNER_OFFS_SPALTE = 4;
	public static final int ANZ_SPALTEN_IN_BLOCK = 4; // 

	public static final String PREFIX_SHEET_NAMEN = "SpielrundePlan";

	private final SpielrundeSheet_Update aktuelleSpielrundeSheet;
	private final AbstractSupermeleeMeldeListeSheet meldeliste;

	/**
	 * @param workingSpreadsheet
	 */
	public SpielrundePlan(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, "Spielrundeplan");
		aktuelleSpielrundeSheet = new SpielrundeSheet_Update(workingSpreadsheet);
		meldeliste = new MeldeListeSheet_Update(workingSpreadsheet);
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

	public String getSpielrundeSheetName(SpielTagNr spieltag, SpielRundeNr spielrunde) {
		return spieltag.getNr() + "." + spielrunde.getNr() + ". " + AbstractSpielrundeSheet.PREFIX_SHEET_NAMEN;
	}

	@Override
	public Logger getLogger() {
		return LOGGER;
	}

	@Override
	protected void doRun() throws GenerateException {
		generate();
	}

	public void generate() throws GenerateException {

		AbstractSupermeleeMeldeListeSheet meldeListe = new MeldeListeSheet_Update(getWorkingSpreadsheet());
		meldeListe.setSpielTag(getKonfigurationSheet().getAktiveSpieltag());
		SpielerMeldungen meldungen = meldeListe.getAktiveMeldungen();

		// Spielrunde sheet ?
		processBoxinfo("Neuer Spielrundeplan " + getSpielRundeNr().getNr() + " für Spieltag " + getSpielTag().getNr());

		if (!NewSheet.from(this, getSheetName(getSpielTag(), getSpielRundeNr())).pos(DefaultSheetPos.SUPERMELEE_WORK)
				.spielTagPageStyle(getSpielTag()).setForceCreate(true).setActiv().tabColor(SHEET_COLOR).create()
				.isDidCreate()) {
			ProcessBox.from().info("Abbruch vom Benutzer, Spielrundeplan wurde nicht erstellt");
			return;
		}

		NumberCellValue spierNrVal = NumberCellValue
				.from(getXSpreadSheet(), Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE))
				.setBorder(BorderFactory.from().allThin().toBorder())
				.setCharColor(ColorHelper.CHAR_COLOR_GRAY_SPIELER_NR).setCharHeight(10).centerJustify();

		// Team A/B und Bahn Nr
		StringCellValue strVal = StringCellValue.from(spierNrVal).removeCharColor().setCharHeight(12).centerJustify();

		int maxAnzSpielerInSpalte = 0;
		int spielerCntr = 1;

		StringCellValue nameFormula = StringCellValue
				.from(getXSpreadSheet(), Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE))
				.setBorder(BorderFactory.from().allThin().toBorder()).setShrinkToFit(true).setCharHeight(12);

		spalteFormat(spierNrVal.getPos());
		blockHeader(spierNrVal.getPos());
		boolean insertTrenner = false;

		Integer maxAnzSpielerInSpalteFromKonfig = getKonfigurationSheet().getMaxAnzSpielerInSpalte();

		for (Spieler spieler : meldungen.getSpielerList()) {

			if (insertTrenner) {
				spierNrVal.spalte((spielerCntr / maxAnzSpielerInSpalteFromKonfig) * (ANZ_SPALTEN_IN_BLOCK + 1))
						.zeile(ERSTE_DATEN_ZEILE);
				spalteFormat(spierNrVal.getPos());
				blockHeader(spierNrVal.getPos());
				insertTrenner = false;

			}

			int spielerNrSpalte = spierNrVal.getPos().getSpalte();
			spierNrVal.setValue((double) spieler.getNr());
			getSheetHelper().setNumberValueInCell(spierNrVal);

			// spieler name
			nameFormula.setValue(meldeliste.formulaSverweisSpielernamen(spierNrVal.getPos().getAddress()));
			nameFormula.spalte(spielerNrSpalte).spaltePlus(SPIELER_NAME_OFFS_SPALTE);
			nameFormula.zeile(spierNrVal.getPos().getZeile());
			getSheetHelper().setFormulaInCell(nameFormula);

			strVal.spalte(spielerNrSpalte).spaltePlus(SPIELER_TEAM_OFFS_SPALTE).zeile(spierNrVal.getPos().getZeile());
			getSheetHelper().setFormulaInCell(strVal.setValue(sVerweisSpielrundeSpalte(spierNrVal.getPos(),
					AbstractSpielrundeSheet.SPALTE_VERTIKALE_ERGEBNISSE_AB, 4)));

			getSheetHelper().setFormulaInCell(strVal.spalte(spielerNrSpalte).spaltePlus(SPIELER_BAHN_NR_OFFS_SPALTE)
					.setValue(sVerweisSpielrundeSpalte(spierNrVal.getPos(),
							AbstractSpielrundeSheet.SPALTE_VERTIKALE_ERGEBNISSE_BA_NR, 5)));

			spierNrVal.zeilePlusEins();

			// Block Trenner
			if ((spielerCntr / maxAnzSpielerInSpalteFromKonfig) * maxAnzSpielerInSpalteFromKonfig == spielerCntr) {
				insertTrenner = true;
			}

			spielerCntr++;
			if (maxAnzSpielerInSpalte < maxAnzSpielerInSpalteFromKonfig) {
				maxAnzSpielerInSpalte++;
			}
		}

		Position rechtsUnten = Position.from(spierNrVal.getPos().getSpalte() + 3,
				ERSTE_DATEN_ZEILE + (maxAnzSpielerInSpalte - 1));
		// Header Spielrunde
		StringCellValue spielrunde = StringCellValue.from(getXSpreadSheet(), SPIELER_NR_SPALTE, HEADER_ZEILE)
				.setEndPosMergeSpalte(rechtsUnten.getSpalte()).setValue("Spielrunde " + getSpielRundeNr().getNr())
				.centerHoriJustify().centerVertJustify().setCharHeight(14);
		getSheetHelper().setStringValueInCell(spielrunde);
		printBereichDefinieren(rechtsUnten);
	}

	private void printBereichDefinieren(Position rechtsUnten) throws GenerateException {
		processBoxinfo("Print-Bereich");
		Position linksOben = Position.from(SPIELER_NR_SPALTE, HEADER_ZEILE);
		PrintArea.from(getXSpreadSheet(), getWorkingSpreadsheet())
				.setPrintArea(RangePosition.from(linksOben, rechtsUnten));
	}

	private void blockHeader(final Position ersteSpielerNr) throws GenerateException {
		Position ersteHeader = Position.from(ersteSpielerNr).zeile(HEADER_ZEILE_2);
		int spielerSpalte = ersteSpielerNr.getSpalte();
		StringCellValue header = StringCellValue.from(getXSpreadSheet(), ersteHeader).setCharWeight(FontWeight.BOLD)
				.setCharHeight(8);
		getSheetHelper().setStringValueInCell(header.setValue("Nr."));
		getSheetHelper().setStringValueInCell(
				header.spalte(spielerSpalte).spaltePlus(SPIELER_NAME_OFFS_SPALTE).setValue("Name"));
		getSheetHelper().setStringValueInCell(
				header.spalte(spielerSpalte).spaltePlus(SPIELER_TEAM_OFFS_SPALTE).setValue("Team"));
		getSheetHelper().setStringValueInCell(
				header.spalte(spielerSpalte).spaltePlus(SPIELER_BAHN_NR_OFFS_SPALTE).setValue("Bahn"));
	}

	private String sVerweisSpielrundeSpalte(Position spielerNr, int letzteSpalte, int idx) throws GenerateException {
		String spielRundeSheetName = "$'" + aktuelleSpielrundeSheet.getSheetName(getSpielTag(), getSpielRundeNr())
				+ "'.";
		String verweisAufSpalteSpielerNr = "INDIRECT(ADDRESS(ROW();" + (spielerNr.getSpalte() + 1) + ";4))";

		int ersteSpalteVertikaleErgebnisse = AbstractSpielrundeSheet.ERSTE_SPALTE_VERTIKALE_ERGEBNISSE;
		int spielrundeSheetErsteDatenzeile = AbstractSpielrundeSheet.ERSTE_DATEN_ZEILE;
		Position erstePos = Position.from(ersteSpalteVertikaleErgebnisse, spielrundeSheetErsteDatenzeile);
		Position letztePos = Position.from(letzteSpalte, 1000 + spielrundeSheetErsteDatenzeile);
		String suchMatrixTeam = erstePos.getAddressWith$() + ":" + letztePos.getAddressWith$();
		String formula = "VLOOKUP(" + verweisAufSpalteSpielerNr + ";" + spielRundeSheetName + suchMatrixTeam + ";" + idx
				+ ";0)";
		return formula;
	}

	private void spalteFormat(Position spielrNr) throws GenerateException {
		ColumnProperties celPropNr = ColumnProperties.from().setHoriJustify(CellHoriJustify.CENTER).setWidth(900);
		getSheetHelper().setColumnProperties(getXSpreadSheet(), spielrNr.getSpalte(), celPropNr);
		getSheetHelper().setColumnProperties(getXSpreadSheet(), spielrNr.getSpalte() + SPIELER_TEAM_OFFS_SPALTE,
				celPropNr);
		getSheetHelper().setColumnProperties(getXSpreadSheet(), spielrNr.getSpalte() + SPIELER_BAHN_NR_OFFS_SPALTE,
				celPropNr);

		celPropNr.setWidth(900 * 5);
		getSheetHelper().setColumnProperties(getXSpreadSheet(), spielrNr.getSpalte() + SPIELER_NAME_OFFS_SPALTE,
				celPropNr);

	}

	public SpielTagNr getSpielTag() throws GenerateException {
		return getKonfigurationSheet().getAktiveSpieltag();
	}

	public SpielRundeNr getSpielRundeNr() throws GenerateException {
		return getKonfigurationSheet().getAktiveSpielRunde();
	}
}
