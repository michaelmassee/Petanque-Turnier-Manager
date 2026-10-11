package de.petanqueturniermanager.toolbar;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.DispatchDescriptor;
import com.sun.star.frame.FeatureStateEvent;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XDispatchProviderInterceptor;
import com.sun.star.frame.XStatusListener;
import com.sun.star.lang.EventObject;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.util.URL;

/**
 * Blockiert Formatierungsbefehle, die LibreOffice auch auf bewusst entsperrten
 * Eingabezellen ausführen würde.
 * <p>
 * Der Blattschutz lässt die vorgesehenen Werteingaben zu. Dieser Filter
 * verhindert ergänzend, dass dabei das Tabellenlayout über Tastaturkürzel,
 * Kontextmenü oder Einfügen verändert wird. Normales Einfügen bleibt möglich,
 * wird aber auf „Unformatierten Text einfügen" umgeleitet, damit nur Werte
 * und keine Zellformate übernommen werden. Er wird nur für die Dauer des
 * Turniermodus am jeweiligen Dokumentrahmen registriert.
 * <p>
 * Nicht abgedeckt ist Drag &amp; Drop von Zellen: dafür gibt es keinen
 * Dispatch-Befehl, den ein Interceptor abfangen könnte.
 */
final class KioskFormatierungsSchutz extends WeakBase implements XDispatchProviderInterceptor {

    private static final Set<String> BLOCKIERTE_BEFEHLE = Set.of(
            // Direkte Formatierung entfernen bzw. ändern
            ".uno:ResetAttributes",
            ".uno:FormatCellDialog",
            ".uno:FormatPaintbrush",
            ".uno:Bold",
            ".uno:Italic",
            ".uno:UnderlineSingle",
            ".uno:UnderlineDouble",
            ".uno:Strikeout",
            ".uno:Overline",
            ".uno:SuperScript",
            ".uno:SubScript",
            ".uno:Shadowed",
            ".uno:OutlineFont",
            ".uno:WrapText",
            ".uno:CommonAlignLeft",
            ".uno:CommonAlignHorizontalCenter",
            ".uno:CommonAlignRight",
            ".uno:CommonAlignJustified",
            ".uno:CommonAlignTop",
            ".uno:CommonAlignVerticalCenter",
            ".uno:CommonAlignBottom",
            ".uno:NumberFormatStandard",
            ".uno:NumberFormatDecimal",
            ".uno:NumberFormatPercent",
            ".uno:NumberFormatCurrency",
            ".uno:NumberFormatDate",
            ".uno:NumberFormatTime",
            ".uno:NumberFormatScientific",
            ".uno:NumberFormatThousands",
            ".uno:EditStyle",
            ".uno:CurrentConditionalFormatDialog",
            ".uno:CurrentConditionalFormatManagerDialog",
            // Der Dialog „Inhalte löschen“, Ausschneiden (verschiebt die Zellformate)
            // und Inhalte-einfügen-Varianten können direkte Formatierungen der
            // Eingabezelle entfernen bzw. überschreiben.
            ".uno:Delete",
            ".uno:Cut",
            ".uno:PasteSpecial",
            ".uno:PasteTransposed");

    /** Befehle, die statt auf das Original auf eine formatneutrale Variante umgeleitet werden. */
    private static final Map<String, String> UMGELEITETE_BEFEHLE = Map.of(
            ".uno:Paste", ".uno:PasteUnformatted");

    private static final String UNO_PROTOKOLL = ".uno:";

    private final XDispatch blockierterDispatch = new BlockierterDispatch();
    private volatile XDispatchProvider slave;
    private volatile XDispatchProvider master;

    static boolean istBlockierterBefehl(String completeUrl) {
        return BLOCKIERTE_BEFEHLE.contains(completeUrl);
    }

    static boolean istUmgeleiteterBefehl(String completeUrl) {
        return UMGELEITETE_BEFEHLE.containsKey(completeUrl);
    }

    @Override
    public XDispatch queryDispatch(URL url, String targetFrameName, int searchFlags) {
        if (url != null && istBlockierterBefehl(url.Complete)) {
            return blockierterDispatch;
        }
        XDispatchProvider aktuellerSlave = slave;
        if (aktuellerSlave == null) {
            return null;
        }
        if (url != null && istUmgeleiteterBefehl(url.Complete)) {
            XDispatch ziel = aktuellerSlave.queryDispatch(unoUrl(UMGELEITETE_BEFEHLE.get(url.Complete)),
                    targetFrameName, searchFlags);
            return ziel == null ? blockierterDispatch : new UmgeleiteterDispatch(ziel, url);
        }
        return aktuellerSlave.queryDispatch(url, targetFrameName, searchFlags);
    }

