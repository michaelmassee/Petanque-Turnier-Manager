/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.onlinesync.sheet.ZuordnungsZusatz;
import de.petanqueturniermanager.ptmonline.PtmOnlineMeldelistenSync.Aenderung;
import de.petanqueturniermanager.ptmonline.PtmOnlineMeldelistenSync.Aktion;
import de.petanqueturniermanager.ptmonline.PtmOnlineMeldelistenSync.Anlage;
import de.petanqueturniermanager.ptmonline.PtmOnlineMeldelistenSync.Ergebnis;
import de.petanqueturniermanager.ptmonline.PtmOnlineMeldelistenSync.LokaleMeldung;
import de.petanqueturniermanager.ptmonline.PtmOnlineMeldelistenSync.Storno;
import de.petanqueturniermanager.ptmonline.PtmOnlineMeldelistenSync.Zuordnungen;
import de.petanqueturniermanager.ptmonline.dto.PersonDto;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;

/**
 * Automatischer Meldelisten-Abgleich (Dokument ist Master): welche Änderungen der Meldeliste als Auftrag online gehen –
 * ohne Dokumentzugriff.
 */
class PtmOnlineMeldelistenSyncTest {

    private static final String TURNIER = "t1";
    private static final String UUID_A = "uuid-a";
    private static final String ONLINE_A = "online-a";
    private static final String UUID_B = "uuid-b";
    private static final String BESTAETIGT = "confirmed";
    private static final boolean MIT_VEREINSSPALTE = true;
    private static final boolean OHNE_VEREINSSPALTE = false;

    private final PtmOnlineMeldelistenSync sync = new PtmOnlineMeldelistenSync();

    @Test
    void abgleichStandErkenntLoeschungVorDemErstenHintergrundlauf() {
        sync.uebernehmen(new PtmOnlineStatusAuftrag(TURNIER, List.of(
                new PtmOnlineStatusAuftrag.Eintrag(UUID_A, OnlineTeilnahme.INAKTIV, null, 1))),
                Map.of(UUID_A, ONLINE_A));

        assertThat(ermittle(List.of(meldungB()), zuordnungAB()).nachlauf()).isTrue();
        assertThat(ermittle(List.of(meldungB()), zuordnungAB()).aktionen()).filteredOn(Storno.class::isInstance)
                .containsExactly(new Storno(UUID_A, ONLINE_A));
    }

    @Test
    void abgleichEinerZurueckgekehrtenZeileSetztDieLoeschbestaetigungZurueck() {
        ermittle(List.of(meldungA("Anna", "Adler", null), meldungB()), zuordnungAB());
        ermittle(List.of(meldungB()), zuordnungAB());
        sync.uebernehmen(new PtmOnlineStatusAuftrag(TURNIER, List.of(
                new PtmOnlineStatusAuftrag.Eintrag(UUID_A, OnlineTeilnahme.INAKTIV, null, 1))),
                Map.of(UUID_A, ONLINE_A));

        Ergebnis erneutFehlend = ermittle(List.of(meldungB()), zuordnungAB());

        assertThat(erneutFehlend.aktionen()).filteredOn(Storno.class::isInstance).isEmpty();
        assertThat(erneutFehlend.nachlauf()).isTrue();
    }

    @Test
    void unveraenderteMeldungErzeugtKeinenAuftrag() {
        assertThat(ermittle(List.of(meldungA("Anna", "Adler", null)), zuordnungA("Les Boulistes")).aktionen())
                .isEmpty();
    }

    @Test
    void geaenderterNameGehtMitDenBenutzerIdsUndDemZuletztAbgeglichenenTeamnamenOnline() {
        Zuordnungen zuordnungen = zuordnungen(Map.of(UUID_A, ONLINE_A), Map.of(UUID_A, new ZuordnungsZusatz(
                AbgeglicheneBesetzung.von(List.of(new AbgeglicheneBesetzung.Person("Anna", "Adler", "konto-anna"),
                        new AbgeglicheneBesetzung.Person("Ben", "Berg", null))).alsText(), "",
                AbgeglichenerTeamname.alsText("Les Boulistes"))), Set.of());
        LokaleMeldung partnerGewechselt = new LokaleMeldung(2, UUID_A,
                List.of(spieler("Anna", "Adler", 2), spieler("Bodo", "Brand", 2)), null, true);

        List<Aktion> aktionen = ermittle(List.of(partnerGewechselt), zuordnungen).aktionen();

        assertThat(aktionen).singleElement().isInstanceOfSatisfying(Aenderung.class, aenderung -> {
            assertThat(aenderung.onlineId()).isEqualTo(ONLINE_A);
            assertThat(aenderung.anmeldung().partnerFirstName()).isEqualTo("Bodo");
            assertThat(aenderung.anmeldung().teamName()).as("ohne Teamname-Spalte bleibt der Online-Teamname")
                    .isEqualTo("Les Boulistes");
            assertThat(aenderung.personen()).extracting(PersonDto::userId).as("Konto bleibt bei Anna")
                    .containsExactly("konto-anna", null);
        });
    }

