package de.petanqueturniermanager.comp.newrelease;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kohsuke.github.GHRelease;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.comp.newrelease.ReleaseInfosAnzeigen;

/**
 * UITest für NewReleaseChecker-Hilfsmethoden und ReleaseInfosAnzeigen.
 * <p>
 * Im gleichen Package wie {@link NewReleaseChecker}, damit package-private Methoden zugänglich sind.
 */
public class NewReleaseInfosAnzeigenUITest extends BaseCalcUITest {

	@TempDir
	Path tempDir;

	/**
	 * TestNewReleaseChecker – überschreibt latestVersionFromCacheFile() für
	 * reproduzierbare Tests ohne Netzwerkzugriff.
	 */
	static class TestNewReleaseChecker extends NewReleaseChecker {

		private final String simulierteVersion;
		private final Path testReleaseFile;

		TestNewReleaseChecker(String simulierteVersion, Path testReleaseFile) {
			this.simulierteVersion = simulierteVersion;
			this.testReleaseFile = testReleaseFile;
		}

		@Override
		Path getReleaseFile() {
			return testReleaseFile;
		}

		@Override
		String latestVersionFromCacheFile() {
			return simulierteVersion;
		}
	}

	private TestNewReleaseChecker testChecker;

	@BeforeEach
	void setup() {
		testChecker = new TestNewReleaseChecker("99.99.99", tempDir.resolve("release.info"));
	}

	@Test
	void testNeueVersionVerfuegbar_WennCacheVersionHoeher() {
		XComponentContext ctx = starter.getxComponentContext();
		// Installierte Version ist niedriger als 99.99.99 → neue Version verfügbar
		boolean neueVersion = testChecker.checkForNewRelease(ctx);
		assertThat(neueVersion).isTrue();
	}

	@Test
	void testReleaseInfosAnzeigen_OhneReleaseDaten_ZeigtFehler() throws Exception {
		// Kein Cache-File vorhanden → readLatestReleaseFromCacheFile() liefert null → Fehler in ProcessBox
		var sheet = new ReleaseInfosAnzeigen(wkingSpreadsheet) {
			@Override
			NewReleaseChecker newReleaseChecker() {
				return new NewReleaseChecker() {
					@Override
					Path getReleaseFile() {
						return tempDir.resolve("nicht_vorhanden.info");
					}
				};
			}
		};
		sheet.start();
		// kurz warten bis Thread fertig
		Thread.sleep(500);
		// kein Absturz – Test erfolgreich wenn keine Exception geworfen wurde
	}
}
