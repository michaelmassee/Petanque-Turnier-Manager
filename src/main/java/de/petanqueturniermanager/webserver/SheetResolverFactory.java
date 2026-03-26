package de.petanqueturniermanager.webserver;

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
 *   SCHWEIZER_RANGLISTE     → Schweizer Rangliste (exakter Schlüssel)
 *   SCHWEIZER_SPIELRUNDE    → aktuellste Schweizer Spielrunde (höchste Nummer)
 *   SUPERMELEE_ENDRANGLISTE → Supermêlée Endrangliste (exakter Schlüssel)
 *   SPIELTAG_RANGLISTE      → aktuellste Spieltag-Rangliste (höchste Nummer)
 *   SPIELTAG_TEILNEHMER     → Supermelee-Teilnehmerliste des aktiven Spieltags (aus KonfigurationSheet)
 *   SPIELTAG_ANMELDUNGEN    → aktuellste Spieltag-Anmeldungen (höchste Nummer, nur Supermelee)
 *   TEILNEHMER              → Teilnehmer-Sheet (exakter Schlüssel, alle Turniersysteme außer Supermelee)
 *   JGJ_RANGLISTE           → Jeder-gegen-Jeden Rangliste (exakter Schlüssel)
 *   LIGA_RANGLISTE          → Liga Rangliste (exakter Schlüssel)
 *   MAASTRICHTER_VORRUNDE   → aktuellste Maastrichter Vorrunde (höchste Nummer)
 *   KO_TURNIERBAUM          → aktuellster K.-O.-Turnierbaum (höchste Nummer)
 * </pre>
 */
public final class SheetResolverFactory {

    private SheetResolverFactory() {
    }

    /**
     * Erzeugt einen Resolver passend zum Konfigurations-String.
     *
     * @param configWert Wert aus der properties-Datei (z.B. "SCHWEIZER_RANGLISTE" oder "Meldeliste")
     * @return passender Resolver
     */
    public static SheetResolver erstellen(String configWert) {
        return switch (configWert.toUpperCase()) {
            case "SCHWEIZER_RANGLISTE" -> new MetadatenSheetResolver(
                    SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_RANGLISTE,
                    I18n.get("webserver.resolver.schweizer.rangliste"));
            case "SCHWEIZER_SPIELRUNDE" -> new MetadatenPrefixSheetResolver(
                    SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_SPIELRUNDE_PREFIX,
                    SheetMetadataHelper.SCHLUESSEL_SUFFIX,
                    I18n.get("webserver.resolver.schweizer.spielrunde"));
            case "SUPERMELEE_ENDRANGLISTE" -> new MetadatenSheetResolver(
                    SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_ENDRANGLISTE,
                    I18n.get("webserver.resolver.supermelee.endrangliste"));
            case "SPIELTAG_RANGLISTE" -> new MetadatenPrefixSheetResolver(
                    SheetMetadataHelper.SCHLUESSEL_SPIELTAG_RANGLISTE_PREFIX,
                    SheetMetadataHelper.SCHLUESSEL_SPIELTAG_RANGLISTE_SUFFIX,
                    I18n.get("webserver.resolver.spieltag"));
            case "SPIELTAG_TEILNEHMER" -> new SupermeleeAktiverSpieltagSheetResolver();
            case "TEILNEHMER" -> new MetadatenSheetResolver(
                    SheetMetadataHelper.SCHLUESSEL_TEILNEHMER,
                    I18n.get("webserver.resolver.teilnehmer"));
            case "SPIELTAG_ANMELDUNGEN" -> new MetadatenPrefixSheetResolver(
                    SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_ANMELDUNGEN_PREFIX,
                    SheetMetadataHelper.SCHLUESSEL_SUFFIX,
                    I18n.get("webserver.resolver.spieltag.anmeldungen"));
            case "JGJ_RANGLISTE" -> new MetadatenSheetResolver(
                    SheetMetadataHelper.SCHLUESSEL_JGJ_RANGLISTE,
                    I18n.get("webserver.resolver.jgj.rangliste"));
            case "LIGA_RANGLISTE" -> new MetadatenSheetResolver(
                    SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE,
                    I18n.get("webserver.resolver.liga.rangliste"));
            case "MAASTRICHTER_VORRUNDE" -> new MetadatenPrefixSheetResolver(
                    SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX,
                    SheetMetadataHelper.SCHLUESSEL_SUFFIX,
                    I18n.get("webserver.resolver.maastrichter.vorrunde"));
            case "KO_TURNIERBAUM" -> new MetadatenPrefixSheetResolver(
                    SheetMetadataHelper.SCHLUESSEL_KO_TURNIERBAUM_PREFIX,
                    SheetMetadataHelper.SCHLUESSEL_SUFFIX,
                    I18n.get("webserver.resolver.ko.turnierbaum"));
            default -> new StaticSheetResolver(configWert);
        };
    }
}
