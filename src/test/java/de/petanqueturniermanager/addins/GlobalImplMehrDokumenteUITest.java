package de.petanqueturniermanager.addins;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.star.awt.XTopWindow;
import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.OfficeDocumentHelper;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleePropertiesSpalte;

/**
 * Regressionstest: Die dokumentbezogenen {@code PTM.ALG.*}-Funktionen lesen das Dokument der Formelzelle, nie das
 * fokussierte. Calc übergibt es verdeckt als Caller-Parameter ({@code XPropertySet}, siehe {@code AddInGlobal.idl}).
 * <p>
 * Früher lösten sie das Dokument über {@code DokumentKontext} und sonst über den Fokus auf. Bei einer Neuberechnung
 * auf dem Main-Thread gab es keinen Kontext: Das Sync-Info-Blatt in Dokument A zeigte die Werte von Dokument B, sobald
 * B beim Neuberechnen den Fokus hatte; ebenso leckte {@code PTM.ALG.INTPROPERTY("Spieltag")} beim Anlegen eines neuen
 * Turniers.
 * <p>
 * Geprüft wird per direktem Aufruf mit dem Dokumentmodell als Caller – genau das, was Calc übergibt. Ein Vergleich
 * über Formelergebnisse ist im UITest nicht möglich: Das Add-in im LO-Prozess hat einen eigenen Property-Cache, der
 * vom Testprozess geschriebene Properties nicht sieht (Diagnose 2026-11-25: Caller kam für beide Dokumente korrekt an,
 * der Cache meldete aber leere Properties und lieferte {@code "fehler"}).
 */
public class GlobalImplMehrDokumenteUITest extends BaseCalcUITest {

	private static final String TEST_PROPERTY = "PTM-Test Dokumentzuordnung";

	private XSpreadsheetDocument zweitesDokument;

	@AfterEach
	public void schliesseZweitesDokument() {
		if (zweitesDokument != null) {
			OfficeDocumentHelper.closeDoc(zweitesDokument);
			zweitesDokument = null;
		}
	}

	/** Direkter Aufruf: Jeder Caller bekommt den Wert seines Dokuments, unabhängig vom Fokus. */
	@Test
	public void intPropertyLiestDasDokumentDesCallersTrotzFokusAufAnderem() throws Exception {
		docPropHelper.setIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG, 2);
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
				TurnierSystem.SUPERMELEE.getId());
		DocumentPropertiesHelper zweiteProperties = zweitesTurnierDokument(TurnierSystem.SUPERMELEE);
		zweiteProperties.setIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG, 1);

		GlobalImpl impl = new GlobalImpl(starter.getxComponentContext());

		fokussiere(doc);
		assertThat(impl.ptmintproperty(caller(zweitesDokument), SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG))
				.as("Fokus auf A, Caller B").isEqualTo(1);
		fokussiere(zweitesDokument);
		assertThat(impl.ptmintproperty(caller(doc), SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG))
				.as("Fokus auf B, Caller A").isEqualTo(2);
	}

	/** Caller ohne Dokument (Berechnung ohne DocShell): leerer Wert statt eines fremden Dokuments. */
	@Test
	public void callerOhneDokumentLiefertDenLeerwert() {
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
				TurnierSystem.SCHWEIZER.getId());
		fokussiere(doc);

		GlobalImpl impl = new GlobalImpl(starter.getxComponentContext());

		assertThat(impl.ptmstringproperty(null, TEST_PROPERTY)).isEqualTo("fehler");
		assertThat(impl.ptmturniersystem(null)).isEqualTo(TurnierSystem.KEIN.getBezeichnung());
	}

	private DocumentPropertiesHelper zweitesTurnierDokument(TurnierSystem system) {
		zweitesDokument = OfficeDocumentHelper.from(loader).createSichtbaresCalc();
		assertThat(zweitesDokument).as("zweites Dokument konnte nicht erstellt werden").isNotNull();
		DocumentPropertiesHelper properties = new DocumentPropertiesHelper(
				new WorkingSpreadsheet(starter.getxComponentContext(), zweitesDokument));
		properties.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, system.getId());
		return properties;
	}

	/** Was Calc als Caller übergibt: das Dokumentmodell. */
	private static XPropertySet caller(XSpreadsheetDocument dokument) {
		return Lo.qi(XPropertySet.class, dokument);
	}

	/** Gleiche Fokus-Sequenz wie {@code TurnierSystemNeueDateiAuswahlDialog.fokussiereDokument()}. */
	private static void fokussiere(XSpreadsheetDocument dokument) {
		XFrame frame = Lo.qi(XModel.class, dokument).getCurrentController().getFrame();
		frame.activate();
		XTopWindow topWindow = Lo.qi(XTopWindow.class, frame.getContainerWindow());
		if (topWindow != null) {
			topWindow.toFront();
		}
	}
}
