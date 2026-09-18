/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync.sheet;

import java.time.Instant;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;

/**
 * "Turnierinformationen"-Sheet einer Online-Turnier-Verbindung: Online-Turnier-ID, Name, Datum,
 * Typ, Online-Status, Verbindungsstatus und letzter Sync-Zeitpunkt. Turniersystem-übergreifend
 * nutzbar; bei Supermelee (mehrere Spieltage) gibt es eine Instanz pro Spieltag (siehe
 * {@code spieltagNr}), sonst genau eine pro Dokument.
 */
public class OnlineTurnierInfoSheet extends SheetRunner implements ISheet {

	private static final int SPALTE_LABEL = 0;
	private static final int SPALTE_WERT = 1;

	private static final int ZEILE_TITEL = 0;
	private static final int ZEILE_ONLINE_ID = 2;
	private static final int ZEILE_NAME = 3;
	private static final int ZEILE_DATUM = 4;
	private static final int ZEILE_TYP = 5;
	private static final int ZEILE_ONLINE_STATUS = 6;
	private static final int ZEILE_VERBINDUNGSSTATUS = 7;
	private static final int ZEILE_LETZTER_SYNC = 8;

	private final Integer spieltagNr;

	public OnlineTurnierInfoSheet(WorkingSpreadsheet workingSpreadsheet, TurnierSystem turnierSystem, Integer spieltagNrOderNull) {
		super(workingSpreadsheet, turnierSystem, "PtmOnlineInfo");
		this.spieltagNr = spieltagNrOderNull;
	}

	@Override
	protected de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet getKonfigurationSheet() {
		return null;
	}

	private String sheetName() {
		return spieltagNr == null ? SheetNamen.ptmOnlineInfo() : SheetNamen.ptmOnlineInfo(spieltagNr);
	}

	private String metadatenSchluessel() {
		return spieltagNr == null ? SheetMetadataHelper.SCHLUESSEL_PTM_ONLINE_INFO
				: SheetMetadataHelper.schluesselPtmOnlineInfo(spieltagNr);
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

	/** Legt das Sheet (falls nötig) an, schreibt die Labels und die Verbindungsdaten in einem Zug. */
	public void verbinden(OnlineTournamentDto turnier) throws GenerateException, InterruptedException {
		start();
		join();
		if (isLetzterLaufFehlgeschlagen()) {
			throw new GenerateException(I18n.get("ptmonline.sheet.fehler.anlegen"));
		}
		schreibeWert(ZEILE_ONLINE_ID, turnier.id);
		schreibeWert(ZEILE_NAME, turnier.name);
		schreibeWert(ZEILE_DATUM, StringUtils.defaultString(turnier.date));
		schreibeWert(ZEILE_TYP, StringUtils.defaultString(turnier.type)
				+ (StringUtils.isBlank(turnier.registrationType) ? "" : " / " + turnier.registrationType));
		schreibeWert(ZEILE_ONLINE_STATUS, StringUtils.defaultString(turnier.status));
		schreibeWert(ZEILE_VERBINDUNGSSTATUS, I18n.get("ptmonline.sheet.status.verbunden"));
	}

	@Override
	protected void doRun() throws GenerateException {
		NewSheet.from(this, sheetName(), metadatenSchluessel())
				.pos(DefaultSheetPos.SUPERMELEE_WORK).useIfExist().create();
		schreibeLabels();
	}

	private void schreibeLabels() throws GenerateException {
		RangeData labels = new RangeData();
		labels.addNewRow().newString(I18n.get("ptmonline.sheet.titel"));
		labels.addNewRow().newString("");
		labels.addNewRow().newString(I18n.get("ptmonline.sheet.label.onlineid"));
		labels.addNewRow().newString(I18n.get("ptmonline.sheet.label.name"));
		labels.addNewRow().newString(I18n.get("ptmonline.sheet.label.datum"));
		labels.addNewRow().newString(I18n.get("ptmonline.sheet.label.typ"));
		labels.addNewRow().newString(I18n.get("ptmonline.sheet.label.online.status"));
		labels.addNewRow().newString(I18n.get("ptmonline.sheet.label.status"));
		labels.addNewRow().newString(I18n.get("ptmonline.sheet.label.letzter.sync"));
		RangeHelper.from(this, labels.getRangePosition(Position.from(SPALTE_LABEL, ZEILE_TITEL))).setDataInRange(labels);
	}

	public Optional<String> getTournamentId() throws GenerateException {
		String wert = leseWert(ZEILE_ONLINE_ID);
		return wert.isBlank() ? Optional.empty() : Optional.of(wert);
	}

	public Optional<Instant> getLastSync() throws GenerateException {
		String wert = leseWert(ZEILE_LETZTER_SYNC);
		if (wert.isBlank()) {
			return Optional.empty();
		}
		try {
			return Optional.of(Instant.parse(wert));
		} catch (java.time.format.DateTimeParseException e) {
			return Optional.empty();
		}
	}

	public void setLastSync(Instant zeitpunkt) throws GenerateException {
		schreibeWert(ZEILE_LETZTER_SYNC, zeitpunkt.toString());
	}

	private String leseWert(int zeile) throws GenerateException {
		return getSheetHelper().getTextFromCell(getXSpreadSheet(), Position.from(SPALTE_WERT, zeile));
	}

	private void schreibeWert(int zeile, String wert) throws GenerateException {
		var zellWert = StringCellValue.from(getXSpreadSheet(), Position.from(SPALTE_WERT, zeile), StringUtils.defaultString(wert));
		getSheetHelper().setStringValueInCell(zellWert);
	}
}
