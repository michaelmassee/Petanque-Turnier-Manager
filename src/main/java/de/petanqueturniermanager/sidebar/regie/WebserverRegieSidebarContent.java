package de.petanqueturniermanager.sidebar.regie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.TextEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XCallback;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XItemListener;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XRequestCallback;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XTextListener;
import com.sun.star.awt.XWindow;
import com.sun.star.lang.EventObject;
import com.sun.star.ui.XSidebar;

import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.GlobalProperties.RegieZielRoh;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.sidebar.BaseSidebarContent;
import de.petanqueturniermanager.sidebar.GuiFactory;
import de.petanqueturniermanager.sidebar.layout.ControlLayout;
import de.petanqueturniermanager.sidebar.layout.HorizontalLayout;
import de.petanqueturniermanager.webserver.RegieQuelleInfo;
import de.petanqueturniermanager.webserver.WebServerManager;

public class WebserverRegieSidebarContent extends BaseSidebarContent {

    private static final Logger logger = LogManager.getLogger(WebserverRegieSidebarContent.class);
    private static final int ZEILE_H = 22;
    private static final int MIN_HOEHE = 90;

    private final ScheduledExecutorService speichernExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        var t = new Thread(r, "PTM-RegieSidebar-Speichern");
        t.setDaemon(true);
        return t;
    });
    private final List<RowControls> rows = new ArrayList<>();
    private final List<RegieQuelleInfo> quellen = new ArrayList<>();
    private XRequestCallback itemDispatcher;
    private volatile ScheduledFuture<?> speichernFuture;
    private volatile List<RegieZielRoh> pendingZiele = List.of();

    public WebserverRegieSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow,
            XSidebar xSidebar) {
        super(workingSpreadsheet, parentWindow, xSidebar);
    }

    @Override
    protected void felderHinzufuegen() {
        rows.clear();
        quellen.clear();
        quellen.addAll(WebServerManager.get().verfuegbareRegieQuellen());
        itemDispatcherInitialisieren();

        var header = new HorizontalLayout();
        header.addLayout(new ControlLayout(label("webserver.regie.sidebar.name", 90)), 4);
        header.addLayout(new ControlLayout(label("webserver.regie.sidebar.view", 80)), 4);
        header.addLayout(new ControlLayout(label("webserver.regie.sidebar.aktiv", 35), 35), 0);
        header.addLayout(new ControlLayout(label("", 22), 22), 0);
        getLayout().addLayout(header, 1);

        var ziele = GlobalProperties.get().getWebserverRegieZiele();
        for (int i = 0; i < ziele.size(); i++) {
            fuegeZielZeileHinzu(i, ziele.get(i));
        }
        fuegePlusZeileHinzu();
    }

    private XControl label(String key, int width) {
        return GuiFactory.createLabel(getGuiFactoryCreateParam(),
                key == null || key.isBlank() ? "" : I18n.get(key),
                new Rectangle(0, 0, width, ZEILE_H), null);
    }

    private void fuegeZielZeileHinzu(int index, RegieZielRoh ziel) {
        var row = new HorizontalLayout();
        XControl nameCtrl = GuiFactory.createTextfield(getGuiFactoryCreateParam(), ziel.name(),
                textListener, new Rectangle(0, 0, 90, ZEILE_H), null);
        XControl viewCtrl = GuiFactory.createListBox(getGuiFactoryCreateParam(), itemListener,
                new Rectangle(0, 0, 90, ZEILE_H), dropdownProps());
        XControl aktivCtrl = GuiFactory.createCheckBox(getGuiFactoryCreateParam().getxMCF(),
                getGuiFactoryCreateParam().getContext(), getGuiFactoryCreateParam().getToolkit(),
                getGuiFactoryCreateParam().getWindowPeer(), "", itemListener,
                new Rectangle(0, 0, 25, ZEILE_H), null);
        XControl delCtrl = GuiFactory.createButton(getGuiFactoryCreateParam(), "-",
                actionListener, new Rectangle(0, 0, 22, ZEILE_H), null);

        var listBox = Lo.qi(XListBox.class, viewCtrl);
        for (int i = 0; i < quellen.size(); i++) {
            var q = quellen.get(i);
            listBox.addItem(q.anzeigename() + " :" + q.port(), (short) i);
            if (q.viewId().equals(ziel.viewId())) {
                listBox.selectItemPos((short) i, true);
            }
        }
        Lo.qi(XCheckBox.class, aktivCtrl).setState((short) (ziel.aktiv() ? 1 : 0));
        Lo.qi(XButton.class, delCtrl).setActionCommand("delete:" + index);

        rows.add(new RowControls(ziel, Lo.qi(XTextComponent.class, nameCtrl), listBox,
                Lo.qi(XCheckBox.class, aktivCtrl)));
        row.addLayout(new ControlLayout(nameCtrl), 4);
        row.addLayout(new ControlLayout(viewCtrl), 4);
        row.addLayout(new ControlLayout(aktivCtrl, 35), 0);
        row.addLayout(new ControlLayout(delCtrl, 22), 0);
        getLayout().addLayout(row, 1);
    }

    private void fuegePlusZeileHinzu() {
        var row = new HorizontalLayout();
        XControl plus = GuiFactory.createButton(getGuiFactoryCreateParam(), "+",
                actionListener, new Rectangle(0, 0, 22, ZEILE_H), null);
        Lo.qi(XButton.class, plus).setActionCommand("add");
        row.addLayout(new ControlLayout(label("", 90)), 4);
        row.addLayout(new ControlLayout(label("", 90)), 4);
        row.addLayout(new ControlLayout(label("", 35), 35), 0);
        row.addLayout(new ControlLayout(plus, 22), 0);
        getLayout().addLayout(row, 1);
    }

    private static HashMap<String, Object> dropdownProps() {
        var props = new HashMap<String, Object>();
        props.put("Dropdown", Boolean.TRUE);
        props.put("LineCount", (short) 10);
        return props;
    }

    private void itemDispatcherInitialisieren() {
        if (itemDispatcher != null) {
            return;
        }
        try {
            var xContext = getCurrentSpreadsheet().getxContext();
            var asyncCallback = xContext.getServiceManager()
                    .createInstanceWithContext("com.sun.star.awt.AsyncCallback", xContext);
            itemDispatcher = Lo.qi(XRequestCallback.class, asyncCallback);
        } catch (Exception e) {
            logger.warn("RegieSidebar: AsyncCallback-Service nicht verfügbar", e);
        }
    }

    private final XActionListener actionListener = new XActionListener() {
        @Override
        public void actionPerformed(ActionEvent event) {
            int gen = getUiGeneration();
            Runnable aktion = () -> {
                if (!isUiAlive(gen)) return;
                String cmd = event.ActionCommand == null ? "" : event.ActionCommand;
                if ("add".equals(cmd)) {
                    zielHinzufuegen();
                } else if (cmd.startsWith("delete:")) {
                    zielLoeschen(Integer.parseInt(cmd.substring("delete:".length())));
                }
            };
            deferred(aktion);
        }

        @Override
        public void disposing(EventObject event) {
        }
    };

    private final XItemListener itemListener = new XItemListener() {
        @Override
        public void itemStateChanged(ItemEvent event) {
            speichernDebounced();
        }

        @Override
        public void disposing(EventObject event) {
        }
    };

    private final XTextListener textListener = new XTextListener() {
        @Override
        public void textChanged(TextEvent event) {
            speichernDebounced();
        }

        @Override
        public void disposing(EventObject event) {
        }
    };

    private void deferred(Runnable aktion) {
        if (itemDispatcher != null) {
            itemDispatcher.addCallback((XCallback) data -> aktion.run(), null);
        } else {
            LoMainThread.post(getCurrentSpreadsheet().getxContext(), aktion);
        }
    }

    private void zielHinzufuegen() {
        var ziele = aktuelleZiele();
        String basisName = I18n.get("webserver.regie.neues.ziel");
        String name = basisName;
        int nr = 1;
        while (nameSchonVorhanden(ziele, name)) {
            nr++;
            name = basisName + " " + nr;
        }
        String viewId = quellen.isEmpty() ? "" : quellen.get(0).viewId();
        ziele.add(new RegieZielRoh(null, name, "", false, viewId));
        speichernSofortUndNeuAufbauen(ziele);
    }

    private void zielLoeschen(int index) {
        var ziele = aktuelleZiele();
        if (index >= 0 && index < ziele.size()) {
            ziele.remove(index);
            speichernSofortUndNeuAufbauen(ziele);
        }
    }

    private static boolean nameSchonVorhanden(List<RegieZielRoh> ziele, String name) {
        return ziele.stream().anyMatch(z -> z.name().equalsIgnoreCase(name));
    }

    private List<RegieZielRoh> aktuelleZiele() {
        var result = new ArrayList<RegieZielRoh>();
        for (var row : rows) {
            String name = row.nameText().getText().trim();
            if (name.isBlank()) {
                continue;
            }
            short selected = row.viewList().getSelectedItemPos();
            String viewId = selected >= 0 && selected < quellen.size()
                    ? quellen.get(selected).viewId()
                    : row.ziel().viewId();
            result.add(new RegieZielRoh(row.ziel().id(), name, "", row.aktiv().getState() == 1,
                    viewId));
        }
        return result;
    }

    private void speichernDebounced() {
        pendingZiele = aktuelleZiele();
        var alt = speichernFuture;
        if (alt != null) {
            alt.cancel(false);
        }
        speichernFuture = speichernExecutor.schedule(() -> {
            var ziele = List.copyOf(pendingZiele);
            GlobalProperties.get().speichernWebserverRegie(
                    GlobalProperties.get().isWebserverRegieAktiv(),
                    GlobalProperties.get().getWebserverRegiePort(),
                    ziele);
            var ws = getCurrentSpreadsheet();
            if (ws != null) {
                LoMainThread.post(ws.getxContext(), WebServerManager.get()::konfigurationGeaendert);
            }
        }, 500, TimeUnit.MILLISECONDS);
    }

    private void speichernSofortUndNeuAufbauen(List<RegieZielRoh> ziele) {
        GlobalProperties.get().speichernWebserverRegie(
                GlobalProperties.get().isWebserverRegieAktiv(),
                GlobalProperties.get().getWebserverRegiePort(),
                ziele);
        WebServerManager.get().konfigurationGeaendert();
        allesFelderEntfernenUndNeuFenster();
    }

    @Override
    protected void felderAktualisieren(ITurnierEvent eventObj) {
        // Kein externer Statuslistener: Regie zieht die Quellenliste nur beim Aufbau.
    }

    @Override
    protected void onDisposing(EventObject event) {
        var future = speichernFuture;
        if (future != null) {
            future.cancel(false);
        }
        speichernExecutor.shutdownNow();
        rows.clear();
    }

    @Override
    public com.sun.star.ui.LayoutSize getHeightForWidth(int breite) {
        int hoehe = Math.max(MIN_HOEHE, (rows.size() + 2) * ZEILE_H + 8);
        return new com.sun.star.ui.LayoutSize(MIN_HOEHE, -1, hoehe);
    }

    private record RowControls(RegieZielRoh ziel, XTextComponent nameText, XListBox viewList, XCheckBox aktiv) {
    }
}
