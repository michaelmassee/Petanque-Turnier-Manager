package de.petanqueturniermanager.webserver;

import static de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem.FORMULEX;
import static de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem.JGJ;
import static de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem.KASKADE;
import static de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem.KEIN;
import static de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem.KO;
import static de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem.LIGA;
import static de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem.MAASTRICHTER;
import static de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem.POULE;
import static de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem.SCHWEIZER;
import static de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem.SUPERMELEE;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

/**
 * Fabrik-Klasse zum Erstellen von {@link SheetResolver}-Instanzen aus Konfigurations-Strings.
 * <p>
 * Da pro Dokument genau ein Turniersystem aktiv ist, sind die Sheet-Rollen
 * <b>generisch und system-agnostisch</b> konfiguriert: ein einziger Schlüssel pro Rolle
 * ({@code RANGLISTE}, {@code AKTUELLE_SPIELRUNDE}, {@code MELDELISTE}, {@code CHECKIN},
 * {@code TEILNEHMER}) löst über {@link AktivesSystemSheetResolver} auf das jeweils aktive
 * System auf. Nur Blätter, die ein Anwender parallel zeigen will (Maastrichter-Finalrunden,
 * Kaskaden-Felder, Poule-K.O., Forme-Cadrage/K.O.-Gruppe, Supermêlée-Endrangliste), bleiben
 * als system-gefilterte Spezial-Einträge erhalten.
 * <p>
 * Unbekannte Werte erzeugen einen {@link StaticSheetResolver} mit dem exakten Sheet-Tab-Namen.
 * Alte, system-spezifische Schlüssel aus früheren Plugin-Versionen werden über
 * {@link #LEGACY_ALIASE} auf die generischen Rollen abgebildet.
 */
public final class SheetResolverFactory {

    private SheetResolverFactory() {
    }

    /**
     * Definition eines Sheet-Typs: Resolver-Factory + die Systeme, für die der Typ in der
     * Konfigurations-ComboBox angeboten wird. Leere Menge = generisch (alle Systeme).
     */
    public record SheetTypDefinition(Supplier<SheetResolver> factory, Set<TurnierSystem> systeme) {
        boolean giltFuer(TurnierSystem aktiv) {
            return systeme.isEmpty() || (aktiv != null && systeme.contains(aktiv));
        }
    }

    /**
     * Zentrale Konfiguration aller bekannten Sheet-Typen. Die Iterationsreihenfolge bestimmt
     * die ComboBox-Reihenfolge.
     */
    private static final Map<String, SheetTypDefinition> RESOLVER_MAP = new LinkedHashMap<>();

    /**
     * Veraltete Resolver-Schlüssel, die per Migration auf einen aktuellen (generischen)
     * Schlüssel abgebildet werden. So bleiben persistierte User-Konfigurationen aus älteren
     * Plugin-Versionen funktionsfähig, ohne dass die alten Schlüssel in der ComboBox erscheinen.
     */
    private static final Map<String, String> LEGACY_ALIASE = Map.ofEntries(
            Map.entry("SPIELTAG_TEILNEHMER", "TEILNEHMER"),
            // → RANGLISTE
            Map.entry("SCHWEIZER_RANGLISTE", "RANGLISTE"),
            Map.entry("JGJ_RANGLISTE", "RANGLISTE"),
            Map.entry("LIGA_RANGLISTE", "RANGLISTE"),
            Map.entry("FORMULEX_RANGLISTE", "RANGLISTE"),
            Map.entry("KASKADE_GRUPPENRANGLISTE", "RANGLISTE"),
            Map.entry("POULE_VORRUNDEN_RANGLISTE", "RANGLISTE"),
            Map.entry("VORRUNDE_RANGLISTE", "RANGLISTE"),
            Map.entry("SPIELTAG_RANGLISTE", "RANGLISTE"),
            // → AKTUELLE_SPIELRUNDE
            Map.entry("SCHWEIZER_SPIELRUNDE", "AKTUELLE_SPIELRUNDE"),
            Map.entry("SCHWEIZER_AKTUELLE_SPIELRUNDE", "AKTUELLE_SPIELRUNDE"),
            Map.entry("FORMULEX_SPIELRUNDE", "AKTUELLE_SPIELRUNDE"),
            Map.entry("MAASTRICHTER_VORRUNDE", "AKTUELLE_SPIELRUNDE"),
            Map.entry("SUPERMELEE_SPIELRUNDE", "AKTUELLE_SPIELRUNDE"),
            Map.entry("SPIELTAG_AKTUELLE_SPIELRUNDE", "AKTUELLE_SPIELRUNDE"),
            Map.entry("KO_TURNIERBAUM", "AKTUELLE_SPIELRUNDE"),
            Map.entry("KASKADE_RUNDE", "AKTUELLE_SPIELRUNDE"),
            Map.entry("JGJ_SPIELPLAN", "AKTUELLE_SPIELRUNDE"),
            Map.entry("LIGA_SPIELPLAN", "AKTUELLE_SPIELRUNDE"),
            Map.entry("POULE_VORRUNDE", "AKTUELLE_SPIELRUNDE"),
            // → CHECKIN
            Map.entry("SPIELTAG_ANMELDUNGEN", "CHECKIN"));

