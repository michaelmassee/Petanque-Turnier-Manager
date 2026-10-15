/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.MeldelisteUuidIndex;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;
import de.petanqueturniermanager.basesheet.meldeliste.TeamUuidFormel;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.ko.KoTurnierbaumSheet;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetTestDaten;

/**
 * Im Namensmodus reichen die Sieger-Zellen des K.-o.-Baums nur den Namen weiter. Treffen zwei gleichnamige Teams im
 * Finale aufeinander, ordnet die Live-Übertragung sie trotzdem richtig zu: Sie folgt den Punkten bis zur UUID der
 * Setzliste ({@code KoWeitergabeFormel}) statt dem Namen.
 */
class TurnierbaumGleichnamigeTeamsUITest extends BaseCalcUITest {

	private static final int ANZ_TEAMS = 4;
	/** Namensspalten der KO-Meldeliste rechts der Nr-Spalte, die für die Anzeige gleichgezogen werden. */
	private static final int NAMEN_SPALTEN = 3;

	@Test
	void finaleZweierGleichnamigerTeamsWirdUeberDieUuidZugeordnet() throws GenerateException {
		new KoKonfigurationSheet(wkingSpreadsheet).setSpielbaumTeamAnzeige(TeamAnzeige.NAME);
		new KoMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).erstelleMeldelisteWithTestdaten();
		new KoTurnierbaumSheet(wkingSpreadsheet).erstelleTurnierbaumOhneDialog();
		XSpreadsheet baum = ersterTurnierbaum();
		// Ergebnis-Zellen je Partie (Team A, Team B), wie sie der Leser verwendet: zwei Halbfinale, dann das Finale.
		List<Position> punkte = TurnierbaumLiveLeser.scorePositionen(SheetMetadataHelper.leseScoreText(doc,
				SheetMetadataHelper.scoreSchluessel(SheetMetadataHelper.schluesselKoTurnierbaum(""))));
		assertThat(punkte).as("Vorbedingung: zwei Halbfinale und ein Finale").hasSizeGreaterThanOrEqualTo(6);

		String uuidA = uuidIn(baum, teamZelle(punkte.get(0)));
		String uuidC = uuidIn(baum, teamZelle(punkte.get(2)));
		gleicherNameWie(uuidA, uuidC);

		ergebnis(baum, punkte.get(0), 13, punkte.get(1), 5);
		ergebnis(baum, punkte.get(2), 13, punkte.get(3), 7);
		recalcAll();

		Position finaleA = teamZelle(punkte.get(4));
		Position finaleB = teamZelle(punkte.get(5));
		assertThat(sheetHlp.getTextFromCell(baum, finaleA)).as("Vorbedingung: gleichnamige Finalisten")
				.isNotBlank().isEqualTo(sheetHlp.getTextFromCell(baum, finaleB));

		List<List<LivePartie>> runden = TurnierbaumLiveLeser.leseRunden(wkingSpreadsheet,
				SheetMetadataHelper.schluesselKoTurnierbaum(""), null);
		MeldelisteUuidIndex index = MeldelisteUuidIndex.lese(wkingSpreadsheet);
		assertThat(runden.get(1)).as("Finale übertragen").first().satisfies(finale -> {
			assertThat(finale.teamA()).containsExactly(index.nr(uuidA).orElseThrow());
			assertThat(finale.teamB()).containsExactly(index.nr(uuidC).orElseThrow());
		});
	}

	private String uuidIn(XSpreadsheet baum, Position zelle) {
		return TeamUuidFormel.findeUuid(sheetHlp.getFormulaFromCell(baum, zelle)).orElseThrow();
	}

	/** Überschreibt die Namen des Teams {@code ziel} in der Meldeliste mit denen von {@code vorlage}. */
	private void gleicherNameWie(String vorlage, String ziel) throws GenerateException {
		XSpreadsheet meldeliste = sheetHlp.findByName(SheetNamen.meldeliste());
		String[][] nrFormeln = sheetHlp.getFormulaArrayFromRange(meldeliste, RangePosition.from(0, 0, 0, 998));
		int vorlageZeile = zeileMit(nrFormeln, vorlage);
		int zielZeile = zeileMit(nrFormeln, ziel);
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

	/** Die Team-Zelle steht links neben der Ergebnis-Zelle. */
	private static Position teamZelle(Position punkte) {
		return Position.from(punkte.getSpalte() - 1, punkte.getZeile());
	}

	private void ergebnis(XSpreadsheet baum, Position zelleA, int punkteA, Position zelleB, int punkteB)
			throws GenerateException {
		schreibePunkte(baum, zelleA, punkteA);
		schreibePunkte(baum, zelleB, punkteB);
	}

	private void schreibePunkte(XSpreadsheet baum, Position zelle, int punkte) throws GenerateException {
		RangeData wert = new RangeData();
		wert.addNewRow(punkte);
		RangeHelper.from(baum, doc, wert.getRangePosition(zelle)).setDataInRange(wert);
	}

	private XSpreadsheet ersterTurnierbaum() {
		String praefix = SheetNamen.koTurnierbaumEinzel();
		String name = Arrays.stream(doc.getSheets().getElementNames()).filter(n -> n.startsWith(praefix))
				.findFirst().orElseThrow();
		return sheetHlp.findByName(name);
	}
}
