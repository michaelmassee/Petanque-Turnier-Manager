/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.rangliste;

import java.util.List;
import java.util.Map;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.algorithmen.schweizer.SchweizerTeamErgebnis;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetUpdate;
import de.petanqueturniermanager.maastrichter.spielrunde.MaastrichterSpielrundeSheetNaechste;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheetUpdate;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Aktualisiert die Maastrichter Vorrunden-Rangliste ohne das Sheet neu zu erstellen.
 *
 * @see SchweizerRanglisteSheetUpdate
 */
public class MaastrichterVorrundenRanglisteSheetUpdate extends SchweizerRanglisteSheetUpdate {

	private Map<Integer, String> preservedGruppen = Map.of();

	public MaastrichterVorrundenRanglisteSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.MAASTRICHTER);
	}

	@Override
	public void doRun() throws GenerateException {
		// Gruppe-Werte vor dem Daten-Refresh sichern; nach dem Sortieren wird in
		// erweitereDaten(...) per TeamNr-Map wieder eingeschrieben.
		preservedGruppen = MaastrichterGruppenSpalteHelper.lesePreservedGruppen(this);
		super.doRun();
	}

	@Override
	protected int letzteAnzeigeSpalte() {
		return MaastrichterGruppenSpalteHelper.GRUPPE_SPALTE;
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
	protected SchweizerRanglisteSheet erstelleNeuAufbauSheet() {
		return new MaastrichterVorrundenRanglisteSheet(getWorkingSpreadsheet());
	}

	/**
	 * Schreibt die Gruppe-Zuweisungen (TeamNr → Buchstabe) direkt in die GRUPPE-Spalte
	 * der bereits bestehenden Vorrunden-Rangliste. Andere Spalten bleiben unverändert.
	 * Wird nach dem Erstellen der KO-Gruppen ({@link de.petanqueturniermanager.maastrichter.korunde.KoGruppeABSheet})
	 * aufgerufen.
	 */
	public void schreibeGruppenZuweisungen(Map<Integer, String> teamNrZuGruppe) throws GenerateException {
		XSpreadsheet sheet = getXSpreadSheet();
		if (sheet == null || teamNrZuGruppe.isEmpty()) {
			return;
		}
		MaastrichterGruppenSpalteHelper.schreibeGruppenZuweisungen(this, sheet, teamNrZuGruppe);
	}
}
