/**
 * Erstellung 12.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog.dialog.element;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XTextComponent;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;

/**
 * @author Michael Massee
 *
 */
public class UITextAreaProperty implements UIProperty {

	private static final int TEXT_HEIGHT = 30;
	private static final int GAP_HEIGHT = 7;

	private static int propCntr = 0;

	private final String propName;
	private final String label;
	private final String uiName;
	private final String labelName;
	private final String defaultVal;
	private DocumentPropertiesHelper documentPropertiesHelper;
	private XTextComponent uITextArea;

	public UITextAreaProperty(String propName, String label, String defaultVal) {
		this.propName = checkNotNull(propName);
		this.label = checkNotNull(label);
		this.defaultVal = checkNotNull(defaultVal);
		uiName = "UITextArea" + propCntr;
		labelName = "UILabel" + propCntr++;
	}

	public int getHeight() {
		return TEXT_HEIGHT + GAP_HEIGHT;
	}

	@Override
	public void initDefault(WorkingSpreadsheet currentSpreadsheet) {
		documentPropertiesHelper = new DocumentPropertiesHelper(currentSpreadsheet);
		documentPropertiesHelper.setStringProperty(getPropName(), defaultVal);
	}

	@Override
	public int doInsert(Object dialogModel, XControlContainer xControlCont, int posY) {

		// @formatter:off
		UILabel.from(dialogModel)
				.name(labelName)
				.label(label + " :")
				.posX(3).posY(posY).width(40).height(14)
				.align(2) // Right
				.doInsert(xControlCont);
		// @formatter:on

		String propVal = documentPropertiesHelper.getStringProperty(getPropName(), false, "");
		// @formatter:off
		uITextArea = UITextArea.from(dialogModel)
				.name(uiName)
				.posX(45).posY(posY).width(200).height(TEXT_HEIGHT)
				.multiLine(true).vScroll(true).hScroll(true)
				.Text(propVal)
				.doInsert(xControlCont);
		// @formatter:on
		return getHeight();
	}

	@Override
	public void save() {
		documentPropertiesHelper.setStringProperty(getPropName(), uITextArea.getText());
	}

	public String getPropName() {
		return propName;
	}

}
