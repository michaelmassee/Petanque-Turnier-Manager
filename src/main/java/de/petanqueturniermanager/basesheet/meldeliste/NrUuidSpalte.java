/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import com.sun.star.sheet.XCalculatable;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.addins.GlobalImpl;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;

/**
 * Identitäten einer Nr-Spalte (Meldeliste, Mêlée-Anmeldung): Lokale UUID und optional die PTM-Online-ID stehen als
 * Literale in der Nr-Zelle ({@link NrZellFormel}) und wandern beim Sortieren mit. Liest Formeln und Werte je in
 * einem Zugriff und schreibt nur geänderte Zellen, blockweise. Zeilen werden 1-basiert (Sheet-Zeile) angesprochen.
 */
public final class NrUuidSpalte {

	private static final int MAX_ZEILE = 999;

	private final SheetHelper sheetHelper;
	private final XSpreadsheet sheet;
	private final XSpreadsheetDocument doc;
	private final int nrSpalte;
	private final int ersteDatenZeile;
	private final NrZellFormel.Art art;

	/**
	 * @param ersteDatenZeile erste Datenzeile (0-basiert)
	 * @param art             Formel neu angelegter Nr-Zellen (Team- oder Spieler-Liste)
	 */
	public NrUuidSpalte(SheetHelper sheetHelper, XSpreadsheet sheet, XSpreadsheetDocument doc, int nrSpalte,
			int ersteDatenZeile, NrZellFormel.Art art) {
		this.sheetHelper = sheetHelper;
		this.sheet = sheet;
		this.doc = doc;
		this.nrSpalte = nrSpalte;
		this.ersteDatenZeile = ersteDatenZeile;
		this.art = art;
	}

