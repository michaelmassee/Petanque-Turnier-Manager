package de.petanqueturniermanager.helper.rangliste;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

/**
 * Builder-Helfer für die {@link RanglisteEingabeSignatur}: stellt pro Turniersystem
 * die jeweilige Liste der {@link SignaturQuelle}n bereit.
 * <p>
 * Die Whitelist-Spalten pro Quelle ist <b>bewusst eng</b> gefasst (nur semantisch
 * relevante Eingabezellen: Spieler-/Team-Nr, Namen, Setzposition, Aktiv-Status,
 * Spielergebnisse). Hilfsspalten, Formeln und Formatierungen fließen nicht in den
 * Hash – das verhindert Phantom-Rebuilds bei reinen Anzeigeänderungen.
 * <p>
 * Sheet-Identität läuft ausschließlich über den Named-Range-Schlüssel; dynamische
 * Quellen (variabel viele Spielrunden) werden über
 * {@link SheetMetadataHelper#getSchluesselMitPrefix} ermittelt und nach Runden-Nr
 * sortiert in stabile IDs überführt.
 */
public final class SignaturQuellen {

    // ── Schweizer / Maastrichter / FormuleX: Spielrunden-Whitelist ──────────
    // TEAM_A, TEAM_B, ERG_TEAM_A, ERG_TEAM_B (= SchweizerAbstractSpielrundeSheet.* / FormuleXAbstractSpielrundeSheet.*)
    private static final Set<Integer> SPIELRUNDE_SCHWEIZER_LIKE_SPALTEN = Set.of(1, 2, 3, 4);
    private static final int SPIELRUNDE_SCHWEIZER_LIKE_ERSTE_DATEN_ZEILE = 2;
    private static final int SPIELRUNDE_SCHWEIZER_LIKE_MAX_ZEILEN = 1000;

    // ── Generische Meldeliste-Whitelist (Schweizer/Maastrichter/Poule/FormuleX/JGJ) ──
    // ERSTE_DATEN_ZEILE = 3, Spalten 0..15 decken: TeamNr (0), Spielernamen (1..N),
    // SetzPosition + Aktiv (max bis Spalte ~15 je nach Trip/Doppel/Triple-Modus).
    private static final Set<Integer> MELDELISTE_BREIT_SPALTEN = unmodifiableIntRange(0, 15);
    private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;
    private static final int MELDELISTE_MAX_ZEILEN = 1000;

    // ── Kaskade Spielrunde ─────────────────────────────────────────────────
    // GRUPPE(0), SPIEL_NR(1), TEAM_A(2), TEAM_B(3), ERG_A(4), ERG_B(5)
    private static final Set<Integer> KASKADE_SPIELRUNDE_SPALTEN = Set.of(0, 1, 2, 3, 4, 5);
    private static final int KASKADE_SPIELRUNDE_ERSTE_DATEN_ZEILE = 3;
    private static final int KASKADE_SPIELRUNDE_MAX_ZEILEN = 1000;
    private static final int KASKADE_MELDELISTE_ERSTE_DATEN_ZEILE = 3;

    // ── Poule Vorrunde ────────────────────────────────────────────────────
    // SPALTE_POULE_NR(2), SPALTE_TEAM_A_NR(3), SPALTE_TEAM_B_NR(5), SPALTE_ERG_A(7), SPALTE_ERG_B(8)
    private static final Set<Integer> POULE_VORRUNDE_SPALTEN = Set.of(2, 3, 5, 7, 8);
    private static final int POULE_VORRUNDE_ERSTE_DATEN_ZEILE = 2;
    private static final int POULE_VORRUNDE_MAX_ZEILEN = 1000;

    // ── Supermelee Spielrunde ─────────────────────────────────────────────
    // ERSTE_SPIELERNR_SPALTE(11) + SPALTE_VERTIKALE_ERGEBNISSE_PLUS(19) + MINUS(20)
    private static final Set<Integer> SUPERMELEE_SPIELRUNDE_SPALTEN = Set.of(11, 19, 20);
    private static final int SUPERMELEE_SPIELRUNDE_ERSTE_DATEN_ZEILE = 2;
    private static final int SUPERMELEE_SPIELRUNDE_MAX_ZEILEN = 1000;

