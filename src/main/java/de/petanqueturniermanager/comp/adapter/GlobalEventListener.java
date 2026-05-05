package de.petanqueturniermanager.comp.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.document.XEventListener;
import com.sun.star.frame.XModel;
import com.sun.star.lang.EventObject;
import com.sun.star.sheet.XSpreadsheetView;

import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.LogUtil;

public class GlobalEventListener implements XEventListener {

	private static final Logger logger = LogManager.getLogger(GlobalEventListener.class);

	private static final String ON_SAVE_AS = "OnSaveAs";
	private static final String ON_SAVE = "OnSave";
	private static final String ON_UNLOAD = "OnUnload";
	private static final String ON_CREATE = "OnCreate";
	private static final String ON_NEW = "OnNew";

	private static final String ON_FOCUS = "OnFocus";
	private static final String ON_LOAD_FINISHED = "OnLoadFinished";
	private static final String ON_VIEW_CREATED = "OnViewCreated";
	private static final String ON_VIEW_CLOSED  = "OnViewClosed";
	private static final String ON_LOAD = "OnLoad";

	private static final String ON_UNFOCUS = "OnUnfocus";

	private final List<IGlobalEventListener> listeners;

	public GlobalEventListener() {
		listeners = Collections.synchronizedList(new ArrayList<IGlobalEventListener>());
	}

	/**
	 * NICHT SYNCHRONIZED, weil es Deadlocks gibt zwischen getUrl() und der Zustellung bestimmter Events (z.B. OnSave).
	 */
	@Override
	public void notifyEvent(com.sun.star.document.EventObject docEvent) {
		// Der try-catch-Block verhindert, daß die Funktion und damit der
		// ganze Listener ohne Fehlermeldung abstürzt.
		try {
			if (docEvent.Source == null) {
				return;
			}
			String event = docEvent.EventName;
			if (logger.isDebugEnabled()) {
				logger.debug("{} [controller={}]", event, ermittleControllerTyp(docEvent.Source));
			}

			// https://wiki.openoffice.org/wiki/Documentation/DevGuide/WritingUNO/Jobs/List_of_Supported_Events
			// https://api.libreoffice.org/docs/idl/ref/servicecom_1_1sun_1_1star_1_1document_1_1Events.html
			if (ON_CREATE.equals(event)) {
				onCreate(docEvent.Source);
			} else if (ON_VIEW_CREATED.equals(event)) {
				onViewCreated(docEvent.Source);
			} else if (ON_VIEW_CLOSED.equals(event)) {
				onViewClosed(docEvent.Source);
			} else if (ON_UNLOAD.equals(event)) {
				onUnload(docEvent.Source);
			} else if (ON_SAVE.equals(event)) {
				onSaveOrSaveAs(docEvent.Source);
			} else if (ON_SAVE_AS.equals(event)) {
				onSaveOrSaveAs(docEvent.Source);
			} else if (ON_NEW.equals(event)) {
				onNew(docEvent.Source);
			} else if (ON_LOAD_FINISHED.equals(event)) { // 1
				onLoadFinished(docEvent.Source);
			} else if (ON_LOAD.equals(event)) { // 5
				onLoad(docEvent.Source);
			} else if (ON_UNFOCUS.equals(event)) {
				onUnfocus(docEvent.Source);
			} else if (ON_FOCUS.equals(event)) {
				onFocus(docEvent.Source);
			}

		} catch (Exception e) {
			LogUtil.error(logger, "Globaler Event-Dispatch fehlgeschlagen", e);
		} catch (Error e) {
			throw e;
		}
	}

	public void addGlobalEventListener(IGlobalEventListener listner) {
		listeners.add(listner);
	}

	public void removeGlobalEventListener(IGlobalEventListener listner) {
		listeners.remove(listner);
	}

	/**
	 * New Document was created visible. Unlike OnCreate this event is sent asynchronously at a time the view is completely created.
	 */
	private void onNew(Object source) {
		verteile("onNew", source, IGlobalEventListener::onNew);
	}

	private void onLoadFinished(Object source) {
		verteile("onLoadFinished", source, IGlobalEventListener::onLoadFinished);
	}

