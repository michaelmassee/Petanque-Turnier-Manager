package de.petanqueturniermanager.sidebar.sheets;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.container.XNamed;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.sidebar.sheets.BlattBaumEintrag.BlattKnoten;
import de.petanqueturniermanager.sidebar.sheets.BlattBaumEintrag.GruppenKopf;
import de.petanqueturniermanager.sidebar.sheets.BlattBaumEintrag.SpieltagKopf;

/**
 * Baut die Baum-Struktur der Sidebar-Blätterliste auf.
 * Liest alle PTM-Metadaten-Schlüssel aus dem Dokument, ordnet die Blätter
 * Gruppen zu und liefert eine sortierte Liste von {@link BlattBaumEintrag}-Elementen.
 * Supermelee-Blätter werden nach Spieltag untergruppiert (ohne übergeordneten Gruppen-Header).
 */
public class SheetBaumOrganisierer {

    private static final Logger logger = LogManager.getLogger(SheetBaumOrganisierer.class);

    private static final String SUPERMELEE_MELDELISTE_SCHLUESSEL = "__PTM_SUPERMELEE_MELDELISTE__";
    private static final String SUPERMELEE_ENDRANGLISTE_SCHLUESSEL = "__PTM_SUPERMELEE_ENDRANGLISTE__";

    private static final List<String> SUPERMELEE_SPIELTAG_PRAEFIXA = List.of(
            "__PTM_SUPERMELEE_ANMELDUNGEN_",
            "__PTM_SUPERMELEE_TEILNEHMER_",
            "__PTM_SUPERMELEE_SPIELRUNDE_PLAN_",
            "__PTM_SUPERMELEE_SPIELRUNDE_",
            "__PTM_SPIELTAG_"
    );

    /**
     * Erzeugt die vollständige Eintrags-Liste für die ListBox.
     *
     * @param xDoc                  Spreadsheet-Dokument
     * @param kollabiert            Menge der aktuell kollabierten Turniersystem-Gruppen
     * @param kollabierteSpielTage  Menge der aktuell kollabierten Spieltag-Nummern (nur Supermelee)
     * @return geordnete Liste aus Gruppen-/Spieltag-Köpfen und BlattKnoten
     */
    public List<BlattBaumEintrag> baumAufbauen(XSpreadsheetDocument xDoc,
            Set<SheetGruppe> kollabiert,
            Set<Integer> kollabierteSpielTage) {
        var gruppenMap = schluesselNachGruppenSortieren(xDoc);
        return eintraegeMitKopfAufbauen(gruppenMap, kollabiert, kollabierteSpielTage);
    }

    // ── Interne Methoden ─────────────────────────────────────────────────────

    private Map<SheetGruppe, List<BlattKnoten>> schluesselNachGruppenSortieren(
            XSpreadsheetDocument xDoc) {
        var gruppenMap = new EnumMap<SheetGruppe, List<BlattKnoten>>(SheetGruppe.class);
        var allKeys = SheetMetadataHelper.getSchluesselMitPrefix(xDoc, "__PTM_");
        for (var schluessel : allKeys) {
            if (schluessel.startsWith("__PTM_SCORE_")) {
                continue;
            }
            var gruppeOpt = SheetGruppe.fuerSchluessel(schluessel);
            var gruppe = gruppeOpt.orElse(SheetGruppe.ALLGEMEIN);
            SheetMetadataHelper.findeSheet(xDoc, schluessel).ifPresent(sheet -> {
                // Supermelee/Liga-Knoten: keine Einrückung (erscheinen auf oberster Ebene)
                var einrueckung = (gruppe == SheetGruppe.SUPERMELEE || gruppe == SheetGruppe.LIGA) ? "" : "  ";
                var knoten = knoten(sheet, schluessel, einrueckung);
                if (knoten != null) {
                    gruppenMap.computeIfAbsent(gruppe, g -> new ArrayList<>()).add(knoten);
                }
            });
        }
        gruppenMap.values().forEach(liste -> liste.sort(this::knotenVergleichen));
        return gruppenMap;
    }

    private BlattKnoten knoten(XSpreadsheet sheet, String schluessel, String einrueckung) {
        var named = Lo.qi(XNamed.class, sheet);
        if (named == null) {
            logger.warn("BlattKnoten: XNamed nicht verfügbar für Schlüssel '{}'", schluessel);
            return null;
        }
        return new BlattKnoten(sheet, einrueckung + named.getName(), schluessel);
    }

    private List<BlattBaumEintrag> eintraegeMitKopfAufbauen(
            Map<SheetGruppe, List<BlattKnoten>> gruppenMap,
            Set<SheetGruppe> kollabiert,
            Set<Integer> kollabierteSpielTage) {
        var ergebnis = new ArrayList<BlattBaumEintrag>();
        for (var gruppe : SheetGruppe.values()) {
            var knoten = gruppenMap.get(gruppe);
            if (knoten == null || knoten.isEmpty()) {
                continue;
            }
            if (gruppe == SheetGruppe.SUPERMELEE) {
                // Kein GruppenKopf – Spieltage erscheinen direkt auf oberster Ebene
                ergebnis.addAll(supermeleeEintraege(knoten, kollabierteSpielTage));
            } else if (gruppe == SheetGruppe.LIGA) {
                // Kein GruppenKopf – Liga-Sheets direkt auf oberster Ebene
                ergebnis.addAll(ligaEintraege(knoten));
            } else {
                var expandiert = !kollabiert.contains(gruppe);
                ergebnis.add(new GruppenKopf(gruppe, expandiert));
                if (expandiert) {
                    ergebnis.addAll(knoten);
                }
            }
        }
        return ergebnis;
    }

