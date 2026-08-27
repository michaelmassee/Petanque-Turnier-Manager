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

import de.petanqueturniermanager.ptmonline.dto.CreateTournamentDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationResultDto;

public class TournamentSyncClientTest {

	@SuppressWarnings("unchecked")
	private HttpResponse<String> mockResponse(int statusCode, String body) {
		HttpResponse<String> response = mock(HttpResponse.class);
		when(response.statusCode()).thenReturn(statusCode);
		when(response.body()).thenReturn(body);
		return response;
	}

	@Test
	public void createTournamentSendetPostUndLiefertId() throws Exception {
		HttpClient httpClient = mock(HttpClient.class);
		HttpResponse<String> response = mockResponse(201, "{\"tournament\":{\"id\":\"tid-1\"}}");
		when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(response);

		TournamentSyncClient client = new TournamentSyncClient(httpClient, "https://ptm-online.example.com", "ptm_secret");
		String id = client.createTournament(
				new CreateTournamentDto("Test-Turnier", "2026-09-01", null, "Testplatz", null, "schweizer", "doublette", "registration", "public"));

		assertThat(id).isEqualTo("tid-1");

		ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
		verify(httpClient).send(captor.capture(), any());
		HttpRequest sent = captor.getValue();
		assertThat(sent.uri().toString()).isEqualTo("https://ptm-online.example.com/api/tournaments");
		assertThat(sent.headers().firstValue("Authorization")).contains("Bearer ptm_secret");
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
				List.of(new RegistrationResultDto("r1", "confirmed", 1), new RegistrationResultDto("r2", "confirmed", 2)));

		assertThat(updated).isEqualTo(2);

		ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
		verify(httpClient).send(captor.capture(), any());
		assertThat(captor.getValue().method()).isEqualTo("POST");
	}

	@Test
	public void wirftIOExceptionBeiNichtFreigeschaltetemSchluessel() throws Exception {
		HttpClient httpClient = mock(HttpClient.class);
		HttpResponse<String> response = mockResponse(401, "{\"error\":\"Invalid or inactive API key\"}");
		when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(response);

		TournamentSyncClient client = new TournamentSyncClient(httpClient, "https://ptm-online.example.com", "ptm_invalid");

		assertThatThrownBy(() -> client.fetchRegistrations("t1", null)).isInstanceOf(IOException.class).hasMessageContaining("401");
	}
}
