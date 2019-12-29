package de.petanqueturniermanager.comp.newrelease;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.kohsuke.github.GHRelease;
import org.powermock.api.mockito.PowerMockito;

import com.sun.star.uno.XComponentContext;

public class NewReleaseCheckerTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	Path testFile;
	private NewReleaseChecker newReleaseChecker;
	private String testFileStr = "release.info";

	@Before
	public void setup() throws IOException {

		XComponentContext xComponentContextMock = PowerMockito.mock(XComponentContext.class);

		testFile = Paths.get(folder.newFile(testFileStr).toURI());
		newReleaseChecker = new NewReleaseChecker(xComponentContextMock) {
			@Override
			Path getReleaseFile() {
				return testFile;
			}
		};
	}

	@Test
	public void testWriteLatestRelease() throws Exception {
		newReleaseChecker.writeLatestRelease();
		assert Files.exists(testFile);
	}

	@Test
	public void testReadLatestRelease() throws Exception {
		newReleaseChecker.writeLatestRelease();
		GHRelease readLatestRelease = newReleaseChecker.readLatestRelease();
		assertThat(readLatestRelease).isNotNull();
		assertThat(readLatestRelease.getName()).isNotNull().isNotEmpty();
	}

}
