package de.petanqueturniermanager.basesheet.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sun.star.beans.Property;
import com.sun.star.beans.XPropertySet;
import com.sun.star.document.XDocumentPropertiesSupplier;
import com.sun.star.util.XModifiable;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetNew;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_New;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeMeldeListeSheetNew;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetNew;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetNew;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetNew;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;

/**
 * Bricht der Anwender den Start-Dialog eines Turniersystems ab, darf das Dokument in keiner Weise
 * verändert werden: keine neuen oder gelöschten Blätter, keine neuen oder geänderten
 * Dokument-Properties (auch keine beim Lesen der Vorbelegung angelegten Defaults) und kein
 * Modified-Flag.
 */
class StartDialogAbbrechenUITest extends BaseCalcUITest {

	static Stream<Arguments> startRunner() {
		return Stream.<Function<WorkingSpreadsheet, SheetRunner>>of(SchweizerMeldeListeSheetNew::new,
				MaastrichterMeldeListeSheetNew::new, KoMeldeListeSheetNew::new, JGJMeldeListeSheet_New::new,
				PouleMeldeListeSheetNew::new, FormuleXMeldeListeSheetNew::new, KaskadeMeldeListeSheetNew::new)
				.map(Arguments::of);
	}

	@ParameterizedTest
	@MethodSource("startRunner")
	void abbrechenLaesstDokumentUnveraendert(Function<WorkingSpreadsheet, SheetRunner> runnerFabrik) throws Exception {
		XModifiable modifiable = Lo.qi(XModifiable.class, doc);
		modifiable.setModified(false);
		List<String> blaetterVorher = List.of(doc.getSheets().getElementNames());
		Map<String, Object> propertiesVorher = benutzerdefinierteProperties();

		SheetRunner runner = runnerFabrik.apply(wkingSpreadsheet);
		TurnierStartDialog.mitSimuliertemAbbrechen(runner::run);

		assertThat(runner.isLetzterLaufFehlgeschlagen()).as("Lauf ohne Fehler").isFalse();
		assertThat(List.of(doc.getSheets().getElementNames())).as("Blätter").isEqualTo(blaetterVorher);
		assertThat(benutzerdefinierteProperties()).as("Dokument-Properties").isEqualTo(propertiesVorher);
		assertThat(modifiable.isModified()).as("Modified-Flag").isFalse();
	}

	private Map<String, Object> benutzerdefinierteProperties() throws Exception {
		XPropertySet props = Lo.qi(XPropertySet.class, Lo.qi(XDocumentPropertiesSupplier.class, doc)
				.getDocumentProperties().getUserDefinedProperties());
		Map<String, Object> werte = new TreeMap<>();
		for (Property property : props.getPropertySetInfo().getProperties()) {
			werte.put(property.Name, props.getPropertyValue(property.Name));
		}
		return werte;
	}
}
