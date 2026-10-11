/*
 * Erstellung 12.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog.dialog.element;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.atomic.AtomicInteger;

import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XTextComponent;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;

/**
 * @author Michael Massee
 *
 */
public class UITextAreaProperty implements UIProperty {

	private static final int DEFAULT_TEXT_HEIGHT = 30;
	private static final int LABEL_HEIGHT = 14;
	private static final int LABEL_TEXT_GAP = 2;

	private static final AtomicInteger PROP_CNTR = new AtomicInteger();

	private final String propName;
	private final String label;
	private final String uiName;
	private final String labelName;
	private final String defaultVal;
	private final int textHeight;
	private DocumentPropertiesHelper documentPropertiesHelper;
	private XTextComponent uITextArea;

	public UITextAreaProperty(String propName, String label, String defaultVal) {
		this(propName, label, defaultVal, DEFAULT_TEXT_HEIGHT);
	}

	public UITextAreaProperty(String propName, String label, String defaultVal, int textHeight) {
		this.propName = checkNotNull(propName);
		this.label = checkNotNull(label);
		this.defaultVal = checkNotNull(defaultVal);
		this.textHeight = textHeight;
		int id = PROP_CNTR.getAndIncrement();
		uiName = "UITextArea" + id;
		labelName = "UILabel" + id;
	}

	public int getHeight() {
		return LABEL_HEIGHT + LABEL_TEXT_GAP + textHeight;
	}

	@Override
	public void initDefault(WorkingSpreadsheet currentSpreadsheet) {
		documentPropertiesHelper = new DocumentPropertiesHelper(currentSpreadsheet);
		documentPropertiesHelper.setStringProperty(getPropName(), defaultVal);
	}

	@Override
	public int doInsert(Object dialogModel, XControlContainer xControlCont, int posY) {
		return doInsert(dialogModel, xControlCont, posY, 250);
	}

	/**
	 * Fügt Beschriftung und Textfeld übereinander über die gesamte Dialogbreite ein.
	 * Damit bleiben auch längere Property-Namen sichtbar und der Editor nutzt den
	 * verfügbaren Platz vollständig aus.
	 */
	public int doInsert(Object dialogModel, XControlContainer xControlCont, int posY, int dialogWidth) {
		int rand = 5;
		int controlWidth = dialogWidth - (2 * rand);

		// @formatter:off
		UILabel.from(dialogModel)
				.name(labelName)
				.label(label + " :")
				.posX(rand).posY(posY).width(controlWidth).height(LABEL_HEIGHT)
				.multiLine()
				.doInsert(xControlCont);
		// @formatter:on

		String propVal = documentPropertiesHelper.getStringProperty(getPropName(), defaultVal);
		// @formatter:off
		uITextArea = UITextArea.from(dialogModel)
				.name(uiName)
				.posX(rand).posY(posY + LABEL_HEIGHT + LABEL_TEXT_GAP).width(controlWidth).height(textHeight)
				.multiLine(true).vScroll(true).hScroll(true)
				.text(propVal)
				.doInsert(xControlCont);
		// @formatter:on
		return getHeight();
	}

	@Override
	public void save() {
		documentPropertiesHelper.setStringProperty(getPropName(), uITextArea.getText());
	}

	/**
	 * Zugriff auf das Textfeld für Platzhalter-Einfüge-Buttons (siehe {@code TextAreaDialog}).
	 */
	public XTextComponent getTextComponent() {
		return uITextArea;
	}

	public String getPropName() {
		return propName;
	}

}
