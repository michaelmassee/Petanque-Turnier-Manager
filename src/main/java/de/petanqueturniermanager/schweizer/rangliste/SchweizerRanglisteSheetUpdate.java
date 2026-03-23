package de.petanqueturniermanager.schweizer.rangliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Aktualisiert die Schweizer Rangliste ohne das Sheet neu zu erstellen.
 * <p>
 * Im Gegensatz zu {@link SchweizerRanglisteSheet} (vollständiger Neuaufbau mit
 * {@code NewSheet.forceCreate()}) schreibt diese Klasse nur den Datenbereich neu.
 * Header, Spaltenbreiten und Metadaten bleiben unverändert.
 * <p>
 * Wird vom {@link de.petanqueturniermanager.helper.rangliste.RanglisteRefreshListener}
 * verwendet, um bei einem Tab-Wechsel zur Rangliste nur die Daten zu aktualisieren –
 * ohne das Sheet zu löschen und neu anzulegen.
 * <p>
 * Fallback: Wenn das Rangliste-Sheet noch nicht existiert, wird automatisch
 * {@link SchweizerRanglisteSheet#doRun()} ausgelöst (vollständiger Erstaufbau).
 */
public class SchweizerRanglisteSheetUpdate extends SchweizerRanglisteSheet {

	private static final Logger logger = LogManager.getLogger(SchweizerRanglisteSheetUpdate.class);

	public SchweizerRanglisteSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	protected SchweizerRanglisteSheetUpdate(WorkingSpreadsheet workingSpreadsheet, TurnierSystem ts) {
		super(workingSpreadsheet, ts);
	}

	@Override
	public void doRun() throws GenerateException {
		XSpreadsheet sheet = getXSpreadSheet();
		if (sheet == null) {
			logger.debug("RanglisteUpdate: Sheet '{}' nicht vorhanden – vollständiger Erstaufbau",
					getRanglistenSheetName());
			erstelleNeuAufbauSheet().doRun();
			return;
		}

		logger.debug("RanglisteUpdate START – Thread='{}'", Thread.currentThread().getName());
		processBoxinfo("processbox.rangliste.aktualisieren");

		SchweizerMeldeListeSheetUpdate meldeliste = erstelleMeldeListeSheet();
		TeamMeldungen aktiveMeldungen = meldeliste.getAktiveMeldungen();
		if (aktiveMeldungen == null || aktiveMeldungen.size() == 0) {
			processBoxinfo("processbox.abbruch");
			return;
		}

		loeSchalteDatenzeilen(sheet, aktiveMeldungen.size());
		berechnungUndSchreiben(sheet, meldeliste, aktiveMeldungen);

		// setActiveSheet nur wenn über SheetRunner.run() aufgerufen (isRunning=true).
		if (SheetRunner.isRunning()) {
			getSheetHelper().setActiveSheet(sheet);
		}
		logger.debug("RanglisteUpdate ENDE – Thread='{}'", Thread.currentThread().getName());
	}

	/**
	 * Erzeugt das Sheet-Objekt für den vollständigen Erstaufbau.
	 * Überschreibbar für Subklassen (z.B. Maastrichter).
	 */
	protected SchweizerRanglisteSheet erstelleNeuAufbauSheet() {
		return new SchweizerRanglisteSheet(getWorkingSpreadsheet());
	}

	/**
	 * Löscht veraltete Datenzeilen wenn sich die Teamanzahl verringert hat.
	 * Bei gleichbleibender oder größerer Teamanzahl passiert nichts, da
	 * {@code insertDatenAlsWerte} die vorhandenen Zellen überschreibt.
	 */
	private void loeSchalteDatenzeilen(XSpreadsheet sheet, int neueTeamAnzahl) throws GenerateException {
		int bisherigeLetzte = sucheLetzteZeileMitSpielerNummer();
		int neueLetzte = ERSTE_DATEN_ZEILE + neueTeamAnzahl - 1;
		if (bisherigeLetzte > neueLetzte) {
			RangeHelper.from(sheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
					RangePosition.from(TEAM_NR_SPALTE, neueLetzte + 1, VALIDATE_SPALTE, bisherigeLetzte))
					.clearRange();
			logger.debug("loeSchalteDatenzeilen: Zeilen {}-{} gelöscht", neueLetzte + 1, bisherigeLetzte);
		}
	}
}
