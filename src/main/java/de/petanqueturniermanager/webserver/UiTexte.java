package de.petanqueturniermanager.webserver;

import java.util.LinkedHashMap;
import java.util.Map;

import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Liefert die übersetzten Frontend-UI-Texte, die kein einzelner View selbst über
 * Daten-Felder transportieren kann (z.&nbsp;B. die Verbindungsverlust-Anzeige).
 * Wird in jede Init-Nachricht (Composite & Startseite) eingebettet — Frontend
 * mergt die Werte in {@code state.i18n} und nutzt sie bei Bedarf.
 */
public final class UiTexte {

    private UiTexte() {}

    /**
     * Aktuelle UI-Texte für das Frontend. Wird bei jeder Init-Nachricht neu aufgebaut,
     * damit Locale-Wechsel ohne Neustart des Webservers wirken.
     */
    public static Map<String, String> aktuelle() {
        Map<String, String> texte = new LinkedHashMap<>();
        texte.put("verbindungGetrennt", I18n.get("frontend.verbindung.getrennt"));
        texte.put("tonNichtAktivTitel", I18n.get("frontend.ton.nicht.aktiv.titel"));
        texte.put("tonNichtAktivHinweis", I18n.get("frontend.ton.nicht.aktiv.hinweis"));
        texte.put("slaves", I18n.get("frontend.slaves.label"));
        return texte;
    }
}
