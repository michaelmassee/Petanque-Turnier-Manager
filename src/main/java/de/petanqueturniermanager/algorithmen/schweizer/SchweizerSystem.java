/*
 * Erstellung 15.02.2020 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.schweizer;
import de.petanqueturniermanager.algorithmen.liga.Direktvergleich;
import de.petanqueturniermanager.algorithmen.common.IsEvenOrOdd;
import de.petanqueturniermanager.algorithmen.common.WiederholungsarmePaarung;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.annotations.VisibleForTesting;

import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamPaarung;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerRankingModus;

/**
 * Implementiert die Logik für ein Turnier nach Schweizer System.
 * <p>
 * Das Schweizer System ist ein Turniermodus, der es ermöglicht, mit einer
 * großen Anzahl von Teilnehmern in relativ wenigen Runden einen Sieger zu
 * ermitteln. Alle Teams spielen in jeder Runde mit.
 * <p>
 * Ab der zweiten Runde spielt man idealerweise immer gegen Gegner, die in etwa
 * gleich erfolgreich sind (z. B. spielen Sieger gegen Sieger).
 *
 * @see <a href="SchweizerTurnierSystem.md">SchweizerTurnierSystem.md</a>
 */
public class SchweizerSystem {

	/**
	 * Sortiert eine Liste von Teamergebnissen nach den Auswertungskriterien des Schweizer Systems:
	 * <ol>
	 *   <li>Anzahl der Siege (absteigend)</li>
	 *   <li>Buchholz / BHZ = Summe der Siege aller Gegner (absteigend)</li>
	 *   <li>Feinbuchholz / FBHZ = Summe der BHZ-Werte aller Gegner (absteigend)</li>
	 *   <li>Punktedifferenz = erzielte minus kassierte Punkte (absteigend)</li>
	 *   <li>Punkte+ = erzielte Punkte (absteigend)</li>
	 * </ol>
	 * Hinweis: Direktvergleich (Kriterium 6) und Los (Kriterium 7) werden hier nicht berechnet
	 * und müssen bei Bedarf nachgelagert behandelt werden.
	 *
	 * @param ergebnisse die Teamergebnisse mit Siegen, Punktedifferenz und Gegnerliste
	 * @return neue Liste, sortiert nach Auswertungskriterien (beste Team zuerst)
	 */
	public List<SchweizerTeamErgebnis> sortiereNachAuswertungskriterien(List<SchweizerTeamErgebnis> ergebnisse) {
		// BHZ (Buchholz) für jedes Team berechnen: Summe der Siege aller Gegner
		Map<Integer, Integer> bhzMap = berechneBuchholz(ergebnisse);

		// FBHZ (Feinbuchholz) berechnen: Summe der BHZ-Werte aller Gegner
		Map<Integer, Integer> fbhzMap = berechneFeinbuchholz(ergebnisse, bhzMap);

		Comparator<SchweizerTeamErgebnis> comparator = Comparator
				.comparingInt(SchweizerTeamErgebnis::siege).reversed()
				.thenComparing(Comparator.comparingInt(
						(SchweizerTeamErgebnis e) -> bhzMap.getOrDefault(e.teamNr(), 0)).reversed())
				.thenComparing(Comparator.comparingInt(
						(SchweizerTeamErgebnis e) -> fbhzMap.getOrDefault(e.teamNr(), 0)).reversed())
				.thenComparing(Comparator.comparingInt(SchweizerTeamErgebnis::punktedifferenz).reversed())
				.thenComparing(Comparator.comparingInt(SchweizerTeamErgebnis::erzieltePunkte).reversed());

		return ergebnisse.stream().sorted(comparator).collect(Collectors.toList());
	}

