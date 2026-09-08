/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Liest das Melee-Anmeldung-Sheet blockweise ein.
 * <p>
 * Bewusst ein eigenständiger, rein lesender Helper (kein {@code SheetRunner}): sowohl die
 * Checkin-Liste als auch der Live-Push der Turnier-Startseite brauchen die Daten, ohne einen
 * Sheet-Aufbau auszulösen. Existiert das Sheet nicht, wird eine leere Liste geliefert.
 */
public final class MeleeAnmeldungLeser implements MeleeAnmeldungKonstanten {

	private static final Logger logger = LogManager.getLogger(MeleeAnmeldungLeser.class);

	private MeleeAnmeldungLeser() {
	}

	/**
	 * Liest alle befüllten Zeilen des Melee-Anmeldung-Sheets.
	 *
	 * @param ws                 aktuelles Dokument
	 * @param metadatenSchluessel Named-Range-Schlüssel des System-Sheets
	 * @return alle Zeilen mit Namen, in Sheet-Reihenfolge; leer wenn kein Sheet vorhanden ist
	 */
	public static List<MeleeAnmeldungZeile> lesen(WorkingSpreadsheet ws, String metadatenSchluessel) {
		XSpreadsheet xSheet = findeSheet(ws, metadatenSchluessel);
		if (xSheet == null) {
			return List.of();
		}
		return lesen(ws, xSheet);
	}

	/**
	 * Liest alle befüllten Zeilen aus einem bereits bekannten Melee-Anmeldung-Sheet.
	 */
	public static List<MeleeAnmeldungZeile> lesen(WorkingSpreadsheet ws, XSpreadsheet xSheet) {
		List<MeleeAnmeldungZeile> zeilen = new ArrayList<>();
		try {
			RangePosition bereich = RangePosition.from(SPALTE_NR, ERSTE_DATEN_ZEILE, LETZTE_SPALTE,
					ERSTE_DATEN_ZEILE + MAX_ANZ_ANMELDUNGEN - 1);
			RangeData data = RangeHelper.from(xSheet, ws.getWorkingSpreadsheetDocument(), bereich).getDataFromRange();
			int zeilenIdx = ERSTE_DATEN_ZEILE;
			for (RowData row : data) {
				MeleeAnmeldungZeile zeile = ausRowData(zeilenIdx++, row);
				if (zeile.hatNamen()) {
					zeilen.add(zeile);
				}
			}
		} catch (RuntimeException e) {
			logger.warn("Melee-Anmeldungen konnten nicht gelesen werden", e);
			return List.of();
		}
		return zeilen;
	}

	/**
	 * Sucht das Melee-Anmeldung-Sheet über die Named-Range-Metadaten, ersatzweise über den
	 * lokalisierten Tabellennamen.
	 *
	 * @return das Sheet oder {@code null} wenn es (noch) nicht existiert
	 */
	public static XSpreadsheet findeSheet(WorkingSpreadsheet ws, String metadatenSchluessel) {
		if (ws == null) {
			return null;
		}
		XSpreadsheetDocument xDoc = ws.getWorkingSpreadsheetDocument();
		if (xDoc == null) {
			return null;
		}
		return SheetMetadataHelper.findeSheet(xDoc, metadatenSchluessel)
				.orElseGet(() -> new SheetHelper(ws).findByName(SheetNamen.meleeAnmeldung()));
	}

	private static MeleeAnmeldungZeile ausRowData(int zeilenIdx, RowData row) {
		return new MeleeAnmeldungZeile(zeilenIdx,
				row.get(SPALTE_NR).getIntVal(0),
				row.get(SPALTE_VORNAME).getStringVal(),
				row.get(SPALTE_NACHNAME).getStringVal(),
				Math.max(0, row.get(SPALTE_SETZPOSITION).getIntVal(0)),
				istMarkiert(row.get(SPALTE_EINGECHECKT).getStringVal()),
				istMarkiert(row.get(SPALTE_UEBERNOMMEN).getStringVal()));
	}

	/**
	 * Eine Markierungs-Spalte gilt als gesetzt, sobald sie irgendeinen nicht-leeren Inhalt hat –
	 * der Anwender soll auch ein „x", „ja" oder einen Haken eintragen dürfen.
	 */
	private static boolean istMarkiert(String wert) {
		return StringUtils.isNotBlank(wert);
	}
}
