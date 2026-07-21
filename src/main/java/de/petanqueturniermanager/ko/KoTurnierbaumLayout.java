/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko;

/**
 * Reine Layout-Berechnung für die sichtbaren Zellen eines KO-Turnierbaums.
 * <p>
 * Die Formeln entsprechen {@link KoTurnierbaumSheet}; separate Ausleser können damit bestehende
 * Turnierbaum-Sheets interpretieren, ohne den SheetRunner-State des Generators zu benutzen.
 */
public final class KoTurnierbaumLayout {

	private static final int ERSTE_ZEILE_BASE = 2;

	private final int teamCount;
	private final boolean mitBahn;
	private final boolean bahnNurRunde1;
	private final boolean spielUmPlatz3;
	private final int bracketGroesse;
	private final int numRunden;
	private final boolean mitCadrage;
	private final int ersteZeile;
	private final int runde1SlotAbstand;
	private final int runde1MatchZeilenAbstand;

	private KoTurnierbaumLayout(int teamCount, boolean mitBahn, boolean bahnNurRunde1,
			boolean mitGruppenHeader, boolean spielUmPlatz3) {
		this.teamCount = teamCount;
		this.mitBahn = mitBahn;
		this.bahnNurRunde1 = bahnNurRunde1;
		this.spielUmPlatz3 = spielUmPlatz3;
		this.bracketGroesse = KoTurnierbaumSheet.berechneBracketGroesse(teamCount);
		this.numRunden = Integer.numberOfTrailingZeros(bracketGroesse);
		this.mitCadrage = teamCount > bracketGroesse;
		this.ersteZeile = ERSTE_ZEILE_BASE + (mitGruppenHeader ? 1 : 0);
		this.runde1SlotAbstand = KoTurnierbaumSheet.berechneRunde1SlotAbstand(teamCount, bracketGroesse);
		this.runde1MatchZeilenAbstand = KoTurnierbaumSheet.berechneRunde1MatchZeilenAbstand(teamCount, bracketGroesse);
	}

	public static KoTurnierbaumLayout from(int teamCount, boolean mitBahn, boolean bahnNurRunde1,
			boolean mitGruppenHeader, boolean spielUmPlatz3) {
		return new KoTurnierbaumLayout(teamCount, mitBahn, bahnNurRunde1, mitGruppenHeader, spielUmPlatz3);
	}

	public int numRunden() {
		return numRunden;
	}

	public int teamAZeile(int runde, int match) {
		if (runde == 1) {
			return match * runde1MatchZeilenAbstand + ersteZeile;
		}
		return teamBZeile(runde - 1, 2 * match);
	}

	public int teamBZeile(int runde, int match) {
		if (runde == 1) {
			return match * runde1MatchZeilenAbstand + ersteZeile + runde1SlotAbstand;
		}
		return teamAZeile(runde - 1, 2 * match + 1);
	}

	public int teamSpalte(int runde) {
		return rundenStartSpalte(runde) + (mitBahnInRunde(runde) ? 1 : 0);
	}

	public int scoreSpalte(int runde) {
		return teamSpalte(runde) + 1;
	}

	public int siegerSpalte() {
		return rundenStartSpalte(numRunden) + hauptRundenSpalten(numRunden);
	}

	public int siegerNameSpalte() {
		return siegerSpalte() + 1;
	}

	public int finaleTeamAZeile() {
		return teamAZeile(numRunden, 0);
	}

	public int finaleTeamBZeile() {
		return teamBZeile(numRunden, 0);
	}

	public int siegerZeile() {
		return (finaleTeamAZeile() + finaleTeamBZeile()) / 2;
	}

	public boolean hatSpielUmPlatz3() {
		return spielUmPlatz3 && numRunden >= 2;
	}

	public int platz3TeamAZeile() {
		return platz3HeaderZeile() + 1;
	}

	public int platz3TeamBZeile() {
		return platz3HeaderZeile() + 2;
	}

	public int platz3SiegerZeile() {
		return (platz3TeamAZeile() + platz3TeamBZeile()) / 2;
	}

	private int platz3HeaderZeile() {
		int anzMatchesR1 = bracketGroesse / 2;
		return teamBZeile(1, anzMatchesR1 - 1) + 3;
	}

	private int rundenStartSpalte(int runde) {
		int spalte = mitCadrage ? cadrageSpalten() : 0;
		for (int r = 1; r < runde; r++) {
			spalte += hauptRundenSpalten(r);
		}
		return spalte;
	}

	private int cadrageSpalten() {
		return spaltenProRunde(mitBahnInCadrage());
	}

	private int hauptRundenSpalten(int runde) {
		return spaltenProRunde(mitBahnInRunde(runde));
	}

	private boolean mitBahnInRunde(int runde) {
		return mitBahn && (!bahnNurRunde1 || (!mitCadrage && runde == 1));
	}

	private boolean mitBahnInCadrage() {
		return mitCadrage && mitBahn;
	}

	private static int spaltenProRunde(boolean mitBahnSpalte) {
		return mitBahnSpalte ? 4 : 3;
	}

	@Override
	public String toString() {
		return "KoTurnierbaumLayout{"
				+ "teamCount=" + teamCount
				+ ", bracketGroesse=" + bracketGroesse
				+ ", numRunden=" + numRunden
				+ ", mitCadrage=" + mitCadrage
				+ '}';
	}
}
