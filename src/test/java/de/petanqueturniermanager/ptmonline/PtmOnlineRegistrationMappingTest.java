package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.helper.DocumentPropertiesHelper;

public class PtmOnlineRegistrationMappingTest {

	/** Simuliert die Property-Ablage von {@link DocumentPropertiesHelper} in einer einfachen Map. */
	private DocumentPropertiesHelper fakeDocProps() {
		Map<String, String> gespeichert = new HashMap<>();
		DocumentPropertiesHelper docProps = mock(DocumentPropertiesHelper.class);
		org.mockito.Mockito.doAnswer(invocation -> {
			gespeichert.put(invocation.getArgument(0), invocation.getArgument(1));
			return null;
		}).when(docProps).setStringProperty(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
		when(docProps.getStringProperty(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
				.thenAnswer(invocation -> gespeichert.getOrDefault(invocation.<String>getArgument(0), invocation.getArgument(1)));
		return docProps;
	}

	@Test
	public void speichertUndLiestMappingRoundtrip() {
		PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(fakeDocProps());

		mapping.addMapping(3, "reg-abc");
		mapping.addMapping(7, "reg-xyz");

		assertThat(mapping.getOnlineId(3)).contains("reg-abc");
		assertThat(mapping.getOnlineId(7)).contains("reg-xyz");
		assertThat(mapping.getOnlineId(99)).isEmpty();
		assertThat(mapping.istBereitsImportiert("reg-abc")).isTrue();
		assertThat(mapping.istBereitsImportiert("reg-unbekannt")).isFalse();
	}

	@Test
	public void liestLeeresMappingOhneFehler() {
		PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(fakeDocProps());

		assertThat(mapping.getOnlineId(1)).isEmpty();
		assertThat(mapping.getLastSync()).isEmpty();
		assertThat(mapping.getTournamentId()).isEmpty();
	}

	@Test
	public void speichertUndLiestLastSyncUndTournamentId() {
		PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(fakeDocProps());
		Instant zeitpunkt = Instant.parse("2026-08-26T10:00:00Z");

		mapping.setLastSync(zeitpunkt);
		mapping.setTournamentId("tid-42");

		assertThat(mapping.getLastSync()).contains(zeitpunkt);
		assertThat(mapping.getTournamentId()).contains("tid-42");
	}

	@Test
	public void behandeltSonderzeichenInIdsKorrekt() {
		PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(fakeDocProps());

		mapping.addMapping(1, "reg-\"quoted\"-äöü");

		assertThat(mapping.getOnlineId(1)).contains("reg-\"quoted\"-äöü");
	}
}
