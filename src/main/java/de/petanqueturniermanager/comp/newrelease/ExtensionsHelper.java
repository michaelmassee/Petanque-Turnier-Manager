/**
 * Erstellung 29.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.deployment.PackageInformationProvider;
import com.sun.star.deployment.XPackageInformationProvider;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.sheet.WeakRefHelper;

/**
 * @author Michael Massee
 *
 */
public class ExtensionsHelper {

	private static final Logger logger = LogManager.getLogger(ExtensionsHelper.class);

	public static final String EXTENSION_ID = "de.petanqueturniermanager";

	private final WeakRefHelper<XComponentContext> xComponentContext;

	/**
	 * @param iSheet
	 */
	private ExtensionsHelper(XComponentContext xComponentContext) {
		this.xComponentContext = new WeakRefHelper<>(checkNotNull(xComponentContext));
	}

	public static final ExtensionsHelper from(XComponentContext xComponentContext) {
		return new ExtensionsHelper(xComponentContext);
	}

	private XPackageInformationProvider getXPackageInformationProvider() {
		return PackageInformationProvider.get(xComponentContext.get());
	}

	public ExtensionsHelper listExtensions() {
		XPackageInformationProvider packageInformationProvider = getXPackageInformationProvider();
		String[][] extensionList = packageInformationProvider.getExtensionList();
		logger.info(extensionList.length);
		return this;
	}

	public String getVersionNummer() {
		String verNr = null;
		XPackageInformationProvider packageInformationProvider = getXPackageInformationProvider();
		String[][] extensionList = packageInformationProvider.getExtensionList();
		String[] pluginInfo = Arrays.asList(extensionList).stream()
				.filter(extension -> extension[0].equals(EXTENSION_ID)).findFirst().orElse(null);
		if (pluginInfo != null && pluginInfo.length > 0) {
			verNr = pluginInfo[1];
		}
		return verNr;
	}

	/**
	 * @return file//......
	 */

	public String getImageUrlDir() {
		return StringUtils.appendIfMissing(getXPackageInformationProvider().getPackageLocation(EXTENSION_ID), "/")
				+ "images/";
	}

}
