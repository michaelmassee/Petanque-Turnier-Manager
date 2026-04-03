/**
 * Erstellung : 30.04.2018 / Michael Massee
 **/

package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.DocumentHelper;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyleDef;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.supermelee.SpielTagNr;

public class NewSheet extends BaseHelper {

	private static final Logger logger = LogManager.getLogger(NewSheet.class);

	private final SheetHelper sheetHelper;
	private final String sheetName;
	private final boolean temporary;
	private boolean didCreate = false;
	private boolean forceOkCreateNewWhenExist = false;
	private boolean setActiv = false;
	private boolean protect = false;
	private short pos = 1;
	private String tabColor = null;
	private PageStyleDef pageStyleDef = null;
	private XSpreadsheet sheet = null;
	private boolean showGrid = true;
	private boolean createNewIfExist = true;
	private boolean setDocVersionWhenNew = false;
	private boolean didCreateRun = false;
	private String metadatenSchluessel = null;

	private NewSheet(ISheet iSheet, String sheetName, String metadatenSchluessel, boolean temporary) {
		super(iSheet);
		checkArgument(StringUtils.isNotBlank(sheetName));
		checkArgument(temporary || StringUtils.isNotBlank(metadatenSchluessel));
		this.sheetName = sheetName;
		this.metadatenSchluessel = metadatenSchluessel;
		this.temporary = temporary;
		sheetHelper = new SheetHelper(iSheet.getWorkingSpreadsheet());
	}

	/** Permanentes Sheet mit Named-Range-Registrierung (Pflicht). */
	public static NewSheet from(ISheet iSheet, String sheetName, String metadatenSchluessel) {
		return new NewSheet(iSheet, sheetName, metadatenSchluessel, false);
	}

	/** Temporäres Rechenhilfsblatt ohne Named-Range-Registrierung. */
	public static NewSheet temporary(ISheet iSheet, String sheetName) {
		return new NewSheet(iSheet, sheetName, null, true);
	}

	/** Lesbarere Abfrage als {@code !temporary}. */
	private boolean hatMetadaten() {
		return !temporary;
	}

	public NewSheet setForceCreate(boolean force) {
		testdidCreateRun();
		forceOkCreateNewWhenExist = force;
		return this;
	}

	public NewSheet forceCreate() {
		testdidCreateRun();
		forceOkCreateNewWhenExist = true;
		return this;
	}

	public NewSheet pos(short pos) {
		testdidCreateRun();
		this.pos = pos;
		return this;
	}

	public NewSheet tabColor(String color) {
		testdidCreateRun();
		tabColor = color;
		return this;
	}

	public NewSheet tabColor(int color) {
		return tabColor(Integer.toHexString(color));
	}

	public NewSheet spielTagPageStyle(SpielTagNr spielTagNr) {
		pageStyleDef = new PageStyleDef(spielTagNr);
		return this;
	}

	public NewSheet newIfExist() {
		testdidCreateRun();
		createNewIfExist = true;
		return this;
	}

	/**
	 * wenn bereits vorhanden, dann nichts tun, sondern einfach return.
	 */
	public NewSheet useIfExist() {
		testdidCreateRun();
		createNewIfExist = false;
		return this;
	}

	public NewSheet setDocVersionWhenNew() {
		testdidCreateRun();
		setDocVersionWhenNew = true;
		return this;
	}

	private void testdidCreateRun() {
		if (didCreateRun) {
			throw new RuntimeException("didCreateRun ist bereits gelaufen");
		}
	}

