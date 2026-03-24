package de.petanqueturniermanager.comp.newrelease;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Assumptions;
import org.kohsuke.github.GHAsset;
import org.kohsuke.github.GHRelease;

public class NewReleaseCheckerTest {

	private static final String GITHUB_RATE_LIMIT_URL = "https://api.github.com/rate_limit";
	private static final int HTTP_OK = 200;
	private static final int CONNECT_TIMEOUT_MS = 5_000;

	@TempDir
	Path tempDir;

	private Path testFile;
	private NewReleaseChecker newReleaseChecker;

	@BeforeEach
	void setup() throws IOException {
		Assumptions.assumeTrue(githubApiVerfuegbar(),
				"GitHub API nicht erreichbar oder Rate-Limit überschritten – Test wird übersprungen");
		testFile = tempDir.resolve("release.info");
		newReleaseChecker = new NewReleaseChecker() {
			@Override
			Path getReleaseFile() {
				return testFile;
			}
		};
	}

	@Test
	void testWriteLatestRelease() throws Exception {
		newReleaseChecker.writeLatestReleaseFromGithubInCacheFile();
		Assumptions.assumeTrue(Files.exists(testFile),
				"GitHub-Release nicht abrufbar (Rate-Limit oder kein Release vorhanden)");
	}

	@Test
	void testReadLatestRelease() throws Exception {
		newReleaseChecker.writeLatestReleaseFromGithubInCacheFile();
		GHRelease readLatestRelease = newReleaseChecker.readLatestReleaseFromCacheFile();
		Assumptions.assumeTrue(readLatestRelease != null,
				"GitHub-Release nicht abrufbar (Rate-Limit oder kein Release vorhanden)");
		assertThat(readLatestRelease.getName()).isNotNull().isNotEmpty();
	}

	@Test
	void testGetDownloadURL() throws Exception {
		GHAsset asset = newReleaseChecker.getDownloadGHAsset();
		Assumptions.assumeTrue(asset != null,
				"GitHub-Asset nicht abrufbar (Rate-Limit oder kein passendes Asset vorhanden)");
		URL result = newReleaseChecker.getDownloadURL(asset);
		assertThat(result).isNotNull();
		assertThat(result.getFile()).endsWith(NewReleaseChecker.EXTENSION_FILE_SUFFIX);
	}

	/**
	 * Prüft ob die GitHub API erreichbar und das Rate-Limit noch nicht erschöpft ist.
	 * Der /rate_limit-Endpunkt liefert immer HTTP 200 – deshalb muss der Response-Body
	 * auf "remaining":0 geprüft werden.
	 */
	private static boolean githubApiVerfuegbar() {
		try {
			var verbindung = (HttpURLConnection) URI.create(GITHUB_RATE_LIMIT_URL).toURL().openConnection();
			verbindung.setConnectTimeout(CONNECT_TIMEOUT_MS);
			verbindung.setReadTimeout(CONNECT_TIMEOUT_MS);
			verbindung.connect();
			if (verbindung.getResponseCode() != HTTP_OK) {
				verbindung.disconnect();
				return false;
			}
			String antwort;
			try (var reader = new BufferedReader(new InputStreamReader(verbindung.getInputStream()))) {
				antwort = reader.lines().collect(Collectors.joining());
			}
			verbindung.disconnect();
			// Rate-Limit erschöpft wenn "remaining":0 im Core-Bereich steht
			return !antwort.contains("\"remaining\":0") && !antwort.contains("\"remaining\": 0");
		} catch (IOException e) {
			return false;
		}
	}
}