	/** @return UUID je 1-basierter Zeile, nur für Zeilen mit UUID */
	public Map<Integer, String> lese(Collection<Integer> zeilen1Basiert) {
		if (zeilen1Basiert.isEmpty()) {
			return Map.of();
		}
		Block block = leseBlock(maxZeile(zeilen1Basiert));
		Map<Integer, String> ergebnis = new LinkedHashMap<>();
		for (int zeile : zeilen1Basiert) {
			block.zelle(zeile).ifPresent(zelle -> ergebnis.put(zeile, zelle.uuid()));
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
		Block block = leseBlock(maxZeile(zeilen1Basiert));
		Map<Integer, String> ergebnis = new LinkedHashMap<>();
		SortedMap<Integer, String> neueFormeln = new TreeMap<>();
		for (int zeile : zeilen1Basiert) {
			if (ergebnis.containsKey(zeile)) {
				continue;
			}
			Optional<NrZellFormel> vorhanden = block.zelle(zeile);
			NrZellFormel zelle = vorhanden
					.orElseGet(() -> new NrZellFormel(art, UUID.randomUUID().toString(), Optional.empty()));
			ergebnis.put(zeile, zelle.uuid());
			if (vorhanden.isEmpty()) {
				neueFormeln.put(zeile - 1, zelle.formel(block.nr(zeile)));
			}
		}
		TeamNummernVergabe.schreibeNrFormeln(sheetHelper, sheet, nrSpalte, neueFormeln);
		return ergebnis;
	}

	/**
	 * Schreibt die UUIDs in die Nr-Zellen; die angezeigte Nr bleibt. Die Online-ID bleibt nur, wenn die UUID gleich
	 * bleibt – eine andere UUID ist eine andere Meldung.
	 */
	public void setze(Map<Integer, String> uuidProZeile) {
		if (uuidProZeile.isEmpty()) {
			return;
		}
		Block block = leseBlock(maxZeile(uuidProZeile.keySet()));
		SortedMap<Integer, String> formeln = new TreeMap<>();
		uuidProZeile.forEach((zeile, uuid) -> {
			NrZellFormel zelle = block.zelle(zeile).filter(alt -> alt.uuid().equalsIgnoreCase(uuid))
					.orElseGet(() -> new NrZellFormel(art, uuid, Optional.empty()));
			formeln.put(zeile - 1, zelle.formel(block.nr(zeile)));
		});
		TeamNummernVergabe.schreibeNrFormeln(sheetHelper, sheet, nrSpalte, formeln);
	}

	/** @return Online-ID je lokaler UUID aller Zeilen der Spalte, die eine tragen */
	public Map<String, String> onlineIdsProUuid() {
		Block block = leseBlock(MAX_ZEILE + 1);
		Map<String, String> ergebnis = new LinkedHashMap<>();
		for (int zeile = ersteDatenZeile + 1; zeile <= MAX_ZEILE + 1; zeile++) {
			block.zelle(zeile).ifPresent(
					zelle -> zelle.onlineId().ifPresent(id -> ergebnis.putIfAbsent(zelle.uuid(), id)));
		}
		return ergebnis;
	}

	/**
	 * Setzt oder entfernt ({@link Optional#empty()}) die Online-ID der Zeilen mit diesen lokalen UUIDs; UUID und
	 * angezeigte Nr bleiben. Unbekannte UUIDs werden übergangen.
	 *
	 * @return lokale UUIDs, deren Zeile gefunden wurde
	 */
	public Set<String> setzeOnlineIds(Map<String, Optional<String>> onlineIdProUuid) {
		if (onlineIdProUuid.isEmpty()) {
			return Set.of();
		}
		Map<String, Optional<String>> gesucht = new LinkedHashMap<>();
		onlineIdProUuid.forEach((uuid, id) -> gesucht.put(uuid.toLowerCase(Locale.ROOT), id));
		Block block = leseBlock(MAX_ZEILE + 1);
		SortedMap<Integer, String> formeln = new TreeMap<>();
		Set<String> gefunden = new HashSet<>();
		for (int zeile = ersteDatenZeile + 1; zeile <= MAX_ZEILE + 1; zeile++) {
			int z = zeile;
			block.zelle(zeile).filter(zelle -> gesucht.containsKey(zelle.uuid())).ifPresent(zelle -> {
				Optional<String> neu = gesucht.get(zelle.uuid());
				gefunden.add(zelle.uuid());
				if (!neu.equals(zelle.onlineId())) {
					formeln.put(z - 1, zelle.mitOnlineId(neu).formel(block.nr(z)));
				}
			});
		}
		TeamNummernVergabe.schreibeNrFormeln(sheetHelper, sheet, nrSpalte, formeln);
		if (!formeln.isEmpty()) {
			// Neu geschriebene Formeln zeigen bis zur Berechnung 0 – bei abgeschalteter automatischer Berechnung
			// (Rundenaufbau) läsen Nr-Leser sonst falsche Nummern. calculate() rechnet nur geänderte Zellen.
			Lo.qi(XCalculatable.class, doc).calculate();
		}
		return gefunden;
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

	private static int maxZeile(Collection<Integer> zeilen1Basiert) {
		return zeilen1Basiert.stream().mapToInt(Integer::intValue).max().orElse(1);
	}

	/** Nr-Spalte von der ersten Datenzeile bis {@code bisZeile1Basiert}. */
	private Block leseBlock(int bisZeile1Basiert) {
		int letzteZeile = Math.min(MAX_ZEILE, bisZeile1Basiert - 1);
		int ersteZeile = Math.min(ersteDatenZeile, letzteZeile);
		RangePosition bereich = RangePosition.from(nrSpalte, ersteZeile, nrSpalte, letzteZeile);
		return new Block(ersteZeile, art, sheetHelper.getFormulaArrayFromRange(sheet, bereich),
				RangeHelper.from(sheet, doc, bereich).getDataFromRange());
	}

	private record Block(int ersteZeile, NrZellFormel.Art art, String[][] formeln, RangeData werte) {

		Optional<NrZellFormel> zelle(int zeile1Basiert) {
			int index = zeile1Basiert - 1 - ersteZeile;
			if (index < 0 || index >= formeln.length || formeln[index].length == 0) {
				return Optional.empty();
			}
			return NrZellFormel.lese(formeln[index][0], art);
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
