/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;

/**
 * Regression: Ältere Turnierdateien können „Teamname" als Anzeige gespeichert haben, obwohl die
 * Meldeliste keine Teamname-Spalte führt. Früher wurden dann Spielernamen angezeigt; die Anzeige
 * muss deshalb auf {@link TeamAnzeige#SPIELERNAMEN} zurückfallen statt eine nicht vorhandene
 * Teamname-Spalte zu lesen.
 */
class TeamAnzeigePropertiesSpalteTest {

	private static final String SPIELBAUM_KEY = "Spielbaum Team Anzeige";

	private SpeicherPropertiesSpalte properties;

	@BeforeEach
	void erstelleProperties() {
		properties = new SpeicherPropertiesSpalte();
	}

	@Test
	void gespeicherterTeamnameOhneTeamnameSpalteWirdAlsSpielernamenGelesen() {
		properties.speicher.put(SPIELBAUM_KEY, TeamAnzeige.NAME.name());
		properties.teamnameAktiv = false;

		assertThat(properties.leseTeamAnzeige(SPIELBAUM_KEY, TeamAnzeige.NR)).isEqualTo(TeamAnzeige.SPIELERNAMEN);
	}

	@Test
	void gespeicherterTeamnameMitTeamnameSpalteBleibtErhalten() {
		properties.speicher.put(SPIELBAUM_KEY, TeamAnzeige.NAME.name());
		properties.teamnameAktiv = true;

		assertThat(properties.leseTeamAnzeige(SPIELBAUM_KEY, TeamAnzeige.NR)).isEqualTo(TeamAnzeige.NAME);
	}

	@Test
	void fehlenderOderUnbekannterWertErgibtDenStandard() {
		assertThat(properties.leseTeamAnzeige(SPIELBAUM_KEY, TeamAnzeige.NR)).isEqualTo(TeamAnzeige.NR);
		properties.speicher.put(SPIELBAUM_KEY, "UNBEKANNT");
		assertThat(properties.leseTeamAnzeige(SPIELBAUM_KEY, TeamAnzeige.NR)).isEqualTo(TeamAnzeige.NR);
	}

	/**
	 * Regression: Das Umschalten der Teamname-Spalte ohne Neuaufbau der Meldeliste ließ den
	 * JGJ-Spielplan scheitern. Die Anzeige wird gespeichert, die Spalte bleibt unverändert.
	 */
	@Test
	void teamnameSpeichernLaesstDieTeamnameSpalteUnveraendert() {
		properties.teamnameAktiv = false;

		properties.setRanglisteTeamAnzeige(TeamAnzeige.NAME);

		assertThat(properties.teamnameAktiv).isFalse();
		assertThat(properties.speicher).containsEntry(TeamAnzeigePropertiesSpalte.KONFIG_PROP_RANGLISTE_TEAM_ANZEIGE,
				TeamAnzeige.NAME.name());
		assertThat(properties.getRanglisteTeamAnzeige()).isEqualTo(TeamAnzeige.SPIELERNAMEN);
	}

	@Test
	void ranglisteZeigtStandardmaessigDenTeamnamenSonstSpielernamen() {
		properties.teamnameAktiv = true;
		assertThat(properties.getRanglisteTeamAnzeige()).isEqualTo(TeamAnzeige.NAME);
		properties.teamnameAktiv = false;
		assertThat(properties.getRanglisteTeamAnzeige()).isEqualTo(TeamAnzeige.SPIELERNAMEN);
	}

	/** Properties im Speicher statt in den Dokumenteigenschaften. */
	private static final class SpeicherPropertiesSpalte extends TeamAnzeigePropertiesSpalte {

		private final Map<String, String> speicher = new HashMap<>();
		private boolean teamnameAktiv;

		SpeicherPropertiesSpalte() {
			super(mock(ISheet.class));
		}

		@Override
		DocumentPropertiesHelper newDocumentPropertiesHelper(WorkingSpreadsheet wkspreadSheet) {
			return null;
		}

		@Override
		protected List<ConfigProperty<?>> getKonfigProperties() {
			return List.of();
		}

		@Override
		public String readStringProperty(String key) {
			return speicher.getOrDefault(key, "");
		}

		@Override
		public void setStringProperty(String key, String val) {
			speicher.put(key, val);
		}

		@Override
		public boolean isMeldeListeTeamnameAnzeigen() {
			return teamnameAktiv;
		}
	}
}
