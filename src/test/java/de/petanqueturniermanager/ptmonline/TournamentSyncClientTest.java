package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Flow;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.auftrag.SyncAuftrag;
import de.petanqueturniermanager.ptmonline.auftrag.versand.SyncAntwort;
import de.petanqueturniermanager.ptmonline.dto.AnmeldungsAbruf;
import de.petanqueturniermanager.ptmonline.dto.KonfliktListeDto;
import de.petanqueturniermanager.ptmonline.dto.LiveMatchDto;
import de.petanqueturniermanager.ptmonline.dto.LiveRankingEntryDto;
import de.petanqueturniermanager.ptmonline.dto.NeueOnlineAnmeldung;
import de.petanqueturniermanager.ptmonline.dto.PersonDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationResultDto;
import de.petanqueturniermanager.ptmonline.dto.ServerZuordnungDto;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.ptmonline.dto.SyncStandDto;

public class TournamentSyncClientTest {

	/** Fehlermeldungen nennen das fehlende Feld über I18n-Platzhalter – unabhängig von der Testreihenfolge laden. */
	@BeforeAll
	static void initI18n() {
		I18n.initFuerTest(Locale.GERMAN);
	}

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
	public void connectSendetProtokollversionUndVerbindungsAuftragsId() throws Exception {
		HttpClient httpClient = httpClientMitAntwort(
				"{\"ok\":true,\"syncDocumentId\":\"e9e9caec-e0b1-4fe0-8fee-a229279b9f73\",\"bindingRevision\":1,\"writeCounter\":0}");
		TournamentSyncClient client = new TournamentSyncClient(httpClient, "https://ptm-online.example.com", "ptm_secret");

		SyncBindingDto binding = client.connect("t1", "e9e9caec-e0b1-4fe0-8fee-a229279b9f73",
				"01234567890123456789012345678901", "connect-1", false);

		assertThat(binding.bindingRevision()).isEqualTo(1);
		assertThat(binding.writeCounter()).isZero();
		assertThat(bodyAlsText(gesendeteAnfrage(httpClient))).contains("\"protocolVersion\":2")
				.contains("\"connectRequestId\":\"connect-1\"").doesNotContain("recovery");
	}

	@Test
	public void wiederherstellungSendetDieAusdrueklicheBestaetigung() throws Exception {
		HttpClient httpClient = httpClientMitAntwort(
				"{\"ok\":true,\"syncDocumentId\":\"e9e9caec-e0b1-4fe0-8fee-a229279b9f73\",\"bindingRevision\":2}");
		TournamentSyncClient client = new TournamentSyncClient(httpClient, "https://ptm-online.example.com", "ptm_secret");

		SyncBindingDto binding = client.takeover("t1", "e9e9caec-e0b1-4fe0-8fee-a229279b9f73",
				"01234567890123456789012345678901", 1, "takeover-1", true);

		assertThat(binding.bindingRevision()).isEqualTo(2);
		HttpRequest request = gesendeteAnfrage(httpClient);
		assertThat(request.uri().toString()).isEqualTo("https://ptm-online.example.com/api/sync/tournaments/t1/takeover");
		assertThat(bodyAlsText(request)).contains("\"recovery\":true").contains("\"takeoverRequestId\":\"takeover-1\"")
				.contains("\"expectedBindingRevision\":1");
	}

	@Test
	public void wiederherstellungNoetigNenntDieOnlineRunden() {
		PtmOnlineHttpException e = new PtmOnlineHttpException(409,
				"{\"error\":\"x\",\"details\":{\"code\":\"recovery_required\",\"roundsOnline\":3}}");

		assertThat(e.istWiederherstellungNoetig()).isTrue();
		assertThat(e.rundenOnline()).isEqualTo(3);
		assertThat(new PtmOnlineHttpException(409, "{\"details\":{\"code\":\"document_forked\"}}").istDokumentGeforkt())
				.isTrue();
	}

