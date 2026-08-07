/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog.gui;

import java.util.HashMap;
import java.util.Map;

import com.sun.star.awt.TextEvent;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XTextListener;
import com.sun.star.beans.XMultiPropertySet;
import com.sun.star.lang.EventObject;
import com.sun.star.style.VerticalAlignment;

import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.StringTools;
import de.petanqueturniermanager.sidebar.GuiFactory;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;

/**
 * Label mit schmalem, einzeiligem Textfeld (ohne Textarea-Edit-Button) — für kurze Werte wie
 * eine Uhrzeit (HH:MM).
 *
 * @author Michael Massee
 */
public class LabelPlusTextBox extends BaseField<LabelPlusTextBox> implements XTextListener {

	private static final int FELD_BREITE = 50;

	private XFixedText label;
	private XTextComponent field;
	private XMultiPropertySet labelProperties;

	private LabelPlusTextBox(GuiFactoryCreateParam guiFactoryCreateParam) {
		super(guiFactoryCreateParam);
	}

	public static final LabelPlusTextBox from(GuiFactoryCreateParam guiFactoryCreateParam) {
		return new LabelPlusTextBox(guiFactoryCreateParam);
	}

	@Override
	protected void doCreate() {
		XControl labelControl = GuiFactory.createLabel(getxMCF(), getxContext(), getToolkit(), getWindowPeer(), "",
				BASE_RECTANGLE, null);
		label = Lo.qi(XFixedText.class, labelControl);
		labelProperties = Lo.qi(XMultiPropertySet.class, labelControl.getModel());
		getLayout().addControl(labelControl, 1);

		Map<String, Object> props = new HashMap<>();
		props.putIfAbsent(GuiFactory.READ_ONLY, false);
		props.putIfAbsent(GuiFactory.ENABLED, true);
		props.putIfAbsent(GuiFactory.VERTICAL_ALIGN, VerticalAlignment.MIDDLE);

		XControl textfieldControl = GuiFactory.createTextfield(getGuiFactoryCreateParam(), "", null, BASE_RECTANGLE, props);
		field = Lo.qi(XTextComponent.class, textfieldControl);
		setProperties(Lo.qi(XMultiPropertySet.class, textfieldControl.getModel()));
		getLayout().addFixedWidthControl(textfieldControl, FELD_BREITE);
	}

	@Override
	public LabelPlusTextBox helpText(String text) {
		super.helpText(text);
		return super.helpText(labelProperties, text);
	}

	public LabelPlusTextBox labelText(String text) {
		if (label != null) {
			label.setText(StringTools.appendIfMissing(text, " :"));
		}
		return this;
	}

	public LabelPlusTextBox fieldText(String text) {
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

	public LabelPlusTextBox addXTextListener(XTextListener xTextListener) {
		if (field != null) {
			field.addTextListener(xTextListener);
		}
		return this;
	}

	/**
	 * Aktuelle Hintergrundfarbe des Textfeldes (für Validierungs-Feedback: Ausgangswert vor einer
	 * temporären Rot-Einfärbung sichern, siehe {@code SimpleTextConfigElement}).
	 */
	public Object getFieldBackgroundColor() {
		return getProperty(GuiFactory.BACKGROUND_COLOR);
	}

	@Override
	public void disposing(EventObject arg0) {
		super.disposing();
		label = null;
		field = null;
		labelProperties = null;
	}

	@Override
	public void textChanged(TextEvent arg0) {
		// Standard-Implementierung: nichts tun
	}

}
