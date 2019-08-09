package de.petanqueturniermanager.konfiguration.dialog;

import javax.swing.JOptionPane;

import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import de.petanqueturniermanager.helper.msgbox.DialogTools;

public class MainKonfigDialogTest {

	DialogTools dialogToolsMock = PowerMockito.mock(DialogTools.class);

	@Test
	public void testLayout() {
		MainKonfigDialog dlg = new MainKonfigDialog(dialogToolsMock);
		dlg.initBox();
		dlg.open();

		JOptionPane.showMessageDialog(null, "Warten");
	}

}