	/**
	 * Sortiert nach Auswertungskriterien mit wählbarem Modus.
	 * <p>
	 * Bei {@code OHNE_BUCHHOLZ}: Siege → Punktedifferenz → Punkte+<br>
	 * Bei {@code MIT_BUCHHOLZ}: delegiert an {@link #sortiereNachAuswertungskriterien(List)}
	 * (Siege → BHZ → FBHZ → Punktedifferenz → Punkte+).
	 *
	 * @param ergebnisse die Teamergebnisse
	 * @param modus      gewünschter Ranking-Modus
	 * @return sortierte Liste
	 */
	public List<SchweizerTeamErgebnis> sortiereNachAuswertungskriterien(List<SchweizerTeamErgebnis> ergebnisse,
			SchweizerRankingModus modus) {
		if (modus == SchweizerRankingModus.OHNE_BUCHHOLZ) {
			return ergebnisse.stream()
					.sorted(Comparator.comparingInt(SchweizerTeamErgebnis::siege).reversed()
							.thenComparing(Comparator.comparingInt(SchweizerTeamErgebnis::punktedifferenz).reversed())
							.thenComparing(Comparator.comparingInt(SchweizerTeamErgebnis::erzieltePunkte).reversed()))
					.collect(Collectors.toList());
		}
		return sortiereNachAuswertungskriterien(ergebnisse);
	}

	/**
	 * Berechnet den Buchholz-Wert für jedes Team.
	 * BHZ = Summe der Siege aller Gegner am Turnierend.
	 *
	 * @param ergebnisse alle Teamergebnisse
	 * @return Map von teamNr → BHZ-Wert
	 */
	@VisibleForTesting
	public Map<Integer, Integer> berechneBuchholz(List<SchweizerTeamErgebnis> ergebnisse) {
		Map<Integer, Integer> siegeMap = ergebnisse.stream()
				.collect(Collectors.toMap(SchweizerTeamErgebnis::teamNr, SchweizerTeamErgebnis::siege));

		return ergebnisse.stream().collect(Collectors.toMap(
				SchweizerTeamErgebnis::teamNr,
				e -> e.gegnerNrn().stream()
						.mapToInt(gegnerNr -> siegeMap.getOrDefault(gegnerNr, 0))
						.sum()));
	}

	/**
	 * Berechnet den Feinbuchholz-Wert für jedes Team.
	 * FBHZ = Summe der BHZ-Werte aller Gegner.
	 *
	 * @param ergebnisse alle Teamergebnisse
	 * @param bhzMap     vorberechnete BHZ-Werte
	 * @return Map von teamNr → FBHZ-Wert
	 */
	@VisibleForTesting
	public Map<Integer, Integer> berechneFeinbuchholz(List<SchweizerTeamErgebnis> ergebnisse,
			Map<Integer, Integer> bhzMap) {
		return ergebnisse.stream().collect(Collectors.toMap(
				SchweizerTeamErgebnis::teamNr,
				e -> e.gegnerNrn().stream()
						.mapToInt(gegnerNr -> bhzMap.getOrDefault(gegnerNr, 0))
						.sum()));
	}

	/**
	 * Die erste Runde wird frei ausgelost, wobei ein Setzen der vermeintlich stärksten Teams sinnvoll ist
	 * (mit dem Ziel, diese nicht in der ersten Runde aufeinandertreffen zu lassen, und, bei ungerader Teilnehmerzahl,
	 * diese nicht mit einem Freilos beginnen zu lassen).
	 * <p>
	 * Das Freilos bekommt ein zufälliges ungesetztes Team (SetzPos 0), nur ohne ungesetzte Teams eines aus der größten
	 * Setzgruppe. Die übrigen Teams stehen in einer Reihe – Ungesetzte, dann jede Setzgruppe als zusammenhängender
	 * Block in zufälliger Reihenfolge – und Platz i spielt gegen Platz i + n/2. Ein Block mit höchstens n/2 Teams
	 * enthält so kein Paar; nur eine größere Setzgruppe muss gegeneinander spielen, und dann so selten wie möglich.
	 *
	 * @param teams die Liste der teilnehmenden Teams; Teams mit gleicher SetzPos &gt; 0 werden nicht gegeneinander
	 *              ausgelost, solange es vermeidbar ist
	 * @return eine Liste der Paarungen für die erste Runde (Freilos am Ende)
	 */
	public List<TeamPaarung> ersteRunde(List<Team> teams) {
		List<Team> gemischt = new ArrayList<>(teams);
		Collections.shuffle(gemischt, RandomSource.asJavaRandom());

		List<Team> ungesetzt = gemischt.stream().filter(team -> team.getSetzPos() <= 0)
				.collect(Collectors.toCollection(ArrayList::new));
		// Reihenfolge der Setzgruppen = erstes Auftreten in der gemischten Liste, also zufällig
		List<List<Team>> setzGruppen = new ArrayList<>(gemischt.stream().filter(team -> team.getSetzPos() > 0)
				.collect(Collectors.groupingBy(Team::getSetzPos, LinkedHashMap::new,
						Collectors.toCollection(ArrayList::new)))
				.values());

		Team freilosTeam = null;
		if (IsEvenOrOdd.isOdd(gemischt.size())) {
			freilosTeam = !ungesetzt.isEmpty() ? ungesetzt.removeLast()
					: setzGruppen.stream().max(Comparator.comparingInt(List::size)).orElseThrow().removeLast();
		}

		List<Team> reihe = new ArrayList<>(ungesetzt);
		setzGruppen.forEach(reihe::addAll);
		int anzTeamPaarungen = reihe.size() / 2;
		List<TeamPaarung> paarungen = new ArrayList<>(IntStream.range(0, anzTeamPaarungen)
				.mapToObj(i -> new TeamPaarung(reihe.get(i), reihe.get(i + anzTeamPaarungen)).addGegner())
				.sorted(Comparator.comparingInt(paarung -> paarung.getA().getNr())).toList());
		if (freilosTeam != null) {
			freilosTeam.setHatteFreilos(true);
			paarungen.add(new TeamPaarung(freilosTeam).addGegner());
		}
		return paarungen;
	}

