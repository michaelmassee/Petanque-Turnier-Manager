package de.petanqueturniermanager.webserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Stateless-Service, der die aktuellen Teilnehmerzahlen (angemeldet, aktiv) aus dem
 * Meldelisten-Sheet des Dokuments ermittelt. Bewusst leichtgewichtig — kein
 * Tabellen-Mapping, keine Diff-Engine, keine Tournament-System-spezifischen
 * Meldeliste-Klassen — damit der Live-Push der Turnier-Startseite vom Heavy-Refresh-Pfad
 * der Composite-Views entkoppelt bleibt.
 *
 * <p>Heuristik (V1): Beide Zahlen ermitteln sich aus der Spieler-Nr-Spalte
 * ({@link MeldeListeKonstanten#SPIELER_NR_SPALTE}, ab
 * {@link MeldeListeKonstanten#ERSTE_DATEN_ZEILE}) — gezählt werden Zeilen mit
 * einer positiven Spielernummer. Eine differenzierte „aktiv vs. angemeldet"-
 * Logik (system-spezifisch über {@code IMeldeliste.getAktiveMeldungen()}) bleibt
 * einer V2 vorbehalten.
 */
public final class TeilnehmerStatusService {

    private static final Logger logger = LogManager.getLogger(TeilnehmerStatusService.class);

    /** Anzahl Leerzeilen in Folge, nach der die Zählung abbricht. */
    private static final int MAX_LEERE_ZEILEN_IN_FOLGE = 10;
    /** Hartes Limit gegen Endlosschleifen bei beschädigten Sheets. */
    private static final int MAX_ZEILEN = 10_000;

    public record TeilnehmerStatus(int angemeldet, int aktiv) {}

    private TeilnehmerStatusService() {}

    /**
     * Ermittelt die aktuellen Teilnehmerzahlen aus dem Meldelisten-Sheet des übergebenen Dokuments.
     * Liefert {@code (0, 0)} wenn kein Turniersystem erkannt wurde, kein Meldelisten-Sheet
     * existiert oder beim Lesen ein Fehler auftritt.
     */
    public static TeilnehmerStatus ermitteln(WorkingSpreadsheet ws) {
        if (ws == null) {
            return new TeilnehmerStatus(0, 0);
        }
        try {
            TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
            if (ts == null || ts == TurnierSystem.KEIN) {
                return new TeilnehmerStatus(0, 0);
            }
            SheetHelper sh = new SheetHelper(ws);
            XSpreadsheet sheet = sh.findByName(SheetNamen.meldeliste());
            if (sheet == null) {
                return new TeilnehmerStatus(0, 0);
            }
            return zaehlen(sh, sheet);
        } catch (RuntimeException e) {
            logger.warn("Teilnehmerzahl konnte nicht ermittelt werden", e);
            return new TeilnehmerStatus(0, 0);
        }
    }

    private static TeilnehmerStatus zaehlen(SheetHelper sh, XSpreadsheet sheet) {
        int treffer = 0;
        int leereZeilenInFolge = 0;
        for (int zeile = MeldeListeKonstanten.ERSTE_DATEN_ZEILE; zeile < MAX_ZEILEN; zeile++) {
            // getTextFromCell liefert null für leere Zellen; getIntFromCell würde -1
            // zurückgeben und damit jede leere Zeile als „vorhanden" fehlinterpretieren.
            String text = sh.getTextFromCell(sheet, Position.from(MeldeListeKonstanten.SPIELER_NR_SPALTE, zeile));
            Integer nr = parsePositiv(text);
            if (nr == null) {
                if (++leereZeilenInFolge >= MAX_LEERE_ZEILEN_IN_FOLGE) {
                    break;
                }
                continue;
            }
            leereZeilenInFolge = 0;
            treffer++;
        }
        return new TeilnehmerStatus(treffer, treffer);
    }

    private static Integer parsePositiv(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            int n = Integer.parseInt(text.trim());
            return n > 0 ? n : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
