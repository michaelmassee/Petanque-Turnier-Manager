/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync.sheet;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.FontWeight;
import com.sun.star.container.XNamed;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;

/**
 * „PTMOnline Sync“: das einzige Blatt einer PTM-Online-Verbindung. Oben die Verbindungsdaten, darunter die
 * Zuordnung lokale Meldung ↔ Online-Anmeldung. Sichtbar ist nur, was die Turnierleitung braucht (Nr, Name,
 * Status, Tarife, Fragen); Online-ID, lokale UUID, Revision und Roh-Status stehen in ausgeblendeten Spalten.
 * <p>
 * Bei Supermelee gibt es ein Blatt pro Spieltag ({@code spieltagNr}), sonst genau eines pro Dokument. Das Blatt
 * schreibt nur das Plugin – im Turnier-Modus ist es vollständig gesperrt (siehe {@link BlattschutzManager}).
 * Blockweise Writes über {@link RangeHelper} entsperren selbst; Einzelzell-Writes und Formatierung laufen über
 * {@link BlattschutzManager#schreibeEntsperrt}.
 */
public class PtmOnlineSyncSheet extends SheetRunner implements ISheet {

	private static final Logger LOGGER = LogManager.getLogger(PtmOnlineSyncSheet.class);

	private static final int SPALTE_LABEL = 0;
	private static final int SPALTE_WERT = 1;

	private static final int ZEILE_TITEL = 0;
	private static final int ZEILE_TURNIER_ID = 1;
	private static final int ZEILE_LETZTER_SYNC = 2;
	private static final int ZEILE_SYNC_DOKUMENT = 3;
	private static final int ZEILE_LEASE = 4;
	/** Sync aktiv/pausiert: Anzeige in Spalte B, maßgeblich ist der Marker in {@link #SPALTE_PAUSE_MARKER}. */
	private static final int ZEILE_SYNC_STATUS = 5;
	private static final int ZEILE_HEADER = 6;
	private static final int ERSTE_DATEN_ZEILE = 7;
	private static final int MAX_ZEILEN = MeldungenSpalte.MAX_ANZ_MELDUNGEN;
	private static final int LETZTE_DATEN_ZEILE = ERSTE_DATEN_ZEILE + MAX_ZEILEN - 1;

	private static final int SPALTE_NR = 0;
	private static final int SPALTE_NAME = 1;
	private static final int SPALTE_STATUS = 2;
	private static final int SPALTE_TARIFE = 3;
	private static final int SPALTE_FRAGEN = 4;
	private static final int SPALTE_ONLINE_ID = 5;
	private static final int SPALTE_LOKALE_UUID = 6;
	private static final int SPALTE_REVISION = 7;
	private static final int SPALTE_STATUS_ROH = 8;
	private static final int LETZTE_SICHTBARE_SPALTE = SPALTE_FRAGEN;
	/** Ausgeblendete Spalte der Statuszeile: sprachneutraler Marker, damit ein Sprachwechsel die Pause nicht bricht. */
	private static final int SPALTE_PAUSE_MARKER = SPALTE_ONLINE_ID;
	private static final String PAUSE_MARKER = "PAUSIERT";
	private static final int LETZTE_SPALTE = SPALTE_STATUS_ROH;

	private static final int BREITE_NR = 1400;
	private static final int BREITE_NAME = 6000;
	private static final int BREITE_STATUS = 2600;
	private static final int BREITE_TEXT = 7000;
	private static final int TITEL_SCHRIFTGROESSE = 14;
	private static final TableBorder2 HEADER_BORDER = BorderFactory.from().allThin().boldLn().forBottom().toBorder();

	private final Integer spieltagNr;

	public PtmOnlineSyncSheet(WorkingSpreadsheet workingSpreadsheet, TurnierSystem turnierSystem,
			Integer spieltagNrOderNull) {
		super(workingSpreadsheet, turnierSystem, "PtmOnlineSync");
		this.spieltagNr = spieltagNrOderNull;
	}

	@Override
	protected IKonfigurationSheet getKonfigurationSheet() {
		return null;
	}

	private String sheetName() {
		return spieltagNr == null ? SheetNamen.ptmOnlineSync() : SheetNamen.ptmOnlineSync(spieltagNr);
	}

