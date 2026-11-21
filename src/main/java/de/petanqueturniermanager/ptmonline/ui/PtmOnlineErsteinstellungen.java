/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.ui;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;

/**
 * Abfrage der PTM-Online-Schalter beim ersten Verbinden eines Dokuments: Live-Ansicht, Ergebnismeldung durch Spieler
 * und direktes Eintragen der Meldungen. Gefragt wird einmal je Dokument und nur nach den Schaltern, die das
 * Turniersystem kennt (Trip-Tête: nur Live-Ansicht). Diese Klasse enthält die Entscheidungen ohne UNO; den Dialog zeigt
 * {@link PtmOnlineErsteinstellungenDialog}.
 */
final class PtmOnlineErsteinstellungen {

    /** Die abgefragten Schalter in Anzeigereihenfolge. */
    static final List<String> SCHALTER = List.of(BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_LIVE_ANSICHT,
            BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_ERGEBNISMELDUNG,
            BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_ERGEBNISSE_DIREKT);

    private static final boolean SCHALTER_DEFAULT = true;

    private PtmOnlineErsteinstellungen() {}

    /** Gewählte Werte der Schalter; ein Schalter, den das System nicht kennt, wird nicht geschrieben. */
    record Auswahl(boolean liveAnsicht, boolean ergebnismeldung, boolean direktEintragen) {}

    /** Liest einen Boolean-Schalter des Dokuments. */
    @FunctionalInterface
    interface SchalterLeser {
        boolean lese(String schalter, boolean defaultWert);
    }

    /** Schreibt einen Boolean-Schalter ins Dokument. */
    @FunctionalInterface
    interface SchalterSchreiber {
        void schreibe(String schalter, boolean wert);
    }

    /** Die Schalter, die in der Property-Liste des Turniersystems vorkommen; leer ohne Liste. */
    static Set<String> vorhandeneSchalter(List<ConfigProperty<?>> konfigProperties) {
        Set<String> vorhanden = new LinkedHashSet<>();
        if (konfigProperties != null) {
            SCHALTER.stream()
                    .filter(schalter -> konfigProperties.stream().anyMatch(prop -> schalter.equals(prop.getKey())))
                    .forEach(vorhanden::add);
        }
        return vorhanden;
    }

    /** Gefragt wird nur einmal je Dokument und nur, wenn das System mindestens einen Schalter hat. */
    static boolean istAbzufragen(boolean schonAbgefragt, Set<String> vorhandeneSchalter) {
        return !schonAbgefragt && !vorhandeneSchalter.isEmpty();
    }

    /** Vorbelegung des Dialogs mit dem Stand des Dokuments (Default jeweils an). */
    static Auswahl vorbelegung(SchalterLeser leser) {
        return new Auswahl(leser.lese(BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_LIVE_ANSICHT, SCHALTER_DEFAULT),
                leser.lese(BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_ERGEBNISMELDUNG, SCHALTER_DEFAULT),
                leser.lese(BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_ERGEBNISSE_DIREKT, SCHALTER_DEFAULT));
    }

    /** Schreibt die Auswahl für die Schalter, die das Turniersystem kennt. */
    static void uebernehmen(Auswahl auswahl, Set<String> vorhandeneSchalter, SchalterSchreiber schreiber) {
        schreibeFallsVorhanden(BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_LIVE_ANSICHT, auswahl.liveAnsicht(),
                vorhandeneSchalter, schreiber);
        schreibeFallsVorhanden(BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_ERGEBNISMELDUNG, auswahl.ergebnismeldung(),
                vorhandeneSchalter, schreiber);
        schreibeFallsVorhanden(BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_ERGEBNISSE_DIREKT, auswahl.direktEintragen(),
                vorhandeneSchalter, schreiber);
    }

    private static void schreibeFallsVorhanden(String schalter, boolean wert, Set<String> vorhandeneSchalter,
            SchalterSchreiber schreiber) {
        if (vorhandeneSchalter.contains(schalter)) {
            schreiber.schreibe(schalter, wert);
        }
    }
}
