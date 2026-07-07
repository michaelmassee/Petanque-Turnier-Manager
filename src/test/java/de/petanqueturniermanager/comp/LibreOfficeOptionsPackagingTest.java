package de.petanqueturniermanager.comp;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class LibreOfficeOptionsPackagingTest {

	private static final List<String> MANIFEST_EINTRAEGE = List.of(
			"registry/schema/org/openoffice/Office/Custom/PetanqueTurnierManager.xcs",
			"registry/data/org/openoffice/Office/Custom/PetanqueTurnierManager.xcu",
			"registry/data/org/openoffice/Office/OptionsDialog.xcu");

	@Test
	void optionsDateienSindImManifestEingetragen() throws Exception {
		String manifest = Files.readString(Path.of("META-INF/manifest.xml"));

		for (String eintrag : MANIFEST_EINTRAEGE) {
			assertThat(manifest)
					.as("Manifest-Eintrag fehlt: %s", eintrag)
					.contains("manifest:full-path=\"" + eintrag + "\"");
		}
	}

	@Test
	void pluginOptionsEventHandlerIstRegistriert() throws Exception {
		String components = Files.readString(Path.of("PetanqueTurnierManager.components"));
		assertThat(components)
				.contains("de.petanqueturniermanager.comp.PluginOptionsEventHandler")
				.contains("de.petanqueturniermanager.PluginOptionsEventHandler");

		String registration = Files.readString(
				Path.of("src/main/resources/de/petanqueturniermanager/comp/RegistrationHandler.classes"));
		assertThat(registration).contains("de.petanqueturniermanager.comp.PluginOptionsEventHandler");
	}

	@Test
	void optionsDialogVerweistAufHandlerUndXdl() throws Exception {
		String xcu = Files.readString(Path.of("registry/data/org/openoffice/Office/OptionsDialog.xcu"));
		Path xdl = Path.of("registry/data/org/openoffice/Office/dialogs/PluginOptions.xdl");

		assertThat(xcu)
				.contains("PetanqueTurnierManager")
				.contains("%origin%/dialogs/PluginOptions.xdl")
				.contains("de.petanqueturniermanager.PluginOptionsEventHandler");
		assertThat(xdl).exists();
	}

	@Test
	void altesPluginKonfigurationsKommandoIstAusMenueEntfernt() throws Exception {
		String addons = Files.readString(Path.of("registry/org/openoffice/Office/Addons_Y2_Info.xcu"));

		assertThat(addons).doesNotContain("ptm:pluginKonfiguration");
	}
}
