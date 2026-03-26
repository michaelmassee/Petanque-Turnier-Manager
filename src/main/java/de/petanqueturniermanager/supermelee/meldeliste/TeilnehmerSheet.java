/**
* Erstellung : 20.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;

import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.SuperMeleeTeamRechner;
import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;

public class TeilnehmerSheet extends SheetRunner implements ISheet {

	public static final int ERSTE_DATEN_ZEILE = 0;
	public static final int SPIELER_NR_SPALTE = 0; // Spalte A=0
	public static final int SPIELER_NAME_SPALTE = 1; // Spalte A=0
	public static final int ANZAHL_SPALTEN = 3; // nr + name + leer
	public static final int MAX_ANZSPIELER_IN_SPALTE = 40;

	private static final String SHEET_COLOR = "6542f4";

	private final SuperMeleeKonfigurationSheet konfigurationSheet;
	private final MeldeListeSheet_Update meldeliste;
	private SpielTagNr spielTagNr = null;

	public TeilnehmerSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SUPERMELEE, "Teilnehmer");
		konfigurationSheet = new SuperMeleeKonfigurationSheet(workingSpreadsheet);
		meldeliste = new MeldeListeSheet_Update(workingSpreadsheet);
	}

	@Override
	protected SuperMeleeKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				SheetMetadataHelper.schluesselSupermeleeTeilnehmer(getSpielTagNr().getNr()),
				getSheetName(getSpielTagNr()));
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	protected void doRun() throws GenerateException {
		setSpielTagNr(getKonfigurationSheet().getAktiveSpieltag());
		meldeliste.setSpielTag(getSpielTagNr());
		meldeliste.upDateSheet();
		generate();
	}

	public void generate() throws GenerateException {
		meldeliste.setSpielTag(getSpielTagNr());

		// wenn hier dann immer neu erstellen, force = true
		NewSheet.from(this, getSheetName(getSpielTagNr()),
				SheetMetadataHelper.schluesselSupermeleeTeilnehmer(getSpielTagNr().getNr()))
				.tabColor(SHEET_COLOR).pos(DefaultSheetPos.SUPERMELEE_WORK).spielTagPageStyle(getSpielTagNr())
				.forceCreate().hideGrid().setActiv().create();

		// meldeliste nach namen sortieren !
		meldeliste.doSort(meldeliste.getSpielerNameErsteSpalte(), true);

		processBoxinfo("processbox.spieltag.meldungen.einlesen", getSpielTagNr().getNr());
		SpielerMeldungen aktiveUndAusgesetztMeldungen = meldeliste.getAktiveUndAusgesetztMeldungen();

		if (aktiveUndAusgesetztMeldungen.size() == 0) {
			MessageBox.from(getWorkingSpreadsheet(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("msg.caption.teilnehmer.fehler"))
					.message(I18n.get("msg.text.keine.meldungen")).show();
			return;
		}

		ColumnProperties celPropNr = ColumnProperties.from().setHoriJustify(CellHoriJustify.CENTER).setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH);
		NumberCellValue spierNrVal = NumberCellValue.from(getXSpreadSheet(), Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE))
				.setBorder(BorderFactory.from().allThin().toBorder()).setCharColor(ColorHelper.CHAR_COLOR_GRAY_SPIELER_NR);

		ColumnProperties celPropName = ColumnProperties.from().setHoriJustify(CellHoriJustify.CENTER).setWidth(SuperMeleeKonfigurationSheet.SUPER_MELEE_MELDUNG_NAME_WIDTH);

		StringCellValue nameFormula = StringCellValue.from(getXSpreadSheet(), Position.from(SPIELER_NAME_SPALTE, ERSTE_DATEN_ZEILE))
				.setBorder(BorderFactory.from().allThin().toBorder()).setShrinkToFit(true);

		int spielerCntr = 1;
		int maxAnzSpielerInSpalte = 0;
		spalteFormat(spierNrVal, celPropNr, nameFormula, celPropName);

		processBoxinfo("processbox.spieltag.meldungen.einfuegen", getSpielTagNr().getNr(), aktiveUndAusgesetztMeldungen.size());

		for (Spieler spieler : aktiveUndAusgesetztMeldungen.getSpielerList()) {

			spierNrVal.setValue((double) spieler.getNr());
			nameFormula.setValue(meldeliste.formulaSverweisSpielernamen(spierNrVal.getPos().getAddress()));

			getSheetHelper().setNumberValueInCell(spierNrVal);
			getSheetHelper().setFormulaInCell(nameFormula);

			spierNrVal.zeilePlusEins();
			nameFormula.zeilePlusEins();

			if ((spielerCntr / MAX_ANZSPIELER_IN_SPALTE) * MAX_ANZSPIELER_IN_SPALTE == spielerCntr) {
				// Nächste Block
				spierNrVal.spalte((spielerCntr / MAX_ANZSPIELER_IN_SPALTE) * ANZAHL_SPALTEN).zeile(ERSTE_DATEN_ZEILE);
				nameFormula.spalte(spierNrVal.getPos().getSpalte() + 1).zeile(ERSTE_DATEN_ZEILE);
				spalteFormat(spierNrVal, celPropNr, nameFormula, celPropName);
			}
			spielerCntr++;
			if (maxAnzSpielerInSpalte < MAX_ANZSPIELER_IN_SPALTE) {
				maxAnzSpielerInSpalte++;
			}
		}

		int letzteSpalte = nameFormula.getPos().getSpalte();

		StringCellValue footer = StringCellValue.from(getXSpreadSheet(), Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE + maxAnzSpielerInSpalte)).zeilePlusEins()
				.setValue(I18n.get("teilnehmer.footer.anzahl", aktiveUndAusgesetztMeldungen.size()))
				.setEndPosMergeSpalte(letzteSpalte).setCharWeight(FontWeight.BOLD).setCharHeight(12)
				.setShrinkToFit(true);
		getSheetHelper().setStringValueInCell(footer);
		SuperMeleeTeamRechner teamRechner = new SuperMeleeTeamRechner(aktiveUndAusgesetztMeldungen.size());
		footer.zeilePlusEins().setValue(I18n.get("teilnehmer.footer.teams",
				teamRechner.getAnzDoublette(), teamRechner.getAnzTriplette()));
		getSheetHelper().setStringValueInCell(footer);
		footer.zeilePlusEins().setValue(I18n.get("teilnehmer.footer.bahnen", teamRechner.getAnzBahnen()));
		getSheetHelper().setStringValueInCell(footer);
		printBereichDefinieren(footer.getPos(), letzteSpalte);
	}

	private void printBereichDefinieren(Position footerPos, int letzteSpalte) throws GenerateException {
		processBoxinfo("processbox.print.bereich");
		Position linksOben = Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE);
		Position rechtsUnten = Position.from(letzteSpalte, footerPos.getZeile());
		PrintArea.from(getXSpreadSheet(), getWorkingSpreadsheet()).setPrintArea(RangePosition.from(linksOben, rechtsUnten));
	}

	private void spalteFormat(NumberCellValue nrVal, ColumnProperties celPropNr, StringCellValue nameVal, ColumnProperties celPropName) throws GenerateException {
		getSheetHelper().setColumnProperties(getXSpreadSheet(), nrVal.getPos().getSpalte(), celPropNr);
		getSheetHelper().setColumnProperties(getXSpreadSheet(), nameVal.getPos().getSpalte(), celPropName);
		// leere spalte breite
		getSheetHelper().setColumnProperties(getXSpreadSheet(), nameVal.getPos().getSpalte() + 1, celPropNr);
	}

	public String getSheetName(SpielTagNr spieltagNr) {
		return SheetNamen.teilnehmer(spieltagNr.getNr());
	}

	public SpielTagNr getSpielTagNr() {
		checkNotNull(spielTagNr, "spielTagNr == null");
		return spielTagNr;
	}

	public void setSpielTagNr(SpielTagNr spielTag) {
		checkNotNull(spielTag, "spielTagNr == null");
		spielTagNr = spielTag;
	}

}
