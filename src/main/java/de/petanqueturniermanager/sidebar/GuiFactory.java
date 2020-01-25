package de.petanqueturniermanager.sidebar;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.PosSize;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.WindowAttribute;
import com.sun.star.awt.WindowClass;
import com.sun.star.awt.WindowDescriptor;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XItemListener;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XNumericField;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XTextListener;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.awt.tree.XMutableTreeDataModel;
import com.sun.star.beans.XMultiPropertySet;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.style.VerticalAlignment;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/**
 * Die Factory enthält Hilfsfunktionen zum einfacheren Erzeugen von UNO-Steuerelementen.<br>
 * Copy von: de.muenchen.allg.itd51.wollmux.sidebar.GuiFactory;
 *
 */
public class GuiFactory {

	private static final Logger logger = LogManager.getLogger(GuiFactory.class);
	public static final String V_SCROLL = "VScroll";
	public static final String READ_ONLY = "ReadOnly";
	public static final String MULTI_LINE = "MultiLine";
	public static final String HELP_TEXT = "HelpText";
	public static final String ENABLED = "Enabled";

	private GuiFactory() {
	}

	/**
	 * Erzeugt ein Fenster ohne Dekorationen. Das Fenster kann als Inhalt eines Sidebar-Panels verwendet werden.
	 *
	 * @param toolkit
	 * @param parentWindow
	 * @return Ein neues XWindowPeer
	 */
	public static XWindowPeer createWindow(XToolkit toolkit, XWindowPeer parentWindow) {
		WindowDescriptor aWindow = new WindowDescriptor();
		aWindow.Type = WindowClass.CONTAINER;
		aWindow.WindowServiceName = "";
		aWindow.Parent = parentWindow;
		aWindow.ParentIndex = -1;
		aWindow.Bounds = new Rectangle(0, 0, 10, 10);
		aWindow.WindowAttributes = WindowAttribute.SIZEABLE | WindowAttribute.MOVEABLE | WindowAttribute.NODECORATION;
		return toolkit.createWindow(aWindow);
	}

	/**
	 * Erzeugt eine CheckBox.
	 *
	 * @return Ein CheckBox-Control.
	 */
	public static XControl createCheckBox(XMultiComponentFactory xMCF, XComponentContext context, XToolkit toolkit, XWindowPeer windowPeer, String label, XItemListener listener,
			Rectangle size, Map<String, Object> props) {
		XControl checkBoxCtrl = createControl(xMCF, context, toolkit, windowPeer, "com.sun.star.awt.UnoControlCheckBox", props, size);
		XCheckBox checkBox = UnoRuntime.queryInterface(XCheckBox.class, checkBoxCtrl);
		checkBox.setLabel(label);
		if (listener != null) {
			checkBox.addItemListener(listener);
		}
		return checkBoxCtrl;
	}

	/**
	 * Erzeugt einen Button mit Label und ActionListener.
	 *
	 * @return Ein Button-Control.
	 */
	public static XControl createButton(XMultiComponentFactory xMCF, XComponentContext context, XToolkit toolkit, XWindowPeer windowPeer, String label, XActionListener listener,
			Rectangle size, SortedMap<String, Object> props) {
		XControl buttonCtrl = createControl(xMCF, context, toolkit, windowPeer, "com.sun.star.awt.UnoControlButton", props, size);
		XButton button = UnoRuntime.queryInterface(XButton.class, buttonCtrl);
		button.setLabel(label);
		button.addActionListener(listener);
		return buttonCtrl;
	}

	/**
	 * Erzeugt ein Texteingabefeld.<br>
	 * properties : <br>
	 * https://api.libreoffice.org/docs/idl/ref/servicecom_1_1sun_1_1star_1_1awt_1_1UnoControlEditModel.html<br>
	 *
	 * @return Ein Textfield-Control.
	 */
	public static XControl createTextfield(XMultiComponentFactory xMCF, XComponentContext context, XToolkit toolkit, XWindowPeer windowPeer, String text, Rectangle size,
			Map<String, Object> props) {
		if (props == null) {
			props = new HashMap<>();
		}
		props.putIfAbsent(MULTI_LINE, false);
		props.putIfAbsent(READ_ONLY, false);
		props.putIfAbsent(V_SCROLL, false);

		XControl controlEdit = createControl(xMCF, context, toolkit, windowPeer, "com.sun.star.awt.UnoControlEdit", props, size);
		XTextComponent txt = UnoRuntime.queryInterface(XTextComponent.class, controlEdit);
		txt.setText(text);
		return controlEdit;
	}

	/**
	 * Erzeugt ein Label.<br>
	 * properties: <br>
	 * https://www.openoffice.org/api/docs/common/ref/com/sun/star/awt/UnoControlFixedTextModel.html<br>
	 *
	 * @return Ein Label-Control
	 */
	public static XControl createLabel(XMultiComponentFactory xMCF, XComponentContext context, XToolkit toolkit, XWindowPeer windowPeer, String text, Rectangle size,
			SortedMap<String, Object> props) {
		if (props == null) {
			props = new TreeMap<>();
		}
		props.putIfAbsent(MULTI_LINE, true);
		props.putIfAbsent("VerticalAlign", VerticalAlignment.MIDDLE);

		XControl fixedTextCtrl = createControl(xMCF, context, toolkit, windowPeer, "com.sun.star.awt.UnoControlFixedText", props, size);
		XFixedText txt = UnoRuntime.queryInterface(XFixedText.class, fixedTextCtrl);
		txt.setText(text);
		return fixedTextCtrl;
	}

