package de.petanqueturniermanager.spielerdb.export;

import de.petanqueturniermanager.spielerdb.SpielerDbException;

/**
 * Schreibt {@link SpielerDbExportData} in eines der unterstützten Formate.
 * Eine Implementierung pro {@link ExportFormat}.
 */
public interface SpielerDbExporter {

    /**
     * Schreibt die Daten gemäß dem Request.
     *
     * @param data    Bereits geladene Daten — nur Listen lesen, keine
     *                Repository-Calls mehr nötig
     * @param request Format, Scope, Filter, Zielpfad
     * @throws SpielerDbException bei IO-, Format- oder DB-Fehlern
     */
    void export(SpielerDbExportData data, ExportRequest request) throws SpielerDbException;
}