    /** Baut eine bereits zerlegte {@code .uno:}-URL, wie sie der URLTransformer liefern würde. */
    static URL unoUrl(String complete) {
        var url = new URL();
        url.Complete = complete;
        url.Main = complete;
        url.Protocol = UNO_PROTOKOLL;
        url.Path = complete.substring(UNO_PROTOKOLL.length());
        return url;
    }

    @Override
    public XDispatch[] queryDispatches(DispatchDescriptor[] requests) {
        XDispatch[] dispatches = new XDispatch[requests.length];
        for (int index = 0; index < requests.length; index++) {
            DispatchDescriptor request = requests[index];
            dispatches[index] = queryDispatch(request.FeatureURL, request.FrameName, request.SearchFlags);
        }
        return dispatches;
    }

    @Override
    public XDispatchProvider getSlaveDispatchProvider() {
        return slave;
    }

    @Override
    public void setSlaveDispatchProvider(XDispatchProvider slave) {
        this.slave = slave;
    }

    @Override
    public XDispatchProvider getMasterDispatchProvider() {
        return master;
    }

    @Override
    public void setMasterDispatchProvider(XDispatchProvider master) {
        this.master = master;
    }

    private static final class BlockierterDispatch extends WeakBase implements XDispatch {

        @Override
        public void dispatch(URL url, PropertyValue[] args) {
            // Absichtlich leer: Im Kioskmodus darf der Befehl keine Änderung ausführen.
        }

        @Override
        public void addStatusListener(XStatusListener listener, URL url) {
            if (listener == null) return;
            listener.statusChanged(new FeatureStateEvent(this, url, "", false, false, null));
        }

        @Override
        public void removeStatusListener(XStatusListener listener, URL url) {
            // Kein Listener-Zustand wird gehalten.
        }

    }

    /**
     * Führt einen Befehl über den Dispatch eines Ersatzbefehls aus. Statusmeldungen
     * werden auf die ursprünglich angefragte URL umgeschrieben, damit Menü- und
     * Toolbar-Controller sie ihrem Eintrag zuordnen.
     */
    private static final class UmgeleiteterDispatch extends WeakBase implements XDispatch {

        private final XDispatch ziel;
        private final URL ursprung;
        private final Map<XStatusListener, XStatusListener> weiterleitungen = new ConcurrentHashMap<>();

        UmgeleiteterDispatch(XDispatch ziel, URL ursprung) {
            this.ziel = ziel;
            this.ursprung = ursprung;
        }

        @Override
        public void dispatch(URL url, PropertyValue[] args) {
            ziel.dispatch(zielUrl(), args);
        }

        @Override
        public void addStatusListener(XStatusListener listener, URL url) {
            if (listener == null) return;
            XStatusListener weiterleitung = weiterleitungen.computeIfAbsent(listener, UrlUmschreibenderListener::new);
            ziel.addStatusListener(weiterleitung, zielUrl());
        }

        @Override
        public void removeStatusListener(XStatusListener listener, URL url) {
            if (listener == null) return;
            XStatusListener weiterleitung = weiterleitungen.remove(listener);
            if (weiterleitung != null) {
                ziel.removeStatusListener(weiterleitung, zielUrl());
            }
        }

        private URL zielUrl() {
            return unoUrl(UMGELEITETE_BEFEHLE.get(ursprung.Complete));
        }

        private final class UrlUmschreibenderListener extends WeakBase implements XStatusListener {

            private final XStatusListener empfaenger;

            UrlUmschreibenderListener(XStatusListener empfaenger) {
                this.empfaenger = empfaenger;
            }

            @Override
            public void statusChanged(FeatureStateEvent event) {
                empfaenger.statusChanged(new FeatureStateEvent(UmgeleiteterDispatch.this, ursprung,
                        event.FeatureDescriptor, event.IsEnabled, event.Requery, event.State));
            }

            @Override
            public void disposing(EventObject source) {
                empfaenger.disposing(source);
            }
        }
    }
}
