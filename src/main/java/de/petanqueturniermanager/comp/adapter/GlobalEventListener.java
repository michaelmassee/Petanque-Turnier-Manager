package de.petanqueturniermanager.comp.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.document.XEventListener;
import com.sun.star.lang.EventObject;


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
	private static final String ON_LOAD = "OnLoad";
	// private static final String ON_LOAD_DONE = "OnLoadDone";

	//	08.08.2022 21:52:34,750 DEBUG d.p.c.a.GlobalEventListener OnLoadFinished
	//	08.08.2022 21:52:34,751 DEBUG d.p.c.a.GlobalEventListener OnTitleChanged
	//	08.08.2022 21:52:34,968 DEBUG d.p.c.a.GlobalEventListener OnFocus
	//	08.08.2022 21:52:34,970 DEBUG d.p.c.a.GlobalEventListener OnViewCreated
	//	08.08.2022 21:52:35,046 DEBUG d.p.c.a.GlobalEventListener OnLoad

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
			logger.debug(event);

			// https://wiki.openoffice.org/wiki/Documentation/DevGuide/WritingUNO/Jobs/List_of_Supported_Events
			// https://api.libreoffice.org/docs/idl/ref/servicecom_1_1sun_1_1star_1_1document_1_1Events.html
			if (ON_CREATE.equals(event)) {
				onCreate(docEvent.Source);
			} else if (ON_VIEW_CREATED.equals(event)) {
				onViewCreated(docEvent.Source);
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

		} catch (Throwable t) {
			logger.error("", t);
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
	 *
	 * @param source
	 */
	private void onNew(Object source) {
		for (IGlobalEventListener listner : listeners) {
			try {
				listner.onNew(source);
			} catch (Throwable e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	private void onLoadFinished(Object source) {
		for (IGlobalEventListener listner : listeners) {
			try {
				listner.onLoadFinished(source);
			} catch (Throwable e) {
				logger.error(e.getMessage(), e);
			}

		}
	}

	/**
	 * @param source
	 */
	private void onLoad(Object source) {
		for (IGlobalEventListener listner : listeners) {
			try {
				listner.onLoad(source);
			} catch (Throwable e) {
				logger.error(e.getMessage(), e);
			}

		}
	}

	private void onFocus(Object source) {
		for (IGlobalEventListener listner : listeners) {
			try {
				listner.onFocus(source);
			} catch (Throwable e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * @param source
	 */
	private void onUnfocus(Object source) {
		for (IGlobalEventListener listner : listeners) {
			try {
				listner.onUnfocus(source);
			} catch (Throwable e) {
				logger.error(e.getMessage(), e);
			}
		}
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
		// XComponent compo = UNO.XComponent(source);
		// if (compo == null) return;
		//
		// // durch das Hinzufügen zum docManager kann im Event onViewCreated erkannt
		// // werden, dass das Dokument frisch erzeugt wurde:
		// XTextDocument xTextDoc = UNO.XTextDocument(source);
		// if (xTextDoc != null)
		// docManager.addTextDocument(xTextDoc);
		// else
		// docManager.add(compo);
		// // Verarbeitet wird das Dokument erst bei onViewCreated
	}

	/**
	 * OnViewCreated kommt, wenn ein Dokument seitens OOo vollständig aufgebaut ist.<br>
	 * Das Event kommt bei allen Dokumenten, egal ob sie neu erzeugt, geladen, sichtbar oder unsichtbar sind.
	 */
	private void onViewCreated(Object source) {
		// XModel compo = UNO.XModel(source);
		// XTextDocument xTextDoc = UNO.XTextDocument(compo);

		for (IGlobalEventListener listner : listeners) {
			try {
				listner.onViewCreated(source);
			} catch (Throwable e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * OnSave oder OnSaveAs-Events werden beim Speichern von Dokumenten aufgerufen.
	 *
	 */
	private void onSaveOrSaveAs(Object source) {
		// XTextDocument xTextDoc = UNO.XTextDocument(source);
		// if (xTextDoc == null) return;
	}

	/**
	 * OnUnload kommt als letztes Event wenn ein Dokument geschlossen wurde.
	 *
	 */
	private void onUnload(Object source) {
		for (IGlobalEventListener listner : listeners) {
			try {
				listner.onUnload(source);
			} catch (Throwable e) {
				logger.error(e.getMessage(), e);
			}

		}

	}

	@Override
	public void disposing(EventObject arg0) {
		listeners.clear();
	}

}
