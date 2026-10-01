package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ZuordnungsVermerkeTest {

	@Test
	public void liestUndSchreibtVermerkeMitUndOhneWert() {
		ZuordnungsVermerke vermerke = ZuordnungsVermerke.lese("BEHALTEN; AUSGESCHLOSSEN=1;UNBEKANNT=x");

		assertThat(vermerke.hat(ZuordnungsVermerke.BEHALTEN)).isTrue();
		assertThat(vermerke.zahl(ZuordnungsVermerke.AUSGESCHLOSSEN)).hasValue(1);
		assertThat(vermerke.alsText()).isEqualTo("AUSGESCHLOSSEN=1;BEHALTEN;UNBEKANNT=x");
	}

	@Test
	public void aendernLaesstUnbekannteVermerkeStehen() {
		ZuordnungsVermerke vermerke = ZuordnungsVermerke.lese("UNBEKANNT=x")
				.mit(ZuordnungsVermerke.AUSGESCHLOSSEN, "-1").mit(ZuordnungsVermerke.BEHALTEN)
				.ohne(ZuordnungsVermerke.BEHALTEN);

		assertThat(vermerke.alsText()).isEqualTo("AUSGESCHLOSSEN=-1;UNBEKANNT=x");
		assertThat(vermerke.zahl(ZuordnungsVermerke.AUSGESCHLOSSEN)).hasValue(-1);
	}

	@Test
	public void leererOderFehlenderInhaltHatKeineVermerke() {
		assertThat(ZuordnungsVermerke.lese(null).alsText()).isEmpty();
		assertThat(ZuordnungsVermerke.lese(" ; ").hat(ZuordnungsVermerke.LOKAL_ENTFERNT)).isFalse();
		assertThat(ZuordnungsVermerke.lese("AUSGESCHLOSSEN=abc").zahl(ZuordnungsVermerke.AUSGESCHLOSSEN)).isEmpty();
	}
}
