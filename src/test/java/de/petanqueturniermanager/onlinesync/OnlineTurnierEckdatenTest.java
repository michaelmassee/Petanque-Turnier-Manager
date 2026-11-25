/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.i18n.I18n;

/** Die Verbinden-Auswahl nennt zu jedem Online-Turnier die Eckdaten des Turniersystems. */
class OnlineTurnierEckdatenTest {

	private static final String TRENNER = " · ";

	@Test
	void kurzformNenntSystemFormationUndAnmeldeart() {
		assertThat(OnlineTurnierEckdaten.kurz(online("schweizer", "forme", "doublette")))
				.isEqualTo(TurnierSystem.SCHWEIZER.getBezeichnung() + TRENNER + Formation.DOUBLETTE.getBezeichnung()
						+ TRENNER + I18n.get("ptmonline.anmeldeart.forme"));
	}

	@Test
	void supermeleeWiederholtDieAnmeldeartNicht() {
		assertThat(OnlineTurnierEckdaten.kurz(online("rangliste", "supermelee", "triplette")))
				.isEqualTo(TurnierSystem.SUPERMELEE.getBezeichnung() + TRENNER + Formation.TRIPLETTE.getBezeichnung());
	}

	@Test
	void unbekannteTurnierartErscheintUnveraendert() {
		assertThat(OnlineTurnierEckdaten.kurz(online("jackpot", "forme", "triplette")))
				.startsWith("jackpot" + TRENNER);
	}

	@Test
	void andereFormationOhneFesteTeamgroesse() {
		OnlineTournamentDto turnier = online("ko", "forme", "tete");
		turnier.formationOther = true;

		assertThat(OnlineTurnierEckdaten.formation(turnier)).isEqualTo(I18n.get("ptmonline.formation.andere"));
	}

	@Test
	void detailsNennenBeginnStatusGrenzeUndOrtAuchOhneAnmeldezahl() {
		OnlineTournamentDto turnier = online("ko", "forme", "triplette");
		turnier.date = "2026-11-19";
		turnier.startTime = "10:00";
		turnier.maxRegistrations = 32;
		turnier.location = "Bouleplatz Karben";

		assertThat(OnlineTurnierEckdaten.details(turnier))
				.contains(feld("turniersystem", TurnierSystem.KO.getBezeichnung()))
				.contains(feld("formation", Formation.TRIPLETTE.getBezeichnung()))
				.contains(feld("anmeldeart", I18n.get("ptmonline.anmeldeart.forme")))
				.contains(feld("beginn", OnlineTurnierEckdaten.datum(turnier) + " 10:00"))
				.contains(feld("status", I18n.get("ptmonline.onlinestatus.registration")))
				.contains(feld("max_anmeldungen", "32"))
				.contains(feld("ort", "Bouleplatz Karben"));
	}

	@Test
	void detailsNennenAnmeldungenMitGrenze() {
		OnlineTournamentDto turnier = online("ko", "forme", "triplette");
		turnier.registrationCount = 12;
		turnier.maxRegistrations = 32;

		assertThat(OnlineTurnierEckdaten.details(turnier)).contains(feld("anmeldungen", "12 / 32"))
				.doesNotContain(I18n.get("ptmonline.turnier.eckdaten.max_anmeldungen"));
	}

	@Test
	void detailsNennenAnmeldungenOhneGrenze() {
		OnlineTournamentDto turnier = online("ko", "forme", "triplette");
		turnier.registrationCount = 0;

		assertThat(OnlineTurnierEckdaten.details(turnier)).contains(feld("anmeldungen", "0"));
	}

	@Test
	void datumErscheintImLokalenFormat() {
		assertThat(OnlineTurnierEckdaten.datum("2026-11-25", Locale.GERMANY)).isEqualTo("25.11.2026");
		assertThat(OnlineTurnierEckdaten.datum("2026-11-25", Locale.FRANCE)).isEqualTo("25 nov. 2026");
	}

	@Test
	void unlesbaresDatumErscheintUnveraendert() {
		assertThat(OnlineTurnierEckdaten.datum("demnächst", Locale.GERMANY)).isEqualTo("demnächst");
		assertThat(OnlineTurnierEckdaten.datum(null, Locale.GERMANY)).isEmpty();
	}

	@Test
	void detailsLassenFehlendeWerteWeg() {
		String details = OnlineTurnierEckdaten.details(online("ko", "forme", "triplette"));

		assertThat(details).doesNotContain(I18n.get("ptmonline.turnier.eckdaten.max_anmeldungen"))
				.doesNotContain(I18n.get("ptmonline.turnier.eckdaten.anmeldungen"))
				.doesNotContain(I18n.get("ptmonline.turnier.eckdaten.ort"))
				.doesNotContain(I18n.get("ptmonline.turnier.eckdaten.beginn"));
	}

	private static String feld(String schluessel, String wert) {
		return I18n.get("ptmonline.turnier.eckdaten." + schluessel) + ": " + wert;
	}

	private static OnlineTournamentDto online(String type, String registrationType, String formation) {
		OnlineTournamentDto dto = new OnlineTournamentDto();
		dto.type = type;
		dto.registrationType = registrationType;
		dto.formation = formation;
		dto.status = "registration";
		return dto;
	}
}
