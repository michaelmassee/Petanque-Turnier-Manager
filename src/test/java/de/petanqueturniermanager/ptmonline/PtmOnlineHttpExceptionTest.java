/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PtmOnlineHttpExceptionTest {

    @Test
    void doppelterSpielerIstBereitsAngemeldet() {
        String antwort = "{\"error\":\"Dieser Spieler ist bereits angemeldet\","
                + "\"details\":{\"field\":\"firstName\",\"name\":\"Hans Müller\"}}";
        assertThat(new PtmOnlineHttpException(409, antwort).istBereitsAngemeldet()).isTrue();
    }

    @Test
    void bindungsKonfliktIstKeinBereitsAngemeldet() {
        String antwort = "{\"error\":\"Die Dokumentbindung wurde zwischenzeitlich geändert\","
                + "\"details\":{\"code\":\"binding_conflict\",\"bindingRevision\":3}}";
        assertThat(new PtmOnlineHttpException(409, antwort).istBereitsAngemeldet()).isFalse();
    }

    @Test
    void konfliktOhneDetailsIstKeinBereitsAngemeldet() {
        String antwort = "{\"error\":\"Die Meldeliste wird nach Turnierstart ausschließlich im Turnierdokument geführt.\"}";
        assertThat(new PtmOnlineHttpException(409, antwort).istBereitsAngemeldet()).isFalse();
    }

    @Test
    void anderesStatusCodeOderKeinJsonIstKeinBereitsAngemeldet() {
        assertThat(new PtmOnlineHttpException(400, "{\"error\":\"x\",\"details\":{\"field\":\"firstName\"}}")
                .istBereitsAngemeldet()).isFalse();
        assertThat(new PtmOnlineHttpException(409, "<html>Bad Gateway</html>").istBereitsAngemeldet()).isFalse();
    }

    @Test
    void meldungBleibtKompatibelZurBisherigenIoException() {
        assertThat(new PtmOnlineHttpException(401, "unauthorized").getMessage())
                .isEqualTo("PTM-Online API Fehler 401: unauthorized");
    }
}
