package de.petanqueturniermanager.jedergegenjeden.meldeliste;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.IMeldeliste;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellung 01.08.2022 / Michael Massee
 */

public class JGJMeldeListeSheet_New extends SheetRunner implements IMeldeliste<TeamMeldungen, Team> {

	private static final Logger logger = LogManager.getLogger(JGJMeldeListeSheet_New.class);

	private static final String METADATA_SCHLUESSEL = SheetMetadataHelper.SCHLUESSEL_JGJ_MELDELISTE;

	private final JGJMeldeListeDelegate delegate;

	public JGJMeldeListeSheet_New(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.JGJ, "JGJ-Meldeliste");
		delegate = new JGJMeldeListeDelegate(this, workingSpreadsheet, TurnierSystem.JGJ);
	}

	@Override
	protected JGJKonfigurationSheet getKonfigurationSheet() {
		return delegate.getKonfigurationSheet();
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), METADATA_SCHLUESSEL, SheetNamen.LEGACY_MELDELISTE);
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	public MeldungenSpalte<TeamMeldungen, Team> getMeldungenSpalte() {
		return delegate.getMeldungenSpalte();
	}

	@Override
	public String formulaSverweisSpielernamen(String spielrNrAdresse) {
		return delegate.formulaSverweisSpielernamen(spielrNrAdresse);
	}

	@Override
	public TeamMeldungen getAktiveUndAusgesetztMeldungen() throws GenerateException {
		return getAlleMeldungen();
	}

	@Override
	public TeamMeldungen getAktiveMeldungen() throws GenerateException {
		return getAlleMeldungen();
	}

	@Override
	public TeamMeldungen getInAktiveMeldungen() throws GenerateException {
		return new TeamMeldungen();
	}

	@Override
	public TeamMeldungen getAlleMeldungen() throws GenerateException {
		return delegate.getAlleMeldungen();
	}

	@Override
	public int letzteSpielTagSpalte() throws GenerateException {
		return delegate.letzteSpielTagSpalte();
	}

	@Override
	public int getSpielerNameErsteSpalte() {
		return delegate.getSpielerNameErsteSpalte();
	}

	@Override
	public int getLetzteDatenZeileUseMin() throws GenerateException {
		return delegate.getLetzteDatenZeileUseMin();
	}

	@Override
	public int getSpielerZeileNr(int spielerNr) throws GenerateException {
		return delegate.getSpielerZeileNr(spielerNr);
	}

	@Override
	public int naechsteFreieDatenZeileInSpielerNrSpalte() throws GenerateException {
		return delegate.naechsteFreieDatenZeileInSpielerNrSpalte();
	}

	@Override
	public int getLetzteMitDatenZeileInSpielerNrSpalte() throws GenerateException {
		return delegate.getLetzteMitDatenZeileInSpielerNrSpalte();
	}

	@Override
	public int getErsteDatenZiele() {
		return delegate.getErsteDatenZiele();
	}

	@Override
	public List<String> getSpielerNamenList() throws GenerateException {
		return delegate.getSpielerNamenList();
	}

	@Override
	public List<Integer> getSpielerNrList() throws GenerateException {
		return delegate.getSpielerNrList();
	}

	@Override
	public int letzteZeileMitSpielerName() throws GenerateException {
		return delegate.letzteZeileMitSpielerName();
	}

	public void upDateSheet() throws GenerateException {
		delegate.upDateSheet();
	}

	/**
	 * Erstellt die Meldeliste mit dem angegebenen Gruppenname ohne Dialog.
	 * Wird von Test-Klassen aufgerufen, um den Start-Dialog zu umgehen.
	 */
	public void createMeldelisteWithParams(String gruppenname) throws GenerateException {
		if (NewSheet.from(this, SheetNamen.meldeliste()).pos(DefaultSheetPos.MELDELISTE).hideGrid().tabColor(SHEET_COLOR)
				.setDocVersionWhenNew().create().isDidCreate()) {
			getKonfigurationSheet().setGruppenname(gruppenname);
			SheetMetadataHelper.schreibeSheetMetadaten(
					getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
					getXSpreadSheet(), METADATA_SCHLUESSEL);
			delegate.upDateSheet();
		}
	}

	@Override
	protected boolean isUpdateKonfigurationSheetBeforeDoRun() {
		return false;
	}

	@Override
	protected void doRun() throws GenerateException {
		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.JGJ)
				.prefix(getLogPrefix()).validate()) {
			return;
		}

		// Dialog zuerst – bei Abbruch keine Änderungen am Dokument
		Optional<JGJStartDialog.StartParameter> param;
		try {
			param = JGJStartDialog.from(getWorkingSpreadsheet()).show();
		} catch (com.sun.star.uno.Exception e) {
			String errMsg = I18n.get("error.dialog.startdialog", e.getMessage());
			logger.error(errMsg, e);
			throw new GenerateException(errMsg);
		}

		if (param.isEmpty()) {
			return; // Benutzer hat abgebrochen – keine Dokument-Änderungen
		}

		// Erst nach Bestätigung: TurnierSystem + Page Styles setzen
		getKonfigurationSheet().update();

		getSheetHelper().removeAllSheetsExclude();
		createMeldelisteWithParams(param.get().gruppenname());
	}

}