	/**
	 * Erzeugt ein Datenmodell für einen Baum-Steuerelement.
	 *
	 * @return Ein Datenmodell für XTrees.
	 * @throws Exception
	 */
	public static XMutableTreeDataModel createTreeModel(XMultiComponentFactory xMCF, XComponentContext context) throws Exception {
		return UnoRuntime.queryInterface(XMutableTreeDataModel.class, xMCF.createInstanceWithContext("com.sun.star.awt.tree.MutableTreeDataModel", context));
	}

	/**
	 * Erzeugt ein Baum-Steuerelement mit einem vorgegebenen Datenmodell. Das Datenmodel kann mit {@link #createTreeModel(XMultiComponentFactory, XComponentContext)} erzeugt
	 * werden.
	 *
	 * @return Ein Treel-Control
	 */
	public static XControl createTree(XMultiComponentFactory xMCF, XComponentContext context, XToolkit toolkit, XWindowPeer windowPeer, XMutableTreeDataModel dataModel) {
		SortedMap<String, Object> props = new TreeMap<>();
		props.putIfAbsent("DataModel", dataModel);
		return GuiFactory.createControl(xMCF, context, toolkit, windowPeer, "com.sun.star.awt.tree.TreeControl", props, new Rectangle(0, 0, 400, 400));
	}

	public static XControl createCombobox(XMultiComponentFactory xMCF, XComponentContext context, XToolkit toolkit, XWindowPeer windowPeer, String text, Rectangle size,
			SortedMap<String, Object> props) {
		XControl ctrl = createControl(xMCF, context, toolkit, windowPeer, "com.sun.star.awt.UnoControlComboBox", props, size);
		XTextComponent tf = UnoRuntime.queryInterface(XTextComponent.class, ctrl);
		tf.setText(text);
		XComboBox cmb = UnoRuntime.queryInterface(XComboBox.class, ctrl);
		cmb.setDropDownLineCount((short) 10);

		return ctrl;
	}

	public static XControl createNumericField(XMultiComponentFactory xMCF, XComponentContext context, XToolkit toolkit, XWindowPeer windowPeer, int value, XTextListener listener,
			Rectangle size, SortedMap<String, Object> props) {
		XControl ctrl = createControl(xMCF, context, toolkit, windowPeer, "com.sun.star.awt.UnoControlNumericField", props, size);
		UnoRuntime.queryInterface(XNumericField.class, ctrl).setValue(value);
		UnoRuntime.queryInterface(XTextComponent.class, ctrl).addTextListener(listener);
		return ctrl;
	}

	public static XControl createSpinField(XMultiComponentFactory xMCF, XComponentContext context, XToolkit toolkit, XWindowPeer windowPeer, int value, XTextListener listener,
			Rectangle size, SortedMap<String, Object> props) {
		if (props == null) {
			props = new TreeMap<>();
		}
		props.putIfAbsent("Spin", Boolean.TRUE);

		return createNumericField(xMCF, context, toolkit, windowPeer, value, listener, size, props);
	}

	public static XControl createHLine(XMultiComponentFactory xMCF, XComponentContext context, XToolkit toolkit, XWindowPeer windowPeer, Rectangle size,
			SortedMap<String, Object> props) {
		return createControl(xMCF, context, toolkit, windowPeer, "com.sun.star.awt.UnoControlFixedLine", props, size);
	}

	public static XControl createListBox(XMultiComponentFactory xMCF, XComponentContext context, XToolkit toolkit, XWindowPeer windowPeer, XItemListener listener, Rectangle size,
			SortedMap<String, Object> props) {
		XControl ctrl = createControl(xMCF, context, toolkit, windowPeer, "com.sun.star.awt.UnoControlListBox", props, size);
		UnoRuntime.queryInterface(XListBox.class, ctrl).addItemListener(listener);

		return ctrl;
	}

	/**
	 * Eine allgemeine Hilfsfunktion, mit der UNO-Steuerelemente erzeugt werden.
	 *
	 * @param type Klasse des Steuerelements, das erzeugt werden soll. https://api.libreoffice.org/docs/idl/ref/dir_f6533bbb374262d299aa8b7962df9f04.html
	 * @return Ein Control-Element.
	 */
	public static XControl createControl(XMultiComponentFactory xMCF, XComponentContext xContext, XToolkit toolkit, XWindowPeer windowPeer, String type, Map<String, Object> props,
			Rectangle rectangle) {
		try {
			XControl control = UnoRuntime.queryInterface(XControl.class, xMCF.createInstanceWithContext(type, xContext));
			XControlModel controlModel = UnoRuntime.queryInterface(XControlModel.class, xMCF.createInstanceWithContext(type + "Model", xContext));
			control.setModel(controlModel);
			XMultiPropertySet properties = UnoRuntime.queryInterface(XMultiPropertySet.class, control.getModel());
			if (props != null && props.size() > 0) {
				properties.setPropertyValues(props.keySet().toArray(new String[props.size()]), props.values().toArray(new Object[props.size()]));
			}
			control.createPeer(toolkit, windowPeer);
			XWindow controlWindow = UnoRuntime.queryInterface(XWindow.class, control);
			setWindowPosSize(controlWindow, rectangle);
			return control;
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return null;
		}
	}

	/**
	 * Ändert die Größe und Position eines Fensters.
	 */
	public static void setWindowPosSize(XWindow window, Rectangle posSize) {
		setWindowPosSize(window, posSize, 0, 0);
	}

	private static void setWindowPosSize(XWindow window, Rectangle posSize, int horizontalOffset, int verticalOffset) {
		window.setPosSize(posSize.X - horizontalOffset, posSize.Y - verticalOffset, posSize.Width, posSize.Height, PosSize.POSSIZE);
	}

}