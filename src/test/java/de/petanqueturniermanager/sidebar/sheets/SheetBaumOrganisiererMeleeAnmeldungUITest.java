/*
 * Erstellung : 2026 / Michael Massee
 **/

package de.petanqueturniermanager.sidebar.sheets;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetNew;
import de.petanqueturniermanager.poule.meldeliste.PouleMeleeAnmeldungSheet;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeleeAnmeldungSheet;
import de.petanqueturniermanager.sidebar.sheets.BlattBaumEintrag.BlattKnoten;

/**
 * Regression: das Mêlée-Anmeldung-Sheet war in keiner {@link SheetGruppe}-Präfixliste enthalten
 * und fiel dadurch über {@link SheetGruppe#fuerSchluessel(String)} auf {@code ALLGEMEIN} zurück –
 * die systemspezifischen Baum-Aufbaumethoden in {@link SheetBaumOrganisierer} kennen dessen
 * Schlüssel gar nicht und ließen es komplett aus der Sidebar-Baumliste verschwinden.
 * <p>
 * Fachlich soll die Mêlée-Anmeldung als Vorstufe direkt <b>vor</b> der Meldeliste ihres Systems
 * erscheinen.
 */
class SheetBaumOrganisiererMeleeAnmeldungUITest extends BaseCalcUITest {

	private final SheetBaumOrganisierer organisierer = new SheetBaumOrganisierer();

	@Test
	void meleeAnmeldungErscheintVorDerMeldelisteSchweizer() throws Exception {
		new SchweizerMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams(Formation.DOUBLETTE, false,
				false);
		docPropHelper.setBooleanProperty(BasePropertiesSpalte.KONFIG_PROP_MELEE_ANMELDUNG, true);
		new SchweizerMeleeAnmeldungSheet(wkingSpreadsheet).generate();

		var eintraege = organisierer.baumAufbauen(doc, Set.of(), Set.of(), Set.of());
		var schluesselReihenfolge = schluessel(eintraege);

		int meleeIdx = schluesselReihenfolge.indexOf(SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELEE_ANMELDUNG);
		int meldelisteIdx = schluesselReihenfolge.indexOf(SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELDELISTE);

		assertThat(meleeIdx).as("Mêlée-Anmeldung muss in der Baumliste vorhanden sein").isNotEqualTo(-1);
		assertThat(meleeIdx).as("Mêlée-Anmeldung muss vor der Meldeliste stehen").isLessThan(meldelisteIdx);
	}

	/**
	 * Poule hat zusätzlich eine "Vorrunde"-Untergruppe, die per Negativ-Filter alles außer
	 * Meldeliste/Checkin-Liste/KO-Blätter einsammelt – ohne explizite Ausnahme würde die
	 * Mêlée-Anmeldung dort ein zweites Mal auftauchen.
	 */
	@Test
	void meleeAnmeldungErscheintNurEinmalUndVorDerMeldelistePoule() throws Exception {
		new PouleMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams(Formation.DOUBLETTE, false, false);
		docPropHelper.setBooleanProperty(BasePropertiesSpalte.KONFIG_PROP_MELEE_ANMELDUNG, true);
		new PouleMeleeAnmeldungSheet(wkingSpreadsheet).generate();

		var eintraege = organisierer.baumAufbauen(doc, Set.of(), Set.of(), Set.of());
		var schluesselReihenfolge = schluessel(eintraege);

		assertThat(schluesselReihenfolge)
				.as("Mêlée-Anmeldung darf nur ein einziges Mal auftauchen (keine Dopplung in der Vorrunde-Gruppe)")
				.filteredOn(SheetMetadataHelper.SCHLUESSEL_POULE_MELEE_ANMELDUNG::equals)
				.hasSize(1);

		int meleeIdx = schluesselReihenfolge.indexOf(SheetMetadataHelper.SCHLUESSEL_POULE_MELEE_ANMELDUNG);
		int meldelisteIdx = schluesselReihenfolge.indexOf(SheetMetadataHelper.SCHLUESSEL_POULE_MELDELISTE);
		assertThat(meleeIdx).as("Mêlée-Anmeldung muss vor der Meldeliste stehen").isLessThan(meldelisteIdx);
	}

	private static List<String> schluessel(List<BlattBaumEintrag> eintraege) {
		return eintraege.stream()
				.filter(BlattKnoten.class::isInstance)
				.map(BlattKnoten.class::cast)
				.map(BlattKnoten::metadatenSchluessel)
				.toList();
	}
}
