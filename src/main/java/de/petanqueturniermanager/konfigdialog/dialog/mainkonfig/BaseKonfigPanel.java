/**
 * Erstellung 01.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog.dialog.mainkonfig;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.ref.WeakReference;

import javax.swing.JPanel;

/**
 * @author Michael Massee
 *
 */
public abstract class BaseKonfigPanel implements ConfigPanel {

	private final WeakReference<JPanel> content;

	public BaseKonfigPanel(JPanel content) {
		this.content = new WeakReference<>(checkNotNull(content));
	}

	/**
	 * @return the content
	 */
	protected final JPanel getContent() {
		return content.get();
	}

	@Override
	public String toString() {
		return getLabel();
	}

}