    static {
        // ── Generische Rollen-Schlüssel (alle Systeme) ──────────────────────────
        RESOLVER_MAP.put("RANGLISTE", generisch(() ->
                new AktivesSystemSheetResolver(ranglisteDelegates(),
                        I18n.get("webserver.resolver.rangliste"))));
        RESOLVER_MAP.put("AKTUELLE_SPIELRUNDE", generisch(() ->
                new AktivesSystemSheetResolver(spielrundeDelegates(),
                        I18n.get("webserver.resolver.aktuelle.spielrunde"))));
        RESOLVER_MAP.put("MELDELISTE", generisch(() ->
                new AktivesSystemSheetResolver(meldelisteDelegates(),
                        I18n.get("webserver.resolver.meldeliste"))));
        RESOLVER_MAP.put("CHECKIN", generisch(() ->
                new AktivesSystemSheetResolver(checkinDelegates(),
                        I18n.get("webserver.resolver.checkin"))));
        RESOLVER_MAP.put("TEILNEHMER", generisch(() ->
                new TeilnehmerSheetResolver(I18n.get("webserver.resolver.teilnehmer"))));

        // ── System-spezifische Spezial-Blätter (echte Parallel-Sub-Blätter) ─────
        RESOLVER_MAP.put("SUPERMELEE_ENDRANGLISTE", fuer(() ->
                new MetadatenSheetResolver(
                        SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_ENDRANGLISTE,
                        I18n.get("webserver.resolver.supermelee.endrangliste")), SUPERMELEE));
        RESOLVER_MAP.put("MAASTRICHTER_FINALRUNDE_A", fuer(() ->
                new MetadatenSheetResolver(
                        SheetMetadataHelper.schluesselMaastrichterFinalrunde("A"),
                        I18n.get("webserver.resolver.maastrichter.finalrunde.a")), MAASTRICHTER));
        RESOLVER_MAP.put("MAASTRICHTER_FINALRUNDE_B", fuer(() ->
                new MetadatenSheetResolver(
                        SheetMetadataHelper.schluesselMaastrichterFinalrunde("B"),
                        I18n.get("webserver.resolver.maastrichter.finalrunde.b")), MAASTRICHTER));
        RESOLVER_MAP.put("MAASTRICHTER_FINALRUNDE_C", fuer(() ->
                new MetadatenSheetResolver(
                        SheetMetadataHelper.schluesselMaastrichterFinalrunde("C"),
                        I18n.get("webserver.resolver.maastrichter.finalrunde.c")), MAASTRICHTER));
        RESOLVER_MAP.put("MAASTRICHTER_FINALRUNDE_D", fuer(() ->
                new MetadatenSheetResolver(
                        SheetMetadataHelper.schluesselMaastrichterFinalrunde("D"),
                        I18n.get("webserver.resolver.maastrichter.finalrunde.d")), MAASTRICHTER));
        RESOLVER_MAP.put("FORME_CADRAGE", fuer(() ->
                new MetadatenSheetResolver(
                        SheetMetadataHelper.SCHLUESSEL_FORME_CADRAGE,
                        I18n.get("webserver.resolver.forme.cadrage")), KO));
        RESOLVER_MAP.put("FORME_KO_GRUPPE", fuer(() ->
                new MetadatenSheetResolver(
                        SheetMetadataHelper.SCHLUESSEL_FORME_KO_GRUPPE,
                        I18n.get("webserver.resolver.forme.ko.gruppe")), KO));
        RESOLVER_MAP.put("POULE_KO_A", fuer(() ->
                new MetadatenSheetResolver(
                        SheetMetadataHelper.schluesselPouleKo("A"),
                        I18n.get("webserver.resolver.poule.ko.a")), POULE));
        RESOLVER_MAP.put("POULE_KO_B", fuer(() ->
                new MetadatenSheetResolver(
                        SheetMetadataHelper.schluesselPouleKo("B"),
                        I18n.get("webserver.resolver.poule.ko.b")), POULE));
        RESOLVER_MAP.put("KASKADE_FELD_A", fuer(() ->
                new MetadatenSheetResolver(
                        SheetMetadataHelper.schluesselKaskadenFeld("A"),
                        I18n.get("webserver.resolver.kaskade.feld.a")), KASKADE));
        RESOLVER_MAP.put("KASKADE_FELD_B", fuer(() ->
                new MetadatenSheetResolver(
                        SheetMetadataHelper.schluesselKaskadenFeld("B"),
                        I18n.get("webserver.resolver.kaskade.feld.b")), KASKADE));
        RESOLVER_MAP.put("KASKADE_FELD_C", fuer(() ->
                new MetadatenSheetResolver(
                        SheetMetadataHelper.schluesselKaskadenFeld("C"),
                        I18n.get("webserver.resolver.kaskade.feld.c")), KASKADE));
        RESOLVER_MAP.put("KASKADE_FELD_D", fuer(() ->
                new MetadatenSheetResolver(
                        SheetMetadataHelper.schluesselKaskadenFeld("D"),
                        I18n.get("webserver.resolver.kaskade.feld.d")), KASKADE));
    }

