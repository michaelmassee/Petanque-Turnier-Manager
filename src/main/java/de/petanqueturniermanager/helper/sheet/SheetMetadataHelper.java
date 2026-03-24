package de.petanqueturniermanager.helper.sheet;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XNamed;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.XCellRangeAddressable;
import com.sun.star.sheet.XCellRangeReferrer;
import com.sun.star.sheet.XNamedRange;
import com.sun.star.sheet.XNamedRanges;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.table.CellAddress;

import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.supermelee.SpielTagNr;

/**
 * Schreibt und liest Sheet-Metadaten als benannte Bereiche im Dokument.
 * <p>
 * Jedes generierte Sheet bekommt einen benannten Bereich (Named Range), der auf Zelle A1
 * des Sheets zeigt. LibreOffice aktualisiert diese Referenz automatisch wenn das Sheet
 * umbenannt oder verschoben wird – die Zuordnung bleibt damit stabil.
 * <p>
 * Namensschema: {@code __PTM_<TURNIERSYSTEM>_<SHEETTYP>[_NUMMER]__}
 * <p>
 * Beispiele:
 * <ul>
 *   <li>{@code __PTM_SCHWEIZER_RANGLISTE__}</li>
 *   <li>{@code __PTM_SCHWEIZER_MELDELISTE__}</li>
 *   <li>{@code __PTM_SCHWEIZER_SPIELRUNDE_3__}</li>
 *   <li>{@code __PTM_SPIELTAG_1__} – Supermelee Spieltag-Rangliste</li>
 *   <li>{@code __PTM_JGJ_RANGLISTE__}</li>
 * </ul>
 */
public class SheetMetadataHelper {

    private static final Logger logger = LogManager.getLogger(SheetMetadataHelper.class);

    // ── Konstanten: bereits vorhanden ────────────────────────────────────────

    public static final String SCHLUESSEL_SPIELTAG_PREFIX = "__PTM_SPIELTAG_";
    public static final String SCHLUESSEL_SPIELTAG_SUFFIX = "__";

    // ── Konstanten: Supermelee ───────────────────────────────────────────────

    public static final String SCHLUESSEL_SUPERMELEE_MELDELISTE = "__PTM_SUPERMELEE_MELDELISTE__";
    public static final String SCHLUESSEL_SUPERMELEE_ENDRANGLISTE = "__PTM_SUPERMELEE_ENDRANGLISTE__";
    public static final String SCHLUESSEL_SUPERMELEE_TEAMS = "__PTM_SUPERMELEE_TEAMS__";
    public static final String SCHLUESSEL_SUPERMELEE_ANMELDUNGEN_PREFIX = "__PTM_SUPERMELEE_ANMELDUNGEN_";
    public static final String SCHLUESSEL_SUPERMELEE_TEILNEHMER_PREFIX = "__PTM_SUPERMELEE_TEILNEHMER_";
    public static final String SCHLUESSEL_SUPERMELEE_SPIELRUNDE_PREFIX = "__PTM_SUPERMELEE_SPIELRUNDE_";
    public static final String SCHLUESSEL_SUPERMELEE_SPIELRUNDE_PLAN_PREFIX = "__PTM_SUPERMELEE_SPIELRUNDE_PLAN_";

    // ── Konstanten: JederGegenJeden ──────────────────────────────────────────

    public static final String SCHLUESSEL_JGJ_MELDELISTE = "__PTM_JGJ_MELDELISTE__";
    public static final String SCHLUESSEL_JGJ_SPIELPLAN = "__PTM_JGJ_SPIELPLAN__";
    public static final String SCHLUESSEL_JGJ_RANGLISTE = "__PTM_JGJ_RANGLISTE__";
    public static final String SCHLUESSEL_JGJ_DIREKTVERGLEICH = "__PTM_JGJ_DIREKTVERGLEICH__";

    // ── Konstanten: Liga ─────────────────────────────────────────────────────

