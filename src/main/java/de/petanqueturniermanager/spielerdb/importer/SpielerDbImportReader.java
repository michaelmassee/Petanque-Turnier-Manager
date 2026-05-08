package de.petanqueturniermanager.spielerdb.importer;

import de.petanqueturniermanager.spielerdb.SpielerDbException;

/**
 * Liest eine Import-Quelle in {@link ImportRohdaten} ein. Eine Implementierung
 * pro nicht-SQLite-Format (CSV, JSON, Calc). Validierung übernimmt
 * {@code SpielerDbImportValidator} im nächsten Schritt.
 */
public interface SpielerDbImportReader {
    ImportRohdaten read(ImportRequest request) throws SpielerDbException;
}
