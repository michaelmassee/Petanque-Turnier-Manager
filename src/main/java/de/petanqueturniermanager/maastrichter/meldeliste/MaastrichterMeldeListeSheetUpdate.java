/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.meldeliste;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Aktualisiert die Maastrichter-Meldeliste.
 * <p>
 * Erweitert {@link SchweizerMeldeListeSheetUpdate}, da das Layout identisch ist.
 * Überschreibt nur {@code getXSpreadSheet()}, um den Maastrichter-Metadaten-Schlüssel
 * zu verwenden – das stellt sicher, dass {@code upDateSheet()} das richtige Sheet
 * findet, unabhängig von der eingestellten Locale.
 */
public class MaastrichterMeldeListeSheetUpdate extends SchweizerMeldeListeSheetUpdate {

	private static final String METADATA_SCHLUESSEL = SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_MELDELISTE;

	public MaastrichterMeldeListeSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.MAASTRICHTER, "Maastrichter-Meldeliste",
				new MaastrichterKonfigurationSheet(workingSpreadsheet));
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

}