    public static final String SCHLUESSEL_LIGA_MELDELISTE = "__PTM_LIGA_MELDELISTE__";
    public static final String SCHLUESSEL_LIGA_SPIELPLAN = "__PTM_LIGA_SPIELPLAN__";
    public static final String SCHLUESSEL_LIGA_RANGLISTE = "__PTM_LIGA_RANGLISTE__";
    public static final String SCHLUESSEL_LIGA_DIREKTVERGLEICH = "__PTM_LIGA_DIREKTVERGLEICH__";

    // ── Konstanten: Schweizer ────────────────────────────────────────────────

    public static final String SCHLUESSEL_SCHWEIZER_RANGLISTE = "__PTM_SCHWEIZER_RANGLISTE__";
    public static final String SCHLUESSEL_SCHWEIZER_MELDELISTE = "__PTM_SCHWEIZER_MELDELISTE__";
    public static final String SCHLUESSEL_SCHWEIZER_SPIELRUNDE_PREFIX = "__PTM_SCHWEIZER_SPIELRUNDE_";

    // ── Konstanten: KO ───────────────────────────────────────────────────────

    public static final String SCHLUESSEL_KO_MELDELISTE = "__PTM_KO_MELDELISTE__";
    public static final String SCHLUESSEL_KO_TURNIERBAUM_PREFIX = "__PTM_KO_TURNIERBAUM_";

    // ── Konstanten: Maastrichter ─────────────────────────────────────────────
    public static final String SCHLUESSEL_MAASTRICHTER_MELDELISTE = "__PTM_MAASTRICHTER_MELDELISTE__";
    public static final String SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX = "__PTM_MAASTRICHTER_VORRUNDE_";
    public static final String SCHLUESSEL_MAASTRICHTER_FINALRUNDE_PREFIX = "__PTM_MAASTRICHTER_FINALRUNDE_";

    // ── Konstanten: Forme / KO-Runden ────────────────────────────────────────

    public static final String SCHLUESSEL_FORME_VORRUNDEN = "__PTM_FORME_VORRUNDEN__";
    public static final String SCHLUESSEL_FORME_CADRAGE = "__PTM_FORME_CADRAGE__";
    public static final String SCHLUESSEL_FORME_KO_GRUPPE = "__PTM_FORME_KO_GRUPPE__";

    /**
     * Gemeinsames Suffix für alle dynamisch erzeugten Schlüssel.
     */
    public static final String SCHLUESSEL_SUFFIX = "__";

    private SheetMetadataHelper() {
    }

    // ── Builder für dynamische Schlüssel ────────────────────────────────────

    public static String schluesselSpieltagRangliste(int spieltagNr) {
        return SCHLUESSEL_SPIELTAG_PREFIX + spieltagNr + SCHLUESSEL_SPIELTAG_SUFFIX;
    }

    public static String schluesselSchweizerSpielrunde(int rundeNr) {
        return SCHLUESSEL_SCHWEIZER_SPIELRUNDE_PREFIX + rundeNr + SCHLUESSEL_SUFFIX;
    }

    public static String schluesselMaastrichterVorrunde(int rundeNr) {
        return SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX + rundeNr + SCHLUESSEL_SUFFIX;
    }

    public static String schluesselMaastrichterFinalrunde(String gruppenBuchstabe) {
        return SCHLUESSEL_MAASTRICHTER_FINALRUNDE_PREFIX + gruppenBuchstabe + SCHLUESSEL_SUFFIX;
    }

    public static String schluesselSupermeleeSpielrunde(int spieltagNr, int rundeNr) {
        return SCHLUESSEL_SUPERMELEE_SPIELRUNDE_PREFIX + spieltagNr + "_" + rundeNr + SCHLUESSEL_SUFFIX;
    }

    public static String schluesselSupermeleeSpielrundePlan(int spieltagNr, int rundeNr) {
        return SCHLUESSEL_SUPERMELEE_SPIELRUNDE_PLAN_PREFIX + spieltagNr + "_" + rundeNr + SCHLUESSEL_SUFFIX;
    }

    public static String schluesselSupermeleeAnmeldungen(int spieltagNr) {
        return SCHLUESSEL_SUPERMELEE_ANMELDUNGEN_PREFIX + spieltagNr + SCHLUESSEL_SUFFIX;
    }

