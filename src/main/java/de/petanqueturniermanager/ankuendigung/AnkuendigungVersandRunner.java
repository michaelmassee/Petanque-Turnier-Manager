/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ankuendigung;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.GlobalProperties.WhatsAppChatEintrag;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.ptmonline.PtmOnlineAnkuendigung;
import de.petanqueturniermanager.webserver.WebServerManager;
import de.petanqueturniermanager.whatsapp.WhatsAppBridgeException;
import de.petanqueturniermanager.whatsapp.WhatsAppBridgeManager;

/**
 * Verteilt die bereits im Dokument gespeicherte Ankündigung im Hintergrund: an den eingebauten Webserver, an ein
 * verbundenes PTM-Online-Turnier und auf Wunsch per WhatsApp. Jeder Kanal ist unabhängig – ein Fehler in einem
 * verhindert die übrigen nicht; am Ende fasst eine Meldung zusammen, was geklappt hat.
 */
public final class AnkuendigungVersandRunner extends SheetRunner {

    private final String text;
    private final WhatsAppChatEintrag whatsAppChat;

    /**
     * @param text         bereinigte Ankündigung; leer löscht sie (dann ohne WhatsApp-Nachricht)
     * @param whatsAppChat Chat für die WhatsApp-Nachricht oder {@code null} für keinen Versand per WhatsApp
     */
    public AnkuendigungVersandRunner(WorkingSpreadsheet ws, TurnierSystem turnierSystem, String text,
            WhatsAppChatEintrag whatsAppChat) {
        super(ws, turnierSystem, "Ankündigung");
        this.text = text;
        this.whatsAppChat = whatsAppChat;
    }

    @Override
    protected IKonfigurationSheet getKonfigurationSheet() {
        return null;
    }

    @Override
    protected void doRun() throws GenerateException {
        List<String> erledigt = new ArrayList<>();
        List<String> fehler = new ArrayList<>();
        verteileAnWebserver(erledigt);
        sendeAnPtmOnline(erledigt, fehler);
        sendePerWhatsApp(erledigt, fehler);
        zeigeErgebnis(erledigt, fehler);
    }

    private void verteileAnWebserver(List<String> erledigt) {
        WebServerManager webserver = WebServerManager.get();
        webserver.ankuendigungGeaendert(getWorkingSpreadsheet());
        if (webserver.istOwnerDocument(getWorkingSpreadsheet().getWorkingSpreadsheetDocument())) {
            erledigt.add(I18n.get("ankuendigung.ergebnis.webserver"));
        }
    }

    private void sendeAnPtmOnline(List<String> erledigt, List<String> fehler) throws GenerateException {
        try {
            if (PtmOnlineAnkuendigung.senden(getWorkingSpreadsheet(), getTurnierSystem(),
                    text) == PtmOnlineAnkuendigung.Ergebnis.GESENDET) {
                erledigt.add(I18n.get("ankuendigung.ergebnis.ptmonline"));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            getLogger().debug("Ankündigung: Versand an PTM-Online abgebrochen", e);
            throw verarbeitungAbgebrochen();
        } catch (GenerateException e) {
            if (istAbbruch(e)) {
                throw e;
            }
            getLogger().error("Ankündigung: Versand an PTM-Online fehlgeschlagen", e);
            fehler.add(I18n.get("ankuendigung.fehler.ptmonline", e.getMessage()));
        }
    }

    private void sendePerWhatsApp(List<String> erledigt, List<String> fehler) {
        if (whatsAppChat == null || text.isEmpty()) {
            return;
        }
        try {
            WhatsAppBridgeManager.sendeText(whatsAppChat.chatId(), I18n.get("ankuendigung.whatsapp.text", text));
            erledigt.add(I18n.get("ankuendigung.ergebnis.whatsapp", whatsAppChat.anzeigeName()));
        } catch (WhatsAppBridgeException e) {
            getLogger().error("Ankündigung: Versand per WhatsApp fehlgeschlagen", e);
            fehler.add(I18n.get("ankuendigung.fehler.whatsapp", e.getMessage()));
        }
    }

    private void zeigeErgebnis(List<String> erledigt, List<String> fehler) {
        List<String> absaetze = new ArrayList<>();
        absaetze.add(I18n.get(text.isEmpty() ? "ankuendigung.ergebnis.geloescht" : "ankuendigung.ergebnis.gespeichert"));
        absaetze.addAll(erledigt);
        absaetze.addAll(fehler);
        absaetze.forEach(absatz -> processBox().info(absatz));
        MessageBox.from(getxContext(), fehler.isEmpty() ? MessageBoxTypeEnum.INFO_OK : MessageBoxTypeEnum.WARN_OK)
                .caption(I18n.get("ankuendigung.titel")).message(String.join("\n\n", absaetze)).show();
    }
}
