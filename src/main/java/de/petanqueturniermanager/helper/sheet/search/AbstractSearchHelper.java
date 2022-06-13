/**
 * Erstellung 13.06.2022 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.sheet.BaseHelper;

/**
 * @author Michael Massee
 *
 */
abstract public class AbstractSearchHelper extends BaseHelper {

	private static final Logger logger = LogManager.getLogger(AbstractSearchHelper.class);

	public AbstractSearchHelper(ISheet iSheet) {
		super(iSheet);
	}

}
