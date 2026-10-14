/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.kompatibilitaet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;

/**
 * Ein Versionssprung, ab dem Dokumente älterer Plugin-Versionen angepasst werden müssen. Neue Schritte werden in
 * {@link DokumentKompatibilitaet#SCHRITTE} in aufsteigender Reihenfolge von {@link #abVersion()} eingetragen.
 * <p>
 * Ein Schritt ist entweder migrierbar – dann stellt {@link #migriere} ein altes Dokument um, bevor ein Lauf es
 * ändert – oder nicht: Dann wird ein Dokument, das ihn bräuchte, nicht mehr aktualisiert (nur noch angezeigt).
 */
public interface DokumentMigration {

	/** Dokumente, die mit einer älteren Version erstellt wurden, brauchen diesen Schritt (z.&nbsp;B. {@code 5.40.00}). */
	String abVersion();

	/** Ob {@link #migriere} ein altes Dokument umstellen kann; sonst wird es nicht mehr aktualisiert. */
	boolean istMigrierbar();

	/**
	 * Für Dokumente ohne gespeicherte Version: ob sie diesen Schritt brauchen, erkannt am Inhalt. Ohne erkennbares
	 * Merkmal gilt ein Dokument als aktuell (z.&nbsp;B. ein gerade neu angelegtes).
	 */
	default boolean istNoetigOhneVersion(WorkingSpreadsheet ws) {
		return false;
	}

	/**
	 * Stellt das Dokument um. Läuft im SheetRunner vor dem Kommando, noch ohne Blattschutz-Scope; muss idempotent
	 * sein, weil die gespeicherte Version erst danach angehoben wird.
	 *
	 * @throws UnsupportedOperationException wenn der Schritt nicht {@linkplain #istMigrierbar() migrierbar} ist
	 */
	default void migriere(WorkingSpreadsheet ws) throws GenerateException {
		throw new UnsupportedOperationException(getClass().getSimpleName() + " ist nicht migrierbar");
	}
}
