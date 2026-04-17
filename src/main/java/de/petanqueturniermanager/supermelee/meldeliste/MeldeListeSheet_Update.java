/**
 * Erstellung : 30.04.2018 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.meldeliste;

import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.IMeldeliste;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzRegistry;
import de.petanqueturniermanager.toolbar.TurnierModus;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;

public class MeldeListeSheet_Update extends SheetRunner implements IMeldeliste<SpielerMeldungen, Spieler> {

	// Öffentliche Konstanten (früher in AbstractSupermeleeMeldeListeSheet)
	public static final int ERSTE_ZEILE_INFO = SupermeleeListeDelegate.ERSTE_ZEILE_INFO;
	public static final int SUMMEN_ERSTE_ZEILE = SupermeleeListeDelegate.SUMMEN_ERSTE_ZEILE;
	public static final int SUMMEN_AKTIVE_ZEILE = SupermeleeListeDelegate.SUMMEN_AKTIVE_ZEILE;
	public static final int SUMMEN_INAKTIVE_ZEILE = SupermeleeListeDelegate.SUMMEN_INAKTIVE_ZEILE;
	public static final int SUMMEN_AUSGESTIEGENE_ZEILE = SupermeleeListeDelegate.SUMMEN_AUSGESTIEGENE_ZEILE;
	public static final int SUMMEN_ANZ_SPIELER = SupermeleeListeDelegate.SUMMEN_ANZ_SPIELER;
	public static final int SUMMEN_GESAMT_ANZ_SPIELER = SupermeleeListeDelegate.SUMMEN_GESAMT_ANZ_SPIELER;
	public static final int TRIPL_MODE_HEADER = SupermeleeListeDelegate.TRIPL_MODE_HEADER;
	public static final int TRIPL_MODE_ANZ_DOUBLETTE = SupermeleeListeDelegate.TRIPL_MODE_ANZ_DOUBLETTE;
	public static final int TRIPL_MODE_ANZ_TRIPLETTE = SupermeleeListeDelegate.TRIPL_MODE_ANZ_TRIPLETTE;
	public static final int TRIPL_MODE_SUMMEN_KANN_DOUBLETTE_ZEILE = SupermeleeListeDelegate.TRIPL_MODE_SUMMEN_KANN_DOUBLETTE_ZEILE;
	public static final int TRIPL_MODE_SUMMEN_SPIELBAHNEN = SupermeleeListeDelegate.TRIPL_MODE_SUMMEN_SPIELBAHNEN;
	public static final int DOUBL_MODE_HEADER = SupermeleeListeDelegate.DOUBL_MODE_HEADER;
	public static final int DOUBL_MODE_ANZ_DOUBLETTE = SupermeleeListeDelegate.DOUBL_MODE_ANZ_DOUBLETTE;
	public static final int DOUBL_MODE_ANZ_TRIPLETTE = SupermeleeListeDelegate.DOUBL_MODE_ANZ_TRIPLETTE;
	public static final int DOUBL_MODE_SUMMEN_KANN_TRIPLETTE_ZEILE = SupermeleeListeDelegate.DOUBL_MODE_SUMMEN_KANN_TRIPLETTE_ZEILE;
	public static final int DOUBL_MODE_SUMMEN_SPIELBAHNEN = SupermeleeListeDelegate.DOUBL_MODE_SUMMEN_SPIELBAHNEN;
	public static final int MIN_ANZAHL_SPIELER_ZEILEN = SupermeleeListeDelegate.MIN_ANZAHL_SPIELER_ZEILEN;
	public static final int SUMMEN_SPALTE_OFFSET = SupermeleeListeDelegate.SUMMEN_SPALTE_OFFSET;
	public static final String PTM_SPIELTAG = SupermeleeListeDelegate.PTM_SPIELTAG;
	public static final String PTM_SPIELRUNDE = SupermeleeListeDelegate.PTM_SPIELRUNDE;

	private final SupermeleeListeDelegate delegate;

	public MeldeListeSheet_Update(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SUPERMELEE, "Meldeliste");
		delegate = new SupermeleeListeDelegate(this, workingSpreadsheet,
				newSuperMeleeKonfigurationSheet(workingSpreadsheet), SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_MELDELISTE);
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
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_MELDELISTE, SheetNamen.LEGACY_MELDELISTE);
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

	public void alleSpielAktivieren() throws GenerateException {
		delegate.alleSpielAktivieren();
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
		setSpielTag(getKonfigurationSheet().getAktiveSpieltag());
		// ReplaceConditionalFormat in LO kehrt lautlos zurück wenn Sheet tab-geschützt ist
		// (sc/source/ui/docshell/docfunc.cxx) → CF wird gelöscht aber nicht neu angelegt.
		// formatDaten() stellt den Schutz am Ende selbst wieder her.
		if (TurnierModus.get().istAktiv()) {
			BlattschutzRegistry.fuer(TurnierSystem.SUPERMELEE)
					.ifPresent(k -> BlattschutzManager.get().entsperren(k, getWorkingSpreadsheet()));
		}
		upDateSheet();
		pruefeUndFragObAlleAktivieren();
	}

	private void pruefeUndFragObAlleAktivieren() throws GenerateException {
		SpielerMeldungen aktiveMeldungen = getAktiveMeldungen();
		if (aktiveMeldungen.size() > 0) {
			return;
		}
		SpielerMeldungen alleMeldungen = getAlleMeldungen();
		if (alleMeldungen.size() == 0) {
			return;
		}
		MessageBoxResult result = MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_YES_NO)
				.caption(I18n.get("msg.caption.keine.aktiven.meldungen"))
				.message(I18n.get("msg.text.keine.aktiven.spieler.aktivieren", alleMeldungen.size()))
				.show();
		if (result == MessageBoxResult.YES) {
			alleSpielAktivieren();
		} else {
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("msg.caption.aktuelle.spielrunde.fehler"))
					.message(I18n.get("supermelee.spielrunde.fehler.zu.wenige.meldungen", 0))
					.show();
		}
	}
}
