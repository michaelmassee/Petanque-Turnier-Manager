/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.arch;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Quality Gate: Die Berechnungs-Schnittstelle {@code XCalculatable} darf nur über
 * {@code de.petanqueturniermanager.helper.Neuberechnung} (bzw. {@code WorkingSpreadsheet.getxCalculatable()},
 * das darüber läuft) bezogen werden.
 * <p>
 * Grund: {@code calculate()}/{@code calculateAll()} schließen in LibreOffice eine laufende Zelleingabe ab
 * ({@code ScDocShell::DoRecalc} → {@code ScModule::InputEnterHandler()}). Aus einem Hintergrundlauf heraus
 * „verschluckt“ das beim zügigen Tippen zweistelliger Ergebnisse die erste Ziffer. Nur über
 * {@code Neuberechnung} greift der eingabeschonende Modus der Hintergrundläufe.
 * <p>
 * Ausnahmen sind ausschließlich Stellen, die nie parallel zur Ergebniserfassung laufen (siehe
 * {@link #AUSNAHMEN}).
 */
class NeuberechnungEingabeschonendKonventionTest {

	private static final String MUSTER = "XCalculatable.class";

	/** Pfade relativ zu {@code src/main/java}, jeweils mit Begründung. */
	private static final Set<String> AUSNAHMEN = Set.of(
			// Testdaten-Erzeugung per Menü: läuft nie während einer Zelleingabe.
			"de/petanqueturniermanager/ko/KoTurnierTestDaten.java",
			"de/petanqueturniermanager/jedergegenjeden/spielplan/JGJTriplette2Gruppen4TurnierTestDaten.java",
			// Beim Laden des Dokuments, bevor der Anwender tippen kann; braucht Hard-Recalc der Add-in-Formeln.
			"de/petanqueturniermanager/addins/UpdatePropertieFunctionsSheetRecalcOnLoad.java",
			// Nach dem Speichern im Konfigurationsdialog (Benutzeraktion); braucht Hard-Recalc der Add-in-Formeln.
			"de/petanqueturniermanager/basesheet/konfiguration/BasePropertiesSpalte.java");

	@Test
	void berechnungNurUeberNeuberechnung() throws IOException {
		Path quellWurzel = Paths.get("src/main/java");
		List<String> verstoesse = new ArrayList<>();

		try (Stream<Path> dateien = Files.walk(quellWurzel)) {
			dateien.filter(p -> p.toString().endsWith(".java")).forEach(datei -> {
				String relativ = quellWurzel.relativize(datei).toString().replace('\\', '/');
				if (!AUSNAHMEN.contains(relativ)) {
					verstoesse.addAll(findeVerstoesse(relativ, liesDatei(datei)));
				}
			});
		}

		assertThat(verstoesse)
				.as("XCalculatable direkt bezogen. calculate()/calculateAll() brechen eine laufende Zelleingabe "
						+ "des Anwenders ab. Stattdessen WorkingSpreadsheet.getxCalculatable() oder "
						+ "Neuberechnung.fuer(...)/nurFallsAutomatikAus(...) verwenden (siehe CLAUDE.md, Abschnitt "
						+ "'Hintergrundläufe: keine Neuberechnung während der Zelleingabe').")
				.isEmpty();
	}

	private static List<String> findeVerstoesse(String datei, String inhalt) {
		List<String> funde = new ArrayList<>();
		String[] zeilen = inhalt.split("\n", -1);
		for (int i = 0; i < zeilen.length; i++) {
			String zeile = zeilen[i];
			if (zeile.contains(MUSTER) && !zeile.contains("Neuberechnung.")) {
				funde.add(datei + ":" + (i + 1) + ": " + zeile.strip());
			}
		}
		return funde;
	}

	private static String liesDatei(Path datei) {
		try {
			return Files.readString(datei);
		} catch (IOException e) {
			throw new IllegalStateException("Fehler beim Lesen von " + datei, e);
		}
	}
}
