/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet.blattschutz;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.util.CellProtection;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;
import de.petanqueturniermanager.helper.sheet.EditierfarbePruefung;
import de.petanqueturniermanager.toolbar.TurnierModus;

/**
 * UITest-Prüfhilfe: Das vorbelegte Ergebnis einer Freilos-Zeile ist weder als editierbar markiert noch im
 * Turniermodus entsperrt, das Ergebnis einer echten Begegnung dagegen schon.
 */
public final class FreilosSperrePruefung {

	private FreilosSperrePruefung() {
	}

	/**
	 * @param ergebnisSpalten Ergebnis-Spalten (A und B) der Spielrunde
	 * @param freilosZeile Zeile einer Freilos-Paarung
	 * @param begegnungZeile Zeile einer echten Begegnung
	 */
	public static void pruefe(WorkingSpreadsheet ws, IBlattschutzKonfiguration konfiguration, XSpreadsheet sheet,
			List<Integer> ergebnisSpalten, int freilosZeile, int begegnungZeile) throws Exception {
		for (int spalte : ergebnisSpalten) {
			Position freilos = Position.from(spalte, freilosZeile);
			assertThat(hatEditierfarbe(sheet, freilos)).as("Freilos-Ergebnis %s ohne Editierfarbe",
					freilos.getAddress()).isFalse();
			Position begegnung = Position.from(spalte, begegnungZeile);
			assertThat(hatEditierfarbe(sheet, begegnung)).as("Begegnung %s mit Editierfarbe",
					begegnung.getAddress()).isTrue();
		}

		TurnierModus.get().setAktivForTest(true);
		try {
			BlattschutzManager.get().schuetzen(konfiguration, ws);
			for (int spalte : ergebnisSpalten) {
				Position freilos = Position.from(spalte, freilosZeile);
				assertThat(istGesperrt(sheet, freilos)).as("Freilos-Ergebnis %s gesperrt", freilos.getAddress())
						.isTrue();
				Position begegnung = Position.from(spalte, begegnungZeile);
				assertThat(istGesperrt(sheet, begegnung)).as("Begegnung %s editierbar", begegnung.getAddress())
						.isFalse();
			}
		} finally {
			BlattschutzManager.get().entsperren(konfiguration, ws);
			TurnierModus.get().setAktivForTest(false);
		}
	}

	private static boolean hatEditierfarbe(XSpreadsheet sheet, Position position) throws Exception {
		return EditierfarbePruefung.cfFormeln(sheet, position).stream()
				.anyMatch(formel -> formel.contains(EditierbaresZelleFormatHelper.PROPERTY_KEY));
	}

	private static boolean istGesperrt(XSpreadsheet sheet, Position position) throws Exception {
		XPropertySet props = Lo.qi(XPropertySet.class,
				sheet.getCellByPosition(position.getSpalte(), position.getZeile()));
		return ((CellProtection) props.getPropertyValue("CellProtection")).IsLocked;
	}
}
