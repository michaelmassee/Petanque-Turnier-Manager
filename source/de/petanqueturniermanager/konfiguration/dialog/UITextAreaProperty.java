/**
 * Erstellung 12.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.konfiguration.dialog;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XTextComponent;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.konfiguration.DocumentPropertiesHelper;

/**
 * @author Michael Massee
 *
 */
public class UITextAreaProperty implements UIProperty {

	private static final int TEXT_HEIGHT = 50;
	private static final int FIRSTLINE_POSY = 10;

	private static int propCntr = 0;

	private final String propName;
	private final String label;
	private final String uiName;
	private final String labelName;
	private final String defaultVal;
	private DocumentPropertiesHelper documentPropertiesHelper;
	private XTextComponent uITextArea;
	private final int line;

	public UITextAreaProperty(String propName, String label, String defaultVal, int line) {
		this.propName = checkNotNull(propName);
		this.label = checkNotNull(label);
		this.defaultVal = checkNotNull(defaultVal);
		uiName = "UITextArea" + propCntr;
		labelName = "UILabel" + propCntr++;
		this.line = line;
	}

	@Override
	public void initDefault(WorkingSpreadsheet currentSpreadsheet) {
		documentPropertiesHelper = new DocumentPropertiesHelper(currentSpreadsheet);
		documentPropertiesHelper.insertStringPropertyIfNotExist(propName, defaultVal);
	}

	@Override
	public void doInsert(Object dialogModel, XControlContainer xControlCont) {

		// FIRSTLINE_POSY
		int posy = (line * TEXT_HEIGHT) + FIRSTLINE_POSY;

		// @formatter:off
		UILabel.from(dialogModel)
				.name(labelName)
				.label(label + " :")
				.posX(3).posY(posy).width(80).height(14)
				.align(2) // Right
				.doInsert(xControlCont);
		// @formatter:on

		String propVal = documentPropertiesHelper.getStringProperty(propName);
		// @formatter:off
		uITextArea = UITextArea.from(dialogModel)
				.name(uiName)
				.posX(100).posY(posy).width(200).height(TEXT_HEIGHT)
				.multiLine(true).vScroll(true).hScroll(true)
				.Text(propVal)
				.doInsert(xControlCont);
		// @formatter:on
	}

	@Override
	public void save() {
		documentPropertiesHelper.setStringProperty(propName, uITextArea.getText());
	}

}