	/**
	 * Ermittelt die Paarungen für die nächste Runde nach den Regeln des Schweizer Systems.
	 * <p>
	 * <b>Paarungs-Regel: nur Siege entscheiden über die Gruppenzugehörigkeit.</b>
	 * Buchholz (BHZ) und Feinbuchholz (FBHZ) sind <em>ausschließlich</em> Ranglisten-Kriterien
	 * (Tie-Breaks) und beeinflussen das Pairing <em>nicht direkt</em>.
	 * <p>
	 * <b>Ablauf:</b>
	 * <ol>
	 *   <li>Teams werden anhand ihrer Siegzahl in <b>Sieggruppen</b> eingeteilt
	 *       (z.B. „3 Siege", „2 Siege", …).</li>
	 *   <li>Innerhalb jeder Gruppe werden Teams nach ihrer Ranglistenposition sortiert,
	 *       die durch {@code sortiereNachAuswertungskriterien()} (Siege→BHZ→FBHZ→Punktediff)
	 *       bestimmt wird. BHZ wirkt also <em>indirekt</em>: es legt fest, wer in der Gruppe
	 *       oben steht und damit gegen wen gepaart wird – aber nicht, in welche Gruppe ein Team fällt.</li>
	 *   <li>Ist eine Gruppe ungerade, wird ein Team in die nächsttiefere Gruppe heruntergelost
	 *       („gefloatet", Carry-Over) – bevorzugt eines, das bisher nicht heruntergelost wurde
	 *       (siehe {@link #baueSiegeGruppen}).</li>
	 * </ol>
	 * <p>
	 * Gibt es keine Ergebnis-Information ({@code ergebnisse} leer), landen alle Teams in einer
	 * Gruppe – identisches Verhalten wie die klassische globale Paarung.
	 *
	 * @param teams       die Teams in aktueller Ranglisten-Reihenfolge (Siege absteigend,
	 *                    Tie-Breaks via {@code sortiereNachAuswertungskriterien()})
	 * @param ergebnisse  bisherige Spielergebnisse (Siegzahl für die Gruppen, Herunterlos-Historie für die
	 *                    Floater-Wahl); leer = globale Paarung ohne Gruppenbildung
	 * @param neueRundeNr Nummer der auszulosenden Runde
	 * @return eine Liste der Paarungen für die nächste Runde (Freilos am Ende)
	 */
	public List<TeamPaarung> weitereRunde(List<Team> teams, List<SchweizerTeamErgebnis> ergebnisse,
			int neueRundeNr) {
		boolean freiSpiel = IsEvenOrOdd.isOdd(teams.size());
		List<TeamPaarung> teamPaarungList = new ArrayList<>();

		// sicher gehen dass hatgegner flag auf false
		teams.forEach(team -> team.setHatGegner(false));

		// zuerst freilos vergeben: schlechtestes Team ohne Freilos, hatten alle schon eins, das schlechteste Team
		if (freiSpiel) {
			Team freilosTeam = IntStream.range(0, teams.size())
					.mapToObj(i -> teams.get(teams.size() - i - 1))
					.filter(team -> !team.isHatteFreilos()).findFirst().orElse(teams.get(teams.size() - 1));
			freilosTeam.setHatteFreilos(true);
			freilosTeam.setHatGegner(true);
			teamPaarungList.add(new TeamPaarung(freilosTeam));
		}

		// Sieggruppen bauen und innerhalb paaren
		Map<Integer, Integer> siegeProTeam = ergebnisse.stream()
				.collect(Collectors.toMap(SchweizerTeamErgebnis::teamNr, SchweizerTeamErgebnis::siege));
		Map<Integer, List<Integer>> herunterlosProTeam = ergebnisse.stream()
				.collect(Collectors.toMap(SchweizerTeamErgebnis::teamNr, SchweizerTeamErgebnis::heruntergelostInRunden));

		List<Team> aktivTeams = teams.stream().filter(t -> !t.isHatGegner()).toList();
		List<List<Team>> gruppen = baueSiegeGruppen(aktivTeams, siegeProTeam, herunterlosProTeam, neueRundeNr);
		paareGruppen(gruppen, teamPaarungList);

		return teamPaarungList.stream().sorted((tp1, tp2) -> {
			// Freilos an letzter Stelle
			if (tp1.isFreilos()) {
				return 1;
			} else if (tp2.isFreilos()) {
				return -1;
			}
			return 0;
		}).toList();
	}

