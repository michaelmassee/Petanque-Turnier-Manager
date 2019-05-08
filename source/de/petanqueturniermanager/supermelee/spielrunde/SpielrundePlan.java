/**
 * Erstellung 04.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.spielrunde;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.konfiguration.KonfigurationSheet;
import de.petanqueturniermanager.model.Meldungen;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.meldeliste.AbstractSupermeleeMeldeListeSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;

/**
 * eine liste von SpielerNr + Team A/B + Bahnnummer
 * 
 * @author Michael Massee
 *
 */
public class SpielrundePlan extends SheetRunner implements ISheet {
	private static final Logger LOGGER = LogManager.getLogger(SpielrundePlan.class);
	private static final String SHEET_COLOR = "b0f442";

	public static final int HEADER_ZEILE = 0; // Spieltag
	public static final int HEADER_ZEILE_2 = 1; // nr + Team + bahn
	public static final int ERSTE_DATEN_ZEILE = 2;
	public static final int SPIELER_NR_SPALTE = 0; // Spalte A=0
	public static final int SPIELER_TEAM_SPALTE = 1; // Spalte A=0
	public static final int SPIELER_BAHN_NR_SPALTE = 2; // Spalte A=0
	public static final int SPIELER_NAME_SPALTE = 3; // Spalte A=0
	public static final int MAX_ANZSPIELER_IN_SPALTE = 30;

	public static final String PREFIX_SHEET_NAMEN = "SpielrundePlan";
	private SpielTagNr spielTag = null;
	private SpielRundeNr spielRundeNr = null;

	private final KonfigurationSheet konfigurationSheet;
	private final SpielrundeSheet_Update aktuelleSpielrundeSheet;

