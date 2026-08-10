/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XContainerWindowEventHandler;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XItemListener;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XWindow;
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

import de.petanqueturniermanager.comp.GlobalProperties.WhatsAppChatEintrag;
import de.petanqueturniermanager.helper.BrowserOeffner;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.whatsapp.WhatsAppBridgeChat;
import de.petanqueturniermanager.whatsapp.WhatsAppBridgeClient;
import de.petanqueturniermanager.whatsapp.WhatsAppBridgeException;
import de.petanqueturniermanager.whatsapp.WhatsAppBridgeManager;
import de.petanqueturniermanager.whatsapp.WhatsAppBridgeSetup;
import de.petanqueturniermanager.whatsapp.WhatsAppBridgeSetupRequiredException;
import de.petanqueturniermanager.whatsapp.WhatsAppBridgeStatus;

/**
 * Event-Handler fuer die WhatsApp-Chat-Seite unter Extras -&gt; Optionen.
 */
public final class WhatsAppChatOptionsEventHandler extends WeakBase
		implements XServiceInfo, XContainerWindowEventHandler {

	private static final Logger logger = LogManager.getLogger(WhatsAppChatOptionsEventHandler.class);

	private static final String IMPLEMENTATION_NAME = WhatsAppChatOptionsEventHandler.class.getName();
	private static final String SERVICE_NAME = "de.petanqueturniermanager.WhatsAppChatOptionsEventHandler";
	private static final String[] SERVICE_NAMES = { SERVICE_NAME };

	private static final String METHOD_EXTERNAL_EVENT = "external_event";
	private static final String EVENT_INITIALIZE = "initialize";
	private static final String EVENT_BACK = "back";
	private static final String EVENT_OK = "ok";

	private static final String CTL_LABEL = "WhatsAppChatLabel";
	private static final String CTL_LISTE = "WhatsAppChatListe";
	private static final String CTL_HINZUFUEGEN = "WhatsAppChatHinzufuegen";
	private static final String CTL_BEARBEITEN = "WhatsAppChatBearbeiten";
	private static final String CTL_KLONEN = "WhatsAppChatKlonen";
	private static final String CTL_LOESCHEN = "WhatsAppChatLoeschen";
	private static final String CTL_FAVORIT = "WhatsAppChatFavorit";
	private static final String CTL_LOGIN = "WhatsAppChatLogin";
	private static final String CTL_AKTUALISIEREN = "WhatsAppChatAktualisieren";
	private static final String CTL_STATUS = "WhatsAppChatStatus";

	private final XComponentContext context;
	private XControlContainer listenerContainer;
	private List<WhatsAppChatEintrag> eintraege;

	public WhatsAppChatOptionsEventHandler(XComponentContext context) {
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
				persistiere();
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
		setLabel(container, CTL_LABEL, I18n.get("whatsapp.chat.konfig.bereich"));
		setLabel(container, CTL_HINZUFUEGEN, I18n.get("whatsapp.chat.konfig.btn.hinzufuegen"));
		setLabel(container, CTL_BEARBEITEN, I18n.get("whatsapp.chat.konfig.btn.bearbeiten"));
		setLabel(container, CTL_KLONEN, I18n.get("whatsapp.chat.konfig.btn.klonen"));
		setLabel(container, CTL_LOESCHEN, I18n.get("whatsapp.chat.konfig.btn.loeschen"));
		setLabel(container, CTL_FAVORIT, I18n.get("whatsapp.chat.konfig.favorit"));
		setLabel(container, CTL_LOGIN, I18n.get("whatsapp.chat.konfig.btn.login"));
		setLabel(container, CTL_AKTUALISIEREN, I18n.get("whatsapp.chat.konfig.btn.aktualisieren"));
		if (eintraege == null) {
			eintraege = new ArrayList<>(GlobalProperties.get().getWhatsAppChatEintraege());
		}
		aktualisiereListe(container);
		setStatus(container, I18n.get("whatsapp.chat.konfig.status.bereit"));
		registriereListener(container);
	}

	private void persistiere() {
		GlobalProperties.get().speichernWhatsAppChats(eintraege);
	}

	private void registriereListener(XControlContainer container) {
		if (listenerContainer != null && UnoRuntime.areSame(listenerContainer, container)) {
			return;
		}
		registriereActionListener(container, CTL_HINZUFUEGEN, () -> aktualisiereAusBridge(container));
		registriereActionListener(container, CTL_BEARBEITEN, () -> aktualisiereAusBridge(container));
		registriereActionListener(container, CTL_KLONEN, () -> kloneZeile(container));
		registriereActionListener(container, CTL_LOESCHEN, () -> loescheZeile(container));
		registriereActionListener(container, CTL_LOGIN, () -> loginAnzeigen(container));
		registriereActionListener(container, CTL_AKTUALISIEREN, () -> aktualisiereAusBridge(container));
		registriereFavoritListener(container);
		registriereAuswahlListener(container, () -> aktualisiereAuswahlAbhaengigeButtons(container));
		listenerContainer = container;
	}

	private static final long QR_POLL_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(15);
	private static final long QR_POLL_INTERVAL_MILLIS = 300;

	private void loginAnzeigen(XControlContainer container) {
		setStatus(container, I18n.get("whatsapp.chat.konfig.status.bridge.startet"));
		new Thread(() -> {
				try {
					var client = starteBridgeMitOptionalemSetup(container);
					var status = warteAufQrOderVerbindung(client);
				if (status.brauchtQrCode()) {
					BrowserOeffner.oeffne(client.qrCodeUri());
					LoMainThread.post(context, () ->
							setStatus(container, I18n.get("whatsapp.chat.konfig.status.qr.geoeffnet")));
					return;
				}
				String meldung = status.verbunden()
						? I18n.get("whatsapp.chat.konfig.status.verbunden")
						: I18n.get("whatsapp.chat.konfig.status.nicht.verbunden", status.status());
				LoMainThread.post(context, () -> setStatus(container, meldung));
			} catch (Exception e) {
				logger.error("WhatsApp-Login/QR konnte nicht geöffnet werden", e);
				LoMainThread.post(context, () -> {
					zeigeFehler(e.getMessage());
					setStatus(container, I18n.get("whatsapp.chat.konfig.status.fehler", e.getMessage()));
				});
			}
		}, "PTM-WhatsApp-Login").start();
	}

	/**
	 * {@link WhatsAppBridgeManager#starteOderVerbinde()} liefert schon zurück, sobald der HTTP-Server
	 * antwortet – der Baileys-Client braucht danach aber noch etwas Zeit, bis er entweder einen QR-Code
	 * erzeugt oder sich mit einer bestehenden Session verbindet. Ohne dieses Polling zeigt der erste
	 * Login-Klick oft nur "starting" statt der QR-Seite.
	 */
	private WhatsAppBridgeStatus warteAufQrOderVerbindung(WhatsAppBridgeClient client) throws WhatsAppBridgeException {
		WhatsAppBridgeStatus status = client.status();
		long ende = System.nanoTime() + QR_POLL_TIMEOUT_NANOS;
		while (!status.brauchtQrCode() && !status.verbunden() && System.nanoTime() < ende) {
			try {
				Thread.sleep(QR_POLL_INTERVAL_MILLIS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new WhatsAppBridgeException("WhatsApp-Login-Polling wurde unterbrochen", e);
			}
			status = client.status();
		}
		return status;
	}

	private void aktualisiereAusBridge(XControlContainer container) {
		setStatus(container, I18n.get("whatsapp.chat.konfig.status.aktualisiere"));
		new Thread(() -> {
				try {
					var client = starteBridgeMitOptionalemSetup(container);
					List<WhatsAppBridgeChat> chats = client.chats();
				LoMainThread.post(context, () -> {
					int geaendert = mergeChats(chats);
					aktualisiereListe(container);
					persistiere();
					setStatus(container, I18n.get("whatsapp.chat.konfig.status.aktualisiert", geaendert));
				});
			} catch (WhatsAppBridgeException e) {
				logger.error("WhatsApp-Chats konnten nicht aktualisiert werden", e);
				LoMainThread.post(context, () -> {
					zeigeFehler(e.getMessage());
					setStatus(container, I18n.get("whatsapp.chat.konfig.status.fehler", e.getMessage()));
				});
			}
			}, "PTM-WhatsApp-Chats").start();
	}

	private WhatsAppBridgeClient starteBridgeMitOptionalemSetup(XControlContainer container)
			throws WhatsAppBridgeException {
		try {
			WhatsAppBridgeManager.vorbereiten(schritt -> zeigeSetupStatus(container, schritt));
		} catch (WhatsAppBridgeSetupRequiredException e) {
			if (!frageWhatsAppSetup()) {
				throw new WhatsAppBridgeException(I18n.get("whatsapp.chat.konfig.setup.abgebrochen"), e);
			}
			WhatsAppBridgeManager.installieren(schritt -> zeigeSetupStatus(container, schritt));
		}
		return WhatsAppBridgeManager.starteOderVerbinde();
	}

	/**
	 * Grosszügiger Sicherheitsnetz-Timeout gegen ein dauerhaftes Blockieren, falls
	 * {@link LoMainThread#post} das Runnable wider Erwarten nie abarbeitet – bewusst weit über
	 * jeder realistischen Nutzer-Bedenkzeit für den Ja/Nein-Dialog gewählt.
	 */
	private static final Duration WHATSAPP_SETUP_DIALOG_TIMEOUT = Duration.ofMinutes(30);

	private boolean frageWhatsAppSetup() throws WhatsAppBridgeException {
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<MessageBoxResult> result = new AtomicReference<>(MessageBoxResult.NO);
		LoMainThread.post(context, () -> {
			try {
				result.set(MessageBox.from(context, MessageBoxTypeEnum.QUESTION_YES_NO)
						.caption(I18n.get("whatsapp.chat.konfig.setup.titel"))
						.message(I18n.get("whatsapp.chat.konfig.setup.text"))
						.show());
			} finally {
				latch.countDown();
			}
		});
		try {
			if (!latch.await(WHATSAPP_SETUP_DIALOG_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
				throw new WhatsAppBridgeException("WhatsApp-Setup-Dialog hat nicht rechtzeitig geantwortet");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new WhatsAppBridgeException("WhatsApp-Setup-Dialog wurde unterbrochen", e);
		}
		return result.get() == MessageBoxResult.YES;
	}

	private void zeigeSetupStatus(XControlContainer container, WhatsAppBridgeSetup.Schritt schritt) {
		String key = switch (schritt) {
		case NODE_DOWNLOAD -> "whatsapp.chat.konfig.status.setup.node.download";
		case NODE_INSTALL -> "whatsapp.chat.konfig.status.setup.node.install";
		case BRIDGE_INSTALL -> "whatsapp.chat.konfig.status.setup.bridge.install";
		case FERTIG -> "whatsapp.chat.konfig.status.setup.fertig";
		};
		LoMainThread.post(context, () -> setStatus(container, I18n.get(key)));
	}

	private int mergeChats(List<WhatsAppBridgeChat> chats) {
		int geaendert = 0;
		String zeit = Instant.now().toString();
		Map<String, Integer> indexNachChatId = new HashMap<>();
		for (int i = 0; i < eintraege.size(); i++) {
			indexNachChatId.put(eintraege.get(i).chatId(), i);
		}
		for (WhatsAppBridgeChat chat : chats) {
			Integer idx = indexNachChatId.get(chat.id());
			var eintrag = new WhatsAppChatEintrag(idx != null ? eintraege.get(idx).id() : null,
					chat.name(), chat.id(), chat.type(), zeit, "", idx != null && eintraege.get(idx).favorit());
			if (idx != null) {
				eintraege.set(idx, eintrag);
			} else {
				eintraege.add(eintrag);
				indexNachChatId.put(chat.id(), eintraege.size() - 1);
			}
			geaendert++;
		}
		return geaendert;
	}

	private void kloneZeile(XControlContainer container) {
		int idx = selectedPos(container, CTL_LISTE);
		if (idx < 0 || idx >= eintraege.size()) {
			zeigeFehler(I18n.get("whatsapp.chat.konfig.fehler.keine.auswahl"));
			return;
		}
		var original = eintraege.get(idx);
		eintraege.add(new WhatsAppChatEintrag(null,
				I18n.get("whatsapp.chat.konfig.klon.name", original.anzeigeName()),
				original.chatId(), original.chatTyp(), original.zuletztGeprueftAm(), original.hinweis(),
				original.favorit()));
		aktualisiereListe(container);
	}

	private void registriereFavoritListener(XControlContainer container) {
		XCheckBox checkBox = control(container, CTL_FAVORIT, XCheckBox.class);
		if (checkBox == null) {
			return;
		}
		checkBox.addItemListener(new XItemListener() {
			@Override
			public void itemStateChanged(ItemEvent event) {
				favoritUmschalten(container, checkBox.getState() == 1);
			}

			@Override
			public void disposing(EventObject event) {
				// nichts zu tun
			}
		});
	}

	private void favoritUmschalten(XControlContainer container, boolean favorit) {
		int idx = selectedPos(container, CTL_LISTE);
		if (idx < 0 || idx >= eintraege.size()) {
			return;
		}
		var alt = eintraege.get(idx);
		eintraege.set(idx, new WhatsAppChatEintrag(alt.id(), alt.name(), alt.chatId(), alt.chatTyp(),
				alt.zuletztGeprueftAm(), alt.hinweis(), favorit));
		String ausgewaehlterChatId = alt.chatId();
		aktualisiereListe(container);
		selektiereChatId(container, ausgewaehlterChatId);
	}

	private void selektiereChatId(XControlContainer container, String chatId) {
		for (int i = 0; i < eintraege.size(); i++) {
			if (eintraege.get(i).chatId().equals(chatId)) {
				XListBox listBox = control(container, CTL_LISTE, XListBox.class);
				if (listBox != null) {
					listBox.selectItemPos((short) i, true);
				}
				aktualisiereAuswahlAbhaengigeButtons(container);
				return;
			}
		}
	}

	private void loescheZeile(XControlContainer container) {
		int idx = selectedPos(container, CTL_LISTE);
		if (idx < 0 || idx >= eintraege.size()) {
			zeigeFehler(I18n.get("whatsapp.chat.konfig.fehler.keine.auswahl"));
			return;
		}
		eintraege.remove(idx);
		aktualisiereListe(container);
	}

	private void aktualisiereListe(XControlContainer container) {
		eintraege.sort(GlobalProperties.whatsAppChatComparator());
		String[] items = eintraege.stream().map(WhatsAppChatOptionsEventHandler::formatiereZeile)
				.toArray(String[]::new);
		setListItems(container, CTL_LISTE, items);
		aktualisiereAuswahlAbhaengigeButtons(container);
	}

	private void aktualisiereAuswahlAbhaengigeButtons(XControlContainer container) {
		int idx = selectedPos(container, CTL_LISTE);
		boolean auswahlVorhanden = idx >= 0 && idx < eintraege.size();
		setEnabled(container, CTL_KLONEN, auswahlVorhanden);
		setEnabled(container, CTL_LOESCHEN, auswahlVorhanden);
		setEnabled(container, CTL_FAVORIT, auswahlVorhanden);
		setCheckbox(container, CTL_FAVORIT, auswahlVorhanden && eintraege.get(idx).favorit());
	}

	private static void setCheckbox(XControlContainer container, String name, boolean wert) {
		XCheckBox checkBox = control(container, name, XCheckBox.class);
		if (checkBox != null) {
			checkBox.setState((short) (wert ? 1 : 0));
		}
	}

	private void zeigeFehler(String meldung) {
		MessageBox.from(context, MessageBoxTypeEnum.ERROR_OK)
				.caption(I18n.get("whatsapp.chat.konfig.fehler.titel"))
				.message(meldung == null ? "" : meldung)
				.show();
	}

	private static String formatiereZeile(WhatsAppChatEintrag e) {
		String typ = e.chatTyp().isBlank() ? "Chat" : e.chatTyp();
		String zeile = I18n.get("whatsapp.chat.konfig.liste.zeile", e.anzeigeName(), typ, e.chatId());
		return e.favorit() ? "★ " + zeile : zeile;
	}

	private static XControlContainer container(XWindow window) {
		XControlContainer container = UnoRuntime.queryInterface(XControlContainer.class, window);
		if (container == null) {
			throw new IllegalStateException("Optionsseite hat kein XControlContainer");
		}
		return container;
	}

	private static short selectedPos(XControlContainer container, String name) {
		XListBox listBox = control(container, name, XListBox.class);
		return listBox == null ? -1 : listBox.getSelectedItemPos();
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

	private static void setEnabled(XControlContainer container, String name, boolean enabled) {
		XControl control = container.getControl(name);
		if (control == null) {
			return;
		}
		XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, control.getModel());
		if (props == null) {
			return;
		}
		try {
			props.setPropertyValue("Enabled", enabled);
		} catch (Exception e) {
			logger.debug("Enabled fuer Control {} konnte nicht gesetzt werden", name, e);
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

	private static void setStatus(XControlContainer container, String status) {
		setLabel(container, CTL_STATUS, status);
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

	private static void registriereAuswahlListener(XControlContainer container, Runnable aktion) {
		XListBox listBox = control(container, CTL_LISTE, XListBox.class);
		if (listBox == null) {
			return;
		}
		listBox.addItemListener(new XItemListener() {
			@Override
			public void itemStateChanged(ItemEvent event) {
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
			return Factory.createComponentFactory(WhatsAppChatOptionsEventHandler.class, SERVICE_NAMES);
		}
		return null;
	}
}
