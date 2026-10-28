/*
 * Erstellung 15.02.2020 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.schweizer;
import de.petanqueturniermanager.algorithmen.liga.Direktvergleich;
import de.petanqueturniermanager.algorithmen.common.IsEvenOrOdd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
	 *   <li>Ist eine Gruppe ungerade, wird das rangniedrigste Team in die nächsttiefere
	 *       Gruppe „gefloatet" (Carry-Over).</li>
	 * </ol>
	 * <p>
	 * Gibt es keine Ergebnis-Information ({@code ergebnisse} leer), landen alle Teams in einer
	 * Gruppe – identisches Verhalten wie die klassische globale Paarung.
	 *
	 * @param teams      die Teams in aktueller Ranglisten-Reihenfolge (Siege absteigend,
	 *                   Tie-Breaks via {@code sortiereNachAuswertungskriterien()})
	 * @param ergebnisse bisherige Spielergebnisse (nur Siegzahl wird für Gruppen verwendet);
	 *                   leer = globale Paarung ohne Gruppenbildung
	 * @return eine Liste der Paarungen für die nächste Runde (Freilos am Ende)
	 */
	public List<TeamPaarung> weitereRunde(List<Team> teams, List<SchweizerTeamErgebnis> ergebnisse) {
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

		List<Team> aktivTeams = teams.stream().filter(t -> !t.isHatGegner()).toList();
		List<List<Team>> gruppen = baueSiegeGruppen(aktivTeams, siegeProTeam);
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
	 * Gruppiert die sortierten Teams nach Siegzahl.
	 * Ungerade Gruppen werden durch Float des letzten Teams in die nächste Gruppe ausgeglichen.
	 * <p>
	 * Gibt es keine Sieginformation (leere siegeProTeam-Map), landen alle Teams in einer Gruppe
	 * und der Algorithmus verhält sich wie die klassische globale Paarung.
	 *
	 * @param sortierteTeams Teams in Ranglistenreihenfolge (Siegzahl absteigend)
	 * @param siegeProTeam   Map teamNr → Siegzahl
	 * @return Liste von Sieggruppen; jede Gruppe hat gerade Größe (nach Float-Ausgleich)
	 */
	@VisibleForTesting
	List<List<Team>> baueSiegeGruppen(List<Team> sortierteTeams, Map<Integer, Integer> siegeProTeam) {
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

		// Ungerade Gruppen: schwächstes Team in nächsttiefere Gruppe floaten (Carry-Over)
		for (int i = 0; i < gruppen.size() - 1; i++) {
			List<Team> gruppe = gruppen.get(i);
			if (gruppe.size() % 2 != 0) {
				Team floatTeam = gruppe.removeLast();
				gruppen.get(i + 1).addFirst(floatTeam); // Float-Team wird zuerst gepaart
			}
		}
		gruppen.removeIf(List::isEmpty);
		return gruppen;
	}

	/**
	 * Paart die Teams von oben nach unten: Jedes Team bekommt den ersten Gegner in Ranglistenreihenfolge – zuerst aus
	 * der eigenen Sieggruppe, sonst aus den tieferen (Float) –, mit dem die kleinstmögliche Zahl an
	 * Gegnerwiederholungen erreichbar bleibt (Kapitel 4 in {@code 03_Schweizer.md}).
	 * <p>
	 * Ob ein Kandidat zur besten Lösung führt, prüft ein maximales Matching über die noch nicht gespielten Paare
	 * (Edmonds-Blüten) exakt. Das Ergebnis entspricht einer vollständigen Rücksetz-Suche, braucht aber keine
	 * Suchgrenze: Ein Backtracking mit Schrittbudget wertete ein erschöpftes Budget als „unlösbar“ und ließ dann
	 * vermeidbare Wiederholungen zu (32 Teams, Runde 16: acht statt keiner).
	 *
	 * @param gruppen         Sieggruppen in Ranglistenreihenfolge (jede gerade Größe)
	 * @param teamPaarungList Ergebnisliste (wird befüllt)
	 */
	private void paareGruppen(List<List<Team>> gruppen, List<TeamPaarung> teamPaarungList) {
		List<Team> reihenfolge = gruppen.stream().flatMap(List::stream).toList();
		int n = reihenfolge.size();
		boolean[][] erlaubt = new boolean[n][n];
		for (int a = 0; a < n; a++) {
			for (int b = 0; b < n; b++) {
				Team teamA = reihenfolge.get(a);
				Team teamB = reihenfolge.get(b);
				erlaubt[a][b] = a != b && !teamA.hatAlsGegner(teamB) && !teamB.hatAlsGegner(teamA);
			}
		}
		boolean[] aktiv = new boolean[n];
		Arrays.fill(aktiv, true);
		int[] partner = new int[n];
		Arrays.fill(partner, -1);
		int groesse = 0;
		// Gieriger Start in Ranglistenreihenfolge: meist ist der gesuchte Gegner dann schon Matching-Partner und die
		// Prüfung kommt ohne Augmentierung aus.
		for (int a = 0; a < n; a++) {
			for (int b = a + 1; b < n && partner[a] < 0; b++) {
				if (partner[b] < 0 && erlaubt[a][b]) {
					partner[a] = b;
					partner[b] = a;
					groesse++;
				}
			}
		}
		for (int i = 0; i < n; i++) {
			if (partner[i] < 0 && augmentiere(i, erlaubt, partner, aktiv)) {
				groesse++;
			}
		}

		for (int team = 0; team < n; team++) {
			if (!aktiv[team]) {
				continue;
			}
			aktiv[team] = false;
			for (int gegner = team + 1; gegner < n; gegner++) {
				if (!aktiv[gegner]) {
					continue;
				}
				// Maximales Matching ohne team und gegner; mehr als groesse minus (team, gegner) ist nicht möglich.
				int[] neu = partner.clone();
				int neueGroesse = groesse;
				int ziel = groesse - (erlaubt[team][gegner] ? 1 : 0);
				aktiv[gegner] = false;
				for (int entfernt : new int[] { team, gegner }) {
					if (neu[entfernt] >= 0) {
						neu[neu[entfernt]] = -1;
						neu[entfernt] = -1;
						neueGroesse--;
					}
				}
				for (int frei = 0; frei < n && neueGroesse < ziel; frei++) {
					if (aktiv[frei] && neu[frei] < 0 && augmentiere(frei, erlaubt, neu, aktiv)) {
						neueGroesse++;
					}
				}
				if (neueGroesse == ziel) {
					partner = neu;
					groesse = neueGroesse;
					teamPaarungList.add(new TeamPaarung(reihenfolge.get(team), reihenfolge.get(gegner)).addGegner()
							.setHatGegner());
					break;
				}
				aktiv[gegner] = true;
			}
		}
	}

	/**
	 * Sucht vom freien Knoten {@code wurzel} einen augmentierenden Pfad (Edmonds-Blüten-Algorithmus) und erweitert
	 * das Matching entlang dieses Pfads.
	 *
	 * @param wurzel  freier, aktiver Knoten
	 * @param erlaubt Kanten (noch nicht gespielte Paare)
	 * @param partner Matching, {@code -1} = frei; wird bei Erfolg geändert
	 * @param aktiv   noch nicht gepaarte Knoten
	 * @return {@code true} wenn das Matching um eine Kante wuchs
	 */
	private static boolean augmentiere(int wurzel, boolean[][] erlaubt, int[] partner, boolean[] aktiv) {
		int n = partner.length;
		boolean[] besucht = new boolean[n];
		int[] vorgaenger = new int[n];
		Arrays.fill(vorgaenger, -1);
		int[] basis = IntStream.range(0, n).toArray();
		int[] warteschlange = new int[n];
		int kopf = 0;
		int ende = 0;
		besucht[wurzel] = true;
		warteschlange[ende++] = wurzel;
		while (kopf < ende) {
			int v = warteschlange[kopf++];
			for (int zu = 0; zu < n; zu++) {
				if (!aktiv[zu] || !erlaubt[v][zu] || basis[v] == basis[zu] || partner[v] == zu) {
					continue;
				}
				if (zu == wurzel || (partner[zu] >= 0 && vorgaenger[partner[zu]] >= 0)) {
					int bluetenBasis = gemeinsameBasis(v, zu, basis, partner, vorgaenger);
					boolean[] bluete = new boolean[n];
					markierePfad(v, bluetenBasis, zu, bluete, basis, partner, vorgaenger);
					markierePfad(zu, bluetenBasis, v, bluete, basis, partner, vorgaenger);
					for (int i = 0; i < n; i++) {
						if (aktiv[i] && bluete[basis[i]]) {
							basis[i] = bluetenBasis;
							if (!besucht[i]) {
								besucht[i] = true;
								warteschlange[ende++] = i;
							}
						}
					}
				} else if (vorgaenger[zu] < 0) {
					vorgaenger[zu] = v;
					if (partner[zu] < 0) {
						for (int u = zu; u >= 0;) {
							int pv = vorgaenger[u];
							int naechster = partner[pv];
							partner[u] = pv;
							partner[pv] = u;
							u = naechster;
						}
						return true;
					}
					besucht[partner[zu]] = true;
					warteschlange[ende++] = partner[zu];
				}
			}
		}
		return false;
	}

	private static int gemeinsameBasis(int a, int b, int[] basis, int[] partner, int[] vorgaenger) {
		boolean[] gesehen = new boolean[partner.length];
		while (true) {
			a = basis[a];
			gesehen[a] = true;
			if (partner[a] < 0) {
				break;
			}
			a = vorgaenger[partner[a]];
		}
		while (true) {
			b = basis[b];
			if (gesehen[b]) {
				return b;
			}
			b = vorgaenger[partner[b]];
		}
	}

	private static void markierePfad(int v, int bluetenBasis, int kind, boolean[] bluete, int[] basis, int[] partner,
			int[] vorgaenger) {
		while (basis[v] != bluetenBasis) {
			bluete[basis[v]] = true;
			bluete[basis[partner[v]]] = true;
			vorgaenger[v] = kind;
			kind = partner[v];
			v = vorgaenger[partner[v]];
		}
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