    @Test
    void geaenderterTeamnameGehtOnline() {
        List<Aktion> aktionen = ermittle(List.of(meldungA("Anna", "Adler", "Carreau-Club")),
                zuordnungA("Les Boulistes")).aktionen();

        assertThat(aktionen).singleElement().isInstanceOfSatisfying(Aenderung.class,
                aenderung -> assertThat(aenderung.anmeldung().teamName()).isEqualTo("Carreau-Club"));
    }

    @Test
    void ohneAbgeglicheneBesetzungWirdNichtsGeaendert() {
        Zuordnungen ohneBesetzung = zuordnungen(Map.of(UUID_A, ONLINE_A),
                Map.of(UUID_A, new ZuordnungsZusatz("", "", "")), Set.of());

        assertThat(ermittle(List.of(meldungA("Anna", "Becker", null)), ohneBesetzung).aktionen())
                .as("ohne Benutzer-IDs löste eine Änderung die Kontoverknüpfungen").isEmpty();
    }

    @Test
    void offenerAuftragVerhindertEinenDoppelauftrag() {
        Zuordnungen gesperrt = zuordnungen(Map.of(UUID_A, ONLINE_A), Map.of(UUID_A, zusatzAnna("Les Boulistes")),
                Set.of(UUID_A));

        assertThat(ermittle(List.of(meldungA("Anna", "Becker", null)), gesperrt).aktionen()).isEmpty();
    }

    @Test
    void neueMeldungWirdBisZumTurnierstartAngelegt() {
        LokaleMeldung neu = new LokaleMeldung(5, "uuid-neu", List.of(spieler("Lena", "Lokal", 5)), "Vor-Ort", true);

        assertThat(ermittle(List.of(neu), zuordnungen(Map.of(), Map.of(), Set.of())).aktionen()).singleElement()
                .isInstanceOfSatisfying(Anlage.class, anlage -> {
                    assertThat(anlage.anmeldung().firstName()).isEqualTo("Lena");
                    assertThat(anlage.anmeldung().teamName()).isEqualTo("Vor-Ort");
                });
        assertThat(sync.ermittle(TURNIER, Formation.DOUBLETTE, MIT_VEREINSSPALTE, false, List.of(neu),
                zuordnungen(Map.of(), Map.of(), Set.of())).aktionen()).as("nach dem Start").isEmpty();
    }

    @Test
    void meldungAnEinemAnderenSpieltagWirdNichtAngelegt() {
        LokaleMeldung pool = new LokaleMeldung(5, "uuid-pool", List.of(spieler("Lena", "Lokal", 5)), null, false);

        assertThat(ermittle(List.of(pool), zuordnungen(Map.of(), Map.of(), Set.of())).aktionen()).isEmpty();
    }

    @Test
    void geloeschteMeldungWirdErstImZweitenLaufOhneZeileStorniert() {
        LokaleMeldung andere = meldungB();
        Zuordnungen zuordnungen = zuordnungAB();
        ermittle(List.of(meldungA("Anna", "Adler", null), andere), zuordnungen);

        Ergebnis ersterLauf = ermittle(List.of(andere), zuordnungen);
        assertThat(ersterLauf.aktionen()).filteredOn(Storno.class::isInstance).isEmpty();
        assertThat(ersterLauf.nachlauf()).as("zweiter Lauf ohne weitere Änderung").isTrue();

        Ergebnis zweiterLauf = ermittle(List.of(andere), zuordnungen);
        assertThat(zweiterLauf.aktionen()).filteredOn(Storno.class::isInstance).singleElement()
                .isEqualTo(new Storno(UUID_A, ONLINE_A));
        assertThat(zweiterLauf.nachlauf()).isFalse();
        Zuordnungen stornoOffen = new Zuordnungen(zuordnungen.onlineIdProUuid(), zuordnungen.zusatzProUuid(),
                zuordnungen.rohStatusProUuid(), Set.of(UUID_A));
        assertThat(ermittle(List.of(andere), stornoOffen).aktionen()).as("kein Doppelauftrag").isEmpty();
        Zuordnungen storniert = new Zuordnungen(zuordnungen.onlineIdProUuid(), zuordnungen.zusatzProUuid(),
                Map.of(UUID_A, "cancelled", UUID_B, BESTAETIGT), Set.of());
        assertThat(ermittle(List.of(andere), storniert).aktionen()).as("nach Bestätigung erledigt").isEmpty();
    }

