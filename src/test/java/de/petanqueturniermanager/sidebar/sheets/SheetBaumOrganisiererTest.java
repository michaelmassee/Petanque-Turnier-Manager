/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.sheets;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.sidebar.sheets.BlattBaumEintrag.BlattKnoten;

/**
 * Sichert den Aufbau der Sidebar-Blätterliste ab (ohne LibreOffice).
 * <p>
 * Regression: Die system-spezifischen Builder ({@code jgjEintraege}, {@code formulexEintraege}, …)
 * filtern hartcodiert nach Schlüsseln. Ein Schlüssel, der zwar in {@link SheetGruppe} steht, aber
 * vom Builder nicht berücksichtigt wird, verschwindet stillschweigend aus der Sidebar.
 */
class SheetBaumOrganisiererTest {

	@ParameterizedTest(name = "{0}")
	@EnumSource(SheetGruppe.class)
	void keinBlattDerGruppeWirdVerschluckt(SheetGruppe gruppe) {
		List<BlattKnoten> knoten = beispielKnoten(gruppe);

		List<String> angezeigt = angezeigteSchluessel(Map.of(gruppe, knoten));

		assertThat(angezeigt).as("Alle Blätter der Gruppe %s müssen in der Sidebar erscheinen", gruppe)
				.containsExactlyInAnyOrderElementsOf(knoten.stream().map(BlattKnoten::metadatenSchluessel).toList());
	}

	@Test
	void tripTeteRanglisteStehtHinterDemSpielplan() {
		var knoten = sortiert(SheetGruppe.TRIPTETE, List.of(
				knoten(SheetMetadataHelper.SCHLUESSEL_TRIPTETE_RANGLISTE),
				knoten(SheetMetadataHelper.SCHLUESSEL_TRIPTETE_SPIELPLAN),
				knoten(SheetMetadataHelper.SCHLUESSEL_TRIPTETE_CHECKIN_LISTE),
				knoten(SheetMetadataHelper.SCHLUESSEL_TRIPTETE_MELDELISTE)));
		var teilnehmer = List.of(knoten(SheetMetadataHelper.SCHLUESSEL_TEILNEHMER));

		var map = new EnumMap<SheetGruppe, List<BlattKnoten>>(SheetGruppe.class);
		map.put(SheetGruppe.TRIPTETE, knoten);
		map.put(SheetGruppe.ALLGEMEIN, teilnehmer);

		assertThat(angezeigteSchluessel(map)).containsExactly(
				SheetMetadataHelper.SCHLUESSEL_TRIPTETE_MELDELISTE,
				SheetMetadataHelper.SCHLUESSEL_TRIPTETE_CHECKIN_LISTE,
				SheetMetadataHelper.SCHLUESSEL_TEILNEHMER,
				SheetMetadataHelper.SCHLUESSEL_TRIPTETE_SPIELPLAN,
				SheetMetadataHelper.SCHLUESSEL_TRIPTETE_RANGLISTE);
	}

	private static List<String> angezeigteSchluessel(Map<SheetGruppe, List<BlattKnoten>> gruppen) {
		var map = new EnumMap<SheetGruppe, List<BlattKnoten>>(SheetGruppe.class);
		map.putAll(gruppen);
		return new SheetBaumOrganisierer().eintraegeMitKopfAufbauen(map, Set.of(), Set.of(), Set.of()).stream()
				.filter(BlattKnoten.class::isInstance).map(BlattKnoten.class::cast)
				.map(BlattKnoten::metadatenSchluessel).toList();
	}

	/** Ein Beispiel-Blatt je Präfix der Gruppe (Präfixe mit offenem Ende erhalten eine Nummer). */
	private static List<BlattKnoten> beispielKnoten(SheetGruppe gruppe) {
		var knoten = new ArrayList<BlattKnoten>();
		for (String praefix : gruppe.praefixa()) {
			knoten.add(knoten(praefix.endsWith("__") ? praefix : praefix + "1__"));
		}
		return sortiert(gruppe, knoten);
	}

	private static List<BlattKnoten> sortiert(SheetGruppe gruppe, List<BlattKnoten> knoten) {
		return knoten.stream()
				.sorted(Comparator.comparingInt(k -> gruppe.reihenfolgeDesSchluessels(k.metadatenSchluessel())))
				.toList();
	}

	private static BlattKnoten knoten(String schluessel) {
		return new BlattKnoten(schluessel, schluessel);
	}
}
