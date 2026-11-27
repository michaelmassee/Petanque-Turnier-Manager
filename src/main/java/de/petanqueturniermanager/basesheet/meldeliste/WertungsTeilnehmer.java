/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;

/**
 * Welche Teams in Wertung und Rangliste eines rundenbasierten Turniers (Schweizer, Formule X) stehen.
 * <p>
 * Bereits gespielte Partien zählen immer, unabhängig vom Aktiv-Wert: Wer gespielt hat, bleibt in der Rangliste, und
 * seine Gegner behalten Siege, Punkte und Buchholz. Gelistet werden daher alle Teams mit Aktiv-Wert 1 oder 2 und
 * zusätzlich Teams, deren Aktiv-Zelle nach gespielten Partien geleert wurde. Letzteres ist eine Fehlbedienung (für
 * einen Ausstieg gehört 2 eingetragen); sie wird wie ein Ausstieg gewertet und beim Auslosen korrigiert.
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
	 * Trägt für Teams, die gespielt haben, deren Aktiv-Zelle aber leer ist, den Ausstiegs-Wert ein und vermerkt das in
	 * der Fortschrittsanzeige. Die Aktiv-Spalte wird als ein Block gelesen und zurückgeschrieben; leere Zellen bleiben
	 * dabei leer (Calc übernimmt beim Block-Schreiben keine leeren Strings).
	 *
	 * @param aktivSpalte      Aktiv-Spalte der Meldeliste
	 * @param ausgestiegenWert Aktiv-Wert für „ausgestiegen“
	 * @param ohneAktivWert    betroffene Teams (siehe {@link #ohneAktivWertMitPartie})
	 */
	public static void alsAusgestiegenEintragen(IMeldeliste<?, ?> meldeliste, int aktivSpalte, int ausgestiegenWert,
			Set<Integer> ohneAktivWert) throws GenerateException {
		if (ohneAktivWert.isEmpty()) {
			return;
		}
		MeldungenSpalte<?, ?> meldungenSpalte = meldeliste.getMeldungenSpalte();
		int ersteZeile = meldungenSpalte.getErsteDatenZiele();
		List<Integer> zeilen = new ArrayList<>();
		for (int teamNr : ohneAktivWert) {
			int zeile = meldungenSpalte.getSpielerZeileNr(teamNr);
			if (zeile >= ersteZeile) {
				zeilen.add(zeile);
			}
		}
		if (zeilen.isEmpty()) {
			return;
		}
		RangePosition aktivBereich = RangePosition.from(aktivSpalte, ersteZeile, aktivSpalte,
				Collections.max(zeilen));
		RangeData aktivWerte = RangeHelper.from(meldeliste, aktivBereich).getDataFromRange();
		zeilen.forEach(zeile -> aktivWerte.get(zeile - ersteZeile).get(0).setVal(ausgestiegenWert));
		RangeHelper.from(meldeliste, aktivBereich).setDataInRange(aktivWerte);

		String teamNrn = ohneAktivWert.stream().map(String::valueOf).collect(Collectors.joining(", "));
		ProcessBox.from().info(I18n.get("processbox.aktiv.leer.als.ausgestiegen", teamNrn));
	}

	private static Set<Integer> teamNummern(TeamMeldungen meldungen) {
		return meldungen.teams().stream().map(Team::getNr).collect(Collectors.toSet());
	}
}
