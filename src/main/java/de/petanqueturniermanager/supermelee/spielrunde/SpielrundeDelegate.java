package de.petanqueturniermanager.supermelee.spielrunde;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.CHAR_HEIGHT;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.CHAR_WEIGHT;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.HEIGHT;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.HORI_JUSTIFY;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.TABLE_BORDER2;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.VERT_JUSTIFY;

import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.FontWeight;
import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.ConditionOperator;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.SuperMeleePaarungenV2;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeHelper;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.exception.AlgorithmenException;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellstyle.FehlerStyle;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.model.MeleeSpielRunde;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.SuperMeleeTeamRechner;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;

class SpielrundeDelegate implements SpielrundeSheetKonstanten {

	private static final Logger logger = LogManager.getLogger(SpielrundeDelegate.class);

	private final ISpielrundeSheet sheet;
	private final SuperMeleeKonfigurationSheet konfigurationSheet;
	private final MeldeListeSheet_Update meldeListe;
	private final SpielrundeHelper spielrundeHelper;

	SpielrundeDelegate(ISpielrundeSheet sheet, SuperMeleeKonfigurationSheet konfigurationSheet,
			MeldeListeSheet_Update meldeListe) {
		this.sheet = checkNotNull(sheet);
		this.konfigurationSheet = checkNotNull(konfigurationSheet);
		this.meldeListe = checkNotNull(meldeListe);
		this.spielrundeHelper = new SpielrundeHelper(sheet,
				konfigurationSheet.getSpielRundeHintergrundFarbeGeradeStyle(),
				konfigurationSheet.getSpielRundeHintergrundFarbeUnGeradeStyle());
	}

	SuperMeleeKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	MeldeListeSheet_Update getMeldeListe() {
		meldeListe.setSpielTag(sheet.getSpielTag());
		return meldeListe;
	}

	String getSheetName(SpielTagNr spieltag, SpielRundeNr spielrunde) {
		return SpielrundeSheetKonstanten.sheetName(spieltag.getNr(), spielrunde.getNr());
	}

	boolean canStart(SpielerMeldungen meldungen) throws GenerateException {
		if (sheet.getSpielRundeNr().getNr() < 1) {
			sheet.getSheetHelper().setActiveSheet(getMeldeListe().getXSpreadSheet());
			String errorMsg = "Ungültige Spielrunde in der Meldeliste '" + sheet.getSpielRundeNr().getNr() + "'";
			MessageBox.from(sheet.getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("msg.caption.aktuelle.spielrunde.fehler"))
					.message(errorMsg).show();
			return false;
		}
		if (meldungen.size() < 6) {
			sheet.getSheetHelper().setActiveSheet(getMeldeListe().getXSpreadSheet());
			String errorMsg = "Ungültige Anzahl '" + meldungen.size() + "' von Aktive Meldungen vorhanden."
					+ "\r\nFür Spieltag " + sheet.getSpielTag().getNr() + " mindestens 6 Meldungen aktivieren.";
			MessageBox.from(sheet.getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("msg.caption.aktuelle.spielrunde.fehler"))
					.message(errorMsg).show();
			return false;
		}
		return true;
	}

	boolean neueSpielrunde(SpielerMeldungen meldungen, SpielRundeNr neueSpielrundeNr) throws GenerateException {
		return neueSpielrunde(meldungen, neueSpielrundeNr, sheet.isForceOk());
	}

