package de.petanqueturniermanager.spielerdb.export;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

/**
 * Versionierte Meta-Information jedes Exports. Wird in JSON als Top-Level-
 * Block geschrieben und ermöglicht später schemakompatible Re-Importe.
 *
 * @param version    Schema-Version, beginnt bei 1
 * @param exportedAt Erstellzeit des Exports (UTC)
 * @param appVersion Plugin-Version, die exportiert hat — kann {@code null}
 *                   sein, wenn die Versionsabfrage fehlschlägt
 */
public record ExportMeta(int version, Instant exportedAt, @Nullable String appVersion) {

    /** Aktuelle Schema-Version aller Export-Formate. */
    public static final int AKTUELLE_VERSION = 1;
}
