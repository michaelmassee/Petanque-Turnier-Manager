/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.addins.GlobalImpl;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;

/**
 * Lokale UUIDs einer Nr-Spalte (Meldeliste, Mêlée-Anmeldung): Jede UUID steht als Literal in der Nr-Zelle
 * ({@code PTM.ALG.TNRUUID(nr;"uuid")}) und wandert beim Sortieren mit. Liest Formeln und Werte je in einem Zugriff
 * und schreibt nur geänderte Zellen, blockweise. Zeilen werden 1-basiert (Sheet-Zeile) angesprochen.
 */
public final class NrUuidSpalte {

	private static final int MAX_ZEILE = 999;

	private final SheetHelper sheetHelper;
	private final XSpreadsheet sheet;
	private final XSpreadsheetDocument doc;
	private final int nrSpalte;
	private final int ersteDatenZeile;

	/**
	 * @param ersteDatenZeile erste Datenzeile (0-basiert)
	 */
	public NrUuidSpalte(SheetHelper sheetHelper, XSpreadsheet sheet, XSpreadsheetDocument doc, int nrSpalte,
			int ersteDatenZeile) {
		this.sheetHelper = sheetHelper;
		this.sheet = sheet;
		this.doc = doc;
		this.nrSpalte = nrSpalte;
		this.ersteDatenZeile = ersteDatenZeile;
	}

	/** @return UUID je 1-basierter Zeile, nur für Zeilen mit UUID */
	public Map<Integer, String> lese(Collection<Integer> zeilen1Basiert) {
		if (zeilen1Basiert.isEmpty()) {
			return Map.of();
		}
		Block block = leseBlock(zeilen1Basiert);
		Map<Integer, String> ergebnis = new LinkedHashMap<>();
		for (int zeile : zeilen1Basiert) {
			block.uuid(zeile).ifPresent(uuid -> ergebnis.put(zeile, uuid));
		}
		return ergebnis;
	}

	/**
	 * Vorhandene UUIDs; fehlende entstehen in der Nr-Zelle. Eine noch nicht nummerierte Zeile bekommt Nr 0, die
	 * nächste Nummernvergabe behält die UUID.
	 *
	 * @return UUID je 1-basierter Zeile, in der Reihenfolge von {@code zeilen1Basiert}
	 */
	public Map<Integer, String> getOderErzeuge(Collection<Integer> zeilen1Basiert) {
		if (zeilen1Basiert.isEmpty()) {
			return Map.of();
		}
		Block block = leseBlock(zeilen1Basiert);
		Map<Integer, String> ergebnis = new LinkedHashMap<>();
		SortedMap<Integer, String> neueFormeln = new TreeMap<>();
		for (int zeile : zeilen1Basiert) {
			if (ergebnis.containsKey(zeile)) {
				continue;
			}
			Optional<String> vorhanden = block.uuid(zeile);
			String uuid = vorhanden.orElseGet(() -> UUID.randomUUID().toString());
			ergebnis.put(zeile, uuid);
			if (vorhanden.isEmpty()) {
				neueFormeln.put(zeile - 1, GlobalImpl.FORMAT_PTM_TNR_UUID(block.nr(zeile), uuid));
			}
		}
		TeamNummernVergabe.schreibeNrFormeln(sheetHelper, sheet, nrSpalte, neueFormeln);
		return ergebnis;
	}

	/** Schreibt die UUIDs in die Nr-Zellen; die angezeigte Nr bleibt. */
	public void setze(Map<Integer, String> uuidProZeile) {
		if (uuidProZeile.isEmpty()) {
			return;
		}
		Block block = leseBlock(uuidProZeile.keySet());
		SortedMap<Integer, String> formeln = new TreeMap<>();
		uuidProZeile.forEach((zeile, uuid) -> formeln.put(zeile - 1, GlobalImpl.FORMAT_PTM_TNR_UUID(block.nr(zeile), uuid)));
		TeamNummernVergabe.schreibeNrFormeln(sheetHelper, sheet, nrSpalte, formeln);
	}

	/**
	 * Formel für die aktuelle Nr zur UUID, leer wenn die Zeile fehlt – bleibt nach Umnummerieren und Sortieren
	 * richtig.
	 *
	 * @param nrBereich Nr-Spalte in Formel-Notation inkl. Blatt, z.&nbsp;B. {@code $'Meldeliste'.$A$1:$A$1000}
	 */
	public static String formelNrAusUuid(String nrBereich, String uuid) {
		return "IFNA(INDEX(" + nrBereich + ";" + GlobalImpl.FORMAT_PTM_UUID_ZEILE("\"" + uuid + "\"", nrBereich)
				+ ");\"\")";
	}

	/** Nr-Spalte von der ersten Datenzeile bis zur höchsten angefragten Zeile. */
	private Block leseBlock(Collection<Integer> zeilen1Basiert) {
		int letzteZeile = Math.min(MAX_ZEILE, zeilen1Basiert.stream().mapToInt(Integer::intValue).max().orElse(1) - 1);
		int ersteZeile = Math.min(ersteDatenZeile, letzteZeile);
		RangePosition bereich = RangePosition.from(nrSpalte, ersteZeile, nrSpalte, letzteZeile);
		return new Block(ersteZeile, sheetHelper.getFormulaArrayFromRange(sheet, bereich),
				RangeHelper.from(sheet, doc, bereich).getDataFromRange());
	}

	private record Block(int ersteZeile, String[][] formeln, RangeData werte) {

		Optional<String> uuid(int zeile1Basiert) {
			int index = zeile1Basiert - 1 - ersteZeile;
			if (index < 0 || index >= formeln.length || formeln[index].length == 0) {
				return Optional.empty();
			}
			return TeamUuidFormel.findeUuid(formeln[index][0]);
		}

		/** Angezeigte Nr, 0 wenn noch keine vergeben ist. */
		int nr(int zeile1Basiert) {
			int index = zeile1Basiert - 1 - ersteZeile;
			if (index < 0 || index >= werte.size() || werte.get(index).isEmpty()) {
				return 0;
			}
			return Math.max(0, werte.get(index).get(0).getIntVal(0));
		}
	}
}
