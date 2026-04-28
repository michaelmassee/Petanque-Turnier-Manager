package de.petanqueturniermanager.maastrichter.korunde;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellung 27.07.2022 / Michael Massee
 */

public class VorrundenSheet extends SheetRunner implements ISheet {

	private static final String METADATA_SCHLUESSEL = SheetMetadataHelper.SCHLUESSEL_FORME_VORRUNDEN;

	private final MaastrichterKonfigurationSheet konfigurationSheet;

	public VorrundenSheet(WorkingSpreadsheet workingSpreadsheet, TurnierSystem spielSystem) {
		super(workingSpreadsheet, spielSystem);
		konfigurationSheet = new MaastrichterKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), METADATA_SCHLUESSEL, SheetNamen.LEGACY_VORRUNDEN_ERGEBNISSE);
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	protected MaastrichterKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	protected void doRun() throws GenerateException {
		NewSheet.from(this, SheetNamen.vorrundenErgebnisse(), METADATA_SCHLUESSEL)
				.pos(DefaultSheetPos.MELDELISTE).tabColor(konfigurationSheet.getTeilnehmerTabFarbe()).hideGrid().setActiv().create();
	}

}
