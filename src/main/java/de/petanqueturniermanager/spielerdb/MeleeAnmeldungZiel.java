/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.spielerdb;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungLeser;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungNummerierung;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungZeile;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * PTM-Online-Ziel bei aktiver Mêlée-Anmeldung: Online-Einzelanmeldungen gehören in das
 * Mêlée-Anmeldung-Sheet, nicht in die Meldeliste. Die Teams der Meldeliste entstehen erst lokal beim
 * „Mêlée übernehmen“ und haben online keine Entsprechung – ein Online-Mêlée-Turnier nimmt nur
 * Einzelspieler an.
 * <p>
 * Jede Zeile des Mêlée-Sheets ist genau ein Spieler ({@link Formation#TETE}). Die lokale PTM-Online-ID
 * steht in einer ausgeblendeten Spalte rechts neben dem Datenbereich ({@link #SPALTE_PTM_ONLINE_UUID}),
 * damit {@link MeleeAnmeldungLeser}, Nummerierung und Druckbereich unverändert bleiben.
 * <p>
 * Das Sheet wird bei jedem Zugriff neu aufgelöst: Das Ziel entsteht auch dann, wenn das Mêlée-Sheet noch
 * fehlt (z.&nbsp;B. beim Rundenstart-Abgleich); Lesen liefert dann nichts, Schreiben scheitert.
 */
public final class MeleeAnmeldungZiel implements MeldelisteZiel, MeleeAnmeldungKonstanten {

	private static final Logger logger = LogManager.getLogger(MeleeAnmeldungZiel.class);

	/** Eine Spalte Abstand zum Datenbereich, wie die UUID-Spalte der Meldeliste. */
	static final int SPALTE_PTM_ONLINE_UUID = LETZTE_SPALTE + 2;

	private static final int LETZTE_DATEN_ZEILE = ERSTE_DATEN_ZEILE + MAX_ANZ_ANMELDUNGEN - 1;

	private final WorkingSpreadsheet ws;
	private final TurnierSystem system;
	private final String metadatenSchluessel;
	private final SheetHelper sheetHelper;

	MeleeAnmeldungZiel(WorkingSpreadsheet ws, TurnierSystem system, String metadatenSchluessel) {
		this.ws = ws;
		this.system = system;
		this.metadatenSchluessel = metadatenSchluessel;
		this.sheetHelper = new SheetHelper(ws);
	}

	@Override
	public Formation getFormation() {
		return Formation.TETE;
	}

	@Override
	public String getSystemBezeichnung() {
		return system.getBezeichnung();
	}

	/** Alle Zeilen der Mêlée-Anmeldung inkl. Eingecheckt-/Übernommen-Markierung und Setzposition. */
	public List<MeleeAnmeldungZeile> leseMeleeZeilen() {
		return MeleeAnmeldungLeser.lesen(ws, metadatenSchluessel);
	}

	private Optional<MeleeAnmeldungZeile> zeile(int zeile1Basiert) {
		return leseMeleeZeilen().stream().filter(z -> z.zeile() == zeile1Basiert - 1).findFirst();
	}

	private XSpreadsheet sheet() throws MeldelisteSchreibException {
		XSpreadsheet sheet = MeleeAnmeldungLeser.findeSheet(ws, metadatenSchluessel);
		if (sheet == null) {
			throw new MeldelisteSchreibException("Mêlée-Anmeldung-Sheet nicht vorhanden");
		}
		return sheet;
	}

	@Override
	public List<MeldelisteSpielerDaten> leseAlleSpielerRoh() {
		return leseMeleeZeilen().stream()
				.map(z -> new MeldelisteSpielerDaten(z.vorname(), z.nachname(), null, z.zeile() + 1))
				.toList();
	}

	@Override
	public List<String> getVorhandeneSpielernamen() {
		return leseMeleeZeilen().stream().map(MeleeAnmeldungZeile::anzeigeName).toList();
	}

	@Override
	public MeldelisteStatus getMeldelisteStatus() {
		List<MeleeAnmeldungZeile> zeilen = leseMeleeZeilen();
		int checkin = (int) zeilen.stream().filter(MeleeAnmeldungZeile::eingecheckt).count();
		return new MeldelisteStatus(zeilen.size() - checkin, checkin, zeilen.size());
	}

	@Override
	public int findeZeileMitName(String spielerName) {
		String gesucht = spielerName.strip().toLowerCase(Locale.ROOT);
		return leseMeleeZeilen().stream()
				.filter(z -> z.anzeigeName().toLowerCase(Locale.ROOT).equals(gesucht))
				.mapToInt(z -> z.zeile() + 1)
				.findFirst().orElse(-1);
	}

	@Override
	public int schreibeBlock(List<SpielerMitVerein> spieler) throws MeldelisteSchreibException {
		return schreibeBlockUndLiefereZeile(spieler, NeueMeldungTeilnahme.AKTIV) < 0 ? 0 : spieler.size();
	}

	/**
	 * Hängt einen Spieler hinter die letzte belegte Zeile an. {@link NeueMeldungTeilnahme#AKTIV} setzt die
	 * Eingecheckt-Markierung, {@link NeueMeldungTeilnahme#INAKTIV} lässt sie leer. Die Nr vergibt
	 * anschließend {@link MeleeAnmeldungNummerierung}.
	 */
	@Override
	public int schreibeBlockUndLiefereZeile(List<SpielerMitVerein> spieler, NeueMeldungTeilnahme teilnahme)
			throws MeldelisteSchreibException {
		if (spieler.isEmpty()) {
			return 0;
		}
		if (spieler.size() > 1) {
			throw new MeldelisteSchreibException("Mêlée-Anmeldung nimmt nur Einzelspieler an: " + spieler.size());
		}
		XSpreadsheet sheet = sheet();
		int zeile = naechsteFreieZeile();
		if (zeile > LETZTE_DATEN_ZEILE) {
			throw new MeldelisteSchreibException("Keine freie Zeile in der Mêlée-Anmeldung");
		}
		SpielerMitVerein neuer = spieler.getFirst();
		RangeData data = new RangeData();
		RowData row = data.addNewRow();
		row.newString(neuer.vorname());
		row.newString(neuer.nachname());
		row.newEmpty();
		row.newString(teilnahme == NeueMeldungTeilnahme.AKTIV ? MARKIERUNG : "");
		try {
			RangeHelper.from(sheet, ws.getWorkingSpreadsheetDocument(),
					RangePosition.from(SPALTE_VORNAME, zeile, SPALTE_EINGECHECKT, zeile)).setDataInRange(data);
			MeleeAnmeldungNummerierung.nummerieren(ws, sheet);
		} catch (GenerateException e) {
			throw new MeldelisteSchreibException("Schreibvorgang fehlgeschlagen", e);
		}
		return zeile + 1;
	}

	private int naechsteFreieZeile() {
		return leseMeleeZeilen().stream().mapToInt(MeleeAnmeldungZeile::zeile).max().orElse(ERSTE_DATEN_ZEILE - 1) + 1;
	}

	@Override
	public String getOderErzeugeLokaleUuid(int zeile1Basiert) throws MeldelisteSchreibException {
		Position position = uuidPosition(zeile1Basiert);
		XSpreadsheet sheet = sheet();
		String vorhanden = text(sheet, position);
		if (!vorhanden.isEmpty()) {
			return vorhanden;
		}
		String uuid = UUID.randomUUID().toString();
		schreibeUuid(sheet, position, uuid);
		return uuid;
	}

	@Override
	public void setzeLokaleUuid(int zeile1Basiert, String uuid) throws MeldelisteSchreibException {
		schreibeUuid(sheet(), uuidPosition(zeile1Basiert), uuid);
	}

	private static Position uuidPosition(int zeile1Basiert) throws MeldelisteSchreibException {
		int zeile = zeile1Basiert - 1;
		if (zeile < ERSTE_DATEN_ZEILE || zeile > LETZTE_DATEN_ZEILE) {
			throw new MeldelisteSchreibException("Ungültige Zeile der Mêlée-Anmeldung: " + zeile1Basiert);
		}
		return Position.from(SPALTE_PTM_ONLINE_UUID, zeile);
	}

	private void schreibeUuid(XSpreadsheet sheet, Position position, String uuid) {
		stelleUuidSpalteBereit(sheet);
		sheetHelper.setStringValueInCell(StringCellValue.from(sheet, position, uuid));
	}

	/** Überschrift der ausgeblendeten UUID-Spalte, einmalig beim ersten Schreiben einer UUID. */
	private void stelleUuidSpalteBereit(XSpreadsheet sheet) {
		String header = I18n.get("ptmonline.meldeliste.header.lokaleuuid");
		Position kopf = Position.from(SPALTE_PTM_ONLINE_UUID, KOPF_ZEILE);
		if (header.equals(text(sheet, kopf))) {
			return;
		}
		sheetHelper.setStringValueInCell(StringCellValue.from(sheet, kopf, header));
		sheetHelper.setColumnProperties(sheet, SPALTE_PTM_ONLINE_UUID, ColumnProperties.from().isVisible(false));
	}

	private String text(XSpreadsheet sheet, Position position) {
		String wert = sheetHelper.getTextFromCell(sheet, position);
		return wert == null ? "" : wert.strip();
	}

	@Override
	public String formelTeamNrAusLokalerUuid(String uuid) {
		String blatt = "$'" + SheetNamen.meleeAnmeldung() + "'.";
		String nummern = blatt + Position.from(SPALTE_NR, ERSTE_DATEN_ZEILE).getAddressWith$() + ":"
				+ Position.from(SPALTE_NR, LETZTE_DATEN_ZEILE).getAddressWith$();
		String uuids = blatt + Position.from(SPALTE_PTM_ONLINE_UUID, ERSTE_DATEN_ZEILE).getAddressWith$() + ":"
				+ Position.from(SPALTE_PTM_ONLINE_UUID, LETZTE_DATEN_ZEILE).getAddressWith$();
		return "IFNA(INDEX(" + nummern + ";MATCH(\"" + uuid + "\";" + uuids + ";0));\"\")";
	}

	/**
	 * Online storniert: Eingecheckt-Markierung entfernen, damit der Spieler beim nächsten „Mêlée
	 * übernehmen“ nicht in ein Team gemischt wird. Ist er bereits übernommen, steckt er schon in einem
	 * Team der Meldeliste – dann bleibt die Zeile unverändert, den Storno zeigt das Mapping-Sheet.
	 */
	@Override
	public void markiereAlsAbgemeldet(int zeile1Basiert) throws MeldelisteSchreibException {
		Optional<MeleeAnmeldungZeile> zeile = zeile(zeile1Basiert);
		if (zeile.isEmpty() || !zeile.get().eingecheckt()) {
			return;
		}
		if (zeile.get().uebernommen()) {
			logger.info("PTM-Online: Storno für bereits übernommenen Mêlée-Spieler {} – Team bleibt unverändert",
					zeile.get().anzeigeName());
			return;
		}
		sheetHelper.clearValInCell(sheet(), Position.from(SPALTE_EINGECHECKT, zeile.get().zeile()));
	}

	/**
	 * Neuanmeldung nach Storno: nichts zu tun. Die Zeile bleibt ohne Eingecheckt-Markierung (= inaktiv),
	 * wie jede neu importierte Anmeldung.
	 */
	@Override
	public void hebeAbmeldungAuf(int zeile1Basiert) {
		logger.debug("PTM-Online: Abmeldung der Mêlée-Zeile {} aufgehoben (keine Änderung nötig)", zeile1Basiert);
	}
}
