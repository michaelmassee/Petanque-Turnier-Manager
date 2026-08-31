/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.online;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
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
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.onlinesync.PtmOnlineApiClient;
import de.petanqueturniermanager.onlinesync.PtmOnlineException;
import de.petanqueturniermanager.onlinesync.RegistrationDto;
import de.petanqueturniermanager.onlinesync.ResultUpdateDto;
import de.petanqueturniermanager.onlinesync.TournamentDto;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;

/**
 * Verbindet ein Supermelee-Turnierdokument mit PTM Online: legt bei Bedarf ein Online-Turnier an,
 * holt neue/geänderte Anmeldungen ab (und trägt sie in die Meldeliste ein) und schreibt lokale
 * Setzpositionen als Ergebnis-Update zurück. Alle Zustände (Verknüpfung, Online-Metadaten,
 * Sync-Protokoll, Mapping-Tabelle) liegen im Sheet {@link SheetNamen#ptmOnline()}.
 */
public class SupermeleeOnlineSyncSheet extends SheetRunner implements ISheet {

	private static final Logger logger = LogManager.getLogger(SupermeleeOnlineSyncSheet.class);

	// Spalten
	private static final int SPALTE_LABEL = 0;
	private static final int SPALTE_WERT = 1;
	private static final int SPALTE_MAPPING_SPIELER_NR = 0;
	private static final int SPALTE_MAPPING_ONLINE_ID = 1;
	private static final int SPALTE_MAPPING_NAME = 2;
	private static final int SPALTE_MAPPING_STATUS = 3;

	// Zeilen: Verknüpfung & Status
	private static final int ZEILE_TITEL = 0;
	private static final int ZEILE_TURNIERNAME = 2;
	private static final int ZEILE_ONLINE_ID = 3;
	private static final int ZEILE_STATUS = 4;
	private static final int ZEILE_LETZTER_SYNC = 5;

	// Zeilen: Online-Metadaten
	private static final int ZEILE_META_TITEL = 7;
	private static final int ZEILE_ANMELDESCHLUSS = 8;
	private static final int ZEILE_MAX_TEILNEHMER = 9;
	private static final int ZEILE_STARTGELD_CENT = 10;
	private static final int ZEILE_KONTAKT = 11;
	private static final int ZEILE_SICHTBARKEIT = 12;
	private static final int ZEILE_REGION = 13;
	private static final int ZEILE_BESCHREIBUNG = 14;

	// Zeilen: Sync-Protokoll
	private static final int ZEILE_LOG_TITEL = 16;
	private static final int ZEILE_LOG_HEADER = 17;
	private static final int LOG_ERSTE_ZEILE = 18;
	private static final int LOG_MAX_ZEILEN = 20;

	// Zeilen: Mapping
	private static final int ZEILE_MAPPING_TITEL = LOG_ERSTE_ZEILE + LOG_MAX_ZEILEN + 1;
	private static final int ZEILE_MAPPING_HEADER = ZEILE_MAPPING_TITEL + 1;
	private static final int MAPPING_ERSTE_ZEILE = ZEILE_MAPPING_HEADER + 1;
	private static final int MAPPING_MAX_ZEILEN = MeldungenSpalte.MAX_ANZ_MELDUNGEN;

	private final SuperMeleeKonfigurationSheet konfigurationSheet;
	private final MeldeListeSheet_Update meldeliste;

	public SupermeleeOnlineSyncSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SUPERMELEE, "PtmOnlineSync");
		konfigurationSheet = new SuperMeleeKonfigurationSheet(workingSpreadsheet);
		meldeliste = new MeldeListeSheet_Update(workingSpreadsheet);
	}

	@Override
	protected SuperMeleeKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_PTM_ONLINE, SheetNamen.ptmOnline());
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	protected void doRun() throws GenerateException {
		meldeliste.setSpielTag(getKonfigurationSheet().getAktiveSpieltag());

		NewSheet.from(this, SheetNamen.ptmOnline(), SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_PTM_ONLINE)
				.pos(DefaultSheetPos.SUPERMELEE_WORK).useIfExist().create();

		processBoxinfo("processbox.ptmonline.sync.start");
		schreibeLabels();

		var zugangsdaten = new LibreOfficePtmOnlineSpeicher(getxContext()).laden();
		var client = new PtmOnlineApiClient(zugangsdaten.apiKey(), zugangsdaten.baseUrl());

		int anzahlNeu = 0;
		int anzahlAktualisiert = 0;
		String fehler = null;
		try {
			String onlineId = leseWert(ZEILE_ONLINE_ID);
			if (StringUtils.isBlank(onlineId)) {
				onlineId = turnierOnlineAnlegen(client);
			}
			if (StringUtils.isNotBlank(onlineId)) {
				anzahlNeu = registrierungenAbholenUndEintragen(client, onlineId);
				anzahlAktualisiert = ergebnisseHochladen(client, onlineId);
				schreibeWert(ZEILE_STATUS, I18n.get("ptmonline.sheet.status.verbunden"));
			}
		} catch (PtmOnlineException e) {
			fehler = e.getMessage();
			logger.warn("PTM-Online-Sync fehlgeschlagen: {}", fehler, e);
			schreibeWert(ZEILE_STATUS, I18n.get("ptmonline.sheet.status.fehler"));
		}

		schreibeWert(ZEILE_LETZTER_SYNC, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
		protokollZeileSchreiben(anzahlNeu, anzahlAktualisiert, fehler);

		if (fehler != null) {
			throw new GenerateException(I18n.get("ptmonline.sync.fehler", fehler));
		}
		processBoxinfo("processbox.ptmonline.sync.fertig", anzahlNeu, anzahlAktualisiert);
	}

	private String turnierOnlineAnlegen(PtmOnlineApiClient client) throws PtmOnlineException, GenerateException {
		String turnierName = leseWert(ZEILE_TURNIERNAME);
		if (StringUtils.isBlank(turnierName)) {
			throw new PtmOnlineException(I18n.get("ptmonline.sync.fehler.kein.turniername"));
		}
		TournamentDto neu = new TournamentDto();
		neu.name = turnierName;
		neu.date = LocalDate.now().toString();
		neu.type = "supermelee";
		neu.formation = getKonfigurationSheet().getSuperMeleeMode() == null
				? null
				: getKonfigurationSheet().getSuperMeleeMode().name().toLowerCase(java.util.Locale.ROOT);
		TournamentDto angelegt = client.createTournament(neu);
		schreibeWert(ZEILE_ONLINE_ID, angelegt.id);
		return angelegt.id;
	}

	private int registrierungenAbholenUndEintragen(PtmOnlineApiClient client, String onlineId)
			throws PtmOnlineException, GenerateException {
		Instant letzterSync = leseLetztenSyncZeitpunkt();
		List<RegistrationDto> registrierungen = client.getRegistrationsSince(onlineId, letzterSync);
		if (registrierungen.isEmpty()) {
			return 0;
		}

		Map<String, Integer> mappingOnlineIdZuSpielerNr = leseMapping();
		List<Integer> vorhandeneSpielerNrList = meldeliste.getSpielerNrList();
		int naechsteSpielerNr = vorhandeneSpielerNrList.isEmpty() ? 1
				: vorhandeneSpielerNrList.get(vorhandeneSpielerNrList.size() - 1) + 1;

		RangeData neueMeldelisteZeilen = new RangeData();
		List<MappingEintrag> neueMappingEintraege = new ArrayList<>();

		for (RegistrationDto registrierung : registrierungen) {
			if (mappingOnlineIdZuSpielerNr.containsKey(registrierung.id)) {
				continue; // bereits übernommen – Namensänderungen werden bewusst nicht automatisch nachgezogen
			}
			int spielerNr = naechsteSpielerNr++;
			RowData zeile = neueMeldelisteZeilen.addNewRow();
			zeile.newInt(spielerNr);
			zeile.newString(StringUtils.defaultString(registrierung.firstName));
			zeile.newString(StringUtils.defaultString(registrierung.lastName));
			String anzeigeName = (StringUtils.defaultString(registrierung.firstName) + " "
					+ StringUtils.defaultString(registrierung.lastName)).trim();
			neueMappingEintraege.add(new MappingEintrag(spielerNr, registrierung.id, anzeigeName,
					StringUtils.defaultString(registrierung.status)));
		}

		if (neueMeldelisteZeilen.isEmpty()) {
			return 0;
		}

		var meldungenSpalte = meldeliste.getMeldungenSpalte();
		Position start = Position.from(meldungenSpalte.getSpielerNrSpalte(),
				meldeliste.naechsteFreieDatenZeileInSpielerNrSpalte());
		RangeHelper.from(meldeliste, neueMeldelisteZeilen.getRangePosition(start)).setDataInRange(neueMeldelisteZeilen);

		meldeliste.upDateSheet();

		mappingSchreiben(neueMappingEintraege);
		return neueMappingEintraege.size();
	}

	private int ergebnisseHochladen(PtmOnlineApiClient client, String onlineId) throws PtmOnlineException, GenerateException {
		Map<String, Integer> mapping = leseMapping();
		if (mapping.isEmpty()) {
			return 0;
		}
		SpielerMeldungen aktiveMeldungen = meldeliste.getAlleMeldungen();
		Map<Integer, Spieler> spielerNachNr = new HashMap<>();
		for (Spieler spieler : aktiveMeldungen.getSpielerList()) {
			spielerNachNr.put(spieler.getNr(), spieler);
		}

		List<ResultUpdateDto> updates = new ArrayList<>();
		for (Map.Entry<String, Integer> eintrag : mapping.entrySet()) {
			Spieler spieler = spielerNachNr.get(eintrag.getValue());
			if (spieler == null) {
				continue;
			}
			updates.add(new ResultUpdateDto(eintrag.getKey(), "confirmed", null));
		}
		client.postResults(onlineId, updates);
		return updates.size();
	}

	// ---- Mapping-Tabelle ----

	private record MappingEintrag(int spielerNr, String onlineId, String name, String status) {
	}

	private Map<String, Integer> leseMapping() throws GenerateException {
		Map<String, Integer> ergebnis = new HashMap<>();
		RangePosition bereich = RangePosition.from(SPALTE_MAPPING_SPIELER_NR, MAPPING_ERSTE_ZEILE,
				SPALTE_MAPPING_STATUS, MAPPING_ERSTE_ZEILE + MAPPING_MAX_ZEILEN);
		RangeData daten = RangeHelper.from(this, bereich).getDataFromRange();
		for (RowData zeile : daten) {
			if (zeile.isEmpty()) {
				continue;
			}
			int spielerNr = zeile.get(SPALTE_MAPPING_SPIELER_NR).getIntVal(-1);
			String onlineId = zeile.size() > SPALTE_MAPPING_ONLINE_ID ? zeile.get(SPALTE_MAPPING_ONLINE_ID).getStringVal() : null;
			if (spielerNr > 0 && StringUtils.isNotBlank(onlineId)) {
				ergebnis.put(onlineId, spielerNr);
			}
		}
		return ergebnis;
	}

	private void mappingSchreiben(List<MappingEintrag> neueEintraege) throws GenerateException {
		if (neueEintraege.isEmpty()) {
			return;
		}
		int naechsteFreieZeile = MAPPING_ERSTE_ZEILE;
		RangePosition bereich = RangePosition.from(SPALTE_MAPPING_SPIELER_NR, MAPPING_ERSTE_ZEILE,
				SPALTE_MAPPING_STATUS, MAPPING_ERSTE_ZEILE + MAPPING_MAX_ZEILEN);
		RangeData vorhanden = RangeHelper.from(this, bereich).getDataFromRange();
		for (RowData zeile : vorhanden) {
			if (zeile.isEmpty() || zeile.get(SPALTE_MAPPING_SPIELER_NR).getIntVal(-1) <= 0) {
				break;
			}
			naechsteFreieZeile++;
		}

		RangeData neu = new RangeData();
		for (MappingEintrag eintrag : neueEintraege) {
			RowData zeile = neu.addNewRow();
			zeile.newInt(eintrag.spielerNr());
			zeile.newString(eintrag.onlineId());
			zeile.newString(eintrag.name());
			zeile.newString(eintrag.status());
		}
		Position start = Position.from(SPALTE_MAPPING_SPIELER_NR, naechsteFreieZeile);
		RangeHelper.from(this, neu.getRangePosition(start)).setDataInRange(neu);
	}

	// ---- Sync-Protokoll ----

	private void protokollZeileSchreiben(int anzahlNeu, int anzahlAktualisiert, String fehler) throws GenerateException {
		RangePosition bereich = RangePosition.from(0, LOG_ERSTE_ZEILE, 3, LOG_ERSTE_ZEILE + LOG_MAX_ZEILEN - 2);
		RangeData bisherige = RangeHelper.from(this, bereich).getDataFromRange();

		RangeData neu = new RangeData();
		RowData ersteZeile = neu.addNewRow();
		ersteZeile.newString(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
		ersteZeile.newString(I18n.get("ptmonline.sheet.log.richtung.beide"));
		ersteZeile.newString(I18n.get("ptmonline.sheet.log.anzahl", anzahlNeu, anzahlAktualisiert));
		ersteZeile.newString(StringUtils.defaultString(fehler));

		for (RowData alteZeile : bisherige) {
			if (alteZeile.isEmpty() || StringUtils.isBlank(alteZeile.get(0).getStringVal())) {
				break;
			}
			neu.add(alteZeile);
		}

		RangeHelper.from(this, bereich).clearRange();
		RangeHelper.from(this, neu.getRangePosition(Position.from(0, LOG_ERSTE_ZEILE))).setDataInRange(neu);
	}

	private Instant leseLetztenSyncZeitpunkt() throws GenerateException {
		String wert = leseWert(ZEILE_LETZTER_SYNC);
		if (StringUtils.isBlank(wert)) {
			return null;
		}
		try {
			return Instant.parse(wert);
		} catch (java.time.format.DateTimeParseException e) {
			logger.debug("Letzter-Sync-Zeitstempel '{}' nicht lesbar, betrachte als 'nie synchronisiert'", wert, e);
			return null;
		}
	}

	// ---- Labels / feste Werte ----

	private void schreibeLabels() throws GenerateException {
		RangeData labels = new RangeData();
		for (int i = 0; i <= ZEILE_BESCHREIBUNG; i++) {
			labels.addNewRow().newString(labelFuerZeile(i));
		}
		RangeHelper.from(this, labels.getRangePosition(Position.from(SPALTE_LABEL, ZEILE_TITEL)))
				.setDataInRange(labels);

		schreibeWert(ZEILE_LOG_TITEL, I18n.get("ptmonline.sheet.log.titel"));
		schreibeZeile(ZEILE_LOG_HEADER, I18n.get("ptmonline.sheet.log.header.zeitpunkt"),
				I18n.get("ptmonline.sheet.log.header.richtung"), I18n.get("ptmonline.sheet.log.header.anzahl"),
				I18n.get("ptmonline.sheet.log.header.fehler"));

		schreibeWert(ZEILE_MAPPING_TITEL, I18n.get("ptmonline.sheet.mapping.titel"));
		schreibeZeile(ZEILE_MAPPING_HEADER, I18n.get("ptmonline.sheet.mapping.header.spielernr"),
				I18n.get("ptmonline.sheet.mapping.header.onlineid"), I18n.get("ptmonline.sheet.mapping.header.name"),
				I18n.get("ptmonline.sheet.mapping.header.status"));
	}

	private static String labelFuerZeile(int zeile) {
		if (zeile == ZEILE_TITEL) {
			return I18n.get("ptmonline.sheet.titel");
		}
		if (zeile == ZEILE_TURNIERNAME) {
			return I18n.get("ptmonline.sheet.label.turniername");
		}
		if (zeile == ZEILE_ONLINE_ID) {
			return I18n.get("ptmonline.sheet.label.onlineid");
		}
		if (zeile == ZEILE_STATUS) {
			return I18n.get("ptmonline.sheet.label.status");
		}
		if (zeile == ZEILE_LETZTER_SYNC) {
			return I18n.get("ptmonline.sheet.label.letzter.sync");
		}
		if (zeile == ZEILE_META_TITEL) {
			return I18n.get("ptmonline.sheet.metadaten.titel");
		}
		if (zeile == ZEILE_ANMELDESCHLUSS) {
			return I18n.get("ptmonline.sheet.label.anmeldeschluss");
		}
		if (zeile == ZEILE_MAX_TEILNEHMER) {
			return I18n.get("ptmonline.sheet.label.max.teilnehmer");
		}
		if (zeile == ZEILE_STARTGELD_CENT) {
			return I18n.get("ptmonline.sheet.label.startgeld");
		}
		if (zeile == ZEILE_KONTAKT) {
			return I18n.get("ptmonline.sheet.label.kontakt");
		}
		if (zeile == ZEILE_SICHTBARKEIT) {
			return I18n.get("ptmonline.sheet.label.sichtbarkeit");
		}
		if (zeile == ZEILE_REGION) {
			return I18n.get("ptmonline.sheet.label.region");
		}
		if (zeile == ZEILE_BESCHREIBUNG) {
			return I18n.get("ptmonline.sheet.label.beschreibung");
		}
		return "";
	}

	private String leseWert(int zeile) throws GenerateException {
		return getSheetHelper().getTextFromCell(getXSpreadSheet(), Position.from(SPALTE_WERT, zeile));
	}

	private void schreibeWert(int zeile, String wert) throws GenerateException {
		var celVal = de.petanqueturniermanager.helper.cellvalue.StringCellValue.from(getXSpreadSheet(),
				Position.from(SPALTE_WERT, zeile), StringUtils.defaultString(wert));
		getSheetHelper().setStringValueInCell(celVal);
	}

	private void schreibeZeile(int zeile, String... werte) throws GenerateException {
		RangeData daten = new RangeData();
		RowData row = daten.addNewRow();
		for (String wert : werte) {
			row.newString(wert);
		}
		RangeHelper.from(this, daten.getRangePosition(Position.from(SPALTE_LABEL, zeile))).setDataInRange(daten);
	}
}
