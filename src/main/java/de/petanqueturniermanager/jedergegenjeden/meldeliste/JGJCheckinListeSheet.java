/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.jedergegenjeden.meldeliste;

import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.AbstractTeilnehmerNamenCheckinListeSheet;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.model.IMeldung;

/**
 * Checkin-Liste des Jeder-gegen-Jeden-Systems: kompakte Druckansicht zum Abhaken der Teilnehmer.
 * Die Namen werden – wie bei den übrigen Systemen – als Werte aus der Meldeliste zusammengesetzt
 * (Formation/Teamname/Vereinsname); die Nummernquelle bleiben die aktiven Meldungen.
 */
public class JGJCheckinListeSheet extends AbstractTeilnehmerNamenCheckinListeSheet {

	private final JGJKonfigurationSheet konfigurationSheet;
	private final JGJMeldeListeSheet_Update meldeliste;

	public JGJCheckinListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.JGJ, "Checkin Liste");
		konfigurationSheet = new JGJKonfigurationSheet(workingSpreadsheet);
		meldeliste = new JGJMeldeListeSheet_Update(workingSpreadsheet);
	}

	@Override
	protected JGJKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	protected void meldelisteVorbereiten() throws GenerateException {
		meldeliste.upDateSheet();
	}

	@Override
	protected ISheet getMeldelisteSheet() {
		return meldeliste;
	}

	@Override
	protected int getMeldelisteErsteDatenZeile() {
		return meldeliste.getErsteDatenZiele();
	}

	@Override
	protected Formation getFormation() {
		return konfigurationSheet.getMeldeListeFormation();
	}

	@Override
	protected boolean istTeamnameAktiv() {
		return konfigurationSheet.isMeldeListeTeamnameAnzeigen();
	}

	@Override
	protected boolean istVereinsnameAktiv() {
		return konfigurationSheet.isMeldeListeVereinsnameAnzeigen();
	}

	/** Nummernquelle: nur die aktiven Meldungen (nicht alle Meldeliste-Zeilen). */
	@Override
	protected List<Integer> ladeNummern() throws GenerateException {
		return meldeliste.getAlleMeldungen().getMeldungen().stream()
				.map(IMeldung::getNr)
				.toList();
	}

	@Override
	protected short getSheetPos() {
		return DefaultSheetPos.JGJ_WORK;
	}

	@Override
	protected String getCheckinSheetName() {
		return SheetNamen.checkinListe();
	}

	@Override
	protected String getMetadatenSchluessel() {
		return SheetMetadataHelper.SCHLUESSEL_JGJ_CHECKIN_LISTE;
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				SheetMetadataHelper.SCHLUESSEL_JGJ_CHECKIN_LISTE,
				getCheckinSheetName());
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}
}
