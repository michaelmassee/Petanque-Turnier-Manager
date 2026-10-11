/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.MeldelisteUuidIndex;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;
import de.petanqueturniermanager.basesheet.meldeliste.TeamUuidFormel;
import de.petanqueturniermanager.basesheet.meldeliste.TeamnummerAendernSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetTestDaten;

/**
 * Runde-1-Zellen des K.-o.-Baums tragen die UUID des Teams; die Sieger-Formeln reichen deren Anzeige weiter. Eine
 * geänderte Teamnummer erscheint daher im ganzen Baum, ohne dass sich Paarungen oder Sieger ändern.
 */
class KoTurnierbaumTeamnummerAendernUITest extends BaseCalcUITest {

	private static final int ANZ_TEAMS = 8;
	private static final int NEUE_NR = 42;

	@Test
	void geaenderteNummerErscheintInRunde1UndBeimSiegerDerFolgerunde() throws GenerateException {
		new KoKonfigurationSheet(wkingSpreadsheet).setSpielbaumTeamAnzeige(TeamAnzeige.NR);
		new KoMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).erstelleMeldelisteWithTestdaten();
		var turnierbaum = new KoTurnierbaumSheet(wkingSpreadsheet);
		turnierbaum.erstelleTurnierbaumOhneDialog();
		XSpreadsheet baum = ersterTurnierbaum();

		Position teamA = Position.from(turnierbaum.teamSpalte(1), turnierbaum.teamAZeile(1, 0));
		Position sieger = Position.from(turnierbaum.teamSpalte(2), turnierbaum.teamAZeile(2, 0));
		String uuidA = TeamUuidFormel.findeUuid(sheetHlp.getFormulaFromCell(baum, teamA)).orElseThrow();
		int alteNr = MeldelisteUuidIndex.lese(wkingSpreadsheet).nr(uuidA).orElseThrow();

		RangeData ergebnis = new RangeData();
		ergebnis.addNewRow(13);
		ergebnis.addNewRow(5);
		RangeHelper.from(baum, doc, ergebnis.getRangePosition(
				Position.from(turnierbaum.scoreSpalte(1), turnierbaum.teamAZeile(1, 0)))).setDataInRange(ergebnis);
		recalcAll();
		assertThat(sheetHlp.getTextFromCell(baum, sieger)).as("Vorbedingung: Team A zieht in Runde 2 ein")
				.isEqualTo(String.valueOf(alteNr));

		// Das Property schreibt sonst erst SheetRunner.run(); der Test ruft doRun() direkt.
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.KO.getId());
		new TeamnummerAendernSheet(wkingSpreadsheet).mitEingabe(alteNr, NEUE_NR).doRun();
		recalcAll();

		assertThat(sheetHlp.getTextFromCell(baum, teamA)).as("Runde 1 zeigt die neue Nummer")
				.isEqualTo(String.valueOf(NEUE_NR));
		assertThat(sheetHlp.getTextFromCell(baum, sieger)).as("Der Sieger in Runde 2 zeigt die neue Nummer")
				.isEqualTo(String.valueOf(NEUE_NR));
		assertThat(MeldelisteUuidIndex.lese(wkingSpreadsheet).nr(uuidA)).as("Die UUID bleibt beim Team")
				.hasValue(NEUE_NR);
	}

	private XSpreadsheet ersterTurnierbaum() {
		String praefix = SheetNamen.koTurnierbaumEinzel();
		String name = Arrays.stream(doc.getSheets().getElementNames()).filter(n -> n.startsWith(praefix))
				.findFirst().orElseThrow();
		return sheetHlp.findByName(name);
	}
}
