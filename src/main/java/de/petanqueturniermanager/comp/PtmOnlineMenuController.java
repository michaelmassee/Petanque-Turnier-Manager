package de.petanqueturniermanager.comp;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import com.sun.star.awt.MenuEvent;
import com.sun.star.awt.XMenuListener;
import com.sun.star.awt.XPopupMenu;
import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XPopupMenuController;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XEventListener;
import com.sun.star.lang.XInitialization;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.URL;
import com.sun.star.util.XURLTransformer;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;

/** LibreOffice verwaltet das Untermenü; seine Einträge existieren nur bei aktivem Online-Zugang. */
public final class PtmOnlineMenuController extends WeakBase
        implements XPopupMenuController, XInitialization, XMenuListener, XComponent, XServiceInfo {
    private static final String SERVICE = "de.petanqueturniermanager.PtmOnlineMenuController";
    private static final Map<PtmOnlineMenuController, Boolean> controller = new WeakHashMap<>();
    /** Befehle des Untermenüs; sie stehen in keiner XCU-Datei, LibreOffice fragt sie bei diesem Controller ab. */
    static final List<String> BEFEHLE = List.of(
        ProtocolHandler.CMD_PTMONLINE_TURNIER_VERBINDEN, ProtocolHandler.CMD_PTMONLINE_ANMELDUNGEN_IMPORTIEREN,
        ProtocolHandler.CMD_PTMONLINE_SYNC_PAUSIEREN, ProtocolHandler.CMD_PTMONLINE_SYNC_FORTSETZEN,
        ProtocolHandler.CMD_PTMONLINE_VERBINDUNG_TRENNEN, ProtocolHandler.CMD_PTMONLINE_ZUORDNUNG_WIEDERHERSTELLEN);
    private static final String[] TEXTE = {"verbinden", "abgleichen", "pausieren", "fortsetzen", "trennen", "wiederherstellen"};
    private final XComponentContext context;
    private final List<XEventListener> listener = new CopyOnWriteArrayList<>();
    private XFrame frame;
    private XPopupMenu popup;
    private volatile boolean disposed;

    public PtmOnlineMenuController(XComponentContext context) {
        this.context = context;
        synchronized (controller) { controller.put(this, Boolean.TRUE); }
    }

    @Override
    public void initialize(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof PropertyValue prop && "Frame".equals(prop.Name)) frame = Lo.qi(XFrame.class, prop.Value);
        }
    }

    @Override
    public void setPopupMenu(XPopupMenu menu) {
        if (disposed) return;
        if (popup != null) popup.removeMenuListener(this);
        popup = menu;
        popup.addMenuListener(this);
        updatePopupMenu();
    }

    @Override
    public void updatePopupMenu() {
        if (disposed || popup == null) return;
        popup.clear();
        if (!PtmOnlineAktivierung.istAktiv()) return;
        XSpreadsheetDocument doc = frame == null || frame.getController() == null ? null
                : Lo.qi(XSpreadsheetDocument.class, frame.getController().getModel());
        for (short i = 0; i < BEFEHLE.size(); i++) {
            short id = (short) (i + 1);
            popup.insertItem(id, I18n.get("ptmonline.menu." + TEXTE[i]), (short) 0, i);
            popup.setCommand(id, "ptm:" + BEFEHLE.get(i));
            popup.enableItem(id, ProtocolHandler.istOnlineBefehlAktiv(BEFEHLE.get(i), doc));
        }
    }

    /** Auf dem LO-Hauptthread, ohne Java-Monitor über UNO-Aufrufe. */
    static void aktualisiereAlle() {
        List<PtmOnlineMenuController> kopie;
        synchronized (controller) { kopie = List.copyOf(controller.keySet()); }
        kopie.forEach(PtmOnlineMenuController::updatePopupMenu);
    }

    @Override
    public void itemSelected(MenuEvent event) {
        if (disposed || !PtmOnlineAktivierung.istAktiv() || popup == null || frame == null) return;
        String command = popup.getCommand(event.MenuId);
        XFrame ziel = frame;
        LoMainThread.post(context, () -> {
            if (disposed || !PtmOnlineAktivierung.istAktiv()) return;
            try {
                URL url = new URL();
                url.Complete = command;
                var transformer = Lo.qi(XURLTransformer.class, context.getServiceManager()
                        .createInstanceWithContext("com.sun.star.util.URLTransformer", context));
                URL[] urls = {url};
                transformer.parseStrict(urls);
                var provider = Lo.qi(XDispatchProvider.class, ziel);
                var dispatch = provider == null ? null : provider.queryDispatch(urls[0], "_self", 0);
                if (dispatch != null) dispatch.dispatch(urls[0], new PropertyValue[0]);
            } catch (Exception e) {
                org.apache.logging.log4j.LogManager.getLogger(PtmOnlineMenuController.class)
                        .debug("Online-Menübefehl nicht ausführbar", e);
            }
        });
    }

    @Override public void itemActivated(MenuEvent event) {}
    @Override public void itemHighlighted(MenuEvent event) {}
    @Override public void itemDeactivated(MenuEvent event) {}
    @Override public void disposing(EventObject event) { dispose(); }
    @Override public void dispose() {
        if (disposed) return;
        disposed = true;
        synchronized (controller) { controller.remove(this); }
        if (popup != null) popup.removeMenuListener(this);
        popup = null;
        frame = null;
        listener.forEach(l -> l.disposing(new EventObject(this)));
        listener.clear();
    }
    @Override public void addEventListener(XEventListener l) { listener.add(l); }
    @Override public void removeEventListener(XEventListener l) { listener.remove(l); }
    @Override public String getImplementationName() { return getClass().getName(); }
    @Override public String[] getSupportedServiceNames() { return new String[] {SERVICE}; }
    @Override public boolean supportsService(String name) { return SERVICE.equals(name); }
    public static boolean __writeRegistryServiceInfo(XRegistryKey key) {
        return Factory.writeRegistryServiceInfo(PtmOnlineMenuController.class.getName(), new String[] {SERVICE}, key);
    }
    public static XSingleComponentFactory __getComponentFactory(String name) {
        return PtmOnlineMenuController.class.getName().equals(name)
                ? Factory.createComponentFactory(PtmOnlineMenuController.class, new String[] {SERVICE}) : null;
    }
}
