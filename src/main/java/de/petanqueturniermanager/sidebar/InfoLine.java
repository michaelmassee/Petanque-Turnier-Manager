/**
 * Erstellung 19.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar;

import static com.google.common.base.Preconditions.checkNotNull;

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
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;
import de.petanqueturniermanager.sidebar.layout.HorizontalLayout;
import de.petanqueturniermanager.sidebar.layout.Layout;

/**
 * @author Michael Massee
 *
 */
public class InfoLine {

	private final WeakRefHelper<XMultiComponentFactory> xMCF;
	private final WeakRefHelper<XComponentContext> xContext;
	private final WeakRefHelper<XToolkit> toolkit;
	private final WeakRefHelper<XWindowPeer> windowPeer;

	private int lineHeight = 25;
	private int lineWidth = 100;

	private XFixedText label = null;
	private XTextComponent field = null;
	private final Layout hLayout = new HorizontalLayout();

	private InfoLine(XMultiComponentFactory xMCF, XComponentContext xContext, XToolkit toolkit, XWindowPeer windowPeer) {
		this.xMCF = new WeakRefHelper<>(checkNotNull(xMCF));
		this.xContext = new WeakRefHelper<>(checkNotNull(xContext));
		this.toolkit = new WeakRefHelper<>(checkNotNull(toolkit));
		this.windowPeer = new WeakRefHelper<>(checkNotNull(windowPeer));
		newLine();
	}

	public static final InfoLine from(XMultiComponentFactory xMCF, XComponentContext xContext, XToolkit toolkit, XWindowPeer windowPeer) {
		return new InfoLine(xMCF, xContext, toolkit, windowPeer);
	}

	public static final InfoLine from(XMultiComponentFactory xMCF, WorkingSpreadsheet workingSpreadsheet, XToolkit toolkit, XWindowPeer windowPeer) {
		return new InfoLine(xMCF, workingSpreadsheet.getxContext(), toolkit, windowPeer);
	}

	private void newLine() {
		XControl labelControl = GuiFactory.createLabel(xMCF.get(), xContext.get(), toolkit.get(), windowPeer.get(), "", new Rectangle(0, 0, lineWidth, lineHeight), null);
		label = UnoRuntime.queryInterface(XFixedText.class, labelControl);
		hLayout.addControl(labelControl);
		Rectangle sizeTextField = new Rectangle(0, 0, lineWidth, lineHeight);
		Map<String, Object> props = new HashMap<>();
		props.putIfAbsent(GuiFactory.HELP_TEXT, "Aktuelle Turniersystem");
		props.putIfAbsent(GuiFactory.READ_ONLY, true);
		props.putIfAbsent(GuiFactory.ENABLED, false);
		XControl textfieldControl = GuiFactory.createTextfield(xMCF.get(), xContext.get(), toolkit.get(), windowPeer.get(), "", sizeTextField, props);
		field = UnoRuntime.queryInterface(XTextComponent.class, textfieldControl);
		hLayout.addControl(textfieldControl);
	}

	public InfoLine labelText(String text) {
		if (label != null) {
			label.setText(text);
		}
		return this;
	}

	public InfoLine fieldText(Integer intVal) {
		if (field != null) {
			field.setText(intVal.toString());
		}
		return this;
	}

	public InfoLine fieldText(String text) {
		if (field != null) {
			field.setText(text);
		}
		return this;
	}

	/**
	 * @return the hLayout
	 */
	public final Layout getLayout() {
		return hLayout;
	}

}
