/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;

/**
 * Vergibt fortlaufende Nummern (Spalte {@code Nr}) für alle Zeilen mit Namen im
 * Mêlée-Anmeldung-Sheet.
 * <p>
 * Eigenständiger Helper statt Methode auf {@link AbstractMeleeAnmeldungSheet}, damit auch
 * {@link AbstractMeleeAnmeldungUebernehmenSheet} neu hinzugefügte, noch nicht durchnummerierte
 * Zeilen (z.B. per Copy-Paste ohne vorherigen Aufruf des Menüpunkts „Mêlée Anmeldung") vor dem
 * Übernehmen ergänzen kann.
 */
public final class MeleeAnmeldungNummerierung implements MeleeAnmeldungKonstanten {

	private MeleeAnmeldungNummerierung() {
	}

	/**
	 * Vergibt fortlaufende Nummern (1..n) für alle Zeilen mit Namen (blockweise geschrieben).
	 * Zeilen ohne Namen bleiben unberührt, damit die Nummerierung dem Anwender nicht in leere
	 * Zeilen „vorläuft". Bereits vergebene Nummern werden überschrieben, sodass Lücken (z.B. durch
	 * nachträglich per Copy-Paste eingefügte Zeilen ohne Nummer) geschlossen werden.
	 * <p>
	 * Die Nr-Zelle trägt die UUID des Spielers ({@code PTM.ALG.TNRUUID}); vorhandene UUIDs bleiben, fehlende
	 * entstehen neu. Eine per Copy-Paste verdoppelte UUID bekommt in der Kopie eine neue.
	 */
	public static void nummerieren(WorkingSpreadsheet ws, XSpreadsheet sheet) throws GenerateException {
		List<MeleeAnmeldungZeile> zeilen = MeleeAnmeldungLeser.lesen(ws, sheet);
		if (zeilen.isEmpty()) {
			return;
		}
		SheetHelper helper = new SheetHelper(ws);
		RangePosition bereich = RangePosition.from(SPALTE_NR, ERSTE_DATEN_ZEILE, SPALTE_NR,
				ERSTE_DATEN_ZEILE + zeilen.size() - 1);
		String[][] bisher = helper.getFormulaArrayFromRange(sheet, bereich);
		Set<String> vergeben = new HashSet<>();
		String[][] formeln = new String[zeilen.size()][1];
		for (int idx = 0; idx < zeilen.size(); idx++) {
			String alt = idx < bisher.length && bisher[idx].length > 0 ? bisher[idx][0] : "";
			// Erstes Vorkommen behält UUID und Online-ID; eine kopierte Zeile wird ein neuer Spieler.
			NrZellFormel zelle = NrZellFormel.lese(alt, NrZellFormel.Art.SPIELER)
					.filter(vorhanden -> vergeben.add(vorhanden.uuid()))
					.orElseGet(() -> NrZellFormel.spieler(UUID.randomUUID().toString()));
			vergeben.add(zelle.uuid());
			formeln[idx][0] = zelle.formel(idx + 1);
		}
		BlattschutzManager.get().schreibeEntsperrt(sheet, () -> helper.setFormulaArrayInRange(sheet, bereich, formeln));
	}
}