	private String metadatenSchluessel() {
		return metadatenSchluessel(spieltagNr);
	}

	private static String metadatenSchluessel(Integer spieltagNr) {
		return spieltagNr == null ? SheetMetadataHelper.SCHLUESSEL_PTM_ONLINE_SYNC
				: SheetMetadataHelper.schluesselPtmOnlineSync(spieltagNr);
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

	// ── Anlegen, Verbinden, Entfernen ────────────────────────────────────────

	/**
	 * Legt das Blatt an (falls nötig) und schreibt die Verbindungsdaten. Läuft synchron und gehört in einen
	 * laufenden SheetRunner (Blattschutz-Scope, kein paralleler Lauf). Wechselt das Online-Turnier, wird die
	 * Zuordnung geleert.
	 */
	public void verbinden(OnlineTournamentDto turnier, String syncDokumentId, String leaseToken)
			throws GenerateException {
		Optional<String> bisherigesTurnier = getTournamentId();
		anlegen();
		if (bisherigesTurnier.isPresent() && !bisherigesTurnier.get().equals(turnier.id)) {
			leeren();
		}
		RangeData kopf = new RangeData();
		kopfZeile(kopf, sheetName() + " – " + StringUtils.defaultString(turnier.name), "");
		kopfZeile(kopf, I18n.get("ptmonline.sheet.label.onlineid"), turnier.id);
		kopfZeile(kopf, I18n.get("ptmonline.sheet.label.letzter.sync"), "");
		kopfZeile(kopf, I18n.get("ptmonline.sheet.label.sync.document"), syncDokumentId);
		kopfZeile(kopf, I18n.get("ptmonline.sheet.label.sync.lease"), leaseToken);
		RangeHelper.from(this, kopf.getRangePosition(Position.from(SPALTE_LABEL, ZEILE_TITEL))).setDataInRange(kopf);
		setPausiert(false);
	}

	@Override
	protected void doRun() throws GenerateException {
		anlegen();
	}

	private static void kopfZeile(RangeData kopf, String label, String wert) {
		RowData zeile = kopf.addNewRow();
		zeile.newString(label);
		zeile.newString(StringUtils.defaultString(wert));
	}

	/**
	 * Legt das Blatt (falls nötig) synchron im aufrufenden Thread an, schreibt die Kopfzeile der Tabelle und
	 * formatiert es. Für Aufrufer, die selbst als SheetRunner laufen. Datenerhaltend.
	 */
	public void anlegen() throws GenerateException {
		NewSheet.from(this, sheetName(), metadatenSchluessel()).pos(DefaultSheetPos.SUPERMELEE_WORK).useIfExist()
				.create();
		RangeData header = new RangeData();
		RowData kopfzeile = header.addNewRow();
		List.of(I18n.get("ptmonline.sheet.mapping.header.spielernr"), I18n.get("ptmonline.sheet.mapping.header.name"),
				I18n.get("ptmonline.sheet.mapping.header.onlinestatus"),
				I18n.get("ptmonline.sheet.mapping.header.onlinetarife"),
				I18n.get("ptmonline.sheet.mapping.header.onlinefragen"),
				I18n.get("ptmonline.sheet.mapping.header.onlineid"),
				I18n.get("ptmonline.sheet.mapping.header.lokaleuuid"),
				I18n.get("ptmonline.sheet.mapping.header.revision"),
				I18n.get("ptmonline.sheet.mapping.header.statusroh")).forEach(kopfzeile::newString);
		RangeHelper.from(this, header.getRangePosition(Position.from(SPALTE_NR, ZEILE_HEADER))).setDataInRange(header);
		setPausiert(istPausiert());
		formatieren();
	}

	/** Entfernt das Blatt wieder aus dem Dokument (Gegenstück zu {@link #verbinden}). */
	public void entfernen() throws GenerateException {
		Optional<XSpreadsheet> sheet = SheetMetadataHelper
				.findeSheet(getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), metadatenSchluessel());
		if (sheet.isEmpty()) {
			return;
		}
		XNamed named = Lo.qi(XNamed.class, sheet.get());
		if (named != null) {
			getSheetHelper().removeSheet(named.getName());
		}
	}

	// ── Formatierung ─────────────────────────────────────────────────────────

