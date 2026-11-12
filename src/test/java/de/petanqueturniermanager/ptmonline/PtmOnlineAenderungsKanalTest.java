/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PtmOnlineAenderungsKanalTest {

    private static final String AENDERUNG_ONLINE = "{\"type\":\"changed\",\"source\":\"online\"}";
    private static final String ECHO_DOKUMENT = "{\"type\":\"changed\",\"source\":\"document\"}";

    private ScheduledExecutorService ausfuehrung;
    private final List<WebSocket.Listener> empfaenger = new ArrayList<>();
    private final List<CompletableFuture<WebSocket>> aufbauten = new ArrayList<>();
    private final AtomicInteger abrufe = new AtomicInteger();
    private PtmOnlineAenderungsKanal kanal;

    @BeforeEach
    void einrichten() {
        ausfuehrung = mock(ScheduledExecutorService.class);
        kanal = new PtmOnlineAenderungsKanal("t1", ausfuehrung, listener -> {
            empfaenger.add(listener);
            CompletableFuture<WebSocket> aufbau = new CompletableFuture<>();
            aufbauten.add(aufbau);
            return aufbau;
        }, abrufe::incrementAndGet);
    }

    @Test
    void wertetNurAenderungenAusPtmOnlineAus() {
        assertThat(PtmOnlineAenderungsKanal.istAenderungAusPtmOnline(AENDERUNG_ONLINE)).isTrue();
        assertThat(PtmOnlineAenderungsKanal.istAenderungAusPtmOnline("{\"type\":\"changed\"}")).as("ohne Quelle")
                .isTrue();
        assertThat(PtmOnlineAenderungsKanal.istAenderungAusPtmOnline(ECHO_DOKUMENT)).as("eigenes Echo").isFalse();
        assertThat(PtmOnlineAenderungsKanal.istAenderungAusPtmOnline("pong")).isFalse();
        assertThat(PtmOnlineAenderungsKanal.istAenderungAusPtmOnline("{\"type\":\"other\"}")).isFalse();
        assertThat(PtmOnlineAenderungsKanal.istAenderungAusPtmOnline("{kaputt")).isFalse();
    }

    @Test
    void stoesstNachDemVerbindenEinenAbrufAnUndHaeltDieVerbindungWach() {
        WebSocket socket = verbinde();

        assertThat(kanal.istVerbunden()).isTrue();
        assertThat(abrufe).hasValue(1);
        ArgumentCaptor<Runnable> ping = ArgumentCaptor.forClass(Runnable.class);
        verify(ausfuehrung).scheduleAtFixedRate(ping.capture(), eq(PtmOnlineAenderungsKanal.PING_ABSTAND_MS),
                eq(PtmOnlineAenderungsKanal.PING_ABSTAND_MS), eq(TimeUnit.MILLISECONDS));
        ping.getValue().run();
        verify(socket).sendText(PtmOnlineAenderungsKanal.PING, true);
    }

    @Test
    void ruftNurBeiAenderungenAusPtmOnlineAbAuchWennDieNachrichtGeteiltKommt() {
        WebSocket socket = verbinde();
        WebSocket.Listener listener = empfaenger.getFirst();

        listener.onText(socket, ECHO_DOKUMENT, true);
        listener.onText(socket, "pong", true);
        assertThat(abrufe).as("nur der Abruf nach dem Verbinden").hasValue(1);

        listener.onText(socket, AENDERUNG_ONLINE.substring(0, 10), false);
        listener.onText(socket, AENDERUNG_ONLINE.substring(10), true);
        assertThat(abrufe).hasValue(2);
    }

    @Test
    void verbindetNachEinemAbbruchMitWachsendemAbstandNeu() {
        WebSocket socket = verbinde();

        empfaenger.getFirst().onClose(socket, 1006, "");
        assertThat(kanal.istVerbunden()).isFalse();
        assertThat(abrufe).as("Abruf auch beim Verbindungsverlust").hasValue(2);
        geplanteWiederholung(PtmOnlineAenderungsKanal.ERSTE_WIEDERHOLUNG_MS).run();
        assertThat(aufbauten).hasSize(2);

        aufbauten.getLast().completeExceptionally(new IOException("offline"));
        geplanteWiederholung(2 * PtmOnlineAenderungsKanal.ERSTE_WIEDERHOLUNG_MS).run();
        assertThat(aufbauten).hasSize(3);

        aufbauten.getLast().complete(mock(WebSocket.class));
        assertThat(kanal.istVerbunden()).isTrue();
        assertThat(abrufe).hasValue(3);
    }

    @Test
    void gibtBeiAbgelehntemZugangAuf() {
        kanal.starten();
        aufbauten.getFirst().completeExceptionally(new CompletionException(handshakeMit(409)));

        assertThat(kanal.istAufgegeben()).isTrue();
        verify(ausfuehrung, never()).schedule(any(Runnable.class), anyLong(), any());
        assertThat(abrufe).hasValue(0);
    }

    @Test
    void versuchtEsBeiEinemAltenServerNurSeltenErneut() {
        kanal.starten();
        aufbauten.getFirst().completeExceptionally(handshakeMit(404));

        assertThat(kanal.istAufgegeben()).isFalse();
        geplanteWiederholung(PtmOnlineAenderungsKanal.ALTER_SERVER_WIEDERHOLUNG_MS);
    }

    @Test
    void bleibtBeiZuVielenVerbindungenBeimRegelmaessigenAbruf() {
        WebSocket socket = verbinde();

        empfaenger.getFirst().onClose(socket, PtmOnlineAenderungsKanal.ZU_VIELE_VERBINDUNGEN, "");

        assertThat(kanal.istAufgegeben()).isTrue();
        verify(ausfuehrung, never()).schedule(any(Runnable.class), anyLong(), any());
    }

    @Test
    void verbindetNachDemStoppenNichtMehrNeu() {
        WebSocket socket = verbinde();

        kanal.stoppen();
        empfaenger.getFirst().onClose(socket, 1000, "");
        empfaenger.getFirst().onText(socket, AENDERUNG_ONLINE, true);

        verify(socket).sendClose(WebSocket.NORMAL_CLOSURE, "");
        assertThat(kanal.istVerbunden()).isFalse();
        assertThat(abrufe).as("nur der Abruf nach dem Verbinden").hasValue(1);
        verify(ausfuehrung, never()).schedule(any(Runnable.class), anyLong(), any());
    }

    @Test
    void verwirftEineVerbindungDieErstNachDemStoppenZustandeKommt() {
        kanal.starten();
        kanal.stoppen();
        WebSocket spaet = mock(WebSocket.class);

        aufbauten.getFirst().complete(spaet);

        verify(spaet).abort();
        assertThat(kanal.istVerbunden()).isFalse();
        assertThat(abrufe).hasValue(0);
    }

    @Test
    void startetNurEinmal() {
        kanal.starten();
        kanal.starten();

        assertThat(aufbauten).hasSize(1);
    }

    private WebSocket verbinde() {
        WebSocket socket = mock(WebSocket.class);
        when(socket.sendText(anyString(), anyBoolean())).thenReturn(CompletableFuture.completedFuture(socket));
        when(socket.sendClose(anyInt(), anyString())).thenReturn(CompletableFuture.completedFuture(socket));
        kanal.starten();
        aufbauten.getLast().complete(socket);
        return socket;
    }

    private Runnable geplanteWiederholung(long abstandMs) {
        ArgumentCaptor<Runnable> wiederholung = ArgumentCaptor.forClass(Runnable.class);
        verify(ausfuehrung).schedule(wiederholung.capture(), eq(abstandMs), eq(TimeUnit.MILLISECONDS));
        return wiederholung.getValue();
    }

    @SuppressWarnings("unchecked")
    private static WebSocketHandshakeException handshakeMit(int status) {
        HttpResponse<Object> antwort = mock(HttpResponse.class);
        when(antwort.statusCode()).thenReturn(status);
        return new WebSocketHandshakeException(antwort);
    }
}
