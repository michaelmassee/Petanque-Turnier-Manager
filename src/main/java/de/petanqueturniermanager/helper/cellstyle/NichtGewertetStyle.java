/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.cellstyle;

import com.sun.star.awt.FontStrikeout;

import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;

/**
 * Zellvorlage für die Tabelle einer nicht gewerteten Spielrunde: hellgrauer Hintergrund, graue,
 * durchgestrichene Schrift. Wird ausschließlich als bedingte Formatierung verwendet, siehe
 * {@link de.petanqueturniermanager.helper.sheet.NichtGewertetFormatHelper}.
 */
public class NichtGewertetStyle extends AbstractCellStyleDef {

	/** Hintergrundfarbe (hellgrau). */
	public static final int HINTERGRUND_FARBE = 0xd9d9d9;

	/** Schriftfarbe (grau). */
	public static final int SCHRIFT_FARBE = 0x808080;

	public NichtGewertetStyle() {
		super(CellStyleDefName.NichtGewertet, buildCellProperties());
	}

	private static CellProperties buildCellProperties() {
		return CellProperties.from()
				.setCellBackColor(HINTERGRUND_FARBE)
				.setCellbackgroundTransparent(false)
				.setCharColor(SCHRIFT_FARBE)
				.setCharStrikeout(FontStrikeout.SINGLE);
	}
}