	private void formatieren() throws GenerateException {
		XSpreadsheet sheet = getXSpreadSheet();
		SheetHelper helper = getSheetHelper();
		BlattschutzManager.get().schreibeEntsperrt(sheet, () -> {
			spalte(helper, sheet, SPALTE_NR, BREITE_NR, true);
			spalte(helper, sheet, SPALTE_NAME, BREITE_NAME, true);
			spalte(helper, sheet, SPALTE_STATUS, BREITE_STATUS, true);
			spalte(helper, sheet, SPALTE_TARIFE, BREITE_TEXT, true);
			spalte(helper, sheet, SPALTE_FRAGEN, BREITE_TEXT, true);
			for (int spalte = SPALTE_ONLINE_ID; spalte <= LETZTE_SPALTE; spalte++) {
				spalte(helper, sheet, spalte, BREITE_STATUS, false);
			}
			helper.setPropertiesInRange(sheet, RangePosition.from(SPALTE_LABEL, ZEILE_TITEL, SPALTE_LABEL, ZEILE_TITEL),
					CellProperties.from().setCharWeight(FontWeight.BOLD).setCharHeight(TITEL_SCHRIFTGROESSE));
			helper.setPropertiesInRange(sheet,
					RangePosition.from(SPALTE_LABEL, ZEILE_TURNIER_ID, SPALTE_LABEL, ZEILE_SYNC_STATUS),
					CellProperties.from().setCharWeight(FontWeight.BOLD));
			helper.setPropertiesInRange(sheet,
					RangePosition.from(SPALTE_NR, ZEILE_HEADER, LETZTE_SICHTBARE_SPALTE, ZEILE_HEADER),
					CellProperties.from().setCharWeight(FontWeight.BOLD).setBorder(HEADER_BORDER)
							.setCellBackColor(BasePropertiesSpalte.DEFAULT_HEADER_BACK_COLOR).centerJustify()
							.setShrinkToFit(true));
		});
		SheetFreeze.from(sheet, getWorkingSpreadsheet()).anzZeilen(ERSTE_DATEN_ZEILE).doFreeze();
		int anzahl = (int) leseDaten().stream().filter(Predicate.not(PtmOnlineSyncSheet::istLeereZeile)).count();
		if (anzahl > 0) {
			formatiereDatenzeilen(ERSTE_DATEN_ZEILE, ERSTE_DATEN_ZEILE + anzahl - 1);
		}
	}

	private static void spalte(SheetHelper helper, XSpreadsheet sheet, int spalte, int breite, boolean sichtbar) {
		helper.setColumnProperties(sheet, spalte, ColumnProperties.from().setWidth(breite).isVisible(sichtbar));
	}

	/** Rahmen, verkleinerte Schrift und Zebra-Streifen (direkt geschrieben, keine bedingte Formatierung). */
	private void formatiereDatenzeilen(int ersteZeile, int letzteZeile) throws GenerateException {
		XSpreadsheet sheet = getXSpreadSheet();
		SheetHelper helper = getSheetHelper();
		RangePosition bereich = RangePosition.from(SPALTE_NR, ersteZeile, LETZTE_SICHTBARE_SPALTE, letzteZeile);
		BlattschutzManager.get().schreibeEntsperrt(sheet, () -> {
			helper.setPropertiesInRange(sheet, bereich,
					CellProperties.from().setAllThinBorder().setShrinkToFit(true));
			helper.setPropertiesInRange(sheet,
					RangePosition.from(SPALTE_NR, ersteZeile, SPALTE_NR, letzteZeile),
					CellProperties.from().setHoriJustify(CellHoriJustify.CENTER));
			try {
				SheetHelper.faerbeZeilenAbwechselnd(this, bereich, BasePropertiesSpalte.DEFAULT_GERADE_BACK_COLOR,
						BasePropertiesSpalte.DEFAULT_UNGERADE_BACK_COLOR);
			} catch (GenerateException e) {
				getLogger().warn("PTM-Online: Zebra-Formatierung übersprungen", e);
			}
		});
	}

	// ── Verbindungsdaten ─────────────────────────────────────────────────────

	public Optional<String> getTournamentId() throws GenerateException {
		return optional(leseWert(ZEILE_TURNIER_ID));
	}

