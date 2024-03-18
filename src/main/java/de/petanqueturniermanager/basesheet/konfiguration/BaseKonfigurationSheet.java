/**
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import org.apache.commons.lang3.StringUtils;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.DocumentHelper;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.newrelease.ExtensionsHelper;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 */
public abstract class BaseKonfigurationSheet extends SheetRunner implements IPropertiesSpalte, IKonfigurationSheet {

	/**
	 * @param workingSpreadsheet
	 */
	protected BaseKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet, TurnierSystem spielSystem) {
		super(workingSpreadsheet, spielSystem);
	}

	// Wird immer von Sheetrunner aufgerufen
	@Override
	public final void update() throws GenerateException {
		processBoxinfo("Update Konfiguration");
		// validate SpielSystem
		validateSpielSystem();
		validateDocErstelltMitVersion();
		updateTurnierSystemInDocument();
		initPageStyles();
		initPageStylesTurnierSystem();
	}

	protected abstract void initPageStylesTurnierSystem() throws GenerateException;

	private void initPageStyles() throws GenerateException {
		// default page Style footer zeilen
		// sicher gehen das änderungen ankommen
		PageStyleHelper.from(this, PageStyle.PETTURNMNGR).initDefaultFooter().setFooterCenter(getFusszeileMitte())
				.setFooterLeft(getFusszeileLinks()).create();
	}

	private void updateTurnierSystemInDocument() {
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(getWorkingSpreadsheet());
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, getTurnierSystem().getId());
	}

	private void validateSpielSystem() throws GenerateException {
		// Property im Document vorhanden ?
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(getWorkingSpreadsheet());
		int spielsystem = docPropHelper.getIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
				TurnierSystem.KEIN.getId());
		if (spielsystem > 0) { // 0 = Kein
			TurnierSystem turnierSystemAusDocument = TurnierSystem.findById(spielsystem);
			TurnierSystem turnierSystemAusSheet = getTurnierSystem();
			if (turnierSystemAusDocument != null && turnierSystemAusSheet.getId() != turnierSystemAusDocument.getId()) {
				de.petanqueturniermanager.helper.msgbox.ProcessBox.from()
						.fehler("Tabellenkalkulationsdokument wurde mit Turniersystem " + turnierSystemAusDocument
								+ " erstellt.");
				throw new GenerateException("Turniersystem '" + getTurnierSystem() + "' stimmt nicht mit Dokument '"
						+ turnierSystemAusDocument + "' überein");
			}
		}
	}

	/**
	 * Schreibt die Aktuelle Plugin Version im Dokument
	 * 
	 * @throws GenerateException
	 */
	public void setDocErstelltMitVersion() throws GenerateException {
		DocumentHelper.setDocErstelltMitVersion(getWorkingSpreadsheet());
	}

	private void validateDocErstelltMitVersion() throws GenerateException {
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(getWorkingSpreadsheet());

		TurnierSystem spielsystem = docPropHelper.getTurnierSystemAusDocument();

		// haben wir ein Turnier Dokument ?
		if (spielsystem != TurnierSystem.KEIN) {
			// Property im Document vorhanden ?
			String docVersion = docPropHelper.getStringProperty(BasePropertiesSpalte.KONFIG_PROP_ERSTELLT_MIT_VERSION,
					"unbekannt");
			boolean versionDoMatch = false;
			String pluginVersionNummer = ExtensionsHelper.from(getxContext()).getVersionNummer();
			if (docVersion != null) {
				versionDoMatch = StringUtils.compare(docVersion, pluginVersionNummer) == 0;
			}

			if (!versionDoMatch) {
				getLogger().debug("Dokument Erstellt mit Version = " + docVersion);
				getLogger().debug("Plugin Version = " + pluginVersionNummer);

				String errMsg = "Dokument-Version '" + docVersion + "' stimmt nicht mit Plugin-Version '"
						+ pluginVersionNummer + "' überein";

				getLogger().warn("Das Turnier-Dokument wurde mit eine andere PTM Plugin-Version erstellt");
				MessageBoxResult answer = MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_YES_NO_CANCEL)
						.caption(errMsg)
						.message("Achtung !! Das Turnier-Dokument wurde mit einer unterschiedliche"
								+ " Turnier-Manager Plugin-Version erstellt.\n\nDie Version im Dokument Aktualisieren ?")
						.show();

				if (answer == MessageBoxResult.CANCEL) {
					throw new GenerateException(errMsg);
				}
				if (answer == MessageBoxResult.YES) {
					DocumentHelper.setDocErstelltMitVersion(getWorkingSpreadsheet());
				}
			}
		}
	}

	@Override
	public final Integer getMeldeListeHintergrundFarbeGerade() throws GenerateException {
		return getPropertiesSpalte().getMeldeListeHintergrundFarbeGerade();
	}

	@Override
	public final MeldungenHintergrundFarbeGeradeStyle getMeldeListeHintergrundFarbeGeradeStyle()
			throws GenerateException {
		return getPropertiesSpalte().getMeldeListeHintergrundFarbeGeradeStyle();
	}

	@Override
	public final Integer getMeldeListeHintergrundFarbeUnGerade() throws GenerateException {
		return getPropertiesSpalte().getMeldeListeHintergrundFarbeUnGerade();
	}

	@Override
	public final MeldungenHintergrundFarbeUnGeradeStyle getMeldeListeHintergrundFarbeUnGeradeStyle()
			throws GenerateException {
		return getPropertiesSpalte().getMeldeListeHintergrundFarbeUnGeradeStyle();
	}

	@Override
	public final Integer getMeldeListeHeaderFarbe() throws GenerateException {
		return getPropertiesSpalte().getMeldeListeHeaderFarbe();
	}

	@Override
	public final String getFusszeileLinks() throws GenerateException {
		return getPropertiesSpalte().getFusszeileLinks();
	}

	@Override
	public final String getFusszeileMitte() throws GenerateException {
		return getPropertiesSpalte().getFusszeileMitte();
	}

	@Override
	public final boolean zeigeArbeitsSpalten() throws GenerateException {
		return getPropertiesSpalte().zeigeArbeitsSpalten();
	}

	@Override
	public final XSpreadsheet getXSpreadSheet() throws GenerateException {
		throw new GenerateException("nicht erlaubt");
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	public final Integer getRanglisteHintergrundFarbeGerade() throws GenerateException {
		return getPropertiesSpalte().getRanglisteHintergrundFarbeGerade();
	}

	@Override
	public final Integer getRanglisteHintergrundFarbeUnGerade() throws GenerateException {
		return getPropertiesSpalte().getRanglisteHintergrundFarbeUnGerade();
	}

	@Override
	public final Integer getRanglisteHeaderFarbe() throws GenerateException {
		return getPropertiesSpalte().getRanglisteHeaderFarbe();
	}

	/**
	 * @return the propertiesSpalte
	 */
	protected abstract IPropertiesSpalte getPropertiesSpalte();

}
