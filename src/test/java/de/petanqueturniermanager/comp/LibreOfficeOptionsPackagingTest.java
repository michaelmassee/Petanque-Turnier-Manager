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
	void optionsEventHandlerSindRegistriert() throws Exception {
		String components = Files.readString(Path.of("PetanqueTurnierManager.components"));
		assertThat(components)
				.contains("de.petanqueturniermanager.comp.PluginOptionsEventHandler")
				.contains("de.petanqueturniermanager.PluginOptionsEventHandler")
				.contains("de.petanqueturniermanager.comp.WebserverRegieOptionsEventHandler")
				.contains("de.petanqueturniermanager.WebserverRegieOptionsEventHandler")
				.contains("de.petanqueturniermanager.comp.CompositeViewsOptionsEventHandler")
				.contains("de.petanqueturniermanager.CompositeViewsOptionsEventHandler");

		String registration = Files.readString(
				Path.of("src/main/resources/de/petanqueturniermanager/comp/RegistrationHandler.classes"));
		assertThat(registration)
				.contains("de.petanqueturniermanager.comp.PluginOptionsEventHandler")
				.contains("de.petanqueturniermanager.comp.WebserverRegieOptionsEventHandler")
				.contains("de.petanqueturniermanager.comp.CompositeViewsOptionsEventHandler");
	}

	@Test
	void optionsDialogVerweistAufHandlerUndXdls() throws Exception {
		String xcu = Files.readString(Path.of("registry/data/org/openoffice/Office/OptionsDialog.xcu"));
		Path pluginXdl = Path.of("registry/data/org/openoffice/Office/dialogs/PluginOptions.xdl");
		Path regieXdl = Path.of("registry/data/org/openoffice/Office/dialogs/WebserverRegieOptions.xdl");
		Path compositeViewsXdl = Path.of("registry/data/org/openoffice/Office/dialogs/CompositeViewsOptions.xdl");

		assertThat(xcu)
				.contains("PetanqueTurnierManager")
				.contains("%origin%/dialogs/PluginOptions.xdl")
				.contains("de.petanqueturniermanager.PluginOptionsEventHandler")
				.contains("%origin%/dialogs/WebserverRegieOptions.xdl")
				.contains("de.petanqueturniermanager.WebserverRegieOptionsEventHandler")
				.contains("%origin%/dialogs/CompositeViewsOptions.xdl")
				.contains("de.petanqueturniermanager.CompositeViewsOptionsEventHandler");
		assertThat(pluginXdl).exists();
		assertThat(regieXdl).exists();
		assertThat(compositeViewsXdl).exists();
	}

	@Test
	void compositeViewsOptionsSeiteIstPaketiert() throws Exception {
		String xcu = Files.readString(Path.of("registry/data/org/openoffice/Office/OptionsDialog.xcu"));
		String compositeViewsXdl = Files.readString(
				Path.of("registry/data/org/openoffice/Office/dialogs/CompositeViewsOptions.xdl"));

		assertThat(xcu)
				.contains("de.petanqueturniermanager.options.compositeviews")
				.contains("de.petanqueturniermanager.compositeviews");
		assertThat(compositeViewsXdl)
				.contains("dlg:id=\"CompositeViewsLabel\"")
				.contains("dlg:id=\"WebserverAktiv\"")
				.contains("dlg:id=\"CompositeViewsFilterSystem\"")
				.contains("dlg:id=\"CompositeViewsListe\"")
				.contains("dlg:id=\"CompositeViewsHinzufuegen\"")
				.contains("dlg:id=\"CompositeViewsBearbeiten\"")
				.contains("dlg:id=\"CompositeViewsLoeschen\"");
	}

	@Test
	void altesWebserverKonfigurationsKommandoIstAusMenueEntfernt() throws Exception {
		String addons = Files.readString(Path.of("registry/org/openoffice/Office/Addons_X3_Webserver.xcu"));

		assertThat(addons).doesNotContain("ptm:webserver_konfiguration");
	}

	@Test
	void webserverRegieOptionenSindInLibreOfficeKonfigurationUndDialogPaketiert() throws Exception {
		String schema = Files.readString(
				Path.of("registry/schema/org/openoffice/Office/Custom/PetanqueTurnierManager.xcs"));
		String data = Files.readString(
				Path.of("registry/data/org/openoffice/Office/Custom/PetanqueTurnierManager.xcu"));
		String regieXdl = Files.readString(
				Path.of("registry/data/org/openoffice/Office/dialogs/WebserverRegieOptions.xdl"));
		String pluginXdl = Files.readString(Path.of("registry/data/org/openoffice/Office/dialogs/PluginOptions.xdl"));

		assertThat(schema)
				.contains("oor:name=\"WebserverRegie\"")
				.contains("oor:name=\"Active\"")
				.contains("oor:name=\"Port\"")
				.contains("oor:name=\"TargetsJson\"")
				.contains("oor:name=\"LegacyPropertiesImported\"");
		assertThat(data)
				.contains("<node oor:name=\"WebserverRegie\">")
				.contains("<prop oor:name=\"Active\">")
				.contains("<prop oor:name=\"Port\">")
				.contains("<prop oor:name=\"TargetsJson\">")
				.contains("<prop oor:name=\"LegacyPropertiesImported\">");
		assertThat(regieXdl)
				.contains("dlg:id=\"WebserverRegieLabel\"")
				.contains("dlg:id=\"WebserverRegieActive\"")
				.contains("dlg:id=\"WebserverRegiePortLabel\"")
				.contains("dlg:id=\"WebserverRegiePort\"");
		assertThat(pluginXdl).doesNotContain("WebserverRegie");
	}

	@Test
	void altesPluginKonfigurationsKommandoIstAusMenueEntfernt() throws Exception {
		String addons = Files.readString(Path.of("registry/org/openoffice/Office/Addons_Y2_Info.xcu"));

		assertThat(addons).doesNotContain("ptm:pluginKonfiguration");
	}
}
