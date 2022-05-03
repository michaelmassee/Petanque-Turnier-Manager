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
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;

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
	public static final URI RELEASE_FILE = new File(PetanqueTurnierManagerImpl.BASE_INTERNAL_DIR, "release.info")
			.toURI();

	private static boolean isUpdateThreadRunning = false; // thread nur einmal laufen lassen
	private static boolean didAlreadyRun = false;
	private static boolean didUpdateCacheFile = false;

	// !! wird mehrmals aufgerufen
	public synchronized void checkForUpdate(XComponentContext xContext) {
		runUpdateCacheFileOnceThread(); // update release info

		if (didUpdateCacheFile && !didAlreadyRun) {
			didAlreadyRun = true;
			boolean isnewRelease = checkForNewRelease(xContext);
			if (isnewRelease) {
				logger.debug("open MessageBox");

				GHRelease gHRelease = readLatestReleaseFromCacheFile();
				String releaseNotes = "";
				if (gHRelease != null) {
					releaseNotes = gHRelease.getBody();
				}

				MessageBoxResult answer = MessageBox.from(xContext, MessageBoxTypeEnum.QUESTION_YES_NO)
						.caption("Neue Version")
						.message("Eine neue Version (" + latestVersionFromCacheFile()
								+ ") von Pétanque-Turnier-Manager ist verfügbar.\r\n\r\n'" + releaseNotes
								+ "'\r\n\r\nDownload ?")
						.show();

				if (MessageBoxResult.YES == answer) {
					new DownloadExtension(new WorkingSpreadsheet(xContext)).start();
				}
			}
		}
	}

	/**
	 * nur einmal abfragen, und latest release info aktualisieren
	 */
	private synchronized void runUpdateCacheFileOnceThread() {
		if (!isUpdateThreadRunning && !didUpdateCacheFile) {
			isUpdateThreadRunning = true;
			logger.debug("start runUpdateOnceThread");
			new Thread("Update latest release") {
				@Override
				public void run() {
					try {
						writeLatestReleaseFromGithubInCacheFile();
					} finally {
						isUpdateThreadRunning = false;
						didUpdateCacheFile = true; // auch wenn keine verbindung auf true
					}
				}
			}.start();
		}
	}

	@VisibleForTesting
	void writeLatestReleaseFromGithubInCacheFile() {
		logger.debug("start writeLatestRelease");

		GHRelease latestRelease = getLatestReleaseFromGitHub();
		if (latestRelease != null && !latestRelease.isPrerelease()) {
			// wenn kein Prerelease
			Gson gson = new GsonBuilder().setPrettyPrinting()
					.addSerializationExclusionStrategy(new ReleaseExclusionStrategy()).create();
			logger.info("Write latest release = " + latestRelease.getName());
			try (BufferedWriter writer = Files.newBufferedWriter(getReleaseFile())) {
				writer.write(gson.toJson(latestRelease));
			} catch (IOException e) {
				logger.error(e);
			}
		}
	}

	private GHRelease getLatestReleaseFromGitHub() {
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

	@VisibleForTesting
	GHRelease readLatestReleaseFromCacheFile() {
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

	private String latestVersionFromCacheFile() {
		String latestVersionFromCacheFile = null;

		GHRelease readLatestRelease = readLatestReleaseFromCacheFile();
		if (readLatestRelease != null) {
			latestVersionFromCacheFile = readLatestRelease.getName();
			// clean up name
			latestVersionFromCacheFile = StringUtils.stripStart(latestVersionFromCacheFile, "vV");
		}
		return latestVersionFromCacheFile;
	}

	private synchronized boolean checkForNewRelease(XComponentContext context) {
		logger.debug("checkForNewRelease");
		boolean newVersionAvailable = false;
		try {
			// https://www.baeldung.com/java-download-file
			// https://github.com/G00fY2/version-compare
			if (!isUpdateThreadRunning) {
				String versionNummer = ExtensionsHelper.from(context).getVersionNummer();
				String latestVersionFromCacheFile = latestVersionFromCacheFile();
				logger.debug("Instalierte Release = " + versionNummer);
				logger.debug("Letzte GitHub Release = " + latestVersionFromCacheFile);
				if (latestVersionFromCacheFile != null) {
					newVersionAvailable = new Version(versionNummer).isLowerThan(latestVersionFromCacheFile);
					if (newVersionAvailable) {
						logger.debug("Neue Version = " + newVersionAvailable);
						ProcessBox.from().infoText(
								"Neue Version von PétTurnMngr (" + latestVersionFromCacheFile + ") verfügbar.");
					}
				}
			}
		} catch (Exception e) {
			// fehler nur loggen
			logger.error(e);
		}
		return newVersionAvailable;
	}

	GHAsset getDownloadGHAsset() {
		GHAsset otxAsset = null;

		GHRelease latestRelease = getLatestReleaseFromGitHub();
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

	URL getDownloadURL(GHAsset otxAsset) {
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
