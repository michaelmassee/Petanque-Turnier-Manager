/**
 * Erstellung 19.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.fields;

import java.util.HashMap;
import java.util.Map;

import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.sidebar.GuiFactory;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;

/**
 * Label mit Text
 *
 * @author Michael Massee
 *
 */
public class LabelPlusTextReadOnly extends BaseField {

	private static final int lineHeight = 25;
	private static final int lineWidth = 100;

	private XFixedText label;
	private XTextComponent field;

	private LabelPlusTextReadOnly(XMultiComponentFactory xMCF, XComponentContext xContext, XToolkit toolkit, XWindowPeer windowPeer) {
		super(new GuiFactoryCreateParam(xMCF, xContext, toolkit, windowPeer));
	}

	private LabelPlusTextReadOnly(GuiFactoryCreateParam guiFactoryCreateParam) {
		super(guiFactoryCreateParam);
	}

	public static final LabelPlusTextReadOnly from(GuiFactoryCreateParam guiFactoryCreateParam) {
		return new LabelPlusTextReadOnly(guiFactoryCreateParam);
	}

	public static final LabelPlusTextReadOnly from(XMultiComponentFactory xMCF, XComponentContext xContext, XToolkit toolkit, XWindowPeer windowPeer) {
		return new LabelPlusTextReadOnly(xMCF, xContext, toolkit, windowPeer);
	}

	public static final LabelPlusTextReadOnly from(XMultiComponentFactory xMCF, WorkingSpreadsheet workingSpreadsheet, XToolkit toolkit, XWindowPeer windowPeer) {
		return new LabelPlusTextReadOnly(xMCF, workingSpreadsheet.getxContext(), toolkit, windowPeer);
	}

	@Override
	protected void doCreate() {
		Rectangle baseRectangle = new Rectangle(0, 0, lineWidth, lineHeight);

		XControl labelControl = GuiFactory.createLabel(getxMCF(), getxContext(), getToolkit(), getWindowPeer(), "", baseRectangle, null);
		label = UnoRuntime.queryInterface(XFixedText.class, labelControl);
		getLayout().addControl(labelControl);

		Map<String, Object> props = new HashMap<>();
		// props.putIfAbsent(GuiFactory.HELP_TEXT, "Aktuelle Turniersystem");
		props.putIfAbsent(GuiFactory.READ_ONLY, true);
		props.putIfAbsent(GuiFactory.ENABLED, false);
		XControl textfieldControl = GuiFactory.createTextfield(getxMCF(), getxContext(), getToolkit(), getWindowPeer(), "", baseRectangle, props);
		field = UnoRuntime.queryInterface(XTextComponent.class, textfieldControl);
		getLayout().addControl(textfieldControl);
	}

	public LabelPlusTextReadOnly labelText(String text) {
		if (label != null) {
			label.setText(text);
		}
		return this;
	}

	public LabelPlusTextReadOnly fieldText(Integer intVal) {
		if (field != null) {
			field.setText(intVal.toString());
		}
		return this;
	}

	public LabelPlusTextReadOnly fieldText(String text) {
		if (field != null) {
			field.setText(text);
		}
		return this;
	}

}
