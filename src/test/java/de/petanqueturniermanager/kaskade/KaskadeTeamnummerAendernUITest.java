/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.kaskade;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.MeldelisteUuidIndex;
import de.petanqueturniermanager.basesheet.meldeliste.TeamUuidFormel;
import de.petanqueturniermanager.basesheet.meldeliste.TeamnummerAendernSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.kaskade.spielrunde.KaskadeSpielrundeSheet;

/**
 * Teamzellen der Kaskadenrunden tragen die UUID des Teams und zeigen die aktuelle Nummer. Nach „Teamnummer ändern“
 * zeigt die Runde die neue Nummer, und die Folgerunde führt den Sieger mit seiner UUID weiter.
 */
public class KaskadeTeamnummerAendernUITest extends BaseCalcUITest {

	private static final int ANZ_TEAMS = 8;
	private static final int ANZ_KASKADEN = 2;
	private static final int NEUE_NR = 42;

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
	public void folgerundeFuehrtDenSiegerMitNeuerNummerWeiter() throws GenerateException {
		var testDaten = new KaskadeTurnierTestDaten(wkingSpreadsheet, ANZ_TEAMS, ANZ_KASKADEN);
		testDaten.generate(1, false);
		XSpreadsheet runde1 = runde(1);
		int zeile = KaskadeSpielrundeSheet.ERSTE_DATEN_ZEILE;
		RangeData ergebnis = RangeHelper.from(runde1, doc, RangePosition.from(KaskadeSpielrundeSheet.ERG_TEAM_A_SPALTE,
				zeile, KaskadeSpielrundeSheet.ERG_TEAM_B_SPALTE, zeile)).getDataFromRange();
		boolean teamAGewinnt = ergebnis.get(0).get(0).getIntVal(0) > ergebnis.get(0).get(1).getIntVal(0);
		Position sieger = Position.from(
				teamAGewinnt ? KaskadeSpielrundeSheet.TEAM_A_SPALTE : KaskadeSpielrundeSheet.TEAM_B_SPALTE, zeile);
		String uuidSieger = TeamUuidFormel.findeUuid(sheetHlp.getFormulaFromCell(runde1, sieger)).orElseThrow();
		int alteNr = MeldelisteUuidIndex.lese(wkingSpreadsheet).nr(uuidSieger).orElseThrow();

		// Das Property schreibt sonst erst SheetRunner.run(); der Test ruft doRun() direkt.
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
				TurnierSystem.KASKADE.getId());
		new TeamnummerAendernSheet(wkingSpreadsheet).mitEingabe(alteNr, NEUE_NR).doRun();
		recalcAll();
		assertThat(sheetHlp.getTextFromCell(runde1, sieger)).as("Runde 1 zeigt die neue Nummer")
				.isEqualTo(String.valueOf(NEUE_NR));

		testDaten.spielrundeSheet.naechsteRunde();
		XSpreadsheet runde2 = runde(2);
		Position siegerInRunde2 = findeTeamZelle(runde2, uuidSieger)
				.orElseThrow(() -> new AssertionError("Sieger fehlt in Runde 2"));
		assertThat(sheetHlp.getTextFromCell(runde2, siegerInRunde2)).as("Runde 2 zeigt die neue Nummer")
				.isEqualTo(String.valueOf(NEUE_NR));
	}

	private XSpreadsheet runde(int rundeNr) {
		return SheetMetadataHelper.findeSheetUndHeile(doc, SheetMetadataHelper.schluesselKaskadenRunde(rundeNr),
				SheetNamen.kaskadenRunde(rundeNr));
	}

	private Optional<Position> findeTeamZelle(XSpreadsheet runde, String uuid) {
		int ersteZeile = KaskadeSpielrundeSheet.ERSTE_DATEN_ZEILE;
		String[][] formeln = sheetHlp.getFormulaArrayFromRange(runde, RangePosition.from(
				KaskadeSpielrundeSheet.TEAM_A_SPALTE, ersteZeile, KaskadeSpielrundeSheet.TEAM_B_SPALTE,
				ersteZeile + ANZ_TEAMS));
		for (int z = 0; z < formeln.length; z++) {
			for (int s = 0; s < formeln[z].length; s++) {
				if (TeamUuidFormel.findeUuid(formeln[z][s]).filter(uuid::equals).isPresent()) {
					return Optional.of(Position.from(KaskadeSpielrundeSheet.TEAM_A_SPALTE + s, ersteZeile + z));
				}
			}
		}
		return Optional.empty();
	}
}
