/**
 * Erstellung 22.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.fields;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XItemListener;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XMultiPropertySet;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.sidebar.GuiFactory;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;

/**
 * @author Michael Massee
 *
 */
public class LabelPlusCheckBox extends BaseField<LabelPlusCheckBox> implements XItemListener {

	static final Logger logger = LogManager.getLogger(LabelPlusCheckBox.class);

	private XCheckBox field;

	private LabelPlusCheckBox(XMultiComponentFactory xMCF, XComponentContext xContext, XToolkit toolkit,
			XWindowPeer windowPeer) {
		super(new GuiFactoryCreateParam(xMCF, xContext, toolkit, windowPeer));
	}

	private LabelPlusCheckBox(GuiFactoryCreateParam guiFactoryCreateParam) {
		super(guiFactoryCreateParam);
	}

	public static final LabelPlusCheckBox from(GuiFactoryCreateParam guiFactoryCreateParam) {
		return new LabelPlusCheckBox(guiFactoryCreateParam);
	}

	public static final LabelPlusCheckBox from(XMultiComponentFactory xMCF, XComponentContext xContext,
			XToolkit toolkit, XWindowPeer windowPeer) {
		return new LabelPlusCheckBox(xMCF, xContext, toolkit, windowPeer);
	}

	public static final LabelPlusCheckBox from(XMultiComponentFactory xMCF, WorkingSpreadsheet workingSpreadsheet,
			XToolkit toolkit, XWindowPeer windowPeer) {
		return new LabelPlusCheckBox(xMCF, workingSpreadsheet.getxContext(), toolkit, windowPeer);
	}

	@Override
	protected void doCreate() {
		Map<String, Object> props = new HashMap<>();
		// props.putIfAbsent(GuiFactory.HELP_TEXT, "Aktuelle Turniersystem");
		// props.putIfAbsent(GuiFactory.READ_ONLY, true);
		// props.putIfAbsent(GuiFactory.ENABLED, false);
		XControl checkBoxControl = GuiFactory.createCheckBox(getxMCF(), getxContext(), getToolkit(), getWindowPeer(),
				"..", this, BASE_RECTANGLE, props);
		field = Lo.qi(XCheckBox.class, checkBoxControl);
		setProperties(Lo.qi(XMultiPropertySet.class, checkBoxControl.getModel()));
		getLayout().addControl(checkBoxControl);
	}

	public LabelPlusCheckBox setStat(boolean state) {
		if (field != null) {
			field.setState((short) (state ? 1 : 0));
		}
		return this;
	}

	public LabelPlusCheckBox labelText(String text) {
		if (field != null) {
			field.setLabel(text);
		}
		return this;
	}

	public LabelPlusCheckBox addListener(XItemListener itemListener) {
		if (field != null) {
			field.addItemListener(itemListener);
		}
		return this;
	}

	@Override
	public void disposing(EventObject arg0) {
		super.disposing();
		field = null;
	}

	@Override
	public void itemStateChanged(ItemEvent arg0) {
	}

}
