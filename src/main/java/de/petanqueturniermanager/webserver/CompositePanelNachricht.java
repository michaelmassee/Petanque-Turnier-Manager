package de.petanqueturniermanager.webserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SSE-Daten für ein einzelnes Panel in einem Composite View.
 * <p>
 * Enthält den vollständigen oder differenziellen Tabellenzustand eines Panels.
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
        int kopfZeilenAnzahl) {

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
                modell.getKopfZeilenAnzahl());
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
                diffModell.getKopfZeilenAnzahl());
    }
}
