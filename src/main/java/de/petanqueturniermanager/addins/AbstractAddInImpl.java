package de.petanqueturniermanager.addins;

import com.sun.star.lib.uno.helper.WeakBase;

/**
 * Erstellung 23.05.2022 / Michael Massee
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

}
