package de.petanqueturniermanager.konfigdialog.properties.element;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlModel;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.XComponent;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.helper.Lo;

/**
 * Regressionstest für einen Layout-Bug in {@code UITextAreaProperty.doInsert()}: das Label links
 * neben dem Textfeld war mit {@code height(200)} statt {@code height(14)} angelegt (Tippfehler,
 * Kommentar im Code verriet die eigentlich gemeinte Höhe). Dadurch überlappte das Label bis weit
 * unterhalb des Textfelds die neuen Platzhalter-Buttons (siehe {@link TextAreaDialog}) – konkret
 * den ersten/linkesten Button ("Datum"), der dadurch Klicks nicht mehr empfing, während die
 * weiter rechts liegenden Buttons (außerhalb des überdimensionierten Labels) funktionierten.
 * <p>
 * Dieser Test baut den echten Dialog auf ({@link TextAreaDialog#baueDialog()}, ohne den
 * blockierenden {@code execute()}) und prüft generisch, dass sich keine zwei Controls im Dialog
 * überlappen.
 */
public class TextAreaDialogLayoutUITest extends BaseCalcUITest {

	@Test
	void keineZweiControlsUeberlappenSichImKopfFusszeilenTextEditor() throws Exception {
		TextAreaDialog textAreaDialog = new TextAreaDialog(wkingSpreadsheet);
		textAreaDialog.initTextArea("Testfeld", "Testfeld", "Seite {SEITE} von {SEITEN}", true);

		TextAreaDialog.DialogAufbau aufbau = textAreaDialog.baueDialog();
		try {
			List<Rectangle> rechtecke = new ArrayList<>();
			for (XControl control : aufbau.xControlCont().getControls()) {
				XControlModel model = control.getModel();
				XPropertySet props = Lo.qi(XPropertySet.class, model);
				int x = (Integer) props.getPropertyValue("PositionX");
				int y = (Integer) props.getPropertyValue("PositionY");
				int w = (Integer) props.getPropertyValue("Width");
				int h = (Integer) props.getPropertyValue("Height");
				rechtecke.add(new Rectangle(x, y, w, h));
			}

			assertThat(rechtecke).as("Dialog muss mindestens Label, Textfeld, 6 Platzhalter- und 2 Aktions-Buttons enthalten")
					.hasSizeGreaterThanOrEqualTo(9);

			for (int i = 0; i < rechtecke.size(); i++) {
				for (int j = i + 1; j < rechtecke.size(); j++) {
					Rectangle a = rechtecke.get(i);
					Rectangle b = rechtecke.get(j);
					assertThat(a.intersects(b))
							.as("Controls %s und %s dürfen sich nicht überlappen (sonst blockiert eines Klicks auf das andere)", a, b)
							.isFalse();
				}
			}
		} finally {
			Lo.qi(XComponent.class, aufbau.dialog()).dispose();
		}
	}
}
