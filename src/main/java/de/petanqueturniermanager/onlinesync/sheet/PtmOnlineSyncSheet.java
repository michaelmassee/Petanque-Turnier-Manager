/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync.sheet;

import java.util.Optional;

import com.sun.star.awt.FontWeight;
import com.sun.star.container.XNamed;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.addins.GlobalImpl;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.onlinesync.speicher.PtmOnlineSyncSpeicher;
import de.petanqueturniermanager.onlinesync.speicher.PtmOnlineSyncSpeicher.Feld;

/**
 * „PTMOnline Sync“: rein informative Anzeige einer PTM-Online-Verbindung. Der Zustand selbst steht in den
 * Dokument-Properties ({@link PtmOnlineSyncSpeicher}); das Blatt zeigt die Kopfwerte per
 * {@code PTM.ALG.STRINGPROPERTY} an und wird vom Plugin nie gelesen. Es ist deshalb nicht Teil des Blattschutzes und
 * wird nur beim Verbinden einmal aufgebaut – der laufende Sync schreibt nicht hinein.
 * <p>
 * Das Blatt ist optional: Nur mit der Turnier-Option {@link BasePropertiesSpalte#KONFIG_PROP_PTMONLINE_SYNC_BLATT}
 * (Standard aus) wird es angelegt, sonst entfernt. Für den Betrieb ist es nicht nötig.
 * <p>
 * {@code PTM.ALG.STRINGPROPERTY} ist nicht volatil: Geänderte Werte erscheinen nach der nächsten Neuberechnung.
 * <p>
 * Bei Supermelee gibt es ein Blatt pro Spieltag ({@code spieltagNr}), sonst genau eines pro Dokument.
 */
public class PtmOnlineSyncSheet extends SheetRunner implements ISheet {

	private static final int SPALTE_LABEL = 0;
	private static final int SPALTE_WERT = 1;
	private static final int SPALTE_LABEL2 = 2;
	private static final int SPALTE_WERT2 = 3;
	private static final int ZEILE_TITEL = 0;
	private static final int ERSTE_KOPF_ZEILE = 1;
	private static final int LETZTE_KOPF_ZEILE = 5;

	private static final int BREITE_LABEL = 6000;
	private static final int BREITE_WERT = 9000;
	private static final int BREITE_LABEL2 = 3500;
	private static final int BREITE_WERT2 = 5000;
	private static final int TITEL_SCHRIFTGROESSE = 14;

	private final Integer spieltagNr;

	public PtmOnlineSyncSheet(WorkingSpreadsheet workingSpreadsheet, TurnierSystem turnierSystem,
			Integer spieltagNrOderNull) {
		super(workingSpreadsheet, turnierSystem, "PtmOnlineSync");
		this.spieltagNr = spieltagNrOderNull;
	}

	@Override
	protected IKonfigurationSheet getKonfigurationSheet() {
		return null;
	}

	private String sheetName() {
		return spieltagNr == null ? SheetNamen.ptmOnlineSync() : SheetNamen.ptmOnlineSync(spieltagNr);
	}

	private String metadatenSchluessel() {
		return spieltagNr == null ? SheetMetadataHelper.SCHLUESSEL_PTM_ONLINE_SYNC
				: SheetMetadataHelper.schluesselPtmOnlineSync(spieltagNr);
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				metadatenSchluessel(), sheetName());
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	protected void doRun() throws GenerateException {
		nachEinstellungAnlegen();
	}

	/**
	 * Beim Verbinden: Mit eingeschalteter Option wird das Blatt neu aufgebaut, sonst ein vorhandenes entfernt.
	 */
	public void nachEinstellungAufbauen() throws GenerateException {
		if (istEingeschaltet()) {
			aufbauen();
		} else {
			entfernen();
		}
	}

	/**
	 * Beim Abgleich: Mit eingeschalteter Option wird ein fehlendes Blatt angelegt, sonst ein vorhandenes entfernt.
	 */
	public void nachEinstellungAnlegen() throws GenerateException {
		if (istEingeschaltet()) {
			anlegen();
		} else {
			entfernen();
		}
	}

	/** Liest nur: Eine fehlende Property wird nicht im Dokument angelegt (läuft auch im Hintergrund). */
	private boolean istEingeschaltet() {
		return DocumentPropertiesHelper.nurLesend(() -> new DocumentPropertiesHelper(getWorkingSpreadsheet())
				.getBooleanProperty(BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_SYNC_BLATT, false));
	}

