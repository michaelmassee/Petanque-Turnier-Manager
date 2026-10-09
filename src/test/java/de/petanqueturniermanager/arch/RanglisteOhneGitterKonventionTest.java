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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Quality Gate: Jede Rangliste wird ohne Tabellengitter angelegt.
 * <p>
 * Das Gitter pro Tabelle ist per UNO nicht auslesbar ({@code ShowGrid} der View ist eine globale
 * Option, {@code .uno:ToggleSheetGrid} wirkt pro Tabelle) – ein UITest kann es daher nicht prüfen.
 * Dieser Test sichert stattdessen im Quelltext ab, dass jede {@code NewSheet.from(...)}-Kette in
 * einer {@code *Rangliste*Sheet}-Klasse {@code hideGrid()} enthält (Bug: Schweizer-/Maastrichter-
 * und Formule-X-Rangliste zeigten das Gitter).
 */
class RanglisteOhneGitterKonventionTest {

	private static final Pattern NEW_SHEET_KETTE = Pattern.compile("NewSheet\\s*\\.from\\(");
	private static final String KETTEN_ENDE = ".create()";

	@Test
	void ranglistenWerdenOhneGitterAngelegt() throws IOException {
		Path quellWurzel = Paths.get("src/main/java");
		List<String> verstoesse = new ArrayList<>();

		try (Stream<Path> dateien = Files.walk(quellWurzel)) {
			dateien.filter(RanglisteOhneGitterKonventionTest::istRanglistenSheet).forEach(datei -> verstoesse
					.addAll(findeVerstoesse(quellWurzel.relativize(datei).toString(), liesDatei(datei))));
		}

		assertThat(verstoesse)
				.as("Rangliste ohne hideGrid() angelegt. In der NewSheet-Kette .hideGrid() ergänzen.")
				.isEmpty();
	}

	private static boolean istRanglistenSheet(Path datei) {
		String name = datei.getFileName().toString();
		return name.contains("Rangliste") && name.endsWith("Sheet.java");
	}

	private static List<String> findeVerstoesse(String datei, String inhalt) {
		List<String> funde = new ArrayList<>();
		Matcher kette = NEW_SHEET_KETTE.matcher(inhalt);
		while (kette.find()) {
			int ende = inhalt.indexOf(KETTEN_ENDE, kette.start());
			String aufruf = ende < 0 ? inhalt.substring(kette.start()) : inhalt.substring(kette.start(), ende);
			if (!aufruf.contains(".hideGrid()")) {
				int zeile = (int) inhalt.substring(0, kette.start()).chars().filter(c -> c == '\n').count() + 1;
				funde.add(datei + ":" + zeile);
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