	public Optional<Instant> getLastSync() throws GenerateException {
		return parseZeitpunkt(leseWert(ZEILE_LETZTER_SYNC));
	}

	public void setLastSync(Instant zeitpunkt) throws GenerateException {
		schreibeWert(ZEILE_LETZTER_SYNC, zeitpunkt == null ? "" : zeitpunkt.toString());
	}

	public Optional<String> getSyncDocumentId() throws GenerateException {
		return optional(leseWert(ZEILE_SYNC_DOKUMENT));
	}

	public Optional<String> getLeaseToken() throws GenerateException {
		return optional(leseWert(ZEILE_LEASE));
	}

	/** Pausiert: kein Meldungsabgleich, kein Rundenstart-Sync. Die Verbindung selbst bleibt bestehen. */
	public boolean istPausiert() throws GenerateException {
		if (SheetMetadataHelper.findeSheet(getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				metadatenSchluessel()).isEmpty()) {
			return false;
		}
		return PAUSE_MARKER.equals(StringUtils.strip(getSheetHelper().getTextFromCell(getXSpreadSheet(),
				Position.from(SPALTE_PAUSE_MARKER, ZEILE_SYNC_STATUS))));
	}

	/** Schreibt die Statuszeile: Label, übersetzte Anzeige (Spalte B) und den sprachneutralen Marker. */
	public void setPausiert(boolean pausiert) throws GenerateException {
		RangeData daten = new RangeData();
		RowData zeile = daten.addNewRow();
		zeile.newString(I18n.get("ptmonline.sheet.label.sync"));
		zeile.newString(I18n.get(pausiert ? "ptmonline.sheet.sync.pausiert" : "ptmonline.sheet.sync.aktiv"));
		for (int spalte = SPALTE_WERT + 1; spalte < SPALTE_PAUSE_MARKER; spalte++) {
			zeile.newEmpty();
		}
		zeile.newString(pausiert ? PAUSE_MARKER : "");
		RangeHelper.from(this, daten.getRangePosition(Position.from(SPALTE_LABEL, ZEILE_SYNC_STATUS)))
				.setDataInRange(daten);
	}

	/**
	 * Liest den Verbindungszustand rein lesend, ohne SheetRunner – für Anzeigen auf dem Main-Thread (Sidebar).
	 * Maßgeblich ist das Dokument {@code ws}, nicht der UI-Fokus.
	 */
	public static PtmOnlineSyncStatus leseStatus(WorkingSpreadsheet ws, Integer spieltagNrOderNull) {
		var xDoc = ws.getWorkingSpreadsheetDocument();
		Optional<XSpreadsheet> sheet = SheetMetadataHelper.findeSheet(xDoc, metadatenSchluessel(spieltagNrOderNull));
		if (sheet.isEmpty()) {
			return PtmOnlineSyncStatus.NICHT_VERBUNDEN;
		}
		try {
			RangeData daten = RangeHelper.from(sheet.get(), xDoc,
					RangePosition.from(SPALTE_LABEL, ZEILE_TURNIER_ID, SPALTE_PAUSE_MARKER, ZEILE_SYNC_STATUS))
					.getDataFromRange();
			boolean verbunden = !kopfWert(daten, ZEILE_TURNIER_ID, SPALTE_WERT).isBlank();
			boolean pausiert = PAUSE_MARKER.equals(kopfWert(daten, ZEILE_SYNC_STATUS, SPALTE_PAUSE_MARKER));
			return new PtmOnlineSyncStatus(verbunden, pausiert,
					parseZeitpunkt(kopfWert(daten, ZEILE_LETZTER_SYNC, SPALTE_WERT)));
		} catch (RuntimeException e) {
			LOGGER.warn("PTM-Online: Verbindungszustand nicht lesbar", e);
			return PtmOnlineSyncStatus.NICHT_VERBUNDEN;
		}
	}

	/** Wert aus dem ab {@link #ZEILE_TURNIER_ID} gelesenen Kopfblock. */
	private static String kopfWert(RangeData kopf, int zeile, int spalte) {
		int index = zeile - ZEILE_TURNIER_ID;
		return index < kopf.size() ? text(kopf.get(index), spalte) : "";
	}

