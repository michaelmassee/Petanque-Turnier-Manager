/**
 * Erstellung 26.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.github.GHAsset;
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
	public static final String EXTENSION_FILE_SUFFIX = "oxt";
	private static final String GITHUB_PETANQUE_TURNIER_MANAGER = "michaelmassee/Petanque-Turnier-Manager";

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
		GHRelease latestRelease = getLatestRelease();
		if (latestRelease != null && !latestRelease.isPrerelease()) {
			// wenn kein Prerelease
			Gson gson = new GsonBuilder().setPrettyPrinting().addSerializationExclusionStrategy(new ReleaseExclusionStrategy()).create();

			logger.info("Latest Release = " + latestRelease.getName());

			try (BufferedWriter writer = Files.newBufferedWriter(getReleaseFile())) {
				writer.write(gson.toJson(latestRelease));
			} catch (IOException e) {
				logger.error(e);
			}
		}
	}

	private GHRelease getLatestRelease() {
		GHRelease latestRelease = null;
		try {
			GitHub github = new GitHubBuilder().build();
			GHRepository repository = github.getRepository(GITHUB_PETANQUE_TURNIER_MANAGER);
			latestRelease = repository.getLatestRelease();
		} catch (IOException e) {
			logger.error(e);
		}
		return latestRelease;
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

	public GHAsset getDownloadGHAsset() {
		GHAsset otxAsset = null;

		GHRelease latestRelease = getLatestRelease();
		if (latestRelease != null && !latestRelease.isPrerelease()) {
			try {
				List<GHAsset> assets = latestRelease.getAssets();
				otxAsset = assets.stream().filter(ghasset -> {
					return ghasset.getName().toLowerCase().endsWith(EXTENSION_FILE_SUFFIX);
				}).findFirst().orElse(null);
			} catch (IOException e) {
				logger.error(e);
			}
		}
		return otxAsset;
	}

	public URL getDownloadURL() {
		GHAsset otxAsset = getDownloadGHAsset();
		return getDownloadURL(otxAsset);
	}

	public URL getDownloadURL(GHAsset otxAsset) {
		URL download = null;
		if (otxAsset != null) {
			try {
				download = new URL(otxAsset.getBrowserDownloadUrl());
			} catch (MalformedURLException e) {
				logger.error(e);
			}
		}
		return download;
	}

}
