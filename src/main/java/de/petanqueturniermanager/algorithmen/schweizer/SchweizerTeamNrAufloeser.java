/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.schweizer;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.schweizer.meldeliste.TeamAnzeigeIndex;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;

/**
 * Ermittelt die Teamnummern einer Spielrunden-Zeile. Die Zeile muss ab
 * {@link SchweizerAbstractSpielrundeSheet#TEAM_A_SPALTE} bis
 * {@link SchweizerAbstractSpielrundeSheet#TECHNISCHE_TEAM_B_NR_SPALTE} gelesen sein.
 * <p>
 * Reihenfolge der Auflösung:
 * <ol>
 * <li>Sichtbare Zahl (Anzeigemodus Nummer) – sie ist dort die Identität, eine manuelle
 * Korrektur im Spielplan muss daher Vorrang vor der versteckten Nummer haben.</li>
 * <li>Versteckte technische Teamnummer (Namensanzeige per Formel).</li>
 * <li>Sichtbare Kennung über die Meldeliste – nur für alte Dateien ohne technische Spalten.
 * Der {@link TeamAnzeigeIndex} wird dafür erst beim ersten Bedarf und nur einmal geladen.</li>
 * </ol>
 * Eine nicht auflösbare oder mehrdeutige Kennung führt zum Abbruch, weil sie sonst als
 * Datenende bzw. Freilos gewertet würde.
 */
public final class SchweizerTeamNrAufloeser {

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
	private TeamAnzeigeIndex index;

	public SchweizerTeamNrAufloeser(IndexQuelle indexQuelle) {
		this.indexQuelle = indexQuelle;
	}

	/** @return Teamnummer von Team A, 0 wenn die Zeile leer ist (Datenende) */
	public int teamA(RowData zeile) throws GenerateException {
		return aufloesen(zeile, TEAM_A_IDX, TECHNISCH_A_IDX);
	}

	/** @return Teamnummer von Team B, 0 wenn kein Gegner eingetragen ist (Freilos) */
	public int teamB(RowData zeile) throws GenerateException {
		return aufloesen(zeile, TEAM_B_IDX, TECHNISCH_B_IDX);
	}

	private int aufloesen(RowData zeile, int anzeigeIdx, int technischIdx) throws GenerateException {
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
			return 0;
		}
		return ausMeldeliste(kennung);
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
