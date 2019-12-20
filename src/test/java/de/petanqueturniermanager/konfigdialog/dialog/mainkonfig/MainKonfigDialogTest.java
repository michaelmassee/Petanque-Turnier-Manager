package de.petanqueturniermanager.konfigdialog.dialog.mainkonfig;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import de.petanqueturniermanager.helper.msgbox.DialogTools;

@org.junit.Ignore
public class MainKonfigDialogTest {

	DialogTools dialogToolsMock = PowerMockito.mock(DialogTools.class);

	@Test
	public void testLayout() {
		MainKonfigDialog dlg = new MainKonfigDialog(dialogToolsMock);
		dlg.initBox();
		dlg.open();

		JDialog waitdlg = new JDialog();
		waitdlg.setModal(false);
		waitdlg.setVisible(true);

		JOptionPane.showMessageDialog(null, "Warten");
	}

}
