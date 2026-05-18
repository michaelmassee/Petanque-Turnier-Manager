package de.petanqueturniermanager.webserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Stateless-Service, der die aktuellen Teilnehmerzahlen (angemeldet, aktiv) aus dem
 * Meldelisten-Sheet des Dokuments ermittelt. Bewusst leichtgewichtig — kein
 * Tabellen-Mapping, keine Diff-Engine, keine Tournament-System-spezifischen
 * Meldeliste-Klassen — damit der Live-Push der Turnier-Startseite vom Heavy-Refresh-Pfad
 * der Composite-Views entkoppelt bleibt.
 *
 * <p><b>Angemeldet</b>: Zeilen, in denen Vorname (Spalte&nbsp;B) ODER Nachname (Spalte&nbsp;C)
 * nicht leer ist — die Spieler-Nr-Spalte (A) ist bei Supermelee oft erst nach dem
 * ersten Sortieren befüllt und kann daher nicht als Trigger genommen werden.
 *
 * <p><b>Aktiv</b>: Wert {@code 1} in der „Aktiv"-Spalte. Spalte wird über den Header in
 * Zeile&nbsp;2 (zweite Header-Zeile) lokalisiert:
 * <ul>
 *   <li>Supermelee: Spalte mit Header {@code column.header.spieltag.nr} für den
 *       aktuell eingestellten Spieltag (aus {@link SuperMeleeKonfigurationSheet}) —
 *       Supermelee hat pro Spieltag eine eigene Status-Spalte.</li>
 *   <li>Alle anderen Systeme: Spalte mit Header {@code column.header.aktiv}.</li>
 * </ul>
 * Findet keine Aktiv-Spalte zugeordnet werden, wird {@code aktiv = angemeldet} als
 * Fallback geliefert.
 */
public final class TeilnehmerStatusService {

    private static final Logger logger = LogManager.getLogger(TeilnehmerStatusService.class);

    /** Anzahl Leerzeilen in Folge, nach der die Zählung abbricht. */
    private static final int MAX_LEERE_ZEILEN_IN_FOLGE = 10;
    /** Hartes Limit gegen Endlosschleifen bei beschädigten Sheets. */
    private static final int MAX_ZEILEN = 10_000;
    /** Maximale Anzahl Spalten, die beim Header-Scan durchsucht werden. */
    private static final int MAX_HEADER_SPALTEN = 200;
    /** Wert in der Aktiv-Spalte, der „nimmt teil" bedeutet (SpielrundeGespielt.JA / AKTIV_WERT_NIMMT_TEIL). */
    private static final int WERT_AKTIV = 1;

    /** Vorname-Spalte (B) — direkt nach der Spieler-Nr-Spalte. */
    private static final int VORNAME_SPALTE  = MeldeListeKonstanten.SPIELER_NR_SPALTE + 1;
    /** Nachname-Spalte (C). */
    private static final int NACHNAME_SPALTE = MeldeListeKonstanten.SPIELER_NR_SPALTE + 2;

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
            int aktivSpalte = ermittleAktivSpalte(sh, sheet, ts, ws);
            return zaehlen(sh, sheet, aktivSpalte);
        } catch (RuntimeException e) {
            logger.warn("Teilnehmerzahl konnte nicht ermittelt werden", e);
            return new TeilnehmerStatus(0, 0);
        }
    }

    /**
     * Sucht die Aktiv-Spalte (für Supermelee die Spalte des aktuell eingestellten Spieltags,
     * sonst die als „Aktiv" beschriftete Spalte) über die Header-Zeile.
     *
     * @return Spaltenindex oder {@code -1} falls nicht gefunden — in dem Fall fällt die
     *         Zählung auf {@code aktiv = angemeldet} zurück.
     */
    private static int ermittleAktivSpalte(SheetHelper sh, XSpreadsheet sheet, TurnierSystem ts,
            WorkingSpreadsheet ws) {
        String gesuchterHeader = gesuchterHeader(ts, ws);
        if (gesuchterHeader == null || gesuchterHeader.isBlank()) {
            return -1;
        }
        for (int spalte = 0; spalte < MAX_HEADER_SPALTEN; spalte++) {
            String headerText = sh.getTextFromCell(sheet,
                    Position.from(spalte, MeldeListeKonstanten.ZWEITE_HEADER_ZEILE));
            if (headerText != null && headerText.equals(gesuchterHeader)) {
                return spalte;
            }
        }
        return -1;
    }

    private static String gesuchterHeader(TurnierSystem ts, WorkingSpreadsheet ws) {
        if (ts == TurnierSystem.SUPERMELEE) {
            try {
                SpielTagNr spieltagNr = new SuperMeleeKonfigurationSheet(ws).getAktiveSpieltag();
                return I18n.get("column.header.spieltag.nr", spieltagNr.getNr());
            } catch (RuntimeException e) {
                logger.debug("Aktiver Spieltag (Supermelee) konnte nicht ermittelt werden: {}", e.getMessage());
                return null;
            }
        }
        return I18n.get("column.header.aktiv");
    }

    private static TeilnehmerStatus zaehlen(SheetHelper sh, XSpreadsheet sheet, int aktivSpalte) {
        int angemeldet = 0;
        int aktiv = 0;
        int leereZeilenInFolge = 0;
        for (int zeile = MeldeListeKonstanten.ERSTE_DATEN_ZEILE; zeile < MAX_ZEILEN; zeile++) {
            if (zeileHatNamen(sh, sheet, zeile)) {
                leereZeilenInFolge = 0;
                angemeldet++;
                if (aktivSpalte >= 0
                        && sh.getIntFromCell(sheet, Position.from(aktivSpalte, zeile)) == WERT_AKTIV) {
                    aktiv++;
                }
            } else if (++leereZeilenInFolge >= MAX_LEERE_ZEILEN_IN_FOLGE) {
                break;
            }
        }
        // Fallback wenn Aktiv-Spalte nicht gefunden: Aktiv = Angemeldet (verhindert „0"-Anzeige bei
        // ungewöhnlichem Header-Layout / Sprachwechsel im Dokument).
        return new TeilnehmerStatus(angemeldet, aktivSpalte >= 0 ? aktiv : angemeldet);
    }

    private static boolean zeileHatNamen(SheetHelper sh, XSpreadsheet sheet, int zeile) {
        return !istLeer(sh.getTextFromCell(sheet, Position.from(VORNAME_SPALTE,  zeile)))
                || !istLeer(sh.getTextFromCell(sheet, Position.from(NACHNAME_SPALTE, zeile)));
    }

    private static boolean istLeer(String text) {
        return text == null || text.isBlank();
    }
}
