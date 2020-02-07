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
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XTextListener;
import com.sun.star.beans.XMultiPropertySet;
import com.sun.star.lang.EventObject;
import com.sun.star.style.VerticalAlignment;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.sidebar.GuiFactory;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;

/**
 * Label mit Text + Btn für TextAreaInput
 *
 * @author Michael Massee
 *
 */
public class LabelPlusTextPlusTextareaBox extends BaseField<LabelPlusTextPlusTextareaBox> implements XActionListener {

	// private static final Logger logger = LogManager.getLogger(LabelPlusTextPlusTextareaBox.class);
	// https://www.flaticon.com/free-icon/color-wheel_1373048 = color chooser

	XFixedText label;
	XTextComponent field;
	XButton btn;
	XMultiPropertySet labelProperties;

	// https://www.flaticon.com/authors/freepikhttps://www.flaticon.com/authors/freepik
	// https://www.flaticon.com/packs/electronic-and-web-element-collection-2
	// https://www.flaticon.com/free-icon/edit_391171 = sidebar-texfield
	public static final String btnImage = "sidebar-texfield.png"; // 19x21

	private LabelPlusTextPlusTextareaBox(GuiFactoryCreateParam guiFactoryCreateParam) {
		super(guiFactoryCreateParam);
	}

	public static final LabelPlusTextPlusTextareaBox from(GuiFactoryCreateParam guiFactoryCreateParam) {
		return new LabelPlusTextPlusTextareaBox(guiFactoryCreateParam);
	}

	@Override
	protected void doCreate() {
		// ---------------------------------------
		{
			Map<String, Object> props = new HashMap<>();
			// props.putIfAbsent(GuiFactory.BACKGROUND_COLOR, Integer.valueOf(StringUtils.strip(StringUtils.strip("#e38029"), "#"), 16)); // 227, 128, 41 #e38029
			XControl labelControl = GuiFactory.createLabel(getxMCF(), getxContext(), getToolkit(), getWindowPeer(), "", BASE_RECTANGLE, props);
			label = UnoRuntime.queryInterface(XFixedText.class, labelControl);
			labelProperties = UnoRuntime.queryInterface(XMultiPropertySet.class, labelControl.getModel());
			getLayout().addControl(labelControl, 1);
		}
		// ---------------------------------------
		{
			Map<String, Object> props = new HashMap<>();
			props.putIfAbsent(GuiFactory.READ_ONLY, false);
			props.putIfAbsent(GuiFactory.ENABLED, true);
			props.putIfAbsent(GuiFactory.VERTICAL_ALIGN, VerticalAlignment.MIDDLE);

			XControl textfieldControl = GuiFactory.createTextfield(getGuiFactoryCreateParam(), "", null, BASE_RECTANGLE, props);
			field = UnoRuntime.queryInterface(XTextComponent.class, textfieldControl);
			setProperties(UnoRuntime.queryInterface(XMultiPropertySet.class, textfieldControl.getModel()));
			getLayout().addControl(textfieldControl, 1);
		}
		// ---------------------------------------
		{
			Map<String, Object> props = new HashMap<>();
			props.putIfAbsent(GuiFactory.HELP_TEXT, "Bearbeiten");
			props.putIfAbsent(GuiFactory.VERTICAL_ALIGN, VerticalAlignment.MIDDLE);
			props.putIfAbsent("TextColor", Integer.valueOf(StringUtils.strip(StringUtils.strip("#e38029"), "#"), 16)); // 227, 128, 41 #e38029
			// specifies the horizontal alignment of the text in the control.
			props.putIfAbsent("Align", (short) 1); // 0=left, 1 = center, 2 = right
			props.putIfAbsent("ImageAlign", ImageAlign.RIGHT);
			props.putIfAbsent(GuiFactory.IMAGE_URL, getImageUrlDir() + btnImage);

			// höhe wird nicht verändert
			Rectangle btnRect = new Rectangle(BASE_RECTANGLE.X, BASE_RECTANGLE.Y, BASE_RECTANGLE.Width, 29);
			XControl btnControl = GuiFactory.createButton(getGuiFactoryCreateParam(), null, this, btnRect, props);
			btn = UnoRuntime.queryInterface(XButton.class, btnControl);
			getLayout().addFixedWidthControl(btnControl, 29); // fest 29px breit
		}
		// ---------------------------------------
	}

	/**
	 */
	public LabelPlusTextPlusTextareaBox color(int newColor) {
		setProperty(GuiFactory.BACKGROUND_COLOR, newColor);
		return this;
	}

	/**
	 * add listner to Button
	 */
	public LabelPlusTextPlusTextareaBox addXActionListener(XActionListener listener) {
		btn.addActionListener(listener);
		return this;
	}

	@Override
	public LabelPlusTextPlusTextareaBox helpText(String text) {
		super.helpText(text);
		return super.helpText(labelProperties, text);
	}

	/**
	 */
	public LabelPlusTextPlusTextareaBox addXTextListener(XTextListener listener) {
		field.addTextListener(listener);
		return this;
	}

	public LabelPlusTextPlusTextareaBox labelText(String text) {
		if (label != null) {
			label.setText(StringUtils.appendIfMissing(text, " :"));
		}
		return this;
	}

	public LabelPlusTextPlusTextareaBox fieldText(String text) {
		if (field != null) {
			field.setText(text);
		}
		return this;
	}

	public String getFieldText() {
		if (field != null) {
			return field.getText();
		}
		return "";
	}

	@Override
	public void disposing(EventObject arg0) {
		super.disposing();
		labelProperties = null;
		label = null;
		field = null;
		btn = null;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub

	}

}