	/**
	 * Gruppiert die sortierten Teams nach Siegzahl und gleicht ungerade Gruppen durch Herunterlosen aus.
	 * <p>
	 * Die Gruppen werden von oben nach unten verarbeitet. Ein heruntergeloster Floater steht vorn in der nächsten
	 * Gruppe und wird dort zuerst gepaart. Ist eine Gruppe (inklusive eingehendem Floater) ungerade, wird aus ihren
	 * eigenen Teams der Floater gewählt (siehe {@link #waehleFloater}) und an die erste Stelle der nächsten Gruppe
	 * verschoben; die übrigen Teams behalten ihre Reihenfolge.
	 * <p>
	 * Gibt es keine Sieginformation (leere siegeProTeam-Map), landen alle Teams in einer Gruppe
	 * und der Algorithmus verhält sich wie die klassische globale Paarung.
	 *
	 * @param sortierteTeams     Teams in Ranglistenreihenfolge (Siegzahl absteigend)
	 * @param siegeProTeam       Map teamNr → Siegzahl
	 * @param herunterlosProTeam Map teamNr → Runden, in denen das Team heruntergelost wurde (fehlend = nie)
	 * @param neueRundeNr        Nummer der auszulosenden Runde
	 * @return Liste von Sieggruppen; jede Gruppe hat gerade Größe (nach Float-Ausgleich)
	 */
	@VisibleForTesting
	List<List<Team>> baueSiegeGruppen(List<Team> sortierteTeams, Map<Integer, Integer> siegeProTeam,
			Map<Integer, List<Integer>> herunterlosProTeam, int neueRundeNr) {
		List<List<Team>> gruppen = gruppiereNachSiegen(sortierteTeams, siegeProTeam);

		boolean hatEingehendenFloater = false;
		for (int i = 0; i < gruppen.size() - 1; i++) {
			List<Team> gruppe = gruppen.get(i);
			boolean floatet = IsEvenOrOdd.isOdd(gruppe.size());
			if (floatet) {
				// Der eingehende Floater wurde in dieser Runde schon heruntergelost – nur wenn er allein ist, erneut
				List<Team> kandidaten = hatEingehendenFloater && gruppe.size() > 1 ? gruppe.subList(1, gruppe.size())
						: gruppe;
				Team floater = waehleFloater(kandidaten, herunterlosProTeam, neueRundeNr);
				gruppe.remove(floater);
				gruppen.get(i + 1).addFirst(floater); // Floater wird in der nächsten Gruppe zuerst gepaart
			}
			hatEingehendenFloater = floatet;
		}
		gruppen.removeIf(List::isEmpty);
		return gruppen;
	}