	boolean neueSpielrunde(SpielerMeldungen meldungen, SpielRundeNr neueSpielrundeNr, boolean force)
			throws GenerateException {
		checkNotNull(meldungen);

		sheet.processBoxinfo("processbox.spielrunde.neue", neueSpielrundeNr.getNr(), sheet.getSpielTag().getNr());
		sheet.processBoxinfo("processbox.spielrunde.meldungen", meldungen.size());

		SuperMeleeMode superMeleeMode = konfigurationSheet.getSuperMeleeMode();
		SuperMeleeTeamRechner superMeleeTeamRechner = new SuperMeleeTeamRechner(meldungen.spieler().size(),
				superMeleeMode);

		if (!superMeleeTeamRechner.valideAnzahlSpieler()) {
			MessageBox.from(sheet.getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("msg.caption.neue.spielrunde"))
					.message(I18n.get("msg.text.ungueltige.anzahl.spieler", superMeleeTeamRechner.getAnzSpieler()))
					.show();
			return false;
		}

		sheet.setSpielRundeNr(neueSpielrundeNr);

		if (meldungen.spieler().size() < 4) {
			throw new GenerateException(I18n.get("error.spielrunde.erstellen", sheet.getSpielTag().getNr(), neueSpielrundeNr.getNr(), meldungen.spieler().size()));
		}
		if (sheet.getSheetHelper().findByName(getSheetName(sheet.getSpielTag(), sheet.getSpielRundeNr())) != null) {
			String msg = "Erstelle für Spieltag " + sheet.getSpielTag().getNr() + "\r\nSpielrunde "
					+ neueSpielrundeNr.getNr() + "\r\nneine neue Spielrunde";
			MessageBoxResult msgBoxRslt = MessageBox.from(sheet.getxContext(), MessageBoxTypeEnum.QUESTION_OK_CANCEL)
					.forceOk(force).caption(I18n.get("msg.caption.neue.spielrunde")).message(msg).show();
			if (MessageBoxResult.CANCEL == msgBoxRslt) {
				ProcessBox.from().info("Abbruch vom Benutzer, Spielrunde wurde nicht erstellt");
				return false;
			}
		}
		var neuesSheet = NewSheet
				.from(sheet, getSheetName(sheet.getSpielTag(), sheet.getSpielRundeNr()),
						SheetMetadataHelper.schluesselSupermeleeSpielrunde(sheet.getSpielTag().getNr(),
								sheet.getSpielRundeNr().getNr()))
				.pos(DefaultSheetPos.SUPERMELEE_WORK).spielTagPageStyle(sheet.getSpielTag()).setForceCreate(force)
				.setActiv().hideGrid().create();
		if (!neuesSheet.isDidCreate()) {
			ProcessBox.from().info("Abbruch vom Benutzer, Spielrunde wurde nicht erstellt");
			return false;
		}

		konfigurationSheet.setAktiveSpielRunde(sheet.getSpielRundeNr());

		boolean doubletteRunde = false;
		boolean tripletteRunde = false;
		boolean isKannNurDoublette = false;
		if (konfigurationSheet.getGleichePaarungenAktiv()) {
			isKannNurDoublette = superMeleeTeamRechner.isNurDoubletteMoeglich();
		}
		if (superMeleeMode == SuperMeleeMode.Triplette && isKannNurDoublette) {
			MessageBox msgbox = MessageBox.from(sheet.getxContext(), MessageBoxTypeEnum.QUESTION_YES_NO).forceOk(force)
					.caption(I18n.get("msg.caption.spielrunde.doublette"));
			msgbox.message(I18n.get("msg.text.spielrunde.doublette",
					sheet.getSpielTag().getNr(), neueSpielrundeNr.getNr()));
			if (MessageBoxResult.YES == msgbox.show()) {
				doubletteRunde = true;
			}
		} else if (superMeleeMode == SuperMeleeMode.Doublette && isKannNurDoublette) {
			doubletteRunde = true;
		}

		if (force && isKannNurDoublette) {
			doubletteRunde = true;
		}

		SuperMeleePaarungenV2 paarungen = new SuperMeleePaarungenV2();
		try {
			MeleeSpielRunde meleeSpielRunde;
			if (superMeleeMode == SuperMeleeMode.Triplette) {
				meleeSpielRunde = paarungen.neueSpielrundeTripletteMode(neueSpielrundeNr.getNr(), meldungen,
						doubletteRunde);
			} else {
				meleeSpielRunde = paarungen.neueSpielrundeDoubletteMode(neueSpielrundeNr.getNr(), meldungen,
						tripletteRunde);
			}

			meleeSpielRunde.validateSpielerTeam(null);
			headerPaarungen(sheet.getXSpreadSheet(), meleeSpielRunde);
			headerSpielerNr(sheet.getXSpreadSheet());
			spielerNummerEinfuegen(meleeSpielRunde);
			vertikaleErgbnisseFormulaEinfuegen(meleeSpielRunde);
			datenErsteSpalte();
			datenformatieren(sheet.getXSpreadSheet());
			spielrundeProperties(sheet.getXSpreadSheet());
			wennNurDoubletteRundeDannSpaltenAusblenden(sheet.getXSpreadSheet(), doubletteRunde);
			printBereichDefinieren(sheet.getXSpreadSheet());
			if (konfigurationSheet.getSpielrundePlan()) {
				new SpielrundePlan(sheet.getWorkingSpreadsheet()).generate();
			}

		} catch (AlgorithmenException e) {
			logger.error(e.getMessage(), e);
			sheet.getSheetHelper().setActiveSheet(getMeldeListe().getXSpreadSheet());
			sheet.getSheetHelper().removeSheet(getSheetName(sheet.getSpielTag(), sheet.getSpielRundeNr()));
			konfigurationSheet.setAktiveSpielRunde(SpielRundeNr.from(sheet.getSpielRundeNr().getNr() - 1));
			MessageBox.from(sheet.getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("msg.caption.fehler.auslosen"))
					.message(e.getMessage()).show();
			throw new RuntimeException(e);
		}
		return true;
	}

