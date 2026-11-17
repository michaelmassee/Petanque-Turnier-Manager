/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ankuendigung;

import java.util.List;
import java.util.Optional;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;

import de.petanqueturniermanager.comp.GlobalProperties.WhatsAppChatEintrag;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;

/**
 * Eingabe der Ankündigung: mehrzeiliger Text (höchstens {@value Ankuendigung#MAX_LAENGE} Zeichen), optional der
 * Versand per WhatsApp an einen der eingerichteten Chats. „Senden“ übernimmt den Text, „Löschen“ entfernt die
 * Ankündigung überall, „Abbrechen“ lässt alles unverändert.
 */
final class AnkuendigungDialog extends AbstractUnoDialog {

    /** Was im Dialog gewählt wurde; ein leerer Text bedeutet Löschen. */
    record Eingabe(String text, Optional<WhatsAppChatEintrag> whatsAppChat) {
    }

    private static final int BREITE = 260;
    private static final int RAND = 6;
    private static final int INNEN_BREITE = BREITE - 2 * RAND;
    private static final int TEXT_Y = 28;
    private static final int TEXT_HOEHE = 70;
    private static final int WHATSAPP_Y = TEXT_Y + TEXT_HOEHE + 8;
    private static final int ZEILEN_HOEHE = 14;
    private static final int BUTTON_BREITE = 60;
    private static final short CHECKBOX_AN = 1;

    private final WorkingSpreadsheet ws;
    private final String vorbelegung;
    private final List<WhatsAppChatEintrag> chats;
    private final String vorgewaehlterChatId;
    private final boolean whatsAppVorbelegt;

    private XPropertySet textModell;
    private XPropertySet whatsAppModell;
    private XPropertySet chatModell;
    private Optional<Eingabe> eingabe = Optional.empty();

    /**
     * @param chats               auswählbare WhatsApp-Chats; leer blendet die WhatsApp-Auswahl aus
     * @param vorgewaehlterChatId zuletzt im Dokument gewählter Chat (interne ID des Eintrags)
     */
    AnkuendigungDialog(WorkingSpreadsheet ws, String vorbelegung, List<WhatsAppChatEintrag> chats,
            String vorgewaehlterChatId, boolean whatsAppVorbelegt) {
        super(ws.getxContext());
        this.ws = ws;
        this.vorbelegung = vorbelegung;
        this.chats = List.copyOf(chats);
        this.vorgewaehlterChatId = vorgewaehlterChatId;
        this.whatsAppVorbelegt = whatsAppVorbelegt;
    }

    /** Zeigt den Dialog modal; leer bei Abbrechen. */
    Optional<Eingabe> zeige() throws com.sun.star.uno.Exception {
        erstelleUndAusfuehren();
        return eingabe;
    }

    @Override
    protected XWindowPeer holeParentPeer() {
        return ws.getContainerWindowPeer();
    }

    @Override
    protected String getTitel() {
        return I18n.get("ankuendigung.titel");
    }

    @Override
    protected int getBreite() {
        return BREITE;
    }

    @Override
    protected int getHoehe() {
        return buttonY() + ZEILEN_HOEHE + RAND;
    }

    private int buttonY() {
        return chats.isEmpty() ? WHATSAPP_Y : WHATSAPP_Y + ZEILEN_HOEHE + 8;
    }

    @Override
    protected void erstelleFelder(XMultiComponentFactory mcf, XMultiServiceFactory xMSF, XNameContainer cont,
            XToolkit xToolkit, XWindowPeer peer, XPropertySet dlgProps, XDialog xDialog)
            throws com.sun.star.uno.Exception {
        label(xMSF, cont, "lblHinweis", I18n.get("ankuendigung.dialog.hinweis", Ankuendigung.MAX_LAENGE),
                new Rahmen(RAND, RAND, INNEN_BREITE, 20), true);
        textModell = textfeld(xMSF, cont);
        if (!chats.isEmpty()) {
            whatsAppModell = checkbox(xMSF, cont);
            chatModell = chatAuswahl(xMSF, cont);
        }
        int y = buttonY();
        int abbrechenX = BREITE - RAND - BUTTON_BREITE;
        button(xMSF, cont, "btnSenden", I18n.get("ankuendigung.dialog.senden"),
                new Rahmen(abbrechenX - 2 * (BUTTON_BREITE + RAND), y, BUTTON_BREITE, ZEILEN_HOEHE),
                (short) PushButtonType.OK_value);
        button(xMSF, cont, "btnLoeschen", I18n.get("ankuendigung.dialog.loeschen"),
                new Rahmen(abbrechenX - BUTTON_BREITE - RAND, y, BUTTON_BREITE, ZEILEN_HOEHE),
                (short) PushButtonType.STANDARD_value);
        button(xMSF, cont, "btnAbbrechen", I18n.get("ankuendigung.dialog.abbrechen"),
                new Rahmen(abbrechenX, y, BUTTON_BREITE, ZEILEN_HOEHE), (short) PushButtonType.CANCEL_value);
        registriereLoeschen(Lo.qi(XControlContainer.class, xDialog), xDialog);
    }

