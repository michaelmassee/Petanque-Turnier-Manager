/*
 * Erstellung : 01.03.2024 / Michael Massee
 **/

package de.petanqueturniermanager.schweizer.meldeliste;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.Exception;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.IMeldeliste;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

public class SchweizerMeldeListeSheetNew extends SheetRunner
		implements IMeldeliste<TeamMeldungen, Team>, MeldeListeKonstanten {

	private static final Logger logger = LogManager.getLogger(SchweizerMeldeListeSheetNew.class);

	protected static final int ERSTE_DATEN_ZEILE = SchweizerListeDelegate.ERSTE_DATEN_ZEILE;

	private final SchweizerListeDelegate delegate;

	public SchweizerMeldeListeSheetNew(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SCHWEIZER, "Schweizer-Meldeliste");
		delegate = new SchweizerListeDelegate(this);
	}

	private static final String METADATA_SCHLUESSEL = SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELDELISTE;

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), METADATA_SCHLUESSEL, SheetNamen.LEGACY_MELDELISTE);
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	protected SchweizerKonfigurationSheet getKonfigurationSheet() {
		return delegate.getKonfigurationSheet();
	}

	// ---------------------------------------------------------------
	// Forwarding-Methoden → Delegate
	// ---------------------------------------------------------------

	public void upDateSheet() throws GenerateException {
		delegate.upDateSheet();
	}

	public int getTeamNrSpalte() {
		return delegate.getTeamNrSpalte();
	}

	public int getTeamnameSpalte() throws GenerateException {
		return delegate.getTeamnameSpalte();
	}

	@Override
	public int getSpielerNameErsteSpalte() {
		try {
			return delegate.getSpielerNameErsteSpalte();
		} catch (GenerateException e) {
			throw new IllegalStateException(e);
		}
	}

	public int getVornameSpalte(int spielerIdx) throws GenerateException {
		return delegate.getVornameSpalte(spielerIdx);
	}

	public int getNachnameSpalte(int spielerIdx) throws GenerateException {
		return delegate.getNachnameSpalte(spielerIdx);
	}

	public int getVereinsnameSpalte(int spielerIdx) throws GenerateException {
		return delegate.getVereinsnameSpalte(spielerIdx);
	}

	public int getSetzPositionSpalte() throws GenerateException {
		return delegate.getSetzPositionSpalte();
	}

	public int getAktivSpalte() throws GenerateException {
		return delegate.getAktivSpalte();
	}

	@Override
	public int getErsteDatenZiele() {
		return delegate.getErsteDatenZiele();
	}

	@Override
	public TeamMeldungen getAktiveMeldungen() throws GenerateException {
		return delegate.getAktiveMeldungen();
	}

	@Override
	public TeamMeldungen getAlleMeldungen() throws GenerateException {
		return delegate.getAlleMeldungen();
	}

	@Override
	public TeamMeldungen getAktiveUndAusgesetztMeldungen() throws GenerateException {
		return getAlleMeldungen();
	}

	@Override
	public TeamMeldungen getInAktiveMeldungen() throws GenerateException {
		return new TeamMeldungen();
	}

	@Override
	public MeldungenSpalte<TeamMeldungen, Team> getMeldungenSpalte() {
		try {
			return delegate.getMeldungenSpalte();
		} catch (GenerateException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public String formulaSverweisSpielernamen(String spielrNrAdresse) {
		try {
			return delegate.formulaSverweisSpielernamen(spielrNrAdresse);
		} catch (GenerateException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public int letzteSpielTagSpalte() throws GenerateException {
		return delegate.getAktivSpalte();
	}

	@Override
	public int getLetzteDatenZeileUseMin() throws GenerateException {
		return delegate.getLetzteDatenZeileUseMin();
	}

	@Override
	public int getLetzteMitDatenZeileInSpielerNrSpalte() throws GenerateException {
		return delegate.getLetzteMitDatenZeileInSpielerNrSpalte();
	}

	@Override
	public int naechsteFreieDatenZeileInSpielerNrSpalte() throws GenerateException {
		return delegate.naechsteFreieDatenZeileInSpielerNrSpalte();
	}

	@Override
	public int letzteZeileMitSpielerName() throws GenerateException {
		return delegate.letzteZeileMitSpielerName();
	}

	@Override
	public int getSpielerZeileNr(int spielerNr) throws GenerateException {
		return delegate.getSpielerZeileNr(spielerNr);
	}

	@Override
	public List<String> getSpielerNamenList() throws GenerateException {
		return delegate.getSpielerNamenList();
	}

	@Override
	public List<Integer> getSpielerNrList() throws GenerateException {
		return delegate.getSpielerNrList();
	}

	public int getTeamNrByTeamname(String teamname) throws GenerateException {
		return delegate.getTeamNrByTeamname(teamname);
	}

	public String getTeamNameByNr(int teamNr) throws GenerateException {
		return delegate.getTeamNameByNr(teamNr);
	}

	public void setAktiveSpielRunde(SpielRundeNr spielRundeNr) throws GenerateException {
		delegate.setAktiveSpielRunde(spielRundeNr);
	}

	// ---------------------------------------------------------------
	// Eigene Methoden
	// ---------------------------------------------------------------

	@Override
	protected boolean isUpdateKonfigurationSheetBeforeDoRun() {
		return false;
	}

	@Override
	protected void doRun() throws GenerateException {
		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.SCHWEIZER)
				.prefix(getLogPrefix()).validate()) {
			return;
		}

		// Dialog zuerst – bei Abbruch keine Änderungen am Dokument
		Optional<SchweizerTurnierParameterDialog.TurnierParameter> param;
		try {
			param = SchweizerTurnierParameterDialog.from(getWorkingSpreadsheet()).show(Formation.DOUBLETTE, false, false,
				SpielplanTeamAnzeige.NR, getKonfigurationSheet().getRankingModus());
		} catch (Exception e) {
			String errMsg = I18n.get("error.dialog.parameterdialog", e.getMessage());
			logger.error(errMsg, e);
			throw new GenerateException(errMsg);
		}

		if (param.isEmpty()) {
			return; // Benutzer hat abgebrochen – keine Dokument-Änderungen
		}

		// Erst nach Bestätigung: TurnierSystem + Page Styles setzen
		getKonfigurationSheet().update();

		getSheetHelper().removeAllSheetsExclude();
		getKonfigurationSheet().setRankingModus(param.get().rankingModus);
		createMeldelisteWithParams(param.get().formation, param.get().teamnameAnzeigen, param.get().vereinsnameAnzeigen,
				param.get().spielplanTeamAnzeige);
	}

	/**
	 * Erstellt die Meldeliste mit den angegebenen Parametern ohne Dialog.
	 * Wird auch von TestDaten-Klassen aufgerufen.
	 */
	public void createMeldelisteWithParams(Formation formation, boolean teamnameAnzeigen, boolean vereinsnameAnzeigen)
			throws GenerateException {
		createMeldelisteWithParams(formation, teamnameAnzeigen, vereinsnameAnzeigen, SpielplanTeamAnzeige.NR);
	}

	public void createMeldelisteWithParams(Formation formation, boolean teamnameAnzeigen, boolean vereinsnameAnzeigen,
			SpielplanTeamAnzeige spielplanTeamAnzeige) throws GenerateException {
		var neuesSheet = NewSheet.from(this, SheetNamen.meldeliste(), METADATA_SCHLUESSEL)
				.pos(DefaultSheetPos.MELDELISTE).hideGrid().tabColor(getKonfigurationSheet().getMeldelisteTabFarbe()).setDocVersionWhenNew().create();
		if (neuesSheet.isDidCreate()) {
			getKonfigurationSheet().setMeldeListeFormation(formation);
			getKonfigurationSheet().setMeldeListeTeamnameAnzeigen(teamnameAnzeigen);
			getKonfigurationSheet().setMeldeListeVereinsnameAnzeigen(vereinsnameAnzeigen);
			getKonfigurationSheet().setSpielplanTeamAnzeige(spielplanTeamAnzeige);
			upDateSheet();
		}
	}

}
