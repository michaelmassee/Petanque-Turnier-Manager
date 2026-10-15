/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheetsync;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

/**
 * Ein Ausgabe-Blatt, das im Modus {@link NachziehModus#FEHLENDE_ANLEGEN} vor einem Export sicher vorhanden und
 * aktuell sein muss.
 *
 * @param persistenzSchluessel Schlüssel des gespeicherten Signatur-Hashs (wie beim Tab-Wechsel)
 * @param signatur             Signatur über die Eingabedaten
 * @param ausgabeFehlt         mindestens ein zugehöriges Blatt fehlt – Aufbau unabhängig vom Hash
 * @param aktion               Aufbau/Aktualisierung; läuft im bereits laufenden SheetRunner
 */
public record SheetSyncAusgabe(String persistenzSchluessel, EingabeSignatur signatur, boolean ausgabeFehlt,
		Aktion aktion) {

	public SheetSyncAusgabe {
		checkNotNull(persistenzSchluessel);
		checkNotNull(signatur);
		checkNotNull(aktion);
	}

	@FunctionalInterface
	public interface Aktion {
		void ausfuehren() throws GenerateException;
	}

	/** Liefert die Ausgaben eines Registry-Eintrags für ein Dokument. */
	@FunctionalInterface
	public interface Lieferant {
		List<SheetSyncAusgabe> ermittle(WorkingSpreadsheet ws) throws GenerateException;
	}

	/** {@code true}, wenn eines der per Named-Range-Schlüssel identifizierten Blätter fehlt. */
	public static boolean fehltEines(XSpreadsheetDocument xDoc, String... schluessel) {
		for (String s : schluessel) {
			if (SheetMetadataHelper.findeSheet(xDoc, s).isEmpty()) {
				return true;
			}
		}
		return false;
	}
}