	void gespieltenRundenEinlesen(SpielerMeldungen aktiveMeldungen, int abSpielrunde, int bisSpielrunde)
			throws GenerateException {
		SpielTagNr aktuelleSpielTag = sheet.getSpielTag();

		Integer maxAnzGespielteSpieltage = sheet.getMaxAnzGespielteSpieltage();
		int bisVergangeneSpieltag = aktuelleSpielTag.getNr() - 1 - maxAnzGespielteSpieltage;
		if (bisVergangeneSpieltag < 0) {
			bisVergangeneSpieltag = 0;
		}

		for (int vergangeneSpieltag = aktuelleSpielTag.getNr()
				- 1; vergangeneSpieltag > bisVergangeneSpieltag; vergangeneSpieltag--) {
			gespieltenRundenEinlesen(aktiveMeldungen, SpielTagNr.from(vergangeneSpieltag), 1, 999);
		}
		gespieltenRundenEinlesen(aktiveMeldungen, aktuelleSpielTag, abSpielrunde, bisSpielrunde);
	}

	private void gespieltenRundenEinlesen(SpielerMeldungen aktiveMeldungen, SpielTagNr spielTagNr, int abSpielrunde,
			int bisSpielrunde) throws GenerateException {
		int spielrunde = 1;

		if (bisSpielrunde < abSpielrunde || bisSpielrunde < 1) {
			return;
		}
		if (abSpielrunde > 1) {
			spielrunde = abSpielrunde;
		}

		sheet.processBoxinfo("processbox.meldungen.gespielter.runden.einlesen", spielTagNr.getNr());
		var xDoc = sheet.getWorkingSpreadsheet().getWorkingSpreadsheetDocument();

		for (; spielrunde <= bisSpielrunde; spielrunde++) {
			SheetRunner.testDoCancelTask();
			// Iterations-Lookup: Metadaten-first (überlebt Umbenennung), Fallback auf Namen
			XSpreadsheet xsheet = SheetMetadataHelper.findeSheetUndHeile(xDoc,
					SheetMetadataHelper.schluesselSupermeleeSpielrunde(spielTagNr.getNr(), spielrunde),
					getSheetName(spielTagNr, SpielRundeNr.from(spielrunde)));

			if (xsheet == null) {
				continue;
			}
			Position pospielerNr = Position.from(ERSTE_SPIELERNR_SPALTE, ERSTE_DATEN_ZEILE);

			boolean zeileIstLeer = false;
			int maxcntr = 999;
			while (!zeileIstLeer && maxcntr > 0) {
				maxcntr--;
				for (int teamCntr = 1; teamCntr <= 2; teamCntr++) {
					Team team = Team.from(1);
					for (int spielerCntr = 1; spielerCntr <= 3; spielerCntr++) {
						pospielerNr.spalte(ERSTE_SPIELERNR_SPALTE + ((teamCntr - 1) * 3) + spielerCntr - 1);
						int spielerNr = sheet.getSheetHelper().getIntFromCell(xsheet, pospielerNr);
						if (spielerNr > 0) {
							Spieler spieler = aktiveMeldungen.findSpielerByNr(spielerNr);
							if (spieler != null) {
								try {
									team.addSpielerWennNichtVorhanden(spieler);
								} catch (AlgorithmenException e) {
									logger.error(e.getMessage(), e);
									throw new GenerateException(I18n.get("error.spielrunde.einlesen"));
								}
							}
						}
					}
				}
				pospielerNr.zeilePlusEins().spalte(PAARUNG_CNTR_SPALTE);
				if (sheet.getSheetHelper().getIntFromCell(xsheet, pospielerNr) == -1) {
					zeileIstLeer = true;
				}
			}
		}
	}

	Position letztePositionRechtsUnten() throws GenerateException {
		Position spielerNrPos = Position.from(ERSTE_SPIELERNR_SPALTE, ERSTE_DATEN_ZEILE);

		if (sheet.getSheetHelper().getIntFromCell(sheet, spielerNrPos) == -1) {
			return null;
		}

		RangePosition erstSpielrNrRange = RangePosition.from(ERSTE_SPIELERNR_SPALTE, ERSTE_DATEN_ZEILE,
				ERSTE_SPIELERNR_SPALTE, ERSTE_DATEN_ZEILE + 999);

		RangeData nrDaten = RangeHelper.from(sheet, erstSpielrNrRange).getDataFromRange();
		int index = IntStream.range(0, nrDaten.size())
				.filter(nrDatenIdx -> nrDaten.get(nrDatenIdx).get(0).getIntVal(-1) == -1).findFirst().orElse(-1);
		if (index > 0) {
			spielerNrPos.zeilePlus(index - 1);
		}

		return spielerNrPos.spalte(ERSTE_SPALTE_ERGEBNISSE + 1);
	}

	Integer getMaxAnzGespielteSpieltage() throws GenerateException {
		return konfigurationSheet.getMaxAnzGespielteSpieltage();
	}

	void clearSheet() throws GenerateException {
		Position letzteZeile = letztePositionRechtsUnten();
		if (letzteZeile == null) {
			return;
		}
		RangePosition rangPos = RangePosition.from(NUMMER_SPALTE_RUNDESPIELPLAN, ERSTE_DATEN_ZEILE, LETZTE_SPALTE,
				letzteZeile.getZeile());
		RangeHelper.from(sheet, rangPos).clearRange();
	}

	// -------------------------------------------------------------------------
	// Private Hilfsmethoden (nur intern in neueSpielrunde aufgerufen)
	// -------------------------------------------------------------------------

