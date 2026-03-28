package de.petanqueturniermanager.webserver;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Repräsentiert den vollständigen Zustand eines Sheets als vorberechnetes Grid.
 * <p>
 * Das {@link #getGitter()} ist vorberechnet: Jede Position enthält entweder eine
 * Zell-ID (Master-Zelle) oder {@code null} (Merge-Slave → kein {@code <td>} rendern).
 * <p>
 * React iteriert das Gitter zeilenweise:
 * <pre>
 *   gitter[row][col] = "cell-r-c" → &lt;Cell data={zellen["cell-r-c"]} /&gt;
 *   gitter[row][col] = null       → kein &lt;td&gt;
 * </pre>
 * Spaltenbreiten und Zeilenhöhen sind in 1/100 mm (UNO-Einheit);
 * das Frontend rechnet mit {@code Math.round(val / 37.795)} in Pixel um.
 * <p>
 * {@link #startZeile} und {@link #startSpalte} geben die absolute Position (0-basiert)
 * der ersten gerenderten Zelle im LibreOffice-Sheet an. Damit lässt sich erkennen,
 * ob der Druckbereich verschoben wurde, auch wenn Größe und Inhalt unverändert sind.
 * <p>
 * Kopf- und Fußzeile (je links/mitte/rechts) stammen aus dem PageStyle des Sheets;
 * {@code null} bedeutet: kein Inhalt oder deaktiviert.
 */
public class TabelleModel {

    private final int zeilen;
    private final int spalten;
    /** Absolute Startzeile des gerenderten Bereichs im Sheet (0-basiert). */
    private final int startZeile;
    /** Absolute Startspalte des gerenderten Bereichs im Sheet (0-basiert). */
    private final int startSpalte;
    /**
     * Anzahl der Kopfzeilen am Anfang des Gitters (aus den Wiederholungszeilen des Druckbereichs).
     * 0 = keine Kopfzeilen definiert.
     */
    private final int kopfZeilenAnzahl;
    /** gitter[row][col] = ZelleId oder null (Merge-Slave). */
    private final List<List<String>> gitter;
    /** Nur Master-Zellen. */
    private final Map<String, ZelleModel> zellen;
    /** Spaltenindex (0-basiert) → Breite in 1/100 mm. */
    private final Map<Integer, Integer> spaltenBreiten;
    /** Zeilenindex (0-basiert) → Höhe in 1/100 mm. */
    private final Map<Integer, Integer> zeilenHoehen;
    /** Seitenkopfzeile links, null = nicht vorhanden. */
    private final String kopfzeileLinks;
    /** Seitenkopfzeile mitte, null = nicht vorhanden. */
    private final String kopfzeileMitte;
    /** Seitenkopfzeile rechts, null = nicht vorhanden. */
    private final String kopfzeileRechts;
    /** Seitenfußzeile links, null = nicht vorhanden. */
    private final String fusszeileLinks;
    /** Seitenfußzeile mitte, null = nicht vorhanden. */
    private final String fusszeileMitte;
    /** Seitenfußzeile rechts, null = nicht vorhanden. */
    private final String fusszeileRechts;

    public TabelleModel(
            int zeilen,
            int spalten,
            List<List<String>> gitter,
            Map<String, ZelleModel> zellen,
            Map<Integer, Integer> spaltenBreiten,
            Map<Integer, Integer> zeilenHoehen,
            int startZeile,
            int startSpalte,
            int kopfZeilenAnzahl,
            String kopfzeileLinks,
            String kopfzeileMitte,
            String kopfzeileRechts,
            String fusszeileLinks,
            String fusszeileMitte,
            String fusszeileRechts) {
        this.zeilen = zeilen;
        this.spalten = spalten;
        this.startZeile = startZeile;
        this.startSpalte = startSpalte;
        this.kopfZeilenAnzahl = kopfZeilenAnzahl;
        this.gitter = Collections.unmodifiableList(gitter);
        this.zellen = Collections.unmodifiableMap(zellen);
        this.spaltenBreiten = Collections.unmodifiableMap(spaltenBreiten);
        this.zeilenHoehen = Collections.unmodifiableMap(zeilenHoehen);
        this.kopfzeileLinks = kopfzeileLinks;
        this.kopfzeileMitte = kopfzeileMitte;
        this.kopfzeileRechts = kopfzeileRechts;
        this.fusszeileLinks = fusszeileLinks;
        this.fusszeileMitte = fusszeileMitte;
        this.fusszeileRechts = fusszeileRechts;
    }

    public int getZeilen() {
        return zeilen;
    }

    public int getSpalten() {
        return spalten;
    }

    public int getStartZeile() {
        return startZeile;
    }

    public int getStartSpalte() {
        return startSpalte;
    }

    public int getKopfZeilenAnzahl() {
        return kopfZeilenAnzahl;
    }

    public List<List<String>> getGitter() {
        return gitter;
    }

    public Map<String, ZelleModel> getZellen() {
        return zellen;
    }

    public Map<Integer, Integer> getSpaltenBreiten() {
        return spaltenBreiten;
    }

    public Map<Integer, Integer> getZeilenHoehen() {
        return zeilenHoehen;
    }

    public String getKopfzeileLinks() {
        return kopfzeileLinks;
    }

    public String getKopfzeileMitte() {
        return kopfzeileMitte;
    }

    public String getKopfzeileRechts() {
        return kopfzeileRechts;
    }

    public String getFusszeileLinks() {
        return fusszeileLinks;
    }

    public String getFusszeileMitte() {
        return fusszeileMitte;
    }

    public String getFusszeileRechts() {
        return fusszeileRechts;
    }

    /** Erzeugt eine stabile Zell-ID aus Zeilen- und Spaltenindex (0-basiert). */
    public static String zelleId(int zeile, int spalte) {
        return "cell-" + zeile + "-" + spalte;
    }
}
