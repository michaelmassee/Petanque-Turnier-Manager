package de.petanqueturniermanager.addins;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.star.awt.XTopWindow;
import com.sun.star.frame.XController;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.sheet.XSpreadsheetDocument;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.OfficeDocumentHelper;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleePropertiesSpalte;

/**
 * Regressionstest für den Spieltag-Property-Leak zwischen zwei gleichzeitig offenen
 * Turnier-Dokumenten. Bug: Wird zu einem bestehenden Turnier (Dokument A, Spieltag=2) über
 * die Toolbar ein neues, leeres Turnier in einer neuen Datei gestartet (Dokument B), liefert
 * {@code PTM.ALG.INTPROPERTY("Spieltag")} in B fälschlich A's Wert statt B's eigenem.
 */
public class GlobalImplMehrDokumenteUITest extends BaseCalcUITest {

	private XSpreadsheetDocument zweitesDokument;

	@AfterEach
	public void schliesseZweitesDokument() {
		if (zweitesDokument != null) {
			OfficeDocumentHelper.closeDoc(zweitesDokument);
			zweitesDokument = null;
		}
	}

	/**
	 * GlobalImpl.getDocumentPropertiesHelper() fällt außerhalb des onLoad-Fensters (in dem
	 * GlobalImpl.mitDokumentKontext() das ThreadLocal setzt) auf
	 * DocumentHelper.getCurrentSpreadsheetDocument() zurück, die fokus-basiert über
	 * Desktop.getCurrentComponent() auflöst. Hält Dokument A zum Auswertungszeitpunkt noch
	 * den Fokus, liest PTM.ALG.INTPROPERTY(...) A's Property statt der des eigentlich
	 * gemeinten Dokuments B.
	 * <p>
	 * Hinweis zur Testtechnik: Der Wert wird bewusst per direktem Java-Aufruf auf einer
	 * eigenen {@code GlobalImpl}-Instanz gelesen (wie auch die anderen PTM.ALG.*-Tests in
	 * {@link GlobalImplUITest}), nicht über eine in eine Zelle geschriebene Calc-Formel: Wie
	 * {@link BaseCalcUITest}'s Klassen-Javadoc dokumentiert, hat das eigentliche AddIn
	 * innerhalb des LibreOffice-Prozesses einen eigenen, vom Testprozess entkoppelten
	 * Property-Cache ({@code DocumentPropertiesHelper.PROPLISTE}) -- über Socket geschriebene
	 * Zellformeln liefern dadurch keine verlässlichen Werte für einen Value-Vergleich. Der
	 * direkte Aufruf prüft exakt dieselbe Auflösungslogik ({@code getDocumentPropertiesHelper()}
	 * / {@code DocumentHelper.getCurrentSpreadsheetDocument()}), ohne von dieser Testharness-
	 * Einschränkung betroffen zu sein.
	 */
	@Test
	public void intPropertyLiestNichtVonFokussiertemAltdokument() {
		// Dokument A ("altes" Turnier): Spieltag = 2
		docPropHelper.setIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG, 2);
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
				TurnierSystem.SUPERMELEE.getId());

		// Dokument B ("neues" Turnier): eigener Spieltag = 1 -- exakt der Produktionspfad aus
		// TurnierSystemNeueDateiAuswahlDialog.beiOkGeklickt() (OfficeDocumentHelper.createSichtbaresCalc()).
		zweitesDokument = OfficeDocumentHelper.from(loader).createSichtbaresCalc();
		assertThat(zweitesDokument).as("zweites Dokument konnte nicht erstellt werden").isNotNull();

		WorkingSpreadsheet zweitesWs = new WorkingSpreadsheet(starter.getxComponentContext(), zweitesDokument);
		DocumentPropertiesHelper zweiterDocPropHelper = new DocumentPropertiesHelper(zweitesWs);

		zweiterDocPropHelper.setIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG, 1);
		zweiterDocPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
				TurnierSystem.SUPERMELEE.getId());

		// Fokus explizit zurück auf Dokument A legen -- erzwingt deterministisch das
		// Race-Fenster aus der Produktion: der Aufrufkontext "gehört" zu B (analog zum
		// SheetRunner-Hintergrundthread, der B's Meldeliste aufbaut), aber A gilt noch als
		// "current component".
		fokussiere(doc);

		GlobalImpl impl = new GlobalImpl(starter.getxComponentContext());
		int spieltagWert = impl.ptmintproperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG);

		assertThat(spieltagWert)
				.as("PTM.ALG.INTPROPERTY(\"Spieltag\") muss den Wert des gemeinten Dokuments B liefern (1), "
						+ "nicht den des noch fokussierten Altdokuments A (2)")
				.isEqualTo(1);
	}

	/** Gleiche Fokus-Sequenz wie {@code TurnierSystemNeueDateiAuswahlDialog.fokussiereDokument()}. */
	private static void fokussiere(XSpreadsheetDocument dokument) {
		XModel xModel = Lo.qi(XModel.class, dokument);
		XController controller = xModel.getCurrentController();
		XFrame frame = controller.getFrame();
		frame.activate();
		XTopWindow topWindow = Lo.qi(XTopWindow.class, frame.getContainerWindow());
		if (topWindow != null) {
			topWindow.toFront();
		}
	}
}
