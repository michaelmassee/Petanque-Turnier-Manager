package de.petanqueturniermanager.addins;

import com.sun.star.lib.uno.helper.WeakBase;

/**
 * Erstellung 23.05.2022 / Michael Massee http://biochemfusion.com/doc/Calc_addin_howto.html<br>
 * https://wiki.openoffice.org/wiki/Calc/Add-In/Project_Type#Generated_Code<br>
 * https://wiki.openoffice.org/wiki/Documentation/DevGuide/Spreadsheets/Spreadsheet_Add-Ins<br>
 * https://github.com/hernad/libreoffice-sdk-examples/blob/4b767eac97f79e3bdb5c16e461891d89ab991470/examples/DevelopersGuide/Spreadsheet/ExampleAddIn.java
 */

public abstract class AbstractAddInImpl extends WeakBase
		implements com.sun.star.lang.XLocalizable,
				   com.sun.star.lang.XServiceInfo,
				   com.sun.star.sheet.XAddIn,
				   com.sun.star.sheet.XCompatibilityNames {
	private com.sun.star.lang.Locale locale = new com.sun.star.lang.Locale();

	// -------- XLocalizable methods ------------
	@Override
	public void setLocale(com.sun.star.lang.Locale l) {
		locale = l;
	}

	@Override
	public com.sun.star.lang.Locale getLocale() {
		return locale;
	}

	// -------- XAddIn methods ------------
	// Standard-Implementierung: Leere Strings zurückgeben
	// LibreOffice macht das Mapping komplett aus der XCU-Datei
	@Override
	public String getProgrammaticFuntionName(String aDisplayName) {
		return "";
	}

	@Override
	public String getDisplayFunctionName(String aProgrammaticName) {
		return "";
	}

	@Override
	public String getFunctionDescription(String aProgrammaticName) {
		// Return empty - descriptions come from XCU file
		return "";
	}

	@Override
	public String getDisplayArgumentName(String aProgrammaticFunctionName, int nArgument) {
		// Return empty - parameter names come from XCU file
		return "";
	}

	@Override
	public String getArgumentDescription(String aProgrammaticFunctionName, int nArgument) {
		// Return empty - descriptions come from XCU file
		return "";
	}

	@Override
	public String getProgrammaticCategoryName(String aProgrammaticFunctionName) {
		// Return empty - categories come from XCU file
		return "";
	}

	@Override
	public String getDisplayCategoryName(String aProgrammaticFunctionName) {
		// Return empty - categories come from XCU file
		return "";
	}

	@Override
	public String getImplementationName() {
		return getImplName();
	}

	// -------- XServiceInfo methods ------------

	@Override
	public boolean supportsService(String sService) {
		for (int i = 0; i < getServiceNames().length; i++) {
			if (sService.equals(getServiceNames()[i])) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String[] getSupportedServiceNames() {
		return getServiceNames();
	}

	// -------- XCompatibilityNames methods ------------
	@Override
	public com.sun.star.sheet.LocalizedName[] getCompatibilityNames(String aProgrammaticName) {
		// Return empty array - compatibility names come from XCU file
		return new com.sun.star.sheet.LocalizedName[0];
	}

	abstract String getImplName();

	abstract String[] getServiceNames();

}
