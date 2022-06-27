package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;

import javax.annotation.processing.Generated;

import org.junit.Test;

@Generated(value = "org.junit-tools-1.1.0")
public class DirektvergleichResultTest {

	@Test
	public void testDirektvergleichResultAnzeigeText() throws Exception {
		assertThat(DirektvergleichResult.FEHLER.getAnzeigeText()).isEqualTo("Fehler");
		assertThat(DirektvergleichResult.KEINERGEBNIS.getAnzeigeText()).isEqualTo("Keinergebnis");
	}

	@Test
	public void testGetByCode() throws Exception {
		assertThat(DirektvergleichResult.getByCode(1)).isEqualTo(DirektvergleichResult.VERLOREN);
	}

}