package de.petanqueturniermanager.schweizer.spielrunde;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.SchweizerSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.properties.RangeProperties;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
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

	public static final int BAHN_NR_SPALTE = 0;
	public static final int TEAM_A_SPALTE = BAHN_NR_SPALTE + 1;
	public static final int TEAM_B_SPALTE = TEAM_A_SPALTE + 1;
	public static final int ERG_TEAM_A_SPALTE = TEAM_B_SPALTE + 1;
	public static final int ERG_TEAM_B_SPALTE = ERG_TEAM_A_SPALTE + 1;

	private final AbstractSchweizerMeldeListeSheet meldeListe;
	private SpielRundeNr spielRundeNrInSheet = null;
	private boolean forceOk = false; // wird fuer Test verwendet

	//	private SpielRundeNr sheetSpielRundeNr = null; // muss nicht der Aktive sein

	protected SchweizerAbstractSpielrundeSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, SHEET_NAMEN);
		meldeListe = initMeldeListeSheet(workingSpreadsheet);
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

		teamPaarungenEinfuegenFormat(paarungen);
		bahnNr(paarungen.size());

		return true;
	}

	/**
	 * Daten und formatierung<br>
	 * kein hintergrund
	 * 
	 * @param paarungen
	 * @throws GenerateException
	 */

	private void teamPaarungenEinfuegenFormat(List<TeamPaarung> paarungen) throws GenerateException {

		if (paarungen != null) {
			RangeData rangeData = new RangeData();

			for (int i = 0; i < paarungen.size(); i++) {
				SheetRunner.testDoCancelTask();
				TeamPaarung teamPaarung = paarungen.get(i);
				rangeData.addNewRow(teamPaarung.getA().getNr(), teamPaarung.getB().getNr());
			}

			Position startPos = Position.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE);
			RangeHelper.from(this, rangeData.getRangePosition(startPos)).setDataInRange(rangeData).setRangeProperties(
					RangeProperties.from().centerJustify().setBorder(BorderFactory.from().allThin().toBorder()));
		}
	}

	private void bahnNr(int anzPaarungen) throws GenerateException {

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
