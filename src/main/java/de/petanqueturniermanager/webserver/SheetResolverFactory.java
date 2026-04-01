package de.petanqueturniermanager.webserver;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

/**
 * Fabrik-Klasse zum Erstellen von {@link SheetResolver}-Instanzen aus Konfigurations-Strings.
 * <p>
 * Bekannte Schlüsselwörter (Groß-/Kleinschreibung egal) werden auf Metadaten-basierte
 * Resolver abgebildet. Alle anderen Werte erzeugen einen {@link StaticSheetResolver}
 * mit dem exakten Sheet-Tab-Namen.
 *
 * <p>Bekannte Schlüsselwörter:
 * <pre>
 *   SCHWEIZER_RANGLISTE      → Schweizer Rangliste (exakter Schlüssel)
 *   SCHWEIZER_SPIELRUNDE     → aktuellste Schweizer Spielrunde (höchste Nummer)
 *   SCHWEIZER_AKTUELLE_SPIELRUNDE → Schweizer Spielrunde (aktuell, höchste Nummer)
 *   SUPERMELEE_ENDRANGLISTE  → Supermêlée Endrangliste (exakter Schlüssel)
 *   SUPERMELEE_SPIELRUNDE    → aktuellste Supermelee-Spielrunde (höchste Nummer)
 *   SPIELTAG_RANGLISTE       → aktuellste Spieltag-Rangliste (höchste Nummer)
 *   SPIELTAG_TEILNEHMER      → Supermelee-Teilnehmerliste des aktiven Spieltags (aus KonfigurationSheet)
 *   SPIELTAG_AKTUELLE_SPIELRUNDE → aktuelle Spielrunde des aktiven Spieltags (Supermelee)
 *   TEILNEHMER               → Teilnehmer-Sheet (exakter Schlüssel, alle Turniersysteme außer Supermelee)
 *   SPIELTAG_ANMELDUNGEN     → aktuellste Spieltag-Anmeldungen (höchste Nummer, nur Supermelee)
 *   JGJ_RANGLISTE            → Jeder-gegen-Jeden Rangliste (exakter Schlüssel)
 *   JGJ_SPIELPLAN            → Jeder-gegen-Jeden Spielplan (exakter Schlüssel)
 *   LIGA_RANGLISTE           → Liga Rangliste (exakter Schlüssel)
 *   LIGA_SPIELPLAN           → Liga Spielplan (exakter Schlüssel)
 *   MAASTRICHTER_VORRUNDE    → aktuellste Maastrichter Vorrunde (höchste Nummer)
 *   KO_TURNIERBAUM           → aktuellster K.-O.-Turnierbaum (höchste Nummer)
 *   VORRUNDE_RANGLISTE       → Vorrunden-Ergebnisse Forme/KO-System (exakter Schlüssel)
 * </pre>
 */
public final class SheetResolverFactory {

    private SheetResolverFactory() {
    }

    /**
     * Zentrale Konfiguration aller bekannten Sheet-Typen und ihrer Resolver-Factories.
     * Die Iteration über diese Map bestimmt automatisch:
     * - Die verfügbaren Sheet-Typen für die Webserver-Konfiguration
     * - Die Resolver-Logik in der {@link #erstellen(String)}-Methode
     * <p>
     * Neue Typen hier hinzufügen → automatisch überall synchronisiert!
     */
    private static final Map<String, Supplier<SheetResolver>> RESOLVER_MAP = new LinkedHashMap<>();

