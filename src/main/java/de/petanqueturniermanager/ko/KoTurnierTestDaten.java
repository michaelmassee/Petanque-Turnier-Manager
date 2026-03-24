/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetTestDaten;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellt ein vollständiges K.-O.-Testturnier ohne Dialoge:<br>
 * Konfiguration + Meldeliste (mit Testdaten) + Turnierbaum (mit Bahnnummern).<br>
 * <br>
 * Varianten:
 * <ul>
 *   <li>8 Teams – Viertelfinale → Halbfinale → Finale</li>
 *   <li>16 Teams – Achtelfinale → … → Finale</li>
 *   <li>10 Teams – Cadrage (Teams 7–10) → Achtelfinale → … → Finale</li>
 * </ul>
 */
public class KoTurnierTestDaten extends SheetRunner implements ISheet, MeldeListeKonstanten {

	private final int anzTeams;
	private final KoMeldeListeSheetTestDaten meldeListeTestDaten;
	private final KoTurnierbaumSheet turnierbaumSheet;

	public KoTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet, int anzTeams) {
		super(workingSpreadsheet, TurnierSystem.KO, "KO-Turnier-Testdaten");
		this.anzTeams = anzTeams;
		meldeListeTestDaten = new KoMeldeListeSheetTestDaten(workingSpreadsheet, anzTeams);
		turnierbaumSheet = new KoTurnierbaumSheet(workingSpreadsheet);
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return getSheetHelper().findByName(SheetNamen.meldeliste());
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	protected KoKonfigurationSheet getKonfigurationSheet() {
		return new KoKonfigurationSheet(getWorkingSpreadsheet());
	}

	@Override
	protected void doRun() throws GenerateException {
		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.KO)
				.prefix(getLogPrefix()).validate()) {
			return;
		}

		// 1. Konfiguration
		var konfig = getKonfigurationSheet();
		konfig.setSpielbaumSpielbahn(SpielrundeSpielbahn.N);
		// Bei 16 Teams: 2 Gruppen à 8 erstellen (demonstriert Gruppen-Feature)
		if (anzTeams == 16) {
			konfig.setGruppenGroesse(8);
		}

		// 2. Meldeliste mit Testdaten füllen
		meldeListeTestDaten.erstelleMeldelisteWithTestdaten();

		// 3. Turnierbaum ohne Dialog erstellen
		turnierbaumSheet.erstelleTurnierbaumOhneDialog();
	}

}
