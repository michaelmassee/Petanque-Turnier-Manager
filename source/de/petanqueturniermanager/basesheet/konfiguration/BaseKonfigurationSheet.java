/**
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.supermelee.meldeliste.SpielSystem;

/**
 * @author Michael Massee
 *
 */
abstract public class BaseKonfigurationSheet extends SheetRunner implements IPropertiesSpalte, IKonfigurationSheet {

	/**
	 * @param workingSpreadsheet
	 */
	public BaseKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	public final Integer getMeldeListeHintergrundFarbeGerade() throws GenerateException {
		return getPropertiesSpalte().getMeldeListeHintergrundFarbeGerade();
	}

	@Override
	public final Integer getMeldeListeHintergrundFarbeUnGerade() throws GenerateException {
		return getPropertiesSpalte().getMeldeListeHintergrundFarbeUnGerade();
	}

	@Override
	public final Integer getMeldeListeHeaderFarbe() throws GenerateException {
		return getPropertiesSpalte().getMeldeListeHeaderFarbe();
	}

	@Override
	public final SpielSystem getSpielSystem() throws GenerateException {
		return getPropertiesSpalte().getSpielSystem();
	}

	/**
	 * @return the propertiesSpalte
	 */
	abstract protected IPropertiesSpalte getPropertiesSpalte();

}