    static {
        // Resolver-Factories definieren – Reihenfolge bestimmt ComboBox-Reihenfolge
        RESOLVER_MAP.put("SCHWEIZER_RANGLISTE", () ->
                new MetadatenSheetResolver(
                        SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_RANGLISTE,
                        I18n.get("webserver.resolver.schweizer.rangliste")));
        RESOLVER_MAP.put("SCHWEIZER_SPIELRUNDE", () ->
                new MetadatenPrefixSheetResolver(
                        SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_SPIELRUNDE_PREFIX,
                        SheetMetadataHelper.SCHLUESSEL_SUFFIX,
                        I18n.get("webserver.resolver.schweizer.spielrunde")));
        RESOLVER_MAP.put("SCHWEIZER_AKTUELLE_SPIELRUNDE", () ->
                new MetadatenPrefixSheetResolver(
                        SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_SPIELRUNDE_PREFIX,
                        SheetMetadataHelper.SCHLUESSEL_SUFFIX,
                        I18n.get("webserver.resolver.schweizer.aktuelle.spielrunde")));
        RESOLVER_MAP.put("SUPERMELEE_ENDRANGLISTE", () ->
                new MetadatenSheetResolver(
                        SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_ENDRANGLISTE,
                        I18n.get("webserver.resolver.supermelee.endrangliste")));
        RESOLVER_MAP.put("SUPERMELEE_SPIELRUNDE", () ->
                new MetadatenPrefixSheetResolver(
                        SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_SPIELRUNDE_PREFIX,
                        SheetMetadataHelper.SCHLUESSEL_SUFFIX,
                        I18n.get("webserver.resolver.supermelee.spielrunde")));
        RESOLVER_MAP.put("SPIELTAG_RANGLISTE", () ->
                new MetadatenPrefixSheetResolver(
                        SheetMetadataHelper.SCHLUESSEL_SPIELTAG_RANGLISTE_PREFIX,
                        SheetMetadataHelper.SCHLUESSEL_SPIELTAG_RANGLISTE_SUFFIX,
                        I18n.get("webserver.resolver.spieltag")));
        RESOLVER_MAP.put("SPIELTAG_TEILNEHMER", () ->
                new SupermeleeAktiverSpieltagSheetResolver());
        RESOLVER_MAP.put("SPIELTAG_AKTUELLE_SPIELRUNDE", () ->
                new SupermeleeAktiverSpielrundeSheetResolver());
        RESOLVER_MAP.put("TEILNEHMER", () ->
                new TeilnehmerSheetResolver(
                        SheetMetadataHelper.SCHLUESSEL_TEILNEHMER,
                        I18n.get("webserver.resolver.teilnehmer"),
                        new String[]{
                                "Schweizer Teilnehmer", "Schweizer Participants",
                                "Participants", "Participantes", "Deelnemers",
                                "JGJ Teilnehmer", "JGJ Participants",
                                "KO Teilnehmer", "KO Participants",
                                "Maastrichter Teilnehmer", "Maastrichter Participants",
                                "Kaskaden Teilnehmer", "Kaskaden Participants",
                                "Ligas Teilnehmer", "Ligas Participants"
                        }));
        RESOLVER_MAP.put("SPIELTAG_ANMELDUNGEN", () ->
                new MetadatenPrefixSheetResolver(
                        SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_ANMELDUNGEN_PREFIX,
                        SheetMetadataHelper.SCHLUESSEL_SUFFIX,
                        I18n.get("webserver.resolver.spieltag.anmeldungen")));
        RESOLVER_MAP.put("JGJ_RANGLISTE", () ->
                new MetadatenSheetResolver(
                        SheetMetadataHelper.SCHLUESSEL_JGJ_RANGLISTE,
                        I18n.get("webserver.resolver.jgj.rangliste")));
        RESOLVER_MAP.put("JGJ_SPIELPLAN", () ->
                new MetadatenSheetResolver(
                        SheetMetadataHelper.SCHLUESSEL_JGJ_SPIELPLAN,
                        I18n.get("webserver.resolver.jgj.spielplan")));
        RESOLVER_MAP.put("LIGA_RANGLISTE", () ->
                new MetadatenSheetResolver(
                        SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE,
                        I18n.get("webserver.resolver.liga.rangliste")));
        RESOLVER_MAP.put("LIGA_SPIELPLAN", () ->
                new MetadatenSheetResolver(
                        SheetMetadataHelper.SCHLUESSEL_LIGA_SPIELPLAN,
                        I18n.get("webserver.resolver.liga.spielplan")));
        RESOLVER_MAP.put("MAASTRICHTER_VORRUNDE", () ->
                new MetadatenPrefixSheetResolver(
                        SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX,
                        SheetMetadataHelper.SCHLUESSEL_SUFFIX,
                        I18n.get("webserver.resolver.maastrichter.vorrunde")));
        RESOLVER_MAP.put("KO_TURNIERBAUM", () ->
                new MetadatenPrefixSheetResolver(
                        SheetMetadataHelper.SCHLUESSEL_KO_TURNIERBAUM_PREFIX,
                        SheetMetadataHelper.SCHLUESSEL_SUFFIX,
                        I18n.get("webserver.resolver.ko.turnierbaum")));
        RESOLVER_MAP.put("VORRUNDE_RANGLISTE", () ->
                new MetadatenSheetResolver(
                        SheetMetadataHelper.SCHLUESSEL_FORME_VORRUNDEN,
                        I18n.get("webserver.resolver.vorrunde.rangliste")));
    }

    /**
     * Zentral definierte Liste aller bekannten Sheet-Typen für die Webserver-Konfiguration.
     * Diese wird automatisch aus RESOLVER_MAP generiert – garantiert synchronisiert!
     */
    public static final String[] SHEET_TYPEN = RESOLVER_MAP.keySet().toArray(String[]::new);

    /**
     * Default Sheet-Typ für neue Webserver-Konfigurationen.
     */
    public static final String DEFAULT_SHEET_TYP = "SPIELTAG_RANGLISTE";

    /**
     * Erzeugt einen Resolver passend zum Konfigurations-String.
     * <p>
     * Nutzt {@link #RESOLVER_MAP} zur Auflösung – garantiert Synchronisierung.
     *
     * @param configWert Wert aus der properties-Datei (z.B. "SCHWEIZER_RANGLISTE" oder "Meldeliste")
     * @return passender Resolver
     */
    public static SheetResolver erstellen(String configWert) {
        var key = configWert.toUpperCase();
        var factory = RESOLVER_MAP.get(key);
        if (factory != null) {
            return factory.get();
        }
        // Default: Exakter Sheet-Name
        return new StaticSheetResolver(configWert);
    }
}