	private void vertikaleErgbnisseFormulaEinfuegen(MeleeSpielRunde spielRunde) throws GenerateException {
		sheet.processBoxinfo("processbox.spielrunde.ergebnisse");
		checkArgument(spielRunde.getNr() == sheet.getSpielRundeNr().getNr());

		XSpreadsheet xsheet = sheet.getXSpreadSheet();
		Position posSpielrNr = Position.from(ERSTE_SPIELERNR_SPALTE, ERSTE_DATEN_ZEILE);
		Position posSpielrNrFormula = Position.from(ERSTE_SPALTE_VERTIKALE_ERGEBNISSE, ERSTE_DATEN_ZEILE);
		StringCellValue spielrNrFormula = StringCellValue.from(xsheet, posSpielrNrFormula);
		Position ergebnisPosA = Position.from(ERSTE_SPALTE_ERGEBNISSE, ERSTE_DATEN_ZEILE);
		Position ergebnisPosB = Position.from(ERSTE_SPALTE_ERGEBNISSE + 1, ERSTE_DATEN_ZEILE);

		List<Team> teams = spielRunde.teams();

		for (int teamCntr = 0; teamCntr < teams.size(); teamCntr++) {
			if ((teamCntr & 1) == 0) {
				for (int spielrNr = 1; spielrNr <= 3; spielrNr++) {
					vertikaleErgbnisseEinezeileEinfuegen(posSpielrNr, spielrNrFormula);
				}
			} else {
				for (int spielrNr = 1; spielrNr <= 3; spielrNr++) {
					vertikaleErgbnisseEinezeileEinfuegen(posSpielrNr, spielrNrFormula);
				}
				posSpielrNr.zeilePlusEins().spalte(ERSTE_SPIELERNR_SPALTE);
				ergebnisPosA.zeilePlusEins();
				ergebnisPosB.zeilePlusEins();
			}
		}

		int letzteZeile = (ERSTE_DATEN_ZEILE + (teams.size() * 3)) - 1;
		String plusminusFormula = "IF(INDIRECT(ADDRESS(ROW();%d;4;1))>0;INDIRECT(ADDRESS(((ROW( )+3)/6)+2;IF(ISODD(ROUNDDOWN((ROW())/3)+2);%d;%d);4;1));\"\")";
		String plusPunkteFormula = String.format(plusminusFormula,
				ERSTE_SPALTE_VERTIKALE_ERGEBNISSE + 1,
				ERSTE_SPALTE_ERGEBNISSE + 1,
				ERSTE_SPALTE_ERGEBNISSE + 2);
		//@formatter:off
		StringCellValue plusSpalteFormula = StringCellValue.from(sheet.getXSpreadSheet())
				.zeile(ERSTE_DATEN_ZEILE)
				.spalte(SPALTE_VERTIKALE_ERGEBNISSE_PLUS)
				.setValue(plusPunkteFormula)
				.setFillAutoDown(letzteZeile);
		//@formatter:on
		sheet.getSheetHelper().setFormulaInCell(plusSpalteFormula);

		String minusPunkteFormula = String.format(plusminusFormula,
				ERSTE_SPALTE_VERTIKALE_ERGEBNISSE + 1,
				ERSTE_SPALTE_ERGEBNISSE + 2,
				ERSTE_SPALTE_ERGEBNISSE + 1);
		//@formatter:off
		StringCellValue minusSpalteFormula = StringCellValue.from(sheet.getXSpreadSheet())
				.zeile(ERSTE_DATEN_ZEILE)
				.spalte(SPALTE_VERTIKALE_ERGEBNISSE_MINUS)
				.setValue(minusPunkteFormula)
				.setFillAutoDown(letzteZeile);
		//@formatter:on
		sheet.getSheetHelper().setFormulaInCell(minusSpalteFormula);

		//@formatter:off
		StringCellValue teamSpalteFormula = StringCellValue.from(sheet.getXSpreadSheet())
				.zeile(ERSTE_DATEN_ZEILE)
				.spalte(SPALTE_VERTIKALE_ERGEBNISSE_AB)
				.setValue("IF(ISODD(ROUNDDOWN((ROW())/3)+2);\"A\";\"B\")")
				.setFillAutoDown(letzteZeile);
		//@formatter:on
		sheet.getSheetHelper().setFormulaInCell(teamSpalteFormula);

		//@formatter:off
		StringCellValue bahnSpalteFormula = StringCellValue.from(sheet.getXSpreadSheet())
				.zeile(ERSTE_DATEN_ZEILE)
				.spalte(SPALTE_VERTIKALE_ERGEBNISSE_BA_NR)
				.setValue("INDIRECT( ADDRESS(ROUNDDOWN((ROW( )+3) /6)+2;" + (NUMMER_SPALTE_RUNDESPIELPLAN + 1) + ";4;1))")
				.setFillAutoDown(letzteZeile);
		//@formatter:on
		sheet.getSheetHelper().setFormulaInCell(bahnSpalteFormula);

		Position ersteHeaderZeile = Position.from(ERSTE_SPALTE_VERTIKALE_ERGEBNISSE, ZWEITE_HEADER_ZEILE);
		ColumnProperties columnProperties = ColumnProperties.from()
				.setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH).setHoriJustify(CellHoriJustify.CENTER)
				.setVertJustify(CellVertJustify2.CENTER).isVisible(konfigurationSheet.zeigeArbeitsSpalten());
		StringCellValue headerText = StringCellValue.from(xsheet, ersteHeaderZeile)
				.addColumnProperties(columnProperties);
		sheet.getSheetHelper().setStringValueInCell(headerText.setValue("Nr"));
		sheet.getSheetHelper().setStringValueInCell(
				headerText.setValue("+").spalte(SPALTE_VERTIKALE_ERGEBNISSE_PLUS).setComment(I18n.get("supermelee.spielrunde.comment.punkte.plus")));
		sheet.getSheetHelper().setStringValueInCell(
				headerText.setValue("-").spalte(SPALTE_VERTIKALE_ERGEBNISSE_MINUS).setComment(I18n.get("supermelee.spielrunde.comment.punkte.minus")));
		sheet.getSheetHelper().setStringValueInCell(
				headerText.setValue("Tm").spalte(SPALTE_VERTIKALE_ERGEBNISSE_AB).setComment(I18n.get("supermelee.spielrunde.comment.mannschaft")));
		sheet.getSheetHelper().setStringValueInCell(
				headerText.setValue("Ba").spalte(SPALTE_VERTIKALE_ERGEBNISSE_BA_NR).setComment(I18n.get("supermelee.spielrunde.comment.spielbahn.nr")));
	}

	private void vertikaleErgbnisseEinezeileEinfuegen(Position posSpielrNr, StringCellValue spielrNrFormula)
			throws GenerateException {
		sheet.getSheetHelper().setFormulaInCell(spielrNrFormula.setValue(posSpielrNr.getAddressWith$()));
		posSpielrNr.spaltePlusEins();
		spielrNrFormula.zeilePlusEins();
	}

	private void datenErsteSpalte() throws GenerateException {
		Integer headerColor = konfigurationSheet.getSpielRundeHeaderFarbe();
		Integer letzteZeile = letztePositionRechtsUnten().getZeile();
		SpielrundeSpielbahn spielrundeSpielbahn = konfigurationSheet.getSpielrundeSpielbahn();

		spielrundeHelper.datenErsteSpalte(spielrundeSpielbahn, ERSTE_DATEN_ZEILE, letzteZeile,
				NUMMER_SPALTE_RUNDESPIELPLAN, ERSTE_HEADER_ZEILE, ZWEITE_HEADER_ZEILE, headerColor);
	}

	private void spielerNummerEinfuegen(MeleeSpielRunde spielRunde) throws GenerateException {
		sheet.processBoxinfo("processbox.spielrunde.einfuegen");
		checkArgument(spielRunde.getNr() == sheet.getSpielRundeNr().getNr());

		HashSet<Integer> spielrNr = new HashSet<>();
		XSpreadsheet xsheet = sheet.getXSpreadSheet();
		Position posErsteSpielrNr = Position.from(ERSTE_SPIELERNR_SPALTE, ERSTE_DATEN_ZEILE - 1);

		NumberCellValue numberCellValue = NumberCellValue
				.from(xsheet, posErsteSpielrNr, MeldungenSpalte.MAX_ANZ_MELDUNGEN)
				.addCellProperty(VERT_JUSTIFY, CellVertJustify2.CENTER);

		StringCellValue validateCellVal = StringCellValue.from(numberCellValue).spalte(EINGABE_VALIDIERUNG_SPALTE)
				.setCharColor(ColorHelper.CHAR_COLOR_RED).setCharWeight(FontWeight.BOLD).setCharHeight(14)
				.setHoriJustify(CellHoriJustify.CENTER);

		List<Team> teams = spielRunde.teams();
		for (int teamCntr = 0; teamCntr < teams.size(); teamCntr++) {
			if ((teamCntr & 1) == 0) {
				numberCellValue.zeilePlusEins();
				numberCellValue.getPos().spalte(ERSTE_SPIELERNR_SPALTE);

				StringCellValue formulaCellValue = StringCellValue.from(numberCellValue).spalte(PAARUNG_CNTR_SPALTE)
						.setValue("=ROW()-" + ERSTE_DATEN_ZEILE);
				sheet.getSheetHelper().setFormulaInCell(formulaCellValue);

				String ergA = Position.from(numberCellValue.getPos()).spalte(ERSTE_SPALTE_ERGEBNISSE).getAddress();
				String ergB = Position.from(numberCellValue.getPos()).spalte(ERSTE_SPALTE_ERGEBNISSE + 1).getAddress();
				// @formatter:off
			    String valFormula = "IF(" +
			    		"OR(" +
						"AND(" +
			    		"ISBLANK(" + ergA + ");" +
			    		"ISBLANK(" + ergB + ")" +
						")" +
						";" +
						"AND(" +
						ergA + "< 14;" +
						ergB + "< 14;" +
						ergA + ">-1;" +
						ergB + ">-1;" +
						ergA + "<>"+ ergB +
						")" +
						")" +
						";\"\";\"FEHLER\")";
				// @formatter:on
				validateCellVal.setValue(valFormula).zeile(numberCellValue.getPos().getZeile());
				sheet.getSheetHelper().setFormulaInCell(validateCellVal);
			} else {
				numberCellValue.getPos().spalte(ERSTE_SPIELERNR_SPALTE + 3);
			}
			for (Spieler spieler : teams.get(teamCntr).spieler()) {
				if (spielrNr.contains(spieler.getNr())) {
					logger.error("Doppelte Spieler in Runde ");
					logger.error("Spieler:" + spieler);
					logger.error("Runde:" + spielRunde);
					throw new RuntimeException("Doppelte Spieler in Runde " + spielRunde + " Spieler " + spieler);
				}
				sheet.getSheetHelper().setNumberValueInCell(numberCellValue.setValue((double) spieler.getNr()));
				verweisAufSpielerNamenEinfuegen(numberCellValue.getPos());
				numberCellValue.spaltePlusEins();
				spielrNr.add(spieler.getNr());
			}
			if (teams.get(teamCntr).spieler().size() == 2) {
				verweisAufSpielerNamenEinfuegen(numberCellValue.getPos());
			}
		}

		FehlerStyle fehlerStyle = new FehlerStyle();
		RangePosition datenRange = RangePosition.from(Position.from(ERSTE_SPIELERNR_SPALTE, ERSTE_DATEN_ZEILE),
				Position.from(ERSTE_SPIELERNR_SPALTE + 5, numberCellValue.getPos().getZeile()));
		Position posSpalteSpielrNr = Position.from(ERSTE_SPALTE_VERTIKALE_ERGEBNISSE, ERSTE_DATEN_ZEILE);
		String conditionfindDoppelt = "COUNTIF(" + posSpalteSpielrNr.getSpalteAddressWith$() + ";"
				+ ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")>1";
		String conditionNotEmpty = ConditionalFormatHelper.FORMULA_CURRENT_CELL + "<>\"\"";
		String formulaFindDoppelteSpielrNr = "AND(" + conditionfindDoppelt + ";" + conditionNotEmpty + ")";
		ConditionalFormatHelper.from(sheet, datenRange).clear().formula1(formulaFindDoppelteSpielrNr)
				.operator(ConditionOperator.FORMULA).style(fehlerStyle).applyAndDoReset();
	}

	private void verweisAufSpielerNamenEinfuegen(Position spielerNrPos) throws GenerateException {
		int anzSpaltenDiv = ERSTE_SPIELERNR_SPALTE - ERSTE_SPALTE_RUNDESPIELPLAN;
		String spielerNrAddress = spielerNrPos.getAddress();
		String formulaVerweis = "IFNA(" + getMeldeListe().formulaSverweisSpielernamen(spielerNrAddress) + ";\"\")";

		StringCellValue val = StringCellValue
				.from(sheet.getXSpreadSheet(), Position.from(spielerNrPos).spaltePlus(-anzSpaltenDiv), formulaVerweis)
				.setVertJustify(CellVertJustify2.CENTER).setShrinkToFit(true).setCharHeight(12);
		sheet.getSheetHelper().setFormulaInCell(val);
	}

	private void headerPaarungen(XSpreadsheet xsheet, MeleeSpielRunde spielRunde) throws GenerateException {
		sheet.processBoxinfo("processbox.spielrunde.paarungen");
		checkArgument(spielRunde.getNr() == sheet.getSpielRundeNr().getNr());

		SpielTagNr spieltag = sheet.getSpielTag();
		Position ersteHeaderZeile = Position.from(ERSTE_SPALTE_RUNDESPIELPLAN, ERSTE_HEADER_ZEILE);
		Position ersteHeaderZeileMerge = Position.from(ersteHeaderZeile).spalte(ERSTE_SPALTE_ERGEBNISSE - 1);

		Integer headerFarbe = konfigurationSheet.getSpielRundeHeaderFarbe();

		String ersteHeader = "Spielrunde " + spielRunde.getNr();
		if (konfigurationSheet.getSpielrunde1Header()) {
			ersteHeader = spieltag.getNr() + ". Spieltag - " + ersteHeader;
		}

		StringCellValue headerVal = StringCellValue.from(xsheet, ersteHeaderZeile, ersteHeader)
				.addCellProperty(CHAR_WEIGHT, FontWeight.BOLD).setEndPosMerge(ersteHeaderZeileMerge)
				.addCellProperty(HORI_JUSTIFY, CellHoriJustify.CENTER)
				.addCellProperty(TABLE_BORDER2, BorderFactory.from().allThin().toBorder()).setCharHeight(13)
				.setVertJustify(CellVertJustify2.CENTER).setCellBackColor(headerFarbe);
		sheet.getSheetHelper().setStringValueInCell(headerVal);

		Position posSpielerNamen = Position.from(ERSTE_SPALTE_RUNDESPIELPLAN, ZWEITE_HEADER_ZEILE);
		Position posSpielerNamenMerge = Position.from(posSpielerNamen).spaltePlus(2);

		headerVal.setValue(I18n.get("column.header.mannschaft.a")).setPos(posSpielerNamen).setEndPosMerge(posSpielerNamenMerge)
				.addCellProperty(TABLE_BORDER2, BorderFactory.from().allThin().doubleLn().forRight().toBorder());
		sheet.getSheetHelper().setStringValueInCell(headerVal);
		headerVal.setValue(I18n.get("column.header.mannschaft.b")).setPos(posSpielerNamen.spaltePlus(3))
				.setEndPosMerge(posSpielerNamenMerge.spaltePlus(3))
				.addCellProperty(TABLE_BORDER2, BorderFactory.from().allThin().toBorder());
		sheet.getSheetHelper().setStringValueInCell(headerVal);

		Position posSpielerSpalte = Position.from(ERSTE_SPALTE_RUNDESPIELPLAN, ERSTE_DATEN_ZEILE - 1);
		for (int i = 1; i <= 6; i++) {
			sheet.getSheetHelper().setColumnWidth(xsheet, posSpielerSpalte, 4000);
			sheet.getSheetHelper().setColumnCellHoriJustify(xsheet, posSpielerSpalte, CellHoriJustify.CENTER);
			posSpielerSpalte.spaltePlusEins();
		}

		Position ergebnis = Position.from(ERSTE_SPALTE_ERGEBNISSE, ERSTE_DATEN_ZEILE - 1);
		headerVal.setValue(I18n.get("column.header.ergebnis")).setPos(ergebnis).setEndPosMerge(Position.from(ergebnis).spaltePlusEins());
		sheet.getSheetHelper().setStringValueInCell(headerVal);

		for (int i = 1; i <= 2; i++) {
			XPropertySet xpropSet = sheet.getSheetHelper().setColumnWidth(xsheet, ergebnis, 1800);
			sheet.getSheetHelper().setProperty(xpropSet, HORI_JUSTIFY, CellHoriJustify.CENTER);
			sheet.getSheetHelper().setProperty(xpropSet, VERT_JUSTIFY, CellVertJustify2.CENTER);
			sheet.getSheetHelper().setProperty(xpropSet, CHAR_WEIGHT, FontWeight.BOLD);
			ergebnis.spaltePlusEins();
		}
	}

	private void headerSpielerNr(XSpreadsheet xsheet) throws GenerateException {
		sheet.processBoxinfo("processbox.formatiere.header");
		Position pos = Position.from(ERSTE_SPIELERNR_SPALTE - 1, ERSTE_DATEN_ZEILE - 1);
		ColumnProperties columnProperties = ColumnProperties.from().setWidth(800).setHoriJustify(CellHoriJustify.CENTER)
				.isVisible(konfigurationSheet.zeigeArbeitsSpalten());
		StringCellValue headerCelVal = StringCellValue.from(xsheet, Position.from(pos), "#")
				.addColumnProperties(columnProperties);
		sheet.getSheetHelper().setStringValueInCell(headerCelVal);
		headerCelVal.spaltePlusEins();

		for (int a = 1; a <= 3; a++) {
			sheet.getSheetHelper().setStringValueInCell(headerCelVal.setValue("A" + a));
			headerCelVal.spaltePlusEins();
		}
		for (int b = 1; b <= 3; b++) {
			sheet.getSheetHelper().setStringValueInCell(headerCelVal.setValue("B" + b));
			headerCelVal.spaltePlusEins();
		}
	}

	private void printBereichDefinieren(XSpreadsheet xsheet) throws GenerateException {
		sheet.processBoxinfo("processbox.print.bereich");
		Position letzteZeile = letztePositionRechtsUnten();
		PrintArea.from(xsheet, sheet.getWorkingSpreadsheet())
				.setPrintArea(RangePosition.from(NUMMER_SPALTE_RUNDESPIELPLAN, ERSTE_HEADER_ZEILE, letzteZeile));
	}

	private void wennNurDoubletteRundeDannSpaltenAusblenden(XSpreadsheet xsheet, boolean doubletteRunde)
			throws GenerateException {
		if (doubletteRunde) {
			sheet.processBoxinfo("processbox.meldungen.ausblenden");
			sheet.getSheetHelper().setColumnProperty(xsheet, ERSTE_SPALTE_RUNDESPIELPLAN + 2, "IsVisible", false);
			sheet.getSheetHelper().setColumnProperty(xsheet, ERSTE_SPALTE_RUNDESPIELPLAN + 5, "IsVisible", false);
		}
	}

	private void spielrundeProperties(XSpreadsheet xsheet) throws GenerateException {
		sheet.processBoxinfo("processbox.spielrunde.validieren");

		Position datenEnd = letztePositionRechtsUnten();

		CellProperties cellPropBez = CellProperties.from().margin(150).setHoriJustify(CellHoriJustify.RIGHT)
				.setVertJustify(CellVertJustify2.CENTER).setBorder(BorderFactory.from().allThin().toBorder());

		StringCellValue propName = StringCellValue
				.from(xsheet, Position.from(NUMMER_SPALTE_RUNDESPIELPLAN + 1, datenEnd.getZeile() + 1))
				.setCellProperties(cellPropBez);
		propName.zeilePlus(2);

		NumberCellValue propVal = NumberCellValue.from(propName).spaltePlusEins().setHoriJustify(CellHoriJustify.LEFT)
				.setBorder(BorderFactory.from().allThin().toBorder());

		int anzAktiv = meldeListe.getAnzahlAktiveSpieler(sheet.getSpielTag());
		sheet.getSheetHelper()
				.setStringValueInCell(propName.setValue("Aktiv :").setComment(I18n.get("supermelee.spielrunde.comment.anzahl.spieler")));
		sheet.getSheetHelper().setNumberValueInCell(propVal.setValue((double) anzAktiv));

		int anzAusg = meldeListe.getAusgestiegenSpieler(sheet.getSpielTag());
		sheet.getSheetHelper().setStringValueInCell(propName.zeilePlusEins()
				.setValue(I18n.get("column.header.ausgestiegen") + " :")
				.setComment(I18n.get("supermelee.spielrunde.comment.anzahl.ausgestiegen")));
		sheet.getSheetHelper().setNumberValueInCell(propVal.zeilePlusEins().setValue((double) anzAusg));

		SuperMeleeMode superMeleeMode = konfigurationSheet.getSuperMeleeMode();
		sheet.getSheetHelper()
				.setStringValueInCell(propName.zeilePlusEins().setValue("Modus :").setComment(I18n.get("supermelee.spielrunde.comment.modus")));
		sheet.getSheetHelper()
				.setStringValueInCell(StringCellValue.from(propVal).zeilePlusEins().setValue(superMeleeMode.name()));
	}

	private void datenformatieren(XSpreadsheet xsheet) throws GenerateException {
		sheet.processBoxinfo("processbox.formatiere.daten");

		Position datenStartOhneNrSpalte = Position.from(ERSTE_SPALTE_RUNDESPIELPLAN, ERSTE_DATEN_ZEILE);
		Position datenEnd = letztePositionRechtsUnten();

		RangePosition datenRangeErsteHaelfteOhneNummer = RangePosition.from(datenStartOhneNrSpalte,
				Position.from(ERSTE_SPALTE_RUNDESPIELPLAN + 2, datenEnd.getZeile()));
		TableBorder2 border = BorderFactory.from().allThin().boldLn().forTop().toBorder();
		sheet.getSheetHelper().setPropertyInRange(xsheet, datenRangeErsteHaelfteOhneNummer, TABLE_BORDER2, border);

		RangePosition datenRangeZweiteHaelfte = RangePosition
				.from(Position.from(ERSTE_SPALTE_RUNDESPIELPLAN + 3, ERSTE_DATEN_ZEILE), datenEnd);
		border = BorderFactory.from().allThin().boldLn().forTop().doubleLn().forLeft().toBorder();
		sheet.getSheetHelper().setPropertyInRange(xsheet, datenRangeZweiteHaelfte, TABLE_BORDER2, border);

		for (int zeileCntr = ERSTE_HEADER_ZEILE; zeileCntr <= datenEnd.getZeile(); zeileCntr++) {
			sheet.getSheetHelper().setRowProperty(xsheet, zeileCntr, HEIGHT, 800);
		}

		RangePosition datenRangeSpielpaarungen = RangePosition.from(
				Position.from(ERSTE_SPALTE_RUNDESPIELPLAN, ERSTE_DATEN_ZEILE),
				datenEnd.spalte(ERSTE_SPALTE_ERGEBNISSE - 1));
		spielrundeHelper.formatiereGeradeUngradeSpielpaarungen(sheet, datenRangeSpielpaarungen,
				konfigurationSheet.getSpielRundeHintergrundFarbeGeradeStyle(),
				konfigurationSheet.getSpielRundeHintergrundFarbeUnGeradeStyle());

		datenEnd = letztePositionRechtsUnten();
		RangePosition ergbenissRange = RangePosition.from(Position.from(ERSTE_SPALTE_ERGEBNISSE, ERSTE_DATEN_ZEILE),
				Position.from(datenEnd));
		sheet.getSheetHelper().setPropertyInRange(xsheet, ergbenissRange, CHAR_HEIGHT, 16);
		sheet.getSheetHelper().setPropertyInRange(xsheet, ergbenissRange, CHAR_WEIGHT, FontWeight.BOLD);

		spielrundeHelper.formatiereErgebnissRange(sheet, ergbenissRange, ERSTE_SPALTE_ERGEBNISSE);
	}
}
