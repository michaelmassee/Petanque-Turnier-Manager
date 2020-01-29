/**
 * Erstellung 28.01.2020 / Michael Massee
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
 * @author Michael Massee
 *
 */
public abstract class BaseLabelPlusTextPlusBtn<T> extends BaseField<T> implements XActionListener {

	XFixedText label;
	XTextComponent field;
	XButton btn;
	XMultiPropertySet labelProperties;

	/**
	 * @param guiFactoryCreateParam
	 */
	public BaseLabelPlusTextPlusBtn(GuiFactoryCreateParam guiFactoryCreateParam) {
		super(guiFactoryCreateParam);
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
			props.putIfAbsent(GuiFactory.IMAGE_URL, getImageUrlDir() + getBtnImage());

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
	@SuppressWarnings("unchecked")
	public T color(int newColor) {
		setProperty(GuiFactory.BACKGROUND_COLOR, newColor);
		return (T) this;
	}

	/**
	 * add listner to Button
	 */
	@SuppressWarnings("unchecked")
	public T addXActionListener(XActionListener listener) {
		btn.addActionListener(listener);
		return (T) this;
	}

	@Override
	public T helpText(String text) {
		super.helpText(text);
		return super.helpText(labelProperties, text);
	}

	/**
	 */
	@SuppressWarnings("unchecked")
	public T addXTextListener(XTextListener listener) {
		field.addTextListener(listener);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T labelText(String text) {
		if (label != null) {
			label.setText(StringUtils.appendIfMissing(text, ":"));
		}
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T fieldText(String text) {
		if (field != null) {
			field.setText(text);
		}
		return (T) this;
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
	public final void actionPerformed(ActionEvent arg0) {

	}

	abstract String getBtnImage();

}