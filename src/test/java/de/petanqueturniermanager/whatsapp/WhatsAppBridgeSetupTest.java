package de.petanqueturniermanager.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WhatsAppBridgeSetupTest {

	@TempDir
	Path tempDir;

	@Test
	void erfuelltNodeMindestversion_akzeptiertUnterstuetzteVersion() {
		assertThat(WhatsAppBridgeSetup.erfuelltNodeMindestversion("v22.22.2")).isTrue();
		assertThat(WhatsAppBridgeSetup.erfuelltNodeMindestversion("v20.0.0")).isTrue();
	}

	@Test
	void erfuelltNodeMindestversion_lehntZuAlteVersionAb() {
		assertThat(WhatsAppBridgeSetup.erfuelltNodeMindestversion("v18.20.4")).isFalse();
		assertThat(WhatsAppBridgeSetup.erfuelltNodeMindestversion("v16.20.2")).isFalse();
	}

	@Test
	void erfuelltNodeMindestversion_lehntUnlesbareAusgabeAb() {
		assertThat(WhatsAppBridgeSetup.erfuelltNodeMindestversion("")).isFalse();
		assertThat(WhatsAppBridgeSetup.erfuelltNodeMindestversion("command not found")).isFalse();
	}

	@Test
	void fehlendesNodeLoestSetupDialogAus() {
		assertThatThrownBy(() -> WhatsAppBridgeSetup.nodePfadOderSetupNoetig(tempDir, ""))
				.isInstanceOf(WhatsAppBridgeSetupRequiredException.class)
				.hasMessageContaining("Node.js wurde nicht gefunden");
	}

	@Test
	void linuxX64PaketVerwendetPortableLtsRuntime() {
		Properties vorher = (Properties) System.getProperties().clone();
		try {
			System.setProperty("os.name", "Linux");
			System.setProperty("os.arch", "amd64");

			WhatsAppBridgeSetup.NodePaket paket = WhatsAppBridgeSetup.paketFuerAktuellePlattform();

			assertThat(paket.dateiname()).isEqualTo("node-v22.22.2-linux-x64.tar.xz");
			assertThat(paket.executableRelativ()).isEqualTo(Path.of("bin", "node"));
			assertThat(paket.url()).isEqualTo("https://nodejs.org/dist/v22.22.2/node-v22.22.2-linux-x64.tar.xz");
		} finally {
			System.setProperties(vorher);
		}
	}

	@Test
	void zipArchivWirdSicherEntpackt() throws Exception {
		Path zip = tempDir.resolve("node.zip");
		erstelleZip(zip, "node/bin/node", "node");

		WhatsAppBridgeSetup.entpackeZip(zip, tempDir.resolve("runtime"));

		assertThat(tempDir.resolve("runtime").resolve("node/bin/node"))
				.hasContent("node");
	}

	@Test
	void zipSlipEintragWirdAbgelehnt() throws Exception {
		Path zip = tempDir.resolve("node.zip");
		erstelleZip(zip, "../evil", "evil");

		assertThatIOException()
				.isThrownBy(() -> WhatsAppBridgeSetup.entpackeZip(zip, tempDir.resolve("runtime")))
				.withMessageContaining("Unsicherer Archiv-Eintrag");
	}

	private static void erstelleZip(Path zip, String name, String inhalt) throws IOException {
		try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
			out.putNextEntry(new ZipEntry(name));
			out.write(inhalt.getBytes(StandardCharsets.UTF_8));
			out.closeEntry();
		}
	}
}
