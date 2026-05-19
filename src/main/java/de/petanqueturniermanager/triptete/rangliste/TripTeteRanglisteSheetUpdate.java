package de.petanqueturniermanager.triptete.rangliste;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.triptete.konfiguration.TripTeteKonfigurationSheet;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetUpdate;

/**
 * Listener-sicheres Rangliste-Update: Aktualisiert nur den Datenbereich der
 * bereits existierenden Trip-Tête-Rangliste, ohne {@code forceCreate} und damit
 * ohne Race-Condition gegen einen parallelen Vollaufbau.
 */
public class TripTeteRanglisteSheetUpdate extends SheetRunner implements ISheet {

	private static final String METADATA_SCHLUESSEL = SheetMetadataHelper.SCHLUESSEL_TRIPTETE_RANGLISTE;

	private final TripTeteKonfigurationSheet konfigurationSheet;
	private final TripTeteMeldeListeSheetUpdate meldeListe;

	public TripTeteRanglisteSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.TRIPTETE, "Trip-Tête-RanglisteUpdate");
		konfigurationSheet = new TripTeteKonfigurationSheet(workingSpreadsheet);
		meldeListe = new TripTeteMeldeListeSheetUpdate(workingSpreadsheet);
	}

	@Override
	protected TripTeteKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), METADATA_SCHLUESSEL,
				SheetNamen.LEGACY_RANGLISTE);
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	protected void doRun() throws GenerateException {
		// Nur ausführen wenn ein Rangliste-Sheet bereits existiert
		if (getSheetHelper().findByName(SheetNamen.rangliste()) == null) {
			return;
		}
		TripTeteRanglisteDatenSchreiber.from(this, meldeListe).schreibeDaten();
	}
}
