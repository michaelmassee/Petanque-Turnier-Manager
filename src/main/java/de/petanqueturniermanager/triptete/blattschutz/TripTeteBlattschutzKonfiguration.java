package de.petanqueturniermanager.triptete.blattschutz;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.CellContentType;
import com.sun.star.table.XCell;

import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.cellstyle.CellStyleHelper;
import de.petanqueturniermanager.helper.cellstyle.EditierbareZelleHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.EditierbareZelleHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.blattschutz.IBlattschutzKonfiguration;
import de.petanqueturniermanager.helper.sheet.blattschutz.SheetSchutzInfo;
import de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet;

/**
 * Blattschutz-Konfiguration für Trip-Tête.
 * <p>
 * Editierbare Bereiche:
 * <ul>
 *   <li><b>Meldeliste:</b> Spieler-Name-Spalte (B)</li>
 *   <li><b>Spielplan:</b> Bahnen + Triplette-/Doublette-/Tête-Ergebnis-Spalten</li>
 *   <li><b>Rangliste:</b> vollständig gesperrt</li>
 * </ul>
 */
public final class TripTeteBlattschutzKonfiguration implements IBlattschutzKonfiguration, MeldeListeKonstanten {

	private static final Logger logger = LogManager.getLogger(TripTeteBlattschutzKonfiguration.class);
	private static final TripTeteBlattschutzKonfiguration INSTANCE = new TripTeteBlattschutzKonfiguration();

	private TripTeteBlattschutzKonfiguration() {
	}

	public static TripTeteBlattschutzKonfiguration get() {
		return INSTANCE;
	}

	@Override
	public void zelleStylesAktualisieren(WorkingSpreadsheet ws) {
		var doc = ws.getWorkingSpreadsheetDocument();
		CellStyleHelper.from(doc,
				new EditierbareZelleHintergrundFarbeGeradeStyle(EditierbaresZelleFormatHelper.EDITIERBAR_GERADE_FARBE))
				.apply();
		CellStyleHelper.from(doc,
				new EditierbareZelleHintergrundFarbeUnGeradeStyle(EditierbaresZelleFormatHelper.EDITIERBAR_UNGERADE_FARBE))
				.apply();
	}

	@Override
	public List<SheetSchutzInfo> berechneSchutzInfos(WorkingSpreadsheet ws) {
		var xDoc = ws.getWorkingSpreadsheetDocument();
		var infos = new ArrayList<SheetSchutzInfo>();

		sammleMeldelisteSchutzInfo(xDoc, infos);
		sammleSpielplanSchutzInfo(xDoc, infos);
		sammleVollGesperrteSheets(xDoc, infos);

		return infos;
	}

	private void sammleMeldelisteSchutzInfo(XSpreadsheetDocument xDoc, List<SheetSchutzInfo> infos) {
		SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.SCHLUESSEL_TRIPTETE_MELDELISTE).ifPresent(sheet ->
				infos.add(SheetSchutzInfo.mitEditierbarenBereichen(sheet, List.of(
						RangePosition.from(SPIELER_NR_SPALTE + 1, ERSTE_DATEN_ZEILE,
								SPIELER_NR_SPALTE + 1, MeldungenSpalte.MAX_ANZ_MELDUNGEN)))));
	}

	private void sammleSpielplanSchutzInfo(XSpreadsheetDocument xDoc, List<SheetSchutzInfo> infos) {
		SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.SCHLUESSEL_TRIPTETE_SPIELPLAN).ifPresent(sheet -> {
			int ersteDatenZeile = TripTeteSpielPlanSheet.ERSTE_DATEN_ZEILE;
			int letzteZeile = ermittleLetzteSpielplanZeile(sheet);
			var bereiche = List.of(
					RangePosition.from(TripTeteSpielPlanSheet.BAHN_TRI_SPALTE, ersteDatenZeile,
							TripTeteSpielPlanSheet.BAHN_TETE_SPALTE, letzteZeile),
					RangePosition.from(TripTeteSpielPlanSheet.TRI_A_SPALTE, ersteDatenZeile,
							TripTeteSpielPlanSheet.TETE_B_SPALTE, letzteZeile));
			infos.add(SheetSchutzInfo.mitEditierbarenBereichen(sheet, bereiche));
		});
	}

	private int ermittleLetzteSpielplanZeile(XSpreadsheet sheet) {
		int letzteZeile = TripTeteSpielPlanSheet.ERSTE_DATEN_ZEILE;
		try {
			for (int zeile = TripTeteSpielPlanSheet.ERSTE_DATEN_ZEILE;
					zeile <= MeldungenSpalte.MAX_ANZ_MELDUNGEN; zeile++) {
				XCell xCell = sheet.getCellByPosition(TripTeteSpielPlanSheet.SPIEL_NR_SPALTE, zeile);
				if (CellContentType.EMPTY.equals(xCell.getType())) {
					break;
				}
				letzteZeile = zeile;
			}
		} catch (Exception e) {
			logger.warn("Letzte Spielplan-Zeile konnte nicht ermittelt werden: {}", e.getMessage(), e);
		}
		return letzteZeile;
	}

	private void sammleVollGesperrteSheets(XSpreadsheetDocument xDoc, List<SheetSchutzInfo> infos) {
		SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.SCHLUESSEL_TRIPTETE_RANGLISTE)
				.ifPresent(sheet -> infos.add(SheetSchutzInfo.vollGesperrt(sheet)));
	}
}
