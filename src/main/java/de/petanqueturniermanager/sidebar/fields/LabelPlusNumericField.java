/**
 * Erstellung 19.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.fields;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.sun.star.awt.TextEvent;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XNumericField;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XTextListener;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XMultiPropertySet;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.style.VerticalAlignment;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.sidebar.GuiFactory;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;

/**
 * Label mit XNumericField<br>
 *
 * @author Michael Massee
 *
 */
public class LabelPlusNumericField extends BaseField<LabelPlusNumericField> implements XTextListener {

	private XFixedText label;
	private XNumericField field;
	private XTextComponent textfield;
	private XMultiPropertySet labelProperties;

	private LabelPlusNumericField(XMultiComponentFactory xMCF, XComponentContext xContext, XToolkit toolkit,
			XWindowPeer windowPeer) {
		super(new GuiFactoryCreateParam(xMCF, xContext, toolkit, windowPeer));
	}

	private LabelPlusNumericField(GuiFactoryCreateParam guiFactoryCreateParam) {
		super(guiFactoryCreateParam);
	}

	public static final LabelPlusNumericField from(GuiFactoryCreateParam guiFactoryCreateParam) {
		return new LabelPlusNumericField(guiFactoryCreateParam);
	}

	public static final LabelPlusNumericField from(XMultiComponentFactory xMCF, XComponentContext xContext,
			XToolkit toolkit, XWindowPeer windowPeer) {
		return new LabelPlusNumericField(xMCF, xContext, toolkit, windowPeer);
	}

	public static final LabelPlusNumericField from(XMultiComponentFactory xMCF, WorkingSpreadsheet workingSpreadsheet,
			XToolkit toolkit, XWindowPeer windowPeer) {
		return new LabelPlusNumericField(xMCF, workingSpreadsheet.getxContext(), toolkit, windowPeer);
	}

	@Override
	protected void doCreate() {

		XControl labelControl = GuiFactory.createLabel(getxMCF(), getxContext(), getToolkit(), getWindowPeer(), "",
				BASE_RECTANGLE, null);
		label = Lo.qi(XFixedText.class, labelControl);
		labelProperties = Lo.qi(XMultiPropertySet.class, labelControl.getModel());
		getLayout().addControl(labelControl);

		Map<String, Object> props = new HashMap<>();
		props.putIfAbsent(GuiFactory.VERTICAL_ALIGN, VerticalAlignment.MIDDLE);
		XControl numfieldControl = GuiFactory.createNumericField(getGuiFactoryCreateParam(), 0, this, BASE_RECTANGLE,
				props);
		setProperties(Lo.qi(XMultiPropertySet.class, numfieldControl.getModel()));
		field = Lo.qi(XNumericField.class, numfieldControl);
		field.setDecimalDigits((short) 0);
		textfield = Lo.qi(XTextComponent.class, numfieldControl);
		getLayout().addFixedWidthControl(numfieldControl, 40);
	}

	@Override
	public LabelPlusNumericField helpText(String text) {
		super.helpText(text);
		return super.helpText(labelProperties, text);
	}

	public LabelPlusNumericField labelText(String text) {
		if (label != null) {
			label.setText(StringUtils.appendIfMissing(text, " :"));
		}
		return this;
	}

	public LabelPlusNumericField fieldVal(Integer intVal) {
		if (field != null) {
			field.setValue(intVal);
		}
		return this;
	}

	public Double getFieldVal() {
		if (field != null) {
			return field.getValue();
		}
		return 0D;
	}

	public LabelPlusNumericField fieldVal(String val) {
		if (field != null) {
			field.setValue(Double.parseDouble(val));
		}
		return this;
	}

	@Override
	public void disposing(EventObject arg0) {
		super.disposing();
		label = null;
		field = null;
		textfield = null;
		labelProperties = null;
	}

	@Override
	public void textChanged(TextEvent arg0) {
		// TODO Auto-generated method stub
	}

	/**
	 * @param integerConfigSidebarElement
	 * @return
	 */
	public LabelPlusNumericField addXTextListener(XTextListener xTextListener) {
		if (textfield != null) {
			textfield.addTextListener(xTextListener);
		}
		return this;
	}

}
