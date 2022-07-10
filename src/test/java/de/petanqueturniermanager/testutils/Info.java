package de.petanqueturniermanager.testutils;

// Info.java
// Andrew Davison, ad@fivedots.coe.psu.ac.th, December 2016

/*    * fonts
      * lookup registry modifications
      * configuration paths in office
      * get info about the loaded document
      * services, interfaces, methods info
      * style info
      * fonts
      * document properties
      * installed package info
      * import/export filters
*/
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.sun.star.awt.FontDescriptor;
import com.sun.star.awt.XDevice;
import com.sun.star.awt.XToolkit;
import com.sun.star.beans.NamedValue;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XHierarchicalPropertySet;
import com.sun.star.beans.XPropertyContainer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XContentEnumerationAccess;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNameContainer;
import com.sun.star.deployment.PackageInformationProvider;
import com.sun.star.deployment.XPackageInformationProvider;
import com.sun.star.document.XDocumentProperties;
import com.sun.star.document.XDocumentPropertiesSupplier;
import com.sun.star.document.XTypeDetection;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XTypeProvider;
import com.sun.star.style.XStyleFamiliesSupplier;
import com.sun.star.uno.Exception;
import com.sun.star.uno.Type;
import com.sun.star.uno.XInterface;
import com.sun.star.util.DateTime;
import com.sun.star.util.XChangesBatch;

public class Info {

	public static final String REG_MOD_FNM = "registrymodifications.xcu";

	public static final String NODE_PRODUCT = "/org.openoffice.Setup/Product";
	public static final String NODE_L10N = "/org.openoffice.Setup/L10N";
	public static final String NODE_OFFICE = "/org.openoffice.Setup/Office";
	// used to access Office configuration nodes at runtime

	private static final String[] NODE_PATHS = { NODE_PRODUCT, NODE_L10N };

	private static final String MIME_FNM = "mime.types";
	// mime.types "borrowed" from Python 3.5

	/*
	 * In LibreOffice 4\share\registry\main.xcd:
	 * 
	 * Arguments (in Product): ooName ooXMLFileFormatVersion ooXMLFileFormatName ooSetupVersion ooSetupVersionAboutBox ooSetupVersionAboutBoxSuffix ooVendor ooSetupExtension ooOpenSourceContext
	 * 
	 * In L10N: ooLocale ooSetupSystemLocale ooSetupCurrency DecimalSeparatorAsLocale IgnoreLanguageChange DateAcceptancePatterns
	 * 
	 * In Office: ooSetupInstCompleted InstalledLocales ooSetupConnectionURL MigrationCompleted LastCompatibilityCheckID Factories
	 */

	// filter flag constants
	// from https://wiki.openoffice.org/wiki/Documentation/
	// DevGuide/OfficeDev/Properties_of_a_Filter
	public static final int IMPORT = 0x00000001;
	public static final int EXPORT = 0x00000002;
	public static final int TEMPLATE = 0x00000004;
	public static final int INTERNAL = 0x00000008;

	public static final int TEMPLATEPATH = 0x00000010;
	public static final int OWN = 0x00000020;
	public static final int ALIEN = 0x00000040;

	public static final int DEFAULT = 0x00000100;
	public static final int SUPPORTSSELECTION = 0x00000400;

	public static final int NOTINFILEDIALOG = 0x00001000;
	public static final int NOTINCHOOSER = 0x00002000;

	public static final int READONLY = 0x00010000;
	public static final int THIRDPARTYFILTER = 0x00080000;

	public static final int PREFERRED = 0x10000000;

	// ----------------------- fonts ----------------------------------

	public static FontDescriptor[] getFonts() {
		XToolkit xToolkit = Lo.createInstanceMCF(XToolkit.class, "com.sun.star.awt.Toolkit");
		XDevice device = xToolkit.createScreenCompatibleDevice(0, 0);
		if (device == null) {
			System.out.println("Could not access graphical output device");
			return null;
		} else
			return device.getFontDescriptors();
	} // end of getFonts()

	public static String[] getFontNames() {
		FontDescriptor[] fds = getFonts();
		if (fds == null)
			return null;

		// use a set to exclude duplicate names
		Set<String> namesSet = new HashSet<String>();
		for (int i = 0; i < fds.length; i++)
			namesSet.add(fds[i].Name);

		String[] names = namesSet.toArray(new String[0]);
		Arrays.sort(names);
		return names;
	} // end of getFontNames()

	// ----------------- lookup registry modifications --------------------

