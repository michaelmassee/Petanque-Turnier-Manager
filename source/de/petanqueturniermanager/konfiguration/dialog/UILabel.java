/**
 * Erstellung 11.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.konfiguration.dialog;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.container.XNameContainer;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.Exception;

/**
 * @author Michael Massee
 *
 */
public class UILabel extends UIElement<UILabel> {
	private static final Logger logger = LogManager.getLogger(UILabel.class);

	final XNameContainer xNameCont;

	private UILabel(XNameContainer xNameCont, XMultiServiceFactory xMultiServiceFactory) throws Exception {
		super(xMultiServiceFactory);
		this.xNameCont = checkNotNull(xNameCont);
	}

	public static final UILabel from(XNameContainer xNameCont, XMultiServiceFactory xMultiServiceFactory) {
		try {
			return new UILabel(xNameCont, xMultiServiceFactory);
		} catch (Exception e) {
			logger.error(e);
		}
		return null;
	}

	@Override
	String getModelClassName() {
		return "com.sun.star.awt.UnoControlFixedTextModel";
	}

}
