/*
 * Erstellung: 2026-05-11 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.spieltagrangliste;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.rangliste.RanglisteUpdateHelper;
import de.petanqueturniermanager.supermelee.SpielTagNr;

/**
 * Aktualisiert eine Supermelee Spieltag-Rangliste ohne das Sheet neu zu
 * erstellen. Schreibt nur den Datenbereich (Spielrunden-Ergebnisse, Summen,
 * NG-Flag) per Block-Write neu; Header, Spaltenbreiten und Metadaten bleiben.
 * <p>
 * Fallback: existiert das Sheet noch nicht, wird {@link SpieltagRanglisteSheet#doRun()}
 * zum vollständigen Erstaufbau aufgerufen.
 * <p>
 * Reentrancy: ein Update pro Dokument gleichzeitig. Trifft während eines Laufs
 * ein weiteres Event ein, wird das Dirty-Flag gesetzt und der laufende Lauf
 * läuft genau einmal nochmal durch — so gehen keine Refreshes verloren, ohne
 * dass parallele Threads dasselbe Sheet bearbeiten.
 */
public class SpieltagRanglisteSheetUpdate extends SpieltagRanglisteSheet {

	private static final Logger logger = LogManager.getLogger(SpieltagRanglisteSheetUpdate.class);

	private static final ConcurrentHashMap<XSpreadsheetDocument, ReentrancyState> STATES = new ConcurrentHashMap<>();

	public SpieltagRanglisteSheetUpdate(WorkingSpreadsheet workingSpreadsheet, SpielTagNr spieltagNr) {
		super(workingSpreadsheet, spieltagNr);
	}

	@Override
	public void doRun() throws GenerateException {
		XSpreadsheetDocument doc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		ReentrancyState state = STATES.computeIfAbsent(doc, k -> new ReentrancyState());
		if (!state.running.compareAndSet(false, true)) {
			state.dirty.set(true);
			logger.debug("RanglisteUpdate bereits aktiv – Dirty-Rerun vorgemerkt");
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
		// SpielTagNr aus dem Refresh-Parameter ins Delegate übertragen –
		// sonst NPE in getSpieltagNr() (delegate.spielTagNr wird sonst nur
		// via generate() gesetzt, und das überspringen wir im Update-Pfad).
		SpielTagNr spielTagNr = getSpielTagFuerRefresh();
		if (spielTagNr == null) {
			spielTagNr = getKonfigurationSheet().getAktiveSpieltag();
		}
		setSpieltagNr(spielTagNr);

		XSpreadsheet sheet = getXSpreadSheet();
		if (sheet == null) {
			logger.debug("Spieltag-Rangliste-Sheet fehlt – vollständiger Erstaufbau");
			new SpieltagRanglisteSheet(getWorkingSpreadsheet(), getSpieltagNr()).doRun();
			return;
		}
		processBoxinfo("processbox.rangliste.aktualisieren");

		int anzSpielRunden = getAktuelleSpielrundeSheet().countNumberOfSpielRundenSheets(getSpieltagNr());
		if (anzSpielRunden < 1) {
			return;
		}

		int spielerAnzahl = getSpielerSpalte().getSpielerNrList().size();
		RanglisteUpdateHelper.loescheDatenzeilen(this, sheet, spielerAnzahl);

		berechnungUndSchreiben(sheet, anzSpielRunden);
		getxCalculatable().calculate();
		getRangListeSorter().doSort();
		getRangListeSpalte().upDateRanglisteSpalte();
	}

	private static final class ReentrancyState {
		final AtomicBoolean running = new AtomicBoolean(false);
		final AtomicBoolean dirty = new AtomicBoolean(false);
	}
}