	@Test
	public void fetchRegistrationsParstListeAusDerAntwort() throws Exception {
		String body = "{\"registrations\":[{\"id\":\"r1\",\"tournamentId\":\"t1\",\"firstName\":\"Max\",\"lastName\":\"Muster\",\"status\":\"confirmed\",\"receivedAfterStart\":true}],\"cursor\":\"2026-01-01T00:00:00.000Z\"}";
		TournamentSyncClient client = clientMitAntwort(body);

		List<RegistrationDto> registrations = client.fetchRegistrations("t1", null);

		assertThat(registrations).hasSize(1);
		assertThat(registrations.get(0).firstName()).isEqualTo("Max");
		assertThat(registrations.get(0).istNachTurnierstartEingegangen()).isTrue();
		assertThat(registrations.get(0).istUeberKapazitaet()).isFalse();
	}

	@Test
	public void fetchSyncStandLiestNurDenTurnierzustand() throws Exception {
		HttpClient httpClient = httpClientMitAntwort("{\"registrations\":[],\"tournament\":{\"status\":\"running\","
				+ "\"registrationClosed\":true,\"roundsOnline\":4,\"writeCounter\":7}}");
		TournamentSyncClient client = new TournamentSyncClient(httpClient, "https://ptm-online.example.com", "ptm_secret");

		SyncStandDto stand = client.fetchSyncStand("t1");

		assertThat(stand.istRunning()).isTrue();
		assertThat(stand.roundsOnline()).isEqualTo(4);
		assertThat(stand.writeCounter()).isEqualTo(7);
		assertThat(gesendeteAnfrage(httpClient).uri().toString()).contains("/api/sync/tournaments/t1/registrations?since=");
	}

	@Test
	public void fetchMappingLiefertDieServerZuordnungen() throws Exception {
		TournamentSyncClient client = clientMitAntwort("{\"mappings\":[{\"onlineRegistrationId\":\"r1\","
				+ "\"localRegistrationUuid\":\"u1\",\"status\":\"confirmed\",\"executionRevision\":3}]}");

		List<ServerZuordnungDto> zuordnungen = client.fetchMapping("t1");

		assertThat(zuordnungen).singleElement().satisfies(zuordnung -> {
			assertThat(zuordnung.localRegistrationUuid()).isEqualTo("u1");
			assertThat(zuordnung.revision()).isEqualTo(3);
		});
	}

	@Test
	public void sendeUebertraegtAuftragsIdUndSchreibzaehlerUndLiefertAuchAblehnungen() throws Exception {
		HttpClient httpClient = mock(HttpClient.class);
		HttpResponse<String> response = mockResponse(409, "{\"details\":{\"code\":\"tournament_running\"}}");
		when(response.headers()).thenReturn(HttpHeaders.of(Map.of("X-PTM-Replayed", List.of("1")), (a, b) -> true));
		when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(response);
		TournamentSyncClient client = new TournamentSyncClient(httpClient, "https://ptm-online.example.com", "ptm_secret");
		SyncAuftrag auftrag = new SyncAuftrag("auftrag-1", 5, AuftragsArt.TEILNAHME, "POST",
				TournamentSyncClient.ergebnissePfad("t1"), "{\"registrations\":[]}", "{}");

		SyncAntwort antwort = client.sende(auftrag);

		assertThat(antwort.status()).isEqualTo(409);
		assertThat(antwort.wiederholt()).isTrue();
		assertThat(antwort.code()).contains("tournament_running");
		HttpRequest request = gesendeteAnfrage(httpClient);
		assertThat(request.headers().firstValue("X-PTM-Request-Id")).contains("auftrag-1");
		assertThat(request.headers().firstValue("X-PTM-Sync-Counter")).contains("5");
		assertThat(bodyAlsText(request)).isEqualTo("{\"registrations\":[]}");
	}

	@Test
	public void ergebnisPayloadMeldetTeilnahmeUndKeinenAnmeldestatus() {
		String json = TournamentSyncClient.ergebnisseBody(
				List.of(new RegistrationResultDto("r1", null, 3, OnlineTeilnahme.INAKTIV.apiWert(), 2)));

		assertThat(json).contains("\"participation\":\"inactive\"").doesNotContain("\"active\"")
				.doesNotContain("\"status\"").contains("\"expectedExecutionRevision\":2");
	}

