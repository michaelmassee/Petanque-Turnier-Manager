package de.petanqueturniermanager.toolbar;

import java.util.Set;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.DispatchDescriptor;
import com.sun.star.frame.FeatureStateEvent;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XDispatchProviderInterceptor;
import com.sun.star.frame.XStatusListener;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.util.URL;

/**
 * Blockiert Formatierungsbefehle, die LibreOffice auch auf bewusst entsperrten
 * Eingabezellen ausführen würde.
 * <p>
 * Der Blattschutz lässt die vorgesehenen Werteingaben zu. Dieser Filter
 * verhindert ergänzend, dass dabei das Tabellenlayout über Tastaturkürzel,
 * Kontextmenü oder Einfügen verändert wird. Er wird nur für die Dauer des
 * Turniermodus am aktiven Dokumentrahmen registriert.
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
            // Der Dialog „Inhalte löschen“ und formatiertes Einfügen können
            // ebenfalls direkte Formatierungen der Eingabezelle entfernen.
            ".uno:Delete",
            ".uno:Paste",
            ".uno:PasteSpecial",
            ".uno:PasteTransposed");

    private final XDispatch blockierterDispatch = new BlockierterDispatch();
    private volatile XDispatchProvider slave;
    private volatile XDispatchProvider master;

    static boolean istBlockierterBefehl(String completeUrl) {
        return BLOCKIERTE_BEFEHLE.contains(completeUrl);
    }

    @Override
    public XDispatch queryDispatch(URL url, String targetFrameName, int searchFlags) {
        if (url != null && istBlockierterBefehl(url.Complete)) {
            return blockierterDispatch;
        }
        XDispatchProvider aktuellerSlave = slave;
        return aktuellerSlave == null ? null : aktuellerSlave.queryDispatch(url, targetFrameName, searchFlags);
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
}
