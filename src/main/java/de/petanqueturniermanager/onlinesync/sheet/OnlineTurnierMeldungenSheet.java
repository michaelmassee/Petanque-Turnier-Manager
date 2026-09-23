/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync.sheet;

import org.apache.commons.lang3.StringUtils;

import com.sun.star.container.XNamed;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
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
 * "Meldungen"-Sheet einer Online-Turnier-Verbindung: Mapping-Tabelle lokale, stabile UUID ↔
 * PTM-Online-Registrierungs-ID. Turniersystem-übergreifend nutzbar; bei Supermelee (mehrere
 * Spieltage) gibt es eine Instanz pro Spieltag (siehe {@code spieltagNr}), sonst genau eine pro
 * Dokument.
 */
public class OnlineTurnierMeldungenSheet extends SheetRunner implements ISheet {

	private static final int SPALTE_ANZEIGE_NR = 0;
	private static final int SPALTE_ONLINE_ID = 1;
	private static final int SPALTE_LOKALE_UUID = 2;
	private static final int SPALTE_EXECUTION_REVISION = 3;
	private static final int SPALTE_LOKALE_BEZEICHNUNG = 4;
	private static final int SPALTE_ONLINE_BEZEICHNUNG = 5;
	private static final int SPALTE_ONLINE_STATUS = 6;
	private static final int SPALTE_ONLINE_TARIFE = 7;
	private static final int SPALTE_ONLINE_FRAGEN = 8;
	private static final int SPALTE_ONLINE_SNAPSHOT = 9;

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
		schreibeZeile(ZEILE_TITEL, I18n.get("ptmonline.sheet.meldungen.titel"), "", "", "", "", "", "", "", "", "");
		schreibeZeile(ZEILE_HEADER, I18n.get("ptmonline.sheet.mapping.header.spielernr"),
				I18n.get("ptmonline.sheet.mapping.header.onlineid"), I18n.get("ptmonline.sheet.mapping.header.lokaleuuid"), "",
				I18n.get("ptmonline.sheet.mapping.header.lokalebezeichnung"),
				I18n.get("ptmonline.sheet.mapping.header.onlinebezeichnung"),
				I18n.get("ptmonline.sheet.mapping.header.onlinestatus"),
				I18n.get("ptmonline.sheet.mapping.header.onlinetarife"),
				I18n.get("ptmonline.sheet.mapping.header.onlinefragen"),
				I18n.get("ptmonline.sheet.mapping.header.onlinesnapshot"));
		getSheetHelper().setColumnProperties(getXSpreadSheet(), SPALTE_LOKALE_UUID, ColumnProperties.from().isVisible(true));
		getSheetHelper().setColumnProperties(getXSpreadSheet(), SPALTE_EXECUTION_REVISION, ColumnProperties.from().isVisible(false));
	}

	public void addMapping(String lokaleUuid, String onlineId, String nummerFormel, int executionRevision,
			String lokaleBezeichnung, String onlineBezeichnung, String onlineStatus) throws GenerateException {
		if (getOnlineId(lokaleUuid).isPresent()) {
			return;
		}
		int naechsteFreieZeile = naechsteFreieZeile();
		RangeData zeile = new RangeData();
		RowData row = zeile.addNewRow();
		row.newEmpty();
		row.newString(onlineId);
		row.newString(lokaleUuid);
		row.newInt(Math.max(1, executionRevision));
		row.newString(StringUtils.defaultString(lokaleBezeichnung));
		row.newString(StringUtils.defaultString(onlineBezeichnung));
		row.newString(StringUtils.defaultString(onlineStatus));
		RangeHelper.from(this, zeile.getRangePosition(Position.from(SPALTE_ANZEIGE_NR, naechsteFreieZeile))).setDataInRange(zeile);
		getSheetHelper().setFormulaInCell(StringCellValue.from(getXSpreadSheet(),
				Position.from(SPALTE_ANZEIGE_NR, naechsteFreieZeile), nummerFormel));
	}

	/** Entfernt dieses Sheet wieder aus dem Dokument (Gegenstueck zu {@link #sicherstellen}). */
	public void entfernen() throws GenerateException {
		java.util.Optional<XSpreadsheet> sheet = SheetMetadataHelper
				.findeSheet(getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), metadatenSchluessel());
		if (sheet.isEmpty()) {
			return;
		}
		XNamed named = Lo.qi(XNamed.class, sheet.get());
		if (named != null) {
			getSheetHelper().removeSheet(named.getName());
		}
	}

	public java.util.Optional<String> getOnlineId(String lokaleUuid) throws GenerateException {
		RangeData daten = leseDaten();
		for (RowData zeile : daten) {
			if (!zeile.isEmpty() && lokaleUuid.equals(zeile.get(SPALTE_LOKALE_UUID).getStringVal())) {
				return java.util.Optional.ofNullable(zeile.get(SPALTE_ONLINE_ID).getStringVal());
			}
		}
		return java.util.Optional.empty();
	}

	public boolean istBereitsImportiert(String onlineId) throws GenerateException {
		for (RowData zeile : leseDaten()) {
			if (!zeile.isEmpty() && onlineId.equals(zeile.get(SPALTE_ONLINE_ID).getStringVal())) {
				return true;
			}
		}
		return false;
	}

	public java.util.Optional<String> getLokaleUuid(String onlineId) throws GenerateException {
		for (RowData zeile : leseDaten()) {
			if (!zeile.isEmpty() && onlineId.equals(text(zeile, SPALTE_ONLINE_ID))) {
				return java.util.Optional.ofNullable(text(zeile, SPALTE_LOKALE_UUID)).filter(uuid -> !uuid.isBlank());
			}
		}
		return java.util.Optional.empty();
	}

	/** Unveränderter JSON-Snapshot der zuletzt übernommenen Online-Anmeldung dieser lokalen Meldung. */
	public java.util.Optional<String> getOnlineSnapshot(String lokaleUuid) throws GenerateException {
		for (RowData zeile : leseDaten()) {
			if (lokaleUuid.equals(text(zeile, SPALTE_LOKALE_UUID))) {
				return java.util.Optional.ofNullable(text(zeile, SPALTE_ONLINE_SNAPSHOT)).filter(json -> !json.isBlank());
			}
		}
		return java.util.Optional.empty();
	}

	/**
	 * Ordnet eine bereits zugeordnete lokale Meldung einer anderen Online-Anmeldung zu (z.&nbsp;B.
	 * Neuanmeldung nach Online-Storno); die Zeile samt Anzeige-Formel bleibt erhalten.
	 */
	public void ersetzeOnlineId(String lokaleUuid, String onlineId, int executionRevision) throws GenerateException {
		RangeData daten = leseDaten();
		for (int i = 0; i < daten.size(); i++) {
			if (lokaleUuid.equals(text(daten.get(i), SPALTE_LOKALE_UUID))) {
				RangeData werte = new RangeData();
				RowData row = werte.addNewRow();
				row.newString(onlineId);
				row.newString(lokaleUuid);
				row.newInt(Math.max(1, executionRevision));
				RangeHelper.from(this, werte.getRangePosition(Position.from(SPALTE_ONLINE_ID, ERSTE_DATEN_ZEILE + i)))
						.setDataInRange(werte);
				return;
			}
		}
		throw new GenerateException("Keine PTM-Online-Zuordnung für lokale Meldung " + lokaleUuid);
	}

	public int getExecutionRevision(String lokaleUuid) throws GenerateException {
		for (RowData zeile : leseDaten()) {
			if (lokaleUuid.equals(text(zeile, SPALTE_LOKALE_UUID))) {
				return zeile.size() > SPALTE_EXECUTION_REVISION
						? Math.max(1, zeile.get(SPALTE_EXECUTION_REVISION).getIntVal(1)) : 1;
			}
		}
		return 1;
	}

	public void setExecutionRevision(String lokaleUuid, int executionRevision) throws GenerateException {
		RangeData daten = leseDaten();
		for (int i = 0; i < daten.size(); i++) {
			if (lokaleUuid.equals(text(daten.get(i), SPALTE_LOKALE_UUID))) {
				getSheetHelper().setNumberValueInCell(de.petanqueturniermanager.helper.cellvalue.NumberCellValue.from(
						getXSpreadSheet(), Position.from(SPALTE_EXECUTION_REVISION, ERSTE_DATEN_ZEILE + i)).setValue(Math.max(1, executionRevision)));
				return;
			}
		}
	}

	public void setBezeichnungen(String lokaleUuid, String lokaleBezeichnung, String onlineBezeichnung,
			String onlineStatus) throws GenerateException {
		RangeData daten = leseDaten();
		for (int i = 0; i < daten.size(); i++) {
			if (lokaleUuid.equals(text(daten.get(i), SPALTE_LOKALE_UUID))) {
				int zeile = ERSTE_DATEN_ZEILE + i;
				getSheetHelper().setStringValueInCell(StringCellValue.from(getXSpreadSheet(),
						Position.from(SPALTE_LOKALE_BEZEICHNUNG, zeile), StringUtils.defaultString(lokaleBezeichnung)));
				getSheetHelper().setStringValueInCell(StringCellValue.from(getXSpreadSheet(),
						Position.from(SPALTE_ONLINE_BEZEICHNUNG, zeile), StringUtils.defaultString(onlineBezeichnung)));
				getSheetHelper().setStringValueInCell(StringCellValue.from(getXSpreadSheet(),
						Position.from(SPALTE_ONLINE_STATUS, zeile), StringUtils.defaultString(onlineStatus)));
				return;
			}
		}
	}

	public void setOnlineDetails(String lokaleUuid, String tarife, String fragen, String snapshot) throws GenerateException {
		RangeData daten = leseDaten();
		for (int i = 0; i < daten.size(); i++) {
			if (lokaleUuid.equals(text(daten.get(i), SPALTE_LOKALE_UUID))) {
				int zeile = ERSTE_DATEN_ZEILE + i;
				getSheetHelper().setStringValueInCell(StringCellValue.from(getXSpreadSheet(),
						Position.from(SPALTE_ONLINE_TARIFE, zeile), StringUtils.defaultString(tarife)));
				getSheetHelper().setStringValueInCell(StringCellValue.from(getXSpreadSheet(),
						Position.from(SPALTE_ONLINE_FRAGEN, zeile), StringUtils.defaultString(fragen)));
				getSheetHelper().setStringValueInCell(StringCellValue.from(getXSpreadSheet(),
						Position.from(SPALTE_ONLINE_SNAPSHOT, zeile), StringUtils.defaultString(snapshot)));
				return;
			}
		}
	}

	/**
	 * Überführt das frühere Nummern-Mapping einmalig. Es wird nur dann benutzt, wenn die erste
	 * Spalte noch keine UUID enthält; danach ist auch ein späteres Umnummerieren folgenlos.
	 */
	public void migriereLegacyTeamnummern(java.util.Map<Integer, String> uuidProTeamnummer) throws GenerateException {
		RangeData daten = leseDaten();
		if (daten.stream().anyMatch(zeile -> zeile.size() > SPALTE_LOKALE_UUID
				&& istUuid(zeile.get(SPALTE_LOKALE_UUID).getStringVal()))) {
			return;
		}
		boolean legacy = false;
		for (RowData zeile : daten) {
			String lokaleId = zeile.get(SPALTE_ANZEIGE_NR).getStringVal();
			if (!zeile.isEmpty() && (zeile.get(SPALTE_ANZEIGE_NR).getIntVal(-1) > 0
					|| (StringUtils.isNotBlank(lokaleId) && !istUuid(lokaleId)))) {
				legacy = true;
				break;
			}
		}
		if (!legacy) {
			return;
		}
		RangeData migriert = new RangeData();
		for (RowData alt : daten) {
			RowData neu = migriert.addNewRow();
			int teamNr = alt.get(SPALTE_ANZEIGE_NR).getIntVal(-1);
			String uuid = uuidProTeamnummer.get(teamNr);
			neu.newEmpty();
			neu.newString(alt.size() > SPALTE_ONLINE_ID ? alt.get(SPALTE_ONLINE_ID).getStringVal() : "");
			neu.newString(StringUtils.defaultString(uuid));
			neu.newInt(1);
			neu.newString("");
			neu.newString("");
			neu.newString("");
			neu.newString("");
			neu.newString("");
			neu.newString("");
		}
		RangeHelper.from(this, migriert.getRangePosition(Position.from(SPALTE_ANZEIGE_NR, ERSTE_DATEN_ZEILE))).setDataInRange(migriert);
		schreibeHeader();
	}

	public void aktualisiereAnzeigeFormeln(java.util.Map<String, String> formelnProUuid) throws GenerateException {
		RangeData daten = leseDaten();
		for (int i = 0; i < daten.size(); i++) {
			RowData zeile = daten.get(i);
			if (zeile.isEmpty() || zeile.size() <= SPALTE_LOKALE_UUID) {
				continue;
			}
			String formel = formelnProUuid.get(zeile.get(SPALTE_LOKALE_UUID).getStringVal());
			if (formel != null) {
				getSheetHelper().setFormulaInCell(StringCellValue.from(getXSpreadSheet(),
						Position.from(SPALTE_ANZEIGE_NR, ERSTE_DATEN_ZEILE + i), formel));
			}
		}
	}

	public void leeren() throws GenerateException {
		RangeHelper.from(this, RangePosition.from(SPALTE_ANZEIGE_NR, ERSTE_DATEN_ZEILE,
				SPALTE_ONLINE_SNAPSHOT, ERSTE_DATEN_ZEILE + MAX_ZEILEN)).clearRange();
	}

	private RangeData leseDaten() throws GenerateException {
		return RangeHelper.from(this, RangePosition.from(SPALTE_ANZEIGE_NR, ERSTE_DATEN_ZEILE,
				SPALTE_ONLINE_SNAPSHOT, ERSTE_DATEN_ZEILE + MAX_ZEILEN)).getDataFromRange();
	}

	private int naechsteFreieZeile() throws GenerateException {
		RangeData daten = leseDaten();
		for (int i = 0; i < daten.size(); i++) {
			if (istLeereMappingZeile(daten.get(i))) {
				return ERSTE_DATEN_ZEILE + i;
			}
		}
		throw new GenerateException("PTM-Online-Mapping ist voll");
	}

	/** RowData enthält auch für leere Calc-Zellen CellData-Objekte; List.isEmpty() ist daher ungeeignet. */
	private static boolean istLeereMappingZeile(RowData zeile) {
		return text(zeile, SPALTE_ONLINE_ID).isBlank() && text(zeile, SPALTE_LOKALE_UUID).isBlank();
	}

	private static String text(RowData zeile, int spalte) {
		return zeile.size() > spalte ? StringUtils.defaultString(zeile.get(spalte).getStringVal()) : "";
	}

	private static boolean istUuid(String wert) {
		try {
			java.util.UUID.fromString(wert);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private void schreibeZeile(int zeile, String... werte) throws GenerateException {
		RangeData daten = new RangeData();
		RowData row = daten.addNewRow();
		for (String wert : werte) {
			row.newString(wert);
		}
		RangeHelper.from(this, daten.getRangePosition(Position.from(SPALTE_ANZEIGE_NR, zeile))).setDataInRange(daten);
	}
}