	public static String getRegModsPath()
	// return path to "registrymodifications.xcu"
	{
		String userConfigDir = FileIO.urlToPath(Info.getPaths("UserConfig"));
		// System.out.println("\nUser Config: " + userConfigDir);

		try {
			String parentPath = new File(userConfigDir).getParent();
			return parentPath + "//" + REG_MOD_FNM;
		} catch (java.lang.Exception e) {
			System.out.println("Could not parse " + userConfigDir);
			return null;
		}
	} // end of getRegModsPath()

//	public static String getRegItemProp(String item, String prop) {
//		return getRegItemProp(item, null, prop);
//	}

//	public static String getRegItemProp(String item, String node, String prop)
//	// return value from "registrymodifications.xcu"
//	// e.g. "Writer/MailMergeWizard" null, "MailAddress"
//	// e.g. "Logging/Settings", "org.openoffice.logging.sdbc.DriverManager", "LogLevel"
//	/*
//	 * This xpath doesn't deal with all cases in the XCU file, which sometimes has many node levels between the item and the prop. Returns null if no value is found, or it's only an empty string.
//	 */
//	{
//		String fnm = getRegModsPath();
//		// System.out.println(fnm);
//
//		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//		factory.setIgnoringElementContentWhitespace(true);
//		factory.setNamespaceAware(true);
//
//		String value = null;
//		try {
//			DocumentBuilder builder = factory.newDocumentBuilder();
//			Document doc = builder.parse(new File(fnm));
//
//			XPathFactory xpathFactory = XPathFactory.newInstance();
//			XPath xpath = xpathFactory.newXPath();
//			xpath.setNamespaceContext(new UniversalNamespaceResolver(doc));
//
//			XPathExpression expr = null;
//			if (node == null)
//				expr = xpath.compile(
//						"//item[@oor:path='/org.openoffice.Office." + item + "']/prop[@oor:name='" + prop + "']");
//			else
//				expr = xpath.compile("//item[@oor:path='/org.openoffice.Office." + item + "']/node[@oor:name='" + node
//						+ "']/prop[@oor:name='" + prop + "']");
//
//			value = (String) expr.evaluate(doc, XPathConstants.STRING);
//			if ((value == null) || value.equals("")) {
//				System.out.println("Item Property not found");
//				value = null;
//			} else {
//				value = value.trim();
//				if (value.equals("")) {
//					System.out.println("Item Property is white space (?)");
//					value = null;
//				}
//			}
//		} catch (XPathExpressionException e) {
//			System.out.println(e);
//		} catch (ParserConfigurationException e) {
//			System.out.println(e);
//		} catch (SAXException e) {
//			System.out.println(e);
//		} catch (java.io.IOException e) {
//			System.out.println(e);
//		}
//
//		return value;
//	} // end of getRegItemProp()

	// -------------------------- configuration paths --------------------

	public static String getConfig(String nodeStr) {
		for (String nodePath : NODE_PATHS) {
			String info = (String) getConfig(nodePath, nodeStr);
			if (info != null)
				return info;
		}
		System.out.println("No configuration info for " + nodeStr);
		return null;
	} // end of getConfig()

	public static Object getConfig(String nodePath, String nodeStr) {
		XPropertySet props = getConfigProps(nodePath);
		if (props == null)
			return null;
		else
			return Props.getProperty(props, nodeStr);
	} // end of getConfig()

	public static XPropertySet getConfigProps(String nodePath) {
		// create the provider and remember it as a XMultiServiceFactory
		XMultiServiceFactory conProv = Lo.createInstanceMCF(XMultiServiceFactory.class,
				"com.sun.star.configuration.ConfigurationProvider");
		if (conProv == null) {
			System.out.println("Could not create configuration provider");
			return null;
		}

		PropertyValue[] props = Props.makeProps("nodepath", nodePath);
		// specifies the location of the view root in the configuration.
		try {
			return Lo.qi(XPropertySet.class,
					conProv.createInstanceWithArguments("com.sun.star.configuration.ConfigurationAccess", props));
		} catch (Exception ex) {
			System.out.println("Unable to access config properties for\n  \"" + nodePath + "\"");
			return null;
		}
	} // end of getConfigProps()

	public static String getPaths(String setting)
	/*
	 * access LO's predefined paths. There are two different groups of properties. One group stores only a single path and the other group stores two or more paths - separated by a semicolon. See
	 * https://wiki.openoffice.org/w/index.php?title=Documentation/DevGuide/OfficeDev/Path_Settings
	 * 
	 * Some setting values (as listed in the OpenOffice docs for PathSettings): Addin, AutoCorrect, AutoText, Backup, Basic, Bitmap, Config, Dictionary, Favorite, Filter, Gallery, Graphic, Help,
	 * Linguistic, Module, Palette, Plugin, Storage, Temp, Template, UIConfig, UserConfig, UserDictionary (deprecated), Work
	 * 
	 * Replaced by thePathSetting in LibreOffice 4.3
	 */
	{
		XPropertySet propSet = Lo.createInstanceMCF(XPropertySet.class, "com.sun.star.util.PathSettings");
		if (propSet == null) {
			System.out.println("Could not access office settings");
			return null;
		}

		// Props.showProps("Path Settings", propSet); // for debugging
		try {
			return (String) propSet.getPropertyValue(setting);
		} catch (Exception e) {
			System.out.println("Could not find setting for: " + setting);
			return null;
		}
	} // end of getPaths()

