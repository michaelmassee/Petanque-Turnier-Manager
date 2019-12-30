/**
 * Erstellung 26.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import com.g00fy2.versioncompare.Version;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.PetanqueTurnierManagerImpl;

/**
 * Test Github if new Release available
 *
 * @author Michael Massee
 *
 */
public class NewReleaseChecker {
	private static final Logger logger = LogManager.getLogger(NewReleaseChecker.class);

	// http://api.github.com/repos/michaelmassee/Petanque-Turnier-Manager/releases/latest
	public static final URI RELEASE_FILE = new File(PetanqueTurnierManagerImpl.BASE_INTERNAL_DIR, "release.info").toURI();

	static boolean isUpdateThreadRunning = false;
	static boolean didAlreadyRun = false;

	/**
	 * nur einmal abfragen, und latest release info aktualisieren
	 */

	public void runUpdateOnceThread() {
		if (!isUpdateThreadRunning && !didAlreadyRun) {
			new Thread("Update Latest Release") {
				@Override
				public void run() {
					try {
						isUpdateThreadRunning = true;
						writeLatestRelease();
					} finally {
						isUpdateThreadRunning = false;
						didAlreadyRun = true; // nur einmal laufen
					}
				}
			}.start();
		}
	}

	void writeLatestRelease() {
		try {
			GitHub github = new GitHubBuilder().build();
			GHRepository repository = github.getRepository("michaelmassee/Petanque-Turnier-Manager");
			GHRelease latestRelease = repository.getLatestRelease();

			if (!latestRelease.isPrerelease()) {
				// wenn kein Prerelease
				Gson gson = new GsonBuilder().setPrettyPrinting().addSerializationExclusionStrategy(new ReleaseExclusionStrategy()).create();

				logger.info("Latest Release = " + latestRelease.getName());

				try (BufferedWriter writer = Files.newBufferedWriter(getReleaseFile())) {
					writer.write(gson.toJson(latestRelease));
				}
			}
		} catch (IOException e) {
			logger.error(e);
		}
	}

	@VisibleForTesting
	Path getReleaseFile() {
		return Paths.get(RELEASE_FILE);
	}

	public GHRelease readLatestRelease() {
		GHRelease ret = null;
		Path pathReleaseFile = getReleaseFile();
		if (!(Files.exists(pathReleaseFile) && Files.isReadable(pathReleaseFile))) {
			return null;
		}
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(RELEASE_FILE))) {
			Gson gson = new GsonBuilder().addDeserializationExclusionStrategy(new ReleaseExclusionStrategy()).create();
			ret = gson.fromJson(reader, GHRelease.class);
		} catch (IOException e) {
			logger.error(e);
		}
		return ret;
	}

	public boolean checkForNewRelease(XComponentContext context) {
		boolean newVersionAvailable = false;
		try {

			// https://www.baeldung.com/java-download-file
			// https://github.com/G00fY2/version-compare

			if (!isUpdateThreadRunning) {
				String versionNummer = ExtensionsHelper.from(context).getVersionNummer();
				GHRelease readLatestRelease = readLatestRelease();
				if (readLatestRelease != null) {
					String latestVersionFromGithub = readLatestRelease.getName();
					// clean up name
					latestVersionFromGithub = StringUtils.stripStart(latestVersionFromGithub, "vV");
					newVersionAvailable = new Version(versionNummer).isLowerThan(latestVersionFromGithub);
				}
			}
		} catch (Exception e) {
			// fehler nur loggen
			logger.error(e);
		}

		return newVersionAvailable;
	}

}
