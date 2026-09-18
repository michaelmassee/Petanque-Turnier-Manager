/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync.sheet;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * "Meldungen"-Sheet einer Online-Turnier-Verbindung: Mapping-Tabelle lokale Team-/Spieler-Nr ↔
 * PTM-Online-Registrierungs-ID ↔ Name. Turniersystem-übergreifend nutzbar; bei Supermelee (mehrere
 * Spieltage) gibt es eine Instanz pro Spieltag (siehe {@code spieltagNr}), sonst genau eine pro
 * Dokument.
 */
public class OnlineTurnierMeldungenSheet extends SheetRunner implements ISheet {

	private static final int SPALTE_SPIELER_NR = 0;
	private static final int SPALTE_ONLINE_ID = 1;
	private static final int SPALTE_NAME = 2;

	private static final int ZEILE_TITEL = 0;
	private static final int ZEILE_HEADER = 1;
	private static final int ERSTE_DATEN_ZEILE = 2;
	private static final int MAX_ZEILEN = MeldungenSpalte.MAX_ANZ_MELDUNGEN;

	private final Integer spieltagNr;

	public OnlineTurnierMeldungenSheet(WorkingSpreadsheet workingSpreadsheet, TurnierSystem turnierSystem, Integer spieltagNrOderNull) {
		super(workingSpreadsheet, turnierSystem, "PtmOnlineMeldungen");
		this.spieltagNr = spieltagNrOderNull;
	}

	@Override
	protected de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet getKonfigurationSheet() {
		return null;
	}

	private String sheetName() {
		return spieltagNr == null ? SheetNamen.ptmOnlineMeldungen() : SheetNamen.ptmOnlineMeldungen(spieltagNr);
	}

	private String metadatenSchluessel() {
		return spieltagNr == null ? SheetMetadataHelper.SCHLUESSEL_PTM_ONLINE_MELDUNGEN
				: SheetMetadataHelper.schluesselPtmOnlineMeldungen(spieltagNr);
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				metadatenSchluessel(), sheetName());
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	/** Legt das Sheet (falls nötig) an und schreibt die Spaltenüberschriften. */
	public void sicherstellen() throws GenerateException, InterruptedException {
		start();
		join();
		if (isLetzterLaufFehlgeschlagen()) {
			throw new GenerateException(I18n.get("ptmonline.sheet.fehler.anlegen"));
		}
	}

	@Override
	protected void doRun() throws GenerateException {
		NewSheet.from(this, sheetName(), metadatenSchluessel())
				.pos(DefaultSheetPos.SUPERMELEE_WORK).useIfExist().create();
		schreibeHeader();
	}

	private void schreibeHeader() throws GenerateException {
		schreibeZeile(ZEILE_TITEL, I18n.get("ptmonline.sheet.meldungen.titel"), "", "");
		schreibeZeile(ZEILE_HEADER, I18n.get("ptmonline.sheet.mapping.header.spielernr"),
				I18n.get("ptmonline.sheet.mapping.header.onlineid"), I18n.get("ptmonline.sheet.mapping.header.name"));
	}

	public void addMapping(int teamNr, String onlineId, String name) throws GenerateException {
		Map<Integer, String> vorhanden = leseMapping();
		if (vorhanden.containsKey(teamNr)) {
			return;
		}
		int naechsteFreieZeile = ERSTE_DATEN_ZEILE + vorhanden.size();
		RangeData zeile = new RangeData();
		RowData row = zeile.addNewRow();
		row.newInt(teamNr);
		row.newString(onlineId);
		row.newString(StringUtils.defaultString(name));
		RangeHelper.from(this, zeile.getRangePosition(Position.from(SPALTE_SPIELER_NR, naechsteFreieZeile))).setDataInRange(zeile);
	}

	public java.util.Optional<String> getOnlineId(int teamNr) throws GenerateException {
		return java.util.Optional.ofNullable(leseMapping().get(teamNr));
	}

	public boolean istBereitsImportiert(String onlineId) throws GenerateException {
		return leseMapping().containsValue(onlineId);
	}

	public Map<Integer, String> getAlleMappings() throws GenerateException {
		return Map.copyOf(leseMapping());
	}

	private Map<Integer, String> leseMapping() throws GenerateException {
		Map<Integer, String> ergebnis = new LinkedHashMap<>();
		RangePosition bereich = RangePosition.from(SPALTE_SPIELER_NR, ERSTE_DATEN_ZEILE, SPALTE_NAME, ERSTE_DATEN_ZEILE + MAX_ZEILEN);
		RangeData daten = RangeHelper.from(this, bereich).getDataFromRange();
		for (RowData zeile : daten) {
			if (zeile.isEmpty()) {
				continue;
			}
			int spielerNr = zeile.get(SPALTE_SPIELER_NR).getIntVal(-1);
			String onlineId = zeile.size() > SPALTE_ONLINE_ID ? zeile.get(SPALTE_ONLINE_ID).getStringVal() : null;
			if (spielerNr > 0 && StringUtils.isNotBlank(onlineId)) {
				ergebnis.put(spielerNr, onlineId);
			}
		}
		return ergebnis;
	}

	private void schreibeZeile(int zeile, String... werte) throws GenerateException {
		RangeData daten = new RangeData();
		RowData row = daten.addNewRow();
		for (String wert : werte) {
			row.newString(wert);
		}
		RangeHelper.from(this, daten.getRangePosition(Position.from(SPALTE_SPIELER_NR, zeile))).setDataInRange(daten);
	}
}
