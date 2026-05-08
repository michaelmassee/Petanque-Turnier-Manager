package de.petanqueturniermanager.spielerdb.export;

/**
 * Filter über die zu exportierenden Datensätze. Aktuell nur
 * {@link AllExportFilter} (Voll-Export); künftig erweiterbar um weitere
 * permits (z.B. {@code ByLabel}, {@code ByVerein}) ohne API-Bruch der
 * Exporter.
 */
public sealed interface ExportFilter permits AllExportFilter {
}
