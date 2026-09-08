/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.List;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.MeleeAnmeldungKonfiguration;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;

/**
 * Gemeinsame Basis der Mêlée-Anmeldung-Sheets aller Turniersysteme mit wählbarer
 * Meldeliste-Formation (Schweizer, JGJ, KO, Kaskade, Poule, Formule&nbsp;X).
 * <p>
 * Das Sheet ist eine schlichte Erfassungsliste loser Einzelspieler (Vorname, Nachname,
 * Setzposition, Eingecheckt-Markierung). Per Menü-Kommando „Mêlée-Anmeldung übernehmen" werden
 * daraus Teams gemischt und in die eigentliche Meldeliste geschrieben; die Zeilen bleiben erhalten
 * und werden als „Übernommen" markiert.
 * <p>
 * Der Aufbau ist bewusst <b>datenerhaltend</b> ({@link NewSheet#useIfExist()}): ein erneuter Aufruf
 * des Menüpunkts baut nur Kopfzeile und Formatierung neu auf, die erfassten Anmeldungen bleiben
 * stehen.
 */
public abstract class AbstractMeleeAnmeldungSheet extends SheetRunner
		implements ISheet, MeleeAnmeldungKonstanten {

	private static final TableBorder2 HEADER_BORDER =
			BorderFactory.from().allThin().boldLn().forBottom().toBorder();

	protected AbstractMeleeAnmeldungSheet(WorkingSpreadsheet workingSpreadsheet, TurnierSystem turnierSystem,
			String logPrefix) {
		super(workingSpreadsheet, turnierSystem, logPrefix);
	}

	@Override
	protected void doRun() throws GenerateException {
		generate();
	}

	/**
	 * Erzeugt das Mêlée-Anmeldung-Sheet (falls noch nicht vorhanden) und baut Kopfzeile,
	 * Spaltenformate, Fixierung und Druckbereich auf.
	 */
	public void generate() throws GenerateException {
		processBoxinfo("processbox.melee.anmeldung.erstellen");
		NewSheet.from(this, SheetNamen.meleeAnmeldung(), getMetadatenSchluessel())
				.tabColor(getKonfigurationSheet().getMeldelisteTabFarbe())
				.pos(DefaultSheetPos.MELEE_ANMELDUNG)
				.useIfExist().setActiv().create();

		kopfzeileSchreiben();
		datenSpaltenFormatieren();
		nummernSchreiben();

		SheetFreeze.from(getXSpreadSheet(), getWorkingSpreadsheet()).anzZeilen(ERSTE_DATEN_ZEILE).doFreeze();
		PrintArea.from(getXSpreadSheet(), getWorkingSpreadsheet())
				.setPrintArea(RangePosition.from(SPALTE_NR, KOPF_ZEILE, LETZTE_SPALTE, letzteFormatierteZeile()));
	}

	private void kopfzeileSchreiben() throws GenerateException {
		int headerFarbe = getKonfigurationSheet().getMeldeListeHeaderFarbe();
		headerZelle(SPALTE_NR, "column.header.nr", NR_SPALTE_WIDTH, headerFarbe, null);
		headerZelle(SPALTE_VORNAME, "column.header.vorname", NAME_SPALTE_WIDTH, headerFarbe, null);
		headerZelle(SPALTE_NACHNAME, "column.header.nachname", NAME_SPALTE_WIDTH, headerFarbe, null);
		headerZelle(SPALTE_SETZPOSITION, "column.header.setzposition", SETZPOSITION_SPALTE_WIDTH, headerFarbe,
				getSetzpositionKommentar());
		headerZelle(SPALTE_EINGECHECKT, "column.header.melee.eingecheckt", MARKIERUNG_SPALTE_WIDTH, headerFarbe,
				I18n.get("melee.anmeldung.comment.eingecheckt"));
		headerZelle(SPALTE_UEBERNOMMEN, "column.header.melee.uebernommen", MARKIERUNG_SPALTE_WIDTH, headerFarbe,
				I18n.get("melee.anmeldung.comment.uebernommen"));
	}

	private void headerZelle(int spalte, String i18nKey, int breite, int headerFarbe, String kommentar)
			throws GenerateException {
		StringCellValue header = StringCellValue
				.from(getXSpreadSheet(), Position.from(spalte, KOPF_ZEILE), I18n.get(i18nKey))
				.setCellBackColor(headerFarbe).setBorder(HEADER_BORDER).setShrinkToFit(true)
				.setVertJustify(CellVertJustify2.CENTER)
				.addColumnProperties(ColumnProperties.from().setWidth(breite)
						.setHoriJustify(CellHoriJustify.CENTER));
		if (kommentar != null) {
			header.setComment(kommentar);
		}
		getSheetHelper().setStringValueInCell(header);
	}

	private void datenSpaltenFormatieren() throws GenerateException {
		int letzteZeile = letzteFormatierteZeile();
		XSpreadsheet xSheet = getXSpreadSheet();

		getSheetHelper().setPropertiesInRange(xSheet,
				RangePosition.from(SPALTE_NR, ERSTE_DATEN_ZEILE, SPALTE_NR, letzteZeile),
				CellProperties.from().centerJustify().setCharColor(ColorHelper.CHAR_COLOR_GRAY_SPIELER_NR)
						.setBorder(BorderFactory.from().allThin().toBorder()));

		getSheetHelper().setPropertiesInRange(xSheet,
				RangePosition.from(SPALTE_VORNAME, ERSTE_DATEN_ZEILE, LETZTE_SPALTE, letzteZeile),
				CellProperties.from().centerJustify().setShrinkToFit(true)
						.setBorder(BorderFactory.from().allThin().toBorder()));

		SheetHelper.faerbeZeilenAbwechselnd(this,
				RangePosition.from(SPALTE_NR, ERSTE_DATEN_ZEILE, LETZTE_SPALTE, letzteZeile),
				getKonfigurationSheet().getMeldeListeHintergrundFarbeGerade(),
				getKonfigurationSheet().getMeldeListeHintergrundFarbeUnGerade());

		// Fehlerfarbe für ungültige Setzpositionen – identische Regel wie in den Meldelisten.
		MeldeListeHelper.formatiereSetzpositionSpalteFehlerfarbe(this,
				RangePosition.from(SPALTE_SETZPOSITION, ERSTE_DATEN_ZEILE, SPALTE_SETZPOSITION, letzteZeile));

		EditierbaresZelleFormatHelper.anwenden(this,
				RangePosition.from(SPALTE_VORNAME, ERSTE_DATEN_ZEILE, SPALTE_EINGECHECKT, letzteZeile));
	}

	/**
	 * Vergibt fortlaufende Nummern für alle Zeilen mit Namen (blockweise geschrieben). Zeilen ohne
	 * Namen bleiben unberührt, damit die Nummerierung dem Anwender nicht in leere Zeilen „vorläuft".
	 */
	private void nummernSchreiben() throws GenerateException {
		List<MeleeAnmeldungZeile> zeilen = leseAnmeldungen();
		if (zeilen.isEmpty()) {
			return;
		}
		RangeData data = new RangeData();
		for (int idx = 0; idx < zeilen.size(); idx++) {
			data.addNewRow().newInt(idx + 1);
		}
		RangePosition bereich = data.getRangePosition(Position.from(SPALTE_NR, ERSTE_DATEN_ZEILE));
		RangeHelper.from(this, bereich).setDataInRange(data);
	}

	/**
	 * Liest die aktuell erfassten Anmeldungen dieses Sheets.
	 */
	public List<MeleeAnmeldungZeile> leseAnmeldungen() throws GenerateException {
		return MeleeAnmeldungLeser.lesen(getWorkingSpreadsheet(), getXSpreadSheet());
	}

	private int letzteFormatierteZeile() throws GenerateException {
		int anzahl = MeleeAnmeldungLeser.lesen(getWorkingSpreadsheet(), getXSpreadSheet()).size();
		return ERSTE_DATEN_ZEILE + Math.max(MIN_ANZ_ZEILEN, anzahl) - 1;
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				getMetadatenSchluessel(), SheetNamen.meleeAnmeldung());
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	/**
	 * @return {@code true} wenn die Mêlée-Anmeldung für dieses Dokument aktiv geschaltet ist
	 */
	protected final boolean istMeleeAnmeldungAktiv() {
		return MeleeAnmeldungKonfiguration.istAktiv(getKonfigurationSheet());
	}

	// ── system-spezifische Hooks ─────────────────────────────────────────────

	@Override
	protected abstract BaseKonfigurationSheet getKonfigurationSheet();

	/** Named-Range-Schlüssel zur Wiedererkennung des Sheets. */
	protected abstract String getMetadatenSchluessel();

	/**
	 * Erklärender Kommentar an der SP-Spaltenüberschrift – jedes System beschreibt die Wirkung der
	 * Setzposition in seinem eigenen Spielsystem.
	 */
	protected abstract String getSetzpositionKommentar();
}