	public static String[] getDirs(String setting) {
		String paths = getPaths(setting);
		if (paths == null) {
			System.out.println("Cound not find paths for \"" + setting + "\"");
			return null;
		}

		String[] pathsArr = paths.split(";");
		if (pathsArr == null) {
			System.out.println("Cound not split paths for \"" + setting + "\"");
			return new String[] { paths }; // return string as a 1-element array
		}

		String[] dirs = new String[pathsArr.length];
		for (int i = 0; i < pathsArr.length; i++)
			dirs[i] = FileIO.URI2Path(pathsArr[i]);

		return dirs;
	} // end of getDirs()

	public static String getOfficeDir()
	// returns the file path to the office dir
	// e.g. "C:\Program Files (x86)\LibreOffice 4\"
	{
		String addinDir = getPaths("Addin");
		// e.g. file:///C:/Program%20Files%20(x86)/LibreOffice%204/program/../program/addin

		if (addinDir == null) {
			System.out.println("Cound not find settings information");
			return null;
		}

		String addinPath = FileIO.URI2Path(addinDir);
		// e.g. C:\Program Files (x86)\LibreOffice 4\program\addin
		int idx = addinPath.indexOf("program");
		if (idx == -1) {
			System.out.println("Cound not extract office path");
			return addinPath;
		} else
			return addinPath.substring(0, idx);
	} // end of getOfficeDir()

	public static String getGalleryDir()
	/*
	 * The gallery string contains two directories: the office location in share/gallery/, and the user one in Roaming; return the first
	 */
	{
		String[] galleryDirs = getDirs("Gallery");
		if (galleryDirs == null)
			return null;
		else
			return galleryDirs[0];
	} // end of getGallerDir()

	public static XHierarchicalPropertySet createConfigurationView(String sPath)
	// Create a specified read-only configuration view
	{
		// create the provider and remember it as a XMultiServiceFactory
		XMultiServiceFactory conProv = Lo.createInstanceMCF(XMultiServiceFactory.class,
				"com.sun.star.configuration.ConfigurationProvider");
		if (conProv == null) {
			System.out.println("Could not create configuration provider");
			return null;
		}

		PropertyValue[] props = Props.makeProps("nodepath", sPath);
		// specifies the location of the view root in the configuration.
		try {
			XInterface root = (XInterface) conProv
					.createInstanceWithArguments("com.sun.star.configuration.ConfigurationAccess", props);
			showServices("ConfigurationAccess", root);
			return Lo.qi(XHierarchicalPropertySet.class, root);
		} catch (Exception ex) { // System.out.println("Unable to access Office info on " + sPath);
			return null;
		}
	} // emd createConfigurationView()

// =================== update configuration settings ================

	public static boolean setConfig(String nodePath, String nodeStr, Object val) {
		XPropertySet props = setConfigProps(nodePath);
		if (props == null)
			return false;
		else {
			Props.setProperty(props, nodeStr, val);
			XChangesBatch secureChange = Lo.qi(XChangesBatch.class, props);
			try {
				secureChange.commitChanges();
				return true;
			} catch (Exception ex) {
				System.out.println("Unable to commit config update for\n  \"" + nodePath + "\"");
				return false;
			}
		}
	} // end of setConfig()

	public static XPropertySet setConfigProps(String nodePath) {
		// create the provider and remember it as a XMultiServiceFactory
		XMultiServiceFactory conProv = Lo.createInstanceMCF(XMultiServiceFactory.class,
				"com.sun.star.configuration.ConfigurationProvider");
		if (conProv == null) {
			System.out.println("Could not create configuration provider");
			return null;
		}

		PropertyValue[] props = Props.makeProps("nodepath", nodePath);
		// specifies the location of the view root in the configuration.

		try {
			return Lo.qi(XPropertySet.class,
					conProv.createInstanceWithArguments("com.sun.star.configuration.ConfigurationUpdateAccess", props));
		} catch (Exception ex) {
			System.out.println("Unable to access config update properties for\n  \"" + nodePath + "\"");
			return null;
		}
	} // end of setConfigProps()

// =================== getting info about a document ====================

