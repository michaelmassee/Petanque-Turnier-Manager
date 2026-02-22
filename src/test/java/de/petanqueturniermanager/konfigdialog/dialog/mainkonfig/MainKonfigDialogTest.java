package de.petanqueturniermanager.konfigdialog.dialog.mainkonfig;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.petanqueturniermanager.helper.msgbox.DialogTools;

@org.junit.jupiter.api.Disabled
public class MainKonfigDialogTest {

	DialogTools dialogToolsMock = Mockito.mock(DialogTools.class);

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
