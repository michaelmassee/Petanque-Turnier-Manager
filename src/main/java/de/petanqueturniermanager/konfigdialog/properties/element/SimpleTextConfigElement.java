/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog.properties.element;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import com.sun.star.awt.TextEvent;
import com.sun.star.awt.XTextListener;
import com.sun.star.lang.EventObject;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.gui.LabelPlusTextBox;
import de.petanqueturniermanager.sidebar.GuiFactory;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;
import de.petanqueturniermanager.sidebar.layout.HorizontalLayout;
import de.petanqueturniermanager.sidebar.layout.Layout;

/**
 * Kompaktes einzeiliges Textfeld ohne Textarea-Edit-Button — für kurze Werte wie eine Uhrzeit
 * (HH:MM). Siehe {@link StringConfigElement} für die Variante mit Textarea-Editor.
 * <p>
 * Optionale Format-Validierung über {@link ConfigProperty#getValidator()}: ein ungültiger Wert
 * wird beim Tippen NIE persistiert — verhindert, dass kaputte Werte in live referenzierte
 * {@code PTM.ALG.STRINGPROPERTY}-Formeln (z.B. die Rundenzeitplanung) gelangen — und das Feld
 * wird live rot eingefärbt (dieselbe Fehlerfarbe wie {@code FehlerStyle} im Sheet), bis wieder
 * ein gültiger Wert eingegeben wird. Bewusst KEIN zusätzlicher Fokus-Listener mit
 * MessageBox-Feedback: {@code textChanged} feuert bei jedem Tastendruck, ein synchron aus einem
 * (neuen) UNO-Event-Callback aufgerufener modaler Dialog waehrend eines Fokus-Wechsels ist ein
 * bekanntes Freeze/Crash-Risiko in diesem Projekt (siehe CLAUDE.md, Abschnitt Threading) —
 * die reine Hintergrundfarben-Property-Änderung hier ist dagegen unkritisch (kein Re-Entrancy,
 * kein neuer Event-Loop).
 *
 * @author Michael Massee
 */
public class SimpleTextConfigElement implements ConfigElement, XTextListener {

	LabelPlusTextBox labelPlusTextBox;
	ConfigProperty<?> configProperty;
	private WorkingSpreadsheet workingSpreadsheet;
	private Object normalHintergrundFarbe;

	public SimpleTextConfigElement(GuiFactoryCreateParam guiFactoryCreateParam, ConfigProperty<String> configProperty,
			WorkingSpreadsheet workingSpreadsheet) {
		this.configProperty = checkNotNull(configProperty);
		this.workingSpreadsheet = checkNotNull(workingSpreadsheet);
		var labelText = configProperty.getDescription() != null ? configProperty.getDescription() : configProperty.getKey();
		labelPlusTextBox = LabelPlusTextBox.from(guiFactoryCreateParam).labelText(labelText).helpText(labelText)
				.addXTextListener(this).fieldText(getPropertyValue());
		// Ausgangs-Hintergrundfarbe sichern, BEVOR jemals rot eingefaerbt wird (Wert ist beim
		// frisch erzeugten Control noch unveraendert) — theme-sicher statt eine feste Farbe zu raten.
		Object aktuelleFarbe = labelPlusTextBox.getFieldBackgroundColor();
		normalHintergrundFarbe = aktuelleFarbe != null ? aktuelleFarbe : Integer.valueOf(-1); // -1 = automatisch
	}

	@Override
	public Layout getLayout() {
		if (labelPlusTextBox != null) {
			return labelPlusTextBox.getLayout();
		}
		return new HorizontalLayout();
	}

	private boolean istGueltig(String wert) {
		return configProperty.getValidator() == null || configProperty.getValidator().test(wert);
	}

	private void setPropertyValue(String newVal) {
		if (Objects.equals(getPropertyValue(), newVal)) {
			return; // nichts zu tun
		}

		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(workingSpreadsheet);
		docPropHelper.setStringProperty(configProperty.getKey(), newVal);
		configProperty.invokeNachSpeichernAktion(workingSpreadsheet);
	}

	String getPropertyValue() {
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(workingSpreadsheet);
		return docPropHelper.getStringProperty(configProperty.getKey(), (String) configProperty.getDefaultVal());
	}

	@Override
	public void disposing(EventObject arg0) {
		workingSpreadsheet = null;
		configProperty = null;
		labelPlusTextBox = null;
		normalHintergrundFarbe = null;
	}

	@Override
	public void textChanged(TextEvent arg0) {
		if (labelPlusTextBox == null) {
			return;
		}
		String wert = labelPlusTextBox.getFieldText();
		if (istGueltig(wert)) {
			setPropertyValue(wert);
			labelPlusTextBox.setProperty(GuiFactory.BACKGROUND_COLOR, normalHintergrundFarbe);
		} else {
			// ungueltiger Zwischenstand (z.B. waehrend des Tippens von "09:00"): bewusst NICHT
			// persistiert, aber auch nicht per MessageBox gemeldet — siehe Klassen-Doku. Stattdessen
			// live rote Hintergrundfarbe, die im naechsten (gueltigen) textChanged sofort wieder
			// zurueckgesetzt wird.
			labelPlusTextBox.setProperty(GuiFactory.BACKGROUND_COLOR, ColorHelper.CHAR_COLOR_RED);
		}
	}

}
