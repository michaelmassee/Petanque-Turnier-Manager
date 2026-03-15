/**
 * Erstellung : 30.04.2018 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.meldeliste;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.IMeldeliste;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;

public class MeldeListeSheet_New extends SheetRunner implements IMeldeliste<SpielerMeldungen, Spieler> {

	private static final Logger logger = LogManager.getLogger(MeldeListeSheet_New.class);

	private final SupermeleeListeDelegate delegate;

	public MeldeListeSheet_New(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SUPERMELEE, "Meldeliste");
		delegate = new SupermeleeListeDelegate(this, workingSpreadsheet,
				newSuperMeleeKonfigurationSheet(workingSpreadsheet));
	}

	@VisibleForTesting
	protected SuperMeleeKonfigurationSheet newSuperMeleeKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new SuperMeleeKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	protected SuperMeleeKonfigurationSheet getKonfigurationSheet() {
		return delegate.getKonfigurationSheet();
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return getSheetHelper().findByName(SHEETNAME);
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	public MeldungenSpalte<SpielerMeldungen, Spieler> getMeldungenSpalte() {
		return delegate.getMeldungenSpalte();
	}

	@Override
	public String formulaSverweisSpielernamen(String spielrNrAdresse) {
		return delegate.formulaSverweisSpielernamen(spielrNrAdresse);
	}

	@Override
	public SpielerMeldungen getAktiveUndAusgesetztMeldungen() throws GenerateException {
		return delegate.getAktiveUndAusgesetztMeldungen();
	}

	@Override
	public SpielerMeldungen getAktiveMeldungen() throws GenerateException {
		return delegate.getAktiveMeldungen();
	}

	@Override
	public SpielerMeldungen getInAktiveMeldungen() throws GenerateException {
		return delegate.getInAktiveMeldungen();
	}

	@Override
	public SpielerMeldungen getAlleMeldungen() throws GenerateException {
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
	 * Erstellt die Meldeliste mit dem angegebenen Modus ohne Dialog.
	 * Wird von Test-Klassen aufgerufen, um den Start-Dialog zu umgehen.
	 */
	public void createMeldelisteWithParams(SuperMeleeMode mode) throws GenerateException {
		if (NewSheet.from(this, SHEETNAME).pos(DefaultSheetPos.MELDELISTE).tabColor(SHEET_COLOR).hideGrid().setActiv()
				.setDocVersionWhenNew().create().isDidCreate()) {
			getKonfigurationSheet().setSuperMeleeMode(mode);
			SpielTagNr spielTag1 = new SpielTagNr(1);
			setSpielTag(spielTag1);
			getKonfigurationSheet().setAktiveSpieltag(spielTag1);
			getKonfigurationSheet().setAktiveSpielRunde(SpielRundeNr.from(1));
			upDateSheet();
		}
	}

	public void doSort(int spalteNr, boolean isAscending) throws GenerateException {
		delegate.doSort(spalteNr, isAscending);
	}

	public SpielTagNr getSpielTag() {
		return delegate.getSpielTag();
	}

	public void setSpielTag(SpielTagNr spielTag) {
		delegate.setSpielTag(spielTag);
	}

	public void setAktiveSpieltag(SpielTagNr spielTagNr) throws GenerateException {
		delegate.setAktiveSpieltag(spielTagNr);
	}

	public void setAktiveSpielRunde(SpielRundeNr spielRundeNr) throws GenerateException {
		delegate.setAktiveSpielRunde(spielRundeNr);
	}

	public int aktuelleSpieltagSpalte() {
		return delegate.aktuelleSpieltagSpalte();
	}

	public int spieltagSpalte(SpielTagNr spieltagNr) {
		return delegate.spieltagSpalte(spieltagNr);
	}

	public int countAnzSpieltageInMeldeliste() throws GenerateException {
		return delegate.countAnzSpieltageInMeldeliste();
	}

	public int ersteSummeSpalte() throws GenerateException {
		return delegate.ersteSummeSpalte();
	}

	public String spielTagHeader(SpielTagNr spieltag) {
		return delegate.spielTagHeader(spieltag);
	}

	public int getAnzahlAktiveSpieler(SpielTagNr spieltag) throws GenerateException {
		return delegate.getAnzahlAktiveSpieler(spieltag);
	}

	public Position getAnzahlAktiveSpielerPosition(SpielTagNr spieltag) throws GenerateException {
		return delegate.getAnzahlAktiveSpielerPosition(spieltag);
	}

	public int getAusgestiegenSpieler(SpielTagNr spieltag) throws GenerateException {
		return delegate.getAusgestiegenSpieler(spieltag);
	}

	public Position getAusgestiegenSpielerPosition(SpielTagNr spieltag) throws GenerateException {
		return delegate.getAusgestiegenSpielerPosition(spieltag);
	}

	public int sucheLetzteZeileMitSpielerNummer() throws GenerateException {
		return delegate.sucheLetzteZeileMitSpielerNummer();
	}

	@Override
	protected void doRun() throws GenerateException {
		// Dialog zuerst – bei Abbruch keine Änderungen am Dokument
		Optional<SupermeleeStartDialog.StartParameter> param;
		try {
			param = SupermeleeStartDialog.from(getWorkingSpreadsheet()).show();
		} catch (com.sun.star.uno.Exception e) {
			logger.error("{} Fehler beim Anzeigen des Start-Dialogs: {}", e.getMessage(), e);
			throw new GenerateException("Fehler beim Anzeigen des Start-Dialogs: " + e.getMessage());
		}

		if (param.isEmpty()) {
			return; // Benutzer hat abgebrochen – keine Dokument-Änderungen
		}

		if (NewSheet.from(this, SHEETNAME).pos(DefaultSheetPos.MELDELISTE).tabColor(SHEET_COLOR).hideGrid().setActiv()
				.setDocVersionWhenNew().create().isDidCreate()) {
			getKonfigurationSheet().setSuperMeleeMode(
					param.get().triplette() ? SuperMeleeMode.Triplette : SuperMeleeMode.Doublette);
			SpielTagNr spielTag1 = new SpielTagNr(1);
			setSpielTag(spielTag1);
			getKonfigurationSheet().setAktiveSpieltag(spielTag1);
			getKonfigurationSheet().setAktiveSpielRunde(SpielRundeNr.from(1));
			upDateSheet();
		}
	}
}
