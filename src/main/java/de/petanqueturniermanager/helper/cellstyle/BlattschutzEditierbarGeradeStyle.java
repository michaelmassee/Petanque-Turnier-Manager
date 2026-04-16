/**
 * Erstellung : 15.04.2026 / Michael Massee
 **/

package de.petanqueturniermanager.helper.cellstyle;

import de.petanqueturniermanager.helper.ColorHelper;

/**
 * Bedingte Formatierung für editierbare Zellen in geraden Zeilen unter Blattschutz (helles Grün).
 * Wird als bedingte Formatierung gesetzt, sodass {@code CellBackColor} (direkte Eigenschaft)
 * die normale Zebra-Farbe behält und der TabellenMapper diese unverändert an den HTML-View übergibt.
 */
public class BlattschutzEditierbarGeradeStyle extends AbstractHintergrundFarbeStyle {

	public BlattschutzEditierbarGeradeStyle() {
		super(CellStyleDefName.BlattschutzEditierbarGerade, ColorHelper.FARBE_BEARBEITBAR_ZEILE_GERADE);
	}
}
