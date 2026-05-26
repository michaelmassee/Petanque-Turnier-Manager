/*
* Erstellung : 20.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.AbstractCheckinListeSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.IMeldung;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;

/**
 * Checkin-Liste des Supermelee-Systems je Spieltag (früher "Anmeldungen").
 * Kompakte Druckansicht zum Abhaken der anwesenden Spieler, gespeist aus der Meldeliste.
 */
public class AnmeldungenSheet extends AbstractCheckinListeSheet {

	private final SuperMeleeKonfigurationSheet konfigurationSheet;
	private final MeldeListeSheet_Update meldeliste;
	private SpielTagNr spielTag = null;

	public AnmeldungenSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SUPERMELEE, "Checkin Liste");
		konfigurationSheet = new SuperMeleeKonfigurationSheet(workingSpreadsheet);
		meldeliste = new MeldeListeSheet_Update(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		setSpielTag(getKonfigurationSheet().getAktiveSpieltag());
		meldeliste.setSpielTag(getSpielTag());
		super.doRun();
	}

	@Override
	protected SuperMeleeKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	protected void meldelisteVorbereiten() throws GenerateException {
		meldeliste.setSpielTag(getSpielTag());
		meldeliste.upDateSheet();
	}

	@Override
	protected void meldelisteAusrichten() {
		meldeliste.setSpielTag(getSpielTag());
	}

	@Override
	protected List<Integer> ladeNummern() throws GenerateException {
		processBoxinfo("processbox.spieltag.meldungen.einlesen", getSpielTag().getNr());
		return meldeliste.getAlleMeldungen().getMeldungen().stream()
				.map(IMeldung::getNr)
				.toList();
	}

	@Override
	protected Map<Integer, SortSchluessel> ladeSortDaten() throws GenerateException {
		var meldungenSpalte = meldeliste.getMeldungenSpalte();
		return leseNachnameSortDaten(meldeliste, meldungenSpalte.getSpielerNrSpalte(),
				meldungenSpalte.getLetzteMeldungNameSpalte(), meldungenSpalte.getErsteDatenZiele());
	}

	@Override
	protected String getNameFormula(String nrZelleAdresse) throws GenerateException {
		return meldeliste.formulaSverweisSpielernamen(nrZelleAdresse);
	}

	@Override
	protected int getNameSpalteWidth() {
		return SuperMeleeKonfigurationSheet.SUPER_MELEE_MELDUNG_NAME_WIDTH;
	}

	@Override
	protected short getSheetPos() {
		return DefaultSheetPos.SUPERMELEE_WORK;
	}

	@Override
	protected void seitenStilAnwenden(NewSheet newSheet) {
		newSheet.spielTagPageStyle(getSpielTag());
	}

	@Override
	protected String getCheckinSheetName() {
		return SheetNamen.checkinListe(getSpielTag().getNr());
	}

	@Override
	protected String getMetadatenSchluessel() {
		return SheetMetadataHelper.schluesselSupermeleeAnmeldungen(getSpielTag().getNr());
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				SheetMetadataHelper.schluesselSupermeleeAnmeldungen(getSpielTag().getNr()),
				getCheckinSheetName());
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	public SpielTagNr getSpielTag() {
		checkNotNull(spielTag);
		return spielTag;
	}

	public void setSpielTag(SpielTagNr spielTag) {
		checkNotNull(spielTag);
		this.spielTag = spielTag;
	}
}