    /**
     * Zentral definierte Liste aller bekannten Sheet-Typen für die Webserver-Konfiguration.
     */
    public static final String[] SHEET_TYPEN = RESOLVER_MAP.keySet().toArray(String[]::new);

    /**
     * Default Sheet-Typ für neue Webserver-Konfigurationen.
     */
    public static final String DEFAULT_SHEET_TYP = "RANGLISTE";

    /**
     * Liefert die für das aktive Turniersystem relevanten Sheet-Typen: alle generischen plus
     * die system-spezifischen Spezial-Einträge des aktiven Systems. Bei {@code null} oder
     * {@link TurnierSystem#KEIN} wird die vollständige Liste zurückgegeben.
     */
    public static String[] sheetTypenFuer(TurnierSystem aktiv) {
        if (aktiv == null || aktiv == KEIN) {
            return SHEET_TYPEN.clone();
        }
        return RESOLVER_MAP.entrySet().stream()
                .filter(e -> e.getValue().giltFuer(aktiv))
                .map(Map.Entry::getKey)
                .toArray(String[]::new);
    }

    /**
     * Erzeugt einen Resolver passend zum Konfigurations-String.
     *
     * @param configWert Wert aus der properties-Datei (z.B. "RANGLISTE" oder "Meldeliste")
     * @return passender Resolver
     */
    public static SheetResolver erstellen(String configWert) {
        var key = configWert.toUpperCase();
        var aufgeloesterKey = LEGACY_ALIASE.getOrDefault(key, key);
        var definition = RESOLVER_MAP.get(aufgeloesterKey);
        if (definition != null) {
            return definition.factory().get();
        }
        // Default: Exakter Sheet-Name
        return new StaticSheetResolver(configWert);
    }

    // ── Hilfen ───────────────────────────────────────────────────────────────

    private static SheetTypDefinition generisch(Supplier<SheetResolver> factory) {
        return new SheetTypDefinition(factory, Set.of());
    }

    private static SheetTypDefinition fuer(Supplier<SheetResolver> factory, TurnierSystem... systeme) {
        return new SheetTypDefinition(factory, Set.of(systeme));
    }

