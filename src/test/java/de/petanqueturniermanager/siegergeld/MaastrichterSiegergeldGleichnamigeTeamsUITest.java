/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.siegergeld;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.MeldelisteUuidIndex;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;
import de.petanqueturniermanager.basesheet.meldeliste.TeamUuidFormel;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.ko.KoTurnierbaumLayout;
import de.petanqueturniermanager.maastrichter.MaastrichterTurnierTestDaten;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;

/**
 * Im Namensmodus reicht das Maastrichter-Finale nur Namen weiter. Bekommt der Zweite denselben Namen wie der Sieger,
 * ordnet das Siegergeld beide Plätze trotzdem den richtigen Teams zu: über die UUID der Setzliste, der die
 * Weitergabe-Formeln anhand der Punkte folgen, statt über den Namen.
 */
class MaastrichterSiegergeldGleichnamigeTeamsUITest extends BaseCalcUITest {

	private static final int ANZ_TEAMS = 8;
	private static final int ANZ_VORRUNDEN = 3;
	/** Namensspalten der Meldeliste rechts der Nr-Spalte, die für die Anzeige gleichgezogen werden. */
	private static final int NAMEN_SPALTEN = 3;

	@BeforeEach
	@Override
	public void beforeTest() {
		super.beforeTest();
		RandomSource.setSeed(42L);
	}

	@AfterEach
	public void resetRandom() {
		RandomSource.reset();
	}

	@Test
	void plaetzeBleibenBeiGleichnamigenFinalisten() throws GenerateException {
		MaastrichterKonfigurationSheet konfiguration = new MaastrichterKonfigurationSheet(wkingSpreadsheet);
		konfiguration.setSpielbaumTeamAnzeige(TeamAnzeige.NAME);
		new MaastrichterTurnierTestDaten(wkingSpreadsheet, ANZ_TEAMS, ANZ_VORRUNDEN, ANZ_TEAMS).generate();
		assertThat(konfiguration.getSpielbaumTeamAnzeige()).as("Vorbedingung: Finale im Namensmodus")
				.isEqualTo(TeamAnzeige.NAME);

		XSpreadsheet finale = sheetHlp.findByName(SheetNamen.koFinaleGruppe("A"));
		KoTurnierbaumLayout layout = KoTurnierbaumLayout.from(ANZ_TEAMS,
				konfiguration.getSpielbaumSpielbahn() != SpielrundeSpielbahn.X,
				konfiguration.isSpielbaumBahnNurRunde1(), true, konfiguration.isSpielbaumSpielUmPlatz3());
		alleSpieleGewinntTeamA(finale, layout);
		recalcAll();

		var quelle = new MaastrichterSiegergeldQuelle(wkingSpreadsheet);
		List<SiegergeldEintrag> vorher = quelle.leseTop(2);
		assertThat(vorher).as("Vorbedingung: Sieger und Zweiter").hasSize(2);

		gleicherNameWie(vorher.get(0).nr(), vorher.get(1).nr());
		recalcAll();
		Position finalistA = Position.from(layout.teamSpalte(layout.numRunden()), layout.finaleTeamAZeile());
		Position finalistB = Position.from(layout.teamSpalte(layout.numRunden()), layout.finaleTeamBZeile());
		assertThat(sheetHlp.getTextFromCell(finale, finalistA)).as("Vorbedingung: gleichnamige Finalisten")
				.isNotBlank().isEqualTo(sheetHlp.getTextFromCell(finale, finalistB));

		List<SiegergeldEintrag> nachher = quelle.leseTop(2);
		assertThat(nachher).extracting(SiegergeldEintrag::nr)
				.containsExactly(vorher.get(0).nr(), vorher.get(1).nr());
	}

	/**
	 * In jeder Partie gewinnt Team A 13:5; die Weitergabe-Formeln rechnen den Baum danach selbst durch. Je Runde ein
	 * Block über die Ergebnisspalte, Zeilen zwischen den Partien bleiben leer.
	 */
	private void alleSpieleGewinntTeamA(XSpreadsheet finale, KoTurnierbaumLayout layout) throws GenerateException {
		for (int runde = 1; runde <= layout.numRunden(); runde++) {
			int letztesSpiel = (1 << (layout.numRunden() - runde)) - 1;
			int ersteZeile = layout.teamAZeile(runde, 0);
			RangeData punkte = new RangeData();
			for (int zeile = ersteZeile; zeile <= layout.teamBZeile(runde, letztesSpiel); zeile++) {
				punkte.addNewRow().newEmpty();
			}
			for (int spiel = 0; spiel <= letztesSpiel; spiel++) {
				punkte.set(layout.teamAZeile(runde, spiel) - ersteZeile, new RowData(13));
				punkte.set(layout.teamBZeile(runde, spiel) - ersteZeile, new RowData(5));
			}
			RangeHelper.from(finale, doc, punkte.getRangePosition(Position.from(layout.scoreSpalte(runde), ersteZeile)))
					.setDataInRange(punkte);
		}
	}

	/** Überschreibt die Namen des Teams {@code zielNr} in der Meldeliste mit denen von {@code vorlageNr}. */
	private void gleicherNameWie(int vorlageNr, int zielNr) throws GenerateException {
		MeldelisteUuidIndex index = MeldelisteUuidIndex.lese(wkingSpreadsheet);
		XSpreadsheet meldeliste = sheetHlp.findByName(SheetNamen.meldeliste());
		String[][] nrFormeln = sheetHlp.getFormulaArrayFromRange(meldeliste, RangePosition.from(0, 0, 0, 998));
		int vorlageZeile = zeileMit(nrFormeln, index.uuid(vorlageNr).orElseThrow());
		int zielZeile = zeileMit(nrFormeln, index.uuid(zielNr).orElseThrow());
		RangeData namen = RangeHelper.from(meldeliste, doc,
				RangePosition.from(1, vorlageZeile, NAMEN_SPALTEN, vorlageZeile)).getDataFromRange();
		RangeHelper.from(meldeliste, doc, namen.getRangePosition(Position.from(1, zielZeile))).setDataInRange(namen);
	}

	private static int zeileMit(String[][] nrFormeln, String uuid) {
		for (int zeile = 0; zeile < nrFormeln.length; zeile++) {
			if (TeamUuidFormel.findeUuid(nrFormeln[zeile][0]).filter(uuid::equals).isPresent()) {
				return zeile;
			}
		}
		throw new IllegalStateException("UUID nicht in der Meldeliste: " + uuid);
	}
}
