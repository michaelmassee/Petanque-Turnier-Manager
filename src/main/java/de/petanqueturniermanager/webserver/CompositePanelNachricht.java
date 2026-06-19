package de.petanqueturniermanager.webserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.petanqueturniermanager.timer.TimerState;

/**
 * SSE-Daten für ein einzelnes Panel in einem Composite View.
 * <p>
 * Enthält den vollständigen oder differenziellen Tabellenzustand eines Panels,
 * oder – bei URL-/Datei-Panels – die iframe-URL für die Anzeige,
 * oder – bei TIMER-Panels – den aktuellen Timer-Zustand.
 * Null-Felder werden von Gson nicht serialisiert.
 */
public record CompositePanelNachricht(
        int panelId,
        int zoom,
        int sichtbarerTabellenAnteil,
        String horizontalAusrichtung,
        String vertikalAusrichtung,
        boolean blattnameAnzeigen,
        String seitenTitel,
        Integer zeilen,
        Integer spalten,
        List<List<String>> gitter,
        List<ZelleModel> zellen,
        Map<Integer, Integer> spaltenBreiten,
        Map<Integer, Integer> zeilenHoehen,
        String kopfzeileLinks,
        String kopfzeileMitte,
        String kopfzeileRechts,
        String fusszeileLinks,
        String fusszeileMitte,
        String fusszeileRechts,
        int kopfZeilenAnzahl,
        String externeUrl,
        String timerAnzeige,
        String timerZustand,
        String timerBezeichnung,
        String timerHintergrundFarbe,
        Boolean timerSnoozed,
        StartseiteSseNachricht startseite,
        String hinweisTitel,
        String hinweisText) {

    /**
     * Erstellt eine vollständige Panel-Nachricht (init) aus einem TabelleModel.
     */
    static CompositePanelNachricht init(int panelId, TabelleModel modell, String seitenTitel, int zoom,
            int sichtbarerTabellenAnteil, String horizontalAusrichtung, String vertikalAusrichtung,
            boolean blattnameAnzeigen) {
        return new CompositePanelNachricht(
                panelId, zoom, sichtbarerTabellenAnteil, horizontalAusrichtung, vertikalAusrichtung, blattnameAnzeigen, seitenTitel,
                modell.getZeilen(), modell.getSpalten(),
                modell.getGitter(),
                new ArrayList<>(modell.getZellen().values()),
                modell.getSpaltenBreiten(), modell.getZeilenHoehen(),
                modell.getKopfzeileLinks(), modell.getKopfzeileMitte(), modell.getKopfzeileRechts(),
                modell.getFusszeileLinks(), modell.getFusszeileMitte(), modell.getFusszeileRechts(),
                modell.getKopfZeilenAnzahl(),
                null, null, null, null, null, null, null, null, null);
    }

    /**
     * Erstellt eine differenzielle Panel-Nachricht (diff) aus einem DiffModel.
     */
    static CompositePanelNachricht diff(int panelId, TabelleModel diffModell, String seitenTitel, int zoom,
            int sichtbarerTabellenAnteil, String horizontalAusrichtung, String vertikalAusrichtung,
            boolean blattnameAnzeigen) {
        return new CompositePanelNachricht(
                panelId, zoom, sichtbarerTabellenAnteil, horizontalAusrichtung, vertikalAusrichtung, blattnameAnzeigen, seitenTitel,
                diffModell.getZeilen(), diffModell.getSpalten(),
                diffModell.getGitter(),
                new ArrayList<>(diffModell.getZellen().values()),
                diffModell.getSpaltenBreiten(), diffModell.getZeilenHoehen(),
                diffModell.getKopfzeileLinks(), diffModell.getKopfzeileMitte(), diffModell.getKopfzeileRechts(),
                diffModell.getFusszeileLinks(), diffModell.getFusszeileMitte(), diffModell.getFusszeileRechts(),
                diffModell.getKopfZeilenAnzahl(),
                null, null, null, null, null, null, null, null, null);
    }

    /**
     * Erstellt eine URL-Panel-Nachricht: das Panel zeigt einen iframe mit der übergebenen URL.
     * <p>
     * Ausrichtungs-Felder werden als {@code null} übergeben — Gson lässt sie aus der Nachricht weg,
     * das Frontend behält den vorigen Wert (Reducer-Fallback).
     *
     * @param panelId    Panel-Index
     * @param externeUrl die anzuzeigende URL (validiert: nur http/https)
     */
    static CompositePanelNachricht url(int panelId, String externeUrl) {
        return new CompositePanelNachricht(
                panelId, 100, 100, null, null, false, "",
                null, null, null, null, null, null,
                null, null, null, null, null, null, 0,
                externeUrl, null, null, null, null, null, null, null, null);
    }

    /**
     * Erstellt eine Panel-Nachricht für eine lokale statische Datei. Der Browser erhält
     * nur den lokalen Composite-Endpunkt, nicht den Dateipfad auf dem Rechner.
     */
    static CompositePanelNachricht statischeDatei(int panelId, String dateiPfad) {
        String cacheKey = dateiPfad == null ? "" : Integer.toHexString(dateiPfad.hashCode());
        String url = "local-panel/" + panelId + "?v=" + cacheKey;
        return new CompositePanelNachricht(
                panelId, 100, 100, null, null, false, "",
                null, null, null, null, null, null,
                null, null, null, null, null, null, 0,
                url, null, null, null, null, null, null, null, null);
    }

    /**
     * Erstellt eine Panel-Nachricht für die Turnier-Startseite im Composite View.
     */
    static CompositePanelNachricht startseite(int panelId, StartseiteSseNachricht startseite) {
        return new CompositePanelNachricht(
                panelId, 100, 100, null, null, false, "",
                null, null, null, null, null, null,
                null, null, null, null, null, null, 0,
                null, null, null, null, null, null,
                startseite, null, null);
    }

    /**
     * Erstellt eine Panel-Hinweis-Nachricht (z.B. "Sheet nicht gefunden") für ein einzelnes Panel.
     * Andere Panels werden davon nicht beeinflusst.
     *
     * @param panelId Panel-Index
     * @param titel   Hinweis-Titel (i18n)
     * @param text    Hinweis-Text (i18n)
     */
    static CompositePanelNachricht fehlend(int panelId, String titel, String text) {
        return new CompositePanelNachricht(
                panelId, 100, 100, null, null, false, "",
                null, null, null, null, null, null,
                null, null, null, null, null, null, 0,
                null, null, null, null, null,
                null,
                null, titel, text);
    }

    /**
     * Erstellt eine TIMER-Panel-Nachricht mit dem aktuellen Timer-Zustand.
     *
     * @param panelId               Panel-Index
     * @param zoom                  Zoom-Faktor in %
     * @param horizontalAusrichtung horizontale Panel-Ausrichtung
     * @param vertikalAusrichtung   vertikale Panel-Ausrichtung
     * @param state                 aktueller Timer-Zustand
     */
    static CompositePanelNachricht timer(int panelId, int zoom, String horizontalAusrichtung,
            String vertikalAusrichtung, TimerState state) {
        return new CompositePanelNachricht(
                panelId, zoom, 100, horizontalAusrichtung, vertikalAusrichtung, false, "",
                null, null, null, null, null, null,
                null, null, null, null, null, null, 0,
                null,
                state.anzeige(), state.zustand().name(), state.bezeichnung(), state.hintergrundFarbe(),
                state.snoozed(), null, null, null);
    }
}
