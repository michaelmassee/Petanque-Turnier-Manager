package de.petanqueturniermanager.helper.notebooklm;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NotebookLmExporterTest {

	@TempDir
	Path basisVerzeichnis;

	@Test
	void bereiteExportOrdnerVorLegtZeitgestempeltenOrdnerAn() throws Exception {
		Path ordner = NotebookLmExporter.bereiteExportOrdnerVor(basisVerzeichnis);

		assertThat(ordner.getParent()).isEqualTo(basisVerzeichnis);
		assertThat(ordner.getFileName().toString()).startsWith("NotebookLM-Export-");
		assertThat(ordner).isDirectory();
	}

	@Test
	void exportiereQuellenSchreibtQuellenMdMitProjektUndWikipediaLinks() throws Exception {
		Path ordner = NotebookLmExporter.bereiteExportOrdnerVor(basisVerzeichnis);

		NotebookLmExporter.exportiereQuellen(ordner);

		Path quellenDatei = ordner.resolve(NotebookLmExporter.QUELLEN_DATEINAME);
		assertThat(quellenDatei).isRegularFile();
		String inhalt = liesDatei(quellenDatei);
		assertThat(inhalt).contains(NotebookLmExporter.GITHUB_REPO_URL);
		assertThat(inhalt).contains(NotebookLmExporter.GITHUB_WIKI_URL);
		assertThat(inhalt).contains("en.wikipedia.org/wiki/Swiss-system_tournament");
		assertThat(inhalt).contains("help.libreoffice.org");
		assertThat(inhalt).contains("api.libreoffice.org");
	}

	private static String liesDatei(Path datei) throws IOException {
		return Files.readString(datei);
	}
}