    public static String schluesselSupermeleeTeilehnehmer(int spieltagNr) {
        return SCHLUESSEL_SUPERMELEE_TEILNEHMER_PREFIX + spieltagNr + SCHLUESSEL_SUFFIX;
    }

    public static String schluesselKoTurnierbaum(String gruppenSuffix) {
        return SCHLUESSEL_KO_TURNIERBAUM_PREFIX + gruppenSuffix + SCHLUESSEL_SUFFIX;
    }

    // ── Schreiben ────────────────────────────────────────────────────────────

    /**
     * Legt einen benannten Bereich an (oder überschreibt ihn), der auf Zelle A1
     * des übergebenen Sheets zeigt.
     *
     * @param xDoc          Spreadsheet-Dokument
     * @param xSheet        das Sheet für das die Metadaten gespeichert werden
     * @param namedRangeKey Name des benannten Bereichs
     */
    public static void schreibeSheetMetadaten(XSpreadsheetDocument xDoc,
                                              XSpreadsheet xSheet, String namedRangeKey) {
        try {
            XNamedRanges namedRanges = namedRangesAusDoc(xDoc);
            if (namedRanges == null) return;
            String sheetName = Lo.qi(XNamed.class, xSheet).getName();
            int sheetIdx = sheetIndex(xDoc, xSheet);
            schreibeSheetMetadaten(namedRanges, sheetName, sheetIdx, namedRangeKey);
        } catch (Throwable t) {
            logger.error("Fehler beim Schreiben der Sheet-Metadaten für '{}'", namedRangeKey, t);
        }
    }

    /**
     * Schreibt die Spieltag-Nummer als benannten Bereich.
     * Delegiert an {@link #schreibeSheetMetadaten}.
     */
    public static void schreibeSpieltagNr(XSpreadsheetDocument xDoc,
                                          XSpreadsheet xSheet, SpielTagNr spieltagNr) {
        String schluessel = SCHLUESSEL_SPIELTAG_PREFIX + spieltagNr.getNr() + SCHLUESSEL_SPIELTAG_SUFFIX;
        schreibeSheetMetadaten(xDoc, xSheet, schluessel);
    }

    /**
     * Kernlogik für {@link #schreibeSheetMetadaten} – package-private für Unit-Tests.
     * Kein UNO-Interface-Casting ({@code Lo.qi}) notwendig.
     *
     * @param namedRanges   Named-Range-Container (bereits aufgelöst)
     * @param sheetName     unescapeter Sheet-Name
     * @param sheetIdx      0-basierter Sheet-Index
     * @param namedRangeKey Schlüssel des benannten Bereichs
     */
    static void schreibeSheetMetadaten(XNamedRanges namedRanges,
                                       String sheetName, int sheetIdx, String namedRangeKey) {
        try {
            if (namedRanges.hasByName(namedRangeKey)) {
                namedRanges.removeByName(namedRangeKey);
            }
            String escaped = sheetName.replace("'", "''");
            String inhalt = "$'" + escaped + "'.$A$1";
            CellAddress refAddr = new CellAddress();
            refAddr.Sheet = (short) sheetIdx;
            namedRanges.addNewByName(namedRangeKey, inhalt, refAddr, 0);
        } catch (Throwable t) {
            logger.error("Fehler beim Schreiben der Sheet-Metadaten für '{}'", namedRangeKey, t);
        }
    }


    /**
     * Sucht ein Sheet primär über den sicheren benannten Bereich.
     * Führt bei alten Dateien einen Fallback über den Namen durch und heilt die Datei automatisch.
     */


    // ── Lesen / Suchen ───────────────────────────────────────────────────────

