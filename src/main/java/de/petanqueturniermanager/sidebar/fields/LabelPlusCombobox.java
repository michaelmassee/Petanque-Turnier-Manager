/**
 * Erstellung 19.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.fields;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XItemListener;
import com.sun.star.awt.XListBox;
import com.sun.star.beans.XMultiPropertySet;
import com.sun.star.lang.EventObject;
import com.sun.star.style.VerticalAlignment;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.konfigdialog.ComboBoxItem;
import de.petanqueturniermanager.sidebar.GuiFactory;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;

/**
 * Label mit Text<br>
 * XNumericField
 *
 * @author Michael Massee
 *
 */
public class LabelPlusCombobox extends BaseField<LabelPlusCombobox> implements XItemListener {

	private XFixedText label;
	// private XTextComponent comboText; // ComboBox
	private XListBox listBox; // ComboBox
	private XMultiPropertySet labelProperties;

	private LabelPlusCombobox(GuiFactoryCreateParam guiFactoryCreateParam) {
		super(guiFactoryCreateParam);
	}

	public static final LabelPlusCombobox from(GuiFactoryCreateParam guiFactoryCreateParam) {
		return new LabelPlusCombobox(guiFactoryCreateParam);
	}

	@Override
	protected void doCreate() {
		XControl labelControl = GuiFactory.createLabel(getxMCF(), getxContext(), getToolkit(), getWindowPeer(), "", BASE_RECTANGLE, null);
		label = UnoRuntime.queryInterface(XFixedText.class, labelControl);
		labelProperties = UnoRuntime.queryInterface(XMultiPropertySet.class, labelControl.getModel());
		getLayout().addControl(labelControl);

		// https://www.openoffice.org/api/docs/common/ref/com/sun/star/awt/UnoControlListBoxModel.html
		Map<String, Object> props = new HashMap<>();
		props.putIfAbsent(GuiFactory.VERTICAL_ALIGN, VerticalAlignment.MIDDLE);
		props.putIfAbsent("Dropdown", true);
		props.putIfAbsent("LineCount", (short) 10);
		props.putIfAbsent("MultiSelection", false);

		XControl comboControl = GuiFactory.createListBox(getGuiFactoryCreateParam(), this, BASE_RECTANGLE, props);
		setProperties(UnoRuntime.queryInterface(XMultiPropertySet.class, comboControl.getModel()));
		listBox = UnoRuntime.queryInterface(XListBox.class, comboControl);
		getLayout().addControl(comboControl);
	}

	public LabelPlusCombobox labelText(String text) {
		if (label != null) {
			label.setText(StringUtils.appendIfMissing(text, " :"));
		}
		return this;
	}

	public LabelPlusCombobox addAuswahlItems(List<ComboBoxItem> items) {
		if (listBox != null) {
			String[] allItems = items.stream().map(item -> item.getText()).toArray(String[]::new);
			listBox.addItems(allItems, (short) 0);
		}
		return this;
	}

	@Override
	public LabelPlusCombobox helpText(String text) {
		super.helpText(text);
		return super.helpText(labelProperties, text);
	}

	public LabelPlusCombobox addListener(XItemListener xItemListener) {
		listBox.addItemListener(xItemListener);
		return this;
	}

	public LabelPlusCombobox select(String itemVal) {
		listBox.selectItem(itemVal, true);
		return this;
	}

	@Override
	public void disposing(EventObject arg0) {
		super.disposing();
		label = null;
		listBox = null;
		labelProperties = null;
	}

	@Override
	public void itemStateChanged(ItemEvent arg0) {
		// logger.debug(arg0);
	}

}
