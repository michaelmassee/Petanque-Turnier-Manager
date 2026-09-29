/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerListeSortModus;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.ptmonline.RegistrationImportTask.ImportErgebnis;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;

/**
 * Zuordnung importierter Online-Anmeldungen zu bestehenden Meldelistenzeilen bei gleichen Namen:
 * eine noch nicht verknüpfte, namensgleiche Zeile wird übernommen, eine bereits verknüpfte nie. Die
 * weitere Anmeldung wird dann gemeldet statt still verworfen, und nach dem Unterscheidbarmachen des
 * lokalen Namens beim nächsten Abgleich übernommen.
 */
class RegistrationImportZuordnungUITest extends BaseCalcUITest {

    /** Tête ohne Teamname: Nr | Vorname | Nachname. */
    private static final int VORNAME_SPALTE_TETE = 1;
    private static final int NACHNAME_SPALTE_TETE = 2;

    private SchweizerMeldeListeSheetNew meldeListe;
    private MeldelisteZiel ziel;
    private PtmOnlineRegistrationMapping mapping;

    @BeforeEach
    void turnierAnlegen() throws Exception {
        meldeListe = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
        meldeListe.createMeldelisteWithParams(Formation.TETE, false, false);
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
                TurnierSystem.SCHWEIZER.getId());
        ziel = MeldelisteZielFactory.fuerAktivesSheet(wkingSpreadsheet).orElseThrow();
        mapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet, TurnierSystem.SCHWEIZER, null);
        mapping.sicherstellen();
    }

    @Test
    void namensgleicheLokaleZeileWirdVerknuepftStattNeuAngelegt() throws Exception {
        ziel.schreibeBlock(List.of(spieler("Hans", "Müller")));

        ImportErgebnis ergebnis = uebernehme(anmeldung("r1", "Hans", "Müller"));

        assertThat(ergebnis).isEqualTo(new ImportErgebnis(0, List.of(), List.of()));
        assertThat(anzahlZeilen()).isEqualTo(1);
        assertThat(mapping.istBereitsImportiert("r1")).isTrue();
    }

    /**
     * Das Aktualisieren nach dem Import sortiert die Meldeliste (hier nach Name) und verschiebt die gerade
     * geschriebenen Zeilen. Jede Online-Anmeldung muss trotzdem an der Zeile ihres Spielers hängen.
     */
    @Test
    void importNachUmsortierenVerknuepftJedeAnmeldungMitIhremSpieler() throws Exception {
        docPropHelper.setStringProperty(BasePropertiesSpalte.KONFIG_PROP_MELDELISTE_SORT_MODUS,
                TeilnehmerListeSortModus.NAME.getKey());
        List<String[]> namen = List.of(new String[] { "Zora", "Zeller" }, new String[] { "Yves", "Yilmaz" },
                new String[] { "Anna", "Adler" }, new String[] { "Moritz", "Maier" }, new String[] { "Berta", "Bauer" });
        RegistrationDto[] anmeldungen = new RegistrationDto[namen.size()];
        for (int i = 0; i < namen.size(); i++) {
            anmeldungen[i] = anmeldung("r" + i, namen.get(i)[0], namen.get(i)[1]);
        }

        ImportErgebnis ergebnis = uebernehme(anmeldungen);

        assertThat(ergebnis.importiert()).isEqualTo(namen.size());
        for (int i = 0; i < namen.size(); i++) {
            int zeile = ziel.findeZeileMitName(namen.get(i)[0] + " " + namen.get(i)[1]);
            assertThat(mapping.getLokaleUuid("r" + i)).as(namen.get(i)[0] + " " + namen.get(i)[1])
                    .contains(ziel.leseLokaleUuids(List.of(zeile)).get(zeile));
        }
    }

    @Test
    void zweiteNamensgleicheAnmeldungWirdNichtUebernommenSondernGemeldet() throws Exception {
        ziel.schreibeBlock(List.of(spieler("Hans", "Müller")));

        ImportErgebnis ergebnis = uebernehme(anmeldung("r1", "Hans", "Müller"), anmeldung("r2", "Hans", "Müller"));

        assertThat(ergebnis.namensgleich()).containsExactly("Hans Müller");
        assertThat(ergebnis.vollstaendig()).isFalse();
        assertThat(anzahlZeilen()).isEqualTo(1);
        assertThat(mapping.istBereitsImportiert("r1")).isTrue();
        assertThat(mapping.istBereitsImportiert("r2")).isFalse();
    }

    @Test
    void namensgleicheAnmeldungenImSelbenAbgleichErzeugenNurEineZeile() throws Exception {
        ImportErgebnis ergebnis = uebernehme(anmeldung("r1", "Hans", "Müller"), anmeldung("r2", "Hans", "Müller"));

        assertThat(ergebnis.importiert()).isEqualTo(1);
        assertThat(ergebnis.namensgleich()).containsExactly("Hans Müller");
        assertThat(anzahlZeilen()).isEqualTo(1);
        assertThat(mapping.istBereitsImportiert("r1")).isTrue();
    }

    @Test
    void nachUmbenennenDerLokalenZeileWirdZweiteAnmeldungUebernommen() throws Exception {
        ziel.schreibeBlock(List.of(spieler("Hans", "Müller")));
        uebernehme(anmeldung("r1", "Hans", "Müller"), anmeldung("r2", "Hans", "Müller"));

        int lokaleZeile = ziel.findeZeileMitName("Hans Müller");
        meldeListe.getSheetHelper().setStringValueInCell(StringCellValue.from(meldeListe.getXSpreadSheet(),
                Position.from(NACHNAME_SPALTE_TETE, lokaleZeile - 1), "Müller jun."));
        ImportErgebnis ergebnis = uebernehme(anmeldung("r2", "Hans", "Müller"));

        assertThat(ergebnis).isEqualTo(new ImportErgebnis(1, List.of(), List.of()));
        assertThat(anzahlZeilen()).isEqualTo(2);
        assertThat(mapping.getLokaleUuid("r2")).isPresent().isNotEqualTo(mapping.getLokaleUuid("r1"));
    }

    @Test
    void leerzeichenInOnlineNamenVerhindernZuordnungNicht() throws Exception {
        ziel.schreibeBlock(List.of(spieler("Anna", "Schmidt")));

        uebernehme(anmeldung("r1", " Anna ", "Schmidt "));

        assertThat(anzahlZeilen()).isEqualTo(1);
        assertThat(mapping.istBereitsImportiert("r1")).isTrue();
    }

    @Test
    void abweichendeSchreibweiseWieOnlineWirdVerknuepft() throws Exception {
        ziel.schreibeBlock(List.of(spieler("Jean-Paul", "Müller")));

        ImportErgebnis ergebnis = uebernehme(anmeldung("r1", "Jean Paul", "Muller"));

        assertThat(ergebnis).isEqualTo(new ImportErgebnis(0, List.of(), List.of()));
        assertThat(anzahlZeilen()).isEqualTo(1);
        assertThat(mapping.istBereitsImportiert("r1")).isTrue();
    }

    @Test
    void neuanmeldungNachOnlineStornoUebernimmtBisherigeZeile() throws Exception {
        ziel.schreibeBlock(List.of(spieler("Hans", "Müller")));
        uebernehme(anmeldung("r1", "Hans", "Müller"));
        int zeile = ziel.findeZeileMitName("Hans Müller");
        String uuid = mapping.getLokaleUuid("r1").orElseThrow();
        // Zustand nach übernommenem Online-Storno (uebernehmeOnlineStornierungen)
        ziel.markiereAlsAbgemeldet(zeile);
        mapping.setOnlineDetails(uuid, anmeldung("r1", "Hans", "Müller", "cancelled"));

        ImportErgebnis ergebnis = uebernehme(anmeldung("r2", "Hans", "Müller"));

        assertThat(ergebnis).isEqualTo(new ImportErgebnis(0, List.of(), List.of()));
        assertThat(anzahlZeilen()).isEqualTo(1);
        assertThat(mapping.getLokaleUuid("r2")).contains(uuid);
        assertThat(mapping.istBereitsImportiert("r1")).isFalse();
        assertThat(aktivWert(zeile)).as("Abmeldung aufgehoben, neue Anmeldung ist noch nicht eingecheckt")
                .isNullOrEmpty();
    }

    @Test
    void onlineSetzpositionWirdInDieMeldelisteUebernommen() throws Exception {
        uebernehme(anmeldungMitSetzposition("r1", "Hans", "Müller", 4), anmeldung("r2", "Anna", "Schmidt"));

        assertThat(ziel.getSetzpositionAusZeile(ziel.findeZeileMitName("Hans Müller"))).hasValue(4);
        assertThat(ziel.getSetzpositionAusZeile(ziel.findeZeileMitName("Anna Schmidt"))).isEmpty();
    }

    @Test
    void lokaleSetzpositionHatBeimVerknuepfenVorrang() throws Exception {
        ziel.schreibeBlock(List.of(spieler("Hans", "Müller")));
        int zeile = ziel.findeZeileMitName("Hans Müller");
        ziel.uebernehmeOnlineSetzposition(zeile, 1);

        uebernehme(anmeldungMitSetzposition("r1", "Hans", "Müller", 5));

        assertThat(mapping.istBereitsImportiert("r1")).isTrue();
        assertThat(ziel.getSetzpositionAusZeile(zeile)).hasValue(1);
    }

    @Test
    void geloeschteZeileVererbtIhreOnlineZuordnungNichtAnEineNeueMeldung() throws Exception {
        uebernehme(anmeldung("r1", "Hans", "Müller"));
        String alteUuid = mapping.getLokaleUuid("r1").orElseThrow();
        int zeile = ziel.findeZeileMitName("Hans Müller");
        meldeListe.getSheetHelper().setStringValueInCell(StringCellValue.from(meldeListe.getXSpreadSheet(),
                Position.from(VORNAME_SPALTE_TETE, zeile - 1), ""));
        meldeListe.getSheetHelper().setStringValueInCell(StringCellValue.from(meldeListe.getXSpreadSheet(),
                Position.from(NACHNAME_SPALTE_TETE, zeile - 1), ""));
        new SchweizerMeldeListeSheetUpdate(wkingSpreadsheet).vollstaendigAktualisieren();
        assertThat(ziel.leseLokaleUuids(List.of(zeile))).isEmpty();

        uebernehme(anmeldung("r2", "Anna", "Schmidt"));

        assertThat(mapping.getLokaleUuid("r2").orElseThrow()).isNotEqualTo(alteUuid);
    }

    private ImportErgebnis uebernehme(RegistrationDto... anmeldungen) throws Exception {
        return RegistrationImportTask.uebernehmeAnmeldungen(List.of(anmeldungen), mapping, ziel,
                () -> new SchweizerMeldeListeSheetUpdate(wkingSpreadsheet).vollstaendigAktualisieren(),
                AbgleichFortschritt.OHNE);
    }

    private long anzahlZeilen() {
        return ziel.leseAlleSpielerRoh().stream().map(MeldelisteSpielerDaten::zeile1Basiert).distinct().count();
    }

    private String aktivWert(int zeile1Basiert) throws GenerateException {
        return meldeListe.getSheetHelper().getTextFromCell(meldeListe.getXSpreadSheet(),
                Position.from(meldeListe.getAktivSpalte(), zeile1Basiert - 1));
    }

    private static RegistrationDto anmeldungMitSetzposition(String id, String vorname, String nachname,
            int setzposition) {
        JsonObject json = new Gson().toJsonTree(anmeldung(id, vorname, nachname)).getAsJsonObject();
        json.addProperty("seedingPosition", setzposition);
        return new Gson().fromJson(json, RegistrationDto.class);
    }

    private static RegistrationDto anmeldung(String id, String vorname, String nachname) {
        return anmeldung(id, vorname, nachname, "confirmed");
    }

    private static RegistrationDto anmeldung(String id, String vorname, String nachname, String status) {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("firstName", vorname);
        json.addProperty("lastName", nachname);
        json.addProperty("status", status);
        return new Gson().fromJson(json, RegistrationDto.class);
    }

    private static SpielerMitVerein spieler(String vorname, String nachname) {
        return new SpielerMitVerein(0, vorname, nachname, null, null, List.of(), List.of(), null);
    }
}
