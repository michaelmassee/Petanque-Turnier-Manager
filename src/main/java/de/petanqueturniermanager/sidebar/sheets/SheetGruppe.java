package de.petanqueturniermanager.sidebar.sheets;

import java.util.List;
import java.util.Optional;

import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Ordnet PTM-Metadaten-Schlüssel einer Sidebar-Gruppe zu und definiert die Anzeigereihenfolge
 * der Blätter innerhalb der Gruppe.
 */
public enum SheetGruppe {

    SUPERMELEE("enum.turniersystem.supermelee", List.of(
            "__PTM_SUPERMELEE_MELDELISTE__",
            "__PTM_SUPERMELEE_TEAMS__",
            "__PTM_SUPERMELEE_ANMELDUNGEN_",
            "__PTM_SUPERMELEE_TEILNEHMER_",
            "__PTM_SUPERMELEE_SPIELRUNDE_PLAN_",
            "__PTM_SUPERMELEE_SPIELRUNDE_",
            "__PTM_SPIELTAG_",
            "__PTM_SUPERMELEE_ENDRANGLISTE__"
    )),

    SCHWEIZER("enum.turniersystem.schweizer", List.of(
            "__PTM_SCHWEIZER_MELDELISTE__",
            "__PTM_SCHWEIZER_SPIELRUNDE_",
            "__PTM_SCHWEIZER_RANGLISTE__"
    )),

    JGJ("enum.turniersystem.jgj", List.of(
            "__PTM_JGJ_MELDELISTE__",
            "__PTM_JGJ_SPIELPLAN__",
            "__PTM_JGJ_RANGLISTE__",
            "__PTM_JGJ_DIREKTVERGLEICH__"
    )),

    LIGA("enum.turniersystem.liga", List.of(
            "__PTM_LIGA_MELDELISTE__",
            "__PTM_LIGA_SPIELPLAN__",
            "__PTM_LIGA_RANGLISTE__",
            "__PTM_LIGA_DIREKTVERGLEICH__"
    )),

    KO("enum.turniersystem.ko", List.of(
            "__PTM_KO_MELDELISTE__",
            "__PTM_KO_TURNIERBAUM_"
    )),

    KASKADE("enum.turniersystem.kaskade", List.of(
            "__PTM_KASKADE_MELDELISTE__",
            "__PTM_KASKADE_RUNDE_",
            "__PTM_KASKADE_FELD_",
            "__PTM_KASKADE_GRUPPENRANGLISTE__"
    )),

    MAASTRICHTER("enum.turniersystem.maastrichter", List.of(
            "__PTM_MAASTRICHTER_MELDELISTE__",
            "__PTM_MAASTRICHTER_VORRUNDE_",
            "__PTM_MAASTRICHTER_FINALRUNDE_"
    )),

    POULE("enum.turniersystem.poule", List.of(
            "__PTM_POULE_MELDELISTE__",
            "__PTM_POULE_VORRUNDE__",
            "__PTM_POULE_SPIELPLAN_",
            "__PTM_POULE_VORRUNDEN_RANGLISTE__",
            "__PTM_POULE_KO_"
    )),

    FORME("enum.turniersystem.forme", List.of(
            "__PTM_FORME_VORRUNDEN__",
            "__PTM_FORME_CADRAGE__",
            "__PTM_FORME_KO_GRUPPE__"
    )),

    ALLGEMEIN("sidebar.sheets.gruppe.allgemein", List.of(
            "__PTM_TEILNEHMER__"
    ));

    private final String i18nKey;
    private final List<String> praefixa;

    SheetGruppe(String i18nKey, List<String> praefixa) {
        this.i18nKey = i18nKey;
        this.praefixa = praefixa;
    }

    /** Gibt die übersetzte Gruppenbezeichnung zurück. */
    public String getAnzeigeBezeichnung() {
        return I18n.get(i18nKey);
    }

    /**
     * Ermittelt die Gruppe für einen Metadaten-Schlüssel.
     * Prüft alle Gruppen in Enum-Reihenfolge; gibt die erste Übereinstimmung zurück.
     */
    public static Optional<SheetGruppe> fuerSchluessel(String schluessel) {
        for (var gruppe : values()) {
            for (var praefix : gruppe.praefixa) {
                if (schluessel.startsWith(praefix)) {
                    return Optional.of(gruppe);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Liefert die Sortierposition eines Schlüssels innerhalb dieser Gruppe.
     * Basiert auf dem Index des passenden Präfixes (kleiner = weiter vorne).
     */
    public int reihenfolgeDesSchluessels(String schluessel) {
        for (int i = 0; i < praefixa.size(); i++) {
            if (schluessel.startsWith(praefixa.get(i))) {
                return i;
            }
        }
        return praefixa.size();
    }
}
