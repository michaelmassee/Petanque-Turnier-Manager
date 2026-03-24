/**
 * Erstellung 21.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.forme.korunde;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.KoRundeTeamPaarungen;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.FormeSpielrunde;
import de.petanqueturniermanager.model.TeamRangliste;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
 */
public class KoGruppeABSheet extends SheetRunner implements ISheet {

	private static final String SHEET_COLOR = "98e2d7";
	private static final String METADATA_SCHLUESSEL = SheetMetadataHelper.SCHLUESSEL_FORME_KO_GRUPPE;

	private final Vorrunden vorrunden;
	private final Ranglisten ranglisten;
	private final SpielRundeInSheet spielRundeInSheet;

	/**
	 * @param workingSpreadsheet
	 * @throws GenerateException
	 */
	public KoGruppeABSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.MAASTRICHTER, "KO Gruppe AB");
		vorrunden = new Vorrunden(this);
		ranglisten = new Ranglisten(this);
		spielRundeInSheet = new SpielRundeInSheet(this);
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), METADATA_SCHLUESSEL, SheetNamen.LEGACY_KO_RUNDE);
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	protected void doRun() throws GenerateException {
		NewSheet.from(this, SheetNamen.koRunde()).tabColor(SHEET_COLOR).pos(DefaultSheetPos.MELEE_WORK).forceCreate().setActiv().create();
		SheetMetadataHelper.schreibeSheetMetadaten(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				getXSpreadSheet(), METADATA_SCHLUESSEL);
		koRundeErstellen();
		getSheetHelper().setActiveSheet(getXSpreadSheet());
	}

	/**
	 * @throws GenerateException
	 *
	 */
	private void koRundeErstellen() throws GenerateException {
		for (int grpCntr = 0; grpCntr < 10; grpCntr++) {
			TeamRangliste gruppeAusRanglist = ranglisten.gruppeAusRanglisteEinlesen(grpCntr, CadrageSheet.RANGLISTE_NACH_CADRAGE_SHEET);
			if (gruppeAusRanglist.isEmpty()) {
				break; // fertig keine weitere gruppen
			}
			vorrunden.vorRundenEinlesen(gruppeAusRanglist); // gegner eintragen
			KoRundeTeamPaarungen teamPaarungen = new KoRundeTeamPaarungen(gruppeAusRanglist);
			FormeSpielrunde spielRunde = teamPaarungen.generatSpielRunde();
			getSheetHelper().setActiveSheet(getXSpreadSheet());
			spielRundeInSheet.erstelleSpielRundeInSheet(grpCntr, getXSpreadSheet(), teamPaarungen, spielRunde);
		}
	}

	@Override
	protected IKonfigurationSheet getKonfigurationSheet() {
		// TODO Auto-generated method stub
		return null;
	}
}
