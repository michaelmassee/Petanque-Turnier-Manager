package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.google.gson.Gson;

import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.ptmonline.dto.NeueOnlineAnmeldung;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationResultDto;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;

public class TournamentSyncClientTest {

	@SuppressWarnings("unchecked")
	private HttpResponse<String> mockResponse(int statusCode, String body) {
		HttpResponse<String> response = mock(HttpResponse.class);
		when(response.statusCode()).thenReturn(statusCode);
		when(response.body()).thenReturn(body);
		return response;
	}

	@Test
	public void listTournamentsParstListeAusDerAntwort() throws Exception {
		HttpClient httpClient = mock(HttpClient.class);
		String body = "{\"tournaments\":[{\"id\":\"t1\",\"name\":\"Herbstturnier\",\"type\":\"schweizer\",\"registrationType\":\"forme\",\"status\":\"registration\",\"documentManaged\":false}]}";
		HttpResponse<String> response = mockResponse(200, body);
		when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(response);

		TournamentSyncClient client = new TournamentSyncClient(httpClient, "https://ptm-online.example.com", "ptm_secret");
		List<OnlineTournamentDto> turniere = client.listTournaments();

		assertThat(turniere).hasSize(1);
		assertThat(turniere.get(0).id).isEqualTo("t1");
		assertThat(turniere.get(0).type).isEqualTo("schweizer");

		ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
		verify(httpClient).send(captor.capture(), any());
		assertThat(captor.getValue().uri().toString()).isEqualTo("https://ptm-online.example.com/api/sync/tournaments");
		assertThat(captor.getValue().headers().firstValue("Authorization")).contains("Bearer ptm_secret");
	}

	@Test
	public void connectBindetDokumentUndLease() throws Exception {
		HttpClient httpClient = mock(HttpClient.class);
		HttpResponse<String> response = mockResponse(200,
				"{\"ok\":true,\"syncDocumentId\":\"e9e9caec-e0b1-4fe0-8fee-a229279b9f73\",\"bindingRevision\":1}");
		when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(response);

		TournamentSyncClient client = new TournamentSyncClient(httpClient, "https://ptm-online.example.com", "ptm_secret");
		SyncBindingDto binding = client.connect("t1", "e9e9caec-e0b1-4fe0-8fee-a229279b9f73", "01234567890123456789012345678901");

		assertThat(binding.bindingRevision()).isEqualTo(1);
		ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
		verify(httpClient).send(captor.capture(), any());
		assertThat(captor.getValue().bodyPublisher()).isPresent();
	}

	@Test
	public void fetchRegistrationsParstListeAusDerAntwort() throws Exception {
		HttpClient httpClient = mock(HttpClient.class);
		String body = "{\"registrations\":[{\"id\":\"r1\",\"tournamentId\":\"t1\",\"firstName\":\"Max\",\"lastName\":\"Muster\",\"status\":\"confirmed\"}],\"cursor\":\"2026-01-01T00:00:00.000Z\"}";
		HttpResponse<String> response = mockResponse(200, body);
		when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(response);

		TournamentSyncClient client = new TournamentSyncClient(httpClient, "https://ptm-online.example.com", "ptm_secret");
		List<RegistrationDto> registrations = client.fetchRegistrations("t1", null);

		assertThat(registrations).hasSize(1);
		assertThat(registrations.get(0).firstName()).isEqualTo("Max");
		assertThat(registrations.get(0).status()).isEqualTo("confirmed");
	}

	@Test
	public void pushResultsSendetRegistrationsArrayUndLiefertUpdatedCount() throws Exception {
		HttpClient httpClient = mock(HttpClient.class);
		HttpResponse<String> response = mockResponse(200, "{\"updatedCount\":2}");
		when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(response);

		TournamentSyncClient client = new TournamentSyncClient(httpClient, "https://ptm-online.example.com", "ptm_secret");
		int updated = client.pushResults("t1",
				List.of(new RegistrationResultDto("r1", null, 1, OnlineTeilnahme.AKTIV.apiWert(), null),
						new RegistrationResultDto("r2", null, 2, OnlineTeilnahme.AUSGESETZT.apiWert(), null)));

		assertThat(updated).isEqualTo(2);

		ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
		verify(httpClient).send(captor.capture(), any());
		assertThat(captor.getValue().method()).isEqualTo("POST");
	}

	@Test
	public void ergebnisPayloadMeldetTeilnahmeUndKeinenAnmeldestatus() {
		String json = new Gson().toJson(new RegistrationResultDto("r1", null, 3, OnlineTeilnahme.INAKTIV.apiWert(), 2));

		assertThat(json).contains("\"participation\":\"inactive\"").doesNotContain("\"active\"")
				.doesNotContain("\"status\"");
	}

	@Test
	public void direkteAnmeldungSendetLeereTeilnehmerantwortenAlsArray() {
		NeueOnlineAnmeldung anmeldung = new NeueOnlineAnmeldung("Max", "Muster", null, null,
				null, null, null, null, null, true, true, List.of(), List.of());

		assertThat(new Gson().toJson(anmeldung)).contains("\"registrationAnswers\":[]");
	}

	@Test
	public void wirftIOExceptionBeiNichtFreigeschaltetemSchluessel() throws Exception {
		HttpClient httpClient = mock(HttpClient.class);
		HttpResponse<String> response = mockResponse(401, "{\"error\":\"Invalid or inactive API key\"}");
		when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(response);

		TournamentSyncClient client = new TournamentSyncClient(httpClient, "https://ptm-online.example.com", "ptm_invalid");

		assertThatThrownBy(() -> client.fetchRegistrations("t1", null)).isInstanceOf(IOException.class).hasMessageContaining("401");
	}

	@Test
	public void upsertMitDoppeltemSpielerLiefertBereitsAngemeldet() throws Exception {
		HttpClient httpClient = mock(HttpClient.class);
		HttpResponse<String> response = mockResponse(409,
				"{\"error\":\"Dieser Spieler ist bereits angemeldet\",\"details\":{\"field\":\"firstName\",\"name\":\"Hans Müller\"}}");
		when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(response);

		TournamentSyncClient client = new TournamentSyncClient(httpClient, "https://ptm-online.example.com", "ptm_secret");
		NeueOnlineAnmeldung anmeldung = new NeueOnlineAnmeldung("Hans", "Müller", null, null, null, null, null, null,
				null, true, true, List.of(), List.of());

		assertThatThrownBy(() -> client.upsertRegistration("t1", "e9e9caec-e0b1-4fe0-8fee-a229279b9f73", anmeldung))
				.isInstanceOfSatisfying(PtmOnlineHttpException.class,
						e -> assertThat(e.istBereitsAngemeldet()).isTrue());
	}
}
