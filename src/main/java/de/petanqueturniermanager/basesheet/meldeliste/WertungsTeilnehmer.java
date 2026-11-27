/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;

/**
 * Welche Teams in Wertung und Rangliste eines rundenbasierten Turniers (Schweizer, Formule X) stehen.
 * <p>
 * Bereits gespielte Partien zählen immer, unabhängig vom Aktiv-Wert: Wer gespielt hat, bleibt in der Rangliste, und
 * seine Gegner behalten Siege, Punkte und Buchholz. Gelistet werden daher alle Teams mit Aktiv-Wert 1 oder 2 und
 * zusätzlich Teams, deren Aktiv-Zelle nach gespielten Partien geleert wurde. Letzteres ist eine Fehlbedienung (für
 * einen Ausstieg gehört 2 eingetragen); sie wird wie ein Ausstieg gewertet und beim Auslosen gemeldet.
 */
public final class WertungsTeilnehmer {

	private WertungsTeilnehmer() {
		// Utility-Klasse – keine Instanzen
	}

	/**
	 * @param mitAktivWert   Meldungen mit Aktiv-Wert 1 oder 2
	 * @param teamsMitPartie Teams, die in einer gewerteten Runde vorkommen
	 * @return Teamnummern der Rangliste
	 */
	public static Set<Integer> gelistet(TeamMeldungen mitAktivWert, Set<Integer> teamsMitPartie) {
		Set<Integer> gelistet = new LinkedHashSet<>(teamNummern(mitAktivWert));
		gelistet.addAll(teamsMitPartie);
		return gelistet;
	}

	/**
	 * @return Teams, die gespielt haben, deren Aktiv-Zelle aber leer ist
	 */
	public static SortedSet<Integer> ohneAktivWertMitPartie(TeamMeldungen mitAktivWert, Set<Integer> teamsMitPartie) {
		SortedSet<Integer> ohneAktivWert = new TreeSet<>(teamsMitPartie);
		ohneAktivWert.removeAll(teamNummern(mitAktivWert));
		return ohneAktivWert;
	}

	/**
	 * Weist darauf hin, dass Teams mit gespielten Partien keinen Aktiv-Wert haben. Sie werden wie ausgestiegen
	 * gewertet; der Hinweis bittet, 2 einzutragen.
	 */
	public static void hinweisFallsNoetig(WorkingSpreadsheet workingSpreadsheet, Set<Integer> ohneAktivWert) {
		if (ohneAktivWert.isEmpty()) {
			return;
		}
		String teamNrn = ohneAktivWert.stream().map(String::valueOf).collect(Collectors.joining(", "));
		MessageBox.from(workingSpreadsheet, MessageBoxTypeEnum.WARN_OK)
				.caption(I18n.get("msg.caption.aktiv.leer.mit.partie"))
				.message(I18n.get("msg.text.aktiv.leer.mit.partie", teamNrn))
				.show();
	}

	private static Set<Integer> teamNummern(TeamMeldungen meldungen) {
		return meldungen.teams().stream().map(Team::getNr).collect(Collectors.toSet());
	}
}
