/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

/**
 * Ein einzelnes Spielpaar innerhalb einer {@link KaskadenKoGruppenRunde}.<br>
 * <br>
 * Die Positionen beziehen sich auf die 1-basierte Reihenfolge innerhalb der
 * jeweiligen Gruppe. In Runde 1 entsprechen sie direkt den Setzpositionen (SP)
 * der Teams; in späteren Runden sind es die relativen Positionen innerhalb der
 * entstandenen Sieger- oder Verlierer-Gruppen.<br>
 * <br>
 * <b>Freilos-Konvention:</b> Das Freilos-Team (bei ungerader Teamanzahl immer
 * die letzte Position in der Gruppe) erscheint <em>nicht</em> in den
 * {@code spielPaare} einer {@link KaskadenKoGruppenRunde}. Es wird durch die
 * Größenberechnung implizit in die Verlierer-Gruppe übernommen.<br>
 * <br>
 * <b>Zielposition-Hinweis:</b> {@code zielPositionSieger} und
 * {@code zielPositionVerlierer} beschreiben, in welche Slot-Nummer des
 * Folge-Feldes der Sieger bzw. Verlierer dieses Spiels gelangt. Sie sind
 * zunächst {@code null} (noch nicht befüllt) und können von der aufrufenden
 * Schicht ergänzt werden, sobald die Mapping-Logik implementiert ist.
 *
 * @param spielNr               laufende Nummer innerhalb der Gruppe (1-basiert)
 * @param positionA             erste Teamposition in der Gruppe (1-basiert)
 * @param positionB             zweite Teamposition in der Gruppe (1-basiert)
 * @param zielPositionSieger    Ziel-Slot im nächsten Sieger-Feld; {@code null} wenn noch nicht befüllt
 * @param zielPositionVerlierer Ziel-Slot im nächsten Verlierer-Feld; {@code null} wenn noch nicht befüllt
 *
 * @author Michael Massee
 * @see KaskadenKoGruppenRunde
 * @see KaskadenKoRundenPlaner
 */
public record KaskadenKoSpielPaar(
        int spielNr,
        int positionA,
        int positionB,
        Integer zielPositionSieger,
        Integer zielPositionVerlierer) {
}
