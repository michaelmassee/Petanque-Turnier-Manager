package de.petanqueturniermanager.helper.sheet.io;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

class PdfExportTest {

	@Test
	void ungespeicherterBasisDateiname_liga() {
		assertThat(PdfExport.ungespeicherterBasisDateiname(TurnierSystem.LIGA)).isEqualTo("Liga");
	}

	@Test
	void ungespeicherterBasisDateiname_ohneTurniersystem() {
		assertThat(PdfExport.ungespeicherterBasisDateiname(null)).isEqualTo("Export");
	}
}
