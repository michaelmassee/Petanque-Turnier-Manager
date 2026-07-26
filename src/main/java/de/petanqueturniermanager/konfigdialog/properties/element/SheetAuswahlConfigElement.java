/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog.properties.element;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.XItemListener;
import com.sun.star.lang.EventObject;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.konfigdialog.ComboBoxItem;
import de.petanqueturniermanager.konfigdialog.SheetAuswahlConfigProperty;
import de.petanqueturniermanager.konfigdialog.gui.LabelPlusCombobox;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;
import de.petanqueturniermanager.sidebar.layout.HorizontalLayout;
import de.petanqueturniermanager.sidebar.layout.Layout;

/**
 * ComboBox mit den Namen der aktuell im Dokument vorhandenen Tabellenblätter, statt Freitext.
 * Anders als {@link AuswahlConfigElement} ist die Auswahlliste nicht statisch in der
 * {@link SheetAuswahlConfigProperty} hinterlegt, sondern wird bei jedem Öffnen des Dialogs neu
 * aus dem {@code WorkingSpreadsheet} ermittelt.
 */
public class SheetAuswahlConfigElement implements ConfigElement, XItemListener {

	private static final String KEIN_SHEET_KEY = "__KEIN_SHEET__";

	private LabelPlusCombobox labelPlusCombobox;
	private SheetAuswahlConfigProperty configProperty;
	private WorkingSpreadsheet workingSpreadsheet;
	private List<ComboBoxItem> auswahl;

	public SheetAuswahlConfigElement(GuiFactoryCreateParam guiFactoryCreateParam,
			SheetAuswahlConfigProperty configProperty, WorkingSpreadsheet workingSpreadsheet) {
		this.configProperty = checkNotNull(configProperty);
		this.workingSpreadsheet = checkNotNull(workingSpreadsheet);
		this.auswahl = ermittleAuswahl(workingSpreadsheet);
		var labelText = configProperty.getDescription() != null ? configProperty.getDescription() : configProperty.getKey();
		labelPlusCombobox = LabelPlusCombobox.from(guiFactoryCreateParam).labelText(labelText)
				.helpText(labelText).addAuswahlItems(auswahl)
				.addListener(this).select(getComboboxItemValue());
	}

	private static List<ComboBoxItem> ermittleAuswahl(WorkingSpreadsheet workingSpreadsheet) {
		List<ComboBoxItem> items = new ArrayList<>();
		items.add(new ComboBoxItem(KEIN_SHEET_KEY, I18n.get("config.auswahl.kein.sheet")));
		for (String sheetName : new SheetHelper(workingSpreadsheet).getSheets().getElementNames()) {
			items.add(new ComboBoxItem(sheetName, sheetName));
		}
		return items;
	}

	@Override
	public Layout getLayout() {
		if (labelPlusCombobox != null) {
			return labelPlusCombobox.getLayout();
		}
		return new HorizontalLayout();
	}

	private void setPropertyValue(String keyVal) {
		String neuerWert = KEIN_SHEET_KEY.equals(keyVal) ? "" : keyVal;
		if (neuerWert.equalsIgnoreCase(getPropertyValue())) {
			// nichts zu tun
			return;
		}
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(workingSpreadsheet);
		docPropHelper.setStringProperty(configProperty.getKey(), neuerWert);
	}

	private String getComboboxItemValue() {
		String wert = getPropertyValue();
		String key = wert.isEmpty() ? KEIN_SHEET_KEY : wert;
		ComboBoxItem itemFromVal = auswahl.stream()
				.filter(cmbItem -> key.equalsIgnoreCase(cmbItem.getKey())).findAny()
				.orElse(auswahl.get(0));
		return itemFromVal.getText();
	}

	/**
	 * @return gespeicherter Sheet-Name, oder "" wenn keiner konfiguriert ist
	 */
	private String getPropertyValue() {
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(workingSpreadsheet);
		return docPropHelper.getStringProperty(configProperty.getKey(), configProperty.getDefaultVal());
	}

	@Override
	public void disposing(EventObject arg0) {
		workingSpreadsheet = null;
		configProperty = null;
		labelPlusCombobox = null;
		auswahl = null;
	}

	@Override
	public void itemStateChanged(ItemEvent itemEvent) {
		if (itemEvent != null && itemEvent.Selected < auswahl.size()) {
			ComboBoxItem comboBoxItem = auswahl.get(itemEvent.Selected);
			setPropertyValue(comboBoxItem.getKey());
		}
	}
}
