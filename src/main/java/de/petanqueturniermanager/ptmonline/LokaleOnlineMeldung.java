/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import org.jspecify.annotations.Nullable;

/**
 * Eine lokale Meldung, wie sie beim Rundenstart nach PTM-Online gemeldet wird.
 *
 * @param zeile1Basiert   Zeile im Sync-Ziel (Meldeliste bzw. Mêlée-Anmeldung), trägt die lokale PTM-Online-ID
 * @param teilnahme       zu meldende Teilnahme; nur {@link OnlineTeilnahme#AKTIV} wird bei fehlender
 *                        Online-Zuordnung neu angelegt
 * @param seedingPosition Wert für {@code seeding_position}; {@code null} löscht ihn online
 */
record LokaleOnlineMeldung(int zeile1Basiert, OnlineTeilnahme teilnahme, @Nullable Integer seedingPosition) {
}