    private XPropertySet textfeld(XMultiServiceFactory xMSF, XNameContainer cont) throws com.sun.star.uno.Exception {
        XPropertySet props = modell(xMSF, "com.sun.star.awt.UnoControlEditModel",
                new Rahmen(RAND, TEXT_Y, INNEN_BREITE, TEXT_HOEHE));
        props.setPropertyValue("MultiLine", Boolean.TRUE);
        props.setPropertyValue("VScroll", Boolean.TRUE);
        props.setPropertyValue("MaxTextLen", (short) Ankuendigung.MAX_LAENGE);
        props.setPropertyValue("Text", vorbelegung);
        cont.insertByName("txtAnkuendigung", props);
        return props;
    }

    private XPropertySet checkbox(XMultiServiceFactory xMSF, XNameContainer cont) throws com.sun.star.uno.Exception {
        XPropertySet props = modell(xMSF, "com.sun.star.awt.UnoControlCheckBoxModel",
                new Rahmen(RAND, WHATSAPP_Y + 2, 110, ZEILEN_HOEHE - 2));
        props.setPropertyValue("Label", I18n.get("ankuendigung.dialog.whatsapp"));
        props.setPropertyValue("State", whatsAppVorbelegt ? CHECKBOX_AN : (short) 0);
        cont.insertByName("chkWhatsApp", props);
        return props;
    }

    private XPropertySet chatAuswahl(XMultiServiceFactory xMSF, XNameContainer cont)
            throws com.sun.star.uno.Exception {
        XPropertySet props = modell(xMSF, "com.sun.star.awt.UnoControlListBoxModel",
                new Rahmen(RAND + 114, WHATSAPP_Y, INNEN_BREITE - 114, ZEILEN_HOEHE));
        props.setPropertyValue("Dropdown", Boolean.TRUE);
        props.setPropertyValue("StringItemList",
                chats.stream().map(WhatsAppChatEintrag::anzeigeName).toArray(String[]::new));
        props.setPropertyValue("SelectedItems", new short[] { (short) vorgewaehlterIndex() });
        cont.insertByName("lstWhatsAppChat", props);
        return props;
    }

    private int vorgewaehlterIndex() {
        for (int i = 0; i < chats.size(); i++) {
            if (chats.get(i).id().equals(vorgewaehlterChatId)) {
                return i;
            }
        }
        return 0;
    }

    private static XPropertySet modell(XMultiServiceFactory xMSF, String service, Rahmen rahmen)
            throws com.sun.star.uno.Exception {
        XPropertySet props = Lo.qi(XPropertySet.class, xMSF.createInstance(service));
        props.setPropertyValue("PositionX", rahmen.x());
        props.setPropertyValue("PositionY", rahmen.y());
        props.setPropertyValue("Width", rahmen.breite());
        props.setPropertyValue("Height", rahmen.hoehe());
        return props;
    }

    /** „Löschen“ schließt den Dialog mit leerem Text, ohne WhatsApp-Nachricht. */
    private void registriereLoeschen(XControlContainer xcc, XDialog xDialog) {
        XButton loeschen = Lo.qi(XButton.class, xcc.getControl("btnLoeschen"));
        if (loeschen == null) {
            return;
        }
        loeschen.addActionListener(new XActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                eingabe = Optional.of(new Eingabe("", Optional.empty()));
                xDialog.endExecute();
            }

            @Override
            public void disposing(EventObject event) {
                // Kein Aufräumen nötig.
            }
        });
    }

    @Override
    protected void beiOkGeklickt() throws com.sun.star.uno.Exception {
        String text = Ankuendigung.bereinige((String) textModell.getPropertyValue("Text"));
        eingabe = Optional.of(new Eingabe(text, gewaehlterChat()));
    }

    private Optional<WhatsAppChatEintrag> gewaehlterChat() throws com.sun.star.uno.Exception {
        if (whatsAppModell == null || ((Short) whatsAppModell.getPropertyValue("State")) != CHECKBOX_AN) {
            return Optional.empty();
        }
        short[] auswahl = (short[]) chatModell.getPropertyValue("SelectedItems");
        int index = auswahl.length == 0 ? 0 : auswahl[0];
        return index < chats.size() ? Optional.of(chats.get(index)) : Optional.empty();
    }
}
