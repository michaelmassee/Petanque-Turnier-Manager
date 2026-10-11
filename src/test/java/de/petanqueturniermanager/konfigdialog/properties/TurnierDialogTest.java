package de.petanqueturniermanager.konfigdialog.properties;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;

class TurnierDialogTest {

	@Test
	void sortiertDieSchweizerKonfigurationNachDemTurnierablauf() {
		List<ConfigProperty<?>> properties = new ArrayList<>(List.of(
				property("Freispiel Punkte -"),
				property("Rangliste Team Anzeige"),
				property("Spielziel"),
				property("Spielrunde Spielbahn"),
				property("Checkin-Liste Sortierung"),
				property("Editierbare Felder hervorheben"),
				property("Melee Anmeldung"),
				property("Spielrunde"),
				property("Unbekannte Systemoption"),
				property("Freispiel Punkte +")));

		properties.sort(TurnierDialog.turnierKonfigComparator());

		assertThat(properties).extracting(ConfigProperty::getKey).containsExactly(
				"Editierbare Felder hervorheben",
				"Melee Anmeldung",
				"Checkin-Liste Sortierung",
				"Spielziel",
				"Spielrunde",
				"Spielrunde Spielbahn",
				"Rangliste Team Anzeige",
				"Freispiel Punkte +",
				"Freispiel Punkte -",
				"Unbekannte Systemoption");
	}

	private static ConfigProperty<String> property(String key) {
		return ConfigProperty.from(ConfigPropertyType.STRING, key);
	}
}
