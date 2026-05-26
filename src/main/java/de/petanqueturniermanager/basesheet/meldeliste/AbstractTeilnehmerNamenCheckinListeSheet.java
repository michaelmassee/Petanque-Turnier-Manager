/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerNamenLeser.TeilnehmerNamen;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Checkin-Listen-Basis für Systeme, deren Meldeliste das spaltenbasierte Teamnamen-/Spieler-Layout
 * verwendet (Schweizer, Poule, Maastrichter, KO, Kaskade, FormuleX, JGJ, Supermelee).
 * <p>
 * Die Namen werden als feste Werte aus der Meldeliste gelesen ({@link TeilnehmerNamenLeser}) und
 * je nach Formation/Teamname-/Vereinsname-Option zusammengesetzt.
 * <p>
 * Das Spaltenlayout folgt der Teilnehmerliste: Ist {@link #istTeamnameAktiv()} {@code true}, wird
 * eine eigene Teamname-Spalte vor der (immer zusammengesetzten) Spieler-Spalte angezeigt.
 */
public abstract class AbstractTeilnehmerNamenCheckinListeSheet extends AbstractCheckinListeSheet {

	private static final int LESE_BIS_ZEILE_OFFSET = 999;

	protected AbstractTeilnehmerNamenCheckinListeSheet(WorkingSpreadsheet workingSpreadsheet,
			TurnierSystem turnierSystem, String logPrefix) {
		super(workingSpreadsheet, turnierSystem, logPrefix);
	}

	/** Liest Spielernamen, Teamnamen und Sortierschlüssel der Meldeliste in einem Durchgang. */
	private TeilnehmerNamen leseTeilnehmerNamen() throws GenerateException {
		return TeilnehmerNamenLeser.from(getMeldelisteSheet(), getMeldelisteErsteDatenZeile(),
				getFormation(), istTeamnameAktiv(), istVereinsnameAktiv()).lesen();
	}

	@Override
	protected Map<Integer, String> namenNachNummer() throws GenerateException {
		return leseTeilnehmerNamen().spielerNamen();
	}

	@Override
	protected boolean teamSpalteAktiv() {
		return istTeamnameAktiv();
	}

	@Override
	protected Map<Integer, String> teamnamenNachNummer() throws GenerateException {
		return leseTeilnehmerNamen().teamnamen();
	}

	@Override
	protected List<Integer> ladeNummern() throws GenerateException {
		List<Integer> nummern = new ArrayList<>();
		ISheet meldelisteSheet = getMeldelisteSheet();
		XSpreadsheet xSheet = meldelisteSheet.getXSpreadSheet();
		if (xSheet == null) {
			return nummern;
		}
		int ersteZeile = getMeldelisteErsteDatenZeile();
		RangeData data = RangeHelper.from(xSheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				RangePosition.from(0, ersteZeile, 0, ersteZeile + LESE_BIS_ZEILE_OFFSET)).getDataFromRange();
		for (RowData row : data) {
			if (row.isEmpty()) {
				break;
			}
			int nr = row.get(0).getIntVal(0);
			if (nr <= 0) {
				break;
			}
			nummern.add(nr);
		}
		return nummern;
	}

	@Override
	protected Map<Integer, SortSchluessel> ladeSortDaten() throws GenerateException {
		TeilnehmerNamen namen = leseTeilnehmerNamen();
		Map<Integer, String> teamnamen = namen.teamnamen();
		Map<Integer, String> sortNamen = namen.sortNamen();
		Map<Integer, SortSchluessel> ergebnis = new java.util.HashMap<>();
		for (Integer nr : sortNamen.keySet()) {
			ergebnis.put(nr, new SortSchluessel(teamnamen.getOrDefault(nr, ""), sortNamen.getOrDefault(nr, "")));
		}
		return ergebnis;
	}

	@Override
	protected boolean teamnameVerfuegbar() {
		return istTeamnameAktiv();
	}

	// ── system-spezifische Hooks ─────────────────────────────────────────────

	/** Die zugrunde liegende Meldeliste als {@link ISheet} (Quelle für Namen und Nummern). */
	protected abstract ISheet getMeldelisteSheet();

	/** Erste Datenzeile der Meldeliste (0-basiert). */
	protected abstract int getMeldelisteErsteDatenZeile();

	/** Formation der Meldeliste (Anzahl Spieler je Team). */
	protected abstract Formation getFormation();

	/** Ob in der Meldeliste eine freie Teamname-Spalte aktiv ist. */
	protected abstract boolean istTeamnameAktiv();

	/** Ob in der Meldeliste eine Vereinsname-Spalte aktiv ist. */
	protected abstract boolean istVereinsnameAktiv();
}