	public static String getName(String fnm)
	// extract the file's name from the supplied string
	{
		int dotPos = fnm.lastIndexOf('.');
		if (dotPos == -1) {
			System.out.println("No extension found for " + fnm);
			return fnm;
		} else if (dotPos == 0) {
			System.out.println("No filename text found for " + fnm);
			return null;
		}

		fnm = fnm.substring(0, dotPos);

		// strip out directories path
		// doesn't handle URLs with parameters or anchors
		int slashIndex = fnm.lastIndexOf('/');
		if (slashIndex > 0)
			fnm = fnm.substring(slashIndex + 1, fnm.length());

		return fnm;
	} // end of getName()

	public static String getExt(String fnm)
	// return extenson without the "."
	{
		int dotPos = fnm.lastIndexOf('.');
		if (dotPos == -1) {
			System.out.println("No extension found for " + fnm);
			return null;
		} else if (dotPos == fnm.length() - 1) {
			System.out.println("No extension text found for " + fnm);
			return null;
		} else
			return fnm.substring(dotPos + 1).toLowerCase();
	} // end of getExt()

	public static String getUniqueFnm(String fnm)
	/*
	 * if a file called fnm already exists, then a number is added to the name so the filename is unique
	 */
	{
		String fName = getName(fnm);
		String ext = getExt(fnm);

		File f = new File(fnm);
		int i = 1;
		while (f.exists()) {
			fnm = fName + i + ext;
			f = new File(fnm);
			i++;
		}
		return fnm;
	} // end of getUniqueFnm()

	public static String getDocType(String fnm)
	/*
	 * use a type detector to determine the type of the file. The commented-out code is a simple, fast detection, while the approach used looks at the file's metadata
	 */
	{
		XTypeDetection xTypeDetect = Lo.createInstanceMCF(XTypeDetection.class, "com.sun.star.document.TypeDetection");
		if (xTypeDetect == null) {
			System.out.println("No type detector reference");
			return null;
		}

		if (!FileIO.isOpenable(fnm))
			return null;
		String urlStr = FileIO.fnmToURL(fnm);
		if (urlStr == null)
			return null;
		// return xTypeDetect.queryTypeByURL(urlStr); // 'flat' type detection
		// uses the URL and some configuration data only

		// or use queryTypeByDescriptor() and a MediaDescriptor
		PropertyValue[][] mediaDescr = new PropertyValue[1][1];
		mediaDescr[0][0] = new PropertyValue();
		mediaDescr[0][0].Name = "URL";
		mediaDescr[0][0].Value = urlStr;

		return xTypeDetect.queryTypeByDescriptor(mediaDescr, true);
	} // end of getDocType()

	/*
	 * public static XTypeDetection getTypeDetector() { if ((xcc == null) || (mcFactory == null)) { System.out.println("No office connection found"); return null; }
	 * 
	 * // get the file type detection interface //try { return createInstanceMCF(XTypeDetection.class, "com.sun.star.document.TypeDetection");
	 * 
	 * //Object typeDetectObj = mcFactory.createInstanceWithContext( // "com.sun.star.document.TypeDetection", xcc); //return (XTypeDetection) Lo.qi(XTypeDetection.class, typeDetectObj); // } // catch
	 * (Exception e) { // System.out.println("Could not create a file type detector"); // return null; // } } // end of getTypeDetector()
	 */

	public static int reportDocType(Object doc) {
		int docType = Lo.UNKNOWN;
		if (isDocType(doc, Lo.WRITER_SERVICE)) {
			System.out.println("A Writer document");
			docType = Lo.WRITER;
		} else if (isDocType(doc, Lo.IMPRESS_SERVICE)) {
			System.out.println("An Impress document");
			docType = Lo.IMPRESS;
		} else if (isDocType(doc, Lo.DRAW_SERVICE)) {
			System.out.println("A Draw document");
			docType = Lo.DRAW;
		} else if (isDocType(doc, Lo.CALC_SERVICE)) {
			System.out.println("A Calc spreadsheet");
			docType = Lo.CALC;
		} else if (isDocType(doc, Lo.BASE_SERVICE)) {
			System.out.println("A Base document");
			docType = Lo.BASE;
		} else if (isDocType(doc, Lo.MATH_SERVICE)) {
			System.out.println("A Math document");
			docType = Lo.MATH;
		} else
			System.out.println("Unknown document");

		return docType;
	} // end of reportDocType()

	public static String docTypeString(Object doc) {
		if (isDocType(doc, Lo.WRITER_SERVICE)) {
			System.out.println("A Writer document");
			return Lo.WRITER_SERVICE;
		} else if (isDocType(doc, Lo.IMPRESS_SERVICE)) {
			System.out.println("An Impress document");
			return Lo.IMPRESS_SERVICE;
		} else if (isDocType(doc, Lo.DRAW_SERVICE)) {
			System.out.println("A Draw document");
			return Lo.DRAW_SERVICE;
		} else if (isDocType(doc, Lo.CALC_SERVICE)) {
			System.out.println("A Calc spreadsheet");
			return Lo.CALC_SERVICE;
		} else if (isDocType(doc, Lo.BASE_SERVICE)) {
			System.out.println("A Base document");
			return Lo.BASE_SERVICE;
		} else if (isDocType(doc, Lo.MATH_SERVICE)) {
			System.out.println("A Math document");
			return Lo.MATH_SERVICE;
		} else {
			System.out.println("Unknown document");
			return Lo.UNKNOWN_SERVICE;
		}
	} // end of docTypeString()