	private static Optional<Instant> parseZeitpunkt(String wert) {
		if (wert.isBlank()) {
			return Optional.empty();
		}
		try {
			return Optional.of(Instant.parse(wert));
		} catch (DateTimeParseException e) {
			LOGGER.warn("PTM-Online: ungültiger Zeitpunkt des letzten Syncs '{}'", wert, e);
			return Optional.empty();
		}
	}

	/** Leer, solange das Dokument nicht verbunden ist (Blatt fehlt). */
	private String leseWert(int zeile) throws GenerateException {
		if (SheetMetadataHelper.findeSheet(getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				metadatenSchluessel()).isEmpty()) {
			return "";
		}
		return StringUtils.defaultString(getSheetHelper().getTextFromCell(getXSpreadSheet(),
				Position.from(SPALTE_WERT, zeile))).strip();
	}

	private void schreibeWert(int zeile, String wert) throws GenerateException {
		RangeData daten = new RangeData();
		daten.addNewRow().newString(StringUtils.defaultString(wert));
		RangeHelper.from(this, daten.getRangePosition(Position.from(SPALTE_WERT, zeile))).setDataInRange(daten);
	}

	private static Optional<String> optional(String wert) {
		return wert.isBlank() ? Optional.empty() : Optional.of(wert);
	}

	// ── Zuordnung lokale Meldung ↔ Online-Anmeldung ─────────────────────────

	public void addMapping(String lokaleUuid, String onlineId, String nummerFormel, int executionRevision,
			String lokaleBezeichnung, String onlineStatus) throws GenerateException {
		addMappings(List.of(new NeueZuordnung(lokaleUuid, onlineId, nummerFormel, executionRevision,
				lokaleBezeichnung, onlineStatus, "", "", "")));
	}

	/**
	 * Hängt neue Zuordnungen in einem Schreibzugriff hinter die letzte belegte Zeile; lokale Meldungen, die schon
	 * einer Online-Anmeldung zugeordnet sind, werden übersprungen. Danach Nr-Formeln und Formatierung als Block.
	 */
	public void addMappings(List<NeueZuordnung> zuordnungen) throws GenerateException {
		RangeData daten = leseDaten();
		Map<String, String> vorhandene = getOnlineIdsProUuid();
		Map<String, NeueZuordnung> neue = new LinkedHashMap<>();
		for (NeueZuordnung zuordnung : zuordnungen) {
			if (!vorhandene.containsKey(zuordnung.lokaleUuid())) {
				neue.putIfAbsent(zuordnung.lokaleUuid(), zuordnung);
			}
		}
		if (neue.isEmpty()) {
			return;
		}
		int ersteZeile = ERSTE_DATEN_ZEILE + anzahlBelegterZeilen(daten);
		int letzteZeile = ersteZeile + neue.size() - 1;
		if (letzteZeile > LETZTE_DATEN_ZEILE) {
			throw new GenerateException("PTM-Online-Zuordnung ist voll");
		}
		RangeData zeilen = new RangeData();
		String[][] formeln = new String[neue.size()][1];
		int index = 0;
		for (NeueZuordnung zuordnung : neue.values()) {
			RowData neu = zeilen.addNewRow();
			neu.newEmpty();
			neu.newString(StringUtils.defaultString(zuordnung.lokaleBezeichnung()));
			neu.newString(StringUtils.defaultString(zuordnung.onlineStatus()));
			neu.newString(StringUtils.defaultString(zuordnung.tarife()));
			neu.newString(StringUtils.defaultString(zuordnung.fragen()));
			neu.newString(zuordnung.onlineId());
			neu.newString(zuordnung.lokaleUuid());
			neu.newInt(Math.max(1, zuordnung.executionRevision()));
			neu.newString(StringUtils.defaultString(zuordnung.rohStatus()));
			formeln[index++][0] = zuordnung.nummerFormel();
		}
		RangeHelper.from(this, zeilen.getRangePosition(Position.from(SPALTE_NR, ersteZeile))).setDataInRange(zeilen);
		XSpreadsheet sheet = getXSpreadSheet();
		SheetHelper helper = getSheetHelper();
		RangePosition nrBereich = RangePosition.from(SPALTE_NR, ersteZeile, SPALTE_NR, letzteZeile);
		BlattschutzManager.get().schreibeEntsperrt(sheet, () -> helper.setFormulaArrayInRange(sheet, nrBereich, formeln));
		formatiereDatenzeilen(ERSTE_DATEN_ZEILE, letzteZeile);
	}

