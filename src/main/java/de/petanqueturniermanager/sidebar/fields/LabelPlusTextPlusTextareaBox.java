/**
 * Erstellung 19.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.fields;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;

/**
 * Label mit Text + Btn für TextAreaInput
 *
 * @author Michael Massee
 *
 */
public class LabelPlusTextPlusTextareaBox extends BaseLabelPlusTextPlusBtn<LabelPlusTextPlusTextareaBox> {

	private static final Logger logger = LogManager.getLogger(LabelPlusTextPlusTextareaBox.class);
	// https://www.flaticon.com/free-icon/color-wheel_1373048 = color chooser

	// https://www.flaticon.com/authors/freepikhttps://www.flaticon.com/authors/freepik
	// https://www.flaticon.com/packs/electronic-and-web-element-collection-2
	// https://www.flaticon.com/free-icon/edit_391171 = sidebar-texfield
	static final String btnImage = "sidebar-texfield.png"; // 19x21

	private LabelPlusTextPlusTextareaBox(XMultiComponentFactory xMCF, XComponentContext xContext, XToolkit toolkit, XWindowPeer windowPeer) {
		super(new GuiFactoryCreateParam(xMCF, xContext, toolkit, windowPeer));
	}

	private LabelPlusTextPlusTextareaBox(GuiFactoryCreateParam guiFactoryCreateParam) {
		super(guiFactoryCreateParam);
	}

	public static final LabelPlusTextPlusTextareaBox from(GuiFactoryCreateParam guiFactoryCreateParam) {
		return new LabelPlusTextPlusTextareaBox(guiFactoryCreateParam);
	}

	@Override
	String getBtnImage() {
		return btnImage;
	}

}