	public static boolean isDocType(Object obj, String docType)
	// was XComponent
	{
		XServiceInfo si = Lo.qi(XServiceInfo.class, obj);
		return si.supportsService(docType);
	}

	public static String getImplementationName(Object obj) {
		XServiceInfo si = Lo.qi(XServiceInfo.class, obj);
		if (si == null) {
			System.out.println("Could not get service information");
			return null;
		} else
			return si.getImplementationName();
	} // end of getImplementationName()

//	public static String getMIMEType(String fnm)
//	// also see FileIO.getMimeType() for use of zipped mimetype;
//	// also see Office API-based Images.getMimeType() for images only
//	{
//		File f = new File(fnm);
//		try {
//			MimetypesFileTypeMap mftMap = new MimetypesFileTypeMap(FileIO.getUtilsFolder() + MIME_FNM);
//			// MimetypesFileTypeMap is a Java API
//			// System.out.println("Mime Type for " + fnm + ": \"" +
//			// mftMap.getContentType(f) + "\"");
//			return mftMap.getContentType(f);
//		} catch (java.lang.Exception e) {
//			System.out.println("Could not find " + MIME_FNM);
//			return "application/octet-stream"; // better than nothing
//		}
//	} // end of getMIMEType()

	public static int mimeDocType(String mimeType) {
		if (mimeType == null)
			return Lo.UNKNOWN;

		if (mimeType.contains("vnd.oasis.opendocument.text"))
			return Lo.WRITER;
		else if (mimeType.contains("vnd.oasis.opendocument.base"))
			return Lo.BASE;
		else if (mimeType.contains("vnd.oasis.opendocument.spreadsheet"))
			return Lo.CALC;
		else if (mimeType.contains("vnd.oasis.opendocument.graphics")
				|| mimeType.contains("vnd.oasis.opendocument.image")
				|| mimeType.contains("vnd.oasis.opendocument.chart"))
			return Lo.DRAW;
		else if (mimeType.contains("vnd.oasis.opendocument.presentation"))
			return Lo.IMPRESS;
		else if (mimeType.contains("vnd.oasis.opendocument.formula"))
			return Lo.MATH;
		else
			return Lo.UNKNOWN;
	} // end of mimeDocType()

	public static boolean isImageMime(String mimeType) {
		if (mimeType.startsWith("image/"))
			return true;
		if (mimeType.startsWith("application/x-openoffice-bitmap"))
			return true;
		return false;
	} // end of isImageMime()

	// ------------------------ services, interfaces, methods info ----------------------

	public static String[] getServiceNames() {
		XMultiComponentFactory mcFactory = Lo.getComponentFactory();
		if (mcFactory == null)
			return null;
		else {
			String[] serviceNames = mcFactory.getAvailableServiceNames();
			Arrays.sort(serviceNames);
			return serviceNames;
		}
	} // end of getServiceNames()

	public static String[] getServiceNames(String serviceName) {
		ArrayList<String> names = new ArrayList<String>();
		try {
			XContentEnumerationAccess enumAccess = Lo.qi(XContentEnumerationAccess.class, Lo.getComponentFactory());
			XEnumeration xEnum = enumAccess.createContentEnumeration(serviceName);
			while (xEnum.hasMoreElements()) {
				Object obj = xEnum.nextElement();
				XServiceInfo si = Lo.qi(XServiceInfo.class, obj);
				names.add(si.getImplementationName());
			}
		} catch (Exception e) {
			System.out.println("Could not collect service names for: " + serviceName);
			return null;
		}

		if (names.size() == 0) {
			System.out.println("No service names found for: " + serviceName);
			return null;
		}

		String[] serviceNames = names.toArray(new String[names.size()]);
		Arrays.sort(serviceNames);
		return serviceNames;
	} // end of getServiceNames()

	public static String[] getServices(Object obj) {
		XServiceInfo si = Lo.qi(XServiceInfo.class, obj);
		if (si == null) {
			System.out.println("No XServiceInfo interface found");
			return null;
		}
		String[] serviceNames = si.getSupportedServiceNames();
		Arrays.sort(serviceNames);
		return serviceNames;
	} // end of getServices()

