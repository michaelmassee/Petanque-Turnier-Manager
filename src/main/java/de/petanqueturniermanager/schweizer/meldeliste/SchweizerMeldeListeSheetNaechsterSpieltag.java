/*
 * Erstellung : 18.08.2026 / Michael Massee
 **/

package de.petanqueturniermanager.schweizer.meldeliste;

import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.IMeldeliste;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

/**
 * Legt einen neuen Spieltag einer Schweizer-Turnierserie an: siehe {@link SchweizerListeDelegate#naechsteSpieltag()}.
 * <p>
 * Eigenständige {@link SheetRunner}-Klasse (statt Wiederverwendung von {@link SchweizerMeldeListeSheetNew}),
 * weil die Vorbedingung entgegengesetzt ist: hier MUSS bereits ein Schweizer-Turnier existieren, während
 * {@code SchweizerMeldeListeSheetNew} nur ausgeführt werden darf, wenn noch KEIN Turnier existiert. Analoges
 * Muster wie Supermelees {@code MeldeListeSheet_NeuerSpieltag}.
 */
public class SchweizerMeldeListeSheetNaechsterSpieltag extends SheetRunner
		implements IMeldeliste<TeamMeldungen, Team> {

	private static final String METADATA_SCHLUESSEL = SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELDELISTE;

	private final SchweizerListeDelegate delegate;

	public SchweizerMeldeListeSheetNaechsterSpieltag(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SCHWEIZER, "Schweizer-Meldeliste");
		delegate = new SchweizerListeDelegate(this);
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
	protected SchweizerKonfigurationSheet getKonfigurationSheet() {
		return delegate.getKonfigurationSheet();
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
		return delegate.getAktiveUndAusgesetztMeldungen();
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

	public void setAktiveSpielRunde(SpielRundeNr spielRundeNr) throws GenerateException {
		delegate.setAktiveSpielRunde(spielRundeNr);
	}

	@Override
	protected boolean isUpdateKonfigurationSheetBeforeDoRun() {
		return false;
	}

	@Override
	protected void doRun() throws GenerateException {
		delegate.naechsteSpieltag();
	}

}
