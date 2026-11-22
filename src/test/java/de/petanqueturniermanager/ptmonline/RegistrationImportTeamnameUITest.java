/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;

/**
 * Online angegebene Teamnamen landen beim Import in der Teamname-Spalte der Meldeliste, sofern es sie gibt. Ein lokal
 * schon eingetragener Teamname hat Vorrang; ohne Teamname-Spalte wird der Teamname ignoriert.
 */
class RegistrationImportTeamnameUITest extends BaseCalcUITest {

    /** Tête mit Teamname: Nr | Teamname | Vorname | Nachname; ohne Teamname folgt dort der Vorname. */
    private static final int ZWEITE_SPALTE = 1;

    private SchweizerMeldeListeSheetNew meldeListe;
    private MeldelisteZiel ziel;
    private PtmOnlineRegistrationMapping mapping;

    @Test
    void onlineTeamnameWirdInDieTeamnameSpalteUebernommen() throws Exception {
        turnierAnlegen(true);

        uebernehme(anmeldung("r1", "Hans", "Müller", "Les Boulistes"), anmeldung("r2", "Anna", "Schmidt", null));

        assertThat(zweiteSpalte(ziel.findeZeileMitName("Hans Müller"))).isEqualTo("Les Boulistes");
        assertThat(zweiteSpalte(ziel.findeZeileMitName("Anna Schmidt"))).isEmpty();
    }

    @Test
    void lokalerTeamnameHatBeimVerknuepfenVorrang() throws Exception {
        turnierAnlegen(true);
        ziel.schreibeBlock(List.of(new SpielerMitVerein(0, "Hans", "Müller", null, null, List.of(), List.of(), null)));
        int zeile = ziel.findeZeileMitName("Hans Müller");
        ziel.uebernehmeOnlineTeamname(zeile, "Lokal");

        uebernehme(verknuepfen("r1", "Hans Müller"), anmeldung("r1", "Hans", "Müller", "Online"));

        assertThat(zweiteSpalte(zeile)).isEqualTo("Lokal");
    }

    @Test
    void ohneTeamnameSpalteBleibtDieMeldelisteUnveraendert() throws Exception {
        turnierAnlegen(false);

        uebernehme(anmeldung("r1", "Hans", "Müller", "Les Boulistes"));

        int zeile = ziel.findeZeileMitName("Hans Müller");
        assertThat(zeile).isPositive();
        assertThat(zweiteSpalte(zeile)).isEqualTo("Hans");
    }

    private void turnierAnlegen(boolean teamnameAnzeigen) throws Exception {
        meldeListe = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
        meldeListe.createMeldelisteWithParams(Formation.TETE, teamnameAnzeigen, false);
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
                TurnierSystem.SCHWEIZER.getId());
        ziel = MeldelisteZielFactory.fuerAktivesSheet(wkingSpreadsheet).orElseThrow();
        mapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet, TurnierSystem.SCHWEIZER, null);
        mapping.sicherstellen();
    }

    private void uebernehme(RegistrationDto... anmeldungen) throws Exception {
        uebernehme(KonfliktSammlung.ohneEntscheidungen(), anmeldungen);
    }

    private void uebernehme(KonfliktSammlung sammlung, RegistrationDto... anmeldungen) throws Exception {
        RegistrationImportTask.uebernehmeAnmeldungen(List.of(anmeldungen), mapping, ziel,
                () -> new SchweizerMeldeListeSheetUpdate(wkingSpreadsheet).vollstaendigAktualisieren(),
                AbgleichFortschritt.OHNE, sammlung);
    }

    /** Entscheidung „verknüpfen“ zur namensgleichen Zeile, wie sie die Turnierleitung in der Konfliktliste trifft. */
    private KonfliktSammlung verknuepfen(String onlineId, String lokalerName) throws Exception {
        String uuid = ziel.getOderErzeugeLokaleUuid(ziel.findeZeileMitName(lokalerName));
        return KonfliktSammlung.mit(Map.of(KonfliktFall.schluessel(KonfliktArt.MOEGLICH_IDENTISCH, uuid,
                List.of(onlineId)), Entscheidung.VERKNUEPFEN.anzeige()));
    }

    /** Inhalt der zweiten Spalte: mit Teamname-Spalte der Teamname, sonst der Vorname. */
    private String zweiteSpalte(int zeile1Basiert) throws Exception {
        return meldeListe.getSheetHelper().getTextFromCell(meldeListe.getXSpreadSheet(),
                Position.from(ZWEITE_SPALTE, zeile1Basiert - 1));
    }

    private static RegistrationDto anmeldung(String id, String vorname, String nachname, String teamname) {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("firstName", vorname);
        json.addProperty("lastName", nachname);
        json.addProperty("status", "confirmed");
        if (teamname != null) {
            json.addProperty("teamName", teamname);
        }
        return new Gson().fromJson(json, RegistrationDto.class);
    }
}
