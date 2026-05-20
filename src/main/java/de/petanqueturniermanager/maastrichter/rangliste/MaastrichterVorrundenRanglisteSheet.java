/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.rangliste;

import java.util.List;
import java.util.Map;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.algorithmen.SchweizerTeamErgebnis;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.rangliste.RanglisteEingabeSignatur;
import de.petanqueturniermanager.helper.rangliste.SignaturQuellen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetUpdate;
import de.petanqueturniermanager.maastrichter.spielrunde.MaastrichterSpielrundeSheetNaechste;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Erstellt die Vorrunden-Rangliste für das Maastrichter Turniersystem.
 * Liest "N. Vorrunde"-Blätter statt "N. Spielrunde"-Blätter.
 */
public class MaastrichterVorrundenRanglisteSheet extends SchweizerRanglisteSheet {

	private Map<Integer, String> preservedGruppen = Map.of();

	public MaastrichterVorrundenRanglisteSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.MAASTRICHTER);
	}

	@Override
	public void doRun() throws GenerateException {
		// Gruppe-Zuweisungen vor NewSheet.forceCreate() retten, sonst gehen sie verloren.
		preservedGruppen = MaastrichterGruppenSpalteHelper.lesePreservedGruppen(this);
		super.doRun();
	}

	@Override
	protected int letzteAnzeigeSpalte() {
		return MaastrichterGruppenSpalteHelper.GRUPPE_SPALTE;
	}

	@Override
	protected void erweitereHeader(XSpreadsheet sheet, Integer headerColor) throws GenerateException {
		MaastrichterGruppenSpalteHelper.schreibeHeader(this, sheet, headerColor);
	}

	@Override
	protected void erweitereDaten(XSpreadsheet sheet, List<SchweizerTeamErgebnis> sortiert,
			int letzteZeile) throws GenerateException {
		MaastrichterGruppenSpalteHelper.schreibeDaten(this, sheet, sortiert, letzteZeile, preservedGruppen);
	}

	@Override
	protected SchweizerKonfigurationSheet initKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new MaastrichterKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	protected String getSpielrundenBasisName() {
		return MaastrichterSpielrundeSheetNaechste.SHEET_BASIS_NAME;
	}

	@Override
	protected String getRanglistenSheetName() {
		return SheetNamen.maastrichterVorrundenRangliste();
	}

	@Override
	protected String getMetadatenSchluessel() {
		return SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX;
	}

	@Override
	protected String getSpielrundenMetadatenSchluessel(int rundeNr) {
		return SheetMetadataHelper.schluesselMaastrichterVorrunde(rundeNr);
	}

	@Override
	protected SchweizerMeldeListeSheetUpdate erstelleMeldeListeSheet() {
		return new MaastrichterMeldeListeSheetUpdate(getWorkingSpreadsheet());
	}

	@Override
	protected RanglisteEingabeSignatur getRanglisteEingabeSignatur() {
		return new RanglisteEingabeSignatur(SignaturQuellen::fuerMaastrichter);
	}

}
