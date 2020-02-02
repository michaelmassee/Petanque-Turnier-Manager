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
	public static final String ALIGN = "Align"; // horiztonal alignment, 0: left, 1: center, 2: right
	public static final String V_SCROLL = "VScroll";
	public static final String H_SCROLL = "HScroll";
	public static final String READ_ONLY = "ReadOnly";
	public static final String MULTI_LINE = "MultiLine";
	public static final String HELP_TEXT = "HelpText";
	public static final String ENABLED = "Enabled";
	public static final String VERTICAL_ALIGN = "VerticalAlign";
	public static final String IMAGE_URL = "ImageURL";
	public static final String BACKGROUND_COLOR = "BackgroundColor"; // rgb color
	public static final String PAINT_TRANSPARENT = "PaintTransparent"; // specifies whether the control paints it background or not.
	public static final String BORDER = "Border"; // (short) specifies the border style of the control. 0: No border 1: 3D border 2: simple border
	public static final String BORDER_COLOR = "BorderColor"; // long

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
	 * Erzeugt eine TextArea.
	 *
	 * @return Ein TextArea Control (XTextComponent).
	 */
	public static XControl createTextArea(GuiFactoryCreateParam guiFactoryCreateParam, String text, XTextListener listener, Rectangle size, Map<String, Object> props) {
		if (props == null) {
			props = new HashMap<>();
		}
		props.putIfAbsent(MULTI_LINE, true);
		props.putIfAbsent(V_SCROLL, true);
		props.putIfAbsent(H_SCROLL, true);

		XControl textBoxCtrl = createControl(guiFactoryCreateParam, "com.sun.star.awt.UnoControlEditModel", props, size);
		XTextComponent textcomp = UnoRuntime.queryInterface(XTextComponent.class, textBoxCtrl);
		textcomp.setText(text);
		if (listener != null) {
			textcomp.addTextListener(listener);
		}
		return textBoxCtrl;
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
	 * @Deprecated <br>
	 * verwende createButton(GuiFactoryCreateParam guiFactoryCreateParam, String label, XActionListener listener, Rectangle size, Map<String, Object> props)
	 */
	@Deprecated
	public static XControl createButton(XMultiComponentFactory xMCF, XComponentContext context, XToolkit toolkit, XWindowPeer windowPeer, String label, XActionListener listener,
			Rectangle size, Map<String, Object> props) {
		return createButton(new GuiFactoryCreateParam(xMCF, context, toolkit, windowPeer), label, listener, size, props);
	}

	/**
	 * Erzeugt einen Button mit Label und ActionListener. <br>
	 * properties: https://www.openoffice.org/api/docs/common/ref/com/sun/star/awt/UnoControlButtonModel.html
	 *
	 * @return Ein Button-Control.
	 */

	public static XControl createButton(GuiFactoryCreateParam guiFactoryCreateParam, String label, XActionListener listener, Rectangle size, Map<String, Object> props) {
		XControl buttonCtrl = createControl(guiFactoryCreateParam, "com.sun.star.awt.UnoControlButton", props, size);
		XButton button = UnoRuntime.queryInterface(XButton.class, buttonCtrl);
		if (label != null) {
			button.setLabel(label);
		}
		if (listener != null) {
			button.addActionListener(listener);
		}
		return buttonCtrl;
	}

	@Deprecated
	public static XControl createTextfield(XMultiComponentFactory xMCF, XComponentContext context, XToolkit toolkit, XWindowPeer windowPeer, String text, XTextListener listener,
			Rectangle size, Map<String, Object> props) {
		return createTextfield(new GuiFactoryCreateParam(xMCF, context, toolkit, windowPeer), text, listener, size, props);
	}

	/**
	 * Erzeugt ein Texteingabefeld.<br>
	 * properties : <br>
	 * https://api.libreoffice.org/docs/idl/ref/servicecom_1_1sun_1_1star_1_1awt_1_1UnoControlEditModel.html<br>
	 *
	 * @return Ein Textfield-Control.
	 */
	public static XControl createTextfield(GuiFactoryCreateParam guiFactoryCreateParam, String text, XTextListener listener, Rectangle size, Map<String, Object> props) {
		if (props == null) {
			props = new HashMap<>();
		}
		props.putIfAbsent(MULTI_LINE, false);
		props.putIfAbsent(READ_ONLY, false);
		props.putIfAbsent(V_SCROLL, false);

		XControl controlEdit = createControl(guiFactoryCreateParam, "com.sun.star.awt.UnoControlEdit", props, size);
		XTextComponent txt = UnoRuntime.queryInterface(XTextComponent.class, controlEdit);
		txt.setText(text);
		if (listener != null) {
			txt.addTextListener(listener);
		}
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
			Map<String, Object> props) {
		if (props == null) {
			props = new HashMap<>();
		}
		props.putIfAbsent(MULTI_LINE, false);
		props.putIfAbsent(VERTICAL_ALIGN, VerticalAlignment.MIDDLE);

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

	// https://www.openoffice.org/api/docs/common/ref/com/sun/star/awt/UnoControlComboBox.html
	// https://www.openoffice.org/api/docs/common/ref/com/sun/star/awt/UnoControlComboBoxModel.html
	public static XControl createCombobox(GuiFactoryCreateParam guiFactoryCreateParam, String text, Rectangle size, Map<String, Object> props) {
		if (props == null) {
			props = new HashMap<>();
		}
		props.putIfAbsent("Dropdown", Boolean.TRUE); // specifies if the control has a drop down button.
		props.putIfAbsent("LineCount", (short) 10); // specifies the maximum line count displayed in the drop down box.
		props.putIfAbsent("Autocomplete", Boolean.TRUE); // specifies whether automatic completion of text is enabled.
		// props.putIfAbsent("Enabled", Boolean.FALSE); // specifies that the content of the control cannot be modified by the user.

		XControl ctrl = createControl(guiFactoryCreateParam, "com.sun.star.awt.UnoControlComboBox", props, size);
		XTextComponent tf = UnoRuntime.queryInterface(XTextComponent.class, ctrl);
		tf.setText(text);
		return ctrl;
	}

	// https://www.openoffice.org/api/docs/common/ref/com/sun/star/awt/XNumericField.html
	public static XControl createNumericField(GuiFactoryCreateParam guiFactoryCreateParam, int value, XTextListener listener, Rectangle size, Map<String, Object> props) {

		if (props == null) {
			props = new HashMap<>();
		}
		props.putIfAbsent("setDecimalDigits", 0); // sets the number of decimals.

		XControl ctrl = createControl(guiFactoryCreateParam, "com.sun.star.awt.UnoControlNumericField", props, size);
		UnoRuntime.queryInterface(XNumericField.class, ctrl).setValue(value);
		if (listener != null) {
			UnoRuntime.queryInterface(XTextComponent.class, ctrl).addTextListener(listener);
		}
		return ctrl;
	}

	public static XControl createSpinField(GuiFactoryCreateParam guiFactoryCreateParam, int value, XTextListener listener, Rectangle size, SortedMap<String, Object> props) {
		if (props == null) {
			props = new TreeMap<>();
		}
		props.putIfAbsent("Spin", Boolean.TRUE);

		return createNumericField(guiFactoryCreateParam, value, listener, size, props);
	}

	public static XControl createHLine(GuiFactoryCreateParam guiFactoryCreateParam, Rectangle size, SortedMap<String, Object> props) {
		return createControl(guiFactoryCreateParam, "com.sun.star.awt.UnoControlFixedLine", props, size);
	}

	// https://www.openoffice.org/api/docs/common/ref/com/sun/star/awt/UnoControlListBoxModel.html
	// https://www.openoffice.org/api/docs/common/ref/com/sun/star/awt/UnoControlListBox.html
	public static XControl createListBox(GuiFactoryCreateParam guiFactoryCreateParam, XItemListener listener, Rectangle size, Map<String, Object> props) {
		XControl ctrl = createControl(guiFactoryCreateParam, "com.sun.star.awt.UnoControlListBox", props, size);
		UnoRuntime.queryInterface(XListBox.class, ctrl).addItemListener(listener);

		return ctrl;
	}

	/**
	 * @deprecated<br>
	 * verwende createControl(GuiFactoryCreateParam guiFactoryCreateParam, String type, Map<String, Object> props, Rectangle rectangle)
	 * @return
	 */
	@Deprecated
	public static XControl createControl(XMultiComponentFactory xMCF, XComponentContext xContext, XToolkit toolkit, XWindowPeer windowPeer, String type, Map<String, Object> props,
			Rectangle rectangle) {
		return createControl(new GuiFactoryCreateParam(xMCF, xContext, toolkit, windowPeer), type, props, rectangle);
	}

	/**
	 * Eine allgemeine Hilfsfunktion, mit der UNO-Steuerelemente erzeugt werden.
	 *
	 * @param type Klasse des Steuerelements, das erzeugt werden soll. https://api.libreoffice.org/docs/idl/ref/dir_f6533bbb374262d299aa8b7962df9f04.html
	 * @return Ein Control-Element.
	 */
	public static XControl createControl(GuiFactoryCreateParam guiFactoryCreateParam, String type, Map<String, Object> props, Rectangle rectangle) {

		try {
			XControl control = UnoRuntime.queryInterface(XControl.class, guiFactoryCreateParam.getxMCF().createInstanceWithContext(type, guiFactoryCreateParam.getContext()));
			XControlModel controlModel = UnoRuntime.queryInterface(XControlModel.class,
					guiFactoryCreateParam.getxMCF().createInstanceWithContext(type + "Model", guiFactoryCreateParam.getContext()));
			control.setModel(controlModel);
			XMultiPropertySet properties = UnoRuntime.queryInterface(XMultiPropertySet.class, control.getModel());
			if (props != null && props.size() > 0) {
				properties.setPropertyValues(props.keySet().toArray(new String[props.size()]), props.values().toArray(new Object[props.size()]));
			}
			control.createPeer(guiFactoryCreateParam.getToolkit(), guiFactoryCreateParam.getWindowPeer());
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
