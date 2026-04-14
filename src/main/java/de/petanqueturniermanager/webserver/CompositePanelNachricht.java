package de.petanqueturniermanager.webserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.petanqueturniermanager.timer.TimerState;

/**
 * SSE-Daten für ein einzelnes Panel in einem Composite View.
 * <p>
 * Enthält den vollständigen oder differenziellen Tabellenzustand eines Panels,
 * oder – bei URL-Panels – die externe URL für die iframe-Anzeige,
 * oder – bei TIMER-Panels – den aktuellen Timer-Zustand.
 * Null-Felder werden von Gson nicht serialisiert.
 */
public record CompositePanelNachricht(
        int panelId,
        int zoom,
        boolean zentriert,
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
        String timerHintergrundFarbe) {

    /**
     * Erstellt eine vollständige Panel-Nachricht (init) aus einem TabelleModel.
     */
    static CompositePanelNachricht init(int panelId, TabelleModel modell, String seitenTitel, int zoom, boolean zentriert, boolean blattnameAnzeigen) {
        return new CompositePanelNachricht(
                panelId, zoom, zentriert, blattnameAnzeigen, seitenTitel,
                modell.getZeilen(), modell.getSpalten(),
                modell.getGitter(),
                new ArrayList<>(modell.getZellen().values()),
                modell.getSpaltenBreiten(), modell.getZeilenHoehen(),
                modell.getKopfzeileLinks(), modell.getKopfzeileMitte(), modell.getKopfzeileRechts(),
                modell.getFusszeileLinks(), modell.getFusszeileMitte(), modell.getFusszeileRechts(),
                modell.getKopfZeilenAnzahl(),
                null, null, null, null, null);
    }

    /**
     * Erstellt eine differenzielle Panel-Nachricht (diff) aus einem DiffModel.
     */
    static CompositePanelNachricht diff(int panelId, TabelleModel diffModell, String seitenTitel, int zoom, boolean zentriert, boolean blattnameAnzeigen) {
        return new CompositePanelNachricht(
                panelId, zoom, zentriert, blattnameAnzeigen, seitenTitel,
                diffModell.getZeilen(), diffModell.getSpalten(),
                diffModell.getGitter(),
                new ArrayList<>(diffModell.getZellen().values()),
                diffModell.getSpaltenBreiten(), diffModell.getZeilenHoehen(),
                diffModell.getKopfzeileLinks(), diffModell.getKopfzeileMitte(), diffModell.getKopfzeileRechts(),
                diffModell.getFusszeileLinks(), diffModell.getFusszeileMitte(), diffModell.getFusszeileRechts(),
                diffModell.getKopfZeilenAnzahl(),
                null, null, null, null, null);
    }

    /**
     * Erstellt eine URL-Panel-Nachricht: das Panel zeigt einen iframe mit der übergebenen URL.
     *
     * @param panelId    Panel-Index
     * @param externeUrl die anzuzeigende URL (validiert: nur http/https)
     */
    static CompositePanelNachricht url(int panelId, String externeUrl) {
        return new CompositePanelNachricht(
                panelId, 100, false, false, "",
                null, null, null, null, null, null,
                null, null, null, null, null, null, 0,
                externeUrl, null, null, null, null);
    }

    /**
     * Erstellt eine TIMER-Panel-Nachricht mit dem aktuellen Timer-Zustand.
     *
     * @param panelId   Panel-Index
     * @param zoom      Zoom-Faktor in %
     * @param zentriert ob der Inhalt horizontal zentriert wird
     * @param state     aktueller Timer-Zustand
     */
    static CompositePanelNachricht timer(int panelId, int zoom, boolean zentriert, TimerState state) {
        return new CompositePanelNachricht(
                panelId, zoom, zentriert, false, "",
                null, null, null, null, null, null,
                null, null, null, null, null, null, 0,
                null,
                state.anzeige(), state.zustand().name(), state.bezeichnung(), state.hintergrundFarbe());
    }
}