    /**
     * Sucht das Sheet, das dem gegebenen Named-Range-Schlüssel zugeordnet ist.
     *
     * @param xDoc          Spreadsheet-Dokument
     * @param namedRangeKey Schlüssel des benannten Bereichs
     * @return Optional mit dem gefundenen XSpreadsheet, sonst empty
     */
    public static Optional<XSpreadsheet> findeSheet(XSpreadsheetDocument xDoc, String namedRangeKey) {
        try {
            XNamedRanges namedRanges = namedRangesAusDoc(xDoc);
            if (namedRanges == null) return Optional.empty();
            XIndexAccess sheets = Lo.qi(XIndexAccess.class, xDoc.getSheets());
            return findeSheet(namedRanges,
                    rangeObj -> sheetIndexAusNamedRangeObj(rangeObj),
                    idx -> {
                        try {
                            if (sheets == null) return null;
                            return Lo.qi(XSpreadsheet.class, sheets.getByIndex(idx));
                        } catch (Throwable t) {
                            return null;
                        }
                    },
                    namedRangeKey);
        } catch (Throwable t) {
            logger.error("Fehler beim Suchen des Sheets für Schlüssel '{}'", namedRangeKey, t);
            return Optional.empty();
        }
    }

    /**
     * Sucht ein Sheet primär über den sicheren benannten Bereich.
     * Führt bei alten Dateien einen Fallback über den Namen durch und heilt die Datei automatisch,
     * d.h. schreibt fehlende Metadaten nach, sodass der nächste Aufruf direkt über den Named Range greift.
     *
     * @param xDoc         Spreadsheet-Dokument
     * @param schluessel   Metadaten-Schlüssel (Named Range)
     * @param fallbackName Sheet-Name für Rückwärtskompatibilität mit alten Dateien
     * @return gefundenes Sheet oder {@code null} wenn nicht vorhanden
     */
    public static XSpreadsheet findeSheetUndHeile(XSpreadsheetDocument xDoc,
                                                  String schluessel, String fallbackName) {
        // 1. Metadaten-Lookup (überlebt Umbenennungen)
        Optional<XSpreadsheet> found = findeSheet(xDoc, schluessel);
        if (found.isPresent()) return found.get();

        // 2. Name-Fallback für alte Dateien ohne Metadaten
        try {
            XSpreadsheets sheets = xDoc.getSheets();
            if (!sheets.hasByName(fallbackName)) return null;
            XSpreadsheet sheet = Lo.qi(XSpreadsheet.class, sheets.getByName(fallbackName));
            if (sheet == null) return null;

            // 3. Datei heilen: Metadaten nachschreiben damit zukünftige Lookups direkt greifen
            schreibeSheetMetadaten(xDoc, sheet, schluessel);
            logger.debug("Sheet '{}' geheilt: Metadaten-Schlüssel '{}' nachgetragen", fallbackName, schluessel);
            return sheet;
        } catch (Throwable t) {
            logger.error("Fehler beim Fallback-Lookup für Sheet '{}'", fallbackName, t);
            return null;
        }
    }

    /**
     * Kernlogik für {@link #findeSheet} – package-private für Unit-Tests.
     * Die UNO-Auflösung wird über Funktionsparameter injiziert.
     *
     * @param namedRanges           Named-Range-Container
     * @param sheetIdxAusNamedRange Funktion: Named-Range-Objekt → Sheet-Index (-1 = nicht gefunden)
     * @param sheetByIdx            Funktion: Sheet-Index → XSpreadsheet
     * @param namedRangeKey         gesuchter Schlüssel
     */
    static Optional<XSpreadsheet> findeSheet(XNamedRanges namedRanges,
                                             Function<Object, Integer> sheetIdxAusNamedRange,
                                             IntFunction<XSpreadsheet> sheetByIdx,
                                             String namedRangeKey) {
        try {
            if (namedRanges == null || !namedRanges.hasByName(namedRangeKey)) return Optional.empty();
            Object rangeObj = namedRanges.getByName(namedRangeKey);
            Integer idx = sheetIdxAusNamedRange.apply(rangeObj);
            if (idx == null || idx < 0) return Optional.empty();
            return Optional.ofNullable(sheetByIdx.apply(idx));
        } catch (Throwable t) {
            logger.error("Fehler beim Suchen des Sheets für Schlüssel '{}'", namedRangeKey, t);
            return Optional.empty();
        }
    }

