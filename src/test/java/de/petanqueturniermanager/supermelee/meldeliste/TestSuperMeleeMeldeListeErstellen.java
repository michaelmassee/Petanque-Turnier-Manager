package de.petanqueturniermanager.supermelee.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.TestnamenLoader;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;

/**
 * Erstellung 16.07.2022 / Michael Massee
 */

public class TestSuperMeleeMeldeListeErstellen {

	private final WorkingSpreadsheet wkingSpreadsheet;
	private final XSpreadsheetDocument doc;
	private final MeldeListeSheet_New meldeListeSheetNew;
	private final MeldeListeSheet_Update meldeListeSheetUpdate;
	private final TestnamenLoader testnamenLoader;

	public static List<String> setzPos1 = Arrays.asList(new String[] { "Cummings, Kay", "Morgenroth, Waldtraut" });
	public static List<String> setzPos2 = Arrays.asList(new String[] { "Barber, Arne", "Weaver, Erwin" });
	public static List<String> nichtmitspielen = Arrays.asList(new String[] { "Karrer, Milan", "Schaeffer, Thorsten" });
	public static List<String> ausgestiegen = Arrays.asList(new String[] { "Töpfer, Lilian" });

	public TestSuperMeleeMeldeListeErstellen(WorkingSpreadsheet wkingSpreadsheet, XSpreadsheetDocument doc) {
		meldeListeSheetNew = new MeldeListeSheet_New(wkingSpreadsheet);
		meldeListeSheetUpdate = new MeldeListeSheet_Update(wkingSpreadsheet);
		testnamenLoader = new TestnamenLoader();
		this.wkingSpreadsheet = wkingSpreadsheet;
		this.doc = doc;
	}

	public int run() throws GenerateException {
		meldeListeSheetNew.createMeldelisteWithParams(SuperMeleeMode.Triplette); // kein Dialog
		int anzMeldungen = testMeldungenEinfuegen();
		meldeListeSheetUpdate.run();// do not start a Thread !

		return anzMeldungen;
	}

	public int initMitAlleDieSpielen(int anzMeldungen) throws GenerateException {
		meldeListeSheetNew.createMeldelisteWithParams(SuperMeleeMode.Triplette); // kein Dialog
		int anzdidInsertMeldungen = testMeldungenEinfuegenAllepielen(anzMeldungen, 0);
		meldeListeSheetUpdate.run();// do not start a Thread !
		return anzdidInsertMeldungen;
	}

	public int addMitAlleDieSpielen(int anzMeldungen) throws GenerateException {
		int anzbereitsinListe = meldeListeSheetNew.getAlleMeldungen().size();
		int anzdidInsertMeldungen = testMeldungenEinfuegenAllepielen(anzMeldungen, anzbereitsinListe);
		meldeListeSheetUpdate.run();// do not start a Thread !
		return anzdidInsertMeldungen;
	}

	/**
	 * Spalte komplett mit 1
	 * 
	 * @param spieltag
	 * @throws GenerateException
	 */

