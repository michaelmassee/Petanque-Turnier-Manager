/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.TeamNummernPlan.Aenderung;
import de.petanqueturniermanager.basesheet.meldeliste.TeamNummernPlan.NrZelle;
import de.petanqueturniermanager.basesheet.meldeliste.TeamNummernPlan.Strategie;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;

/**
 * Vergibt fehlende Teamnummern einer Meldeliste und stellt sicher, dass jede Zeile mit Namen ihre UUID in der
 * Nr-Zelle trägt ({@code PTM.ALG.TNRUUID(nr;"uuid")}). Liest Nr- und Namensspalte je einmal als Block und schreibt
 * nur geänderte Zeilen, ebenfalls blockweise – unveränderte Zellen werden nicht zurückgeschrieben, weil
 * {@code setFormulaArray} reine Zahlen sonst als Text ablegen würde.
 */
public final class TeamNummernVergabe {

	private final ISheet sheet;
	private final int nrSpalte;
	private final int namenSpalte;
	private final int ersteZeile;

	private TeamNummernVergabe(ISheet sheet, int nrSpalte, int namenSpalte, int ersteZeile) {
		this.sheet = sheet;
		this.nrSpalte = nrSpalte;
		this.namenSpalte = namenSpalte;
		this.ersteZeile = ersteZeile;
	}

	/**
	 * @param namenSpalte Spalte, an der eine belegte Zeile erkannt wird (erster Vorname bzw. Teamname)
	 * @param ersteZeile  erste Datenzeile (0-basiert)
	 */
	public static TeamNummernVergabe from(ISheet sheet, int nrSpalte, int namenSpalte, int ersteZeile) {
		return new TeamNummernVergabe(sheet, nrSpalte, namenSpalte, ersteZeile);
	}

	/**
	 * @param letzteZeile letzte Zeile mit Namen (0-basiert); liegt sie vor der ersten Datenzeile, passiert nichts
	 */
	public void vergebe(int letzteZeile, Strategie strategie) throws GenerateException {
		if (letzteZeile < ersteZeile) {
			return;
		}
		SheetHelper helper = sheet.getSheetHelper();
		XSpreadsheet xSheet = sheet.getXSpreadSheet();
		RangePosition nrBereich = RangePosition.from(nrSpalte, ersteZeile, nrSpalte, letzteZeile);
		String[][] formeln = helper.getFormulaArrayFromRange(xSheet, nrBereich);
		RangeData nummern = RangeHelper.from(sheet, nrBereich).getDataFromRange();
		RangeData namen = RangeHelper.from(sheet, RangePosition.from(namenSpalte, ersteZeile, namenSpalte, letzteZeile))
				.getDataFromRange();

		List<NrZelle> zellen = new ArrayList<>();
		for (int index = 0; index <= letzteZeile - ersteZeile; index++) {
			zellen.add(new NrZelle(formel(formeln, index), nr(nummern, index), hatNamen(namen, index)));
		}
		SortedMap<Integer, String> formelProZeile = new TreeMap<>();
		for (Aenderung aenderung : TeamNummernPlan.berechne(zellen, strategie, () -> UUID.randomUUID().toString())) {
			formelProZeile.put(ersteZeile + aenderung.index(), aenderung.formel());
		}
		schreibeNrFormeln(helper, xSheet, nrSpalte, formelProZeile);
	}

	/**
	 * Schreibt Nr-Formeln je Zeile, jede zusammenhängende Folge von Zeilen in einem Aufruf. Andere Zellen der Spalte
	 * bleiben unberührt.
	 *
	 * @param formelProZeile Formel je 0-basierter Zeile
	 */
	public static void schreibeNrFormeln(SheetHelper helper, XSpreadsheet xSheet, int nrSpalte,
			SortedMap<Integer, String> formelProZeile) {
		if (formelProZeile.isEmpty()) {
			return;
		}
		List<Integer> zeilen = new ArrayList<>(formelProZeile.keySet());
		BlattschutzManager.get().schreibeEntsperrt(xSheet, () -> {
			int start = 0;
			while (start < zeilen.size()) {
				int ende = start;
				while (ende + 1 < zeilen.size() && zeilen.get(ende + 1) == zeilen.get(ende) + 1) {
					ende++;
				}
				String[][] block = new String[ende - start + 1][1];
				for (int i = start; i <= ende; i++) {
					block[i - start][0] = formelProZeile.get(zeilen.get(i));
				}
				helper.setFormulaArrayInRange(xSheet,
						RangePosition.from(nrSpalte, zeilen.get(start), nrSpalte, zeilen.get(ende)), block);
				start = ende + 1;
			}
		});
	}

	private static String formel(String[][] formeln, int index) {
		return index < formeln.length && formeln[index].length > 0 ? StringUtils.defaultString(formeln[index][0]) : "";
	}

	private static int nr(RangeData nummern, int index) {
		if (index >= nummern.size() || nummern.get(index).isEmpty()) {
			return -1;
		}
		return nummern.get(index).get(0).getIntVal(-1);
	}

	private static boolean hatNamen(RangeData namen, int index) {
		return index < namen.size() && !namen.get(index).isEmpty()
				&& StringUtils.isNotBlank(namen.get(index).get(0).getStringVal());
	}
}
