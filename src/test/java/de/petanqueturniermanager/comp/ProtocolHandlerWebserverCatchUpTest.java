package de.petanqueturniermanager.comp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.sun.star.frame.XController;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XFrames;
import com.sun.star.frame.XFramesSupplier;
import com.sun.star.frame.XModel;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.XComponentContext;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.helper.Lo;

/**
 * Regressionstest für {@link ProtocolHandler#ermittleEinzigesOffenesSpreadsheetDokument}: den
 * Webserver-Start-Catch-up im Konstruktor darf sich nur binden, wenn zweifelsfrei genau ein
 * Turnier-Dokument offen ist – bei Mehrdeutigkeit oder Fehlen darf er NICHT raten (kein
 * Fokus-Fallback, siehe CLAUDE.md „Mehrere offene Turnier-Dokumente").
 * <p>
 * Reiner Mockito-Unit-Test (kein echtes LibreOffice nötig) statt UI-Test: {@code GlobalProperties}
 * und {@code WebServerManager} sind Java-Singletons pro JVM – ein UI-Test würde nur die
 * Test-JVM-eigene Instanz umschalten, nicht die tatsächlich in der laufenden LibreOffice-Instanz
 * aktive (getrennte Prozesse). Die Frame-Scan-Logik selbst ist dagegen reine, UNO-Interface-basierte
 * Java-Logik und lässt sich mit gemockten Frames vollständig und deterministisch prüfen.
 */
class ProtocolHandlerWebserverCatchUpTest {

	@Test
	void liefertNullOhneOffeneFrames() throws Exception {
		XFrames frames = mock(XFrames.class);
		when(frames.getCount()).thenReturn(0);

		assertThat(ProtocolHandler.ermittleEinzigesOffenesSpreadsheetDokument(kontextMit(frames))).isNull();
	}

	@Test
	void liefertDasEinzigeOffeneSpreadsheetDokument() throws Exception {
		XModel model = mockSpreadsheetModel();
		XFrames frames = frameListeFuer(model);

		XSpreadsheetDocument erwartet = Lo.qi(XSpreadsheetDocument.class, model);
		assertThat(ProtocolHandler.ermittleEinzigesOffenesSpreadsheetDokument(kontextMit(frames)))
				.isSameAs(erwartet);
	}

	@Test
	void liefertNullBeiZweiUnterschiedlichenOffenenDokumenten() throws Exception {
		XFrames frames = frameListeFuer(mockSpreadsheetModel(), mockSpreadsheetModel());

		assertThat(ProtocolHandler.ermittleEinzigesOffenesSpreadsheetDokument(kontextMit(frames)))
				.as("bei mehreren offenen Dokumenten darf NICHT geraten werden")
				.isNull();
	}

	@Test
	void behandeltMehrereFramesAufDasselbeDokumentAlsEindeutig() throws Exception {
		// z.B. zwei Fenster/Views desselben Dokuments -- keine echte Mehrdeutigkeit.
		XModel model = mockSpreadsheetModel();
		XFrames frames = frameListeFuer(model, model);

		XSpreadsheetDocument erwartet = Lo.qi(XSpreadsheetDocument.class, model);
		assertThat(ProtocolHandler.ermittleEinzigesOffenesSpreadsheetDokument(kontextMit(frames)))
				.isSameAs(erwartet);
	}

	@Test
	void ignoriertFramesOhneSpreadsheetDokument() throws Exception {
		XModel writerModel = mock(XModel.class, withSettings().extraInterfaces(XTextDocument.class));
		XModel calcModel = mockSpreadsheetModel();

		XFrames frames = frameListeFuer(writerModel, calcModel);

		XSpreadsheetDocument erwartet = Lo.qi(XSpreadsheetDocument.class, calcModel);
		assertThat(ProtocolHandler.ermittleEinzigesOffenesSpreadsheetDokument(kontextMit(frames)))
				.isSameAs(erwartet);
	}

	// -----------------------------------------------------------------------

	private static XModel mockSpreadsheetModel() {
		return mock(XModel.class, withSettings().extraInterfaces(XSpreadsheetDocument.class));
	}

	private static XFrames frameListeFuer(XModel... modelle) throws Exception {
		XFrames frames = mock(XFrames.class);
		when(frames.getCount()).thenReturn(modelle.length);
		for (int i = 0; i < modelle.length; i++) {
			XController controller = mock(XController.class);
			when(controller.getModel()).thenReturn(modelle[i]);
			XFrame frame = mock(XFrame.class);
			when(frame.getController()).thenReturn(controller);
			when(frames.getByIndex(i)).thenReturn(frame);
		}
		return frames;
	}

	private static XComponentContext kontextMit(XFrames frames) throws Exception {
		XDesktop desktop = mock(XDesktop.class, withSettings().extraInterfaces(XFramesSupplier.class));
		when(((XFramesSupplier) desktop).getFrames()).thenReturn(frames);

		XMultiComponentFactory mcf = mock(XMultiComponentFactory.class);
		XComponentContext ctx = mock(XComponentContext.class);
		when(ctx.getServiceManager()).thenReturn(mcf);
		when(mcf.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)).thenReturn(desktop);
		return ctx;
	}
}