	/**
	 * Aktualisiert Bezeichnung, Status und Online-Details mehrerer Zuordnungen in einem Lese- und je einem
	 * Schreibzugriff für die sichtbaren Spalten und den Roh-Status; nicht genannte Zeilen bleiben unverändert.
	 */
	public void setAnzeigen(Map<String, ZuordnungsAnzeige> anzeigeProUuid) throws GenerateException {
		RangeData daten = leseDaten();
		int anzahl = anzahlBelegterZeilen(daten);
		if (anzahl == 0 || anzeigeProUuid.isEmpty()) {
			return;
		}
		RangeData sichtbar = new RangeData();
		RangeData rohStatus = new RangeData();
		for (int i = 0; i < anzahl; i++) {
			RowData zeile = daten.get(i);
			ZuordnungsAnzeige anzeige = anzeigeProUuid.get(text(zeile, SPALTE_LOKALE_UUID));
			RowData neu = sichtbar.addNewRow();
			RowData roh = rohStatus.addNewRow();
			if (anzeige == null) {
				for (int spalte = SPALTE_NAME; spalte <= SPALTE_FRAGEN; spalte++) {
					neu.add(zeile.get(spalte));
				}
				roh.add(zeile.get(SPALTE_STATUS_ROH));
				continue;
			}
			neu.newString(StringUtils.defaultString(anzeige.lokaleBezeichnung()));
			neu.newString(StringUtils.defaultString(anzeige.onlineStatus()));
			if (anzeige.details().isPresent()) {
				ZuordnungsAnzeige.OnlineDetails details = anzeige.details().get();
				neu.newString(StringUtils.defaultString(details.tarife()));
				neu.newString(StringUtils.defaultString(details.fragen()));
				roh.newString(StringUtils.defaultString(details.rohStatus()));
			} else {
				neu.add(zeile.get(SPALTE_TARIFE));
				neu.add(zeile.get(SPALTE_FRAGEN));
				roh.add(zeile.get(SPALTE_STATUS_ROH));
			}
		}
		RangeHelper.from(this, sichtbar.getRangePosition(Position.from(SPALTE_NAME, ERSTE_DATEN_ZEILE)))
				.setDataInRange(sichtbar);
		RangeHelper.from(this, rohStatus.getRangePosition(Position.from(SPALTE_STATUS_ROH, ERSTE_DATEN_ZEILE)))
				.setDataInRange(rohStatus);
	}

	public Optional<String> getOnlineId(String lokaleUuid) throws GenerateException {
		return zeileMitUuid(lokaleUuid).map(zeile -> text(zeile, SPALTE_ONLINE_ID)).filter(id -> !id.isBlank());
	}

	/** Alle zugeordneten Online-IDs, in einem Lesezugriff. */
	public Set<String> getOnlineIds() throws GenerateException {
		Set<String> ergebnis = new HashSet<>();
		for (RowData zeile : leseDaten()) {
			String onlineId = text(zeile, SPALTE_ONLINE_ID);
			if (!onlineId.isBlank()) {
				ergebnis.add(onlineId);
			}
		}
		return ergebnis;
	}

	public boolean istBereitsImportiert(String onlineId) throws GenerateException {
		return leseDaten().stream().anyMatch(zeile -> onlineId.equals(text(zeile, SPALTE_ONLINE_ID)));
	}

	public Optional<String> getLokaleUuid(String onlineId) throws GenerateException {
		return leseDaten().stream().filter(zeile -> onlineId.equals(text(zeile, SPALTE_ONLINE_ID)))
				.map(zeile -> text(zeile, SPALTE_LOKALE_UUID)).filter(uuid -> !uuid.isBlank()).findFirst();
	}

	/** Anmeldestatus der zuletzt übernommenen Online-Anmeldung, unübersetzt (z.B. {@code cancelled}). */
	public Optional<String> getRohStatus(String lokaleUuid) throws GenerateException {
		return zeileMitUuid(lokaleUuid).map(zeile -> text(zeile, SPALTE_STATUS_ROH)).filter(s -> !s.isBlank());
	}