	public void addMitAlleDieSpielenAktuelleSpieltag(SpielTagNr spieltag) throws GenerateException {
		meldeListeSheetNew.setSpielTag(spieltag);
		meldeListeSheetNew.setAktiveSpieltag(spieltag);
		meldeListeSheetUpdate.setSpielTag(spieltag);
		meldeListeSheetUpdate.setAktiveSpieltag(spieltag);

		int anzbereitsinListe = meldeListeSheetNew.getAlleMeldungen().size();
		int ersteDatenZeile = meldeListeSheetNew.getMeldungenSpalte().getErsteDatenZiele();
		int spielTagSpalte = meldeListeSheetUpdate.aktuelleSpieltagSpalte();

		Position startPos = Position.from(spielTagSpalte, ersteDatenZeile);
		RangeData data = new RangeData(anzbereitsinListe, 1);
		RangeHelper.from(getMeldeListeSheetNew().getXSpreadSheet(), doc, data.getRangePosition(startPos))
				.setDataInRange(data, true);

		meldeListeSheetUpdate.run();
	}

	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return meldeListeSheetNew.getXSpreadSheet();
	}

	public MeldeListeSheet_New getMeldeListeSheetNew() {
		return meldeListeSheetNew;
	}

	public int ersteSummeSpalte() throws GenerateException {
		return meldeListeSheetNew.ersteSummeSpalte();
	}

	/**
	 * Splittet einen Namens-String "Nachname, Vorname" in [Vorname, Nachname].
	 * Wenn kein Komma enthalten ist, landet alles in Vorname (Nachname leer).
	 */
	private static String[] splitVornameNachname(String name) {
		int komma = name.indexOf(',');
		if (komma < 0) {
			return new String[] { name.trim(), "" };
		}
		String nachname = name.substring(0, komma).trim();
		String vorname = name.substring(komma + 1).trim();
		return new String[] { vorname, nachname };
	}

	/** Baut die 4 Datenspalten für die Supermelee-Meldeliste:
	 *  Vorname | Nachname | setzPos | alle_spielen — initial mit alle_spielen=1. */
	private static RangeData baueMeldungenData(List<String> namen) {
		List<String> vornamen = new ArrayList<>(namen.size());
		List<String> nachnamen = new ArrayList<>(namen.size());
		for (String name : namen) {
			String[] splitted = splitVornameNachname(name);
			vornamen.add(splitted[0]);
			nachnamen.add(splitted[1]);
		}
		RangeData data = new RangeData(vornamen);     // [0] Vorname
		data.addNewEmptySpalte();                     // [1] Nachname (gleich überschreiben)
		for (int i = 0; i < data.size(); i++) {
			data.get(i).get(1).setVal(nachnamen.get(i));
		}
		data.addNewEmptySpalte();                     // [2] setzPos
		data.addNewSpalte(1);                         // [3] alle spielen
		return data;
	}

	private int testMeldungenEinfuegenAllepielen(int anzMeldungen, int skip) throws GenerateException {

		int ersteDatenZeile = getMeldeListeSheetNew().getMeldungenSpalte().getErsteDatenZiele();
		ersteDatenZeile += skip;
		int spielerNameErsteSpalte = getMeldeListeSheetNew().getMeldungenSpalte().getErsteMeldungNameSpalte();
		XSpreadsheet sheet = getMeldeListeSheetNew().getXSpreadSheet();

		List<String> listeMitTestNamen = testnamenLoader.listeMitTestNamen(anzMeldungen, skip);
		RangeData data = baueMeldungenData(listeMitTestNamen);

		Position startPos = Position.from(spielerNameErsteSpalte, ersteDatenZeile);
		RangeHelper.from(sheet, doc, data.getRangePosition(startPos)).setDataInRange(data, true);

		return listeMitTestNamen.size();
	}

	private int testMeldungenEinfuegen() throws GenerateException {

		int ersteDatenZeile = getMeldeListeSheetNew().getMeldungenSpalte().getErsteDatenZiele();
		int spielerNameErsteSpalte = getMeldeListeSheetNew().getMeldungenSpalte().getErsteMeldungNameSpalte();
		XSpreadsheet sheet = getMeldeListeSheetNew().getXSpreadSheet();

		List<String> listeMitTestNamen = listeMitTestNamen();
		RangeData data = baueMeldungenData(listeMitTestNamen);

		// ------------------------------------------------------------------------------------------
		// Filter-Helper: rekonstruiert "Nachname, Vorname" aus den geteilten Spalten.
		java.util.function.Function<de.petanqueturniermanager.helper.sheet.rangedata.RowData, String> fullName = r -> {
			String vorname = r.get(0).getStringVal() == null ? "" : r.get(0).getStringVal();
			String nachname = r.get(1).getStringVal() == null ? "" : r.get(1).getStringVal();
			if (nachname.isEmpty()) {
				return vorname;
			}
			if (vorname.isEmpty()) {
				return nachname;
			}
			return nachname + ", " + vorname;
		};

		// setzPosition 1
		data.stream().filter(rData -> setzPos1.contains(fullName.apply(rData))).forEach(r -> {
			r.get(2).setVal(1);
		});

		// setzPosition 2
		data.stream().filter(rData -> setzPos2.contains(fullName.apply(rData))).forEach(r -> {
			r.get(2).setVal(2);
		});
		// ------------------------------------------------------------------------------------------
		// die spielen nicht mit
		data.stream().filter(rData -> nichtmitspielen.contains(fullName.apply(rData))).forEach(r -> {
			r.get(3).setVal(null);
		});

		// ausgestiegen
		data.stream().filter(rData -> ausgestiegen.contains(fullName.apply(rData))).forEach(r -> {
			r.get(3).setVal(2);
		});
		// ------------------------------------------------------------------------------------------

		Position startPos = Position.from(spielerNameErsteSpalte, ersteDatenZeile);
		RangePosition rangePos = RangeHelper.from(sheet, doc, data.getRangePosition(startPos)).setDataInRange(data)
				.getRangePos();
		assertThat(rangePos.getAddress()).isEqualTo("B3:E" + (2 + listeMitTestNamen.size()));
		return listeMitTestNamen.size();
	}

	// http://migano.de/testdaten.php
	public List<String> listeMitTestNamen() {
		List<String> testNamen = new ArrayList<>();

		testNamen.add("Wegner, Silas");
		testNamen.add("Wright, Silvia");
		testNamen.add("Karrer, Milan");
		testNamen.add("Böhme, Bjarne");
		testNamen.add("Cummings, Kay");
		testNamen.add("Trost, Simon");
		testNamen.add("Adrian, Isabella");
		testNamen.add("Gruber, Chantall");
		testNamen.add("Erpel, Leander");
		testNamen.add("Breunig, Lili");
		testNamen.add("Schulte, Catharina");
		testNamen.add("Lau, Henrik");
		testNamen.add("Seel, Dominic");
		testNamen.add("Edwards, Victor");
		testNamen.add("Hoffmann, Arne");
		testNamen.add("Morgenroth, Waldtraut");
		testNamen.add("Töpfer, Lilian");
		testNamen.add("Reiter, Enno");
		testNamen.add("Schaeffer, Thorsten");
		testNamen.add("Kübler, Matis");
		testNamen.add("Barber, Arne");
		testNamen.add("Sinn, Lya");
		testNamen.add("Schreiber, Justus");
		testNamen.add("Weaver, Erwin");
		testNamen.add("Crawford, Lorena");
		return testNamen;
	}

	// 

}