	private void onLoad(Object source) {
		verteile("onLoad", source, IGlobalEventListener::onLoad);
	}

	private void onFocus(Object source) {
		verteile("onFocus", source, IGlobalEventListener::onFocus);
	}

	private void onUnfocus(Object source) {
		verteile("onUnfocus", source, IGlobalEventListener::onUnfocus);
	}

	/**
	 * OnCreate ist das erste Event das aufgerufen wird, wenn ein neues leeres Dokument über eine Factory erzeugt wird wie z.B. mit loadComponentFromURL(... "private:factory/swriter" ...) oder in OOo über
	 * Datei->Neu. Auch OOo erzeugt manchmal im Hintergrund unsichtbare leere Dokumente über die Factory. Bekannt sind folgende Fälle: Beim OOo-Seriendruck über den Seriendruck-Assistent (für jeden
	 * Datensatz); Beim Einfügen von Autotexten (z.B. mit "bt<F3>" in OOo).
	 *
	 * Das Event kommt nicht, wenn ein Dokument von einer Datei geladen oder von einer Vorlage erzeugt wird. Das Event kommt auch dann nicht, wenn eine Vorlagendatei als Standardvorlage für neue Dokumente
	 * definiert ist und Datei->Neu verwendet wird.
	 */
	private void onCreate(Object source) {
		// keine Verarbeitung — bewusst leer
	}

	/**
	 * OnViewCreated kommt, wenn ein Dokument seitens OOo vollständig aufgebaut ist.<br>
	 * Das Event kommt bei allen Dokumenten, egal ob sie neu erzeugt, geladen, sichtbar oder unsichtbar sind.
	 */
	private void onViewCreated(Object source) {
		verteile("onViewCreated", source, IGlobalEventListener::onViewCreated);
	}

	/**
	 * OnViewClosed kommt, wenn ein View (z.B. Druckvorschau oder normale Tabellen-Ansicht) geschlossen wird.
	 * Beim Verlassen der Druckvorschau ist der Controller zu diesem Zeitpunkt bereits auf ScTabViewShell gewechselt.
	 */
	private void onViewClosed(Object source) {
		verteile("onViewClosed", source, IGlobalEventListener::onViewClosed);
	}

	/**
	 * OnSave oder OnSaveAs-Events werden beim Speichern von Dokumenten aufgerufen.
	 */
	private void onSaveOrSaveAs(Object source) {
		// derzeit keine Verarbeitung
	}

	/**
	 * OnUnload kommt als letztes Event wenn ein Dokument geschlossen wurde.
	 */
	private void onUnload(Object source) {
		DocumentPropertiesHelper.removeDocument(source);
		verteile("onUnload", source, IGlobalEventListener::onUnload);
	}

	private void verteile(String eventName, Object source, BiConsumer<IGlobalEventListener, Object> dispatch) {
		for (IGlobalEventListener listner : snapshot()) {
			try {
				dispatch.accept(listner, source);
			} catch (Exception e) {
				LogUtil.warn(logger, "Event " + eventName + " an "
						+ listner.getClass().getSimpleName() + " fehlgeschlagen", e);
			} catch (Error e) {
				throw e;
			}
		}
	}

	private List<IGlobalEventListener> snapshot() {
		synchronized (listeners) {
			return new ArrayList<>(listeners);
		}
	}

	@Override
	public void disposing(EventObject arg0) {
		listeners.clear();
	}

	/** Ermittelt den Controller-Typ für Debug-Logs (erkennt Druckvorschau vs. normale Tabellen-Ansicht). */
	private static String ermittleControllerTyp(Object source) {
		try {
			var xModel = Lo.qi(XModel.class, source);
			if (xModel == null) return "kein-XModel";
			var ctrl = xModel.getCurrentController();
			if (ctrl == null) return "kein-Controller";
			return Lo.qi(XSpreadsheetView.class, ctrl) != null
					? "ScTabViewShell" : "Druckvorschau/Sonstige";
		} catch (Exception e) {
			return "Fehler:" + e.getMessage();
		}
	}
}
