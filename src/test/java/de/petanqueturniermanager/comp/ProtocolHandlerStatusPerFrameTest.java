package de.petanqueturniermanager.comp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sun.star.frame.FeatureStateEvent;
import com.sun.star.frame.XModel;
import com.sun.star.frame.XStatusListener;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.uno.XInterface;
import com.sun.star.util.URL;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;

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
 * Testtechnik: {@code StatusEntry} wird per Reflection direkt in die private statische
 * {@code STATUS_LISTENERS}-Map injiziert (umgeht die UNO-Frame-Auflösung von
 * {@code addStatusListener()}, die ohne echtes LibreOffice nicht sinnvoll mockbar ist) und
 * {@code DocumentPropertiesHelper.PROPLISTE} wird direkt für die Mock-Dokumente vorbelegt
 * (umgeht die tief verschachtelte {@code XPropertyContainer}/{@code XMultiPropertySet}-UNO-Kette).
 */
class ProtocolHandlerStatusPerFrameTest {

	private static final String CMD = ProtocolHandler.CMD_UPDATE_MELDELISTE;

	private Object vorherigeSharedContext;
	private Object vorherigeStatusListenerListe;
	private String oidDokA;
	private String oidDokB;

	@AfterEach
	void aufraeumen() throws Exception {
		setStaticField(ProtocolHandler.class, "SHARED_CONTEXT", vorherigeSharedContext);
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

		vorherigeSharedContext = getStaticField(ProtocolHandler.class, "SHARED_CONTEXT");
		vorherigeStatusListenerListe = statusListenersMap().put(CMD, entries);
		// RETURNS_DEEP_STUBS: notifyAllListeners() ruft intern auch holeAktivesDokument() (nur fuer
		// das Trace-Log) auf, das ueber DocumentHelper.getCurrentDesktop() den ServiceManager
		// verwendet -- ohne Deep-Stubs wuerde das eine (harmlose, aber laute) NPE loggen.
		setStaticField(ProtocolHandler.class, "SHARED_CONTEXT",
				mock(XComponentContext.class, withSettings().defaultAnswer(org.mockito.Answers.RETURNS_DEEP_STUBS)));

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

	/** Belegt {@link DocumentPropertiesHelper}'s OID-Cache direkt, ohne die UNO-Property-Kette zu mocken. */
	private static String seedeTurnierSystem(XSpreadsheetDocument dokument, TurnierSystem turnierSystem)
			throws Exception {
		String oid = UnoRuntime.generateOid(Lo.qi(XInterface.class, dokument));
		ConcurrentHashMap<String, String> properties = new ConcurrentHashMap<>();
		properties.put(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, String.valueOf(turnierSystem.getId()));
		propListeMap().put(oid, properties);
		return oid;
	}

	private static Object neuerStatusEntry(XStatusListener listener, URL url, XSpreadsheetDocument dokument)
			throws Exception {
		Class<?> statusEntryClass = Class.forName(ProtocolHandler.class.getName() + "$StatusEntry");
		Constructor<?> ctor = statusEntryClass.getDeclaredConstructor(XStatusListener.class, URL.class,
				XSpreadsheetDocument.class);
		ctor.setAccessible(true);
		return ctor.newInstance(listener, url, dokument);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> statusListenersMap() throws Exception {
		return (Map<String, Object>) getStaticField(ProtocolHandler.class, "STATUS_LISTENERS");
	}

	@SuppressWarnings("unchecked")
	private static Map<String, ConcurrentHashMap<String, String>> propListeMap() throws Exception {
		return (Map<String, ConcurrentHashMap<String, String>>) getStaticField(DocumentPropertiesHelper.class, "PROPLISTE");
	}

	private static Object getStaticField(Class<?> clazz, String name) throws Exception {
		Field field = clazz.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(null);
	}

	private static void setStaticField(Class<?> clazz, String name, Object value) throws Exception {
		Field field = clazz.getDeclaredField(name);
		field.setAccessible(true);
		field.set(null, value);
	}
}
