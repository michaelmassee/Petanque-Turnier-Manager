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
 * <p>Heuristik:
 * <ul>
 *   <li><b>angemeldet</b> = Anzahl Zeilen mit nicht-leerer Spieler-Nr in Spalte
 *       {@link MeldeListeKonstanten#SPIELER_NR_SPALTE}, ab
 *       {@link MeldeListeKonstanten#ERSTE_DATEN_ZEILE}.</li>
 *   <li><b>aktiv</b> = Anzahl Zeilen mit Spieler-Nr {@code > 0} (negative Werte sind
 *       inaktive/ausgesetzte Spieler).</li>
 * </ul>
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
        int angemeldet = 0;
        int aktiv = 0;
        int leereZeilenInFolge = 0;
        for (int zeile = MeldeListeKonstanten.ERSTE_DATEN_ZEILE; zeile < MAX_ZEILEN; zeile++) {
            Integer nr = sh.getIntFromCell(sheet, Position.from(MeldeListeKonstanten.SPIELER_NR_SPALTE, zeile));
            if (nr == null || nr == 0) {
                if (++leereZeilenInFolge >= MAX_LEERE_ZEILEN_IN_FOLGE) {
                    break;
                }
                continue;
            }
            leereZeilenInFolge = 0;
            angemeldet++;
            if (nr > 0) {
                aktiv++;
            }
        }
        return new TeilnehmerStatus(angemeldet, aktiv);
    }
}