    /**
     * Prüft ob das Sheet dem benannten Bereich mit dem gegebenen Schlüssel entspricht.
     * Überlebt Umbenennungen des Sheets, da LibreOffice die Zellreferenz aktuell hält.
     */
    public static boolean istRegistriertesSheet(XSpreadsheetDocument xDoc,
                                                XSpreadsheet xSheet, String namedRangeKey) {
        try {
            XNamedRanges namedRanges = namedRangesAusDoc(xDoc);
            if (namedRanges == null) return false;
            int targetIdx = sheetIndex(xDoc, xSheet);
            return istRegistriertesSheet(namedRanges,
                    rangeObj -> sheetIndexAusNamedRangeObj(rangeObj),
                    targetIdx, namedRangeKey);
        } catch (Throwable t) {
            logger.error("Fehler beim Prüfen des Sheet-Metadaten-Schlüssels '{}'", namedRangeKey, t);
            return false;
        }
    }

    /**
     * Kernlogik für {@link #istRegistriertesSheet} – package-private für Unit-Tests.
     */
    static boolean istRegistriertesSheet(XNamedRanges namedRanges,
                                         Function<Object, Integer> sheetIdxAusNamedRange,
                                         int targetSheetIdx, String namedRangeKey) {
        if (namedRanges == null || !namedRanges.hasByName(namedRangeKey)) return false;
        try {
            Object rangeObj = namedRanges.getByName(namedRangeKey);
            Integer idx = sheetIdxAusNamedRange.apply(rangeObj);
            return idx != null && idx >= 0 && idx == targetSheetIdx;
        } catch (Throwable t) {
            logger.error("Fehler beim Prüfen des Sheet-Metadaten-Schlüssels '{}'", namedRangeKey, t);
            return false;
        }
    }

    /**
     * Sucht die Spieltag-Nummer für das angegebene Sheet, indem alle
     * {@code __PTM_SPIELTAG_N__}-Einträge durchsucht werden.
     *
     * @return SpielTagNr wenn das Sheet zugeordnet ist, sonst {@link Optional#empty()}
     */
    public static Optional<SpielTagNr> findeSpieltagNr(XSpreadsheetDocument xDoc,
                                                       XSpreadsheet xSheet) {
        try {
            XNamedRanges namedRanges = namedRangesAusDoc(xDoc);
            if (namedRanges == null) return Optional.empty();
            int targetIdx = sheetIndex(xDoc, xSheet);
            return findeSpieltagNr(namedRanges,
                    rangeObj -> sheetIndexAusNamedRangeObj(rangeObj),
                    targetIdx);
        } catch (Throwable t) {
            logger.error("Fehler beim Suchen der Spieltag-Nr", t);
        }
        return Optional.empty();
    }

    /**
     * Kernlogik für {@link #findeSpieltagNr} – package-private für Unit-Tests.
     */
    static Optional<SpielTagNr> findeSpieltagNr(XNamedRanges namedRanges,
                                                Function<Object, Integer> sheetIdxAusNamedRange,
                                                int targetSheetIdx) {
        try {
            if (namedRanges == null) return Optional.empty();
            for (String name : namedRanges.getElementNames()) {
                if (!name.startsWith(SCHLUESSEL_SPIELTAG_PREFIX)) continue;
                if (!name.endsWith(SCHLUESSEL_SPIELTAG_SUFFIX)) continue;
                try {
                    Object rangeObj = namedRanges.getByName(name);
                    Integer idx = sheetIdxAusNamedRange.apply(rangeObj);
                    if (idx != null && idx >= 0 && idx == targetSheetIdx) {
                        String nStr = name.substring(SCHLUESSEL_SPIELTAG_PREFIX.length(),
                                name.length() - SCHLUESSEL_SPIELTAG_SUFFIX.length());
                        return Optional.of(SpielTagNr.from(Integer.parseInt(nStr)));
                    }
                } catch (Throwable t) {
                    logger.error("Fehler beim Prüfen von Named-Range '{}'", name, t);
                }
            }
        } catch (Throwable t) {
            logger.error("Fehler beim Suchen der Spieltag-Nr", t);
        }
        return Optional.empty();
    }

    // ── Hilfsmethoden ────────────────────────────────────────────────────────

