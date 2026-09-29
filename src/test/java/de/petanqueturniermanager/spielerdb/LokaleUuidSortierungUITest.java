/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.spielerdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerListeSortModus;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXCheckinListeSheet;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetNew;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXTeilnehmerSheet;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJCheckinListeSheet;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_New;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJTeilnehmerSheet;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeCheckinListeSheet;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeMeldeListeSheetNew;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeTeilnehmerSheet;
import de.petanqueturniermanager.ko.meldeliste.KoCheckinListeSheet;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetNew;
import de.petanqueturniermanager.ko.meldeliste.KoTeilnehmerSheet;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterCheckinListeSheet;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetNew;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterTeilnehmerSheet;
import de.petanqueturniermanager.poule.meldeliste.PouleCheckinListeSheet;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetNew;
import de.petanqueturniermanager.poule.meldeliste.PouleTeilnehmerSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerCheckinListeSheet;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerTeilnehmerSheet;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;
import de.petanqueturniermanager.supermelee.meldeliste.AnmeldungenSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;
import de.petanqueturniermanager.supermelee.meldeliste.SupermeleeTeilnehmerSheet;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteCheckinListeSheet;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetNew;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteTeilnehmerSheet;

/**
 * Die lokale PTM-Online-ID (UUID) einer Meldung steht in einer ausgeblendeten Spalte der Meldeliste. Egal, auf welchem
 * Weg die Meldeliste umsortiert wird, muss jede UUID bei ihrer Meldung bleiben – sonst hängen Online-Anmeldungen an
 * fremden Teams. Geprüft wird das für jedes Turniersystem mit PTM-Online-Anbindung.
 */
class LokaleUuidSortierungUITest extends BaseCalcUITest {

    /** Umgekehrt alphabetisch, damit jede Sortierung nach Name die Zeilen tatsächlich verschiebt. */
    private static final List<String> NACHNAMEN = List.of("Zimmer", "Yildiz", "Weber", "Vogel", "Ulrich", "Schulz",
            "Richter", "Peters", "Otto", "Neumann");

    @FunctionalInterface
    private interface BlattAktion {
        void ausfuehren(WorkingSpreadsheet ws) throws Exception;
    }

    /**
     * @param anlage      legt die leere Meldeliste an
     * @param folgelisten erstellt Teilnehmer- und Check-in-Liste, die dafür die Meldeliste lesen bzw. sortieren
     */
    private record Szenario(TurnierSystem system, BlattAktion anlage, BlattAktion folgelisten) {
        @Override
        public String toString() {
            return system.name();
        }
    }

