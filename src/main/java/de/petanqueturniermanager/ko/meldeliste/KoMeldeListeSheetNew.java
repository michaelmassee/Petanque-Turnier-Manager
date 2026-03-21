/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko.meldeliste;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellt ein neues K.-O.-Turnier: Konfigurationsblatt + Meldeliste.
 */
public class KoMeldeListeSheetNew extends SheetRunner implements ISheet, MeldeListeKonstanten {

	private static final Logger logger = LogManager.getLogger(KoMeldeListeSheetNew.class);

	protected static final int ERSTE_DATEN_ZEILE = KoListeDelegate.ERSTE_DATEN_ZEILE;

	private final KoListeDelegate delegate;

	public KoMeldeListeSheetNew(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.KO, "KO-Meldeliste");
		delegate = new KoListeDelegate(this);
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return getSheetHelper().findByName(SHEETNAME);
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

	public TeamMeldungen getAktiveMeldungen() throws GenerateException {
		return delegate.getAktiveMeldungen();
	}

	public TeamMeldungen getMeldungenSortiertNachRangliste() throws GenerateException {
		return delegate.getMeldungenSortiertNachRangliste();
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
		if (NewSheet.from(this, SHEETNAME).pos(DefaultSheetPos.MELDELISTE).hideGrid().tabColor(SHEET_COLOR)
				.setDocVersionWhenNew().create().isDidCreate()) {
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
							konfig.getGruppenGroesse(),
							konfig.getMinRestGroesse());
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
		konfig.setSpielbaumSpielUmPlatz3(params.spielUmPlatz3);
		konfig.setGruppenGroesse(params.gruppenGroesse);
		konfig.setMinRestGroesse(params.minRestGroesse);

		// KonfigSheet mit neuen Werten neu rendern
		getKonfigurationSheet().update();

		// Alle anderen Blätter entfernen, dann Meldeliste erstellen
		getSheetHelper().removeAllSheetsExclude();
		createMeldelisteWithParams();
	}

}
