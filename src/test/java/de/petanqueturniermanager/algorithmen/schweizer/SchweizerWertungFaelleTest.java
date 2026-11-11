/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.schweizer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.petanqueturniermanager.schweizer.konfiguration.SchweizerFreilosBuchholzModus;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerRankingModus;

/**
 * Gemeinsame Testfälle mit PTM-Online ({@code src/lib/pairing/schweizer-wertung-faelle.json}, identische Kopie):
 * Siege, BHZ/FBHZ inkl. Freilos-Buchholz, Sortierung, geteilte Plätze und die Reihenfolge innerhalb eines geteilten Platzes (Teamnummer, keine Wertung). Beide
 * Projekte müssen für dieselben Runden dieselbe Rangliste liefern.
 * <p>
 * Die Runden werden wie in {@code SchweizerAbstractSpielrundeSheet.gespieltenRundenEinlesen} ausgewertet: Ein Freilos
 * zählt immer als Sieg mit 13:7, für den Buchholz aber nur in Runden mit mindestens einem Ergebnis; eine ausgeloste
 * Partie ohne Ergebnis (0:0) zählt nicht als Begegnung. Der Platz folgt der Platz-Formel der Rangliste
 * ({@code RangListeSpalte}): gleicher Platz, wenn alle Ranglistenspalten mit der Zeile darüber übereinstimmen.
 */
class SchweizerWertungFaelleTest {

	private static final String DATEI = "/de/petanqueturniermanager/schweizer/schweizer-wertung-faelle.json";
	private static final int FREILOS_PLUS = 13;
	private static final int FREILOS_MINUS = 7;

	static Stream<Arguments> faelle() throws Exception {
		try (var reader = new InputStreamReader(SchweizerWertungFaelleTest.class.getResourceAsStream(DATEI),
				StandardCharsets.UTF_8)) {
			JsonArray faelle = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("faelle");
			List<Arguments> args = new ArrayList<>();
			for (JsonElement fall : faelle) {
				args.add(Arguments.of(fall.getAsJsonObject().get("name").getAsString(), fall.getAsJsonObject()));
			}
			return args.stream();
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("faelle")
	void liefertDieselbeRanglisteWiePtmOnline(String name, JsonObject fall) {
		var modus = SchweizerRankingModus.valueOf(fall.get("rankingModus").getAsString());
		var freilosModus = SchweizerFreilosBuchholzModus.valueOf(fall.get("freilosBuchholz").getAsString());

		// teamNr → [siege, plus, minus], Gegner, gewertete Freilose
		Map<Integer, int[]> stats = new LinkedHashMap<>();
		Map<Integer, List<Integer>> gegner = new LinkedHashMap<>();
		Map<Integer, Integer> freilose = new LinkedHashMap<>();
		for (JsonElement nr : fall.getAsJsonArray("teams")) {
			stats.put(nr.getAsInt(), new int[3]);
			gegner.put(nr.getAsInt(), new ArrayList<>());
			freilose.put(nr.getAsInt(), 0);
		}
		int gewerteteRunden = 0;
		for (JsonElement runde : fall.getAsJsonArray("runden")) {
			boolean hatErgebnis = false;
			List<Integer> freilosTeams = new ArrayList<>();
			for (JsonElement partieElement : runde.getAsJsonArray()) {
				JsonArray partie = partieElement.getAsJsonArray();
				int a = partie.get(0).getAsInt();
				if (partie.get(1).isJsonNull()) {
					stats.get(a)[0]++;
					stats.get(a)[1] += FREILOS_PLUS;
					stats.get(a)[2] += FREILOS_MINUS;
					freilosTeams.add(a);
					continue;
				}
				int b = partie.get(1).getAsInt();
				int pa = partie.get(2).isJsonNull() ? 0 : partie.get(2).getAsInt();
				int pb = partie.get(3).isJsonNull() ? 0 : partie.get(3).getAsInt();
				if (pa == 0 && pb == 0) {
					continue; // ausgelost, noch ohne Ergebnis
				}
				hatErgebnis = true;
				stats.get(a)[1] += pa;
				stats.get(a)[2] += pb;
				stats.get(b)[1] += pb;
				stats.get(b)[2] += pa;
				gegner.get(a).add(b);
				gegner.get(b).add(a);
				stats.get(pa > pb ? a : b)[0]++;
			}
			if (hatErgebnis) {
				gewerteteRunden++;
				freilosTeams.forEach(nr -> freilose.merge(nr, 1, Integer::sum));
			}
		}

		List<SchweizerTeamErgebnis> ergebnisse = new ArrayList<>();
		stats.forEach((nr, s) -> ergebnisse.add(new SchweizerTeamErgebnis(nr, s[0], s[1] - s[2], s[1], gegner.get(nr),
				List.of(), freilose.get(nr))));
		var freilosBuchholz = new FreilosBuchholz(freilosModus, gewerteteRunden);
		var system = new SchweizerSystem();
		List<SchweizerTeamErgebnis> sortiert = system.sortiereNachAuswertungskriterien(ergebnisse, modus,
				freilosBuchholz);
		Map<Integer, Double> bhz = system.berechneBuchholz(ergebnisse, freilosBuchholz);
		Map<Integer, Double> fbhz = system.berechneFeinbuchholz(ergebnisse, bhz, freilosBuchholz);

		List<List<Integer>> tatsaechlich = new ArrayList<>();
		int platz = 0;
		for (int i = 0; i < sortiert.size(); i++) {
			var e = sortiert.get(i);
			if (i == 0 || !gleicherPlatz(sortiert.get(i - 1), e, modus, bhz, fbhz)) {
				platz = i + 1;
			}
			tatsaechlich.add(List.of(platz, e.teamNr(), e.siege(), bhz.get(e.teamNr()).intValue(),
					fbhz.get(e.teamNr()).intValue(), e.punktedifferenz(), e.erzieltePunkte()));
		}

		List<List<Integer>> erwartet = new ArrayList<>();
		for (JsonElement zeile : fall.getAsJsonArray("erwartet")) {
			List<Integer> werte = new ArrayList<>();
			zeile.getAsJsonArray().forEach(wert -> werte.add(wert.getAsInt()));
			erwartet.add(werte);
		}
		assertThat(tatsaechlich).as(name).isEqualTo(erwartet);
		bhz.values().forEach(wert -> assertThat(wert % 1).as("BHZ ganzzahlig").isZero());
		fbhz.values().forEach(wert -> assertThat(wert % 1).as("FBHZ ganzzahlig").isZero());
	}

	/** Wie die Platz-Formel der Rangliste: alle Ranglistenspalten gleich ⇒ gleicher Platz. */
	private static boolean gleicherPlatz(SchweizerTeamErgebnis a, SchweizerTeamErgebnis b, SchweizerRankingModus modus,
			Map<Integer, Double> bhz, Map<Integer, Double> fbhz) {
		boolean buchholzGleich = modus == SchweizerRankingModus.OHNE_BUCHHOLZ
				|| (bhz.get(a.teamNr()).equals(bhz.get(b.teamNr())) && fbhz.get(a.teamNr()).equals(fbhz.get(b.teamNr())));
		return a.siege() == b.siege() && buchholzGleich && a.punktedifferenz() == b.punktedifferenz()
				&& a.erzieltePunkte() == b.erzieltePunkte();
	}
}
