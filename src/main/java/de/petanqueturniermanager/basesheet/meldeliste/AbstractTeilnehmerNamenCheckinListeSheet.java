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
 * verwendet (Schweizer, Poule, Maastrichter, KO, Kaskade, FormuleX).
 * <p>
 * Die Namen werden – im Gegensatz zur Formel-Variante in {@link AbstractCheckinListeSheet} –
 * als feste Werte aus der Meldeliste gelesen ({@link TeilnehmerNamenLeser}).
 * Je nach {@link #istProTeam()} wird der freie Teamname oder die Spielernamen angezeigt.
 */
public abstract class AbstractTeilnehmerNamenCheckinListeSheet extends AbstractCheckinListeSheet {

	private static final int LESE_BIS_ZEILE_OFFSET = 999;

	protected AbstractTeilnehmerNamenCheckinListeSheet(WorkingSpreadsheet workingSpreadsheet,
			TurnierSystem turnierSystem, String logPrefix) {
		super(workingSpreadsheet, turnierSystem, logPrefix);
	}

	@Override
	protected boolean nameAlsFormel() {
		return false;
	}

	@Override
	protected Map<Integer, String> namenNachNummer() throws GenerateException {
		boolean teamnameAktiv = istTeamnameAktiv();
		TeilnehmerNamen namen = TeilnehmerNamenLeser.from(getMeldelisteSheet(), getMeldelisteErsteDatenZeile(),
				getFormation(), teamnameAktiv, istVereinsnameAktiv()).lesen();
		Map<Integer, String> spielerNamen = namen.spielerNamen();
		Map<Integer, String> teamnamen = namen.teamnamen();

		Map<Integer, String> ergebnis = new java.util.HashMap<>();
		for (Integer nr : spielerNamen.keySet()) {
			String spieler = spielerNamen.getOrDefault(nr, "");
			String team = teamnamen.getOrDefault(nr, "");
			ergebnis.put(nr, waehleAnzeigeName(istProTeam(), team, spieler));
		}
		return ergebnis;
	}

	private static String waehleAnzeigeName(boolean proTeam, String teamname, String spielerName) {
		String bevorzugt = proTeam ? teamname : spielerName;
		String alternative = proTeam ? spielerName : teamname;
		if (bevorzugt != null && !bevorzugt.isBlank()) {
			return bevorzugt;
		}
		return alternative != null ? alternative : "";
	}

	@Override
	protected List<Integer> ladeSortierteNummern() throws GenerateException {
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

	/** {@code true}: Teamname als Anzeigename bevorzugen; {@code false}: Spielernamen bevorzugen. */
	protected abstract boolean istProTeam();
}
