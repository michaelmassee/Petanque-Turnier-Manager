package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.SyncStandDto;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;

public class RegistrationImportFilterTest {

	private static RegistrationDto anmeldungMitStatus(String status) {
		return new Gson().fromJson("{\"id\":\"r1\",\"firstName\":\"Max\",\"lastName\":\"Muster\",\"status\":\"" + status + "\"}",
				RegistrationDto.class);
	}

	@Test
	public void nurBestaetigteAnmeldungenWerdenImportiert() {
		assertThat(RegistrationImportTask.istImportierbar(anmeldungMitStatus("confirmed"))).isTrue();
	}

	@Test
	public void offeneWartelisteUndStornierteAnmeldungenWerdenNichtImportiert() {
		assertThat(RegistrationImportTask.istImportierbar(anmeldungMitStatus("pending"))).isFalse();
		assertThat(RegistrationImportTask.istImportierbar(anmeldungMitStatus("waitlist"))).isFalse();
		assertThat(RegistrationImportTask.istImportierbar(anmeldungMitStatus("cancelled"))).isFalse();
	}

	private static RegistrationDto anmeldung(String json) {
		return new Gson().fromJson(json, RegistrationDto.class);
	}

	@Test
	public void tripletteMitZweiPersonenWirdUnvollstaendigImportiert() {
		RegistrationDto zuZweit = anmeldung("""
				{"id":"r1","status":"confirmed","club":"BC","persons":[
				  {"slot":1,"firstName":"Anna","lastName":"Adler","licenseNr":"L1"},
				  {"slot":2,"firstName":"Ben","lastName":"Berg"}]}""");

		assertThat(RegistrationImportTask.zuSpielerListe(zuZweit, Formation.TRIPLETTE))
				.extracting(SpielerMitVerein::nachname, SpielerMitVerein::lizenznr, SpielerMitVerein::vereinName)
				.containsExactly(tuple("Adler", "L1", "BC"), tuple("Berg", null, "BC"));
	}

	@Test
	public void personenzahlAusserhalbDerAnmeldeeinheitWirdNichtImportiert() {
		RegistrationDto allein = anmeldung("""
				{"id":"r1","firstName":"Anna","lastName":"Adler"}""");
		RegistrationDto zuZweit = anmeldung("""
				{"id":"r2","firstName":"Anna","lastName":"Adler","partnerFirstName":"Ben","partnerLastName":"Berg"}""");

		assertThat(RegistrationImportTask.zuSpielerListe(allein, Formation.TRIPLETTE)).isNull();
		assertThat(RegistrationImportTask.zuSpielerListe(allein, Formation.DOUBLETTE)).isNull();
		assertThat(RegistrationImportTask.zuSpielerListe(zuZweit, Formation.TETE)).isNull();
		assertThat(RegistrationImportTask.zuSpielerListe(allein, Formation.TETE)).hasSize(1);
	}

	@Test
	public void onlineStornierteUndWartendeMeldungenWerdenAusgeschlossen() {
		assertThat(RegistrationImportTask.istOnlineAusgeschlossen(anmeldungMitStatus("cancelled"))).isTrue();
		assertThat(RegistrationImportTask.istOnlineAusgeschlossen(anmeldungMitStatus("waitlist"))).isTrue();
		assertThat(RegistrationImportTask.istOnlineAusgeschlossen(anmeldungMitStatus("confirmed"))).isFalse();
		assertThat(RegistrationImportTask.istOnlineAusgeschlossen(anmeldungMitStatus("pending"))).isFalse();
	}

	private static AbgeglicheneBesetzung besetzung(String... namen) {
		List<AbgeglicheneBesetzung.Person> personen = new ArrayList<>();
		for (int i = 0; i < namen.length; i += 2) {
			personen.add(new AbgeglicheneBesetzung.Person(namen[i], namen[i + 1], null));
		}
		return AbgeglicheneBesetzung.von(personen);
	}

