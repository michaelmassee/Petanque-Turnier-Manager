/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;


/**
 * Berechnet, welche Zellen der Nr-Spalte einer Meldeliste neu geschrieben werden müssen: Jede Zeile mit Namen
 * braucht eine Nummer und eine UUID, beide stehen als {@code PTM.ALG.TNRUUID(nr;"uuid")} in der Nr-Zelle. UNO-frei,
 * das Lesen und Schreiben übernimmt {@link TeamNummernVergabe}.
 * <ul>
 * <li>Zeile mit Nr und UUID: bleibt unverändert.</li>
 * <li>Zeile mit Nr ohne UUID (z.&nbsp;B. von Hand eingetragen): behält die Nr, bekommt eine neue UUID.</li>
 * <li>Zeile ohne Nr: bekommt die nächste Nummer laut {@link Strategie}; eine schon vorhandene UUID (Nr 0, z.&nbsp;B.
 * frisch aus PTM-Online importiert) bleibt erhalten, sonst eine neue.</li>
 * <li>Zeile ohne Namen: wird nicht angefasst (das Leeren übernimmt die Meldelisten-Aktualisierung).</li>
 * </ul>
 */
public final class TeamNummernPlan {

	/** Wie fehlende Nummern vergeben werden. */
	public enum Strategie {
		/** Kleinste freie Nummer zuerst (Lücken gelöschter Meldungen werden wiederverwendet). */
		LUECKEN_FUELLEN,
		/** Fortlaufend hinter der höchsten vergebenen Nummer. */
		HOCHZAEHLEN
	}

	/**
	 * Inhalt einer Nr-Zelle.
	 *
	 * @param formel   Formeltext laut {@code getFormulaArray} (leer bei leerer Zelle)
	 * @param nr       angezeigter Wert, {@code <= 0} wenn keine Nummer vergeben ist
	 * @param hatNamen ob in der Zeile ein Spieler- oder Teamname steht
	 */
	public record NrZelle(String formel, int nr, boolean hatNamen) {
	}

	/** Eine neu zu schreibende Nr-Zelle, {@code index} relativ zur ersten Zeile des gelesenen Bereichs. */
	public record Aenderung(int index, int nr, String uuid, String formel) {
	}

	private TeamNummernPlan() {
	}

	/**
	 * @param zellen     Nr-Zellen des Datenbereichs, in Zeilenreihenfolge
	 * @param strategie  Vergabe fehlender Nummern
	 * @param neueUuid   Quelle für neue UUIDs
	 * @return die zu schreibenden Zellen, nach Index aufsteigend
	 */
	public static List<Aenderung> berechne(List<NrZelle> zellen, Strategie strategie, Supplier<String> neueUuid) {
		Set<Integer> belegt = new HashSet<>();
		for (NrZelle zelle : zellen) {
			if (zelle.hatNamen() && zelle.nr() > 0) {
				belegt.add(zelle.nr());
			}
		}
		NummernQuelle nummern = new NummernQuelle(strategie, belegt);
		Set<String> vergebeneUuids = new HashSet<>();
		List<Aenderung> aenderungen = new ArrayList<>();
		for (int index = 0; index < zellen.size(); index++) {
			NrZelle zelle = zellen.get(index);
			if (!zelle.hatNamen()) {
				continue;
			}
			boolean hatNr = zelle.nr() > 0;
			Optional<NrZellFormel> eigene = NrZellFormel.lese(zelle.formel(), NrZellFormel.Art.TEAM);
			// Erstes Vorkommen behält UUID und Online-ID; eine kopierte Zeile wird eine neue Meldung.
			boolean uuidBleibt = eigene.isPresent() && vergebeneUuids.add(eigene.get().uuid());
			if (hatNr && uuidBleibt) {
				continue;
			}
			int nr = hatNr ? zelle.nr() : nummern.naechste();
			NrZellFormel neu = uuidBleibt ? eigene.get() : NrZellFormel.team(neueUuid.get());
			vergebeneUuids.add(neu.uuid());
			aenderungen.add(new Aenderung(index, nr, neu.uuid(), neu.formel(nr)));
		}
		return aenderungen;
	}


	private static final class NummernQuelle {
		private final Strategie strategie;
		private final Set<Integer> belegt;
		private int kandidat;

		NummernQuelle(Strategie strategie, Set<Integer> belegt) {
			this.strategie = strategie;
			this.belegt = belegt;
			this.kandidat = strategie == Strategie.HOCHZAEHLEN
					? belegt.stream().mapToInt(Integer::intValue).max().orElse(0) + 1
					: 1;
		}

		int naechste() {
			while (strategie == Strategie.LUECKEN_FUELLEN && belegt.contains(kandidat)) {
				kandidat++;
			}
			int nr = kandidat++;
			belegt.add(nr);
			return nr;
		}
	}
}
