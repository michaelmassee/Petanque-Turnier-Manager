/*
 * Erstellung: 2026-05-11 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.endrangliste;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.rangliste.RanglisteUpdateHelper;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzRegistry;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;
import de.petanqueturniermanager.toolbar.TurnierModus;

/**
 * Aktualisiert die Supermelee Endrangliste ohne Sheet-Neuaufbau: nur der
 * Datenbereich (Spieltagsblöcke, End-Summen, Anzahl Spieltage,
 * Streichspieltag) wird per Block-Write neu geschrieben.
 * <p>
 * Fallback: existiert das Sheet noch nicht, wird {@link EndranglisteSheet#doRun()}
 * für den vollständigen Erstaufbau aufgerufen.
 * <p>
 * Reentrancy: pro Dokument höchstens ein Update gleichzeitig. Trifft während
 * eines laufenden Updates ein weiteres Event ein, wird das Dirty-Flag gesetzt
 * und der laufende Lauf wiederholt sich genau einmal – keine verlorenen
 * Refreshes, keine parallelen Threads auf demselben Sheet.
 */
public class EndranglisteSheetUpdate extends EndranglisteSheet {

	private static final Logger logger = LogManager.getLogger(EndranglisteSheetUpdate.class);

	private static final ConcurrentHashMap<XSpreadsheetDocument, ReentrancyState> STATES = new ConcurrentHashMap<>();

	public EndranglisteSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	public void doRun() throws GenerateException {
		XSpreadsheetDocument doc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		ReentrancyState state = STATES.computeIfAbsent(doc, k -> new ReentrancyState());
		if (!state.running.compareAndSet(false, true)) {
			state.dirty.set(true);
			logger.debug("EndranglisteUpdate bereits aktiv – Dirty-Rerun vorgemerkt");
			return;
		}
		try {
			do {
				state.dirty.set(false);
				updateIntern();
			} while (state.dirty.get());
		} finally {
			state.running.set(false);
		}
	}

	private void updateIntern() throws GenerateException {
		if (TurnierModus.get().istAktiv()) {
			BlattschutzRegistry.fuer(TurnierSystem.SUPERMELEE)
					.ifPresent(k -> BlattschutzManager.get().entsperren(k, getWorkingSpreadsheet()));
		}
		try {
			XSpreadsheet sheet = getXSpreadSheet();
			if (sheet == null) {
				logger.debug("Endrangliste-Sheet fehlt – vollständiger Erstaufbau");
				new EndranglisteSheet(getWorkingSpreadsheet()).doRun();
				return;
			}
			processBoxinfo("processbox.rangliste.aktualisieren");

			int anzSpieltage = getAnzahlSpieltage();
			if (anzSpieltage < 2) {
				return;
			}

			int spielerAnzahl = getSpielerSpalte().getSpielerNrList().size();
			RanglisteUpdateHelper.loescheDatenzeilen(this, sheet, spielerAnzahl);

			berechnungUndSchreiben(sheet);
			getxCalculatable().calculate();
			getRangListeSorter().doSort();
			getRangListeSpalte().upDateRanglisteSpalte();
		} finally {
			if (TurnierModus.get().istAktiv()) {
				BlattschutzRegistry.fuer(TurnierSystem.SUPERMELEE)
						.ifPresent(k -> BlattschutzManager.get().schuetzen(k, getWorkingSpreadsheet()));
			}
		}
	}

	private static final class ReentrancyState {
		final AtomicBoolean running = new AtomicBoolean(false);
		final AtomicBoolean dirty = new AtomicBoolean(false);
	}
}
