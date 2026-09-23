package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class OnlineTeilnahmeTest {

	@Test
	public void leereAktivZelleIstInaktiv() {
		assertThat(OnlineTeilnahme.aus(false, false)).isEqualTo(OnlineTeilnahme.INAKTIV);
		assertThat(OnlineTeilnahme.INAKTIV.apiWert()).isEqualTo("inactive");
	}

	@Test
	public void aktivZelleEinsIstAktiv() {
		assertThat(OnlineTeilnahme.aus(true, false)).isEqualTo(OnlineTeilnahme.AKTIV);
		assertThat(OnlineTeilnahme.AKTIV.apiWert()).isEqualTo("active");
	}

	@Test
	public void aktivZelleZweiIstAusgesetzt() {
		assertThat(OnlineTeilnahme.aus(false, true)).isEqualTo(OnlineTeilnahme.AUSGESETZT);
		assertThat(OnlineTeilnahme.aus(true, true)).isEqualTo(OnlineTeilnahme.AUSGESETZT);
		assertThat(OnlineTeilnahme.AUSGESETZT.apiWert()).isEqualTo("withdrawn");
	}
}
