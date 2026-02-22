package de.petanqueturniermanager.comp.newrelease;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kohsuke.github.GHRelease;

public class NewReleaseCheckerTest {

	@TempDir
	Path tempDir;

	Path testFile;
	private NewReleaseChecker newReleaseChecker;
	private String testFileStr = "release.info";

	@BeforeEach
	public void setup() throws IOException {

		// XComponentContext xComponentContextMock =
		// PowerMockito.mock(XComponentContext.class);

		testFile = tempDir.resolve(testFileStr);
		newReleaseChecker = new NewReleaseChecker() {
			@Override
			Path getReleaseFile() {
				return testFile;
			}
		};
	}

	@Test
	public void testWriteLatestRelease() throws Exception {
		newReleaseChecker.writeLatestReleaseFromGithubInCacheFile();
		assert Files.exists(testFile);
	}

	@Test
	public void testReadLatestRelease() throws Exception {
		newReleaseChecker.writeLatestReleaseFromGithubInCacheFile();
		GHRelease readLatestRelease = newReleaseChecker.readLatestReleaseFromCacheFile();
		assertThat(readLatestRelease).isNotNull();
		assertThat(readLatestRelease.getName()).isNotNull().isNotEmpty();
	}

	@Test
	public void testGetDownloadURL() throws Exception {
		URL result = newReleaseChecker.getDownloadURL(newReleaseChecker.getDownloadGHAsset());
		assertThat(result).isNotNull();
		assertThat(result.getFile()).endsWith(NewReleaseChecker.EXTENSION_FILE_SUFFIX);
	}

}