	@Test
	public void anlageSendetLeereTeilnehmerantwortenUndDieLokaleTeilnahme() {
		NeueOnlineAnmeldung anmeldung = new NeueOnlineAnmeldung("Max", "Muster", null, null,
				null, null, null, null, null, true, true, List.of(), List.of());

		assertThat(TournamentSyncClient.anlageBody(anmeldung, OnlineTeilnahme.AKTIV.apiWert(), 4))
				.contains("\"registrationAnswers\":[]").contains("\"participation\":\"active\"")
				.contains("\"seedingPosition\":4");
	}

	@Test
	public void dokumentMasterUpdateSendetLokaleNamenUndErwarteteRevision() {
		NeueOnlineAnmeldung anmeldung = new NeueOnlineAnmeldung("Anna", "Schmidt", "Verein", null,
				null, null, null, null, null, true, true, List.of(), List.of());

		List<PersonDto> personen = List.of(new PersonDto(1, "Anna", "Schmidt", null, "konto-anna"));

		assertThat(TournamentSyncClient.aenderungBody(anmeldung, personen, "d8d8caec-e0b1-4fe0-8fee-a229279b9f73", 4))
				.contains("\"documentMaster\":true")
				.contains("\"onlineRegistrationId\":\"d8d8caec-e0b1-4fe0-8fee-a229279b9f73\"")
				.contains("\"expectedExecutionRevision\":4").contains("\"firstName\":\"Anna\"")
				.contains("\"persons\":[{\"slot\":1,\"firstName\":\"Anna\",\"lastName\":\"Schmidt\",\"userId\":\"konto-anna\"}]");
	}

	@Test
	public void abgleichLiefertPersonenMitBenutzerIdUndDieKonfliktliste() throws Exception {
		HttpClient httpClient = httpClientMitAntwort("""
				{"registrations":[{"id":"r1","firstName":"Anna","lastName":"Adler","status":"confirmed",
				  "persons":[{"slot":1,"firstName":"Anna","lastName":"Adler","userId":"u1"},
				             {"slot":2,"firstName":"Ben","lastName":"Berg","userId":null}],
				  "accountConflict":true,"incomplete":true,"meleeTeamUuid":"team-1"}],
				 "cursor":"x","conflicts":{"accountConflicts":[{"userId":"u1","registrationIds":["r1","r2"]}],
				  "possibleDuplicates":[{"kind":"name","registrationIds":["r3","r4"]}]}}""");
		TournamentSyncClient client = new TournamentSyncClient(httpClient, "https://ptm-online.example.com", "ptm_secret");

		AnmeldungsAbruf abruf = client.fetchAbgleich("t1", null);

		RegistrationDto anmeldung = abruf.registrations().getFirst();
		assertThat(anmeldung.personen()).extracting(PersonDto::userId).containsExactly("u1", null);
		assertThat(anmeldung.istKontoKonflikt()).isTrue();
		assertThat(anmeldung.istUnvollstaendig()).isTrue();
		assertThat(anmeldung.meleeTeamUuid()).isEqualTo("team-1");
		assertThat(abruf.konflikte().accountConflicts().getFirst().registrationIds()).containsExactly("r1", "r2");
		assertThat(abruf.konflikte().possibleDuplicates().getFirst().kind()).isEqualTo("name");
	}

