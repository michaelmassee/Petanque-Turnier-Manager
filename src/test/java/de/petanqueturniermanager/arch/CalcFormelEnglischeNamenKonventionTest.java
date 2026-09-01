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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Verhindert, dass in per {@code SheetHelper.setFormulaInCell()} geschriebenen Calc-Formeln
 * versehentlich deutsche Funktionsnamen landen. Die ODF-Formelsprache ist immer Englisch,
 * unabhängig vom LO-UI-Locale (siehe CLAUDE.md, Abschnitt „Calc-Formeln … immer englische
 * ODF-Funktionsnamen") – ein deutscher Name führt in jeder Locale zu {@code #NAME?}.
 * <p>
 * Bis 2026-09 gab es dafür eine Übersetzungs-Krücke ({@code FORMULA_GERMAN_SEARCH_LIST} in
 * {@code SheetHelper}), die aber unvollständig war (fehlender Eintrag für {@code UND} führte zu
 * einem {@code #NAME?}-Fehler im KO-Turnierbaum, siehe {@code KoTurnierbaumSheet.schreibeSieger()}).
 * Die Krücke wurde entfernt; Formel-Strings im Produktivcode müssen jetzt direkt ODF-englische
 * Namen verwenden. Dieser Test ersetzt die Krücke durch einen Quelltext-Scan (analog
 * {@link DokumentFokusKonventionTest}): jedes Java-String-Literal in {@code src/main/java} wird
 * auf bekannte deutsche Calc-Funktionsnamen unmittelbar vor einer öffnenden Klammer geprüft.
 * <p>
 * Ausgenommen sind unsere eigenen {@code PTM.ALG.*}-Add-in-Funktionsnamen (z.B.
 * {@code PTM.ALG.DIREKTVERGLEICH}) – deren DisplayNames sind laut {@code GlobalAddIn.xcu} pro
 * Locale registriert und werden von LO für Add-ins locale-unabhängig aufgelöst (anders als
 * ODF-Kernfunktionen), siehe Kommentar in {@code GlobalImpl.java}.
 */
class CalcFormelEnglischeNamenKonventionTest {

	/** Deutsche Calc-Funktionsnamen, deren englisches ODF-Pendant abweicht. */
	private static final Set<String> DEUTSCHE_CALC_FUNKTIONSNAMEN = Set.of(
			"WENN", "UND", "ODER", "NICHT", "ISTZAHL", "ISTTEXT", "ISTLEER", "ISTFEHLER", "ISTNV",
			"WENNNV", "WENNFEHLER", "RUNDEN", "AUFRUNDEN", "ABRUNDEN", "SUMME", "MITTELWERT",
			"ANZAHL", "ANZAHL2", "ZÄHLENWENN", "ZÄHLENWENNS", "SUMMEWENN", "SUMMEWENNS",
			"SVERWEIS", "WVERWEIS", "VERWEIS", "VERGLEICH", "INDIREKT", "ADRESSE", "ZEILE",
			"ZEILEN", "SPALTE", "SPALTEN", "ISTGERADE", "ISTUNGERADE", "HEUTE", "JETZT", "JAHR",
			"MONAT", "TAG", "ISOKALENDERWOCHE", "WOCHENTAG", "ZEIT", "STUNDE", "SEKUNDE",
			"LÄNGE", "TEIL", "LINKS", "RECHTS", "GLÄTTEN", "GROSS", "KLEIN", "VERKETTEN", "WERT",
			"ZUFALLSZAHL", "RANG", "GANZZAHL", "REST", "WAHR", "FALSCH", "POTENZ", "WURZEL");

	/** Funktionsname unmittelbar vor einer öffnenden Klammer, z.B. {@code WENN(}. */
	private static final Pattern FUNKTIONS_AUFRUF = Pattern.compile("([A-ZÄÖÜ]{2,})\\(");

	@Test
	void keineDeutschenCalcFunktionsnamenInFormelStrings() throws IOException {
		Path quellWurzel = Paths.get("src/main/java");
		List<String> verstoesse = new ArrayList<>();

		try (Stream<Path> dateien = Files.walk(quellWurzel)) {
			dateien.filter(p -> p.toString().endsWith(".java")).forEach(datei -> {
				String inhalt = liesDatei(datei);
				for (String fund : findeDeutscheFunktionsnamen(inhalt)) {
					verstoesse.add(quellWurzel.relativize(datei).toString().replace('\\', '/') + ": " + fund);
				}
			});
		}

		assertThat(verstoesse)
				.as("Deutsche Calc-Funktionsnamen in Formel-Strings gefunden. Die ODF-Formelsprache "
						+ "ist immer Englisch, unabhängig vom LO-UI-Locale – ein deutscher Name führt in "
						+ "jeder Locale zu einem '#NAME?'-Fehler (siehe CLAUDE.md, Abschnitt 'Calc-Formeln "
						+ "in SheetHelper.setFormulaInCell()'). Bitte direkt den englischen ODF-Namen "
						+ "verwenden.")
				.isEmpty();
	}

	private static List<String> findeDeutscheFunktionsnamen(String inhalt) {
		List<String> funde = new ArrayList<>();
		for (String zeile : inhalt.split("\n", -1)) {
			for (String literal : extrahiereStringLiterale(zeile)) {
				Matcher aufrufMatcher = FUNKTIONS_AUFRUF.matcher(literal);
				while (aufrufMatcher.find()) {
					String name = aufrufMatcher.group(1);
					if (DEUTSCHE_CALC_FUNKTIONSNAMEN.contains(name)) {
						funde.add(name + "(");
					}
				}
			}
		}
		return funde;
	}

	/** Extrahiert den Inhalt aller {@code "..."}-Literale einer einzelnen Zeile (manueller Scan statt Regex, um Backtracking auf langen Zeilen zu vermeiden). */
	private static List<String> extrahiereStringLiterale(String zeile) {
		List<String> literale = new ArrayList<>();
		StringBuilder aktuelles = null;
		boolean escaped = false;
		for (int i = 0; i < zeile.length(); i++) {
			char c = zeile.charAt(i);
			if (aktuelles == null) {
				if (c == '"') {
					aktuelles = new StringBuilder();
				}
				continue;
			}
			if (escaped) {
				escaped = false;
				continue;
			}
			if (c == '\\') {
				escaped = true;
				continue;
			}
			if (c == '"') {
				literale.add(aktuelles.toString());
				aktuelles = null;
				continue;
			}
			aktuelles.append(c);
		}
		return literale;
	}

	private static String liesDatei(Path datei) {
		try {
			return Files.readString(datei);
		} catch (IOException e) {
			throw new IllegalStateException("Fehler beim Lesen von " + datei, e);
		}
	}
}
