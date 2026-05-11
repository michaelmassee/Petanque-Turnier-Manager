/*
 * Erstellung: 2026-05-11 / Michael Massee
 */
package de.petanqueturniermanager.kaskade.spielrunde;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.algorithmen.KaskadenFeldBelegung;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzRegistry;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;
import de.petanqueturniermanager.toolbar.TurnierModus;

/**
 * Aktualisiert die Kaskade-Gruppenrangliste ohne Sheet-Neuaufbau: schreibt nur
 * den Datenbereich (Pos + Team-Nr pro Gruppe) neu. Header, Spaltenbreiten und
 * Metadaten bleiben unverändert.
 * <p>
 * Fallback: existiert das Sheet noch nicht, wird {@link KaskadeGruppenRanglisteSheet#doRun()}
 * zum vollständigen Erstaufbau aufgerufen.
 * <p>
 * Reentrancy: pro Dokument höchstens ein Update gleichzeitig. Während eines
 * Laufs eintreffende Events lösen genau einen Dirty-Rerun aus.
 */
public class KaskadeGruppenRanglisteSheetUpdate extends KaskadeGruppenRanglisteSheet {

	private static final Logger logger = LogManager.getLogger(KaskadeGruppenRanglisteSheetUpdate.class);

	private static final ConcurrentHashMap<XSpreadsheetDocument, ReentrancyState> STATES = new ConcurrentHashMap<>();

	public KaskadeGruppenRanglisteSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	public void doRun() throws GenerateException {
		XSpreadsheetDocument doc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		ReentrancyState state = STATES.computeIfAbsent(doc, k -> new ReentrancyState());
		if (!state.running.compareAndSet(false, true)) {
			state.dirty.set(true);
			logger.debug("KaskadeGruppenrangliste-Update bereits aktiv – Dirty-Rerun vorgemerkt");
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
			BlattschutzRegistry.fuer(TurnierSystem.KASKADE)
					.ifPresent(k -> BlattschutzManager.get().entsperren(k, getWorkingSpreadsheet()));
		}
		try {
			XSpreadsheet sheet = getXSpreadSheet();
			if (sheet == null) {
				logger.debug("Kaskade-Gruppenrangliste-Sheet fehlt – vollständiger Erstaufbau");
				new KaskadeGruppenRanglisteSheet(getWorkingSpreadsheet()).doRun();
				return;
			}
			processBoxinfo("processbox.rangliste.aktualisieren");

			var plan = ermittlePlan();
			List<KaskadenFeldBelegung> belegungen = ermittleBelegungen(plan);
			if (belegungen.isEmpty()) {
				return;
			}
			aktualisiereDatenblock(belegungen);
			getxCalculatable().calculate();
		} finally {
			if (TurnierModus.get().istAktiv()) {
				BlattschutzRegistry.fuer(TurnierSystem.KASKADE)
						.ifPresent(k -> BlattschutzManager.get().schuetzen(k, getWorkingSpreadsheet()));
			}
		}
	}

	private static final class ReentrancyState {
		final AtomicBoolean running = new AtomicBoolean(false);
		final AtomicBoolean dirty = new AtomicBoolean(false);
	}
}
