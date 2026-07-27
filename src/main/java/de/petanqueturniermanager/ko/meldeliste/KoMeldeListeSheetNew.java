/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko.meldeliste;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
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
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Erstellt ein neues K.-O.-Turnier: Konfigurationsblatt + Meldeliste.
 */
public class KoMeldeListeSheetNew extends SheetRunner
		implements IMeldeliste<TeamMeldungen, Team>, MeldeListeKonstanten {

	private static final Logger logger = LogManager.getLogger(KoMeldeListeSheetNew.class);

	protected static final int ERSTE_DATEN_ZEILE = KoListeDelegate.ERSTE_DATEN_ZEILE;

	private static final String METADATA_SCHLUESSEL = SheetMetadataHelper.SCHLUESSEL_KO_MELDELISTE;

	private final KoListeDelegate delegate;

	public KoMeldeListeSheetNew(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.KO, "KO-Meldeliste");
		delegate = new KoListeDelegate(this);
	}

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
	protected KoKonfigurationSheet getKonfigurationSheet() {
		return delegate.getKonfigurationSheet();
	}

	// ---------------------------------------------------------------
	// Forwarding-Methoden → Delegate
	// ---------------------------------------------------------------

	public void upDateSheet() throws GenerateException {
		delegate.upDateSheet();
	}

	public int getNrSpalte() {
		return delegate.getNrSpalte();
	}

	public int getTeamnameSpalte() throws GenerateException {
		return delegate.getTeamnameSpalte();
	}

	public int getVornameSpalte(int spielerIdx) throws GenerateException {
		return delegate.getVornameSpalte(spielerIdx);
	}

	public int getNachnameSpalte(int spielerIdx) throws GenerateException {
		return delegate.getNachnameSpalte(spielerIdx);
	}

	public int getAktivSpalte() throws GenerateException {
		return delegate.getAktivSpalte();
	}

	public int getRanglisteSpalte() throws GenerateException {
		return delegate.getRanglisteSpalte();
	}

	public int getErsteDatenZeile() {
		return delegate.getErsteDatenZeile();
	}

	@Override
	public TeamMeldungen getAktiveMeldungen() throws GenerateException {
		return delegate.getAktiveMeldungen();
	}

	public TeamMeldungen getMeldungenSortiertNachRangliste() throws GenerateException {
		return delegate.getMeldungenSortiertNachRangliste();
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
	public int getSpielerNameErsteSpalte() {
		try {
			return delegate.getSpielerNameErsteSpalte();
		} catch (GenerateException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public int getLetzteDatenZeileUseMin() throws GenerateException {
		return delegate.getLetzteDatenZeileUseMin();
	}

	@Override
	public int getErsteDatenZiele() {
		return delegate.getErsteDatenZiele();
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

	// ---------------------------------------------------------------
	// Eigene Methoden
	// ---------------------------------------------------------------

	@Override
	protected boolean isUpdateKonfigurationSheetBeforeDoRun() {
		return false;
	}

	/**
	 * Erstellt die Meldeliste (liest Parameter aus KonfigurationSheet).<br>
	 * Wird von Test-Klassen aufgerufen; im normalen Ablauf via {@link #doRun()}.
	 */
	public void createMeldelisteWithParams() throws GenerateException {
		var neuesSheet = NewSheet.from(this, SheetNamen.meldeliste(), METADATA_SCHLUESSEL)
				.pos(DefaultSheetPos.MELDELISTE).hideGrid().tabColor(getKonfigurationSheet().getMeldelisteTabFarbe()).setDocVersionWhenNew().create();
		if (neuesSheet.isDidCreate()) {
			upDateSheet();
		}
	}

	@Override
	protected void doRun() throws GenerateException {
		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.KO)
				.prefix(getLogPrefix()).validate()) {
			return;
		}

		// KonfigSheet anlegen, damit Defaults lesbar sind
		getKonfigurationSheet().update();

		// Dialog anzeigen
		KoKonfigurationSheet konfig = getKonfigurationSheet();
		Optional<KoTurnierParameterDialog.TurnierParameter> result;
		try {
			result = KoTurnierParameterDialog.from(getWorkingSpreadsheet())
					.show(konfig.getMeldeListeFormation(),
							konfig.isMeldeListeTeamnameAnzeigen(),
							konfig.isMeldeListeVereinsnameAnzeigen(),
							konfig.getSpielbaumTeamAnzeige(),
							konfig.getSpielbaumSpielbahn(),
							konfig.isSpielbaumSpielUmPlatz3(),
							konfig.isSpielbaumBahnNurRunde1(),
							konfig.getGruppenGroesse(),
							konfig.getMinLetzteGruppeGroesse());
		} catch (com.sun.star.uno.Exception e) {
			String errMsg = I18n.get("error.dialog.parameterdialog", e.getMessage());
			logger.error(errMsg, e);
			throw new GenerateException(errMsg);
		}

		if (result.isEmpty()) {
			return;
		}

		// Werte in KonfigSheet speichern
		var params = result.get();
		konfig.setMeldeListeFormation(params.formation);
		konfig.setMeldeListeTeamnameAnzeigen(params.teamnameAnzeigen);
		konfig.setMeldeListeVereinsnameAnzeigen(params.vereinsnameAnzeigen);
		konfig.setSpielbaumTeamAnzeige(params.spielbaumTeamAnzeige);
		konfig.setSpielbaumSpielbahn(params.spielbaumSpielbahn);
		konfig.setSpielbaumBahnNurRunde1(params.spielbaumBahnNurRunde1);
		konfig.setSpielbaumSpielUmPlatz3(params.spielUmPlatz3);
		konfig.setGruppenGroesse(params.gruppenGroesse);
		konfig.setMinLetzteGruppeGroesse(params.minLetzteGruppeGroesse);

		// KonfigSheet mit neuen Werten neu rendern
		getKonfigurationSheet().update();

		// Alle anderen Blätter entfernen, dann Meldeliste erstellen
		getSheetHelper().removeAllSheetsExclude();
		createMeldelisteWithParams();
	}

}
