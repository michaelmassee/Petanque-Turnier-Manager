/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.spielrunde;

import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.TABLE_BORDER2;

import java.util.List;

import com.sun.star.container.XNamed;
import com.sun.star.sheet.ConditionOperator;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.addins.GlobalImpl;
import de.petanqueturniermanager.algorithmen.common.DurchgangAufteilungRechner;
import de.petanqueturniermanager.basesheet.konfiguration.IZeitplanPropertiesSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellstyle.FehlerStyle;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.basesheet.meldeliste.SpielRundeNr;

/**
 * Gemeinsame Implementierung der Rundenzeitplanung (Turnier-Startzeit-Feld, optionale
 * Durchgang-Aufteilung: Start-/Endzeiten pro Durchgang, optische Trennlinien, pro-Durchgang
 * neu beginnende Bahn-Nummerierung samt Duplikat-Prüfung) für Spielrunde-Sheets, die dieses
 * Feature unterstützen.
 * <p>
 * Aktuell implementiert von {@code SchweizerAbstractSpielrundeSheet} (und darüber transitiv von
 * Maastrichter, das dieselben Spielrunde-Klassen wiederverwendet) sowie
 * {@code FormuleXAbstractSpielrundeSheet} — identischer Spaltenaufbau (Bahn-Nr, Team A/B,
 * Ergebnis A/B, Zeit-Spalte) und identische Verkettungslogik in beiden Systemen, daher als
 * Default-Methoden hier zentralisiert statt in beiden Klassen dupliziert.
 * <p>
 * Implementierende Klassen müssen nur die kleinen, Sheet-spezifischen Zugriffsmethoden
 * bereitstellen (Spalten-/Zeilenkonstanten als Instanzmethoden, Sheet-Namensauflösung,
 * Konfigurationssheet) — die eigentliche Zeitplan-Logik läuft für alle Implementierungen identisch.
 */
public interface IZeitplanSpielrundeSheet extends ISheet {

	SpielRundeNr getSpielRundeNr() throws GenerateException;

	String getSheetName(SpielRundeNr nr);

	String getSpielrundeSchluessel(int rundeNr);

	IZeitplanPropertiesSpalte getKonfigurationSheet();

	int getErsteHeaderZeile();

	int getZweiteHeaderZeile();

	int getErsteDatenZeile();

	int getBahnNrSpalte();

	int getZeitSpalte();

	int getNrCharHeight();

