/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XContainerWindowEventHandler;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.GlobalProperties.CompositeViewEintragRoh;
import de.petanqueturniermanager.comp.turnierevent.OnProperiesChangedEvent;
import de.petanqueturniermanager.comp.turnierevent.TurnierEventType;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.webserver.CompositeViewDetailDialog;
import de.petanqueturniermanager.webserver.SheetResolverFactory;
import de.petanqueturniermanager.webserver.WebServerManager;

/**
 * Event-Handler fuer die Composite-Views-Seite unter Extras -&gt; Optionen.
 * <p>
 * Verwaltet sowohl das globale Webserver-Flag als auch die Liste der Composite Views direkt auf
 * der Optionsseite (Turniersystem-Filter, Views-Liste, Hinzufuegen/Bearbeiten/Loeschen). Die
 * eigentliche Detail-Konfiguration eines einzelnen Views (Split-Baum, Panels) bleibt im modalen
 * {@link CompositeViewDetailDialog}.
 */
public final class CompositeViewsOptionsEventHandler extends WeakBase
		implements XServiceInfo, XContainerWindowEventHandler {

	private static final Logger logger = LogManager.getLogger(CompositeViewsOptionsEventHandler.class);

	private static final String IMPLEMENTATION_NAME = CompositeViewsOptionsEventHandler.class.getName();
	private static final String SERVICE_NAME = "de.petanqueturniermanager.CompositeViewsOptionsEventHandler";
	private static final String[] SERVICE_NAMES = { SERVICE_NAME };

	private static final String METHOD_EXTERNAL_EVENT = "external_event";
	private static final String EVENT_INITIALIZE = "initialize";
	private static final String EVENT_BACK = "back";
	private static final String EVENT_OK = "ok";

	private static final String CTL_COMPOSITE_VIEWS_LABEL = "CompositeViewsLabel";
	private static final String CTL_WEBSERVER_AKTIV = "WebserverAktiv";
	private static final String CTL_FILTER_LABEL = "CompositeViewsFilterLabel";
	private static final String CTL_FILTER_SYSTEM = "CompositeViewsFilterSystem";
	private static final String CTL_BEREICH = "CompositeViewsBereich";
	private static final String CTL_LISTE = "CompositeViewsListe";
	private static final String CTL_HINZUFUEGEN = "CompositeViewsHinzufuegen";
	private static final String CTL_BEARBEITEN = "CompositeViewsBearbeiten";
	private static final String CTL_LOESCHEN = "CompositeViewsLoeschen";

	/** Auswaehlbare Turniersysteme des Filters (alle ausser {@link TurnierSystem#KEIN}). */
	private static final TurnierSystem[] FILTER_SYSTEME = Arrays.stream(TurnierSystem.values())
			.filter(system -> system != TurnierSystem.KEIN)
			.toArray(TurnierSystem[]::new);

	private final XComponentContext context;

	/** Container, an dessen Buttons bereits Listener haengen (verhindert Doppelregistrierung). */
	private XControlContainer listenerContainer;

	/** Arbeitskopie der Composite Views; {@code null} solange die Seite noch nicht initialisiert wurde. */
	private List<CompositeViewEintragRoh> eintraege;

	/** Blatt-Typ-Vorschlaege des Detail-Dialogs, abhaengig vom Turniersystem-Filter. */
	private String[] komboBoxItems;

	/** Peer der Optionsseite, als Parent fuer modale Detail-Dialoge (siehe {@link #windowPeer}). */
	private XWindowPeer pagePeer;

	public CompositeViewsOptionsEventHandler(XComponentContext context) {
		this.context = context;
		GlobalProperties.setLibreOfficeContext(context);
	}

	@Override
	public boolean callHandlerMethod(XWindow window, Object eventObject, String method)
			throws WrappedTargetException {
		if (!METHOD_EXTERNAL_EVENT.equals(method)) {
			return true;
		}
		try {
			String event = AnyConverter.toString(eventObject);
			if (EVENT_INITIALIZE.equals(event) || EVENT_BACK.equals(event)) {
				ladeInOberflaeche(window);
			} else if (EVENT_OK.equals(event)) {
				speichereAusOberflaeche(window);
			}
			return true;
		} catch (Exception e) {
			throw new WrappedTargetException(e, method, this, e);
		}
	}

	@Override
	public String[] getSupportedMethodNames() {
		return new String[] { METHOD_EXTERNAL_EVENT };
	}

	private void ladeInOberflaeche(XWindow window) {
		XControlContainer container = container(window);
		pagePeer = windowPeer(window);
		setzeLabels(container);
		setCheckbox(container, CTL_WEBSERVER_AKTIV, GlobalProperties.get().isWebserverAktiv());
		if (eintraege == null) {
			eintraege = new ArrayList<>(GlobalProperties.get().getCompositeViewEintraege());
			komboBoxItems = SheetResolverFactory.sheetTypenFuer(null);
			setListItems(container, CTL_FILTER_SYSTEM, filterEintraege());
			setSelectedPos(container, CTL_FILTER_SYSTEM, (short) 0);
		}
		aktualisiereListe(container);
		registriereListener(container);
	}

	private void speichereAusOberflaeche(XWindow window) {
		persistiereUndBenachrichtige(container(window));
	}

	/**
	 * Persistiert die aktuelle Arbeitskopie der Composite Views (und das Webserver-Aktiv-Flag) sofort
	 * in {@link GlobalProperties} und benachrichtigt bei tatsaechlicher Aenderung den laufenden
	 * Webserver. Wird sowohl beim finalen „OK" der Optionsseite als auch direkt nach jeder
	 * Hinzufuegen-/Bearbeiten-/Loeschen-Aktion aufgerufen, damit bereits laufende Composite-View-Ansichten
	 * live aktualisiert werden.
	 */
	private void persistiereUndBenachrichtige(XControlContainer container) {
		GlobalProperties properties = GlobalProperties.get();
		boolean alterWebserverAktiv = properties.isWebserverAktiv();
		boolean neuerWebserverAktiv = checkbox(container, CTL_WEBSERVER_AKTIV);
		List<CompositeViewEintragRoh> alteEintraege = properties.getCompositeViewEintraege();

		String fehler = validiereEintraege(properties);
		if (fehler != null) {
			zeigeFehler(fehler);
			return;
		}
		properties.speichernCompositeViews(neuerWebserverAktiv, eintraege);
		if (alterWebserverAktiv != neuerWebserverAktiv || !alteEintraege.equals(eintraege)) {
			WebServerManager.get().konfigurationGeaendert();
			benachrichtigeCompositeViewKonfigurationGeaendert();
		}
	}

	private void setzeLabels(XControlContainer container) {
		setLabel(container, CTL_COMPOSITE_VIEWS_LABEL, I18n.get("konfig.webserver.views.bereich"));
		setLabel(container, CTL_WEBSERVER_AKTIV, I18n.get("konfig.webserver.views.aktiv"));
		setLabel(container, CTL_FILTER_LABEL, I18n.get("webserver.composite.konfig.filter.system"));
		setLabel(container, CTL_BEREICH, I18n.get("webserver.composite.konfig.bereich.views"));
		setLabel(container, CTL_HINZUFUEGEN, I18n.get("webserver.composite.konfig.btn.hinzufuegen"));
		setLabel(container, CTL_BEARBEITEN, I18n.get("webserver.composite.konfig.btn.bearbeiten"));
		setLabel(container, CTL_LOESCHEN, I18n.get("webserver.composite.konfig.btn.loeschen"));
	}

	private void registriereListener(XControlContainer container) {
		if (listenerContainer != null && UnoRuntime.areSame(listenerContainer, container)) {
			return;
		}
		registriereActionListener(container, CTL_HINZUFUEGEN, () -> fuegeZeileHinzu(container));
		registriereActionListener(container, CTL_BEARBEITEN, () -> bearbeiteZeile(container));
		registriereActionListener(container, CTL_LOESCHEN, () -> loescheZeile(container));
		listenerContainer = container;
	}

	// ---- Aktionen ----

	private void fuegeZeileHinzu(XControlContainer container) {
		try {
			aktualisiereKomboBoxItems(container);
			var tempIdx = new int[] { -1 };
			Consumer<CompositeViewEintragRoh> callback = e -> {
				if (tempIdx[0] == -1) {
					eintraege.add(e);
					tempIdx[0] = eintraege.size() - 1;
				} else {
					eintraege.set(tempIdx[0], e);
				}
				aktualisiereListe(container);
				persistiereUndBenachrichtige(container);
			};
			var detailDialog = new CompositeViewDetailDialog(
					context, null, berechneNaechstenFreienPort(), komboBoxItems, callback, pagePeer);
			var neuerEintrag = detailDialog.zeigen();
			if (neuerEintrag != null && tempIdx[0] == -1) {
				// OK ohne vorheriges Anwenden: normaler Add-Pfad
				eintraege.add(neuerEintrag);
			}
			aktualisiereListe(container);
		} catch (com.sun.star.uno.Exception e) {
			logger.error("Fehler beim Hinzufügen eines Composite Views: {}", e.getMessage(), e);
		}
	}

	private void bearbeiteZeile(XControlContainer container) {
		int idx = selectedPos(container, CTL_LISTE);
		if (idx < 0 || idx >= eintraege.size()) {
			zeigeFehler(I18n.get("webserver.composite.konfig.fehler.keine.auswahl"));
			return;
		}
		try {
			aktualisiereKomboBoxItems(container);
			var eintrag = eintraege.get(idx);
			Consumer<CompositeViewEintragRoh> callback = geaendert -> {
				migriereRegieZieleBeiPortWechsel(eintrag, geaendert);
				eintraege.set(idx, geaendert);
				aktualisiereListe(container);
				persistiereUndBenachrichtige(container);
			};
			var detailDialog = new CompositeViewDetailDialog(
					context, eintrag, eintrag.port(), komboBoxItems, callback, pagePeer);
			var geaenderterEintrag = detailDialog.zeigen();
			if (geaenderterEintrag != null) {
				// idempotent falls Callback schon gesetzt hat (migriereRegieZieleBeiPortWechsel ist
				// no-op, wenn die alte viewId bereits migriert wurde)
				migriereRegieZieleBeiPortWechsel(eintrag, geaenderterEintrag);
				eintraege.set(idx, geaenderterEintrag);
			}
			aktualisiereListe(container);
		} catch (com.sun.star.uno.Exception e) {
			logger.error("Fehler beim Bearbeiten des Composite Views: {}", e.getMessage(), e);
		}
	}

	/**
	 * Composite-View-Ports sind frei editierbar, wodurch sich die aus dem Port abgeleitete
	 * {@code viewId} (siehe {@link WebServerManager#compositeViewId(int)}) aendert. Ohne diese
	 * Migration bleiben Webserver-Regie-Ziele, die auf die alte viewId zeigen, verwaist: Die
	 * View-Auswahl in der Regie-Sidebar zeigt dann keine Selektion mehr, sichtbar spaetestens nach
	 * einem Neuaufbau der Sidebar (z.B. nach einem Neustart).
	 */
	private static void migriereRegieZieleBeiPortWechsel(CompositeViewEintragRoh alt, CompositeViewEintragRoh neu) {
		if (alt.port() == neu.port()) {
			return;
		}
		GlobalProperties.get().migriereWebserverRegieViewId(
				WebServerManager.compositeViewId(alt.port()), WebServerManager.compositeViewId(neu.port()));
	}

	private void loescheZeile(XControlContainer container) {
		int idx = selectedPos(container, CTL_LISTE);
		if (idx < 0 || idx >= eintraege.size()) {
			zeigeFehler(I18n.get("webserver.composite.konfig.fehler.keine.auswahl"));
			return;
		}
		var entfernt = eintraege.remove(idx);
		GlobalProperties.get().entferneWebserverRegieViewId(WebServerManager.compositeViewId(entfernt.port()));
		aktualisiereListe(container);
		persistiereUndBenachrichtige(container);
	}

	private void benachrichtigeCompositeViewKonfigurationGeaendert() {
		// Best-effort: Nur das aktuell aktive Dokument (falls vorhanden) über die Änderung informieren.
		var doc = DocumentHelper.getCurrentSpreadsheetDocument(context);
		if (doc == null) {
			return;
		}
		PetanqueTurnierMngrSingleton.triggerTurnierEventListener(TurnierEventType.PropertiesChanged,
				new OnProperiesChangedEvent(doc)
						.addChanged("webserver_composite_views", "", ""));
	}

	private String validiereEintraege(GlobalProperties properties) {
		Set<Integer> bekannte = new HashSet<>();
		for (int i = 0; i < eintraege.size(); i++) {
			var e = eintraege.get(i);
			int nr = i + 1;
			if (e.port() == 0) {
				return I18n.get("webserver.composite.konfig.fehler.port.leer", nr);
			}
			if (e.port() < 1 || e.port() > 65535) {
				return I18n.get("webserver.composite.konfig.fehler.port.ungueltig", nr, e.port());
			}
			if (!bekannte.add(e.port())) {
				return I18n.get("webserver.composite.konfig.fehler.port.duplikat", e.port());
			}
			if (e.panels().isEmpty()) {
				return I18n.get("webserver.composite.konfig.fehler.kein.panel");
			}
		}
		if (properties.isWebserverRegieAktiv()) {
			int regiePort = properties.getWebserverRegiePort();
			if (bekannte.contains(regiePort)) {
				return I18n.get("webserver.regie.konfig.fehler.port.duplikat", regiePort);
			}
		}
		return null;
	}

	private void zeigeFehler(String meldung) {
		MessageBox.from(context, MessageBoxTypeEnum.ERROR_OK)
				.caption(I18n.get("webserver.composite.konfig.fehler.titel"))
				.message(meldung)
				.show();
	}

	// ---- Hilfsmethoden ----

	private int berechneNaechstenFreienPort() {
		Set<Integer> belegt = new HashSet<>();
		for (var e : eintraege) {
			belegt.add(e.port());
		}
		int kandidat = 9100;
		while (belegt.contains(kandidat)) {
			kandidat++;
		}
		return kandidat;
	}

	/** Einträge der Filter-Liste: „Alle" plus alle auswählbaren Turniersysteme. */
	private static String[] filterEintraege() {
		String[] items = new String[FILTER_SYSTEME.length + 1];
		items[0] = I18n.get("webserver.composite.konfig.filter.alle");
		for (int i = 0; i < FILTER_SYSTEME.length; i++) {
			items[i + 1] = FILTER_SYSTEME[i].getBezeichnung();
		}
		return items;
	}

	/** Aktuell im Filter gewähltes Turniersystem; {@code null} für „Alle". */
	private TurnierSystem ausgewaehltesFilterSystem(XControlContainer container) {
		short pos = selectedPos(container, CTL_FILTER_SYSTEM);
		return (pos <= 0) ? null : FILTER_SYSTEME[pos - 1];
	}

	/** Berechnet die Blatt-Typ-Vorschläge des Detail-Dialogs anhand der aktuellen Filterauswahl neu. */
	private void aktualisiereKomboBoxItems(XControlContainer container) {
		komboBoxItems = SheetResolverFactory.sheetTypenFuer(ausgewaehltesFilterSystem(container));
	}

	private void aktualisiereListe(XControlContainer container) {
		String[] items = eintraege.stream().map(CompositeViewsOptionsEventHandler::formatiereZeile)
				.toArray(String[]::new);
		setListItems(container, CTL_LISTE, items);
	}

	private static String formatiereZeile(CompositeViewEintragRoh e) {
		String anzeigeName;
		if (!e.name().isBlank()) {
			anzeigeName = e.name();
		} else {
			anzeigeName = e.panels().isEmpty() ? "" : e.panels().get(0).sheetConfig();
		}
		String status = e.aktiv()
				? I18n.get("webserver.konfig.tabelle.kopf.aktiv")
				: I18n.get("webserver.composite.konfig.liste.status.inaktiv");
		return I18n.get("webserver.composite.konfig.liste.zeile", e.port(), anzeigeName, e.zoom(), status);
	}

	// ---- UNO-Control-Hilfsmethoden ----

	private static XControlContainer container(XWindow window) {
		XControlContainer container = UnoRuntime.queryInterface(XControlContainer.class, window);
		if (container == null) {
			throw new IllegalStateException("Optionsseite hat kein XControlContainer");
		}
		return container;
	}

	/**
	 * Peer der Optionsseite, als Parent fuer modale Detail-Dialoge. Das an {@code callHandlerMethod}
	 * uebergebene {@code window} ist selbst kein {@link XWindowPeer} (siehe LO-Quelle
	 * {@code cui/source/options/treeopt.cxx}, {@code ExtensionsTabPage::CreateDialogWithHandler}) –
	 * es implementiert {@link XControl}, dessen {@code getPeer()} den echten, in den
	 * Optionen-Dialog eingehaengten Fenster-Peer liefert. Ohne diesen Peer erkennt der
	 * Fenster-Manager den Detail-Dialog nicht als Kind des Optionen-Dialogs, wodurch dieser
	 * bedienbar bleibt, waehrend der Detail-Dialog geoeffnet ist (fehlende Modalitaet).
	 */
	private static XWindowPeer windowPeer(XWindow window) {
		XControl control = UnoRuntime.queryInterface(XControl.class, window);
		return control == null ? null : control.getPeer();
	}

	private static boolean checkbox(XControlContainer container, String name) {
		XCheckBox checkBox = control(container, name, XCheckBox.class);
		return checkBox != null && checkBox.getState() == 1;
	}

	private static void setCheckbox(XControlContainer container, String name, boolean wert) {
		XCheckBox checkBox = control(container, name, XCheckBox.class);
		if (checkBox != null) {
			checkBox.setState((short) (wert ? 1 : 0));
		}
	}

	private static short selectedPos(XControlContainer container, String name) {
		XListBox listBox = control(container, name, XListBox.class);
		return listBox == null ? -1 : listBox.getSelectedItemPos();
	}

	private static void setSelectedPos(XControlContainer container, String name, short pos) {
		XListBox listBox = control(container, name, XListBox.class);
		if (listBox != null) {
			listBox.selectItemPos(pos, true);
		}
	}

	private static void setListItems(XControlContainer container, String name, String[] items) {
		XControl control = container.getControl(name);
		if (control == null) {
			return;
		}
		XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, control.getModel());
		if (props == null) {
			return;
		}
		try {
			props.setPropertyValue("StringItemList", items);
		} catch (Exception e) {
			logger.debug("StringItemList fuer Control {} konnte nicht gesetzt werden", name, e);
		}
	}

	private static void setLabel(XControlContainer container, String name, String label) {
		XControl control = container.getControl(name);
		if (control == null) {
			return;
		}
		XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, control.getModel());
		if (props == null) {
			return;
		}
		try {
			props.setPropertyValue("Label", label);
		} catch (Exception e) {
			logger.debug("Label fuer Control {} konnte nicht gesetzt werden", name, e);
		}
	}

	private static void registriereActionListener(XControlContainer container, String name, Runnable aktion) {
		XButton button = control(container, name, XButton.class);
		if (button == null) {
			return;
		}
		button.addActionListener(new XActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				aktion.run();
			}

			@Override
			public void disposing(EventObject event) {
				// nichts zu tun
			}
		});
	}

	private static <T> T control(XControlContainer container, String name, Class<T> type) {
		XControl control = container.getControl(name);
		return control == null ? null : UnoRuntime.queryInterface(type, control);
	}

	@Override
	public String getImplementationName() {
		return IMPLEMENTATION_NAME;
	}

	@Override
	public boolean supportsService(String name) {
		return Arrays.asList(SERVICE_NAMES).contains(name);
	}

	@Override
	public String[] getSupportedServiceNames() {
		return SERVICE_NAMES;
	}

	public static boolean __writeRegistryServiceInfo(XRegistryKey registryKey) {
		return Factory.writeRegistryServiceInfo(IMPLEMENTATION_NAME, SERVICE_NAMES, registryKey);
	}

	public static XSingleComponentFactory __getComponentFactory(String implementationName) {
		if (IMPLEMENTATION_NAME.equals(implementationName)) {
			return Factory.createComponentFactory(CompositeViewsOptionsEventHandler.class, SERVICE_NAMES);
		}
		return null;
	}
}
