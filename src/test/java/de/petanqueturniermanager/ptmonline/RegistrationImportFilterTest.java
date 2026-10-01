package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
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

	@Test
	public void anmeldestatusKenntKeinAusgestiegen() {
		assertThat(OnlineAnmeldeStatus.aus("withdrawn")).isEmpty();
		assertThat(OnlineAnmeldeStatus.anzeige(null)).isEmpty();
	}
}