    static Stream<Arguments> systeme() {
        return Stream.of(
                new Szenario(TurnierSystem.SUPERMELEE,
                        ws -> new MeldeListeSheet_New(ws).createMeldelisteWithParams(SuperMeleeMode.Triplette),
                        ws -> {
                            new SupermeleeTeilnehmerSheet(ws).run();
                            new AnmeldungenSheet(ws).run();
                        }),
                new Szenario(TurnierSystem.SCHWEIZER,
                        ws -> new SchweizerMeldeListeSheetNew(ws).createMeldelisteWithParams(Formation.TETE, false,
                                false),
                        ws -> {
                            new SchweizerTeilnehmerSheet(ws).run();
                            new SchweizerCheckinListeSheet(ws).run();
                        }),
                new Szenario(TurnierSystem.JGJ,
                        ws -> new JGJMeldeListeSheet_New(ws).createMeldelisteWithParams(Formation.TETE, false, false,
                                SpielplanTeamAnzeige.NR),
                        ws -> {
                            new JGJTeilnehmerSheet(ws).run();
                            new JGJCheckinListeSheet(ws).run();
                        }),
                new Szenario(TurnierSystem.KO, ws -> new KoMeldeListeSheetNew(ws).createMeldelisteWithParams(),
                        ws -> {
                            new KoTeilnehmerSheet(ws).run();
                            new KoCheckinListeSheet(ws).run();
                        }),
                new Szenario(TurnierSystem.MAASTRICHTER,
                        ws -> new MaastrichterMeldeListeSheetNew(ws).erstelleMeldeliste(Formation.TETE, false, false,
                                SpielplanTeamAnzeige.NR),
                        ws -> {
                            new MaastrichterTeilnehmerSheet(ws).run();
                            new MaastrichterCheckinListeSheet(ws).run();
                        }),
                new Szenario(TurnierSystem.KASKADE,
                        ws -> new KaskadeMeldeListeSheetNew(ws).createMeldelisteWithParams(Formation.TETE, false,
                                false, 4),
                        ws -> {
                            new KaskadeTeilnehmerSheet(ws).run();
                            new KaskadeCheckinListeSheet(ws).run();
                        }),
                new Szenario(TurnierSystem.FORMULEX,
                        ws -> new FormuleXMeldeListeSheetNew(ws).createMeldelisteWithParams(Formation.TETE, false,
                                false, 4),
                        ws -> {
                            new FormuleXTeilnehmerSheet(ws).run();
                            new FormuleXCheckinListeSheet(ws).run();
                        }),
                new Szenario(TurnierSystem.POULE,
                        ws -> new PouleMeldeListeSheetNew(ws).createMeldelisteWithParams(Formation.TETE, false,
                                false),
                        ws -> {
                            new PouleTeilnehmerSheet(ws).run();
                            new PouleCheckinListeSheet(ws).run();
                        }),
                new Szenario(TurnierSystem.TRIPTETE, ws -> new TripTeteMeldeListeSheetNew(ws).createMeldeliste(),
                        ws -> {
                            new TripTeteTeilnehmerSheet(ws).run();
                            new TripTeteCheckinListeSheet(ws).run();
                        }))
                .map(Arguments::of);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("systeme")
    void aktualisierenNachNameUndNummerBehaeltUuidsJeMeldung(Szenario szenario) throws Exception {
        MeldelisteZiel ziel = meldelisteMitUuids(szenario);
        Map<String, String> vorher = uuidProBesetzung(ziel);

        aktualisieren(szenario, TeilnehmerListeSortModus.NUMMER);
        assertThat(uuidProBesetzung(ziel)).as("nach Sortierung nach Nummer").isEqualTo(vorher);

        aktualisieren(szenario, TeilnehmerListeSortModus.NAME);
        assertThat(uuidProBesetzung(ziel)).as("nach Sortierung nach Name").isEqualTo(vorher);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("systeme")
    void neueMeldungDieNachVorneSortiertWirdVerschiebtKeineUuids(Szenario szenario) throws Exception {
        MeldelisteZiel ziel = meldelisteMitUuids(szenario);
        Map<String, String> vorher = uuidProBesetzung(ziel);

        ziel.schreibeBlock(block(ziel, "Aaron", "Anfang"));
        aktualisieren(szenario, TeilnehmerListeSortModus.NAME);

        Map<String, String> nachher = uuidProBesetzung(ziel);
        assertThat(nachher).as("bestehende Meldungen behalten ihre UUID").containsAllEntriesOf(vorher);
    }

    /**
     * Beim Import aus PTM-Online bekommt eine neue Zeile ihre UUID, bevor die Meldeliste aktualisiert wird und die
     * Zeile eine Nummer erhält. Auch diese UUID muss beim Sortieren mitwandern und darf nicht verloren gehen.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("systeme")
    void uuidEinerNeuenZeileOhneNummerWandertBeimAktualisierenMit(Szenario szenario) throws Exception {
        MeldelisteZiel ziel = meldelisteMitUuids(szenario);
        Map<String, String> vorher = uuidProBesetzung(ziel);

        int neueZeile = ziel.schreibeBlockUndLiefereZeile(block(ziel, "Aaron", "Anfang"),
                MeldelisteZiel.NeueMeldungTeilnahme.INAKTIV);
        String neueUuid = ziel.getOderErzeugeLokaleUuids(List.of(neueZeile)).get(neueZeile);
        aktualisieren(szenario, TeilnehmerListeSortModus.NAME);

        Map<String, String> nachher = uuidProBesetzung(ziel);
        assertThat(nachher).as("bestehende Meldungen behalten ihre UUID").containsAllEntriesOf(vorher);
        assertThat(nachher).as("neue Meldung behält ihre UUID")
                .containsEntry(besetzung(block(ziel, "Aaron", "Anfang")), neueUuid);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("systeme")
    void teilnehmerUndCheckinListeVerschiebenKeineUuids(Szenario szenario) throws Exception {
        MeldelisteZiel ziel = meldelisteMitUuids(szenario);
        Map<String, String> vorher = uuidProBesetzung(ziel);

        szenario.folgelisten().ausfuehren(wkingSpreadsheet);

        assertThat(uuidProBesetzung(meldelisteZiel())).isEqualTo(vorher);
    }

    /**
     * Legt die Meldeliste an, schreibt die Meldungen in umgekehrt alphabetischer Reihenfolge, lässt Nummern vergeben
     * (Sortierung nach Nummer, also noch in Schreibreihenfolge) und erzeugt dann für jede Meldung eine UUID.
     */
    private MeldelisteZiel meldelisteMitUuids(Szenario szenario) throws Exception {
        szenario.anlage().ausfuehren(wkingSpreadsheet);
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, szenario.system().getId());
        MeldelisteZiel ziel = meldelisteZiel();
        for (int i = 0; i < NACHNAMEN.size(); i++) {
            ziel.schreibeBlock(block(ziel, "Spieler" + i, NACHNAMEN.get(i)));
        }
        aktualisieren(szenario, TeilnehmerListeSortModus.NUMMER);
        ziel = meldelisteZiel();
        List<Integer> zeilen = ziel.leseAlleSpielerRoh().stream().map(MeldelisteSpielerDaten::zeile1Basiert)
                .distinct().toList();
        ziel.getOderErzeugeLokaleUuids(zeilen);
        assertThat(uuidProBesetzung(ziel)).as("jede Meldung hat eine eigene UUID").hasSize(NACHNAMEN.size())
                .doesNotContainValue(null);
        return ziel;
    }

    private void aktualisieren(Szenario szenario, TeilnehmerListeSortModus sortModus) throws Exception {
        docPropHelper.setStringProperty(BasePropertiesSpalte.KONFIG_PROP_MELDELISTE_SORT_MODUS, sortModus.getKey());
        MeldelisteZielFactory.aktualisiereMeldelisteSynchron(wkingSpreadsheet, szenario.system());
    }

    private MeldelisteZiel meldelisteZiel() {
        return MeldelisteZielFactory.fuerAktivesSheet(wkingSpreadsheet).orElseThrow();
    }

    /** UUID je Besetzung (sortierte Spielernamen einer Zeile). */
    private static Map<String, String> uuidProBesetzung(MeldelisteZiel ziel) throws Exception {
        Map<Integer, List<MeldelisteSpielerDaten>> spielerProZeile = ziel.leseAlleSpielerRoh().stream()
                .collect(Collectors.groupingBy(MeldelisteSpielerDaten::zeile1Basiert, LinkedHashMap::new,
                        Collectors.toList()));
        Map<Integer, String> uuidProZeile = ziel.leseLokaleUuids(spielerProZeile.keySet());
        Map<String, String> ergebnis = new LinkedHashMap<>();
        spielerProZeile.forEach((zeile, spieler) -> ergebnis.put(
                spieler.stream().map(s -> s.vorname() + " " + s.nachname()).sorted()
                        .collect(Collectors.joining(" / ")),
                uuidProZeile.get(zeile)));
        return ergebnis;
    }

    /** Ein Block passend zur Formation: der erste Spieler mit dem Namen, weitere mit abgeleiteten Namen. */
    private static List<SpielerMitVerein> block(MeldelisteZiel ziel, String vorname, String nachname) {
        List<SpielerMitVerein> spieler = new ArrayList<>();
        for (int i = 0; i < ziel.getFormation().getAnzSpieler(); i++) {
            spieler.add(new SpielerMitVerein(0, i == 0 ? vorname : vorname + "Partner" + i, nachname, null, null,
                    List.of(), List.of(), null));
        }
        return spieler;
    }

    private static String besetzung(List<SpielerMitVerein> spieler) {
        return spieler.stream().map(s -> s.vorname() + " " + s.nachname()).sorted()
                .collect(Collectors.joining(" / "));
    }
}
