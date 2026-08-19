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
import de.petanqueturniermanager.comp.DokumentKontext;
import de.petanqueturniermanager.comp.OfficeDocumentHelper;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleePropertiesSpalte;

/**
 * Regressionstest für den Spieltag-Property-Leak zwischen zwei gleichzeitig offenen
 * Turnier-Dokumenten. Bug: Wird zu einem bestehenden Turnier (Dokument A, Spieltag=2) über
 * die Toolbar ein neues, leeres Turnier in einer neuen Datei gestartet (Dokument B), liefert
 * {@code PTM.ALG.INTPROPERTY("Spieltag")} in B fälschlich A's Wert statt B's eigenem.
 * <p>
 * Fix: {@code SheetRunner.run()} umschließt {@code doRun()} jetzt mit
 * {@link DokumentKontext#mitKontextWerfend}, sodass {@code GlobalImpl.getDocumentPropertiesHelper()}
 * das jeweils eigene Dokument liest statt fokus-basiert über
 * {@code DocumentHelper.getCurrentSpreadsheetDocument()} zurückzufallen.
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
	 * GlobalImpl.getDocumentPropertiesHelper() liest vorrangig {@link DokumentKontext#get()} und
	 * fällt nur ohne gesetzten Kontext auf das fokus-basierte
	 * {@code DocumentHelper.getCurrentSpreadsheetDocument()} zurück. {@code SheetRunner.run()}
	 * setzt diesen Kontext jetzt auf das eigene Dokument, bevor {@code doRun()} (und damit z.B.
	 * {@code MeldeListeSheet_New} beim Anlegen eines neuen Turniers) läuft -- unabhängig davon,
	 * welches Dokument gerade den UI-Fokus hält. Dieser Test stellt exakt die Produktionssituation
	 * nach ("Dokument B wird gerade aufgebaut, Dokument A hat noch den Fokus") und prüft, dass
	 * {@code PTM.ALG.INTPROPERTY(...)} dabei B's eigenen Wert liefert statt A's.
	 * <p>
	 * Hinweis zur Testtechnik: Der Wert wird bewusst per direktem Java-Aufruf auf einer
	 * eigenen {@code GlobalImpl}-Instanz gelesen (wie auch die anderen PTM.ALG.*-Tests in
	 * {@link GlobalImplUITest}), nicht über eine in eine Zelle geschriebene Calc-Formel: Wie
	 * {@link BaseCalcUITest}'s Klassen-Javadoc dokumentiert, hat das eigentliche AddIn
	 * innerhalb des LibreOffice-Prozesses einen eigenen, vom Testprozess entkoppelten
	 * Property-Cache ({@code DocumentPropertiesHelper.PROPLISTE}) -- über Socket geschriebene
	 * Zellformeln liefern dadurch keine verlässlichen Werte für einen Value-Vergleich. Der
	 * direkte Aufruf prüft exakt dieselbe Auflösungslogik.
	 */
	@Test
	public void intPropertyLiestEigenesDokumentTrotzFokussiertemAltdokument() throws GenerateException {
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
		int[] spieltagWert = new int[1];
		// Simuliert, was SheetRunner.run() jetzt für B tut: doRun() läuft mit B als DokumentKontext,
		// unabhängig davon, dass A gerade den Fokus hält.
		DokumentKontext.mitKontextWerfend(zweitesDokument, () ->
				spieltagWert[0] = impl.ptmintproperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG));

		assertThat(spieltagWert[0])
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
