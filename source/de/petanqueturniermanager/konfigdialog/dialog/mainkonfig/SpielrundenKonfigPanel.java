/**
 * Erstellung 03.08.2019 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog.dialog.mainkonfig;

import javax.swing.JPanel;

/**
 * @author Michael Massee
 *
 */
public class SpielrundenKonfigPanel extends BaseKonfigPanel {

	private static String LABEL = "SpeilrundeInfo";

	/**
	 * @param content
	 */
	public SpielrundenKonfigPanel(JPanel content) {
		super(content);
	}

	@Override
	public String getLabel() {
		return LABEL;
	}

	@Override
	public void drawContent() {
		JPanel content = getContent();

	}
}
