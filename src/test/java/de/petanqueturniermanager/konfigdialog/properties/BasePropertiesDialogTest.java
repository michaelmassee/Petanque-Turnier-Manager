package de.petanqueturniermanager.konfigdialog.properties;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BasePropertiesDialogTest {

	@Test
	void reserviertUnterDerLetztenKonfigurationszeileEineVolleDialogzeile() {
		// 13 sichtbare Standardzeilen mit den Abständen des VerticalLayout.
		int layoutHoehe = (13 * 29) + (13 * 2);

		int dialogHoehe = BasePropertiesDialog.berechneDialogHoehe(layoutHoehe);

		assertThat(dialogHoehe)
				.as("Die letzte Konfigurationszeile braucht eine Sicherheitsreserve am unteren Rand")
				.isEqualTo((int) Math.ceil(layoutHoehe / 3.0) + 20);
	}
}
