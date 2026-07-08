/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XContainerWindowEventHandler;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
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

import de.petanqueturniermanager.comp.GlobalProperties.FtpServerEintrag;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.upload.FtpServerDetailDialog;

/**
 * Event-Handler fuer die FTP-Server-Seite unter Extras -&gt; Optionen.
 * <p>
 * Verwaltet die zentrale Liste der FTP/SFTP-Server direkt auf der Optionsseite
 * (Liste, Hinzufuegen/Bearbeiten/Loeschen). Die Detail-Konfiguration eines einzelnen
 * Servers (inkl. Passwort) erfolgt im modalen {@link FtpServerDetailDialog}.
 */
public final class FtpServerOptionsEventHandler extends WeakBase
		implements XServiceInfo, XContainerWindowEventHandler {

	private static final Logger logger = LogManager.getLogger(FtpServerOptionsEventHandler.class);

	private static final String IMPLEMENTATION_NAME = FtpServerOptionsEventHandler.class.getName();
	private static final String SERVICE_NAME = "de.petanqueturniermanager.FtpServerOptionsEventHandler";
	private static final String[] SERVICE_NAMES = { SERVICE_NAME };

	private static final String METHOD_EXTERNAL_EVENT = "external_event";
	private static final String EVENT_INITIALIZE = "initialize";
	private static final String EVENT_BACK = "back";
	private static final String EVENT_OK = "ok";

	private static final String CTL_FTP_SERVER_LABEL = "FtpServerLabel";
	private static final String CTL_LISTE = "FtpServerListe";
	private static final String CTL_HINZUFUEGEN = "FtpServerHinzufuegen";
	private static final String CTL_BEARBEITEN = "FtpServerBearbeiten";
	private static final String CTL_LOESCHEN = "FtpServerLoeschen";

	private final XComponentContext context;

	/** Container, an dessen Buttons bereits Listener haengen (verhindert Doppelregistrierung). */
	private XControlContainer listenerContainer;

	/** Arbeitskopie der FTP-Server; {@code null} solange die Seite noch nicht initialisiert wurde. */
	private List<FtpServerEintrag> eintraege;

	public FtpServerOptionsEventHandler(XComponentContext context) {
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
				speichereAusOberflaeche();
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
		setLabel(container, CTL_FTP_SERVER_LABEL, I18n.get("ftp.server.konfig.bereich"));
		setLabel(container, CTL_HINZUFUEGEN, I18n.get("ftp.server.konfig.btn.hinzufuegen"));
		setLabel(container, CTL_BEARBEITEN, I18n.get("ftp.server.konfig.btn.bearbeiten"));
		setLabel(container, CTL_LOESCHEN, I18n.get("ftp.server.konfig.btn.loeschen"));
		if (eintraege == null) {
			eintraege = new ArrayList<>(GlobalProperties.get().getFtpServerEintraege());
		}
		aktualisiereListe(container);
		registriereListener(container);
	}

	private void speichereAusOberflaeche() {
		persistiere();
	}

	private void persistiere() {
		GlobalProperties.get().speichernFtpServer(eintraege);
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
			Consumer<FtpServerEintrag> callback = e -> {
				eintraege.add(e);
				aktualisiereListe(container);
				persistiere();
			};
			var detailDialog = new FtpServerDetailDialog(context, null);
			var neuerEintrag = detailDialog.zeigen();
			if (neuerEintrag != null) {
				callback.accept(neuerEintrag);
			}
		} catch (com.sun.star.uno.Exception e) {
			logger.error("Fehler beim Hinzufügen eines FTP-Servers: {}", e.getMessage(), e);
		}
	}

	private void bearbeiteZeile(XControlContainer container) {
		int idx = selectedPos(container, CTL_LISTE);
		if (idx < 0 || idx >= eintraege.size()) {
			zeigeFehler(I18n.get("ftp.server.konfig.fehler.keine.auswahl"));
			return;
		}
		try {
			var eintrag = eintraege.get(idx);
			var detailDialog = new FtpServerDetailDialog(context, eintrag);
			var geaenderterEintrag = detailDialog.zeigen();
			if (geaenderterEintrag != null) {
				eintraege.set(idx, geaenderterEintrag);
				aktualisiereListe(container);
				persistiere();
			}
		} catch (com.sun.star.uno.Exception e) {
			logger.error("Fehler beim Bearbeiten des FTP-Servers: {}", e.getMessage(), e);
		}
	}

	private void loescheZeile(XControlContainer container) {
		int idx = selectedPos(container, CTL_LISTE);
		if (idx < 0 || idx >= eintraege.size()) {
			zeigeFehler(I18n.get("ftp.server.konfig.fehler.keine.auswahl"));
			return;
		}
		eintraege.remove(idx);
		aktualisiereListe(container);
		persistiere();
	}

	private void zeigeFehler(String meldung) {
		MessageBox.from(context, MessageBoxTypeEnum.ERROR_OK)
				.caption(I18n.get("ftp.server.konfig.fehler.titel"))
				.message(meldung)
				.show();
	}

	// ---- Hilfsmethoden ----

	private void aktualisiereListe(XControlContainer container) {
		String[] items = eintraege.stream().map(FtpServerOptionsEventHandler::formatiereZeile)
				.toArray(String[]::new);
		setListItems(container, CTL_LISTE, items);
	}

	private static String formatiereZeile(FtpServerEintrag e) {
		return I18n.get("ftp.server.konfig.liste.zeile", e.anzeigeName(), e.protokoll().name(), e.host(), e.port());
	}

	// ---- UNO-Control-Hilfsmethoden ----

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
			return Factory.createComponentFactory(FtpServerOptionsEventHandler.class, SERVICE_NAMES);
		}
		return null;
	}
}
