/**
 * Erstellung 19.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.fields;

import java.util.HashMap;
import java.util.Map;

import com.sun.star.awt.TextEvent;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XTextListener;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XMultiPropertySet;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.style.VerticalAlignment;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.sidebar.GuiFactory;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;

/**
 * Label mit Text<br>
 * XNumericField
 *
 * @author Michael Massee
 *
 */
public class LabelPlusTextReadOnly extends BaseField<LabelPlusTextReadOnly> implements XTextListener {

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

		XControl labelControl = GuiFactory.createLabel(getxMCF(), getxContext(), getToolkit(), getWindowPeer(), "", BASE_RECTANGLE, null);
		label = UnoRuntime.queryInterface(XFixedText.class, labelControl);
		getLayout().addControl(labelControl);

		Map<String, Object> props = new HashMap<>();
		// props.putIfAbsent(GuiFactory.HELP_TEXT, "Aktuelle Turniersystem");
		props.putIfAbsent(GuiFactory.READ_ONLY, true);
		props.putIfAbsent(GuiFactory.ENABLED, false);
		props.putIfAbsent(GuiFactory.VERTICAL_ALIGN, VerticalAlignment.MIDDLE);
		XControl textfieldControl = GuiFactory.createTextfield(getGuiFactoryCreateParam(), "", this, BASE_RECTANGLE, props);
		setProperties(UnoRuntime.queryInterface(XMultiPropertySet.class, textfieldControl.getModel()));
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

	@Override
	public void disposing(EventObject arg0) {
		super.disposing();
		label = null;
		field = null;
	}

	@Override
	public void textChanged(TextEvent arg0) {
		// TODO Auto-generated method stub
	}

}
