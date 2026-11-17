/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ankuendigung;

import java.util.List;
import java.util.Optional;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.GlobalProperties.WhatsAppChatEintrag;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.whatsapp.WhatsAppPostRunner;

/**
 * Menüaktion „Ankündigung“: Dialog zeigen, die Ankündigung im Dokument speichern (LO-Main-Thread) und die Verteilung
 * an Webserver, PTM-Online und WhatsApp im Hintergrund anstoßen.
 */
public final class AnkuendigungAktion {

    static final String PROP_WHATSAPP_VERSAND = "Ankuendigung WhatsApp";

    private AnkuendigungAktion() {
    }

    /** Muss auf dem LO-Main-Thread laufen (modaler Dialog, Properties-Event). */
    public static void ausfuehren(WorkingSpreadsheet ws) throws com.sun.star.uno.Exception {
        DocumentPropertiesHelper docProps = new DocumentPropertiesHelper(ws);
        Optional<AnkuendigungDialog.Eingabe> eingabe = new AnkuendigungDialog(ws, Ankuendigung.lese(ws),
                waehlbareChats(), docProps.getStringProperty(WhatsAppPostRunner.PROP_LETZTER_WHATSAPP_CHAT, ""),
                docProps.getBooleanProperty(PROP_WHATSAPP_VERSAND, false)).zeige();
        if (eingabe.isEmpty()) {
            return;
        }
        String text = Ankuendigung.speichere(ws, eingabe.get().text());
        Optional<WhatsAppChatEintrag> chat = eingabe.get().whatsAppChat();
        if (!text.isEmpty()) {
            merkeWhatsAppWahl(docProps, chat);
        }
        TurnierSystem ts = docProps.getTurnierSystemAusDocument();
        new AnkuendigungVersandRunner(ws, ts, text, chat.orElse(null)).start();
    }

    /** Wie beim WhatsApp-Posten: Favoriten, sonst alle eingerichteten Chats. */
    static List<WhatsAppChatEintrag> waehlbareChats() {
        List<WhatsAppChatEintrag> alle = GlobalProperties.get().getWhatsAppChatEintraege();
        List<WhatsAppChatEintrag> favoriten = alle.stream().filter(WhatsAppChatEintrag::favorit).toList();
        return favoriten.isEmpty() ? alle : favoriten;
    }

    private static void merkeWhatsAppWahl(DocumentPropertiesHelper docProps, Optional<WhatsAppChatEintrag> chat) {
        docProps.setBooleanProperty(PROP_WHATSAPP_VERSAND, chat.isPresent());
        chat.ifPresent(eintrag -> docProps.setStringProperty(WhatsAppPostRunner.PROP_LETZTER_WHATSAPP_CHAT,
                eintrag.id()));
    }
}