    // ── Supermelee Meldeliste ─────────────────────────────────────────────
    // SPIELER_NR(0), Vorname(1), Nachname(2), SetzPos + Spieltag-Aktiv-Spalten (3..20)
    private static final Set<Integer> SUPERMELEE_MELDELISTE_SPALTEN = unmodifiableIntRange(0, 20);
    private static final int SUPERMELEE_MELDELISTE_ERSTE_DATEN_ZEILE = 2;
    private static final int SUPERMELEE_MELDELISTE_MAX_ZEILEN = 1000;

    // ── Spieltag-Rangliste (für Endrangliste-Eingabe) ─────────────────────
    // SPIELER_NR(0), Name(1), RANGLISTE(2), Spielrunden-Ergebnisse (3..20)
    private static final Set<Integer> SPIELTAG_RANGLISTE_SPALTEN = unmodifiableIntRange(0, 20);
    private static final int SPIELTAG_RANGLISTE_ERSTE_DATEN_ZEILE = 3;
    private static final int SPIELTAG_RANGLISTE_MAX_ZEILEN = 1000;

    private SignaturQuellen() {
    }

    private static Set<Integer> unmodifiableIntRange(int von, int bisInklusiv) {
        Set<Integer> set = new TreeSet<>();
        for (int i = von; i <= bisInklusiv; i++) {
            set.add(i);
        }
        return Set.copyOf(set);
    }

    // ── Bausteine ───────────────────────────────────────────────────────────

    public static SignaturQuelle meldelisteSchweizerLike(String stabileId, String schluessel) {
        return new SignaturQuelle(stabileId, schluessel, MELDELISTE_ERSTE_DATEN_ZEILE,
                MELDELISTE_MAX_ZEILEN, MELDELISTE_BREIT_SPALTEN, true);
    }

    /**
     * Erzeugt eine SignaturQuelle für eine Schweizer-/Maastrichter-/FormuleX-Spielrunde.
     */
    private static SignaturQuelle spielrundeSchweizerLike(String stabileId, String schluessel) {
        return new SignaturQuelle(stabileId, schluessel,
                SPIELRUNDE_SCHWEIZER_LIKE_ERSTE_DATEN_ZEILE,
                SPIELRUNDE_SCHWEIZER_LIKE_MAX_ZEILEN,
                SPIELRUNDE_SCHWEIZER_LIKE_SPALTEN, true);
    }

    // ── System-Builder ──────────────────────────────────────────────────────

    /**
     * Quellen für die Schweizer Rangliste: Meldeliste + alle existierenden
     * Schweizer-Spielrunden (sortiert nach Runden-Nr).
     */
    public static List<SignaturQuelle> fuerSchweizer(XSpreadsheetDocument xDoc) {
        List<SignaturQuelle> quellen = new ArrayList<>();
        quellen.add(meldelisteSchweizerLike("SCHWEIZER-MELDELISTE",
                SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELDELISTE));
        sammelePraefixSchluessel(xDoc, SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_SPIELRUNDE_PREFIX,
                "SCHWEIZER-SPIELRUNDE-", quellen, SignaturQuellen::spielrundeSchweizerLike);
        return quellen;
    }

    /** Quellen für Maastrichter Vorrunden-Rangliste. */
    public static List<SignaturQuelle> fuerMaastrichter(XSpreadsheetDocument xDoc) {
        List<SignaturQuelle> quellen = new ArrayList<>();
        quellen.add(meldelisteSchweizerLike("MAASTRICHTER-MELDELISTE",
                SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_MELDELISTE));
        sammelePraefixSchluessel(xDoc, SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX,
                "MAASTRICHTER-VORRUNDE-", quellen, SignaturQuellen::spielrundeSchweizerLike);
        return quellen;
    }

    /** Quellen für FormuleX Rangliste. */
    public static List<SignaturQuelle> fuerFormuleX(XSpreadsheetDocument xDoc) {
        List<SignaturQuelle> quellen = new ArrayList<>();
        quellen.add(meldelisteSchweizerLike("FORMULEX-MELDELISTE",
                SheetMetadataHelper.SCHLUESSEL_FORMULEX_MELDELISTE));
        sammelePraefixSchluessel(xDoc, SheetMetadataHelper.SCHLUESSEL_FORMULEX_SPIELRUNDE_PREFIX,
                "FORMULEX-SPIELRUNDE-", quellen, SignaturQuellen::spielrundeSchweizerLike);
        return quellen;
    }

