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
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Verhindert, dass Produktivcode unbemerkt neue fokus-basierte Dokumentzugriffe
 * ({@code DocumentHelper.getCurrentSpreadsheetDocument(...)},
 * {@code ProtocolHandler.holeAktivesDokument()}) einführt. Beide liefern das UI-fokussierte
 * Dokument statt eines konkret gemeinten Dokuments; bei mehreren gleichzeitig offenen
 * Turnier-Dokumenten führte das zu Leaks zwischen Dokumenten (Fixes {@code 286754dc} „Spieltag-
 * Property leakt" und {@code e828766b} „Toolbar-Status/Sortier-Dropdown leakt").
 * <p>
 * Statt eines vollen ArchUnit-Freezes genügt hier ein einfacher Quelltext-Scan (analog
 * {@link SwingTimerKonventionTest}): die aktuell bekannten, akzeptierten Fundstellen sind fest
 * in {@link #ERWARTETE_TREFFER} hinterlegt. Eine neue Datei oder eine höhere Trefferzahl in
 * einer bestehenden Datei lässt den Test fehlschlagen — jeder neue fokus-basierte Zugriff muss
 * dadurch bewusst gegen {@code DokumentKontext} bzw. eine frame-gebundene Auflösung
 * (siehe {@code ProtocolHandler.ermittleDokumentAusFrame()}) geprüft werden, siehe CLAUDE.md,
 * Abschnitt „Mehrere offene Turnier-Dokumente". Bei bewusster, gerechtfertigter Neuanlage wird
 * die Allowlist hier im selben Commit erweitert.
 */
class DokumentFokusKonventionTest {

	private static final String MARKER_GET_CURRENT = "DocumentHelper.getCurrentSpreadsheetDocument(";
	private static final String MARKER_HOLE_AKTIV = "holeAktivesDokument(";

	/** relativer Pfad ab {@code src/main/java} → erwartete Trefferzahl des jeweiligen Markers. */
	private static final Map<String, Integer> ERWARTETE_TREFFER_GET_CURRENT = Map.of(
			"de/petanqueturniermanager/addins/GlobalImpl.java", 1,
			"de/petanqueturniermanager/comp/CompositeViewsOptionsEventHandler.java", 1,
			"de/petanqueturniermanager/comp/WorkingSpreadsheet.java", 1,
			"de/petanqueturniermanager/comp/PtmPublicService.java", 1,
			"de/petanqueturniermanager/comp/ProtocolHandler.java", 1,
			"de/petanqueturniermanager/comp/DokumentKontext.java", 1,
			"de/petanqueturniermanager/webserver/CompositeViewDetailDialog.java", 1);

	private static final Map<String, Integer> ERWARTETE_TREFFER_HOLE_AKTIV = Map.of(
			"de/petanqueturniermanager/comp/ProtocolHandler.java", 4);

	@Test
	void keineNeuenFokusBasiertenDokumentZugriffe() throws IOException {
		Path quellWurzel = Paths.get("src/main/java");
		List<String> verstoesse = new ArrayList<>();

		verstoesse.addAll(pruefeMarker(quellWurzel, MARKER_GET_CURRENT, ERWARTETE_TREFFER_GET_CURRENT));
		verstoesse.addAll(pruefeMarker(quellWurzel, MARKER_HOLE_AKTIV, ERWARTETE_TREFFER_HOLE_AKTIV));

		assertThat(verstoesse)
				.as("Neue/zusätzliche fokus-basierte Dokumentzugriffe gefunden. Bei mehreren offenen "
						+ "Turnier-Dokumenten liefert DocumentHelper.getCurrentSpreadsheetDocument(...)/"
						+ "ProtocolHandler.holeAktivesDokument() das UI-fokussierte statt des gemeinten "
						+ "Dokuments (siehe CLAUDE.md, Abschnitt 'Mehrere offene Turnier-Dokumente'). "
						+ "Bitte DokumentKontext bzw. eine frame-gebundene Auflösung verwenden; falls der "
						+ "fokus-basierte Zugriff hier wirklich gerechtfertigt ist, die Allowlist in "
						+ "DokumentFokusKonventionTest bewusst erweitern.")
				.isEmpty();
	}

	private static List<String> pruefeMarker(Path quellWurzel, String marker, Map<String, Integer> erwartet)
			throws IOException {
		Map<String, Integer> gefunden = zaehleTreffer(quellWurzel, marker);
		List<String> verstoesse = new ArrayList<>();
		for (Map.Entry<String, Integer> eintrag : gefunden.entrySet()) {
			int erlaubt = erwartet.getOrDefault(eintrag.getKey(), 0);
			if (eintrag.getValue() > erlaubt) {
				verstoesse.add(eintrag.getKey() + ": " + eintrag.getValue() + "x '" + marker
						+ "' gefunden, erlaubt sind " + erlaubt);
			}
		}
		return verstoesse;
	}

	private static Map<String, Integer> zaehleTreffer(Path quellWurzel, String marker) throws IOException {
		Map<String, Integer> treffer = new TreeMap<>();
		try (Stream<Path> dateien = Files.walk(quellWurzel)) {
			dateien.filter(p -> p.toString().endsWith(".java")).forEach(datei -> {
				String inhalt = liesDatei(datei);
				int anzahl = zaehleVorkommen(inhalt, marker);
				if (anzahl > 0) {
					treffer.put(quellWurzel.relativize(datei).toString().replace('\\', '/'), anzahl);
				}
			});
		}
		return treffer;
	}

	private static int zaehleVorkommen(String inhalt, String marker) {
		int anzahl = 0;
		int idx = 0;
		while ((idx = inhalt.indexOf(marker, idx)) != -1) {
			anzahl++;
			idx += marker.length();
		}
		return anzahl;
	}

	private static String liesDatei(Path datei) {
		try {
			return Files.readString(datei);
		} catch (IOException e) {
			throw new IllegalStateException("Fehler beim Lesen von " + datei, e);
		}
	}
}
