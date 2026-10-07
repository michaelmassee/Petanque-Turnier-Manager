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
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Quality Gate: Die Zeilen-Zebrafarbe darf nie als bedingte Formatierung gesetzt werden.
 * <p>
 * Gründe:
 * <ul>
 * <li>Bedingte Formatierungen gehen bei der HTML-Generierung verloren – das Zebra fehlt dann im
 * Export.</li>
 * <li>Eine unbedingte Zebra-Regel ({@code ISEVEN(ROW())}) greift immer und verdeckt jede danach
 * angehängte Regel, insbesondere die Editierfarbe aus {@code EditierbaresZelleFormatHelper}
 * (Bug: Formule-X-Meldeliste zeigte Teamname/Verein/SP/Aktiv nicht als editierbar).</li>
 * </ul>
 * Zebra wird stattdessen direkt per {@code SheetHelper.faerbeZeilenAbwechselnd} als
 * Zellhintergrund geschrieben. Zusammengesetzte Bedingungen wie {@code AND(ISEVEN(ROW());…)}
 * (z.B. Editierfarbe mit Property-Schalter, Ranglisten-Hervorhebungen) bleiben erlaubt.
 * <p>
 * Ergänzt den Laufzeit-Guard {@code ConditionalFormatHelper.istReineZebraFormel} (der zur Laufzeit nur loggt
 * und überspringt, damit die Generierung nie abbricht): dieser Test
 * schlägt schon beim Build fehl, nicht erst beim Sheet-Aufbau in LibreOffice.
 */
class ZebraNichtPerConditionalFormatKonventionTest {

	/** Einzige Datei, die die Zebra-Formeln (als Konstanten für zusammengesetzte Bedingungen) definiert. */
	private static final String DEFINIERENDE_DATEI = "de/petanqueturniermanager/helper/sheet/ConditionalFormatHelper.java";

	private static final List<Pattern> VERBOTENE_MUSTER = List.of(
			// alte Builder-Methoden für reine Zebra-CF
			Pattern.compile("\\.formulaIs(Even|Odd)Row\\(\\)"),
			// reine Zebra-Konstante direkt als Bedingung
			Pattern.compile("formula1\\(\\s*(ConditionalFormatHelper\\.)?FORMULA_IS(EVEN|ODD)_ROW\\s*\\)"),
			// reine Zebra-Formel als String-Literal
			Pattern.compile("\"\\s*IS(EVEN|ODD)\\(\\s*ROW\\(\\s*\\)\\s*\\)\\s*\""));

	@Test
	void keineZebraZeilenfarbeAlsBedingteFormatierung() throws IOException {
		Path quellWurzel = Paths.get("src/main/java");
		List<String> verstoesse = new ArrayList<>();

		try (Stream<Path> dateien = Files.walk(quellWurzel)) {
			dateien.filter(p -> p.toString().endsWith(".java")).forEach(datei -> {
				String relativ = quellWurzel.relativize(datei).toString().replace('\\', '/');
				if (!DEFINIERENDE_DATEI.equals(relativ)) {
					verstoesse.addAll(findeVerstoesse(relativ, liesDatei(datei)));
				}
			});
		}

		assertThat(verstoesse)
				.as("Zebra-Zeilenfarbe als bedingte Formatierung gefunden. Bedingte Formatierungen gehen "
						+ "bei der HTML-Generierung verloren und verdecken die Editierfarbe. Zebra direkt per "
						+ "SheetHelper.faerbeZeilenAbwechselnd schreiben (siehe CLAUDE.md, Abschnitt "
						+ "'Zebra-Zeilenfarbe & Editierfarbe').")
				.isEmpty();
	}

	private static List<String> findeVerstoesse(String datei, String inhalt) {
		List<String> funde = new ArrayList<>();
		String[] zeilen = inhalt.split("\n", -1);
		for (int i = 0; i < zeilen.length; i++) {
			String zeile = zeilen[i];
			for (Pattern muster : VERBOTENE_MUSTER) {
				if (muster.matcher(zeile).find()) {
					funde.add(datei + ":" + (i + 1) + ": " + zeile.strip());
				}
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