	public static void showServices(String objName, Object obj) {
		String[] services = getServices(obj);
		if (services == null) {
			System.out.println("No supported services found for " + objName);
			return;
		}
		System.out.println(objName + " Supported Services (" + services.length + ")");
		for (String service : services)
			System.out.println("  \"" + service + "\"");
	} // end of showServices()

	public static boolean supportService(Object obj, String serviceName) {
		XServiceInfo si = Lo.qi(XServiceInfo.class, obj);
		if (si == null) {
			System.out.println("No service info found");
			return false;
		} else
			return si.supportsService(serviceName);
	} // end of supportService()

	public static String[] getAvailableServices(Object obj) {
		XMultiServiceFactory msf = Lo.qi(XMultiServiceFactory.class, obj);
		String[] serviceNames = msf.getAvailableServiceNames();
		Arrays.sort(serviceNames);
		return serviceNames;
	} // end of getAvailableServices()

	public static Type[] getInterfaceTypes(Object target) {
		Type[] types = null;
		XTypeProvider typeProvider = Lo.qi(XTypeProvider.class, target);
		if (typeProvider != null)
			types = typeProvider.getTypes();
		return types;
	} // end of getInterfaceTypes()

	public static String[] getInterfaces(Object target) {
		XTypeProvider typeProvider = Lo.qi(XTypeProvider.class, target);
		if (typeProvider == null)
			return null;
		else
			return getInterfaces(typeProvider);
	} // end of getInterfaces()

	public static String[] getInterfaces(XTypeProvider typeProvider) {
		Type[] types = typeProvider.getTypes();

		// use a set to exclude duplicate names
		Set<String> namesSet = new HashSet<String>();
		for (int i = 0; i < types.length; i++)
			namesSet.add(types[i].getTypeName());

		String[] typeNames = namesSet.toArray(new String[0]);
		Arrays.sort(typeNames);
		return typeNames;
	} // end of getInterfaces()

	public static void showInterfaces(String objName, Object obj) {
		String[] intfs = getInterfaces(obj);
		if (intfs == null) {
			System.out.println("No interfaces found for " + objName);
			return;
		}
		System.out.println(objName + " Interfaces (" + intfs.length + ")");
		for (String intf : intfs)
			System.out.println("  " + intf);
	} // end of showInterfaces()

	public static String[] getMethods(String interfaceName) {
		try {
			return getMethods(new Type(interfaceName));
		} catch (com.sun.star.uno.RuntimeException e) {
			System.out.println("Could not find the interface name: " + interfaceName);
			return null;
		}
	} // end of getMethods()

	public static String[] getMethods(Type intf) {
		Method[] methods = intf.getZClass().getMethods(); // methods from Class class

		String[] methodNames = new String[methods.length];
		for (int i = 0; i < methods.length; i++)
			methodNames[i] = methods[i].getName();
		Arrays.sort(methodNames);
		return methodNames;
	} // end of getMethods()

	public static void showMethods(String interfaceName) {
		String[] methods = getMethods(interfaceName);
		if (methods == null)
			return;
		System.out.println(interfaceName + " Methods (" + methods.length + ")");
		for (String method : methods)
			System.out.println("  " + method);
	} // end of showMethods()

	// -------------------------- style info --------------------------

	public static String[] getStyleFamilyNames(Object doc)
	// get the names of all the style families
	// was XComponent
	{
		XStyleFamiliesSupplier xSupplier = Lo.qi(XStyleFamiliesSupplier.class, doc);
		XNameAccess nameAcc = xSupplier.getStyleFamilies();
		String[] names = nameAcc.getElementNames();
		Arrays.sort(names);
		return names;
	} // end of getStyleFamilyNames()

	public static XNameContainer getStyleContainer(Object doc, String familyStyleName)
	// get the container for a specified style family name
	{
		try {
			XStyleFamiliesSupplier xSupplier = Lo.qi(XStyleFamiliesSupplier.class, doc);
			XNameAccess nameAcc = xSupplier.getStyleFamilies();
			return Lo.qi(XNameContainer.class, nameAcc.getByName(familyStyleName));
		} catch (Exception e) {
			System.out.println("Could not access the family style: " + familyStyleName);
			return null;
		}
	} // end of getStyleContainer()

	public static String[] getStyleNames(Object doc, String familyStyleName)
	// get all the style names for a style family
	{
		XNameContainer styleContainer = getStyleContainer(doc, familyStyleName);
		if (styleContainer == null)
			return null;
		else {
			String[] names = styleContainer.getElementNames();
			Arrays.sort(names);
			return names;
		}
	} // end of getStyleNames()

	public static XPropertySet getPageStyleProps(Object doc) {
		return getStyleProps(doc, "PageStyles", "Standard");
	}

	public static XPropertySet getParagraphStyleProps(Object doc) {
		return getStyleProps(doc, "ParagraphStyles", "Standard");
	}

