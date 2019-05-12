/**
 * Erstellung 11.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.konfiguration.dialog;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XFixedText;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

/**
 * @author Michael Massee
 *
 */
public class UILabel extends UIElement<UILabel, XFixedText> {
	private static final Logger logger = LogManager.getLogger(UILabel.class);
	private final String PROP_LABEL = "Label";
	private final String PROP_ALIGN = "Align";

	private UILabel(Object dialogModel) throws Exception {
		super(dialogModel);

	}

	public static final UILabel from(Object dialogModel) {
		try {
			return new UILabel(dialogModel);
		} catch (Exception e) {
			logger.error(e);
		}
		return null;
	}

	public final UILabel label(String label) {
		StringUtils.isEmpty(label);
		setProperty(PROP_LABEL, label);
		return this;
	}

	/**
	 * specifies the horiztonal alignment of the text in the control.<br>
	 * 0: left<br>
	 * 1: center<br>
	 * 2: right<br>
	 */

	public final UILabel align(int alignment) {
		setProperty(PROP_ALIGN, Short.valueOf((short) alignment));
		return this;
	}

	@Override
	String getModelClassName() {
		return "com.sun.star.awt.UnoControlFixedTextModel";
	}

	@Override
	public XFixedText doInsert(XControlContainer xControlCont) {
		super.doInsert(xControlCont);
		Object propComponent = getxControlCont().getControl(getFieldname());
		XFixedText xFixedText = UnoRuntime.queryInterface(XFixedText.class, propComponent);
		return xFixedText;
	}

}