    private static Map<TurnierSystem, SheetResolver> ranglisteDelegates() {
        var name = I18n.get("webserver.resolver.rangliste");
        var m = new EnumMap<TurnierSystem, SheetResolver>(TurnierSystem.class);
        m.put(SCHWEIZER, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_RANGLISTE, name));
        m.put(JGJ, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_JGJ_RANGLISTE, name));
        m.put(LIGA, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE, name));
        m.put(FORMULEX, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_FORMULEX_RANGLISTE, name));
        m.put(KASKADE, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_KASKADE_GRUPPENRANGLISTE, name));
        m.put(POULE, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDEN_RANGLISTE, name));
        m.put(KO, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_FORME_VORRUNDEN, name));
        m.put(SUPERMELEE, new MetadatenPrefixSheetResolver(
                SheetMetadataHelper.SCHLUESSEL_SPIELTAG_RANGLISTE_PREFIX,
                SheetMetadataHelper.SCHLUESSEL_SPIELTAG_RANGLISTE_SUFFIX, name));
        // MAASTRICHTER: kein Single-Ranking-Blatt → bewusst nicht hinterlegt.
        return m;
    }

    private static Map<TurnierSystem, SheetResolver> spielrundeDelegates() {
        var name = I18n.get("webserver.resolver.aktuelle.spielrunde");
        var m = new EnumMap<TurnierSystem, SheetResolver>(TurnierSystem.class);
        m.put(SCHWEIZER, new MetadatenPrefixSheetResolver(
                SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_SPIELRUNDE_PREFIX,
                SheetMetadataHelper.SCHLUESSEL_SUFFIX, name));
        m.put(FORMULEX, new MetadatenPrefixSheetResolver(
                SheetMetadataHelper.SCHLUESSEL_FORMULEX_SPIELRUNDE_PREFIX, "", name));
        m.put(MAASTRICHTER, new MetadatenPrefixSheetResolver(
                SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX,
                SheetMetadataHelper.SCHLUESSEL_SUFFIX, name));
        m.put(KO, new MetadatenPrefixSheetResolver(
                SheetMetadataHelper.SCHLUESSEL_KO_TURNIERBAUM_PREFIX,
                SheetMetadataHelper.SCHLUESSEL_SUFFIX, name));
        m.put(KASKADE, new MetadatenPrefixSheetResolver(
                SheetMetadataHelper.SCHLUESSEL_KASKADE_RUNDE_PREFIX,
                SheetMetadataHelper.SCHLUESSEL_SUFFIX, name));
        m.put(SUPERMELEE, new SupermeleeAktiverSpielrundeSheetResolver());
        m.put(POULE, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDE, name));
        m.put(JGJ, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_JGJ_SPIELPLAN, name));
        m.put(LIGA, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_LIGA_SPIELPLAN, name));
        return m;
    }

    private static Map<TurnierSystem, SheetResolver> meldelisteDelegates() {
        var name = I18n.get("webserver.resolver.meldeliste");
        var m = new EnumMap<TurnierSystem, SheetResolver>(TurnierSystem.class);
        m.put(SUPERMELEE, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_MELDELISTE, name));
        m.put(LIGA, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_LIGA_MELDELISTE, name));
        m.put(MAASTRICHTER, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_MELDELISTE, name));
        m.put(SCHWEIZER, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELDELISTE, name));
        m.put(JGJ, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_JGJ_MELDELISTE, name));
        m.put(KO, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_KO_MELDELISTE, name));
        m.put(POULE, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_POULE_MELDELISTE, name));
        m.put(KASKADE, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_KASKADE_MELDELISTE, name));
        m.put(FORMULEX, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_FORMULEX_MELDELISTE, name));
        return m;
    }

    private static Map<TurnierSystem, SheetResolver> checkinDelegates() {
        var name = I18n.get("webserver.resolver.checkin");
        var m = new EnumMap<TurnierSystem, SheetResolver>(TurnierSystem.class);
        m.put(SCHWEIZER, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_CHECKIN_LISTE, name));
        m.put(JGJ, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_JGJ_CHECKIN_LISTE, name));
        m.put(KO, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_KO_CHECKIN_LISTE, name));
        m.put(KASKADE, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_KASKADE_CHECKIN_LISTE, name));
        m.put(FORMULEX, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_FORMULEX_CHECKIN_LISTE, name));
        m.put(MAASTRICHTER, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_CHECKIN_LISTE, name));
        m.put(POULE, new MetadatenSheetResolver(SheetMetadataHelper.SCHLUESSEL_POULE_CHECKIN_LISTE, name));
        m.put(SUPERMELEE, new MetadatenPrefixSheetResolver(
                SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_ANMELDUNGEN_PREFIX,
                SheetMetadataHelper.SCHLUESSEL_SUFFIX, name));
        // LIGA: kein Checkin-Blatt.
        return m;
    }
}
