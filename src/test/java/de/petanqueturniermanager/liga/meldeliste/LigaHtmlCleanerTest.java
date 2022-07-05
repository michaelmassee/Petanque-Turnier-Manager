package de.petanqueturniermanager.liga.meldeliste;

import java.io.File;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LigaHtmlCleanerTest {

	static String PDF_IMAGE = "<img src=\"http://bc-linden.de/images/bclinden/pdf-download.png\" align=\"right\" style=\"width:50px;\">";
	static String BFL_IMAGE = "<img src=\"http://bc-linden.de/oeffentlich/bfl/logo.png\" align=\"right\" style=\"width:80px;\">";
	static String PDF_BASE_URL = "http://bc-linden.de/oeffentlich/bfl/";

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void testCleanUp() throws Exception {
		File targetHtml = folder.newFile("LigaGeneratedClean.html");
		URL ligaorg = LigaHtmlCleanerTest.class.getClassLoader().getResource("LigaGeneratedOrginal.html");
		LigaHtmlCleaner ligaHtmlCleaner = new LigaHtmlCleaner(ligaorg.toURI(), targetHtml);
		ligaHtmlCleaner.logoUrl(BFL_IMAGE).gruppe("Gruppe 1").pdfImageUrl(PDF_IMAGE).pdfDownloadBaseUrl(PDF_BASE_URL)
				.cleanUp();

		// for local test only
		File target = new File("/home/michael/tmp", "LigaGeneratedClean.html");
		System.out.println("Copy nach " + target);
		FileUtils.copyFile(targetHtml, target);
	}
}