    /** Quellen für JGJ-Rangliste. */
    public static List<SignaturQuelle> fuerJGJ(XSpreadsheetDocument xDoc) {
        List<SignaturQuelle> quellen = new ArrayList<>();
        quellen.add(meldelisteSchweizerLike("JGJ-MELDELISTE",
                SheetMetadataHelper.SCHLUESSEL_JGJ_MELDELISTE));
        // JGJ-Spielplan ist ein einziges Sheet, kein Prefix-Set.
        quellen.add(new SignaturQuelle("JGJ-SPIELPLAN",
                SheetMetadataHelper.SCHLUESSEL_JGJ_SPIELPLAN,
                /* ersteZeile */ 2,
                /* maxZeilen */ 5000,
                // SPIEL_NR(0), TEAM_A_NR(14), TEAM_B_NR(15), SPIELE_A(12), SPIELE_B(13),
                // SPIELPNKT_A(?), SPIELPNKT_B(?) – breit für Robustheit.
                unmodifiableIntRange(0, 18), true));
        return quellen;
    }

    /** Quellen für Poule-Vorrunden-Rangliste. */
    public static List<SignaturQuelle> fuerPoule(XSpreadsheetDocument xDoc) {
        List<SignaturQuelle> quellen = new ArrayList<>();
        quellen.add(meldelisteSchweizerLike("POULE-MELDELISTE",
                SheetMetadataHelper.SCHLUESSEL_POULE_MELDELISTE));
        quellen.add(new SignaturQuelle("POULE-VORRUNDE",
                SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDE,
                POULE_VORRUNDE_ERSTE_DATEN_ZEILE, POULE_VORRUNDE_MAX_ZEILEN,
                POULE_VORRUNDE_SPALTEN, true));
        return quellen;
    }

    /** Quellen für Kaskade Gruppen-Rangliste. */
    public static List<SignaturQuelle> fuerKaskade(XSpreadsheetDocument xDoc) {
        List<SignaturQuelle> quellen = new ArrayList<>();
        quellen.add(new SignaturQuelle("KASKADE-MELDELISTE",
                SheetMetadataHelper.SCHLUESSEL_KASKADE_MELDELISTE,
                KASKADE_MELDELISTE_ERSTE_DATEN_ZEILE, MELDELISTE_MAX_ZEILEN,
                MELDELISTE_BREIT_SPALTEN, true));
        sammelePraefixSchluessel(xDoc, SheetMetadataHelper.SCHLUESSEL_KASKADE_RUNDE_PREFIX,
                "KASKADE-RUNDE-", quellen,
                (id, key) -> new SignaturQuelle(id, key,
                        KASKADE_SPIELRUNDE_ERSTE_DATEN_ZEILE, KASKADE_SPIELRUNDE_MAX_ZEILEN,
                        KASKADE_SPIELRUNDE_SPALTEN, true));
        return quellen;
    }

    /**
     * Quellen für eine konkrete Supermelee-Spieltag-Rangliste:
     * Meldeliste + alle Spielrunden dieses Spieltags.
     */
    public static List<SignaturQuelle> fuerSupermeleeSpieltag(XSpreadsheetDocument xDoc,
            int spieltagNr) {
        List<SignaturQuelle> quellen = new ArrayList<>();
        quellen.add(new SignaturQuelle("SUPERMELEE-MELDELISTE",
                SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_MELDELISTE,
                SUPERMELEE_MELDELISTE_ERSTE_DATEN_ZEILE, SUPERMELEE_MELDELISTE_MAX_ZEILEN,
                SUPERMELEE_MELDELISTE_SPALTEN, true));
        String prefix = SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_SPIELRUNDE_PREFIX
                + spieltagNr + "_";
        sammelePraefixSchluessel(xDoc, prefix,
                "SUPERMELEE-SPIELTAG-" + spieltagNr + "-RUNDE-", quellen,
                (id, key) -> new SignaturQuelle(id, key,
                        SUPERMELEE_SPIELRUNDE_ERSTE_DATEN_ZEILE,
                        SUPERMELEE_SPIELRUNDE_MAX_ZEILEN,
                        SUPERMELEE_SPIELRUNDE_SPALTEN, true));
        return quellen;
    }