    @Test
    void zurueckgekehrteZeileWirdNichtStorniert() {
        LokaleMeldung anna = meldungA("Anna", "Adler", null);
        LokaleMeldung andere = meldungB();
        Zuordnungen zuordnungen = zuordnungAB();
        ermittle(List.of(anna, andere), zuordnungen);
        ermittle(List.of(andere), zuordnungen);

        assertThat(ermittle(List.of(andere, anna), zuordnungen).aktionen()).as("ausgeschnitten und eingefügt")
                .isEmpty();
    }

    @Test
    void loeschungWaehrendOffenemAenderungsauftragBleibtBisZurFreigabeVorgemerkt() {
        Zuordnungen zuordnungen = zuordnungAB();
        ermittle(List.of(meldungA("Anna", "Adler", null), meldungB()), zuordnungen);
        Zuordnungen gesperrt = new Zuordnungen(zuordnungen.onlineIdProUuid(), zuordnungen.zusatzProUuid(),
                zuordnungen.rohStatusProUuid(), Set.of(UUID_A));

        for (int i = 0; i < 3; i++) {
            Ergebnis lauf = ermittle(List.of(meldungB()), gesperrt);
            assertThat(lauf.aktionen()).filteredOn(Storno.class::isInstance).isEmpty();
            assertThat(lauf.nachlauf()).as("ein offener Auftrag braucht keinen zusätzlichen Erfassungslauf").isFalse();
        }

        assertThat(ermittle(List.of(meldungB()), zuordnungen).aktionen()).filteredOn(Storno.class::isInstance)
                .containsExactly(new Storno(UUID_A, ONLINE_A));
    }

    @Test
    void leererZwischenstandVergisstBekannteMeldungenNichtUndBestaetigtKeineLoeschung() {
        Zuordnungen zuordnungen = zuordnungAB();
        ermittle(List.of(meldungA("Anna", "Adler", null), meldungB()), zuordnungen);
        ermittle(List.of(meldungB()), zuordnungen);
        assertThat(ermittle(List.of(), zuordnungen).aktionen()).isEmpty();

        Ergebnis ersterVollstaendigerLauf = ermittle(List.of(meldungB()), zuordnungen);
        assertThat(ersterVollstaendigerLauf.aktionen()).filteredOn(Storno.class::isInstance).isEmpty();
        assertThat(ersterVollstaendigerLauf.nachlauf()).isTrue();
        assertThat(ermittle(List.of(meldungB()), zuordnungen).aktionen()).filteredOn(Storno.class::isInstance)
                .containsExactly(new Storno(UUID_A, ONLINE_A));
    }

    @Test
    void leereMeldelisteStorniertNichts() {
        Zuordnungen zuordnungen = zuordnungA("Les Boulistes");
        ermittle(List.of(meldungA("Anna", "Adler", null)), zuordnungen);
        ermittle(List.of(), zuordnungen);

        assertThat(ermittle(List.of(), zuordnungen).aktionen()).as("Blatt neu aufgebaut oder nicht lesbar").isEmpty();
    }

    @Test
    void nurTeamnameSendetDenTeamnamenOhnePersonen() {
        Zuordnungen zuordnungen = zuordnungen(Map.of(UUID_A, ONLINE_A), Map.of(UUID_A,
                new ZuordnungsZusatz(AbgeglicheneBesetzung.von(List.of(new AbgeglicheneBesetzung.Person(
                        "Les Boulistes", "", null))).alsText(), "", AbgeglichenerTeamname.alsText("Les Boulistes"))),
                Set.of());
        LokaleMeldung meldung = new LokaleMeldung(2, UUID_A, List.of(spieler("Carreau-Club", "", 2)), null, true);

        assertThat(sync.ermittle(TURNIER, Formation.NUR_TEAMNAME, MIT_VEREINSSPALTE, true, List.of(meldung), zuordnungen).aktionen())
                .singleElement().isInstanceOfSatisfying(Aenderung.class, aenderung -> {
                    assertThat(aenderung.anmeldung().teamName()).isEqualTo("Carreau-Club");
                    assertThat(aenderung.anmeldung().firstName()).as("Kontaktperson bleibt online").isEmpty();
                    assertThat(aenderung.personen()).isEmpty();
                });
    }

