package de.petanqueturniermanager.schweizer.spielrunde;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.TABLE_BORDER2;

import java.util.List;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.SchweizerSystem;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeHelper;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.model.TeamPaarung;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerSheet;
import de.petanqueturniermanager.schweizer.meldeliste.AbstractSchweizerMeldeListeSheet;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

/**
 * Erstellung 27.03.2024 / Michael Massee
 */

public abstract class SchweizerAbstractSpielrundeSheet extends SchweizerSheet implements ISheet {

	private static final Logger LOGGER = LogManager.getLogger(SchweizerAbstractSpielrundeSheet.class);

	public static final String SHEET_COLOR = "b0f442";
	public static final String SHEET_NAMEN = "Spielrunde";

	public static final int ERSTE_HEADER_ZEILE = 0;
	public static final int ZWEITE_HEADER_ZEILE = ERSTE_HEADER_ZEILE + 1;
	public static final int ERSTE_DATEN_ZEILE = ZWEITE_HEADER_ZEILE + 1;

	public static final int NR_CHARHEIGHT = 18;
	public static final int BAHN_NR_SPALTE = 0;
	public static final int TEAM_A_SPALTE = BAHN_NR_SPALTE + 1;
	public static final int TEAM_B_SPALTE = TEAM_A_SPALTE + 1;
	public static final int ERG_TEAM_A_SPALTE = TEAM_B_SPALTE + 1;
	public static final int ERG_TEAM_B_SPALTE = ERG_TEAM_A_SPALTE + 1;

	private final AbstractSchweizerMeldeListeSheet meldeListe;
	private final SpielrundeHelper spielrundeHelper;
	private SpielRundeNr spielRundeNrInSheet = null;
	private boolean forceOk = false; // wird fuer Test verwendet

	//	private SpielRundeNr sheetSpielRundeNr = null; // muss nicht der Aktive sein

	protected SchweizerAbstractSpielrundeSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, SHEET_NAMEN);
		meldeListe = initMeldeListeSheet(workingSpreadsheet);
		spielrundeHelper = new SpielrundeHelper(this, NR_CHARHEIGHT, NR_CHARHEIGHT, true,
				getKonfigurationSheet().getSpielRundeHintergrundFarbeGeradeStyle(),
				getKonfigurationSheet().getSpielRundeHintergrundFarbeUnGeradeStyle());
	}

	protected final boolean canStart(TeamMeldungen meldungen) throws GenerateException {
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
					+ "\r\nmindestens 6 Meldungen aktivieren.";
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK).caption("Aktuelle Spielrunde Fehler")
					.message(errorMsg).show();
			return false;
		}
		return true;
	}

	@VisibleForTesting
	AbstractSchweizerMeldeListeSheet initMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new SchweizerMeldeListeSheetUpdate(workingSpreadsheet);
	}

	@Override
	public final XSpreadsheet getXSpreadSheet() throws GenerateException {
		return getSheetHelper().findByName(getSheetName(getSpielRundeNr()));
	}

	public final String getSheetName(SpielRundeNr nr) {
		return nr.getNr() + ". " + SHEET_NAMEN;
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	public final SpielRundeNr getSpielRundeNr() throws GenerateException {
		return getKonfigurationSheet().getAktiveSpielRunde();
	}

	public SpielRundeNr getSpielRundeNrInSheet() {
		return spielRundeNrInSheet;
	}

	public void setSpielRundeNrInSheet(SpielRundeNr spielRundeNrInSheet) {
		this.spielRundeNrInSheet = spielRundeNrInSheet;
	}

	public AbstractSchweizerMeldeListeSheet getMeldeListe() {
		return meldeListe;
	}

	/**
	 * @param aktiveMeldungen liste der Aktive Meldungen
	 * @param spielTagNr
	 * @param abSpielrunde
	 * @param bisSpielrunde
	 * @throws GenerateException
	 */

	protected void gespieltenRundenEinlesen(TeamMeldungen aktiveMeldungen, int abSpielrunde, int bisSpielrunde)
			throws GenerateException {
		int spielrunde = 1;

		if (bisSpielrunde < abSpielrunde || bisSpielrunde < 1) {
			return;
		}

		if (abSpielrunde > 1) {
			spielrunde = abSpielrunde;
		}

		processBoxinfo(
				"Meldungen von gespielten Runden einlesen. Von Runde:" + spielrunde + " Bis Runde:" + bisSpielrunde);

		for (; spielrunde <= bisSpielrunde; spielrunde++) {
			SheetRunner.testDoCancelTask();

			XSpreadsheet sheet = getSheetHelper().findByName(getSheetName(SpielRundeNr.from(spielrunde)));

			if (sheet == null) {
				continue;
			}

			// TODO

		}
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
		Integer headerColor = getKonfigurationSheet().getSpielRundeHeaderFarbe();
		Integer letzteZeile = letztePositionRechtsUnten().getZeile();
		SpielrundeSpielbahn spielrundeSpielbahn = getKonfigurationSheet().getSpielrundeSpielbahn();

		spielrundeHelper.datenErsteSpalte(spielrundeSpielbahn, ERSTE_DATEN_ZEILE, letzteZeile, BAHN_NR_SPALTE,
				ERSTE_HEADER_ZEILE, ZWEITE_HEADER_ZEILE, headerColor);
	}

	private void header() throws GenerateException {
		processBoxinfo("Header Formatieren");
		Integer headerColor = getKonfigurationSheet().getSpielRundeHeaderFarbe();

		Position headerStart = Position.from(TEAM_A_SPALTE, ERSTE_HEADER_ZEILE);

		StringCellValue headerValue = StringCellValue.from(getXSpreadSheet(), headerStart)
				.setVertJustify(CellVertJustify2.CENTER).setHoriJustify(CellHoriJustify.CENTER)
				.setBorder(BorderFactory.from().allThin().toBorder()).setCellBackColor(headerColor)
				.setCharHeight(NR_CHARHEIGHT).setEndPosMergeSpaltePlus(4)
				.setValue("Spielrunde " + getSpielRundeNr().getNr());
		getSheetHelper().setStringValueInCell(headerValue);

		StringCellValue headerValueZeile2 = StringCellValue
				.from(getXSpreadSheet(), headerStart.zeile(ZWEITE_HEADER_ZEILE)).setVertJustify(CellVertJustify2.CENTER)
				.setHoriJustify(CellHoriJustify.CENTER)
				.setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder()).setCellBackColor(headerColor)
				.setCharHeight(NR_CHARHEIGHT).setShrinkToFit(true);

		headerValueZeile2.setValue("A");
		getSheetHelper().setStringValueInCell(headerValueZeile2);

		headerValueZeile2.setValue("B").spaltePlus(1);
		getSheetHelper().setStringValueInCell(headerValueZeile2);

		headerValueZeile2.setValue("Ergebnis").spaltePlus(1).setEndPosMergeSpaltePlus(1);
		getSheetHelper().setStringValueInCell(headerValueZeile2);

	}

	/**
	 * spalten Teampaarungen + Ergebnis
	 * 
	 * @throws GenerateException
	 */

	private void datenformatieren() throws GenerateException {
		processBoxinfo("Daten Formatieren");

		// gitter
		Position datenStart = Position.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE);
		Position datenEnd = letztePositionRechtsUnten();

		// komplett mit normal gitter
		RangePosition datenRangeInclErg = RangePosition.from(datenStart, datenEnd);
		TableBorder2 border = BorderFactory.from().allThin().toBorder();
		getSheetHelper().setPropertyInRange(getXSpreadSheet(), datenRangeInclErg, TABLE_BORDER2, border);

		SpielrundeHintergrundFarbeGeradeStyle geradeColor = getKonfigurationSheet()
				.getSpielRundeHintergrundFarbeGeradeStyle();
		SpielrundeHintergrundFarbeUnGeradeStyle unGeradeColor = getKonfigurationSheet()
				.getSpielRundeHintergrundFarbeUnGeradeStyle();

		RangePosition datenRangeSpielpaarungen = RangePosition.from(datenRangeInclErg).endeSpalte(TEAM_B_SPALTE);
		spielrundeHelper.formatiereGeradeUngradeSpielpaarungen(this, datenRangeSpielpaarungen, geradeColor,
				unGeradeColor);
	}

	/**
	 * Spalte SpielerNR A verwenden um die letzte zeile zu ermitteln<br>
	 * Spalte ist dann ergebniss Team B
	 * 
	 * @return
	 * @throws GenerateException
	 */

	public Position letztePositionRechtsUnten() throws GenerateException {
		Position spielerNrPos = Position.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE);

		if (getSheetHelper().getIntFromCell(this, spielerNrPos) == -1) {
			return null; // Keine Daten
		}

		RangePosition erstSpielrNrRange = RangePosition.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE, TEAM_A_SPALTE,
				ERSTE_DATEN_ZEILE + 999);

		// alle Daten einlesen
		RangeData nrDaten = RangeHelper.from(this, erstSpielrNrRange).getDataFromRange();
		// erste pos ohne int value
		int index = IntStream.range(0, nrDaten.size())
				.filter(nrDatenIdx -> nrDaten.get(nrDatenIdx).get(0).getIntVal(-1) == -1).findFirst().orElse(-1);
		if (index > 0) {
			spielerNrPos.zeilePlus(index - 1);
		}

		return spielerNrPos.spalte(ERG_TEAM_B_SPALTE);
	}

	protected boolean neueSpielrunde(TeamMeldungen meldungen, SpielRundeNr neueSpielrundeNr) throws GenerateException {
		return neueSpielrunde(meldungen, neueSpielrundeNr, isForceOk());
	}

	protected boolean neueSpielrunde(TeamMeldungen meldungen, SpielRundeNr neueSpielrundeNr, boolean force)
			throws GenerateException {
		checkNotNull(meldungen);

		processBoxinfo("Neue Spielrunde " + neueSpielrundeNr.getNr());
		processBoxinfo(meldungen.size() + " Meldungen");

		// wenn hier dann neu erstellen
		if (!NewSheet.from(this, getSheetName(getSpielRundeNr())).pos(DefaultSheetPos.SCHWEIZER_WORK)
				.setForceCreate(force).setActiv().hideGrid().create().isDidCreate()) {
			ProcessBox.from().info("Abbruch vom Benutzer, Spielrunde wurde nicht erstellt");
			return false;
		}

		// neue Spielrunde speichern, sheet vorhanden
		getKonfigurationSheet().setAktiveSpielRunde(getSpielRundeNr());

		SchweizerSystem schweizerSystem = new SchweizerSystem(meldungen);

		List<TeamPaarung> paarungen = null;

		if (neueSpielrundeNr.getNr() == 1) {
			paarungen = schweizerSystem.ersteRunde();
		} else {
			// TODO
		}

		teamPaarungenEinfuegen(paarungen);
		datenErsteSpalte(); // BahnNr 
		datenformatieren();
		header();

		return true;
	}

	/**
	 * Daten <br>
	 * kein hintergrund
	 * 
	 * @param paarungen
	 * @throws GenerateException
	 */

	private void teamPaarungenEinfuegen(List<TeamPaarung> paarungen) throws GenerateException {

		if (paarungen != null) {
			RangeData rangeData = new RangeData();

			for (int i = 0; i < paarungen.size(); i++) {
				SheetRunner.testDoCancelTask();
				TeamPaarung teamPaarung = paarungen.get(i);
				rangeData.addNewRow(teamPaarung.getA().getNr(), teamPaarung.getB().getNr());
			}

			Position startPos = Position.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE);
			RangeHelper.from(this, rangeData.getRangePosition(startPos)).setDataInRange(rangeData);
		}
	}

	/**
	 * fuer Test
	 * 
	 * @return
	 */
	public boolean isForceOk() {
		return forceOk;
	}

	/**
	 * fuer Test
	 * 
	 * @return
	 */
	public void setForceOk(boolean forceOk) {
		this.forceOk = forceOk;
	}

}
