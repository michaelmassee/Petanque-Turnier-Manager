package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;

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

	@Test
	public void anmeldestatusKenntKeinAusgestiegen() {
		assertThat(OnlineAnmeldeStatus.aus("withdrawn")).isEmpty();
		assertThat(OnlineAnmeldeStatus.anzeige(null)).isEmpty();
	}
}