	public static XPropertySet getStyleProps(Object doc, String familyStyleName, String propSetNm)
	/* get the named property set from the given style family */
	{
		XNameContainer styleContainer = getStyleContainer(doc, familyStyleName);
		// container is a collection of named property sets
		if (styleContainer == null)
			return null;
		else {
			XPropertySet nameProps = null;
			try {
				nameProps = Lo.qi(XPropertySet.class, styleContainer.getByName(propSetNm));
			} catch (Exception e) {
				System.out.println("Could not access style: " + e);
			}
			return nameProps;
		}
	} // end of getStyleProps()

	// ----------------------------- document properties ----------------------

	public static void printDocProperties(Object doc) {
		// XDocumentInfo removed from LibreOffice; deprecated in OpenOffice
		// XDocumentInfoSupplier xdis = Lo.qi(XDocumentInfoSupplier.class, doc);
		// XDocumentInfo xdi = xdis.getDocumentInfo();
		// Props.showObjProps("Document Info", xdi);

		XDocumentPropertiesSupplier docPropsSupp = Lo.qi(XDocumentPropertiesSupplier.class, doc);
		XDocumentProperties dps = docPropsSupp.getDocumentProperties();

		// Props.showObjProps("Doc Info", dps);
		printDocProps(dps);

		XPropertyContainer udProps = dps.getUserDefinedProperties();
		Props.showObjProps("UserDefined Info", udProps);
	} // end of printDocProperties()

	public static void printDocProps(XDocumentProperties dps) {
		System.out.println("Document Properties Info");

		System.out.println("  Author: " + dps.getAuthor()); // not dps.Author
		System.out.println("  Title: " + dps.getTitle());
		System.out.println("  Subject: " + dps.getSubject());
		System.out.println("  Description: " + dps.getDescription());
		System.out.println("  Generator: " + dps.getGenerator());

		// System.out.println(" MIME type: " + dps.getMIMEType()); // not supported?

		String[] keys = dps.getKeywords();
		System.out.println("  Keywords: ");
		for (String keyword : keys)
			System.out.println("  " + keyword);

		System.out.println("  Modified by: " + dps.getModifiedBy());
		System.out.println("  Printed by: " + dps.getPrintedBy());

		System.out.println("  Template Name: " + dps.getTemplateName());
		System.out.println("  Template URL: " + dps.getTemplateURL());
		System.out.println("  Autoload URL: " + dps.getAutoloadURL());
		System.out.println("  Default Target: " + dps.getDefaultTarget());

		com.sun.star.lang.Locale l = dps.getLanguage();
		System.out.println("  Locale: " + l.Language + "; " + l.Country + "; " + l.Variant);

		System.out.println("  Modification Date: " + strDateTime(dps.getModificationDate()));
		System.out.println("  Creation Date: " + strDateTime(dps.getCreationDate()));
		System.out.println("  Print Date: " + strDateTime(dps.getPrintDate()));
		System.out.println("  Template Date: " + strDateTime(dps.getTemplateDate()));

		NamedValue[] docStats = dps.getDocumentStatistics();
		System.out.println("  Document statistics:");
		for (NamedValue nv : docStats)
			System.out.println("  " + nv.Name + " = " + nv.Value);

		try {
			System.out.println("  Autoload Secs: " + dps.getAutoloadSecs());
		} catch (IllegalArgumentException e) {
			System.out.println("  Autoload Secs: " + e);
		}

		try {
			System.out.println("  Editing Cycles: " + dps.getEditingCycles());
		} catch (IllegalArgumentException e) {
			System.out.println("  Editing Cycles: " + e);
		}

		try {
			System.out.println("  Editing Duration: " + dps.getEditingDuration());
		} catch (IllegalArgumentException e) {
			System.out.println("  Editing Duration: " + e);
		}

		System.out.println();
	} // end of printDocProps()

	public static String strDateTime(DateTime dt) {
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm");
		java.util.Calendar cal = new java.util.GregorianCalendar(dt.Year, dt.Month - 1, dt.Day, dt.Hours, dt.Minutes);
		// Office DateTime month is 1-12; Java is 0-11 !
		return sdf.format(cal.getTime());
	}

	// Set document properties for subject, title, author
	public static void setDocProps(Object doc, String subject, String title, String author) {
		XDocumentPropertiesSupplier dpSupplier = Lo.qi(XDocumentPropertiesSupplier.class, doc);
		XDocumentProperties docProps = dpSupplier.getDocumentProperties();
		docProps.setSubject(subject);
		docProps.setTitle(title);
		docProps.setAuthor(author);
	} // end setDocProps()

