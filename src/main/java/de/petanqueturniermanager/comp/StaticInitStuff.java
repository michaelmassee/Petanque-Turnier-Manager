/**
 * Erstellung 12.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.newrelease.NewReleaseChecker;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;

/**
 * @author Michael Massee
 *
 */
public class StaticInitStuff {

	/**
	 * der erste Konstruktur macht Init
	 *
	 * @param context
	 */

	public static final void init(XComponentContext context) {
		ProcessBox.init(context); // der muss zuerst
		TerminateListener.addThisListenerOnce(context);
		new NewReleaseChecker().checkForUpdate(context);
	}

}