    @Test
    void teamnamenaenderungOhneVereinsspalteBehaeltDenOnlineVerein() {
        Zuordnungen zuordnungen = zuordnungen(Map.of(UUID_A, ONLINE_A), Map.of(UUID_A, new ZuordnungsZusatz(
                AbgeglicheneBesetzung.ausOnline(List.of(new PersonDto(1, "Anna", "Adler", "L-111", "konto-anna")),
                        "BC Linden").alsText(), "", AbgeglichenerTeamname.alsText("Les Boulistes"))), Set.of());
        List<LokaleMeldung> nurTeamnameGeaendert = List.of(meldungA("Anna", "Adler", "Neuer Name"));

        assertThat(sync.ermittle(TURNIER, Formation.DOUBLETTE, OHNE_VEREINSSPALTE, true, nurTeamnameGeaendert,
                zuordnungen).aktionen()).singleElement().isInstanceOfSatisfying(Aenderung.class, aenderung -> {
                    assertThat(aenderung.anmeldung().teamName()).isEqualTo("Neuer Name");
                    assertThat(aenderung.anmeldung().club()).as("Dokument kennt keinen Verein").isEqualTo("BC Linden");
                    assertThat(aenderung.personen()).extracting(PersonDto::licenseNr).containsExactly("L-111");
                });
        assertThat(sync.ermittle(TURNIER, Formation.DOUBLETTE, MIT_VEREINSSPALTE, true, nurTeamnameGeaendert,
                zuordnungen).aktionen()).singleElement().isInstanceOfSatisfying(Aenderung.class,
                        aenderung -> assertThat(aenderung.anmeldung().club()).as("leere Vereinszelle gilt").isNull());
    }

    private Ergebnis ermittle(List<LokaleMeldung> meldungen, Zuordnungen zuordnungen) {
        return sync.ermittle(TURNIER, Formation.DOUBLETTE, MIT_VEREINSSPALTE, true, meldungen, zuordnungen);
    }

    private static LokaleMeldung meldungA(String vorname, String nachname, @Nullable String teamname) {
        return new LokaleMeldung(2, UUID_A, List.of(spieler(vorname, nachname, 2)), teamname, true);
    }

    private static LokaleMeldung meldungB() {
        return new LokaleMeldung(3, UUID_B, List.of(spieler("Ben", "Berg", 3)), null, true);
    }

    private static Zuordnungen zuordnungAB() {
        ZuordnungsZusatz zusatzBen = new ZuordnungsZusatz(AbgeglicheneBesetzung.von(List.of(
                new AbgeglicheneBesetzung.Person("Ben", "Berg", null))).alsText(), "", AbgeglichenerTeamname.alsText(""));
        return zuordnungen(Map.of(UUID_A, ONLINE_A, UUID_B, "online-b"),
                Map.of(UUID_A, zusatzAnna("Les Boulistes"), UUID_B, zusatzBen), Set.of());
    }

    private static Zuordnungen zuordnungA(String teamname) {
        return zuordnungen(Map.of(UUID_A, ONLINE_A), Map.of(UUID_A, zusatzAnna(teamname)), Set.of());
    }

    private static ZuordnungsZusatz zusatzAnna(String teamname) {
        return new ZuordnungsZusatz(AbgeglicheneBesetzung.von(List.of(new AbgeglicheneBesetzung.Person("Anna",
                "Adler", "konto-anna"))).alsText(), "", AbgeglichenerTeamname.alsText(teamname));
    }

    private static Zuordnungen zuordnungen(Map<String, String> onlineIds, Map<String, ZuordnungsZusatz> zusaetze,
            Set<String> gesperrt) {
        return new Zuordnungen(onlineIds, zusaetze, Map.of(UUID_A, BESTAETIGT, UUID_B, BESTAETIGT), gesperrt);
    }

    private static MeldelisteSpielerDaten spieler(String vorname, String nachname, int zeile) {
        return new MeldelisteSpielerDaten(vorname, nachname, null, zeile);
    }
}
