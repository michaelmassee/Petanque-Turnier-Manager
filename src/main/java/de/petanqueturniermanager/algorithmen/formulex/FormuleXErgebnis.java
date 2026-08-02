package de.petanqueturniermanager.algorithmen.formulex;

import java.util.List;

/**
 * Hält die Auswertungsdaten eines Teams für das Formule X Turniersystem.
 * <p>
 * Wird für die Rematch-Vermeidung (Gegnerliste) und die BYE-Vergabe bei der Paarung
 * der nächsten Runde verwendet. Der eigentliche Wertungsscore wird funktional berechnet
 * ({@code berechneWertung()}) und ist bewusst NICHT Teil dieses Records.
 * <p>
 * <b>Achtung — zwei Nutzungsmodi:</b>
 * <ul>
 *   <li><i>Einzelspiel</i>: {@code eigenePunkte}/{@code kassiertePunkte} stammen aus
 *       genau einem Match; {@link #istSieger()} und
 *       {@link FormuleX#berechneWertung(FormuleXErgebnis, int)} sind direkt
 *       anwendbar.</li>
 *   <li><i>Aggregat über mehrere Runden</i>: {@code eigenePunkte}/{@code kassiertePunkte}
 *       sind Summen über alle bisher gespielten Runden. In diesem Modus liefert
 *       {@link #istSieger()} <b>keine</b> sinnvolle Aussage über die Sieg-Bilanz, weil
 *       knappe Siege durch eine hohe Niederlage in der Summe „verloren“ aussehen
 *       können. {@link FormuleX#berechneWertung(FormuleXErgebnis, int)} darf hier
 *       <b>nicht</b> verwendet werden — stattdessen muss die Wertungssumme spielweise
 *       extern aufsummiert werden (siehe {@link FormuleXTeamErgebnis} und
 *       {@link FormuleXRanglisteRechner}, die für die eigentliche Ranglisten-/
 *       Paarungsreihenfolge zuständig sind).</li>
 * </ul>
 *
 * @param teamNr          Nummer des Teams
 * @param eigenePunkte    Erzielte Punkte (Einzelspiel oder Aggregat-Summe — siehe oben)
 * @param kassiertePunkte Kassierte Punkte (Einzelspiel oder Aggregat-Summe)
 * @param gegnerNrn       Team-Nummern aller bisherigen Gegner (für Rematch-Prüfung)
 * @param hatteFreilos    true wenn das Team irgendwann ein Freilos (BYE) hatte
 */
public record FormuleXErgebnis(int teamNr, int eigenePunkte, int kassiertePunkte,
		List<Integer> gegnerNrn, boolean hatteFreilos) {

	/**
	 * Prüft ob dieses Team gewonnen hat.
	 * <p>
	 * Ein Team gewinnt wenn:
	 * <ul>
	 *   <li>es ein Freilos hatte (BYE zählt als Sieg), oder</li>
	 *   <li>es mehr eigene Punkte als kassierte Punkte erzielt hat</li>
	 * </ul>
	 *
	 * @return true wenn Sieg, false wenn Niederlage
	 */
	public boolean istSieger() {
		return hatteFreilos || eigenePunkte > kassiertePunkte;
	}

	/**
	 * Berechnet die Punktedifferenz aus eigenen und kassierten Punkten.
	 *
	 * @return eigenePunkte - kassiertePunkte (positiv bei Sieg, negativ bei Niederlage)
	 */
	public int punktedifferenz() {
		return eigenePunkte - kassiertePunkte;
	}
}