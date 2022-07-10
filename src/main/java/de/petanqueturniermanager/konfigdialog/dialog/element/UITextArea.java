/**
 * Erstellung 11.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog.dialog.element;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XTextComponent;
import com.sun.star.uno.Exception;

import de.petanqueturniermanager.helper.Lo;

/**
 * @author Michael Massee
 *
 */
public class UITextArea extends UIElement<UITextArea, XTextComponent> {
	private static final Logger logger = LogManager.getLogger(UITextArea.class);

	private final String PROP_MULTILINE = "MultiLine";
	private final String PROP_HSCROLL = "HScroll";
	private final String PROP_VSCROLL = "VScroll";
	private final String PROP_TEXT = "Text";

	private UITextArea(Object dialogModel) throws Exception {
		super(dialogModel);
	}

	public static final UITextArea from(Object dialogModel) {
		try {
			return new UITextArea(dialogModel);
		} catch (Exception e) {
			logger.error(e);
		}
		return null;
	}

	@Override
	String getModelClassName() {
		return "com.sun.star.awt.UnoControlEditModel";
	}

	@Override
	public XTextComponent doInsert(XControlContainer xControlCont) {
		super.doInsert(xControlCont);
		Object propTextComponent = getxControlCont().getControl(getFieldname());
		XTextComponent xTextComponent = Lo.qi(XTextComponent.class, propTextComponent);
		return xTextComponent;
	}

	public final UITextArea multiLine(boolean multiLine) {
		setProperty(PROP_MULTILINE, Boolean.valueOf(multiLine));
		return this;
	}

	public final UITextArea hScroll(boolean hScroll) {
		setProperty(PROP_HSCROLL, Boolean.valueOf(hScroll));
		return this;
	}

	public final UITextArea vScroll(boolean vScroll) {
		setProperty(PROP_VSCROLL, Boolean.valueOf(vScroll));
		return this;
	}

	public final UITextArea Text(String text) {
		setProperty(PROP_TEXT, text);
		return this;
	}
}
