/**
 * Erstellung : 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.DispatchDescriptor;
import com.sun.star.frame.FeatureStateEvent;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XStatusListener;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.util.URL;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEventListener;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * UNO ProtocolHandler für das benutzerdefinierte Protokoll "ptm:".
 * <p>
 * Implementiert {@link XDispatchProvider} und {@link XDispatch}, um
 * Menüpunkte dynamisch zu aktivieren/deaktivieren basierend auf dem
 * aktuellen Turniersystem im Dokument.
 * <p>
 * Zustandsregeln für Schweizer Menüpunkte:
 * <ul>
 *   <li>{@code schweizer_start} – aktiv wenn kein Turnier vorhanden (KEIN)</li>
 *   <li>{@code schweizer_neue_meldeliste} – aktiv wenn Schweizer-Turnier aktiv</li>
 * </ul>
 */
public class ProtocolHandler extends WeakBase implements XDispatchProvider, XDispatch, XServiceInfo {

	private static final Logger logger = LogManager.getLogger(ProtocolHandler.class);

	public static final String PROTOCOL = "ptm:";
	private static final String IMPLEMENTATION_NAME = ProtocolHandler.class.getName();
	private static final String[] SERVICE_NAMES = { "de.petanqueturniermanager.ProtocolHandler" };

	// Shared state across all instances (LibreOffice may create multiple per session)
	private static final Map<String, List<StatusEntry>> STATUS_LISTENERS =
			Collections.synchronizedMap(new HashMap<>());
	private static volatile XComponentContext SHARED_CONTEXT;
	private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

	private final XComponentContext xContext;

	public ProtocolHandler(XComponentContext xContext) {
		this.xContext = xContext;
		SHARED_CONTEXT = xContext;
		PetanqueTurnierMngrSingleton.init(xContext);
		if (REGISTERED.compareAndSet(false, true)) {
			PetanqueTurnierMngrSingleton.addGlobalEventListener(new IGlobalEventListener() {
				@Override
				public void onFocus(Object source) {
					notifyAllListeners();
				}

				@Override
				public void onLoadFinished(Object source) {
					notifyAllListeners();
				}

				@Override
				public void onNew(Object source) {
					notifyAllListeners();
				}
			});
			PetanqueTurnierMngrSingleton.addTurnierEventListener(new ITurnierEventListener() {
				@Override
				public void onPropertiesChanged(ITurnierEvent event) {
					notifyAllListeners();
				}
			});
		}
	}

	// -------------------------------------------------------------------------
	// XDispatchProvider

	@Override
	public XDispatch queryDispatch(URL url, String targetFrameName, int searchFlags) {
		if (PROTOCOL.equalsIgnoreCase(url.Protocol)) {
			return this;
		}
		return null;
	}

	@Override
	public XDispatch[] queryDispatches(DispatchDescriptor[] requests) {
		XDispatch[] dispatches = new XDispatch[requests.length];
		for (int i = 0; i < requests.length; i++) {
			dispatches[i] = queryDispatch(requests[i].FeatureURL, requests[i].FrameName, requests[i].SearchFlags);
		}
		return dispatches;
	}

	// -------------------------------------------------------------------------
	// XDispatch

	@Override
	public void dispatch(URL url, PropertyValue[] args) {
		String command = url.Path;
		try {
			WorkingSpreadsheet ws = new WorkingSpreadsheet(xContext);
			switch (command) {
			case "schweizer_start":
				new SchweizerMeldeListeSheetNew(ws).start();
				break;
			case "schweizer_neue_meldeliste":
				if (new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument() != TurnierSystem.SCHWEIZER) {
					MessageBox.from(ws.getxContext(), MessageBoxTypeEnum.ERROR_OK)
							.caption("Kein Schweizer-Turnier-Dokument")
							.message("Kein Schweizer-Turnier vorhanden").show();
				} else {
					new SchweizerMeldeListeSheetNew(ws).start();
				}
				break;
			default:
				logger.warn("Unbekannter Befehl: {}", command);
			}
		} catch (Exception e) {
			logger.error("Fehler beim Ausführen von '{}': {}", command, e.getMessage(), e);
		}
	}

	@Override
	public void addStatusListener(XStatusListener listener, URL url) {
		String command = url.Path;
		STATUS_LISTENERS.computeIfAbsent(command, k -> Collections.synchronizedList(new ArrayList<>()))
				.add(new StatusEntry(listener, url));
		postStatus(listener, url, isEnabled(command));
	}

	@Override
	public void removeStatusListener(XStatusListener listener, URL url) {
		String command = url.Path;
		List<StatusEntry> list = STATUS_LISTENERS.get(command);
		if (list != null) {
			list.removeIf(e -> e.listener == listener);
		}
	}

	// -------------------------------------------------------------------------
	// Zustandsprüfung und Listener-Benachrichtigung (statisch, cross-instance)

	private static boolean isEnabled(String command) {
		XComponentContext ctx = SHARED_CONTEXT;
		if (ctx == null) {
			return false;
		}
		try {
			TurnierSystem ts = new DocumentPropertiesHelper(new WorkingSpreadsheet(ctx))
					.getTurnierSystemAusDocument();
			return switch (command) {
			case "schweizer_start" -> ts == TurnierSystem.KEIN;
			case "schweizer_neue_meldeliste" -> ts == TurnierSystem.SCHWEIZER;
			default -> false;
			};
		} catch (Exception e) {
			return false;
		}
	}

	private static void notifyAllListeners() {
		Map<String, List<StatusEntry>> snapshot;
		synchronized (STATUS_LISTENERS) {
			snapshot = new HashMap<>(STATUS_LISTENERS);
		}
		for (Map.Entry<String, List<StatusEntry>> entry : snapshot.entrySet()) {
			boolean enabled = isEnabled(entry.getKey());
			for (StatusEntry e : new ArrayList<>(entry.getValue())) {
				postStatus(e.listener, e.url, enabled);
			}
		}
	}

	private static void postStatus(XStatusListener listener, URL url, boolean enabled) {
		try {
			FeatureStateEvent event = new FeatureStateEvent();
			event.FeatureURL = url;
			event.IsEnabled = enabled;
			event.Requery = false;
			listener.statusChanged(event);
		} catch (Exception e) {
			logger.warn("Fehler beim Benachrichtigen des Status-Listeners: {}", e.getMessage());
		}
	}

	// -------------------------------------------------------------------------
	// XServiceInfo

	@Override
	public String getImplementationName() {
		return IMPLEMENTATION_NAME;
	}

	@Override
	public boolean supportsService(String serviceName) {
		return Arrays.asList(SERVICE_NAMES).contains(serviceName);
	}

	@Override
	public String[] getSupportedServiceNames() {
		return SERVICE_NAMES;
	}

	// -------------------------------------------------------------------------
	// UNO Factory

	public static boolean __writeRegistryServiceInfo(XRegistryKey xRegistryKey) {
		return Factory.writeRegistryServiceInfo(IMPLEMENTATION_NAME, SERVICE_NAMES, xRegistryKey);
	}

	public static XSingleComponentFactory __getComponentFactory(String sImplementationName) {
		if (sImplementationName.equals(IMPLEMENTATION_NAME)) {
			return Factory.createComponentFactory(ProtocolHandler.class, SERVICE_NAMES);
		}
		return null;
	}

	// -------------------------------------------------------------------------

	private record StatusEntry(XStatusListener listener, URL url) {
	}
}
