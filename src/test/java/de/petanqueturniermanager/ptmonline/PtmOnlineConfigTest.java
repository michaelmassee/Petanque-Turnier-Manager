package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PtmOnlineConfigTest {

	@TempDir
	Path tempDir;

	@Test
	public void speichertUndLaedtBaseUrlUndApiKey() throws Exception {
		Path configFile = tempDir.resolve("ptm-online.properties");

		PtmOnlineConfig config = new PtmOnlineConfig(configFile);
		config.save("https://ptm-online.example.com", "ptm_secret");

		PtmOnlineConfig reloaded = new PtmOnlineConfig(configFile);
		assertThat(reloaded.getBaseUrl()).isEqualTo("https://ptm-online.example.com");
		assertThat(reloaded.getApiKey()).isEqualTo("ptm_secret");
		assertThat(reloaded.isConfigured()).isTrue();
	}

	@Test
	public void istNichtKonfiguriertWennDateiFehlt() {
		PtmOnlineConfig config = new PtmOnlineConfig(tempDir.resolve("fehlt.properties"));
		assertThat(config.isConfigured()).isFalse();
	}
}