	/**
	 * @param workingSpreadsheet
	 */
	public SpielrundePlan(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, "Spielrundeplan");
		konfigurationSheet = newKonfigurationSheet(workingSpreadsheet);
		aktuelleSpielrundeSheet = new SpielrundeSheet_Update(workingSpreadsheet);
	}

	@VisibleForTesting
	KonfigurationSheet newKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new KonfigurationSheet(workingSpreadsheet);
	}

	@Override
	public XSpreadsheet getSheet() throws GenerateException {
		return getSheetHelper().findByName(getSheetName(getSpielTag(), getSpielRundeNr()));
	}

	public String getSheetName(SpielTagNr spieltag, SpielRundeNr spielrunde) throws GenerateException {
		return spieltag.getNr() + "." + spielrunde.getNr() + ". " + PREFIX_SHEET_NAMEN;
	}

	public String getSpielrundeSheetName(SpielTagNr spieltag, SpielRundeNr spielrunde) throws GenerateException {
		return spieltag.getNr() + "." + spielrunde.getNr() + ". " + AbstractSpielrundeSheet.PREFIX_SHEET_NAMEN;
	}

	@Override
	public Logger getLogger() {
		return LOGGER;
	}

	@Override
	protected void doRun() throws GenerateException {
		AbstractSupermeleeMeldeListeSheet meldeListe = new MeldeListeSheet_Update(getWorkingSpreadsheet());
		generate(meldeListe.getAktiveMeldungen());
	}

	public void generate(Meldungen meldungen) throws GenerateException {
		setSpielTag(konfigurationSheet.getAktiveSpieltag());
		setSpielRundeNr(konfigurationSheet.getAktiveSpielRunde());

		// Spielrunde sheet ?
		processBoxinfo("Neuer Spielrundeplan " + getSpielRundeNr().getNr() + " für Spieltag " + getSpielTag().getNr());

		if (!NewSheet.from(getWorkingSpreadsheet(), getSheetName(getSpielTag(), getSpielRundeNr())).pos(DefaultSheetPos.SUPERMELEE_WORK).spielTagPageStyle(getSpielTag())
				.setForceCreate(true).setActiv().tabColor(SHEET_COLOR).create().isDidCreate()) {
			ProcessBox.from().info("Abbruch vom Benutzer, Spielrundeplan wurde nicht erstellt");
			return;
		}

		NumberCellValue spierNrVal = NumberCellValue.from(getSheet(), Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE)).setBorder(BorderFactory.from().allThin().toBorder())
				.setCharHeight(14);

		// meldeliste nach nr sortieren !
		meldungen.sortNachNummer();
		int maxAnzSpielerInSpalte = 0;
		int spielerCntr = 1;

		spalteFormat(spierNrVal);
		blockHeader(spierNrVal.getPos());
		for (Spieler spieler : meldungen.getSpielerList()) {
			spierNrVal.setValue((double) spieler.getNr());
			getSheetHelper().setValInCell(spierNrVal);

			StringCellValue strVal = StringCellValue.from(spierNrVal).spaltePlusEins();
			getSheetHelper().setFormulaInCell(strVal.setValue(sVerweisSpielrundeSpalte(spierNrVal.getPos(), AbstractSpielrundeSheet.SPALTE_VERTIKALE_ERGEBNISSE_AB, 4)));
			getSheetHelper().setFormulaInCell(
					strVal.spaltePlusEins().setValue(sVerweisSpielrundeSpalte(spierNrVal.getPos(), AbstractSpielrundeSheet.SPALTE_VERTIKALE_ERGEBNISSE_BA_NR, 5)));

			spierNrVal.zeilePlusEins();

			if ((spielerCntr / MAX_ANZSPIELER_IN_SPALTE) * MAX_ANZSPIELER_IN_SPALTE == spielerCntr) {
				// naechste Block
				spierNrVal.spalte((spielerCntr / MAX_ANZSPIELER_IN_SPALTE) * 4).zeile(ERSTE_DATEN_ZEILE);
				spalteFormat(spierNrVal);
				blockHeader(spierNrVal.getPos());
			}
			spielerCntr++;
			if (maxAnzSpielerInSpalte < MAX_ANZSPIELER_IN_SPALTE) {
				maxAnzSpielerInSpalte++;
			}
		}

		Position rechtsUnten = Position.from(spierNrVal.getPos().getSpalte() + 2, ERSTE_DATEN_ZEILE + (maxAnzSpielerInSpalte - 1));
		// Header Spielrunde
		// CellProperties headerProp = CellProperties.from().setHoriJustify(CellHoriJustify.CENTER).setHeight(90);
		StringCellValue spielrunde = StringCellValue.from(getSheet(), SPIELER_NR_SPALTE, HEADER_ZEILE).setEndPosMergeSpalte(rechtsUnten.getSpalte())
				.setValue("Spielrunde " + getSpielRundeNr().getNr()).centerHoriJustify().centerVertJustify().setCharHeight(14);
		getSheetHelper().setTextInCell(spielrunde);
		printBereichDefinieren(rechtsUnten);
	}

	private void printBereichDefinieren(Position rechtsUnten) throws GenerateException {
		processBoxinfo("Print-Bereich");
		Position linksOben = Position.from(SPIELER_NR_SPALTE, HEADER_ZEILE);
		PrintArea.from(getSheet()).setPrintArea(RangePosition.from(linksOben, rechtsUnten));
	}

	private void blockHeader(final Position ersteSpielerNr) throws GenerateException {
		Position ersteHeader = Position.from(ersteSpielerNr).zeile(HEADER_ZEILE_2);
		StringCellValue header = StringCellValue.from(getSheet(), ersteHeader).setCharWeight(FontWeight.BOLD).setCharHeight(8);
		getSheetHelper().setTextInCell(header.setValue("Nr."));
		getSheetHelper().setTextInCell(header.spaltePlusEins().setValue("Team"));
		getSheetHelper().setTextInCell(header.spaltePlusEins().setValue("Bahn"));
	}

	private String sVerweisSpielrundeSpalte(Position spielerNr, int letzteSpalte, int idx) throws GenerateException {
		String spielRundeSheetName = "$'" + aktuelleSpielrundeSheet.getSheetName(spielTag, spielRundeNr) + "'.";
		String verweisAufSpalteSpielerNr = "INDIRECT(ADDRESS(ROW();" + (spielerNr.getSpalte() + 1) + ";8))";

		int ersteSpalteVertikaleErgebnisse = AbstractSpielrundeSheet.ERSTE_SPALTE_VERTIKALE_ERGEBNISSE;
		int spielrundeSheetErsteDatenzeile = AbstractSpielrundeSheet.ERSTE_DATEN_ZEILE;
		Position erstePos = Position.from(ersteSpalteVertikaleErgebnisse, spielrundeSheetErsteDatenzeile);
		Position letztePos = Position.from(letzteSpalte, 1000 + spielrundeSheetErsteDatenzeile);
		String suchMatrixTeam = erstePos.getAddressWith$() + ":" + letztePos.getAddressWith$();
		String formula = "VLOOKUP(" + verweisAufSpalteSpielerNr + ";" + spielRundeSheetName + suchMatrixTeam + ";" + idx + ";0)";
		return formula;
	}

	private void spalteFormat(NumberCellValue nrVal) throws GenerateException {
		CellProperties celPropNr = CellProperties.from().setHoriJustify(CellHoriJustify.CENTER).setWidth(900);
		for (int i = 0; i < 3; i++) {
			getSheetHelper().setColumnProperties(getSheet(), nrVal.getPos().getSpalte() + i, celPropNr);
		}
	}

	public SpielTagNr getSpielTag() throws GenerateException {
		checkNotNull(spielTag);
		return spielTag;
	}

	public void setSpielTag(SpielTagNr spielTag) throws GenerateException {
		checkNotNull(spielTag);
		ProcessBox.from().spielTag(spielTag);
		this.spielTag = spielTag;
	}

	public SpielRundeNr getSpielRundeNr() throws GenerateException {
		checkNotNull(spielRundeNr);
		return spielRundeNr;
	}

	public void setSpielRundeNr(SpielRundeNr spielrunde) throws GenerateException {
		checkNotNull(spielrunde);
		ProcessBox.from().spielRunde(spielrunde);
		spielRundeNr = spielrunde;
	}
}