	public static XPropertyContainer getUserDefinedProps(Object doc) {
		XDocumentPropertiesSupplier docPropsSupp = Lo.qi(XDocumentPropertiesSupplier.class, doc);
		XDocumentProperties dps = docPropsSupp.getDocumentProperties();

		return dps.getUserDefinedProperties();
	} // end of getUserDefinedProps()

	// ----------- installed package info -----------------

	public static XPackageInformationProvider getPip() {
		return PackageInformationProvider.get(Lo.getContext());
	}

	public static void listExtensions()
	// shared extensions are in <OFFICE>\share\extensions;
	// user extensions are below C:\Users\<USER_NAME>\AppData\Roaming\LibreOffice\
	{
		XPackageInformationProvider pip = getPip();
		if (pip == null)
			System.out.println("No package info provider found");
		else {
			String[][] extsTable = pip.getExtensionList();
			// Lo.printTable("Extensions", extsTable);
			System.out.println("\nExtensions:");
			String serviceName;
			for (int i = 0; i < extsTable.length; i++) {
				System.out.println((i + 1) + ". ID: " + extsTable[i][0]);
				System.out.println("   Version: " + extsTable[i][1]);
				System.out.println("   Loc: " + pip.getPackageLocation(extsTable[i][0]));
				System.out.println();
			}
		}
	} // end of listExtensions()

	public static String[] getExtensionInfo(String id) {
		XPackageInformationProvider pip = getPip();
		if (pip == null) {
			System.out.println("No package info provider found");
			return null;
		} else {
			String[][] extsTable = pip.getExtensionList();
			Lo.printTable("Extensions", extsTable);
			String serviceName;
			for (int i = 0; i < extsTable.length; i++) {
				if (extsTable[i][0].equals(id))
					return extsTable[i];
			}
			System.out.println("Extension " + id + " is not found");
			return null;
		}
	} // end of getExtensionInfo()

	public static String getExtensionLoc(String id) {
		XPackageInformationProvider pip = getPip();
		if (pip == null) {
			System.out.println("No package info provider found");
			return null;
		} else
			return pip.getPackageLocation(id);
	} // end of getExtensionLoc()

	// -------------------- import/export filters ------------

	public static String[] getFilterNames() {
		XNameAccess na = Lo.createInstanceMCF(XNameAccess.class, "com.sun.star.document.FilterFactory");
		if (na == null) {
			System.out.println("No Filter factory found");
			return null;
		} else
			return na.getElementNames();
	} // end of getFilterNames()

	public static PropertyValue[] getFilterProps(String filterNm) {
		XNameAccess na = Lo.createInstanceMCF(XNameAccess.class, "com.sun.star.document.FilterFactory");
		if (na == null) {
			System.out.println("No Filter factory found");
			return null;
		} else {
			try {
				return (PropertyValue[]) na.getByName(filterNm);
			} catch (Exception e) {
				System.out.println("Could not find filter for " + filterNm);
				return null;
			}
		}
	} // end of getFilterProps()

	public static boolean isImport(int filterFlags) {
		return ((filterFlags & IMPORT) == IMPORT);
	}

	public static boolean isExport(int filterFlags) {
		return ((filterFlags & EXPORT) == EXPORT);
	}

	public static boolean isTemplate(int filterFlags) {
		return ((filterFlags & TEMPLATE) == TEMPLATE);
	}

	public static boolean isInternal(int filterFlags) {
		return ((filterFlags & INTERNAL) == INTERNAL);
	}

	public static boolean isTemplatePath(int filterFlags) {
		return ((filterFlags & TEMPLATEPATH) == TEMPLATEPATH);
	}

	public static boolean isOwn(int filterFlags) {
		return ((filterFlags & OWN) == OWN);
	}

	public static boolean isAlien(int filterFlags) {
		return ((filterFlags & ALIEN) == ALIEN);
	}

	public static boolean isDefault(int filterFlags) {
		return ((filterFlags & DEFAULT) == DEFAULT);
	}

	public static boolean isSupportSelection(int filterFlags) {
		return ((filterFlags & SUPPORTSSELECTION) == SUPPORTSSELECTION);
	}

	public static boolean isNotInFileDialog(int filterFlags) {
		return ((filterFlags & NOTINFILEDIALOG) == NOTINFILEDIALOG);
	}

	public static boolean isNotInChooser(int filterFlags) {
		return ((filterFlags & NOTINCHOOSER) == NOTINCHOOSER);
	}

	public static boolean isReadOnly(int filterFlags) {
		return ((filterFlags & READONLY) == READONLY);
	}

	public static boolean isThirdPartyFilter(int filterFlags) {
		return ((filterFlags & THIRDPARTYFILTER) == THIRDPARTYFILTER);
	}

	public static boolean isPreferred(int filterFlags) {
		return ((filterFlags & PREFERRED) == PREFERRED);
	}

} // end of Info class
