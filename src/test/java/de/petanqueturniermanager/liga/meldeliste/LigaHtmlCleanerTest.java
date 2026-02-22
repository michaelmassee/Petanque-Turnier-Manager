package de.petanqueturniermanager.liga.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.IntStream;

import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class LigaHtmlCleanerTest {

	private static final Logger logger = LogManager.getLogger(LigaHtmlCleanerTest.class);

	static String BFL_LOGO_URL = "http://bc-linden.de/oeffentlich/bfl/logo.png";
	static String PDF_BASE_URL = "http://bc-linden.de/oeffentlich/bfl/";
	static String PDF_IMAGE_URL = "http://bc-linden.de/images/bclinden/pdf-download.png";

	@TempDir
	Path tempDir;

	@Test
	public void testCleanUp() throws Exception {
		File targetHtml = tempDir.resolve("LigaGeneratedClean.html").toFile();

		URL htmlOrgUrl = LigaHtmlCleanerTest.class.getResource("LigaGruppe2.html");
		assertThat(htmlOrgUrl).isNotNull();

		LigaHtmlCleaner ligaHtmlCleaner = new LigaHtmlCleaner(htmlOrgUrl.toURI(), targetHtml);
		ligaHtmlCleaner.logoUrl(BFL_LOGO_URL).pdfImageUrl(PDF_IMAGE_URL).ranglistePdfName("rangliste.pdf")
				.spielplanPdfName("spielplan.pdf").pdfDownloadBaseUrl(PDF_BASE_URL).cleanUp();

		URL refHtml = LigaHtmlCleanerTest.class.getResource("LigaGeneratedClean_ref.html");
		assertThat(refHtml).isNotNull();
		assertThat(targetHtml).exists();

		// for local test only
		File target = new File("/home/michael/tmp", "LigaGeneratedClean.html");
		System.out.println("Copy nach " + target);
		FileUtils.copyFile(targetHtml, target);

		List<String> reflist = Files.readAllLines(Paths.get(refHtml.toURI()));
		List<String> targetlist = Files.readAllLines(Paths.get(targetHtml.toURI()));

		IntStream.range(0, reflist.size()).forEach(i -> {
			logger.info("Validate " + reflist.get(i));
			assertThat(targetlist.get(i)).isEqualTo(reflist.get(i));
		});

	}
}