	@Test
	public void gleicheBesetzungBrauchtKeinenAbgleich() {
		assertThat(RegistrationImportTask.richtung(besetzung("Anna", "Adler", "Ben", "Berg"),
				besetzung("ben", "BERG", "Anna", "Adler"), besetzung("Carl", "Cramer", "Anna", "Adler")))
				.isEqualTo(RegistrationImportTask.BesetzungsRichtung.GLEICH);
	}

	@Test
	public void nurLokalGeaendertGehtOnline() {
		assertThat(RegistrationImportTask.richtung(besetzung("Anna", "Adler", "Ben", "Berg"),
				besetzung("Anna", "Adler", "Carl", "Cramer"), besetzung("Anna", "Adler", "Carl", "Cramer")))
				.isEqualTo(RegistrationImportTask.BesetzungsRichtung.NACH_ONLINE);
	}

	@Test
	public void nurOnlineGeaendertWirdLokalUebernommen() {
		assertThat(RegistrationImportTask.richtung(besetzung("Anna", "Adler", "Carl", "Cramer"),
				besetzung("Anna", "Adler", "Ben", "Berg"), besetzung("Anna", "Adler", "Carl", "Cramer")))
				.isEqualTo(RegistrationImportTask.BesetzungsRichtung.NACH_LOKAL);
	}

	@Test
	public void beidseitigUnterschiedlichGeaendertIstKonflikt() {
		assertThat(RegistrationImportTask.richtung(besetzung("Anna", "Adler", "Dora", "Dietz"),
				besetzung("Anna", "Adler", "Ben", "Berg"), besetzung("Anna", "Adler", "Carl", "Cramer")))
				.isEqualTo(RegistrationImportTask.BesetzungsRichtung.KONFLIKT);
	}

	@Test
	public void ohneGespeicherteBesetzungBleibtDasDokumentMaster() {
		assertThat(RegistrationImportTask.richtung(besetzung("Anna", "Adler"), besetzung("Anne", "Adler"),
				AbgeglicheneBesetzung.leer())).isEqualTo(RegistrationImportTask.BesetzungsRichtung.NACH_ONLINE);
	}

	@Test
	public void namensKonfliktZeigtGrundStattVorauswahl() {
		RegistrationImportTask.NamensKonflikt mitGrund = new RegistrationImportTask.NamensKonflikt("u1", "Adler",
				"Berg", false, "schon übernommen");
		assertThat(mitGrund.hinweis()).isEqualTo("schon übernommen");
	}

	@Test
	public void anmeldestatusKenntKeinAusgestiegen() {
		assertThat(OnlineAnmeldeStatus.aus("withdrawn")).isEmpty();
		assertThat(OnlineAnmeldeStatus.anzeige(null)).isEmpty();
	}

	private static SyncStandDto stand(String status, boolean geschlossen, String datum) {
		return new SyncStandDto(status, geschlossen, null, 0, 0, datum);
	}

	@Test
	public void schliessenWirdAmTurniertagOderNachDemErstenCheckinAngeboten() {
		LocalDate heute = LocalDate.of(2026, 10, 3);

		assertThat(RegistrationImportTask.schliessenAnbieten(stand("registration", false, "2026-10-03"), heute, false))
				.isTrue();
		assertThat(RegistrationImportTask.schliessenAnbieten(stand("registration", false, "2026-10-04"), heute, false))
				.as("Vortag ohne Check-in").isFalse();
		assertThat(RegistrationImportTask.schliessenAnbieten(stand("registration", false, "2026-10-04"), heute, true))
				.as("schon eingecheckt").isTrue();
		assertThat(RegistrationImportTask.schliessenAnbieten(stand("registration", true, "2026-10-03"), heute, true))
				.as("bereits geschlossen").isFalse();
		assertThat(RegistrationImportTask.schliessenAnbieten(stand("running", false, "2026-10-03"), heute, true))
				.as("läuft schon").isFalse();
		assertThat(RegistrationImportTask.schliessenAnbieten(stand("registration", false, null), heute, false))
				.as("älterer Server ohne Datum").isFalse();
		assertThat(RegistrationImportTask.schliessenAnbieten(null, heute, true)).isFalse();
	}
}
