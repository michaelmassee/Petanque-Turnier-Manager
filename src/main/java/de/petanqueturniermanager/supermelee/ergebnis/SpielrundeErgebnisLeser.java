/*
 * Erstellung: 2026-05-11 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.ergebnis;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheetKonstanten;

/**
 * Liest die vertikalen Spielergebnisse aller Spielrunde-Sheets eines Spieltags
 * per Block-Read in den Speicher und stellt sie als Map zur Verfügung. Ersetzt
 * die VLOOKUP-/COUNTA-Formeln in der Spieltag-Rangliste.
 *
 * <p>Pro Runde wird der Bereich
 * {@code [ERSTE_SPALTE_VERTIKALE_ERGEBNISSE .. SPALTE_VERTIKALE_ERGEBNISSE_MINUS]}
 * über alle Datenzeilen einmalig via {@link RangeHelper#getDataFromRange()}
 * geladen. Spielernummern und Punktwerte werden über
 * {@link de.petanqueturniermanager.helper.sheet.rangedata.CellData#getIntVal(int)}
 * normalisiert (akzeptiert {@code Double}, parsbaren {@code String}, leer ⇒ -1).
 */
public final class SpielrundeErgebnisLeser {

	private static final Logger logger = LogManager.getLogger(SpielrundeErgebnisLeser.class);

	/** Maximal zu lesende Datenzeilen pro Spielrunde-Sheet. */
	private static final int MAX_DATENZEILEN = 1000;

	/** Eingetragenes Ergebnis eines Spielers in einer Runde. */
	public record RundenErgebnis(int plus, int minus) { }

	private final WorkingSpreadsheet workingSpreadsheet;
	private final SpielTagNr spielTag;

	public SpielrundeErgebnisLeser(WorkingSpreadsheet workingSpreadsheet, SpielTagNr spielTag) {
		this.workingSpreadsheet = checkNotNull(workingSpreadsheet);
		this.spielTag = checkNotNull(spielTag);
	}

	/**
	 * Liest alle {@code anzRunden} Spielrunden des Spieltags.
	 *
	 * @param anzRunden Anzahl Spielrunden ({@code 1..anzRunden}); werden positiv erwartet
	 * @return Aggregat mit Ergebnis-Maps pro Runde sowie der Menge nicht gespielter Runden
	 */
	public SpieltagErgebnisse lese(int anzRunden) throws GenerateException {
		SpieltagErgebnisse ergebnisse = new SpieltagErgebnisse();
		var xDoc = workingSpreadsheet.getWorkingSpreadsheetDocument();

		for (int rundeNr = 1; rundeNr <= anzRunden; rundeNr++) {
			String schluessel = SheetMetadataHelper.schluesselSupermeleeSpielrunde(spielTag.getNr(), rundeNr);
			String legacyName = SheetNamen.supermeleeSpielrunde(spielTag.getNr(), rundeNr);
			XSpreadsheet sheet = SheetMetadataHelper.findeSheetUndHeile(xDoc, schluessel, legacyName);
			if (sheet == null) {
				logger.warn("Spielrunde-Sheet {}.{} nicht gefunden", spielTag.getNr(), rundeNr);
				ergebnisse.nichtGespielteRunden.add(rundeNr);
				continue;
			}
			// Vertikale Ergebnis-Spalten T/U sind formel-getrieben aus den horizontalen
			// Ergebnissen. Bei leerer Runde liefern sie 0/0 (Formelresultat für leeres H);
			// das ist mathematisch konsistent mit der alten VLOOKUP-Auswertung.
			Map<Integer, RundenErgebnis> proSpieler = leseRunde(sheet);
			ergebnisse.proRunde.put(rundeNr, proSpieler);
			if (!hatHorizontaleErgebnisse(sheet)) {
				// Runde gilt für die NG-Markierung als "nicht gespielt" (keine Einträge
				// in der horizontalen H-Spalte) – unabhängig von den 0-Werten der Vertikalen.
				ergebnisse.nichtGespielteRunden.add(rundeNr);
			}
		}
		return ergebnisse;
	}

	private Map<Integer, RundenErgebnis> leseRunde(XSpreadsheet sheet) throws GenerateException {
		Map<Integer, RundenErgebnis> proSpieler = new HashMap<>();

		int ersteSpalte = SpielrundeSheetKonstanten.ERSTE_SPALTE_VERTIKALE_ERGEBNISSE;
		int letzteSpalte = SpielrundeSheetKonstanten.SPALTE_VERTIKALE_ERGEBNISSE_MINUS;
		int ersteZeile = SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE;
		int letzteZeile = ersteZeile + MAX_DATENZEILEN - 1;

		RangePosition range = RangePosition.from(ersteSpalte, ersteZeile, letzteSpalte, letzteZeile);
		RangeData daten = RangeHelper
				.from(sheet, workingSpreadsheet.getWorkingSpreadsheetDocument(), range)
				.getDataFromRange();

		for (RowData row : daten) {
			if (row.isEmpty()) {
				continue;
			}
			int spielerNr = row.get(0).getIntVal(-1);
			if (spielerNr <= 0) {
				continue;
			}
			int plus = row.size() > 1 ? row.get(1).getIntVal(-1) : -1;
			int minus = row.size() > 2 ? row.get(2).getIntVal(-1) : -1;
			if (plus < 0 || minus < 0) {
				// Spieler war zugeteilt, aber kein Ergebnis erfasst → für Spieler-Sicht
				// "nicht gespielt"; nicht in Map aufnehmen.
				continue;
			}
			proSpieler.put(spielerNr, new RundenErgebnis(plus, minus));
		}
		return proSpieler;
	}

	private boolean hatHorizontaleErgebnisse(XSpreadsheet sheet) throws GenerateException {
		int spalte = SpielrundeSheetKonstanten.ERSTE_SPALTE_ERGEBNISSE;
		int ersteZeile = SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE;
		int letzteZeile = ersteZeile + MAX_DATENZEILEN - 1;
		RangeData spalteH = RangeHelper
				.from(sheet, workingSpreadsheet.getWorkingSpreadsheetDocument(),
						RangePosition.from(spalte, ersteZeile, spalte, letzteZeile))
				.getDataFromRange();
		for (RowData row : spalteH) {
			if (row.isEmpty()) {
				continue;
			}
			Object data = row.get(0).getData();
			if (data instanceof Number) {
				return true;
			}
			if (data instanceof String s && !s.isBlank()) {
				return true;
			}
		}
		return false;
	}

	/** Aggregat aller Spielrunden-Ergebnisse eines Spieltags. */
	public static final class SpieltagErgebnisse {

		private final Map<Integer, Map<Integer, RundenErgebnis>> proRunde = new HashMap<>();
		private final Set<Integer> nichtGespielteRunden = new HashSet<>();

		SpieltagErgebnisse() { }

		/**
		 * Ergebnis eines Spielers in einer Runde, falls vorhanden.
		 *
		 * @return leeres Optional wenn der Spieler in dieser Runde nicht eingetragen ist
		 *         oder kein Ergebnis erfasst wurde
		 */
		public Optional<RundenErgebnis> ergebnis(int rundeNr, int spielerNr) {
			Map<Integer, RundenErgebnis> runde = proRunde.get(rundeNr);
			if (runde == null) {
				return Optional.empty();
			}
			return Optional.ofNullable(runde.get(spielerNr));
		}

		/** True, wenn die Runde keine Ergebnisse enthält (oder das Sheet fehlt). */
		public boolean istRundeNichtGespielt(int rundeNr) {
			return nichtGespielteRunden.contains(rundeNr);
		}
	}
}
