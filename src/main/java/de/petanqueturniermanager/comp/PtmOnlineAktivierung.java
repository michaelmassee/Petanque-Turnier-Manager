package de.petanqueturniermanager.comp;

import org.apache.logging.log4j.LogManager;
import com.sun.star.uno.XComponentContext;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.ptmonline.PtmOnlineLiveBeobachter;

/** Zentrale, rein im Speicher abgefragte Freigabe. Ohne Zugang keine Online-Arbeit. */
public final class PtmOnlineAktivierung {
    private static volatile boolean aktiv;

    private PtmOnlineAktivierung() {}

    public static boolean istAktiv() {
        return aktiv;
    }

    static void beenden() {
        aktiv = false;
    }

    static void initialisieren(XComponentContext context) {
        try {
            aktiv = new LibreOfficePtmOnlineSpeicher(context).laden().isConfigured();
        } catch (IllegalStateException e) {
            aktiv = false;
            LogManager.getLogger(PtmOnlineAktivierung.class).debug("PTM-Online bleibt deaktiviert: Zugang nicht lesbar", e);
        }
    }

    static void aktualisieren(XComponentContext context, LibreOfficePtmOnlineSpeicher.Zugangsdaten daten) {
        aktiv = daten.isConfigured();
        if (!aktiv) PtmOnlineLiveBeobachter.konfigurationGeaendert(context);
        // Options-Callbacks nicht durch synchronen Menüaufbau reentrant betreten.
        LoMainThread.post(context, () -> {
            PtmOnlineLiveBeobachter.konfigurationGeaendert(context);
            PtmOnlineMenuController.aktualisiereAlle();
            ProtocolHandler.notifyAllListeners();
        });
    }

    static void setzenFuerTest(LibreOfficePtmOnlineSpeicher.Zugangsdaten daten) {
        aktiv = daten != null && daten.isConfigured();
    }
}
