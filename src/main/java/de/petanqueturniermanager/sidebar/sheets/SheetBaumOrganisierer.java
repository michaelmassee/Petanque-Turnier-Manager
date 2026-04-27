package de.petanqueturniermanager.sidebar.sheets;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
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
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.sidebar.sheets.BlattBaumEintrag.BlattKnoten;
import de.petanqueturniermanager.sidebar.sheets.BlattBaumEintrag.GruppenKopf;
import de.petanqueturniermanager.sidebar.sheets.BlattBaumEintrag.SpieltagKopf;
import de.petanqueturniermanager.sidebar.sheets.BlattBaumEintrag.UnterGruppenKopf;

/**
 * Baut die Baum-Struktur der Sidebar-Blätterliste auf.
 * Liest alle PTM-Metadaten-Schlüssel aus dem Dokument, ordnet die Blätter
 * Gruppen zu und liefert eine sortierte Liste von {@link BlattBaumEintrag}-Elementen.
 * Supermelee-Blätter werden nach Spieltag untergruppiert (ohne übergeordneten Gruppen-Header).
 */
public class SheetBaumOrganisierer {

    private static final Logger logger = LogManager.getLogger(SheetBaumOrganisierer.class);

    static final String POULE_VORRUNDE_GRUPPE_ID = "POULE_VORRUNDE";
    static final String POULE_KO_GRUPPE_ID = "POULE_KO";

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
     * @param xDoc                      Spreadsheet-Dokument
     * @param kollabiert                Menge der aktuell kollabierten Turniersystem-Gruppen
     * @param kollabierteSpielTage      Menge der aktuell kollabierten Spieltag-Nummern (nur Supermelee)
     * @param kollabierteUnterGruppen   Menge der aktuell kollabierten Untergruppen-IDs (z.B. Poule)
     * @return geordnete Liste aus Gruppen-/Spieltag-Köpfen und BlattKnoten
     */
    public List<BlattBaumEintrag> baumAufbauen(XSpreadsheetDocument xDoc,
            Set<SheetGruppe> kollabiert,
            Set<Integer> kollabierteSpielTage,
            Set<String> kollabierteUnterGruppen) {
        var gruppenMap = schluesselNachGruppenSortieren(xDoc);
        return eintraegeMitKopfAufbauen(gruppenMap, kollabiert, kollabierteSpielTage, kollabierteUnterGruppen);
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
                // Supermelee/Liga/Schweizer-Knoten: keine Einrückung (erscheinen auf oberster Ebene)
                var einrueckung = (gruppe == SheetGruppe.SUPERMELEE || gruppe == SheetGruppe.LIGA
                        || gruppe == SheetGruppe.SCHWEIZER) ? "" : "  ";
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
            Set<Integer> kollabierteSpielTage,
            Set<String> kollabierteUnterGruppen) {
        var ergebnis = new ArrayList<BlattBaumEintrag>();
        // Gruppen, deren ALLGEMEIN-Teilnehmer bereits integriert wurden (Schweizer, Poule)
        var verbrauchteGruppen = new HashSet<SheetGruppe>();
        for (var gruppe : SheetGruppe.values()) {
            if (verbrauchteGruppen.contains(gruppe)) {
                continue;
            }
            var knoten = gruppenMap.get(gruppe);
            if (knoten == null || knoten.isEmpty()) {
                continue;
            }
            if (gruppe == SheetGruppe.SUPERMELEE) {
                ergebnis.addAll(supermeleeEintraege(knoten, kollabierteSpielTage));
            } else if (gruppe == SheetGruppe.LIGA) {
                ergebnis.addAll(ligaEintraege(knoten));
            } else if (gruppe == SheetGruppe.SCHWEIZER) {
                var allgemeinKnoten = gruppenMap.getOrDefault(SheetGruppe.ALLGEMEIN, List.of());
                ergebnis.addAll(schweizerEintraege(knoten, allgemeinKnoten));
                verbrauchteGruppen.add(SheetGruppe.ALLGEMEIN);
            } else if (gruppe == SheetGruppe.POULE) {
                var allgemeinKnoten = gruppenMap.getOrDefault(SheetGruppe.ALLGEMEIN, List.of());
                ergebnis.addAll(pouleEintraege(knoten, allgemeinKnoten, kollabierteUnterGruppen));
                verbrauchteGruppen.add(SheetGruppe.ALLGEMEIN);
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

    /**
     * Baut die flache Eintrags-Liste für Schweizer-Blätter auf (ohne Gruppen-Header):
     * Meldeliste → Teilnehmerliste → Spielrunden 1..n → Rangliste.
     */
    private List<BlattBaumEintrag> schweizerEintraege(List<BlattKnoten> knoten, List<BlattKnoten> allgemeinKnoten) {
        var ergebnis = new ArrayList<BlattBaumEintrag>();

        knoten.stream()
                .filter(k -> SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELDELISTE.equals(k.metadatenSchluessel()))
                .map(k -> new BlattKnoten(k.sheet(), blattName(k), k.metadatenSchluessel()))
                .forEach(ergebnis::add);

        allgemeinKnoten.stream()
                .map(k -> new BlattKnoten(k.sheet(), blattName(k), k.metadatenSchluessel()))
                .forEach(ergebnis::add);

        knoten.stream()
                .filter(k -> k.metadatenSchluessel().startsWith(SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_SPIELRUNDE_PREFIX))
                .map(k -> new BlattKnoten(k.sheet(), blattName(k), k.metadatenSchluessel()))
                .forEach(ergebnis::add);

        knoten.stream()
                .filter(k -> SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_RANGLISTE.equals(k.metadatenSchluessel()))
                .map(k -> new BlattKnoten(k.sheet(), blattName(k), k.metadatenSchluessel()))
                .forEach(ergebnis::add);

        return ergebnis;
    }

    /**
     * Baut die Eintrags-Liste für Poule-A/B-Blätter auf:
     * <ol>
     *   <li>Meldeliste (oberste Ebene)</li>
     *   <li>Teilnehmer aus ALLGEMEIN (oberste Ebene)</li>
     *   <li>Vorrunde-Untergruppe: Vorrunde-Sheet, Spielpläne, Vorrunden-Rangliste</li>
     *   <li>KO-Runde-Untergruppe: A-Finale, B-Finale</li>
     * </ol>
     */
    private List<BlattBaumEintrag> pouleEintraege(
            List<BlattKnoten> pouleKnoten,
            List<BlattKnoten> allgemeinKnoten,
            Set<String> kollabierteUnterGruppen) {
        var ergebnis = new ArrayList<BlattBaumEintrag>();

        // Meldeliste an erster Stelle ohne Einrückung
        pouleKnoten.stream()
                .filter(k -> SheetMetadataHelper.SCHLUESSEL_POULE_MELDELISTE.equals(k.metadatenSchluessel()))
                .map(k -> new BlattKnoten(k.sheet(), blattName(k), k.metadatenSchluessel()))
                .forEach(ergebnis::add);

        // Teilnehmer aus ALLGEMEIN direkt danach
        allgemeinKnoten.stream()
                .map(k -> new BlattKnoten(k.sheet(), blattName(k), k.metadatenSchluessel()))
                .forEach(ergebnis::add);

        // Vorrunde-Untergruppe
        var vorrundeKnoten = pouleKnoten.stream()
                .filter(k -> !SheetMetadataHelper.SCHLUESSEL_POULE_MELDELISTE.equals(k.metadatenSchluessel())
                        && !k.metadatenSchluessel().startsWith(SheetMetadataHelper.SCHLUESSEL_POULE_KO_PREFIX))
                .map(k -> new BlattKnoten(k.sheet(), "  " + blattName(k), k.metadatenSchluessel()))
                .toList();

        if (!vorrundeKnoten.isEmpty()) {
            var vorrundeExpandiert = !kollabierteUnterGruppen.contains(POULE_VORRUNDE_GRUPPE_ID);
            ergebnis.add(new UnterGruppenKopf(
                    POULE_VORRUNDE_GRUPPE_ID,
                    I18n.get("sidebar.sheets.poule.vorrunde.gruppe"),
                    vorrundeExpandiert));
            if (vorrundeExpandiert) {
                ergebnis.addAll(vorrundeKnoten);
            }
        }

        // KO-Runde-Untergruppe
        var koKnoten = pouleKnoten.stream()
                .filter(k -> k.metadatenSchluessel().startsWith(SheetMetadataHelper.SCHLUESSEL_POULE_KO_PREFIX))
                .map(k -> new BlattKnoten(k.sheet(), "  " + blattName(k), k.metadatenSchluessel()))
                .toList();

        if (!koKnoten.isEmpty()) {
            var koExpandiert = !kollabierteUnterGruppen.contains(POULE_KO_GRUPPE_ID);
            ergebnis.add(new UnterGruppenKopf(
                    POULE_KO_GRUPPE_ID,
                    I18n.get("sidebar.sheets.poule.ko.gruppe"),
                    koExpandiert));
            if (koExpandiert) {
                ergebnis.addAll(koKnoten);
            }
        }

        return ergebnis;
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
