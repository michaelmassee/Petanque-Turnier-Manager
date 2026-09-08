/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungKonstanten;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;

/** Regression: derselbe Mêlée-Spieler darf nicht ein zweites Mal übernommen werden. */
class MaastrichterMeleeAnmeldungUebernehmenUITest extends BaseCalcUITest implements MeleeAnmeldungKonstanten {

	/** Entspricht SchweizerListeDelegate.ERSTE_DATEN_ZEILE (identisches Meldeliste-Layout). */
	private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;

	private MaastrichterMeldeListeSheetUpdate meldeliste;

	@Test
	void zweiteUebernahmeSchreibtKeineDoppeltenTeilnehmer() throws Exception {
		new MaastrichterMeldeListeSheetNew(wkingSpreadsheet)
				.erstelleMeldeliste(Formation.TRIPLETTE, false, false, SpielplanTeamAnzeige.NR);
		meldeliste = new MaastrichterMeldeListeSheetUpdate(wkingSpreadsheet);
		docPropHelper.setBooleanProperty(BasePropertiesSpalte.KONFIG_PROP_MELEE_ANMELDUNG, true);
		meleeAnmeldungenAnlegen(6);

		new MaastrichterMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();
		List<String> nachErsterUebernahme = nachnamenInMeldeliste();

		new MaastrichterMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();

		assertThat(nachnamenInMeldeliste())
				.as("eine erneute Übernahme darf keine Spieler ein zweites Mal in die Meldeliste schreiben")
				.containsExactlyInAnyOrderElementsOf(nachErsterUebernahme)
				.doesNotHaveDuplicates()
				.hasSize(6);
	}

	private void meleeAnmeldungenAnlegen(int anzahl) throws Exception {
		MaastrichterMeleeAnmeldungSheet melee = new MaastrichterMeleeAnmeldungSheet(wkingSpreadsheet);
		melee.generate();
		RangeData data = new RangeData();
		for (int i = 0; i < anzahl; i++) {
			RowData zeile = data.addNewRow();
			zeile.newInt(i + 1);
			zeile.newString("Vorname " + i);
			zeile.newString("MeleeTestNachname" + i);
			zeile.newEmpty();
			zeile.newString(MARKIERUNG);
			zeile.newEmpty();
		}
		RangeHelper.from(melee.getXSpreadSheet(), doc,
				data.getRangePosition(Position.from(SPALTE_NR, ERSTE_DATEN_ZEILE))).setDataInRange(data);
	}

	private List<String> nachnamenInMeldeliste() throws Exception {
		RangeData data = RangeHelper.from(meldeliste.getXSpreadSheet(), doc,
				RangePosition.from(0, MELDELISTE_ERSTE_DATEN_ZEILE, meldeliste.getAktivSpalte(),
						MELDELISTE_ERSTE_DATEN_ZEILE + 30)).getDataFromRange();
		List<String> nachnamen = new ArrayList<>();
		for (RowData zeile : data) {
			for (int spalte = 0; spalte <= meldeliste.getAktivSpalte(); spalte++) {
				String wert = zeile.get(spalte).getStringVal();
				if (wert != null && wert.startsWith("MeleeTestNachname")) {
					nachnamen.add(wert);
				}
			}
		}
		return nachnamen;
	}
}
