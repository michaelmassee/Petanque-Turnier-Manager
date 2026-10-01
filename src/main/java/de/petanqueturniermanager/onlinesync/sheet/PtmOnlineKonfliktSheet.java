/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync.sheet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.AuswahllistenHelper;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Blatt „PTMOnline Konflikte“: die gesammelte Konfliktliste des Abgleichs (A-29). Jeder offene Fall steht in einer
 * Zeile; wo die Turnierleitung wählen kann, trägt sie ihre Wahl in der Spalte „Entscheidung“ ein (Auswahlliste). Der
 * nächste Abgleich wendet die Entscheidungen an und schreibt die Liste neu. Eine Wahl zu einem Fall, der danach noch
 * offen ist, bleibt stehen.
 * <p>
 * Das Blatt kennt nur Texte: Was ein Fall bedeutet, steht im sprachneutralen Schlüssel einer ausgeblendeten Spalte,
 * den der Abgleich selbst vergibt. Im Turnier-Modus ist nur die Entscheidungsspalte editierbar
 * ({@link #editierbarerBereich()}).
 */
public class PtmOnlineKonfliktSheet extends SheetRunner implements ISheet {

	private static final Logger LOGGER = LogManager.getLogger(PtmOnlineKonfliktSheet.class);

	private static final int ZEILE_TITEL = 0;
	private static final int ZEILE_ANLEITUNG = 1;
	private static final int ZEILE_STAND = 2;
	private static final int ZEILE_HEADER = 3;
	private static final int ERSTE_DATEN_ZEILE = 4;
	private static final int MAX_ZEILEN = 999;
	private static final int LETZTE_DATEN_ZEILE = ERSTE_DATEN_ZEILE + MAX_ZEILEN - 1;

	private static final int SPALTE_ART = 0;
	private static final int SPALTE_LOKAL = 1;
	private static final int SPALTE_ONLINE = 2;
	private static final int SPALTE_HINWEIS = 3;
	private static final int SPALTE_ENTSCHEIDUNG = 4;
	private static final int SPALTE_SCHLUESSEL = 5;
	private static final int LETZTE_SICHTBARE_SPALTE = SPALTE_ENTSCHEIDUNG;
	private static final int LETZTE_SPALTE = SPALTE_SCHLUESSEL;

	private static final int BREITE_ART = 4500;
	private static final int BREITE_NAME = 5500;
	private static final int BREITE_HINWEIS = 11000;
	private static final int BREITE_ENTSCHEIDUNG = 5000;
	private static final int TITEL_SCHRIFTGROESSE = 14;
	private static final TableBorder2 HEADER_BORDER = BorderFactory.from().allThin().boldLn().forBottom().toBorder();

	/**
	 * Eine Zeile der Konfliktliste.
	 *
	 * @param schluessel   sprachneutraler, stabiler Schlüssel des Falls
	 * @param optionen     Anzeigetexte der möglichen Entscheidungen; leer für reine Hinweise
	 * @param entscheidung bereits gewählte Entscheidung (Anzeigetext) oder leer
	 */
	public record Zeile(String art, String lokal, String online, String hinweis, String schluessel,
			List<String> optionen, String entscheidung) {

		public Zeile {
			optionen = List.copyOf(optionen);
		}
	}

	public PtmOnlineKonfliktSheet(WorkingSpreadsheet workingSpreadsheet, TurnierSystem turnierSystem) {
		super(workingSpreadsheet, turnierSystem, "PtmOnlineKonflikte");
	}

	/** Bereich, den die Turnierleitung auch im Turnier-Modus bearbeiten darf: die Entscheidungsspalte. */
	public static RangePosition editierbarerBereich() {
		return RangePosition.from(SPALTE_ENTSCHEIDUNG, ERSTE_DATEN_ZEILE, SPALTE_ENTSCHEIDUNG, LETZTE_DATEN_ZEILE);
	}

	@Override
	protected IKonfigurationSheet getKonfigurationSheet() {
		return null;
	}

	@Override
	protected void doRun() throws GenerateException {
		// Wird nur synchron aus dem Abgleich beschrieben.
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				SheetMetadataHelper.SCHLUESSEL_PTM_ONLINE_KONFLIKTE, SheetNamen.ptmOnlineKonflikte());
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	private boolean istVorhanden() {
		return SheetMetadataHelper.findeSheet(getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				SheetMetadataHelper.SCHLUESSEL_PTM_ONLINE_KONFLIKTE).isPresent();
	}

	/** Eingetragene Entscheidungen je Schlüssel (Anzeigetext, nicht leer); leer, wenn es das Blatt nicht gibt. */
	public Map<String, String> leseEntscheidungen() throws GenerateException {
		Map<String, String> ergebnis = new LinkedHashMap<>(leseFaelle());
		ergebnis.values().removeIf(String::isEmpty);
		return ergebnis;
	}

	/**
	 * Schreibt die Liste neu. Ohne offene Fälle wird ein noch nicht vorhandenes Blatt nicht angelegt; ein vorhandenes
	 * wird geleert und meldet „keine offenen Fälle“.
	 *
	 * @param stand Zeitpunkt bzw. Anlass des Abgleichs, als Text für die Kopfzeile
	 */
	public void schreibe(List<Zeile> zeilen, String stand) throws GenerateException {
		if (zeilen.isEmpty() && !istVorhanden()) {
			return;
		}
		if (zeilen.size() > MAX_ZEILEN) {
			LOGGER.warn("PTM-Online: {} offene Fälle, die Konfliktliste zeigt nur {}", zeilen.size(), MAX_ZEILEN);
		}
		List<Zeile> sichtbar = zeilen.size() > MAX_ZEILEN ? zeilen.subList(0, MAX_ZEILEN) : zeilen;
		NewSheet.from(this, SheetNamen.ptmOnlineKonflikte(), SheetMetadataHelper.SCHLUESSEL_PTM_ONLINE_KONFLIKTE)
				.pos(DefaultSheetPos.SUPERMELEE_WORK).useIfExist().create();
		schreibeKopf(stand, sichtbar.isEmpty());
		RangeHelper.from(this, RangePosition.from(SPALTE_ART, ERSTE_DATEN_ZEILE, LETZTE_SPALTE, LETZTE_DATEN_ZEILE))
				.clearRange();
		XSpreadsheet sheet = getXSpreadSheet();
		BlattschutzManager.get().mitEntsperrt(sheet, () -> {
			AuswahllistenHelper.entferneAuswahlliste(sheet, editierbarerBereich());
			return null;
		});
		if (!sichtbar.isEmpty()) {
			RangeData daten = new RangeData();
			sichtbar.forEach(zeile -> {
				RowData row = daten.addNewRow();
				row.newString(zeile.art());
				row.newString(zeile.lokal());
				row.newString(zeile.online());
				row.newString(zeile.hinweis());
				row.newString(zeile.optionen().contains(zeile.entscheidung()) ? zeile.entscheidung() : "");
				row.newString(zeile.schluessel());
			});
			RangeHelper.from(this, daten.getRangePosition(Position.from(SPALTE_ART, ERSTE_DATEN_ZEILE)))
					.setDataInRange(daten);
			setzeAuswahllisten(sheet, sichtbar);
		}
		formatieren(sichtbar.size());
	}

	private void schreibeKopf(String stand, boolean keineFaelle) throws GenerateException {
		RangeData oben = new RangeData();
		oben.addNewRow().newString(SheetNamen.ptmOnlineKonflikte());
		oben.addNewRow().newString(I18n.get("ptmonline.konflikte.anleitung"));
		oben.addNewRow().newString(keineFaelle ? I18n.get("ptmonline.konflikte.keine", stand)
				: I18n.get("ptmonline.konflikte.stand", stand));
		RangeHelper.from(this, oben.getRangePosition(Position.from(SPALTE_ART, ZEILE_TITEL))).setDataInRange(oben);
		RangeData header = new RangeData();
		RowData kopfzeile = header.addNewRow();
		List.of(I18n.get("ptmonline.konflikte.header.art"), I18n.get("ptmonline.konflikte.header.lokal"),
				I18n.get("ptmonline.konflikte.header.online"), I18n.get("ptmonline.konflikte.header.hinweis"),
				I18n.get("ptmonline.konflikte.header.entscheidung"), I18n.get("ptmonline.konflikte.header.schluessel"))
				.forEach(kopfzeile::newString);
		RangeHelper.from(this, header.getRangePosition(Position.from(SPALTE_ART, ZEILE_HEADER)))
				.setDataInRange(header);
	}

	/** Eine Auswahlliste je zusammenhängendem Block von Zeilen mit denselben Optionen. */
	private static void setzeAuswahllisten(XSpreadsheet sheet, List<Zeile> zeilen) throws GenerateException {
		List<int[]> bloecke = new ArrayList<>();
		for (int i = 0; i < zeilen.size(); i++) {
			if (zeilen.get(i).optionen().isEmpty()) {
				continue;
			}
			int[] letzter = bloecke.isEmpty() ? null : bloecke.getLast();
			if (letzter != null && letzter[1] == i - 1
					&& zeilen.get(letzter[0]).optionen().equals(zeilen.get(i).optionen())) {
				letzter[1] = i;
			} else {
				bloecke.add(new int[] { i, i });
			}
		}
		for (int[] block : bloecke) {
			RangePosition bereich = RangePosition.from(SPALTE_ENTSCHEIDUNG, ERSTE_DATEN_ZEILE + block[0],
					SPALTE_ENTSCHEIDUNG, ERSTE_DATEN_ZEILE + block[1]);
			List<String> optionen = zeilen.get(block[0]).optionen();
			BlattschutzManager.get().mitEntsperrt(sheet, () -> {
				AuswahllistenHelper.setzeAuswahlliste(sheet, bereich, optionen);
				return null;
			});
		}
	}

	private void formatieren(int anzahl) throws GenerateException {
		XSpreadsheet sheet = getXSpreadSheet();
		SheetHelper helper = getSheetHelper();
		BlattschutzManager.get().schreibeEntsperrt(sheet, () -> {
			spalte(helper, sheet, SPALTE_ART, BREITE_ART, true);
			spalte(helper, sheet, SPALTE_LOKAL, BREITE_NAME, true);
			spalte(helper, sheet, SPALTE_ONLINE, BREITE_NAME, true);
			spalte(helper, sheet, SPALTE_HINWEIS, BREITE_HINWEIS, true);
			spalte(helper, sheet, SPALTE_ENTSCHEIDUNG, BREITE_ENTSCHEIDUNG, true);
			spalte(helper, sheet, SPALTE_SCHLUESSEL, BREITE_ART, false);
			helper.setPropertiesInRange(sheet, RangePosition.from(SPALTE_ART, ZEILE_TITEL, SPALTE_ART, ZEILE_TITEL),
					CellProperties.from().setCharWeight(FontWeight.BOLD).setCharHeight(TITEL_SCHRIFTGROESSE));
			helper.setPropertiesInRange(sheet,
					RangePosition.from(SPALTE_ART, ZEILE_HEADER, LETZTE_SICHTBARE_SPALTE, ZEILE_HEADER),
					CellProperties.from().setCharWeight(FontWeight.BOLD).setBorder(HEADER_BORDER)
							.setCellBackColor(BasePropertiesSpalte.DEFAULT_HEADER_BACK_COLOR).centerJustify());
			if (anzahl > 0) {
				helper.setPropertiesInRange(sheet,
						RangePosition.from(SPALTE_ART, ERSTE_DATEN_ZEILE, LETZTE_SICHTBARE_SPALTE,
								ERSTE_DATEN_ZEILE + anzahl - 1),
						CellProperties.from().setAllThinBorder().put(ICommonProperties.IS_TEXT_WRAPPED, Boolean.TRUE));
			}
		});
		SheetFreeze.from(sheet, getWorkingSpreadsheet()).anzZeilen(ERSTE_DATEN_ZEILE).doFreeze();
	}

	private static void spalte(SheetHelper helper, XSpreadsheet sheet, int spalte, int breite, boolean sichtbar) {
		helper.setColumnProperties(sheet, spalte, ColumnProperties.from().setWidth(breite).isVisible(sichtbar));
	}

	private static String text(RowData zeile, int spalte) {
		return zeile.size() > spalte ? StringUtils.defaultString(zeile.get(spalte).getStringVal()).strip() : "";
	}

	/** Alle Fälle in Blattreihenfolge: Schlüssel → eingetragene Entscheidung (leer, wenn keine). */
	public Map<String, String> leseFaelle() throws GenerateException {
		Map<String, String> ergebnis = new LinkedHashMap<>();
		if (!istVorhanden()) {
			return ergebnis;
		}
		RangeHelper.from(this, RangePosition.from(SPALTE_ENTSCHEIDUNG, ERSTE_DATEN_ZEILE, SPALTE_SCHLUESSEL,
				LETZTE_DATEN_ZEILE)).getDataFromRange().stream()
				.filter(zeile -> !text(zeile, 1).isEmpty())
				.forEach(zeile -> ergebnis.put(text(zeile, 1), text(zeile, 0)));
		return ergebnis;
	}

	/** Trägt eine Entscheidung ein, wie es die Turnierleitung über die Auswahlliste täte (für Tests). */
	public void setzeEntscheidung(String schluessel, String entscheidung) throws GenerateException {
		List<String> alleSchluessel = new ArrayList<>(leseFaelle().keySet());
		int index = alleSchluessel.indexOf(schluessel);
		if (index < 0) {
			throw new GenerateException("Kein Fall " + schluessel);
		}
		// Direkt in die Zelle wie eine Eingabe der Turnierleitung: die Spalte ist auch im Turnier-Modus editierbar.
		try {
			getXSpreadSheet().getCellByPosition(SPALTE_ENTSCHEIDUNG, ERSTE_DATEN_ZEILE + index).setFormula(entscheidung);
		} catch (com.sun.star.lang.IndexOutOfBoundsException e) {
			throw new GenerateException("Entscheidung konnte nicht eingetragen werden: " + e.getMessage());
		}
	}
}
