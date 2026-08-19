/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager;

import static de.petanqueturniermanager.comp.ProtocolHandlerTestSupport.getSharedContext;
import static de.petanqueturniermanager.comp.ProtocolHandlerTestSupport.neuerStatusEntry;
import static de.petanqueturniermanager.comp.ProtocolHandlerTestSupport.notifyAllListenersRunnable;
import static de.petanqueturniermanager.comp.ProtocolHandlerTestSupport.propListeMap;
import static de.petanqueturniermanager.comp.ProtocolHandlerTestSupport.seedeTurnierSystem;
import static de.petanqueturniermanager.comp.ProtocolHandlerTestSupport.setSharedContext;
import static de.petanqueturniermanager.comp.ProtocolHandlerTestSupport.statusListenersMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.sun.star.frame.FeatureStateEvent;
import com.sun.star.frame.XModel;
import com.sun.star.frame.XStatusListener;
import com.sun.star.sheet.XCalculatable;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheetView;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.URL;

import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;

/**
 * Integrationstest über die volle Kette der beiden Fixes für den Multi-Dokument-Fokus-Bug:
 * {@code SheetRunner.run()} setzt jetzt {@code DokumentKontext} für sein eigenes Dokument B
 * (statt sich auf UI-Fokus zu verlassen), und der beim Lauf-Ende über
 * {@code koordinator.benachrichtigeListener()} gefeuerte {@code ProtocolHandler.notifyAllListeners()}
 * wertet jeden Toolbar-Listener mit dessen EIGENEM Dokument aus (statt mit einem global
 * fokussierten). Dieser Test verkabelt beide Mechanismen exakt wie in Produktion
 * ({@code ProtocolHandler}-Konstruktor registriert
 * {@code SheetRunner.addStateChangeListener(ProtocolHandler::notifyAllListeners)}) und prüft,
 * dass nach einem SheetRunner-Lauf für Dokument B ein Status-Listener von Dokument A weiterhin
 * A's eigenen (unveränderten) Status meldet.
 */
class SheetRunnerNotifyAllListenersTest {

	static final Logger logger = LogManager.getLogger(SheetRunnerNotifyAllListenersTest.class);

	private static final String CMD = de.petanqueturniermanager.comp.ProtocolHandler.CMD_UPDATE_MELDELISTE;

	private Runnable notifyAllListeners;
	private Object vorherigeSharedContext;
	private Object vorherigeStatusListenerListe;
	private String oidDokA;
	private String oidDokB;

	@BeforeEach
	void setUp() throws Exception {
		// Frischer Koordinator: kein globaler Zustand aus anderen SheetRunner-Tests.
		SheetRunner.koordinator = new SheetRunnerKoordinator();
		notifyAllListeners = notifyAllListenersRunnable();
		SheetRunner.addStateChangeListener(notifyAllListeners);
		vorherigeSharedContext = getSharedContext();
	}

	@AfterEach
	void aufraeumen() throws Exception {
		SheetRunner.removeStateChangeListener(notifyAllListeners);
		setSharedContext(vorherigeSharedContext);
		if (vorherigeStatusListenerListe == null) {
			statusListenersMap().remove(CMD);
		} else {
			statusListenersMap().put(CMD, vorherigeStatusListenerListe);
		}
		if (oidDokA != null) {
			propListeMap().remove(oidDokA);
		}
		if (oidDokB != null) {
			propListeMap().remove(oidDokB);
		}
	}

