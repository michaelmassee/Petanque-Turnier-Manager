/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ki;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.BrowserOeffner;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;

public final class KiFehlerMeldenService {

    private static final String GITHUB_ISSUE_URL =
            "https://github.com/michaelmassee/Petanque-Turnier-Manager/issues/new";
    private static final int MAX_BODY_URL_ZEICHEN = 6_000;

    private KiFehlerMeldenService() {
    }

    public static void melden(WorkingSpreadsheet ws) {
        Thread worker = new Thread(() -> bereiteIssueVorUndPoste(ws), "PTM-KI-FehlerMelden");
        worker.start();
    }

    private static void bereiteIssueVorUndPoste(WorkingSpreadsheet ws) {
        try {
            Fehlerbericht bericht = FehlerberichtBuilder.erstellen(ws.getxContext());
            String url = issueUrl(bericht);
            BrowserOeffner.oeffne(url);
            LoMainThread.post(ws.getxContext(), () -> MessageBox.from(ws, MessageBoxTypeEnum.INFO_OK)
                    .caption(I18n.get("ki.fehler.melden.titel"))
                    .message(I18n.get("ki.fehler.melden.vorbereitet"))
                    .show());
        } catch (Exception e) {
            LoMainThread.post(ws.getxContext(), () -> MessageBox.from(ws, MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("ki.fehler.melden.titel"))
                    .message(I18n.get("ki.fehler.melden.fehler", e.getMessage()))
                    .show());
        }
    }

    static String issueUrl(Fehlerbericht bericht) {
        return GITHUB_ISSUE_URL
                + "?title=" + enc(bericht.titel())
                + "&body=" + enc(gekuerzterBody(bericht.body()));
    }

    private static String gekuerzterBody(String body) {
        if (body.length() <= MAX_BODY_URL_ZEICHEN) {
            return body;
        }
        return body.substring(0, MAX_BODY_URL_ZEICHEN)
                + "\n\n[Bericht wegen Browser-URL-Laenge gekuerzt. Das vollstaendige Logfile liegt lokal in PTM.]";
    }

    private static String enc(String wert) {
        return URLEncoder.encode(wert, StandardCharsets.UTF_8);
    }
}
