/**
 * Erstellung : 15.04.2026 / Michael Massee
 **/

package de.petanqueturniermanager.helper.cellstyle;

import de.petanqueturniermanager.helper.ColorHelper;

/**
 * Bedingte Formatierung für editierbare Zellen in ungeraden Zeilen unter Blattschutz (sehr helles Grün).
 * Wird als bedingte Formatierung gesetzt, sodass {@code CellBackColor} (direkte Eigenschaft)
 * die normale Zebra-Farbe behält und der TabellenMapper diese unverändert an den HTML-View übergibt.
 */
public class BlattschutzEditierbarUnGeradeStyle extends AbstractHintergrundFarbeStyle {

	public BlattschutzEditierbarUnGeradeStyle() {
		super(CellStyleDefName.BlattschutzEditierbarUnGerade, ColorHelper.FARBE_BEARBEITBAR_ZEILE_UNGERADE);
	}
}