	/** Hängt eine zugeordnete lokale Meldung auf eine andere Online-Anmeldung um (Neuanmeldung nach Storno). */
	public void ersetzeOnlineId(String lokaleUuid, String onlineId, int executionRevision) throws GenerateException {
		int zeile = zeileIndexMitUuid(lokaleUuid).orElseThrow(
				() -> new GenerateException("Keine PTM-Online-Zuordnung für lokale Meldung " + lokaleUuid));
		RangeData daten = new RangeData();
		RowData ids = daten.addNewRow();
		ids.newString(onlineId);
		ids.newString(lokaleUuid);
		ids.newInt(Math.max(1, executionRevision));
		RangeHelper.from(this, daten.getRangePosition(Position.from(SPALTE_ONLINE_ID, zeile))).setDataInRange(daten);
	}

	public void setBezeichnung(String lokaleUuid, String lokaleBezeichnung, String onlineStatus)
			throws GenerateException {
		Optional<Integer> zeile = zeileIndexMitUuid(lokaleUuid);
		if (zeile.isPresent()) {
			RangeData daten = new RangeData();
			RowData bezeichnung = daten.addNewRow();
			bezeichnung.newString(StringUtils.defaultString(lokaleBezeichnung));
			bezeichnung.newString(StringUtils.defaultString(onlineStatus));
			RangeHelper.from(this, daten.getRangePosition(Position.from(SPALTE_NAME, zeile.get())))
					.setDataInRange(daten);
		}
	}

	public void setOnlineDetails(String lokaleUuid, String tarife, String fragen, String rohStatus)
			throws GenerateException {
		Optional<Integer> zeile = zeileIndexMitUuid(lokaleUuid);
		if (zeile.isEmpty()) {
			return;
		}
		RangeData details = new RangeData();
		RowData detailZeile = details.addNewRow();
		detailZeile.newString(StringUtils.defaultString(tarife));
		detailZeile.newString(StringUtils.defaultString(fragen));
		RangeHelper.from(this, details.getRangePosition(Position.from(SPALTE_TARIFE, zeile.get())))
				.setDataInRange(details);
		RangeData status = new RangeData();
		status.addNewRow().newString(StringUtils.defaultString(rohStatus));
		RangeHelper.from(this, status.getRangePosition(Position.from(SPALTE_STATUS_ROH, zeile.get())))
				.setDataInRange(status);
	}

	/**
	 * Aktualisiert die Nr-Formeln (z.B. nachdem sich die Formel des Sync-Ziels geändert hat) in einem
	 * Schreibzugriff; unveränderte Formeln lösen keinen Schreibzugriff aus.
	 */
	public void aktualisiereAnzeigeFormeln(Map<String, String> formelnProUuid) throws GenerateException {
		RangeData daten = leseDaten();
		int anzahl = anzahlBelegterZeilen(daten);
		if (anzahl == 0) {
			return;
		}
		XSpreadsheet sheet = getXSpreadSheet();
		SheetHelper helper = getSheetHelper();
		RangePosition bereich = RangePosition.from(SPALTE_NR, ERSTE_DATEN_ZEILE, SPALTE_NR, ERSTE_DATEN_ZEILE + anzahl - 1);
		String[][] bisher = helper.getFormulaArrayFromRange(sheet, bereich);
		if (bisher.length != anzahl) {
			throw new GenerateException("PTM-Online: Nr-Formeln der Zuordnung nicht lesbar");
		}
		String[][] neu = new String[anzahl][1];
		boolean geaendert = false;
		for (int i = 0; i < anzahl; i++) {
			String formel = formelnProUuid.get(text(daten.get(i), SPALTE_LOKALE_UUID));
			neu[i][0] = formel == null ? bisher[i][0] : "=" + formel;
			geaendert |= !neu[i][0].equals(bisher[i][0]);
		}
		if (geaendert) {
			BlattschutzManager.get().schreibeEntsperrt(sheet, () -> helper.setFormulaArrayInRange(sheet, bereich, neu));
		}
	}

	/** Online-ID je lokaler UUID aller Zuordnungen, in einem Lesezugriff. */
	public Map<String, String> getOnlineIdsProUuid() throws GenerateException {
		Map<String, String> ergebnis = new LinkedHashMap<>();
		for (RowData zeile : leseDaten()) {
			String uuid = text(zeile, SPALTE_LOKALE_UUID);
			String onlineId = text(zeile, SPALTE_ONLINE_ID);
			if (!uuid.isBlank() && !onlineId.isBlank()) {
				ergebnis.putIfAbsent(uuid, onlineId);
			}
		}
		return ergebnis;
	}

