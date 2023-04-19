/**
 * Erstellung 19.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.fields;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.ImageAlign;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XMultiPropertySet;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.style.VerticalAlignment;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.sidebar.GuiFactory;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;

/**
 * Label mit Color feld + Btn für Colorchooser
 *
 * @author Michael Massee
 *
 */
public class LabelPlusBackgrColorAndColorChooser extends BaseField<LabelPlusBackgrColorAndColorChooser>
		implements XActionListener {

	// https://www.flaticon.com/free-icon/color-wheel_1373048 = color chooser
	public static final String btnImage = "sidebar-colorpicker.png"; // 24x24

	XFixedText label;
	XMultiPropertySet labelProperties;
	XFixedText colorLabel;
	XButton btn;

	private LabelPlusBackgrColorAndColorChooser(XMultiComponentFactory xMCF, XComponentContext xContext,
			XToolkit toolkit, XWindowPeer windowPeer) {
		super(new GuiFactoryCreateParam(xMCF, xContext, toolkit, windowPeer));
	}

	private LabelPlusBackgrColorAndColorChooser(GuiFactoryCreateParam guiFactoryCreateParam) {
		super(guiFactoryCreateParam);
	}

	public static final LabelPlusBackgrColorAndColorChooser from(GuiFactoryCreateParam guiFactoryCreateParam) {
		return new LabelPlusBackgrColorAndColorChooser(guiFactoryCreateParam);
	}

	@Override
	protected void doCreate() {
		// ---------------------------------------
		{
			Map<String, Object> props = new HashMap<>();
			// props.putIfAbsent("Align", (short) 2); // rechts
			XControl labelControl = GuiFactory.createLabel(getxMCF(), getxContext(), getToolkit(), getWindowPeer(), "",
					BASE_RECTANGLE, props);
			label = Lo.qi(XFixedText.class, labelControl);
			labelProperties = Lo.qi(XMultiPropertySet.class, labelControl.getModel());
			getLayout().addControl(labelControl, 1);
		}
		// ---------------------------------------
		{
			Map<String, Object> props = new HashMap<>();
			props.putIfAbsent(GuiFactory.BORDER, (short) 2);
			props.putIfAbsent(GuiFactory.BORDER_COLOR, 0); // 0 = black

			XControl labelControl = GuiFactory.createLabel(getxMCF(), getxContext(), getToolkit(), getWindowPeer(), "",
					BASE_RECTANGLE, props);
			colorLabel = Lo.qi(XFixedText.class, labelControl);
			setProperties(Lo.qi(XMultiPropertySet.class, labelControl.getModel()));
			getLayout().addFixedWidthControl(labelControl, 70);
		}
		// ---------------------------------------
		{
			Map<String, Object> props = new HashMap<>();
			props.putIfAbsent(GuiFactory.HELP_TEXT, "Farbauswahl");
			props.putIfAbsent(GuiFactory.VERTICAL_ALIGN, VerticalAlignment.MIDDLE);
			// specifies the horizontal alignment of the text in the control.
			props.putIfAbsent("Align", (short) 1); // 0=left, 1 = center, 2 = right
			props.putIfAbsent("ImageAlign", ImageAlign.RIGHT);
			props.putIfAbsent(GuiFactory.IMAGE_URL, getImageUrlDir() + btnImage);

			// höhe wird nicht verändert
			Rectangle btnRect = new Rectangle(BASE_RECTANGLE.X, BASE_RECTANGLE.Y, BASE_RECTANGLE.Width, 29);
			XControl btnControl = GuiFactory.createButton(getGuiFactoryCreateParam(), null, this, btnRect, props);
			btn = Lo.qi(XButton.class, btnControl);
			getLayout().addFixedWidthControl(btnControl, 29); // fest 29px breit
		}
		// ---------------------------------------
	}

	public LabelPlusBackgrColorAndColorChooser labelText(String text) {
		if (label != null) {
			label.setText(StringUtils.appendIfMissing(text, ":"));
		}
		return this;
	}

	@Override
	public LabelPlusBackgrColorAndColorChooser helpText(String text) {
		super.helpText(text);
		return super.helpText(labelProperties, text);
	}

	@Override
	public void disposing(EventObject arg0) {
		super.disposing();
		labelProperties = null;
		label = null;
		colorLabel = null;
		btn = null;
	}

	@Override
	public final void actionPerformed(ActionEvent arg0) {

	}

	/**
	 * add listner to Button
	 */
	public LabelPlusBackgrColorAndColorChooser addXActionListener(XActionListener listener) {
		btn.addActionListener(listener);
		return this;
	}

	/**
	 * @param propertyValue
	 * @return
	 */
	public LabelPlusBackgrColorAndColorChooser color(int propertyValue) {
		setProperty(GuiFactory.BACKGROUND_COLOR, propertyValue);
		return this;
	}

}