    /**
     * Baut die flache Eintrags-Liste für Supermelee-Blätter auf:
     * Meldeliste → Spieltag-Untergruppen (mit Teams in Spieltag 1) → Endrangliste.
     */
    private List<BlattBaumEintrag> supermeleeEintraege(List<BlattKnoten> knoten,
            Set<Integer> kollabierteSpielTage) {
        var ergebnis = new ArrayList<BlattBaumEintrag>();

        Optional<BlattKnoten> meldeliste = knoten.stream()
                .filter(k -> SUPERMELEE_MELDELISTE_SCHLUESSEL.equals(k.metadatenSchluessel()))
                .findFirst();
        Optional<BlattKnoten> endrangliste = knoten.stream()
                .filter(k -> SUPERMELEE_ENDRANGLISTE_SCHLUESSEL.equals(k.metadatenSchluessel()))
                .findFirst();

        // Spieltage nach Nummer gruppieren; Blätter ohne Spieltagnummer (Teams) → Spieltag 1
        var spieltageMap = new TreeMap<Integer, List<BlattKnoten>>();
        for (var k : knoten) {
            if (SUPERMELEE_MELDELISTE_SCHLUESSEL.equals(k.metadatenSchluessel())
                    || SUPERMELEE_ENDRANGLISTE_SCHLUESSEL.equals(k.metadatenSchluessel())) {
                continue;
            }
            int spieltagNr = Optional.ofNullable(spieltagNrAusSchluessel(k.metadatenSchluessel()))
                    .orElse(1);
            var mitEinrueckung = new BlattKnoten(k.sheet(), "  " + blattName(k), k.metadatenSchluessel());
            spieltageMap.computeIfAbsent(spieltagNr, n -> new ArrayList<>()).add(mitEinrueckung);
        }
        spieltageMap.values().forEach(liste -> liste.sort(this::knotenVergleichen));

        meldeliste.map(k -> new BlattKnoten(k.sheet(), blattName(k), k.metadatenSchluessel()))
                .ifPresent(ergebnis::add);

        for (var eintrag : spieltageMap.entrySet()) {
            int nr = eintrag.getKey();
            var expandiert = !kollabierteSpielTage.contains(nr);
            ergebnis.add(new SpieltagKopf(nr, expandiert));
            if (expandiert) {
                ergebnis.addAll(eintrag.getValue());
            }
        }

        endrangliste.map(k -> new BlattKnoten(k.sheet(), blattName(k), k.metadatenSchluessel()))
                .ifPresent(ergebnis::add);

        return ergebnis;
    }

    /**
     * Baut die flache Eintrags-Liste für Liga-Blätter auf: direkt auf oberster Ebene, ohne Gruppen-Header.
     * Die Reihenfolge entspricht der Sortierung in {@link SheetGruppe#LIGA}.
     */
    private List<BlattBaumEintrag> ligaEintraege(List<BlattKnoten> knoten) {
        return new ArrayList<>(knoten);
    }

    /** Gibt den reinen Blattnamen ohne Einrückung zurück. */
    private String blattName(BlattKnoten knoten) {
        return knoten.anzeigeText().stripLeading();
    }

    /**
     * Extrahiert die Spieltag-Nummer aus einem Supermelee-Metadaten-Schlüssel.
     * Gibt {@code null} zurück, wenn kein Spieltag-Präfix passt (z.B. Teams-Schlüssel).
     */
    private Integer spieltagNrAusSchluessel(String schluessel) {
        for (var praefix : SUPERMELEE_SPIELTAG_PRAEFIXA) {
            if (schluessel.startsWith(praefix)) {
                return ersteZahl(schluessel.substring(praefix.length()));
            }
        }
        return null;
    }

    private Integer ersteZahl(String text) {
        int start = 0;
        while (start < text.length() && !Character.isDigit(text.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < text.length() && Character.isDigit(text.charAt(end))) {
            end++;
        }
        if (start == end) {
            return null;
        }
        try {
            return Integer.parseInt(text.substring(start, end));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int knotenVergleichen(BlattKnoten a, BlattKnoten b) {
        var gruppeA = SheetGruppe.fuerSchluessel(a.metadatenSchluessel()).orElse(SheetGruppe.ALLGEMEIN);
        int rangA = gruppeA.reihenfolgeDesSchluessels(a.metadatenSchluessel());
        int rangB = gruppeA.reihenfolgeDesSchluessels(b.metadatenSchluessel());
        if (rangA != rangB) {
            return Integer.compare(rangA, rangB);
        }
        return vergleicheNumerischSuffix(a.metadatenSchluessel(), b.metadatenSchluessel());
    }

    /** Vergleicht zwei Schlüssel numerisch-bewusst, d.h. Runde_2 vor Runde_10. */
    private int vergleicheNumerischSuffix(String a, String b) {
        var numA = numerischesEnde(a);
        var numB = numerischesEnde(b);
        if (numA != null && numB != null) {
            int cmp = Integer.compare(numA, numB);
            if (cmp != 0) {
                return cmp;
            }
        }
        return a.compareTo(b);
    }

    private Integer numerischesEnde(String schluessel) {
        if (!schluessel.endsWith("__")) {
            return null;
        }
        int ende = schluessel.length() - 2;
        int start = ende;
        while (start > 0 && Character.isDigit(schluessel.charAt(start - 1))) {
            start--;
        }
        if (start == ende) {
            return null;
        }
        try {
            return Integer.parseInt(schluessel.substring(start, ende));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
