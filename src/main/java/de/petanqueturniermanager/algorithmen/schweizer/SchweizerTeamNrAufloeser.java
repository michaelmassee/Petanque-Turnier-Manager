/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.schweizer;

import java.util.Optional;
import java.util.function.Supplier;

import de.petanqueturniermanager.basesheet.meldeliste.MeldelisteUuidIndex;
import de.petanqueturniermanager.basesheet.meldeliste.TeamUuidFormel;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.schweizer.meldeliste.TeamAnzeigeIndex;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;

/**
 * Ermittelt die aktuelle Teamnummer einer Teamzelle der Spielrunde. Die Werte-Zeile muss ab
 * {@link SchweizerAbstractSpielrundeSheet#TEAM_A_SPALTE} bis
 * {@link SchweizerAbstractSpielrundeSheet#TECHNISCHE_TEAM_B_NR_SPALTE} gelesen sein, dazu die Formel der Teamzelle.
 * <p>
 * Reihenfolge der Auflösung:
 * <ol>
 * <li>UUID in der Formel der Teamzelle – über die Meldeliste auf die aktuelle Nummer abgebildet. Bleibt richtig,
 * wenn die Teamnummer geändert wird.</li>
 * <li>Sichtbare Zahl ohne UUID: Altbestand im Anzeigemodus Nummer oder manuelle Korrektur im Spielplan.</li>
 * <li>Versteckte technische Teamnummer (Altbestand mit Namensanzeige).</li>
 * <li>Sichtbare Kennung über die Meldeliste – nur für noch ältere Dateien. Der {@link TeamAnzeigeIndex} wird dafür
 * erst beim ersten Bedarf und nur einmal geladen.</li>
 * </ol>
 * Eine nicht auflösbare oder mehrdeutige Kennung führt zum Abbruch, weil sie sonst als Datenende bzw. Freilos
 * gewertet würde.
 */
public final class SchweizerTeamNrAufloeser {

	/** Leere Teamzelle: Datenende (Team A) bzw. Freilos (Team B). */
	public static final int KEIN_TEAM = 0;
	/** Die UUID der Teamzelle gehört zu keiner Meldung mehr (Meldung gelöscht). */
	public static final int NICHT_IN_MELDELISTE = -1;

	/** Lädt den Anzeige-Index aus der Meldeliste. */
	@FunctionalInterface
	public interface IndexQuelle {
		TeamAnzeigeIndex lade() throws GenerateException;
	}

	private static final int TEAM_A_IDX = 0;
	private static final int TEAM_B_IDX = SchweizerAbstractSpielrundeSheet.TEAM_B_SPALTE
			- SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE;
	private static final int TECHNISCH_A_IDX = SchweizerAbstractSpielrundeSheet.TECHNISCHE_TEAM_A_NR_SPALTE
			- SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE;
	private static final int TECHNISCH_B_IDX = SchweizerAbstractSpielrundeSheet.TECHNISCHE_TEAM_B_NR_SPALTE
			- SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE;

	private final IndexQuelle indexQuelle;
	private final Supplier<MeldelisteUuidIndex> uuidIndexQuelle;
	private TeamAnzeigeIndex index;
	private MeldelisteUuidIndex uuidIndex;

	public SchweizerTeamNrAufloeser(IndexQuelle indexQuelle, Supplier<MeldelisteUuidIndex> uuidIndexQuelle) {
		this.indexQuelle = indexQuelle;
		this.uuidIndexQuelle = uuidIndexQuelle;
	}

	/**
	 * @param formel Formel der Teamzelle A
	 * @return Teamnummer von Team A, {@link #KEIN_TEAM} wenn die Zeile leer ist (Datenende),
	 *         {@link #NICHT_IN_MELDELISTE} wenn die Meldung gelöscht wurde
	 */
	public int teamA(RowData zeile, String formel) throws GenerateException {
		return aufloesen(zeile, formel, TEAM_A_IDX, TECHNISCH_A_IDX);
	}

	/**
	 * @param formel Formel der Teamzelle B
	 * @return Teamnummer von Team B, {@link #KEIN_TEAM} wenn kein Gegner eingetragen ist (Freilos),
	 *         {@link #NICHT_IN_MELDELISTE} wenn die Meldung gelöscht wurde
	 */
	public int teamB(RowData zeile, String formel) throws GenerateException {
		return aufloesen(zeile, formel, TEAM_B_IDX, TECHNISCH_B_IDX);
	}

	private int aufloesen(RowData zeile, String formel, int anzeigeIdx, int technischIdx) throws GenerateException {
		Optional<String> uuid = TeamUuidFormel.findeUuid(formel);
		if (uuid.isPresent()) {
			return uuidIndex().nr(uuid.get()).orElse(NICHT_IN_MELDELISTE);
		}
		CellData anzeige = zelle(zeile, anzeigeIdx);
		int sichtbareNr = anzeige != null && anzeige.getData() instanceof Number ? anzeige.getIntVal(0) : 0;
		if (sichtbareNr > 0) {
			return sichtbareNr;
		}
		CellData technisch = zelle(zeile, technischIdx);
		int technischeNr = technisch != null ? technisch.getIntVal(0) : 0;
		if (technischeNr > 0) {
			return technischeNr;
		}
		String kennung = anzeige != null ? anzeige.getStringVal() : null;
		if (kennung == null || kennung.isBlank()) {
			return KEIN_TEAM;
		}
		return ausMeldeliste(kennung);
	}

	private MeldelisteUuidIndex uuidIndex() {
		if (uuidIndex == null) {
			uuidIndex = uuidIndexQuelle.get();
		}
		return uuidIndex;
	}

	private int ausMeldeliste(String kennung) throws GenerateException {
		if (index == null) {
			index = indexQuelle.lade();
		}
		if (index.istMehrdeutig(kennung)) {
			throw new GenerateException(I18n.get("schweizer.spielrunde.fehler.team.mehrdeutig", kennung));
		}
		int nr = index.teamNr(kennung);
		if (nr <= 0) {
			throw new GenerateException(I18n.get("schweizer.spielrunde.fehler.team.unbekannt", kennung));
		}
		return nr;
	}

	private static CellData zelle(RowData zeile, int idx) {
		return idx < zeile.size() ? zeile.get(idx) : null;
	}
}
