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
import de.petanqueturniermanager.basesheet.konfiguration.MeleeAnmeldungKonfiguration;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungLeser;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungZeile;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.ptmonline.RegistrationImportTask.ImportErgebnis;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeleeAnmeldungSheet;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel.NeueMeldungTeilnahme;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;

/**
 * PTM-Online-Abgleich bei aktiver Mêlée-Anmeldung: Online-Mêlée-Turniere liefern Einzelspieler. Sie
 * gehören ins Mêlée-Anmeldung-Sheet, nicht in die Doublette-Meldeliste – dort wären sie mangels Partner
 * nicht zuordenbar.
 */
class RegistrationImportMeleeUITest extends BaseCalcUITest {

    /** Freie Zelle der Meldeliste, in der die Teamnummer-Formel des Mappings ausgewertet wird. */
    private static final Position FORMEL_TESTZELLE = Position.from(30, 0);

    private SchweizerMeldeListeSheetNew meldeListe;
    private MeldelisteZiel ziel;
    private PtmOnlineRegistrationMapping mapping;

    @BeforeEach
    void turnierMitMeleeAnmeldungAnlegen() throws Exception {
        meldeListe = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
        meldeListe.createMeldelisteWithParams(Formation.DOUBLETTE, false, false);
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
                TurnierSystem.SCHWEIZER.getId());
        MeleeAnmeldungKonfiguration.einschalten(wkingSpreadsheet);
        new SchweizerMeleeAnmeldungSheet(wkingSpreadsheet).generate();
        ziel = MeldelisteZielFactory.fuerPtmOnline(wkingSpreadsheet).orElseThrow();
        mapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet, TurnierSystem.SCHWEIZER, null);
        mapping.sicherstellen();
    }

    @Test
    void zielIstDasMeleeSheetMitEinzelspielern() {
        assertThat(ziel.istMeleeAnmeldung()).isTrue();
        assertThat(ziel.getFormation()).isEqualTo(Formation.TETE);
    }

    @Test
    void einzelanmeldungenLandenInaktivImMeleeSheet() throws Exception {
        ImportErgebnis ergebnis = uebernehme(anmeldung("r1", "Hans", "Müller"), anmeldung("r2", "Anna", "Schmidt"));

        assertThat(ergebnis).isEqualTo(new ImportErgebnis(2, List.of(), List.of()));
        assertThat(meleeZeilen()).extracting(MeleeAnmeldungZeile::anzeigeName)
                .containsExactly("Hans Müller", "Anna Schmidt");
        assertThat(meleeZeilen()).extracting(MeleeAnmeldungZeile::nr).containsExactly(1, 2);
        assertThat(meleeZeilen()).noneMatch(MeleeAnmeldungZeile::eingecheckt);
        assertThat(MeldelisteZielFactory.fuerAktivesSheet(wkingSpreadsheet).orElseThrow().leseAlleSpielerRoh())
                .as("Doublette-Meldeliste bleibt leer").isEmpty();
        assertThat(mapping.istBereitsImportiert("r1")).isTrue();
        assertThat(mapping.istBereitsImportiert("r2")).isTrue();
    }

    @Test
    void vorOrtErfassterSpielerWirdVerknuepftStattDoppeltAngelegt() throws Exception {
        ziel.schreibeBlock(List.of(spieler("Hans", "Müller")));

        ImportErgebnis ergebnis = uebernehme(anmeldung("r1", "Hans", "Müller"));

        assertThat(ergebnis).isEqualTo(new ImportErgebnis(0, List.of(), List.of()));
        assertThat(meleeZeilen()).hasSize(1);
        assertThat(mapping.istBereitsImportiert("r1")).isTrue();
    }

    @Test
    void teamnummerFormelLiefertNrDerMeleeZeile() throws Exception {
        uebernehme(anmeldung("r1", "Hans", "Müller"), anmeldung("r2", "Anna", "Schmidt"));
        String uuid = mapping.getLokaleUuid("r2").orElseThrow();

        meldeListe.getSheetHelper().setFormulaInCell(meldeListe.getXSpreadSheet(), FORMEL_TESTZELLE,
                ziel.formelTeamNrAusLokalerUuid(uuid));

        assertThat(meldeListe.getSheetHelper().getIntFromCell(meldeListe.getXSpreadSheet(), FORMEL_TESTZELLE))
                .isEqualTo(2);
    }

    @Test
    void onlineStornoEntferntEingecheckt() throws Exception {
        int zeile = ziel.schreibeBlockUndLiefereZeile(List.of(spieler("Hans", "Müller")), NeueMeldungTeilnahme.AKTIV);
        assertThat(meleeZeilen().getFirst().eingecheckt()).isTrue();

        ziel.markiereAlsAbgemeldet(zeile);

        assertThat(meleeZeilen().getFirst().eingecheckt()).isFalse();
    }

    private ImportErgebnis uebernehme(RegistrationDto... anmeldungen) throws Exception {
        return RegistrationImportTask.uebernehmeAnmeldungen(List.of(anmeldungen), mapping, ziel,
                () -> MeldelisteZielFactory.aktualisiereZielSynchron(wkingSpreadsheet, TurnierSystem.SCHWEIZER, ziel),
                AbgleichFortschritt.OHNE);
    }

    private List<MeleeAnmeldungZeile> meleeZeilen() {
        return MeleeAnmeldungLeser.lesen(wkingSpreadsheet, SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELEE_ANMELDUNG);
    }

    private static RegistrationDto anmeldung(String id, String vorname, String nachname) {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("firstName", vorname);
        json.addProperty("lastName", nachname);
        json.addProperty("status", "confirmed");
        return new Gson().fromJson(json, RegistrationDto.class);
    }

    private static SpielerMitVerein spieler(String vorname, String nachname) {
        return new SpielerMitVerein(0, vorname, nachname, null, null, List.of(), List.of(), null);
    }
}
