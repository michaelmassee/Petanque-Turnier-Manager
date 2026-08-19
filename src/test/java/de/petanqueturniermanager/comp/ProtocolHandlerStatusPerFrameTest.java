package de.petanqueturniermanager.comp;

import static de.petanqueturniermanager.comp.ProtocolHandlerTestSupport.getSharedContext;
import static de.petanqueturniermanager.comp.ProtocolHandlerTestSupport.neuerStatusEntry;
import static de.petanqueturniermanager.comp.ProtocolHandlerTestSupport.propListeMap;
import static de.petanqueturniermanager.comp.ProtocolHandlerTestSupport.seedeTurnierSystem;
import static de.petanqueturniermanager.comp.ProtocolHandlerTestSupport.setSharedContext;
import static de.petanqueturniermanager.comp.ProtocolHandlerTestSupport.statusListenersMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sun.star.frame.FeatureStateEvent;
import com.sun.star.frame.XModel;
import com.sun.star.frame.XStatusListener;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.URL;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Regressionstest für den Statuszeilen-Leak zwischen mehreren gleichzeitig offenen
 * Turnier-Dokumenten: {@code ProtocolHandler.notifyAllListeners()} berechnete den
 * Enabled-Status früher EINMAL pro Kommando über das global fokussierte Dokument und
 * wendete ihn auf ALLE registrierten Listener an — bei mehreren offenen Dokumenten bekamen
 * so alle Toolbars denselben (ggf. falschen) Status. Fix: jeder {@code StatusEntry} trägt
 * sein eigenes, per {@code ermittleDokumentAusFrame()} zum Registrierungszeitpunkt
 * aufgelöstes Dokument; {@code notifyAllListeners()} wertet {@code isEnabled(...)} jetzt
 * pro Listener mit dessen eigenem Dokument aus.
 * <p>
 * Testtechnik: siehe {@link ProtocolHandlerTestSupport}.
 */
class ProtocolHandlerStatusPerFrameTest {

	private static final String CMD = ProtocolHandler.CMD_UPDATE_MELDELISTE;

	private Object vorherigeSharedContext;
	private Object vorherigeStatusListenerListe;
	private String oidDokA;
	private String oidDokB;

	@AfterEach
	void aufraeumen() throws Exception {
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
	void notifyAllListeners_wertetJedenListenerMitSeinemEigenenDokumentAus() throws Exception {
		XSpreadsheetDocument dokA = mockDokument();
		XSpreadsheetDocument dokB = mockDokument();
		oidDokA = seedeTurnierSystem(dokA, TurnierSystem.SUPERMELEE);
		oidDokB = seedeTurnierSystem(dokB, TurnierSystem.KEIN);

		XStatusListener listenerA = mock(XStatusListener.class);
		XStatusListener listenerB = mock(XStatusListener.class);
		URL url = new URL();
		url.Path = CMD;

		Object entryA = neuerStatusEntry(listenerA, url, dokA);
		Object entryB = neuerStatusEntry(listenerB, url, dokB);
		List<Object> entries = Collections.synchronizedList(new ArrayList<>(List.of(entryA, entryB)));

		vorherigeSharedContext = getSharedContext();
		vorherigeStatusListenerListe = statusListenersMap().put(CMD, entries);
		// RETURNS_DEEP_STUBS: notifyAllListeners() ruft intern auch holeAktivesDokument() (nur fuer
		// das Trace-Log) auf, das ueber DocumentHelper.getCurrentDesktop() den ServiceManager
		// verwendet -- ohne Deep-Stubs wuerde das eine (harmlose, aber laute) NPE loggen.
		setSharedContext(mock(XComponentContext.class, withSettings().defaultAnswer(org.mockito.Answers.RETURNS_DEEP_STUBS)));

		ProtocolHandler.notifyAllListeners();

		ArgumentCaptor<FeatureStateEvent> eventA = ArgumentCaptor.forClass(FeatureStateEvent.class);
		ArgumentCaptor<FeatureStateEvent> eventB = ArgumentCaptor.forClass(FeatureStateEvent.class);
		verify(listenerA).statusChanged(eventA.capture());
		verify(listenerB).statusChanged(eventB.capture());

		assertThat(eventA.getValue().IsEnabled)
				.as("CMD_UPDATE_MELDELISTE muss für das Supermelee-Dokument A aktiv sein")
				.isTrue();
		assertThat(eventB.getValue().IsEnabled)
				.as("CMD_UPDATE_MELDELISTE muss für das Nicht-Supermelee-Dokument B inaktiv sein, "
						+ "auch wenn beide Listener im selben notifyAllListeners()-Durchlauf bedient werden")
				.isFalse();
	}

	private static XSpreadsheetDocument mockDokument() {
		return mock(XSpreadsheetDocument.class, withSettings().extraInterfaces(XModel.class));
	}
}
