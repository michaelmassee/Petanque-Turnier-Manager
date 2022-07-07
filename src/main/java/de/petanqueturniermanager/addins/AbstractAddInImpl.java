package de.petanqueturniermanager.addins;

import com.sun.star.lib.uno.helper.WeakBase;

/**
 * Erstellung 23.05.2022 / Michael Massee http://biochemfusion.com/doc/Calc_addin_howto.html<br>
 * https://wiki.openoffice.org/wiki/Calc/Add-In/Project_Type#Generated_Code<br>
 * https://wiki.openoffice.org/wiki/Documentation/DevGuide/Spreadsheets/Spreadsheet_Add-Ins<br>
 * https://github.com/hernad/libreoffice-sdk-examples/blob/4b767eac97f79e3bdb5c16e461891d89ab991470/examples/DevelopersGuide/Spreadsheet/ExampleAddIn.java
 */

public abstract class AbstractAddInImpl extends WeakBase
		implements com.sun.star.lang.XLocalizable, com.sun.star.lang.XServiceInfo {
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

	abstract String getImplName();

	abstract String[] getServiceNames();

}