    /**
     * Sucht nach allen PTM-Metadaten (Named Ranges), die ins Leere zeigen (#REF!)
     * und löscht diese ersatzlos aus dem Dokument.
     * Dies bereinigt das Dokument nach manuellen oder programmatischen Sheet-Löschungen.
     */
    public static void bereinigeVerwaisteMetadaten(XSpreadsheetDocument xDoc) {
        bereinigeVerwaisteMetadaten(namedRangesAusDoc(xDoc), obj -> {
            XNamedRange range = Lo.qi(XNamedRange.class, obj);
            return range != null ? range.getContent() : null;
        });
    }

    static void bereinigeVerwaisteMetadaten(XNamedRanges namedRanges,
            Function<Object, String> namedRangeContentAusObj) {
        if (namedRanges == null) return;
        try {
            String[] names = namedRanges.getElementNames();
            for (String name : names) {
                if (name.startsWith("__PTM_")) {
                    Object rangeObj = namedRanges.getByName(name);
                    String content = namedRangeContentAusObj.apply(rangeObj);
                    if (content != null && (content.contains("#REF!") || content.contains("#BEZUG!"))) {
                        namedRanges.removeByName(name);
                        logger.debug("Verwaisten Metadaten-Schlüssel '{}' gelöscht (zeigte ins Leere).", name);
                    }
                }
            }
        } catch (Throwable t) {
            logger.error("Fehler bei der Bereinigung verwaister Sheet-Metadaten", t);
        }
    }

    /**
     * Liefert das Named-Range-Container-Objekt des Dokuments.
     */
    static XNamedRanges namedRangesAusDoc(XSpreadsheetDocument xDoc) {
        try {
            XPropertySet props = Lo.qi(XPropertySet.class, xDoc);
            if (props == null) return null;
            Object namedRangesObj = props.getPropertyValue("NamedRanges");
            return Lo.qi(XNamedRanges.class, namedRangesObj);
        } catch (com.sun.star.beans.UnknownPropertyException | WrappedTargetException e) {
            logger.error("NamedRanges-Property nicht gefunden", e);
            return null;
        }
    }

    /**
     * Ermittelt den Sheet-Index aus einem Named-Range-Objekt via UNO-Interface-Casting.
     * Wird als Funktionsargument in {@link #findeSheet} und {@link #istRegistriertesSheet}
     * verwendet.
     *
     * @return Sheet-Index (≥ 0) oder -1 wenn nicht ermittelbar
     */
    private static int sheetIndexAusNamedRangeObj(Object rangeObj) {
        try {
            XNamedRange range = Lo.qi(XNamedRange.class, rangeObj);
            if (range == null) return -1;
            // --- Prüfen ob die Referenz durch Löschen kaputt gegangen ist ---
            String content = range.getContent();
            if (content != null && (content.contains("#REF!") || content.contains("#BEZUG!"))) {
                logger.debug("Named-Range Referenz ist ungültig geworden ({}). Sheet wurde gelöscht.", content);
                return -1;
            }
            XCellRangeReferrer referrer = Lo.qi(XCellRangeReferrer.class, range);
            if (referrer == null) return -1;
            XCellRangeAddressable addrAble = Lo.qi(XCellRangeAddressable.class,
                    referrer.getReferredCells());
            if (addrAble == null) return -1;
            return addrAble.getRangeAddress().Sheet;
        } catch (Throwable t) {
            logger.error("Fehler beim Auflösen des Named-Range-Sheet-Index", t);
            return -1;
        }
    }

    /**
     * Ermittelt den Index (Position) eines Sheets im Dokument.
     *
     * @return Sheet-Index (≥ 0) oder -1 wenn nicht gefunden
     */
    private static int sheetIndex(XSpreadsheetDocument xDoc, XSpreadsheet xSheet) {
        String targetName = Lo.qi(XNamed.class, xSheet).getName();
        String[] names = xDoc.getSheets().getElementNames();
        for (int i = 0; i < names.length; i++) {
            if (targetName.equals(names[i])) return i;
        }
        return -1;
    }
}