    /**
     * Quellen für Supermelee-Endrangliste: Meldeliste + alle Spieltag-Ranglisten
     * (sortiert nach Spieltag-Nr).
     */
    public static List<SignaturQuelle> fuerSupermeleeEnd(XSpreadsheetDocument xDoc) {
        List<SignaturQuelle> quellen = new ArrayList<>();
        quellen.add(new SignaturQuelle("SUPERMELEE-MELDELISTE",
                SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_MELDELISTE,
                SUPERMELEE_MELDELISTE_ERSTE_DATEN_ZEILE, SUPERMELEE_MELDELISTE_MAX_ZEILEN,
                SUPERMELEE_MELDELISTE_SPALTEN, true));
        sammelePraefixSchluessel(xDoc,
                SheetMetadataHelper.SCHLUESSEL_SPIELTAG_RANGLISTE_PREFIX,
                "SUPERMELEE-SPIELTAG-RANGLISTE-", quellen,
                (id, key) -> new SignaturQuelle(id, key,
                        SPIELTAG_RANGLISTE_ERSTE_DATEN_ZEILE, SPIELTAG_RANGLISTE_MAX_ZEILEN,
                        SPIELTAG_RANGLISTE_SPALTEN, false));
        return quellen;
    }

    // ── Helfer für Prefix-Suche ─────────────────────────────────────────────

    @FunctionalInterface
    private interface QuellenFabrik {
        SignaturQuelle erzeugen(String stabileId, String schluessel);
    }

    /**
     * Sucht alle Named-Range-Schlüssel mit dem Prefix, sortiert sie deterministisch
     * (lexikographisch nach Schlüsselname, was bei den verwendeten Schemata gleichbedeutend
     * mit "nach laufender Nummer" ist – siehe z.B. {@code __PTM_SCHWEIZER_SPIELRUNDE_3__}),
     * und fügt für jeden eine Quelle hinzu.
     */
    private static void sammelePraefixSchluessel(XSpreadsheetDocument xDoc, String prefix,
            String stabileIdPrefix, List<SignaturQuelle> ziel, QuellenFabrik fabrik) {
        String[] schluessel = SheetMetadataHelper.getSchluesselMitPrefix(xDoc, prefix);
        List<String> sortiert = new ArrayList<>(Arrays.asList(schluessel));
        sortiert.sort(Comparator.comparing(SignaturQuellen::numerischerSortierschluessel)
                .thenComparing(Comparator.naturalOrder()));
        for (String s : sortiert) {
            String suffix = s.startsWith(prefix) ? s.substring(prefix.length()) : s;
            String stabileId = stabileIdPrefix + entferneSchluesselSuffix(suffix);
            ziel.add(fabrik.erzeugen(stabileId, s));
        }
    }

    /**
     * Numerischer Sortierschlüssel: extrahiert die erste Zahl aus dem Namen für stabile
     * "nach Nummer"-Sortierung (1, 2, 3, …, 10, 11 – statt 1, 10, 11, 2, 3 lexikographisch).
     */
    private static long numerischerSortierschluessel(String name) {
        long nummer = 0;
        boolean gefunden = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c >= '0' && c <= '9') {
                nummer = nummer * 10 + (c - '0');
                gefunden = true;
            } else if (gefunden) {
                return nummer;
            }
        }
        return gefunden ? nummer : Long.MAX_VALUE;
    }

    /** Entfernt das abschließende {@code __} aus einem Named-Range-Suffix. */
    private static String entferneSchluesselSuffix(String suffix) {
        if (suffix.endsWith(SheetMetadataHelper.SCHLUESSEL_SUFFIX)) {
            return suffix.substring(0,
                    suffix.length() - SheetMetadataHelper.SCHLUESSEL_SUFFIX.length());
        }
        return suffix;
    }
}
