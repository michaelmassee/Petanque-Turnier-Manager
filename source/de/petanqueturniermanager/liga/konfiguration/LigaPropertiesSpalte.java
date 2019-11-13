/**
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.konfiguration;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;

/**
 * @author Michael Massee
 *
 */
public class LigaPropertiesSpalte extends BasePropertiesSpalte implements ILigaPropertiesSpalte {

	private static final List<ConfigProperty<?>> KONFIG_PROPERTIES = new ArrayList<>();

	static {
		ADDBaseProp(KONFIG_PROPERTIES);
	}

	/**
	 * @param propertiesSpalte
	 * @param erstePropertiesZeile
	 * @param sheet
	 */
	LigaPropertiesSpalte(int propertiesSpalte, int erstePropertiesZeile, ISheet sheet) {
		super(propertiesSpalte, erstePropertiesZeile, sheet);
	}

	@Override
	protected List<ConfigProperty<?>> getKonfigProperties() {
		return KONFIG_PROPERTIES;
	}

	@Override
	public SpielTagNr getAktiveSpieltag() throws GenerateException {
		return SpielTagNr.from(1);
	}

	@Override
	public void setAktiveSpieltag(SpielTagNr spieltag) throws GenerateException {
		// nichts
	}

	@Override
	public SpielRundeNr getAktiveSpielRunde() throws GenerateException {
		return SpielRundeNr.from(1);
	}

	@Override
	public void setAktiveSpielRunde(SpielRundeNr neueSpielrunde) throws GenerateException {
		// Nichts
	}

}