	/** Ausführungsrevision je lokaler UUID aller Zuordnungen, in einem Lesezugriff. */
	public Map<String, Integer> getExecutionRevisionenProUuid() throws GenerateException {
		Map<String, Integer> ergebnis = new LinkedHashMap<>();
		for (RowData zeile : leseDaten()) {
			String uuid = text(zeile, SPALTE_LOKALE_UUID);
			if (!uuid.isBlank()) {
				ergebnis.putIfAbsent(uuid, Math.max(1, zeile.get(SPALTE_REVISION).getIntVal(1)));
			}
		}
		return ergebnis;
	}

	/** Setzt die Ausführungsrevisionen mehrerer Zuordnungen in einem Schreibzugriff. */
	public void setExecutionRevisionen(Map<String, Integer> revisionProUuid) throws GenerateException {
		RangeData daten = leseDaten();
		int anzahl = anzahlBelegterZeilen(daten);
		if (anzahl == 0 || revisionProUuid.isEmpty()) {
			return;
		}
		RangeData revisionen = new RangeData();
		for (int i = 0; i < anzahl; i++) {
			RowData zeile = daten.get(i);
			Integer neu = revisionProUuid.get(text(zeile, SPALTE_LOKALE_UUID));
			RowData ziel = revisionen.addNewRow();
			if (neu != null) {
				ziel.newInt(Math.max(1, neu));
			} else {
				ziel.add(zeile.get(SPALTE_REVISION));
			}
		}
		RangeHelper.from(this, revisionen.getRangePosition(Position.from(SPALTE_REVISION, ERSTE_DATEN_ZEILE)))
				.setDataInRange(revisionen);
	}

	/** Zeilen bis einschließlich der letzten belegten Zuordnung (Lücken zählen mit). */
	private static int anzahlBelegterZeilen(RangeData daten) {
		for (int i = daten.size() - 1; i >= 0; i--) {
			if (!istLeereZeile(daten.get(i))) {
				return i + 1;
			}
		}
		return 0;
	}

	public void leeren() throws GenerateException {
		RangeHelper.from(this, RangePosition.from(SPALTE_NR, ERSTE_DATEN_ZEILE, LETZTE_SPALTE, LETZTE_DATEN_ZEILE))
				.clearRange();
	}

	/** Keine Zeilen, solange das Dokument nicht verbunden ist (Blatt fehlt). */
	private RangeData leseDaten() throws GenerateException {
		if (SheetMetadataHelper.findeSheet(getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				metadatenSchluessel()).isEmpty()) {
			return new RangeData();
		}
		return RangeHelper.from(this, RangePosition.from(SPALTE_NR, ERSTE_DATEN_ZEILE, LETZTE_SPALTE,
				LETZTE_DATEN_ZEILE)).getDataFromRange();
	}

	private Optional<RowData> zeileMitUuid(String lokaleUuid) throws GenerateException {
		return leseDaten().stream().filter(zeile -> lokaleUuid.equals(text(zeile, SPALTE_LOKALE_UUID))).findFirst();
	}

	private Optional<Integer> zeileIndexMitUuid(String lokaleUuid) throws GenerateException {
		RangeData daten = leseDaten();
		for (int i = 0; i < daten.size(); i++) {
			if (lokaleUuid.equals(text(daten.get(i), SPALTE_LOKALE_UUID))) {
				return Optional.of(ERSTE_DATEN_ZEILE + i);
			}
		}
		return Optional.empty();
	}

	/** RowData enthält auch für leere Calc-Zellen CellData-Objekte; List.isEmpty() ist daher ungeeignet. */
	private static boolean istLeereZeile(RowData zeile) {
		return text(zeile, SPALTE_ONLINE_ID).isBlank() && text(zeile, SPALTE_LOKALE_UUID).isBlank();
	}

	private static String text(RowData zeile, int spalte) {
		return zeile.size() > spalte ? StringUtils.defaultString(zeile.get(spalte).getStringVal()).strip() : "";
	}
}
