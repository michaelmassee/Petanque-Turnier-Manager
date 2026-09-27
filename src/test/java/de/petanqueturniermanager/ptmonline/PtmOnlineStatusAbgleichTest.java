/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.google.gson.Gson;

import de.petanqueturniermanager.ptmonline.dto.NeueOnlineAnmeldung;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationResultDto;

class PtmOnlineStatusAbgleichTest {

    private static final NeueOnlineAnmeldung ANMELDUNG = new NeueOnlineAnmeldung("Paul", "Neu", null, null, null,
            null, null, null, null, true, true, List.of(), List.of());

    private final TournamentSyncClient client = mock(TournamentSyncClient.class);
    private final PtmOnlineRegistrationMapping mapping = mock(PtmOnlineRegistrationMapping.class);

    @BeforeEach
    void vorhandeneZuordnung() throws Exception {
        when(mapping.getOnlineIdsProUuid()).thenReturn(Map.of("u1", "r1"));
        when(mapping.getExecutionRevisionenProUuid()).thenReturn(Map.of("u1", 3));
        when(client.pushResults(anyString(), anyList())).thenAnswer(aufruf -> aufruf.<List<?>>getArgument(1).size());
    }

    @Test
    void legtNachmeldungAnUndPushtDanachAlleMitRevision() throws Exception {
        when(client.upsertRegistration(eq("t1"), eq("u2"), any())).thenReturn(registration("online-2"));
        PtmOnlineStatusAuftrag auftrag = new PtmOnlineStatusAuftrag("t1", false, List.of(
                eintrag("u1", OnlineTeilnahme.AKTIV, null), eintrag("u2", OnlineTeilnahme.AKTIV, anlage())));

        PtmOnlineStatusAbgleich.Ergebnis ergebnis = PtmOnlineStatusAbgleich.senden(auftrag, mapping, client);

        verify(client, never()).start(anyString());
        assertThat(ergebnis.neueZuordnungen()).singleElement()
                .satisfies(zuordnung -> assertThat(zuordnung.onlineId()).isEqualTo("online-2"));
        assertThat(gepusht()).extracting(RegistrationResultDto::id, RegistrationResultDto::expectedExecutionRevision)
                .containsExactly(tuple("r1", 3), tuple("online-2", 1));
        assertThat(ergebnis.revisionen()).containsEntry("u1", 4).containsEntry("u2", 2);
    }

    @Test
    void ersteRundeStartetDasTurnier() throws Exception {
        PtmOnlineStatusAbgleich.senden(new PtmOnlineStatusAuftrag("t1", true,
                List.of(eintrag("u1", OnlineTeilnahme.AKTIV, null))), mapping, client);

        verify(client).start("t1");
    }

    @Test
    void bereitsZugeordneteMeldungWirdNichtErneutAngelegt() throws Exception {
        PtmOnlineStatusAbgleich.senden(new PtmOnlineStatusAuftrag("t1", false,
                List.of(eintrag("u1", OnlineTeilnahme.AKTIV, anlage()))), mapping, client);

        verify(client, never()).upsertRegistration(anyString(), anyString(), any());
    }

    @Test
    void alsBereitsAngemeldetAbgelehnteMeldungWirdGemeldet() throws Exception {
        when(client.upsertRegistration(eq("t1"), eq("u2"), any())).thenThrow(new PtmOnlineHttpException(409,
                "{\"error\":\"bereits angemeldet\",\"details\":{\"field\":\"lastName\"}}"));

        PtmOnlineStatusAbgleich.Ergebnis ergebnis = PtmOnlineStatusAbgleich.senden(new PtmOnlineStatusAuftrag("t1",
                false, List.of(eintrag("u2", OnlineTeilnahme.AKTIV, anlage()))), mapping, client);

        assertThat(ergebnis.abgelehnt()).containsExactly("Paul Neu");
        assertThat(ergebnis.neueZuordnungen()).isEmpty();
    }

    @Test
    void unvollstaendigerPushLaesstRevisionenUnveraendert() throws Exception {
        when(client.pushResults(anyString(), anyList())).thenReturn(0);

        PtmOnlineStatusAbgleich.Ergebnis ergebnis = PtmOnlineStatusAbgleich.senden(new PtmOnlineStatusAuftrag("t1",
                false, List.of(eintrag("u1", OnlineTeilnahme.AUSGESETZT, null))), mapping, client);

        assertThat(ergebnis.revisionen()).isEmpty();
    }

    @Test
    void neuererAuftragBehaeltDenTurnierstartDesAelteren() {
        PtmOnlineStatusAuftrag aelter = new PtmOnlineStatusAuftrag("t1", true, List.of());
        PtmOnlineStatusAuftrag neuer = new PtmOnlineStatusAuftrag("t1", false,
                List.of(eintrag("u1", OnlineTeilnahme.AKTIV, null)));

        PtmOnlineStatusAuftrag zusammen = neuer.ersetzt(aelter);

        assertThat(zusammen.turnierStarten()).isTrue();
        assertThat(zusammen.eintraege()).isEqualTo(neuer.eintraege());
        assertThat(neuer.ersetzt(null)).isSameAs(neuer);
    }

    @SuppressWarnings("unchecked")
    private List<RegistrationResultDto> gepusht() throws Exception {
        ArgumentCaptor<List<RegistrationResultDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(client).pushResults(eq("t1"), captor.capture());
        return captor.getValue();
    }

    private static PtmOnlineStatusAuftrag.Eintrag eintrag(String uuid, OnlineTeilnahme teilnahme,
            PtmOnlineStatusAuftrag.NeueAnlage anlage) {
        return new PtmOnlineStatusAuftrag.Eintrag(uuid, teilnahme, null, anlage);
    }

    private static PtmOnlineStatusAuftrag.NeueAnlage anlage() {
        return new PtmOnlineStatusAuftrag.NeueAnlage(ANMELDUNG, "Paul Neu", "1");
    }

    private static RegistrationDto registration(String id) {
        return new Gson().fromJson("{\"id\":\"" + id + "\",\"status\":\"confirmed\"}", RegistrationDto.class);
    }
}