	/**
	 * Erstellt oder öffnet das Sheet.<br>
	 * Bei permanenten Sheets (nicht {@code temporary}) werden Named-Range-Metadaten geschrieben, sofern sie fehlen oder
	 * veraltet sind.<br>
	 * Wenn das Sheet unter einem anderen Namen existiert (z.B. nach Sprachenwechsel), wird es automatisch umbenannt.
	 */
	public NewSheet create() throws GenerateException {
		XSpreadsheetDocument xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		sheet = sheetHelper.findByName(sheetName);
		didCreate = false;
		didCreateRun = true;

		TurnierSheet turnierSheet = null;

		// i18n-aware Suche: Sheet könnte nach Sprachenwechsel anders heißen
		if (sheet == null && hatMetadaten()) {
			Optional<XSpreadsheet> byKey = SheetMetadataHelper.findeSheet(xDoc, metadatenSchluessel);
			if (byKey.isPresent()) {
				XSpreadsheet gefunden = byKey.get();
				String alterName = TurnierSheet.from(gefunden, getWorkingSpreadsheet()).getName();
				if (alterName.equals(sheetName)) {
					logger.error("Inkonsistenz: Sheet '{}' per Metadaten gefunden, aber nicht per findByName (key='{}')",
							sheetName, metadatenSchluessel);
				} else {
					logger.warn("Sheet '{}' → '{}' (i18n-Umbenennung, Risiko: String-Referenzen in Formeln)",
							alterName, sheetName);
					sheetHelper.reNameSheet(gefunden, sheetName);
					sheet = gefunden;
				}
			}
		}

		if (sheet != null) {
			turnierSheet = TurnierSheet.from(sheet, getWorkingSpreadsheet());
			if (createNewIfExist) {
				// setActiv() nur wenn der User einen Dialog sieht – bei forceCreate() (programmatisch)
				// würde setActiv() unnötig den RanglisteRefreshListener auslösen (isRunning=false im Test)
				if (!forceOkCreateNewWhenExist) {
					turnierSheet.setActiv();
				}
				MessageBoxResult result = MessageBox
						.from(getWorkingSpreadsheet().getxContext(), MessageBoxTypeEnum.WARN_YES_NO)
						.caption(I18n.get("msg.caption.tabelle.erstellen", sheetName))
						.message(I18n.get("msg.text.tabelle.erstellen.frage", sheetName))
						.forceOk(forceOkCreateNewWhenExist).show();
				if (MessageBoxResult.YES != result) {
					return this;
				}
				// removeSheet() bereinigt intern bereits verwaiste Named-Range-Einträge
				sheetHelper.removeSheet(sheetName);
				sheet = null;
				turnierSheet = null;
			}
		}

		if (sheet == null) {
			try {
				sheetHelper.newIfNotExist(sheetName, pos);

				if (setDocVersionWhenNew) {
					DocumentHelper.setDocErstelltMitVersion(getWorkingSpreadsheet());
				}

				sheet = sheetHelper.findByName(sheetName);
				turnierSheet = TurnierSheet.from(sheet, getWorkingSpreadsheet());
				if (!showGrid) {
					// nur bei Neu, einmal abschalten
					turnierSheet.toggleSheetGrid();
				}

				if (pageStyleDef != null) {
					// Info: alle PageStyles werden in KonfigurationSheet initialisiert, (Header etc)
					// @see KonfigurationSheet#initPageStyles
					// @see SheetRunner#updateKonfigurationSheet
					// sheet direkt übergeben – Metadaten sind zu diesem Zeitpunkt noch nicht geschrieben,
					// daher darf applytoSheet() nicht über iSheet.getXSpreadSheet() suchen.
					PageStyleHelper.from(getISheet(), pageStyleDef).initDefaultFooter().create().applytoSheet(sheet);
				} else {
					// dann nur der default, mit copyright footer
					PageStyleHelper.from(getISheet(), new PageStyleDef(PageStyle.PETTURNMNGR)).initDefaultFooter()
							.create().applytoSheet(sheet);
				}
				didCreate = true;

			} catch (IllegalArgumentException e) {
				logger.error(e.getMessage(), e);
			}
		}

		if (turnierSheet != null) {
			turnierSheet.protect(protect).tabColor(tabColor).setActiv(setActiv);
		}

		if (hatMetadaten() && sheet != null) {
			if (didCreate || !SheetMetadataHelper.istRegistriertesSheet(xDoc, sheet, metadatenSchluessel)) {
				SheetMetadataHelper.schreibeSheetMetadaten(xDoc, sheet, metadatenSchluessel);
			}
		}

		return this;
	}

	public boolean isDidCreate() {
		return didCreate;
	}

	public NewSheet setActiv(boolean activ) {
		setActiv = activ;
		return this;
	}

	public NewSheet setActiv() {
		setActiv = true;
		return this;
	}

	public NewSheet showGrid() {
		showGrid = true;
		return this;
	}

	public NewSheet hideGrid() {
		showGrid = false;
		return this;
	}

	public NewSheet protect() {
		protect = true;
		return this;
	}

	public XSpreadsheet getSheet() {
		return sheet;
	}
}
