package de.petanqueturniermanager.schweizer.spielrunde;

import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.algorithmen.SchweizerTeamErgebnis;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

/**
 * Erstellung 26.03.2024 / Michael Massee
 */

public class SchweizerSpielrundeSheetNaechste extends SchweizerAbstractSpielrundeSheet {

	public SchweizerSpielrundeSheetNaechste(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	protected SchweizerSpielrundeSheetNaechste(WorkingSpreadsheet workingSpreadsheet,
			de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem ts, String sheetBaseName) {
		super(workingSpreadsheet, ts, sheetBaseName);
	}

	@Override
	public void doRun() throws GenerateException {
		getxCalculatable().enableAutomaticCalculation(false); // speed up
		processBoxinfo("processbox.naechste.spielrunde", getSpielRundeNr().getNr());
		naechsteSpielrundeEinfuegen();
	}

	public boolean naechsteSpielrundeEinfuegen() throws GenerateException {
		SpielRundeNr aktuelleSpielrunde = getKonfigurationSheet().getAktiveSpielRunde();
		setSpielRundeNrInSheet(aktuelleSpielrunde);
		getMeldeListe().upDateSheet();
		TeamMeldungen aktiveMeldungen = getMeldeListe().getAktiveMeldungen();

		if (!canStart(aktiveMeldungen)) {
			return false;
		}

		// Ermitteln ob es eine aktuelle Runde gibt (= Sheet vorhanden)
		int neueSpielrunde = aktuelleSpielrunde.getNr();
		if (getSheetHelper().findByName(getSheetName(aktuelleSpielrunde)) != null) {
			// Aktuelle Runde vorhanden → prüfen ob alle Ergebnisse eingetragen sind
			if (!alleErgebnisseEingetragen(aktuelleSpielrunde)) {
				getSheetHelper().setActiveSheet(getXSpreadSheet());
				MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
						.caption(I18n.get("msg.caption.naechste.runde.nicht.moeglich"))
						.message(I18n.get("msg.text.naechste.runde.ergebnisse.fehlen", aktuelleSpielrunde.getNr()))
						.show();
				return false;
			}
			neueSpielrunde++;
		}

		// Konfiguration auf neue Rundennummer setzen (muss VOR neueSpielrunde() passieren,
		// da getSpielRundeNr() den Sheet-Namen daraus ableitet)
		getKonfigurationSheet().setAktiveSpielRunde(SpielRundeNr.from(neueSpielrunde));

		List<SchweizerTeamErgebnis> ergebnisse = gespieltenRundenEinlesen(aktiveMeldungen, 1, neueSpielrunde - 1);

		// Teams nach Rangliste sortieren (ab Runde 2)
		TeamMeldungen meldungenFuerAuslosung = (neueSpielrunde > 1)
				? sortierteTeamMeldungen(aktiveMeldungen, ergebnisse)
				: aktiveMeldungen;

		return neueSpielrunde(meldungenFuerAuslosung, SpielRundeNr.from(neueSpielrunde), ergebnisse);
	}

	/**
	 * Prüft ob in der angegebenen Runde alle Paarungen (ohne Freilos) ein Ergebnis haben.
	 * Ein Ergebnis gilt als eingetragen wenn erg_A + erg_B > 0.
	 */
	private boolean alleErgebnisseEingetragen(SpielRundeNr spielRundeNr) throws GenerateException {
		XSpreadsheet sheet = getSheetHelper().findByName(getSheetName(spielRundeNr));
		if (sheet == null) {
			return true;
		}
		RangePosition readRange = RangePosition.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE, ERG_TEAM_B_SPALTE,
				ERSTE_DATEN_ZEILE + 999);
		RangeData rowsData = RangeHelper
				.from(sheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), readRange).getDataFromRange();

		for (RowData row : rowsData) {
			if (row.size() < 2) {
				break;
			}
			int nrA = row.get(0).getIntVal(0);
			if (nrA <= 0) {
				// Name statt Zahl → keine Ergebnisprüfung möglich per int, String-Inhalt prüfen
				String valA = row.get(0).getStringVal();
				if (valA == null || valA.isEmpty()) {
					break; // Ende der Daten
				}
			}
			// Team B: leer = Freilos, Freilos braucht kein Ergebnis
			int nrB = row.get(1).getIntVal(0);
			if (nrB <= 0) {
				String valB = row.get(1).getStringVal();
				if (valB == null || valB.isEmpty()) {
					continue; // Freilos-Zeile
				}
			}
			// Ergebnisse prüfen
			int ergA = (row.size() > 2) ? row.get(2).getIntVal(0) : 0;
			int ergB = (row.size() > 3) ? row.get(3).getIntVal(0) : 0;
			if (ergA == 0 && ergB == 0) {
				return false; // Ergebnis fehlt
			}
		}
		return true;
	}

}
