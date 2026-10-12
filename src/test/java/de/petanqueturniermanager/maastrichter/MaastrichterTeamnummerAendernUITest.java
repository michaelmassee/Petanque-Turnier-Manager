/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.MeldelisteUuidIndex;
import de.petanqueturniermanager.basesheet.meldeliste.TeamnummerAendernSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterGruppenSpalteHelper;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterVorrundenRanglisteSheetUpdate;

/**
 * Die KO-Gruppe eines Teams hängt in der Vorrunden-Rangliste an der Teamnummer. Tauschen zwei Teams aus
 * verschiedenen Gruppen per „Teamnummer ändern“ die Nummern, behält jedes Team seine Gruppe – auch nach dem
 * Neuaufbau der Rangliste.
 */
public class MaastrichterTeamnummerAendernUITest extends BaseCalcUITest {

	private static final int ANZ_TEAMS = 12;
	private static final int ANZ_VORRUNDEN = 3;
	/** Vier Teams je KO-Gruppe: mehrere Gruppen, zwischen denen getauscht werden kann. */
	private static final int GRUPPEN_GROESSE = 4;

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
	public void tauschBehaeltDieKoGruppeJeTeam() throws GenerateException {
		new MaastrichterTurnierTestDaten(wkingSpreadsheet, ANZ_TEAMS, ANZ_VORRUNDEN, GRUPPEN_GROESSE).generate();
		Map<Integer, String> gruppeProNr = gruppen();
		int nrA = gruppeProNr.keySet().stream().filter(nr -> istKoGruppe(gruppeProNr.get(nr))).findFirst()
				.orElseThrow();
		int nrB = gruppeProNr.keySet().stream()
				.filter(nr -> istKoGruppe(gruppeProNr.get(nr)) && !gruppeProNr.get(nr).equals(gruppeProNr.get(nrA)))
				.findFirst().orElseThrow();
		Map<String, String> gruppeProUuidVorher = gruppeProUuid(gruppeProNr);

		// Das Property schreibt sonst erst SheetRunner.run(); der Test ruft doRun() direkt.
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
				TurnierSystem.MAASTRICHTER.getId());
		new TeamnummerAendernSheet(wkingSpreadsheet).mitEingabe(nrA, nrB).doRun();
		recalcAll();

		assertThat(gruppeProUuid(gruppen())).as("Gruppe je Team direkt nach dem Tausch")
				.isEqualTo(gruppeProUuidVorher);
		new MaastrichterVorrundenRanglisteSheetUpdate(wkingSpreadsheet).doRun();
		assertThat(gruppeProUuid(gruppen())).as("Gruppe je Team nach dem Neuaufbau der Rangliste")
				.isEqualTo(gruppeProUuidVorher);
	}

	private Map<Integer, String> gruppen() {
		return MaastrichterGruppenSpalteHelper
				.lesePreservedGruppen(new MaastrichterVorrundenRanglisteSheetUpdate(wkingSpreadsheet));
	}

	private Map<String, String> gruppeProUuid(Map<Integer, String> gruppeProNr) {
		MeldelisteUuidIndex index = MeldelisteUuidIndex.lese(wkingSpreadsheet);
		Map<String, String> ergebnis = new HashMap<>();
		gruppeProNr.forEach((nr, gruppe) -> ergebnis.put(index.uuid(nr).orElseThrow(), gruppe));
		return ergebnis;
	}

	private static boolean istKoGruppe(String gruppe) {
		return !gruppe.equals(MaastrichterGruppenSpalteHelper.keinKoMarker());
	}
}