	/** Baut das Blatt neu auf; ein vorhandenes Blatt wird ersetzt. */
	public void aufbauen() throws GenerateException {
		NewSheet.from(this, sheetName(), metadatenSchluessel()).pos(DefaultSheetPos.SUPERMELEE_WORK).newIfExist()
				.forceCreate().hideGrid().create();
		schreibeKopf();
		formatieren();
	}

	/** Legt das Blatt an, falls es fehlt; ein vorhandenes Blatt bleibt unverändert. */
	public void anlegen() throws GenerateException {
		if (!blattVorhanden()) {
			aufbauen();
		}
	}

	/** Entfernt das Blatt aus dem Dokument. */
	public void entfernen() throws GenerateException {
		Optional<XSpreadsheet> sheet = SheetMetadataHelper
				.findeSheet(getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), metadatenSchluessel());
		if (sheet.isEmpty()) {
			return;
		}
		XNamed named = Lo.qi(XNamed.class, sheet.get());
		if (named != null) {
			getSheetHelper().removeSheet(named.getName());
		}
	}

	private boolean blattVorhanden() {
		return SheetMetadataHelper.findeSheet(getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				metadatenSchluessel()).isPresent();
	}

	/** Beschriftungen als Text-Formeln, Werte als Property-Formeln – ein einziger Block-Write. */
	private void schreibeKopf() throws GenerateException {
		String[][] kopf = {
				{ text(sheetName() + " – ") + "&" + wert(Feld.TURNIER_NAME), "", "", "" },
				{ label("ptmonline.sheet.label.onlineid"), wert(Feld.TURNIER_ID),
						label("ptmonline.sheet.label.onlinestatus"), wert(Feld.ONLINE_STATUS_ANZEIGE) },
				{ label("ptmonline.sheet.label.letzter.sync"), wert(Feld.LETZTER_SYNC), "", "" },
				{ label("ptmonline.sheet.label.sync.document"), wert(Feld.SYNC_DOKUMENT), "", "" },
				{ label("ptmonline.sheet.label.sync.lease"), wert(Feld.SCHLUESSEL), "", "" },
				{ label("ptmonline.sheet.label.sync"), wert(Feld.SYNC_STATUS_ANZEIGE), "", "" } };
		getSheetHelper().setFormulaArrayInRange(getXSpreadSheet(),
				RangePosition.from(SPALTE_LABEL, ZEILE_TITEL, SPALTE_WERT2, LETZTE_KOPF_ZEILE), kopf);
	}

	private String wert(Feld feld) {
		return GlobalImpl.FORMAT_PTM_STRING_PROPERTY(feld.propertyName(spieltagNr));
	}

	private static String label(String schluessel) {
		return text(I18n.get(schluessel));
	}

	/** Zeichenkette als Formel-Literal. */
	private static String text(String wert) {
		return "\"" + wert.replace("\"", "\"\"") + "\"";
	}

	private void formatieren() throws GenerateException {
		XSpreadsheet sheet = getXSpreadSheet();
		SheetHelper helper = getSheetHelper();
		helper.setColumnProperties(sheet, SPALTE_LABEL, ColumnProperties.from().setWidth(BREITE_LABEL));
		helper.setColumnProperties(sheet, SPALTE_WERT, ColumnProperties.from().setWidth(BREITE_WERT));
		helper.setColumnProperties(sheet, SPALTE_LABEL2, ColumnProperties.from().setWidth(BREITE_LABEL2));
		helper.setColumnProperties(sheet, SPALTE_WERT2, ColumnProperties.from().setWidth(BREITE_WERT2));
		helper.setPropertiesInRange(sheet, RangePosition.from(SPALTE_LABEL, ZEILE_TITEL, SPALTE_LABEL, ZEILE_TITEL),
				CellProperties.from().setCharWeight(FontWeight.BOLD).setCharHeight(TITEL_SCHRIFTGROESSE));
		helper.setPropertiesInRange(sheet,
				RangePosition.from(SPALTE_LABEL, ERSTE_KOPF_ZEILE, SPALTE_LABEL, LETZTE_KOPF_ZEILE),
				CellProperties.from().setCharWeight(FontWeight.BOLD));
		helper.setPropertiesInRange(sheet,
				RangePosition.from(SPALTE_LABEL2, ERSTE_KOPF_ZEILE, SPALTE_LABEL2, ERSTE_KOPF_ZEILE),
				CellProperties.from().setCharWeight(FontWeight.BOLD));
	}
}