	/**
	 * Wählt das Team, das aus einer ungeraden Sieggruppe heruntergelost wird. Herunterlosen ist ein
	 * Optimierungskriterium, kein Verbot – es wird immer ein Team gewählt. Vorrang in dieser Reihenfolge:
	 * <ol>
	 *   <li>nicht in der letzten Runde heruntergelost</li>
	 *   <li>nicht in der vorletzten Runde heruntergelost</li>
	 *   <li>möglichst selten heruntergelost</li>
	 *   <li>rangniedrigstes Team</li>
	 * </ol>
	 *
	 * @param kandidaten         Teams der Gruppe in Ranglistenreihenfolge (ohne eingehenden Floater), nicht leer
	 * @param herunterlosProTeam Map teamNr → Runden, in denen das Team heruntergelost wurde
	 * @param neueRundeNr        Nummer der auszulosenden Runde
	 * @return das herunterzulosende Team
	 */
	private static Team waehleFloater(List<Team> kandidaten, Map<Integer, List<Integer>> herunterlosProTeam,
			int neueRundeNr) {
		Function<Team, List<Integer>> runden = team -> herunterlosProTeam.getOrDefault(team.getNr(), List.of());
		Comparator<Integer> vorrang = Comparator
				.comparing((Integer pos) -> runden.apply(kandidaten.get(pos)).contains(neueRundeNr - 1))
				.thenComparing(pos -> runden.apply(kandidaten.get(pos)).contains(neueRundeNr - 2))
				.thenComparingInt(pos -> runden.apply(kandidaten.get(pos)).size())
				.thenComparing(Comparator.reverseOrder());
		return IntStream.range(0, kandidaten.size()).boxed().min(vorrang).map(kandidaten::get).orElseThrow();
	}

	private static List<List<Team>> gruppiereNachSiegen(List<Team> sortierteTeams,
			Map<Integer, Integer> siegeProTeam) {
		List<List<Team>> gruppen = new ArrayList<>();
		List<Team> aktuelleGruppe = null;
		int letztesSiege = Integer.MIN_VALUE;

		for (Team team : sortierteTeams) {
			int siege = siegeProTeam.getOrDefault(team.getNr(), 0);
			if (aktuelleGruppe == null || siege != letztesSiege) {
				aktuelleGruppe = new ArrayList<>();
				gruppen.add(aktuelleGruppe);
				letztesSiege = siege;
			}
			aktuelleGruppe.add(team);
		}
		return gruppen;
	}

	/**
	 * Paart die Teams von oben nach unten: Jedes Team bekommt den ersten Gegner in Ranglistenreihenfolge – zuerst aus
	 * der eigenen Sieggruppe, sonst aus den tieferen (Float) –, mit dem die kleinstmögliche Zahl an
	 * Gegnerwiederholungen erreichbar bleibt (Kapitel 4 in {@code 03_Schweizer.md}, {@link WiederholungsarmePaarung}).
	 *
	 * @param gruppen         Sieggruppen in Ranglistenreihenfolge (jede gerade Größe)
	 * @param teamPaarungList Ergebnisliste (wird befüllt)
	 */
	private void paareGruppen(List<List<Team>> gruppen, List<TeamPaarung> teamPaarungList) {
		List<Team> reihenfolge = gruppen.stream().flatMap(List::stream).toList();
		WiederholungsarmePaarung.paare(reihenfolge.size(),
				(a, b) -> reihenfolge.get(a).hatAlsGegner(reihenfolge.get(b)),
				WiederholungsarmePaarung.GegnerWahl.NAECHSTER_IN_RANGLISTE)
				.forEach(paar -> teamPaarungList.add(new TeamPaarung(reihenfolge.get(paar.a()), reihenfolge.get(paar.b()))
						.addGegner().setHatGegner()));
	}

	/**
	 * Flacht eine Liste von Paarungen zu einer sortierten Teamliste ab.
	 * Freilos-Paarungen (kein Team B) werden dabei übersprungen.
	 *
	 * @param paarungen die Paarungsliste
	 * @return flache, sortierte Teamliste (ohne Freilos-Einträge)
	 */
	public List<Team> flattenTeampaarungen(List<TeamPaarung> paarungen) {
		return paarungen.stream()
				.flatMap(tp -> Stream.concat(Stream.of(tp.getA()), tp.getOptionalB().stream()))
				.sorted(Comparator.comparingInt(Team::getNr))
				.toList();
	}
}