	@Test
	public void abgleichOhneKonfliktlisteUndOhnePersonenNutztDieNamensfelder() throws Exception {
		HttpClient httpClient = httpClientMitAntwort("""
				{"registrations":[{"id":"r1","firstName":"Anna","lastName":"Adler","partnerFirstName":"Ben",
				  "partnerLastName":"Berg","status":"confirmed"}],"cursor":"x"}""");
		TournamentSyncClient client = new TournamentSyncClient(httpClient, "https://ptm-online.example.com", "ptm_secret");

		AnmeldungsAbruf abruf = client.fetchAbgleich("t1", null);

		assertThat(abruf.konflikte()).isEqualTo(KonfliktListeDto.leer());
		assertThat(abruf.registrations().getFirst().personen())
				.containsExactly(new PersonDto(1, "Anna", "Adler", null, null), new PersonDto(2, "Ben", "Berg", null, null));
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
	public void antwortOhneUpdatedCountWirftIOException() {
		assertThatThrownBy(() -> TournamentSyncClient.zahlAus(new SyncAntwort(200, "{}", false), "updatedCount"))
				.isExactlyInstanceOf(IOException.class).hasMessageContaining("updatedCount");
	}

	@Test
	public void fetchRegistrationsOhneArrayWirftIOException() throws Exception {
		TournamentSyncClient client = clientMitAntwort("{\"registrations\":null}");

		assertThatThrownBy(() -> client.fetchRegistrations("t1", null)).isExactlyInstanceOf(IOException.class)
				.hasMessageContaining("registrations");
	}

	@Test
	public void antwortOhneRegistrationWirftIOException() {
		assertThatThrownBy(() -> TournamentSyncClient.registrationAus(new SyncAntwort(200, "{\"ok\":true}", false)))
				.isExactlyInstanceOf(IOException.class).hasMessageContaining("registration");
	}

	@Test
	public void listTournamentsMitKaputterAntwortWirftIOException() throws Exception {
		TournamentSyncClient client = clientMitAntwort("<html>Bad Gateway</html>");

		assertThatThrownBy(client::listTournaments).isExactlyInstanceOf(IOException.class);
	}

	@Test
	public void listTournamentsMitLeererAntwortWirftIOException() throws Exception {
		TournamentSyncClient client = clientMitAntwort("");

		assertThatThrownBy(client::listTournaments).isExactlyInstanceOf(IOException.class);
	}

	@Test
	public void rundeSendetPartienOhneNullFelder() {
		assertThat(TournamentSyncClient.rundePfad("t 1", 2)).isEqualTo("/api/sync/tournaments/t+1/rounds/2");
		assertThat(TournamentSyncClient.rundeBody(List.of(new LiveMatchDto(List.of("r1"), List.of(), null, null, "7",
				null)))).isEqualTo("{\"matches\":[{\"teamA\":[\"r1\"],\"teamB\":[],\"court\":\"7\"}]}");
	}

	@Test
	public void ranglisteSendetEintraege() {
		assertThat(TournamentSyncClient.ranglistePfad("t1")).isEqualTo("/api/sync/tournaments/t1/ranking");
		assertThat(TournamentSyncClient.ranglisteBody(List.of(new LiveRankingEntryDto(1, List.of("r1"), 3, 39, 12))))
				.isEqualTo(
						"{\"entries\":[{\"place\":1,\"registrationIds\":[\"r1\"],\"wins\":3,\"pointsFor\":39,\"pointsAgainst\":12}]}");
	}

	private HttpClient httpClientMitAntwort(String body) throws Exception {
		HttpClient httpClient = mock(HttpClient.class);
		HttpResponse<String> response = mockResponse(200, body);
		when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(response);
		return httpClient;
	}

	private static HttpRequest gesendeteAnfrage(HttpClient httpClient) throws Exception {
		ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
		verify(httpClient).send(captor.capture(), any());
		return captor.getValue();
	}

	/** Liest den Body einer gebauten Anfrage über das Flow-API des {@link HttpRequest.BodyPublisher}. */
	private static String bodyAlsText(HttpRequest request) {
		StringBuilder text = new StringBuilder();
		request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
			@Override
			public void onSubscribe(Flow.Subscription subscription) {
				subscription.request(Long.MAX_VALUE);
			}

			@Override
			public void onNext(ByteBuffer teil) {
				text.append(StandardCharsets.UTF_8.decode(teil));
			}

			@Override
			public void onError(Throwable fehler) {
				throw new IllegalStateException(fehler);
			}

			@Override
			public void onComplete() {
				// Body vollständig gelesen
			}
		});
		return text.toString();
	}

	private TournamentSyncClient clientMitAntwort(String body) throws Exception {
		HttpClient httpClient = mock(HttpClient.class);
		HttpResponse<String> response = mockResponse(200, body);
		when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(response);
		return new TournamentSyncClient(httpClient, "https://ptm-online.example.com", "ptm_secret");
	}
}
