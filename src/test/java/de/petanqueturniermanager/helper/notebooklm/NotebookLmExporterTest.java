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
	void bereiteExportOrdnerVorLegtZeitgestempeltenOrdnerMitDokumentationUnterordnerAn() throws Exception {
		Path ordner = NotebookLmExporter.bereiteExportOrdnerVor(basisVerzeichnis);

		assertThat(ordner.getParent()).isEqualTo(basisVerzeichnis);
		assertThat(ordner.getFileName().toString()).startsWith("NotebookLM-Export-");
		assertThat(ordner.resolve(NotebookLmExporter.DOKUMENTATION_UNTERORDNER)).isDirectory();
	}

	@Test
	void exportiereDokumentationUndQuellenKopiertGebuendelteDokusUndSchreibtQuellenMd() throws Exception {
		Path ordner = NotebookLmExporter.bereiteExportOrdnerVor(basisVerzeichnis);

		NotebookLmExporter.exportiereDokumentationUndQuellen(ordner);

		Path dokuOrdner = ordner.resolve(NotebookLmExporter.DOKUMENTATION_UNTERORDNER);
		assertThat(dokuOrdner.resolve("CLAUDE.md")).isRegularFile();
		assertThat(dokuOrdner.resolve("BUILD_ISSUES.md")).isRegularFile();
		assertThat(dokuOrdner.resolve("tools-README.md")).isRegularFile();
		assertThat(dokuOrdner.resolve("03_Schweizer.md")).isRegularFile();

		Path quellenDatei = ordner.resolve(NotebookLmExporter.QUELLEN_DATEINAME);
		assertThat(quellenDatei).isRegularFile();
		String inhalt = liesDatei(quellenDatei);
		assertThat(inhalt).contains(NotebookLmExporter.GITHUB_REPO_URL);
		assertThat(inhalt).contains(NotebookLmExporter.GITHUB_WIKI_URL);
		assertThat(inhalt).contains("en.wikipedia.org/wiki/Swiss-system_tournament");
	}

	private static String liesDatei(Path datei) throws IOException {
		return Files.readString(datei);
	}
}