	/**
	 * Schreibt (nur wenn {@code isZeitplanAktiv()}) das einzige haendisch editierbare
	 * Zeit-Eingabefeld dieses Features in die Header-Zellen von {@link #getZeitSpalte()} (steht
	 * damit direkt ueber den darunter befuellten Durchgang-Zeiten derselben Spalte). Runde 1 nutzt
	 * die zentrale Turnier-Startzeit als Default, alle Folgerunden verketten sich per
	 * Cross-Sheet-Zellbezug auf die Startzeit der Vorrunde plus deren Gesamtdauer plus
	 * Rundenpause. Alle Werte werden live per {@code PTM.ALG.INTPROPERTY}/{@code STRINGPROPERTY}
	 * referenziert, damit Aenderungen an der Konfiguration ohne Sheet-Neuaufbau wirken.
	 * <p>
	 * Zelle ist bewusst TEXT (Runde 1: literaler String, Runde N&gt;1: {@code TEXT(...;"HH:MM")}),
	 * kein numerisch formatierter Zeitwert — siehe {@link #zeitZelleSchreiben} fuer die Begruendung.
	 */
	default void rundenStartzeitFeld() throws GenerateException {
		if (!getKonfigurationSheet().isZeitplanAktiv()) {
			return;
		}
		zeitplanPropertiesPersistieren();
		getSheetHelper().setColumnWidth(getXSpreadSheet(), Position.from(getZeitSpalte(), getErsteHeaderZeile()), 1800);

		Position startzeitPos = Position.from(getZeitSpalte(), getZweiteHeaderZeile());
		if (getSpielRundeNr().getNr() <= 1) {
			// Runde 1: einmaliger literaler Default aus der Turnier-Startzeit (kein Formelbezug) —
			// konsistent mit der Persistenz-Regel (das Sheet-Feld selbst ist die fuehrende Quelle,
			// keine retroaktive Verschiebung wenn die zentrale Turnier-Startzeit spaeter geaendert wird).
			StringCellValue startzeitValue = StringCellValue.from(getXSpreadSheet(), startzeitPos)
					.setVertJustify(CellVertJustify2.CENTER).setHoriJustify(CellHoriJustify.CENTER)
					.setCharHeight(getNrCharHeight()).setShrinkToFit(true)
					.setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder())
					.setValue(getKonfigurationSheet().getZeitplanTurnierStartzeit());
			getSheetHelper().setStringValueInCell(startzeitValue);
		} else {
			String formel = "TEXT(" + rundenStartzeitFormel() + ";\"HH:MM\")";
			StringCellValue startzeitValue = StringCellValue.from(getXSpreadSheet(), startzeitPos, formel)
					.setVertJustify(CellVertJustify2.CENTER).setHoriJustify(CellHoriJustify.CENTER)
					.setCharHeight(getNrCharHeight()).setShrinkToFit(true)
					.setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder());
			getSheetHelper().setFormulaInCell(startzeitValue);
		}

		RangePosition startzeitRange = RangePosition.from(startzeitPos, startzeitPos);
		EditierbaresZelleFormatHelper.anwenden(this, startzeitRange);
	}

	/**
	 * Ruft die Getter der Zeitplan-Zeitwerte einmal auf, damit deren Konfig-Default via
	 * {@code readIntProperty} in die UserDefinedProperties des Dokuments persistiert wird, falls
	 * dort noch kein Wert existiert (z.B. weil der Zeitplan-Dialog noch nie geoeffnet wurde). Ohne
	 * das liefert das weiter unten live referenzierte {@code PTM.ALG.INTPROPERTY(...)} in den
	 * gebauten Formeln 0 statt des echten Defaults — sichtbarer Bug: Endzeit == Startzeit.
	 */
	default void zeitplanPropertiesPersistieren() {
		var konfig = getKonfigurationSheet();
		konfig.getZeitplanZeitlimitMinuten();
		konfig.getZeitplanDurchgangPauseMinuten();
		konfig.getZeitplanRundenPauseMinuten();
	}

	/**
	 * Nur fuer Runde N&gt;1: numerischer Tagesbruchteil-Ausdruck (kein fertig formatiertes {@code TEXT(...)}
	 * — das uebernimmt der Aufrufer, siehe {@link #zeitZelleSchreiben}) fuer das tatsaechliche Ende des
	 * letzten Durchgangs der Vorrunde (siehe {@link #ermittleLetzteZeitZeile}) plus Rundenpause —
	 * dieselbe Verkettungslogik wie zwischen zwei Durchgaengen innerhalb einer Runde, nur
	 * rundenuebergreifend. War die Vorrunde nicht aufgeteilt (kein Eintrag in {@link #getZeitSpalte()}),
	 * wird ersatzweise die Rundenstartzeit der Vorrunde plus ein Zeitlimit (Dauer der einzigen,
	 * ungeteilten Runde) plus Rundenpause verwendet. Die referenzierte Vorrunden-Zelle ist immer TEXT
	 * (siehe {@link #zeitZelleSchreiben}), daher {@code TIMEVALUE(...)} vor der Addition.
	 * <p>
	 * Der Sheet-Name im Formel-Bezug wird ueber {@link SheetMetadataHelper#findeSheetUndHeile}
	 * aufgeloest (Metadaten-first, ueberlebt Umbenennung), nicht ueber den lokalisierten
	 * Default-Namen {@link #getSheetName} — sonst entsteht ein {@code #REF!}, sobald die Vorrunde
	 * umbenannt wurde, obwohl die Metadaten-Suche das richtige Sheet findet.
	 */
	default String rundenStartzeitFormel() throws GenerateException {
		SpielRundeNr aktuelleRunde = getSpielRundeNr();
		SpielRundeNr vorherigeRunde = SpielRundeNr.from(aktuelleRunde.getNr() - 1);
		var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		XSpreadsheet vorSheet = SheetMetadataHelper.findeSheetUndHeile(xDoc,
				getSpielrundeSchluessel(vorherigeRunde.getNr()), getSheetName(vorherigeRunde));
		// Vorrunde muesste eigentlich existieren (Runde N>1 setzt Runde N-1 voraus); defensiv
		// trotzdem auf den Default-Namen zurueckfallen statt eine NPE zu riskieren.
		String vorSheetName = vorSheet != null ? Lo.qi(XNamed.class, vorSheet).getName() : getSheetName(vorherigeRunde);
		String rundenPause = GlobalImpl.FORMAT_PTM_INT_PROPERTY(IZeitplanPropertiesSpalte.KONFIG_PROP_ZEITPLAN_RUNDEN_PAUSE_MINUTEN);

		int letzteZeitZeile = vorSheet != null ? ermittleLetzteZeitZeile(vorSheet, xDoc) : -1;
		if (letzteZeitZeile >= 0) {
			String letzteEndeZelle = "$'" + vorSheetName + "'." + Position.from(getZeitSpalte(), letzteZeitZeile).getAddressWith$();
			return "TIMEVALUE(" + letzteEndeZelle + ")+" + minutenAlsTagesbruchteil(rundenPause);
		}

		String vorZelle = "$'" + vorSheetName + "'." + Position.from(getZeitSpalte(), getZweiteHeaderZeile()).getAddressWith$();
		String zeitlimit = GlobalImpl.FORMAT_PTM_INT_PROPERTY(IZeitplanPropertiesSpalte.KONFIG_PROP_ZEITPLAN_ZEITLIMIT_MINUTEN);
		return "TIMEVALUE(" + vorZelle + ")+" + minutenAlsTagesbruchteil(zeitlimit + "+" + rundenPause);
	}

	/** Minuten-Ausdruck als Tagesbruchteil-Formel ({@code (Ausdruck)/1440}). */
	private static String minutenAlsTagesbruchteil(String minutenAusdruck) {
		return "(" + minutenAusdruck + ")/1440";
	}

	/**
	 * Liest die Zeile der letzten befuellten {@link #getZeitSpalte()}-Zelle der Vorrunde aus
	 * (= Ende des letzten Durchgangs, da Durchgang-Bloecke lueckenlos aufeinanderfolgen und der
	 * letzte Block immer bei der letzten Datenzeile endet). -1, wenn die Vorrunde nicht aufgeteilt war.
	 */
	default int ermittleLetzteZeitZeile(XSpreadsheet vorSheet, XSpreadsheetDocument xDoc) throws GenerateException {
		RangePosition zeitRange = RangePosition.from(getZeitSpalte(), getErsteDatenZeile(), getZeitSpalte(),
				getErsteDatenZeile() + 999);
		RangeData zeitDaten = RangeHelper.from(vorSheet, xDoc, zeitRange).getDataFromRange();
		int letzteNichtLeere = -1;
		for (int i = 0; i < zeitDaten.size(); i++) {
			String val = zeitDaten.get(i).get(0).getStringVal();
			if (val != null && !val.isEmpty()) {
				letzteNichtLeere = i;
			}
		}
		return letzteNichtLeere >= 0 ? getErsteDatenZeile() + letzteNichtLeere : -1;
	}

	/**
	 * Schreibt (nur wenn {@code isDurchgangAufteilungWirksam()} und tatsaechlich mehr als ein
	 * Durchgang noetig ist) fuer jeden Durchgang-Block dessen Startzeit in die erste und dessen
	 * Endzeit in die letzte Datenzeile von {@link #getZeitSpalte()} (bei einzeiligen Bloecken ist das
	 * dieselbe Zelle — die zuletzt geschriebene Endzeit gewinnt dort). Jeder Durchgang ab dem
	 * zweiten verkettet sich direkt an die tatsaechliche Endzeit-Zelle des vorherigen Durchgangs
	 * plus Durchgang-Pause (kein rekonstruierter Faktor N-1 mehr — vermeidet Drift, falls Zeitlimit
	 * oder Pause sich waehrend der laufenden Runde aendern). Kein Text-Label ("Durchgang N") — die
	 * Durchgaenge werden stattdessen rein optisch per doppelter Trennlinie unterschieden, siehe
	 * {@link #durchgangTrennlinienSetzen}.
	 */
	default void durchgangInfoSpaltenSchreiben(int anzahlPaarungen) throws GenerateException {
		if (!getKonfigurationSheet().isDurchgangAufteilungWirksam() || anzahlPaarungen <= 0) {
			return;
		}
		int bahnen = getKonfigurationSheet().getZeitplanAnzahlBahnen();
		List<Integer> bloecke = DurchgangAufteilungRechner.berechne(anzahlPaarungen, bahnen);
		if (bloecke.size() <= 1) {
			return; // Paarungen passen in einen Durchgang, keine Aufteilung noetig
		}

		String rundenStartzeitAdresse = Position.from(getZeitSpalte(), getZweiteHeaderZeile()).getAddressWith$();
		String zeitlimit = GlobalImpl.FORMAT_PTM_INT_PROPERTY(IZeitplanPropertiesSpalte.KONFIG_PROP_ZEITPLAN_ZEITLIMIT_MINUTEN);
		String pause = GlobalImpl.FORMAT_PTM_INT_PROPERTY(IZeitplanPropertiesSpalte.KONFIG_PROP_ZEITPLAN_DURCHGANG_PAUSE_MINUTEN);

		int zeile = getErsteDatenZeile();
		String vorherigeEndeAdresse = null;
		for (int groesse : bloecke) {
			SheetRunner.testDoCancelTask();

			int letzteZeileDesBlocks = zeile + groesse - 1;
			// vorherigeEndeAdresse/rundenStartzeitAdresse sind TEXT-Zellen (siehe zeitZelleSchreiben) -> TIMEVALUE() vor der Addition
			String startAusdruck = vorherigeEndeAdresse == null ? "TIMEVALUE(" + rundenStartzeitAdresse + ")"
					: "TIMEVALUE(" + vorherigeEndeAdresse + ")+" + minutenAlsTagesbruchteil(pause);
			Position startPos = Position.from(getZeitSpalte(), zeile);

			String endeAdresse;
			if (letzteZeileDesBlocks == zeile) {
				// einzeiliger Block: nur die Endzeit wird angezeigt; Endausdruck direkt aus startAusdruck
				// ableiten (keine Zellreferenz auf startPos, da dort nur die Endzeit geschrieben wird -
				// eine Referenz auf startPos.getAddress() waere ein Zirkelbezug)
				String endeAusdruck = startAusdruck + "+" + minutenAlsTagesbruchteil(zeitlimit);
				zeitZelleSchreiben(startPos, endeAusdruck);
				endeAdresse = startPos.getAddress();
			} else {
				zeitZelleSchreiben(startPos, startAusdruck);
				Position endePos = Position.from(getZeitSpalte(), letzteZeileDesBlocks);
				String endeAusdruck = "TIMEVALUE(" + startPos.getAddress() + ")+" + minutenAlsTagesbruchteil(zeitlimit);
				zeitZelleSchreiben(endePos, endeAusdruck);
				endeAdresse = endePos.getAddress();
			}
			vorherigeEndeAdresse = endeAdresse;

			zeile += groesse;
		}
	}

	/**
	 * Schreibt einen numerischen Tagesbruchteil-Ausdruck als {@code TEXT(...;"HH:MM")}-Formel in
	 * {@link #getZeitSpalte()} — bewusst TEXT statt numerischer Zeitwert mit {@code NumberFormat}-Property:
	 * LibreOffice Calc kann Formelzellen bei Neuberechnung (z.B. nach Aenderung eines
	 * Zeitplan-Konfigwerts) intern ein eigenes Anzeigeformat zuweisen und dabei das per API gesetzte
	 * HH:MM lautlos auf HH:MM:SS erweitern — nicht deterministisch reproduzierbar, aber im echten
	 * Dokument beobachtet. Text-Zellen sind von dieser Auto-Formatierung nicht betroffen.
	 */
	default void zeitZelleSchreiben(Position pos, String numerischerAusdruck) throws GenerateException {
		String formel = "TEXT(" + numerischerAusdruck + ";\"HH:MM\")";
		StringCellValue zeitValue = StringCellValue.from(getXSpreadSheet(), pos, formel)
				.setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER)
				.setCharHeight(12);
		getSheetHelper().setFormulaInCell(zeitValue);
	}

	/**
	 * Trennt die Durchgaenge optisch durch eine doppelte horizontale Linie am oberen Rand der
	 * jeweils ersten Zeile eines neuen Durchgangs (kein Trennstrich vor dem ersten Durchgang,
	 * der liegt direkt unter dem bereits abgegrenzten Header). Muss NACH der Daten-Formatierung
	 * des Sheets aufgerufen werden, da deren {@code allThin()}-Rahmen sonst die Trennlinie
	 * ueberschreiben wuerde; nur die obere Linie wird gesetzt (IsTopLineValid=true), alle anderen
	 * Seiten bleiben unveraendert.
	 */
	default void durchgangTrennlinienSetzen(int anzahlPaarungen) throws GenerateException {
		if (!getKonfigurationSheet().isDurchgangAufteilungWirksam() || anzahlPaarungen <= 0) {
			return;
		}
		List<Integer> bloecke = DurchgangAufteilungRechner.berechne(anzahlPaarungen,
				getKonfigurationSheet().getZeitplanAnzahlBahnen());
		if (bloecke.size() <= 1) {
			return;
		}

		XSpreadsheet sheet = getXSpreadSheet();
		TableBorder2 doppelteLinieOben = BorderFactory.from().doubleLn().forTop().toBorder();

		int zeile = getErsteDatenZeile();
		for (int i = 0; i < bloecke.size(); i++) {
			if (i > 0) {
				RangePosition trennzeile = RangePosition.from(getBahnNrSpalte(), zeile, getZeitSpalte(), zeile);
				getSheetHelper().setPropertyInRange(sheet, trennzeile, TABLE_BORDER2, doppelteLinieOben);
			}
			zeile += bloecke.get(i);
		}
	}

	/**
	 * Muss NACH dem Schreiben der Standard-Bahn-Nr-Spalte (fortlaufend 1..Anzahl Paarungen)
	 * aufgerufen werden, da {@link #bahnNummerierungProDurchgang} diese gezielt ueberschreibt.
	 */
	default void bahnNummerierungProDurchgangFallsAktiv(int anzahlPaarungen) throws GenerateException {
		if (!getKonfigurationSheet().isDurchgangAufteilungWirksam() || anzahlPaarungen <= 0) {
			return;
		}
		List<Integer> bloecke = DurchgangAufteilungRechner.berechne(anzahlPaarungen,
				getKonfigurationSheet().getZeitplanAnzahlBahnen());
		if (bloecke.size() <= 1) {
			return;
		}
		bahnNummerierungProDurchgang(bloecke);
	}

	/**
	 * Bei aktiver Durchgang-Aufteilung beginnt die Bahn-Nummer in {@link #getBahnNrSpalte()} pro
	 * Durchgang-Block neu bei 1 (physische Bahnen werden zwischen Durchgaengen wiederverwendet).
	 * Ueberschreibt gezielt nur die Datenzeilen, die zuvor im Standard-Modus (fortlaufend
	 * 1..Anzahl Paarungen) geschrieben wurden. Nur fuer die "nummerierten"
	 * {@link SpielrundeSpielbahn}-Modi X/N sinnvoll — L (haendisch) und R (zufaellig) bleiben
	 * unveraendert, diese Property ist unabhaengig von {@link SpielrundeSpielbahn} und veraendert
	 * dessen bestehende Semantik nicht.
	 */
	default void bahnNummerierungProDurchgang(List<Integer> bloecke) throws GenerateException {
		SpielrundeSpielbahn modus = getKonfigurationSheet().getSpielrundeSpielbahn();
		if (modus != SpielrundeSpielbahn.X && modus != SpielrundeSpielbahn.N) {
			return;
		}
		RangeData rangeData = new RangeData();
		for (int groesse : bloecke) {
			for (int n = 1; n <= groesse; n++) {
				rangeData.addNewRow(n);
			}
		}
		Position startPos = Position.from(getBahnNrSpalte(), getErsteDatenZeile());
		RangeHelper.from(this, rangeData.getRangePosition(startPos)).setDataInRange(rangeData);

		bahnNrDuplikatPruefungProDurchgang(bloecke);
	}

	/**
	 * Die Standard-Bahn-Nr-Duplikatpruefung markiert doppelte Bahn-Nummern per bedingter
	 * Formatierung rot — die COUNTIF-Formel prueft dabei ueber die GESAMTE Spalte. Da die
	 * Bahn-Nummern bei aktiver Durchgang-Aufteilung pro Durchgang bewusst neu ab 1 beginnen
	 * (z.B. 1,2,3,1,2,3,1,2), wuerde diese Pruefung faelschlich jede Zeile als „doppelt" rot
	 * einfaerben. Ersetzt die Pruefung durch eine pro Durchgang-Block skalierte Variante, die
	 * Duplikate nur innerhalb desselben Durchgangs erkennt.
	 * <p>
	 * Loescht VOR dem Anwenden der Block-Regeln die bedingte Formatierung ueber die GESAMTE
	 * Bahn-Nr-Datenspalte in einem Rutsch (nicht nur die jeweilige Block-Range) — sonst bleiben bei
	 * wiederholtem "Neu auslosen" mit unterschiedlicher Bahnen-Anzahl (unterschiedliche
	 * Block-Grenzen je Lauf) Fragmente frueherer Regeln auf Zeilen liegen, die in der aktuellen
	 * Blockstruktur nicht mehr durch eine neue Regel exakt ueberschrieben werden — LO haeuft dann
	 * mehrere ueberlappende Regeln je Zelle an, die unabhaengig voneinander auswerten (beobachteter
	 * Bug: Zellen mit legitim wiederkehrenden Werten wurden faelschlich rot markiert).
	 */
	default void bahnNrDuplikatPruefungProDurchgang(List<Integer> bloecke) throws GenerateException {
		int gesamtAnzahlZeilen = bloecke.stream().mapToInt(Integer::intValue).sum();
		RangePosition gesamteSpalte = RangePosition.from(getBahnNrSpalte(), getErsteDatenZeile(),
				getBahnNrSpalte(), getErsteDatenZeile() + gesamtAnzahlZeilen - 1);
		ConditionalFormatHelper.clearOnly(this, gesamteSpalte);

		FehlerStyle fehlerStyle = new FehlerStyle();
		int zeile = getErsteDatenZeile();
		for (int groesse : bloecke) {
			RangePosition blockRange = RangePosition.from(getBahnNrSpalte(), zeile, getBahnNrSpalte(), zeile + groesse - 1);
			String conditionFindDoppelt = "COUNTIF(" + blockRange.getAddressWith$() + ";"
					+ ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")>1";
			String conditionNotEmpty = ConditionalFormatHelper.FORMULA_CURRENT_CELL + "<>\"\"";
			String formulaFindDoppelteBahnNr = "AND(" + conditionFindDoppelt + ";" + conditionNotEmpty + ")";
			ConditionalFormatHelper.from(this, blockRange).clear().formula1(formulaFindDoppelteBahnNr)
					.operator(ConditionOperator.FORMULA).style(fehlerStyle).applyAndDoReset();
			zeile += groesse;
		}
	}

}