	@Test
	void sheetRunnerLaufFuerDokumentB_meldetDokumentAWeiterhinKorrekt() throws Exception {
		// Dokument A: bereits offenes Supermelee-Turnier, NICHT Ziel dieses SheetRunner-Laufs.
		XSpreadsheetDocument dokA = mockDokument();
		oidDokA = seedeTurnierSystem(dokA, TurnierSystem.SUPERMELEE);
		XStatusListener listenerA = mock(XStatusListener.class);
		URL url = new URL();
		url.Path = CMD;
		Object entryA = neuerStatusEntry(listenerA, url, dokA);

		// Dokument B: leeres, neues Turnier -- das eigentliche Ziel des SheetRunner-Laufs.
		XSpreadsheetDocument dokB = mockDokument();
		oidDokB = seedeTurnierSystem(dokB, TurnierSystem.KEIN);
		XStatusListener listenerB = mock(XStatusListener.class);
		Object entryB = neuerStatusEntry(listenerB, url, dokB);

		statusListenersMap().put(CMD, Collections.synchronizedList(new ArrayList<>(List.of(entryA, entryB))));
		setSharedContext(mock(XComponentContext.class, withSettings().defaultAnswer(org.mockito.Answers.RETURNS_DEEP_STUBS)));

		WorkingSpreadsheet wsB = mockWorkingSpreadsheet(dokB);
		DokumentKontextPruefenderRunner runner = new DokumentKontextPruefenderRunner(wsB, dokB,
				Mockito.mock(ProcessBox.class, Mockito.RETURNS_SELF));

		runner.run();

		assertThat(runner.dokumentKontextWarDokB)
				.as("SheetRunner.run() muss DokumentKontext waehrend doRun() auf das EIGENE Dokument (B) setzen")
				.isTrue();

		ArgumentCaptor<FeatureStateEvent> eventA = ArgumentCaptor.forClass(FeatureStateEvent.class);
		ArgumentCaptor<FeatureStateEvent> eventB = ArgumentCaptor.forClass(FeatureStateEvent.class);
		// Zwei Aufrufe: "Menü deaktivieren" (Start, isRunning=true -> alles disabled außer Abbruch)
		// und "Menü reaktivieren" (Ende, isRunning=false -> echte pro-Dokument-Bewertung).
		verify(listenerA, org.mockito.Mockito.atLeastOnce()).statusChanged(eventA.capture());
		verify(listenerB, org.mockito.Mockito.atLeastOnce()).statusChanged(eventB.capture());

		assertThat(eventA.getAllValues().get(eventA.getAllValues().size() - 1).IsEnabled)
				.as("Nach dem Lauf muss der Status-Listener von Dokument A weiterhin A's eigenen "
						+ "(Supermelee-)Status melden, nicht den von Dokument B")
				.isTrue();
		assertThat(eventB.getAllValues().get(eventB.getAllValues().size() - 1).IsEnabled)
				.as("Dokument B hat kein Supermelee-Turnier -> CMD_UPDATE_MELDELISTE bleibt inaktiv")
				.isFalse();
	}

	private static XSpreadsheetDocument mockDokument() {
		return mock(XSpreadsheetDocument.class, withSettings().extraInterfaces(XModel.class));
	}

	private static WorkingSpreadsheet mockWorkingSpreadsheet(XSpreadsheetDocument dokument) {
		WorkingSpreadsheet ws = Mockito.mock(WorkingSpreadsheet.class);
		when(ws.getWorkingSpreadsheetDocument()).thenReturn(dokument);
		when(ws.getxCalculatable()).thenReturn(Mockito.mock(XCalculatable.class));
		var xStorableMock = Mockito.mock(com.sun.star.frame.XStorable.class);
		when(xStorableMock.getLocation()).thenReturn("");
		when(ws.getXStorable()).thenReturn(xStorableMock);
		return ws;
	}

	/** Minimaler SheetRunner, der beim doRun() prüft, ob DokumentKontext korrekt auf das eigene Dokument zeigt. */
	private static final class DokumentKontextPruefenderRunner extends SheetRunner {

		private final XSpreadsheetDocument erwartetesDokument;
		private final ProcessBox processBoxMock;
		boolean dokumentKontextWarDokB;

		DokumentKontextPruefenderRunner(WorkingSpreadsheet ws, XSpreadsheetDocument erwartetesDokument,
				ProcessBox processBoxMock) {
			super(ws, TurnierSystem.KEIN, "Test");
			this.erwartetesDokument = erwartetesDokument;
			this.processBoxMock = processBoxMock;
		}

		@Override
		@SuppressWarnings("deprecation")
		protected IKonfigurationSheet getKonfigurationSheet() {
			return null; // TurnierSystem.KEIN -> updateKonfigurationSheet() wird nicht aufgerufen
		}

		@Override
		public Logger getLogger() {
			return logger;
		}

		@Override
		protected void doRun() throws GenerateException {
			dokumentKontextWarDokB = de.petanqueturniermanager.comp.DokumentKontext.get() == erwartetesDokument;
		}

		@Override
		public ProcessBox processBox() {
			return processBoxMock;
		}

		@Override
		public void processBoxinfo(String i18nKey, Object... args) {
			// kein LibreOffice ProcessBox in Tests
		}

		/** Überschrieben um LibreOffice MessageBox-Aufrufe in Tests zu vermeiden */
		@Override
		protected void handleGenerateException(GenerateException e) {
			// nicht relevant fuer diesen Test
		}
	}
}
