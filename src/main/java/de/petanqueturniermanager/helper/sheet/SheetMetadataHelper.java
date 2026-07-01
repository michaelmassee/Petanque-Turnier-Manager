package de.petanqueturniermanager.helper.sheet;

import java.util.ArrayList;
import java.util.List;
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
import com.sun.star.table.CellRangeAddress;
import com.sun.star.table.XCell;
import com.sun.star.text.XText;

import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.LogUtil;
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

    // ── Konstanten: allgemein ─────────────────────────────────────────────────

    /** Exakter Schlüssel für das turniersystemübergreifende Teilnehmer-Sheet (je Turnier genau eines). */
    public static final String SCHLUESSEL_TEILNEHMER = "__PTM_TEILNEHMER__";

    // ── Konstanten: bereits vorhanden ────────────────────────────────────────

    public static final String SCHLUESSEL_SPIELTAG_RANGLISTE_PREFIX = "__PTM_SPIELTAG_";
    public static final String SCHLUESSEL_SPIELTAG_RANGLISTE_SUFFIX = "__";

    // ── Konstanten: Supermelee ───────────────────────────────────────────────

    public static final String SCHLUESSEL_SUPERMELEE_MELDELISTE = "__PTM_SUPERMELEE_MELDELISTE__";
    public static final String SCHLUESSEL_SUPERMELEE_ENDRANGLISTE = "__PTM_SUPERMELEE_ENDRANGLISTE__";
    public static final String SCHLUESSEL_SUPERMELEE_TEAMS = "__PTM_SUPERMELEE_TEAMS__";
    public static final String SCHLUESSEL_SUPERMELEE_ANMELDUNGEN_PREFIX = "__PTM_SUPERMELEE_ANMELDUNGEN_";
    /** Prefix für nummerierte Supermelee-Teilnehmer-Sheets (1 pro Spieltag). */
    public static final String SCHLUESSEL_SUPERMELEE_SPIELTAG_TEILNEHMER_PREFIX = "__PTM_SUPERMELEE_TEILNEHMER_";
    public static final String SCHLUESSEL_SUPERMELEE_SPIELRUNDE_PREFIX = "__PTM_SUPERMELEE_SPIELRUNDE_";
    public static final String SCHLUESSEL_SUPERMELEE_SPIELRUNDE_PLAN_PREFIX = "__PTM_SUPERMELEE_SPIELRUNDE_PLAN_";

    // ── Konstanten: JederGegenJeden ──────────────────────────────────────────

    public static final String SCHLUESSEL_JGJ_MELDELISTE = "__PTM_JGJ_MELDELISTE__";
    public static final String SCHLUESSEL_JGJ_SPIELPLAN = "__PTM_JGJ_SPIELPLAN__";
    public static final String SCHLUESSEL_JGJ_RANGLISTE = "__PTM_JGJ_RANGLISTE__";
    public static final String SCHLUESSEL_JGJ_GESAMTRANGLISTE = "__PTM_JGJ_GESAMTRANGLISTE__";
    public static final String SCHLUESSEL_JGJ_DIREKTVERGLEICH = "__PTM_JGJ_DIREKTVERGLEICH__";
    /** Prefix für nummerierte JGJ-Gruppen-Spielplan-Aushang-Sheets (1 pro Gruppe). */
    public static final String SCHLUESSEL_JGJ_GRUPPE_SPIELPLAN_PREFIX = "__PTM_JGJ_GRUPPE_SPIELPLAN_";

    // ── Konstanten: Liga ─────────────────────────────────────────────────────

    public static final String SCHLUESSEL_LIGA_MELDELISTE = "__PTM_LIGA_MELDELISTE__";
    public static final String SCHLUESSEL_LIGA_SPIELPLAN = "__PTM_LIGA_SPIELPLAN__";
    public static final String SCHLUESSEL_LIGA_RANGLISTE = "__PTM_LIGA_RANGLISTE__";
    public static final String SCHLUESSEL_LIGA_DIREKTVERGLEICH = "__PTM_LIGA_DIREKTVERGLEICH__";
    public static final String SCHLUESSEL_LIGA_TERMINE_PRO_TEILNEHMER = "__PTM_LIGA_TERMINE_PRO_TEILNEHMER__";
    public static final String SCHLUESSEL_LIGA_TERMINE_PRO_TEILNEHMER_PREFIX =
            "__PTM_LIGA_TERMINE_PRO_TEILNEHMER_";

    // ── Konstanten: TripTete ─────────────────────────────────────────────────

    public static final String SCHLUESSEL_TRIPTETE_MELDELISTE   = "__PTM_TRIPTETE_MELDELISTE__";
    public static final String SCHLUESSEL_TRIPTETE_SPIELPLAN    = "__PTM_TRIPTETE_SPIELPLAN__";
    public static final String SCHLUESSEL_TRIPTETE_RANGLISTE    = "__PTM_TRIPTETE_RANGLISTE__";
    public static final String SCHLUESSEL_TRIPTETE_CHECKIN_LISTE = "__PTM_TRIPTETE_CHECKIN_LISTE__";

    // ── Konstanten: Schweizer ────────────────────────────────────────────────

    public static final String SCHLUESSEL_SCHWEIZER_RANGLISTE = "__PTM_SCHWEIZER_RANGLISTE__";
    public static final String SCHLUESSEL_SCHWEIZER_MELDELISTE = "__PTM_SCHWEIZER_MELDELISTE__";
    public static final String SCHLUESSEL_SCHWEIZER_SPIELRUNDE_PREFIX = "__PTM_SCHWEIZER_SPIELRUNDE_";

    // ── Konstanten: KO ───────────────────────────────────────────────────────

    public static final String SCHLUESSEL_KO_MELDELISTE = "__PTM_KO_MELDELISTE__";
    public static final String SCHLUESSEL_KO_TURNIERBAUM_PREFIX = "__PTM_KO_TURNIERBAUM_";

    // ── Konstanten: Kaskaden-KO ──────────────────────────────────────────────

    public static final String SCHLUESSEL_KASKADE_MELDELISTE        = "__PTM_KASKADE_MELDELISTE__";
    public static final String SCHLUESSEL_KASKADE_RUNDE_PREFIX      = "__PTM_KASKADE_RUNDE_";
    public static final String SCHLUESSEL_KASKADE_FELD_PREFIX       = "__PTM_KASKADE_FELD_";
    public static final String SCHLUESSEL_KASKADE_GRUPPENRANGLISTE  = "__PTM_KASKADE_GRUPPENRANGLISTE__";

    // ── Konstanten: Maastrichter ─────────────────────────────────────────────
    public static final String SCHLUESSEL_MAASTRICHTER_MELDELISTE = "__PTM_MAASTRICHTER_MELDELISTE__";
    public static final String SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX = "__PTM_MAASTRICHTER_VORRUNDE_";
    public static final String SCHLUESSEL_MAASTRICHTER_FINALRUNDE_PREFIX = "__PTM_MAASTRICHTER_FINALRUNDE_";

    // ── Konstanten: Poule ────────────────────────────────────────────────────

    public static final String SCHLUESSEL_POULE_MELDELISTE           = "__PTM_POULE_MELDELISTE__";
    public static final String SCHLUESSEL_POULE_VORRUNDE             = "__PTM_POULE_VORRUNDE__";
    public static final String SCHLUESSEL_POULE_SPIELPLAN_PREFIX      = "__PTM_POULE_SPIELPLAN_";
    public static final String SCHLUESSEL_POULE_VORRUNDEN_RANGLISTE   = "__PTM_POULE_VORRUNDEN_RANGLISTE__";
    public static final String SCHLUESSEL_POULE_KO_PREFIX             = "__PTM_POULE_KO_";

    // ── Konstanten: Formule X ────────────────────────────────────────────────

    public static final String SCHLUESSEL_FORMULEX_MELDELISTE        = "__PTM_FORMULEX_MELDELISTE__";
    public static final String SCHLUESSEL_FORMULEX_SPIELRUNDE_PREFIX = "__PTM_FORMULEX_SPIELRUNDE_";
    public static final String SCHLUESSEL_FORMULEX_RANGLISTE         = "__PTM_FORMULEX_RANGLISTE__";

    // ── Konstanten: Forme / KO-Runden ────────────────────────────────────────

    public static final String SCHLUESSEL_FORME_VORRUNDEN = "__PTM_FORME_VORRUNDEN__";
    public static final String SCHLUESSEL_FORME_CADRAGE = "__PTM_FORME_CADRAGE__";
    public static final String SCHLUESSEL_FORME_KO_GRUPPE = "__PTM_FORME_KO_GRUPPE__";

    // ── Konstanten: Checkin-Listen (je System, außer Supermelee/Liga) ─────────

    public static final String SCHLUESSEL_JGJ_CHECKIN_LISTE = "__PTM_JGJ_CHECKIN_LISTE__";
    public static final String SCHLUESSEL_KO_CHECKIN_LISTE = "__PTM_KO_CHECKIN_LISTE__";
    public static final String SCHLUESSEL_KASKADE_CHECKIN_LISTE = "__PTM_KASKADE_CHECKIN_LISTE__";
    public static final String SCHLUESSEL_FORMULEX_CHECKIN_LISTE = "__PTM_FORMULEX_CHECKIN_LISTE__";
    public static final String SCHLUESSEL_SCHWEIZER_CHECKIN_LISTE = "__PTM_SCHWEIZER_CHECKIN_LISTE__";
    public static final String SCHLUESSEL_POULE_CHECKIN_LISTE = "__PTM_POULE_CHECKIN_LISTE__";
    public static final String SCHLUESSEL_MAASTRICHTER_CHECKIN_LISTE = "__PTM_MAASTRICHTER_CHECKIN_LISTE__";

    /**
     * Gemeinsames Suffix für alle dynamisch erzeugten Schlüssel.
     */
    public static final String SCHLUESSEL_SUFFIX = "__";

    private SheetMetadataHelper() {
    }

    /**
     * Prüft, ob der Named-Range-Inhalt eine kaputte Referenz auf ein gelöschtes Blatt ist
     * ({@code #REF!}).
     * <p>
     * {@link XNamedRange#getContent()} rendert den Inhalt immer mit {@code GRAM_API} – einer
     * festen, nicht lokalisierten Formel-Grammatik (siehe LO {@code ScNamedRangeObj::getContent},
     * Symbol-Tabelle {@code RID_STRLIST_FUNCTION_NAMES_ENGLISH_API} mit hartkodiertem
     * {@code "#REF!"}). Der Fehler-String ist damit in jeder Locale (DE/EN/FR/NL/ES) identisch
     * {@code #REF!}; ein lokalisiertes {@code #BEZUG!} kann hier nicht auftreten.
     */
    private static boolean istKaputteReferenz(String content) {
        return content != null && content.contains("#REF!");
    }

    // ── Builder für dynamische Schlüssel ────────────────────────────────────

    public static String schluesselSpieltagRangliste(int spieltagNr) {
        return SCHLUESSEL_SPIELTAG_RANGLISTE_PREFIX + spieltagNr + SCHLUESSEL_SPIELTAG_RANGLISTE_SUFFIX;
    }

    public static String schluesselSchweizerSpielrunde(int rundeNr) {
        return SCHLUESSEL_SCHWEIZER_SPIELRUNDE_PREFIX + rundeNr + SCHLUESSEL_SUFFIX;
    }

    public static String schluesselFormuleXSpielrunde(int rundeNr) {
        return SCHLUESSEL_FORMULEX_SPIELRUNDE_PREFIX + rundeNr;
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

    public static String schluesselSupermeleeTeilnehmer(int spieltagNr) {
        return SCHLUESSEL_SUPERMELEE_SPIELTAG_TEILNEHMER_PREFIX + spieltagNr + SCHLUESSEL_SUFFIX;
    }

    public static String schluesselKoTurnierbaum(String gruppenSuffix) {
        return SCHLUESSEL_KO_TURNIERBAUM_PREFIX + gruppenSuffix + SCHLUESSEL_SUFFIX;
    }

    public static String schluesselLigaTermineProTeilnehmer(int teamNr) {
        return SCHLUESSEL_LIGA_TERMINE_PRO_TEILNEHMER_PREFIX + teamNr + SCHLUESSEL_SUFFIX;
    }

    /**
     * Leitet den Dokument-Property-Schlüssel für Score-Positionen aus dem Bracket-Metadaten-Schlüssel ab.
     * <p>
     * Das Präfix {@code __PTM_} wird durch {@code __PTM_SCORE_} ersetzt, so dass der Score-Schlüssel
     * eindeutig dem Bracket-Sheet zugeordnet bleibt und für alle Turniersysteme funktioniert.
     * <p>
     * Beispiele:
     * <ul>
     *   <li>{@code __PTM_KO_TURNIERBAUM_A__} → {@code __PTM_SCORE_KO_TURNIERBAUM_A__}</li>
     *   <li>{@code __PTM_MAASTRICHTER_FINALRUNDE_A__} → {@code __PTM_SCORE_MAASTRICHTER_FINALRUNDE_A__}</li>
     * </ul>
     */
    public static String scoreSchluessel(String bracketSchluessel) {
        return bracketSchluessel.replace("__PTM_", "__PTM_SCORE_");
    }

    /**
     * Legt einen Named Range an (oder aktualisiert ihn ohne remove+add), der auf eine
     * bestimmte Zelle des Sheets zeigt. Wird für Score-Positions-Daten verwendet.
     *
     * @param xDoc          Spreadsheet-Dokument
     * @param xSheet        Ziel-Sheet
     * @param scoreSchluessel Named-Range-Schlüssel (z.B. {@code __PTM_SCORE_KO_TURNIERBAUM_A__})
     * @param spalte        0-basierter Spaltenindex der Datenzelle
     * @param zeile         0-basierter Zeilenindex der Datenzelle
     */
    public static void schreibeScoreZellenMetadaten(XSpreadsheetDocument xDoc,
                                                     XSpreadsheet xSheet,
                                                     String scoreSchluessel,
                                                     int spalte, int zeile) {
        try {
            XNamedRanges namedRanges = namedRangesAusDoc(xDoc);
            if (namedRanges == null) return;
            String sheetName = Lo.qi(XNamed.class, xSheet).getName();
            int sheetIdx = sheetIndex(xDoc, xSheet);
            String escaped = sheetName.replace("'", "''");
            String inhalt = "$'" + escaped + "'.$" + spaltenNummer2Buchstabe(spalte) + "$" + (zeile + 1);
            CellAddress refAddr = new CellAddress();
            refAddr.Sheet = (short) sheetIdx;
            if (namedRanges.hasByName(scoreSchluessel)) {
                XNamedRange existing = Lo.qi(XNamedRange.class, namedRanges.getByName(scoreSchluessel));
                if (existing != null) {
                    existing.setContent(inhalt);
                    return;
                }
            }
            namedRanges.addNewByName(scoreSchluessel, inhalt, refAddr, 0);
        } catch (Exception e) {
            LogUtil.error(logger, "Score-Zellen-Metadaten schreiben fehlgeschlagen für '" + scoreSchluessel + "'", e);
        } catch (Error e) {
            throw e;
        }
    }

    /**
     * Liest den String-Inhalt der Zelle, auf die der Named Range mit {@code scoreSchluessel} zeigt.
     * Gibt {@code null} zurück wenn der Named Range fehlt, die Referenz ungültig ist oder die Zelle leer ist.
     *
     * @param xDoc           Spreadsheet-Dokument
     * @param scoreSchluessel Named-Range-Schlüssel (z.B. {@code __PTM_SCORE_KO_TURNIERBAUM_A__})
     * @return Zelleninhalt oder {@code null}
     */
    public static String leseScoreText(XSpreadsheetDocument xDoc, String scoreSchluessel) {
        try {
            Optional<CellRangeAddress> addr = aufloeseNamedRangeAddresse(xDoc, scoreSchluessel);
            if (addr.isEmpty()) return null;
            Optional<XCell> cell = zelleAusAdresse(xDoc, addr.get());
            if (cell.isEmpty()) return null;
            XText text = Lo.qi(XText.class, cell.get());
            if (text == null) return null;
            String val = text.getString();
            return (val == null || val.isBlank()) ? null : val;
        } catch (Exception e) {
            LogUtil.warn(logger, "Score-Text lesen fehlgeschlagen für '" + scoreSchluessel + "'", e);
            return null;
        } catch (Error e) {
            throw e;
        }
    }

    /**
     * Löst einen Named Range zu seiner aktuellen Cell-Range-Adresse auf.
     * Liefert {@link Optional#empty()} wenn der Range fehlt, ungültig (#REF!/#BEZUG!) ist
     * oder die UNO-Schnittstellen nicht verfügbar sind.
     */
    private static Optional<CellRangeAddress> aufloeseNamedRangeAddresse(XSpreadsheetDocument xDoc,
                                                                         String scoreSchluessel) throws Exception {
        XNamedRanges namedRanges = namedRangesAusDoc(xDoc);
        if (namedRanges == null || !namedRanges.hasByName(scoreSchluessel)) return Optional.empty();
        XNamedRange range = Lo.qi(XNamedRange.class, namedRanges.getByName(scoreSchluessel));
        if (range == null) return Optional.empty();
        if (istKaputteReferenz(range.getContent())) return Optional.empty();
        XCellRangeReferrer referrer = Lo.qi(XCellRangeReferrer.class, range);
        if (referrer == null) return Optional.empty();
        XCellRangeAddressable addrAble = Lo.qi(XCellRangeAddressable.class, referrer.getReferredCells());
        if (addrAble == null) return Optional.empty();
        return Optional.of(addrAble.getRangeAddress());
    }

    /** Liefert die Zelle an der Startposition der gegebenen Range-Adresse. */
    private static Optional<XCell> zelleAusAdresse(XSpreadsheetDocument xDoc, CellRangeAddress addr) throws Exception {
        XIndexAccess sheets = Lo.qi(XIndexAccess.class, xDoc.getSheets());
        if (sheets == null) return Optional.empty();
        XSpreadsheet sheet = Lo.qi(XSpreadsheet.class, sheets.getByIndex(addr.Sheet));
        if (sheet == null) return Optional.empty();
        return Optional.ofNullable(sheet.getCellByPosition(addr.StartColumn, addr.StartRow));
    }

    /**
     * Konvertiert einen 0-basierten Spaltenindex in die Buchstaben-Notation
     * (0→A, 25→Z, 26→AA, …).
     */
    private static String spaltenNummer2Buchstabe(int spalte) {
        var sb = new StringBuilder();
        int n = spalte + 1;
        while (n > 0) {
            n--;
            sb.insert(0, (char) ('A' + (n % 26)));
            n /= 26;
        }
        return sb.toString();
    }

    public static String schluesselJgjGruppeSpielplan(String gruppenBuchstabe) {
        return SCHLUESSEL_JGJ_GRUPPE_SPIELPLAN_PREFIX + gruppenBuchstabe + SCHLUESSEL_SUFFIX;
    }

    public static String schluesselPouleSpielplan(int pouleNr) {
        return SCHLUESSEL_POULE_SPIELPLAN_PREFIX + pouleNr + SCHLUESSEL_SUFFIX;
    }

    public static String schluesselPouleKo(String buchstabe) {
        return SCHLUESSEL_POULE_KO_PREFIX + buchstabe + SCHLUESSEL_SUFFIX;
    }

    public static String schluesselKaskadenRunde(int rundeNr) {
        return SCHLUESSEL_KASKADE_RUNDE_PREFIX + rundeNr + SCHLUESSEL_SUFFIX;
    }

    public static String schluesselKaskadenFeld(String bezeichner) {
        return SCHLUESSEL_KASKADE_FELD_PREFIX + bezeichner + SCHLUESSEL_SUFFIX;
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
        } catch (Exception e) {
            LogUtil.error(logger, "Sheet-Metadaten schreiben fehlgeschlagen für '" + namedRangeKey + "'", e);
        } catch (Error e) {
            throw e;
        }
    }

    /**
     * Schreibt die Spieltag-Nummer als benannten Bereich.
     * Delegiert an {@link #schreibeSheetMetadaten}.
     */
    public static void schreibeSpieltagNr(XSpreadsheetDocument xDoc,
                                          XSpreadsheet xSheet, SpielTagNr spieltagNr) {
        String schluessel = SCHLUESSEL_SPIELTAG_RANGLISTE_PREFIX + spieltagNr.getNr() + SCHLUESSEL_SPIELTAG_RANGLISTE_SUFFIX;
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
            entferneFremdeIdentitaetsSchluessel(namedRanges, sheetIdx, namedRangeKey);
            if (namedRanges.hasByName(namedRangeKey)) {
                namedRanges.removeByName(namedRangeKey);
            }
            String escaped = sheetName.replace("'", "''");
            String inhalt = "$'" + escaped + "'.$A$1";
            CellAddress refAddr = new CellAddress();
            refAddr.Sheet = (short) sheetIdx;
            namedRanges.addNewByName(namedRangeKey, inhalt, refAddr, 0);
        } catch (Exception e) {
            LogUtil.error(logger, "Sheet-Metadaten schreiben fehlgeschlagen für '" + namedRangeKey + "'", e);
        } catch (Error e) {
            throw e;
        }
    }


    /**
     * Erzwingt die Invariante <em>„höchstens ein Identitäts-Schlüssel pro Blatt"</em>: entfernt
     * alle {@code __PTM_*}-Schlüssel (außer {@code __PTM_SCORE_*}, die legitim mit dem
     * Identitäts-Schlüssel auf Bracket-Blättern koexistieren), die bereits auf das Ziel-Blatt
     * {@code sheetIdx} zeigen – außer dem gerade zu schreibenden {@code behalteSchluessel}.
     * <p>
     * Hintergrund: Generische Blattnamen (z.B. „Meldeliste", „Rangliste", „Teilnehmer") werden
     * über mehrere Turniersysteme geteilt, jedes mit eigenem Schlüssel. Ohne diese Bereinigung
     * behält ein von System A beanspruchtes Blatt beim Wechsel zu System B den fremden
     * A-Schlüssel und erscheint doppelt in der Sidebar (zwei Einträge, dasselbe Blatt).
     * <p>
     * Konservativ: {@code #REF!}-Schlüssel (Index -1) werden hier nicht angefasst – die
     * übernimmt {@link #bereinigeVerwaisteMetadaten}.
     */
    private static void entferneFremdeIdentitaetsSchluessel(XNamedRanges namedRanges,
                                                            int sheetIdx, String behalteSchluessel) {
        if (namedRanges == null || sheetIdx < 0) {
            return;
        }
        String[] namen = namedRanges.getElementNames();
        if (namen == null) {
            return;
        }
        List<String> zuEntfernen = new ArrayList<>();
        for (String name : namen) {
            if (!name.startsWith("__PTM_") || name.startsWith("__PTM_SCORE_") || name.equals(behalteSchluessel)) {
                continue;
            }
            try {
                int idx = sheetIndexAusNamedRangeObj(namedRanges.getByName(name));
                if (idx >= 0 && idx == sheetIdx) {
                    zuEntfernen.add(name);
                }
            } catch (Exception e) {
                LogUtil.warn(logger, "Fremdschlüssel-Prüfung fehlgeschlagen für '" + name + "'", e);
            } catch (Error e) {
                throw e;
            }
        }
        for (String name : zuEntfernen) {
            try {
                namedRanges.removeByName(name);
                logger.debug("Fremden Identitäts-Schlüssel '{}' von Blatt-Index {} entfernt (Blatt-Eindeutigkeit).",
                        name, sheetIdx);
            } catch (Exception e) {
                LogUtil.warn(logger, "Entfernen des Fremdschlüssels '" + name + "' fehlgeschlagen", e);
            } catch (Error e) {
                throw e;
            }
        }
    }

    // ── Lesen / Suchen ───────────────────────────────────────────────────────

    /**
     * Liefert alle Metadaten-Schlüsselnamen, die mit dem gegebenen Prefix beginnen.
     * Nützlich für prefix-basierte Sheet-Suche, z.B. alle Spielrunden-Schlüssel.
     *
     * @param xDoc   Spreadsheet-Dokument
     * @param prefix Prefix des gesuchten Schlüssels (z.B. {@code __PTM_SCHWEIZER_SPIELRUNDE_})
     * @return Array der gefundenen Schlüsselnamen (nie null, ggf. leer)
     */
    public static String[] getSchluesselMitPrefix(XSpreadsheetDocument xDoc, String prefix) {
        try {
            XNamedRanges namedRanges = namedRangesAusDoc(xDoc);
            if (namedRanges == null) return new String[0];
            return java.util.Arrays.stream(namedRanges.getElementNames())
                    .filter(name -> name.startsWith(prefix))
                    .toArray(String[]::new);
        } catch (Exception e) {
            LogUtil.warn(logger, "Schlüssel-Lookup mit Prefix '" + prefix + "' fehlgeschlagen", e);
            return new String[0];
        } catch (Error e) {
            throw e;
        }
    }

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
                    idx -> sheetByIndex(sheets, idx),
                    namedRangeKey);
        } catch (Exception e) {
            LogUtil.warn(logger, "Sheet-Suche fehlgeschlagen für Schlüssel '" + namedRangeKey + "'", e);
            return Optional.empty();
        } catch (Error e) {
            throw e;
        }
    }

    private static XSpreadsheet sheetByIndex(XIndexAccess sheets, int idx) {
        if (sheets == null) return null;
        try {
            return Lo.qi(XSpreadsheet.class, sheets.getByIndex(idx));
        } catch (Exception e) {
            LogUtil.warn(logger, "Sheet-Lookup über Index " + idx + " fehlgeschlagen", e);
            return null;
        } catch (Error e) {
            throw e;
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
        } catch (Exception e) {
            LogUtil.warn(logger, "Fallback-Lookup für Sheet '" + fallbackName + "' fehlgeschlagen", e);
            return null;
        } catch (Error e) {
            throw e;
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
        } catch (Exception e) {
            LogUtil.warn(logger, "Sheet-Suche (intern) fehlgeschlagen für Schlüssel '" + namedRangeKey + "'", e);
            return Optional.empty();
        } catch (Error e) {
            throw e;
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
        } catch (Exception e) {
            LogUtil.warn(logger, "Sheet-Metadaten-Schlüssel prüfen fehlgeschlagen für '" + namedRangeKey + "'", e);
            return false;
        } catch (Error e) {
            throw e;
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
        } catch (Exception e) {
            LogUtil.warn(logger, "Sheet-Metadaten-Schlüssel prüfen (intern) fehlgeschlagen für '" + namedRangeKey + "'", e);
            return false;
        } catch (Error e) {
            throw e;
        }
    }

    /**
     * Prüft ob dem angegebenen Sheet irgendein Metadaten-Schlüssel zugeordnet ist,
     * der mit {@code praefix} beginnt. Nützlich für Systeme mit dynamischen
     * Schlüsseln (z.B. {@code __PTM_SCHWEIZER_SPIELRUNDE_1__}, {@code _2__} …).
     */
    public static boolean hatPraefixSchluessel(XSpreadsheetDocument xDoc,
                                               XSpreadsheet xSheet, String praefix) {
        try {
            XNamedRanges namedRanges = namedRangesAusDoc(xDoc);
            if (namedRanges == null) return false;
            int targetIdx = sheetIndex(xDoc, xSheet);
            for (String name : namedRanges.getElementNames()) {
                if (!name.startsWith(praefix)) continue;
                try {
                    Object rangeObj = namedRanges.getByName(name);
                    Integer idx = sheetIndexAusNamedRangeObj(rangeObj);
                    if (idx != null && idx == targetIdx) return true;
                } catch (Exception e) {
                    LogUtil.warn(logger, "Named-Range '" + name + "' Präfix-Check fehlgeschlagen", e);
                } catch (Error e) {
                    throw e;
                }
            }
        } catch (Exception e) {
            LogUtil.warn(logger, "Präfix-Schlüssel-Suche fehlgeschlagen für '" + praefix + "'", e);
        } catch (Error e) {
            throw e;
        }
        return false;
    }

    /**
     * Sucht die Spieltag-Nummer für das angegebene Sheet, indem alle
     * {@code __PTM_SPIELTAG_N__}-Einträge durchsucht werden.
     *
     * @return SpielTagNr wenn das Sheet zugeordnet ist, sonst {@link Optional#empty()}
     */
    public static Optional<SpielTagNr> findeSpieltagNr(XSpreadsheetDocument xDoc,
                                                       XSpreadsheet xSheet) {
        return findeSpieltagNrMitPrefix(xDoc, xSheet,
                SCHLUESSEL_SPIELTAG_RANGLISTE_PREFIX, SCHLUESSEL_SPIELTAG_RANGLISTE_SUFFIX);
    }

    /**
     * Sucht die Spieltag-Nummer für ein Supermelee-Teilnehmer-Sheet, indem alle
     * {@code __PTM_SUPERMELEE_TEILNEHMER_N__}-Einträge durchsucht werden.
     *
     * @return SpielTagNr wenn das Sheet zugeordnet ist, sonst {@link Optional#empty()}
     */
    public static Optional<SpielTagNr> findeTeilnehmerSpieltagNr(XSpreadsheetDocument xDoc,
                                                                 XSpreadsheet xSheet) {
        return findeSpieltagNrMitPrefix(xDoc, xSheet,
                SCHLUESSEL_SUPERMELEE_SPIELTAG_TEILNEHMER_PREFIX, SCHLUESSEL_SUFFIX);
    }

    /**
     * Sucht die Spieltag-Nummer für ein Supermelee-Anmeldungen-Sheet (Checkin-Liste), indem alle
     * {@code __PTM_SUPERMELEE_ANMELDUNGEN_N__}-Einträge durchsucht werden.
     *
     * @return SpielTagNr wenn das Sheet zugeordnet ist, sonst {@link Optional#empty()}
     */
    public static Optional<SpielTagNr> findeAnmeldungenSpieltagNr(XSpreadsheetDocument xDoc,
                                                                  XSpreadsheet xSheet) {
        return findeSpieltagNrMitPrefix(xDoc, xSheet,
                SCHLUESSEL_SUPERMELEE_ANMELDUNGEN_PREFIX, SCHLUESSEL_SUFFIX);
    }

    private static Optional<SpielTagNr> findeSpieltagNrMitPrefix(XSpreadsheetDocument xDoc,
                                                                 XSpreadsheet xSheet,
                                                                 String prefix, String suffix) {
        try {
            XNamedRanges namedRanges = namedRangesAusDoc(xDoc);
            if (namedRanges == null) return Optional.empty();
            int targetIdx = sheetIndex(xDoc, xSheet);
            return findeSpieltagNr(namedRanges,
                    rangeObj -> sheetIndexAusNamedRangeObj(rangeObj),
                    targetIdx, prefix, suffix);
        } catch (Exception e) {
            LogUtil.warn(logger, "Spieltag-Nr-Suche fehlgeschlagen", e);
        } catch (Error e) {
            throw e;
        }
        return Optional.empty();
    }

    /**
     * Kernlogik für {@link #findeSpieltagNr} – package-private für Unit-Tests.
     */
    static Optional<SpielTagNr> findeSpieltagNr(XNamedRanges namedRanges,
                                                Function<Object, Integer> sheetIdxAusNamedRange,
                                                int targetSheetIdx) {
        return findeSpieltagNr(namedRanges, sheetIdxAusNamedRange, targetSheetIdx,
                SCHLUESSEL_SPIELTAG_RANGLISTE_PREFIX, SCHLUESSEL_SPIELTAG_RANGLISTE_SUFFIX);
    }

    /**
     * Generische Suche nach Spieltag-Nr für einen konfigurierbaren Schlüssel-Prefix/Suffix.
     */
    static Optional<SpielTagNr> findeSpieltagNr(XNamedRanges namedRanges,
                                                Function<Object, Integer> sheetIdxAusNamedRange,
                                                int targetSheetIdx,
                                                String prefix, String suffix) {
        try {
            if (namedRanges == null) return Optional.empty();
            for (String name : namedRanges.getElementNames()) {
                if (!name.startsWith(prefix)) continue;
                if (!name.endsWith(suffix)) continue;
                try {
                    Object rangeObj = namedRanges.getByName(name);
                    Integer idx = sheetIdxAusNamedRange.apply(rangeObj);
                    if (idx != null && idx >= 0 && idx == targetSheetIdx) {
                        String nStr = name.substring(prefix.length(),
                                name.length() - suffix.length());
                        return Optional.of(SpielTagNr.from(Integer.parseInt(nStr)));
                    }
                } catch (Exception e) {
                    LogUtil.warn(logger, "Named-Range '" + name + "' prüfen fehlgeschlagen", e);
                } catch (Error e) {
                    throw e;
                }
            }
        } catch (Exception e) {
            LogUtil.warn(logger, "Spieltag-Nr-Suche (intern) fehlgeschlagen", e);
        } catch (Error e) {
            throw e;
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
                    if (istKaputteReferenz(content)) {
                        namedRanges.removeByName(name);
                        logger.debug("Verwaisten Metadaten-Schlüssel '{}' gelöscht (zeigte ins Leere).", name);
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.warn(logger, "Bereinigung verwaister Sheet-Metadaten fehlgeschlagen", e);
        } catch (Error e) {
            throw e;
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
            LogUtil.error(logger, "NamedRanges-Property nicht gefunden", e);
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
            if (istKaputteReferenz(content)) {
                logger.debug("Named-Range Referenz ist ungültig geworden ({}). Sheet wurde gelöscht.", content);
                return -1;
            }
            XCellRangeReferrer referrer = Lo.qi(XCellRangeReferrer.class, range);
            if (referrer == null) return -1;
            XCellRangeAddressable addrAble = Lo.qi(XCellRangeAddressable.class,
                    referrer.getReferredCells());
            if (addrAble == null) return -1;
            return addrAble.getRangeAddress().Sheet;
        } catch (Exception e) {
            LogUtil.warn(logger, "Named-Range-Sheet-Index auflösen fehlgeschlagen", e);
            return -1;
        } catch (Error e) {
            throw e;
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
