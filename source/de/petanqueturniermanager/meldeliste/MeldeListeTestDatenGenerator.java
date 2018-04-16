/**
* Erstellung : 24.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.meldeliste;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.model.Meldungen;

public class MeldeListeTestDatenGenerator extends SheetRunner {
	private static final Logger logger = LogManager.getLogger(MeldeListeTestDatenGenerator.class);

	private final MeldeListeSheet meldeListe;

	public MeldeListeTestDatenGenerator(XComponentContext xContext) {
		super(xContext);
		this.meldeListe = new MeldeListeSheet(xContext);
	}

	@Override
	protected Logger getLogger() {
		return MeldeListeTestDatenGenerator.logger;
	}

	@Override
	protected void doRun() {
		generateTestDaten();
	}

	public void spielerAufAktivInaktivMischen() {
		Meldungen aktiveUndAusgesetztMeldungenAktuellenSpielTag = this.meldeListe
				.getAktiveUndAusgesetztMeldungenAktuellenSpielTag();

		int aktuelleSpieltagSpalte = this.meldeListe.aktuelleSpieltagSpalte();
		NumberCellValue numVal = NumberCellValue.from(this.meldeListe.getSheet(),
				Position.from(aktuelleSpieltagSpalte, MeldeListeSheet.ERSTE_DATEN_ZEILE));

		aktiveUndAusgesetztMeldungenAktuellenSpielTag.spieler().forEach((spieler) -> {
			int randomNum = ThreadLocalRandom.current().nextInt(1, 5);
			int spielerZeile = this.meldeListe.getSpielerZeileNr(spieler.getNr());
			numVal.zeile(spielerZeile);
			if (randomNum == 2) {
				getSheetHelper().setValInCell(numVal.setValue((double) randomNum));
			} else {
				getSheetHelper().setValInCell(numVal.setValue((double) 1));
			}
		});
	}

	public void generateTestDaten() {
		XSpreadsheet meldelisteSheet = this.meldeListe.getSheet();
		SheetHelper sheetHelper = getSheetHelper();
		sheetHelper.setActiveSheet(meldelisteSheet);

		List<String> testNamen = listeMitTestNamen();

		Position pos = Position.from(this.meldeListe.getSpielerNameSpalte(), MeldeListeSheet.ERSTE_DATEN_ZEILE - 1);

		int aktuelleSpieltagSpalte = this.meldeListe.aktuelleSpieltagSpalte();

		NumberCellValue numVal = NumberCellValue.from(meldelisteSheet,
				Position.from(pos).spalte(aktuelleSpieltagSpalte));

		for (int spielerCntr = 0; spielerCntr < testNamen.size(); spielerCntr++) {
			sheetHelper.setTextInCell(meldelisteSheet, pos.zeilePlusEins(), testNamen.get(spielerCntr));
			numVal.setPos(pos).spaltePlusEins();
			int randomNum = ThreadLocalRandom.current().nextInt(0, 3);
			if (randomNum == 1) { // nur die einser eintragen
				sheetHelper.setValInCell(numVal.setValue((double) randomNum));
			} else {
				sheetHelper.setTextInCell(StringCellValue.from(numVal).setValue(""));
			}
		}

		this.meldeListe.upDateSheet();
	}

	List<String> listeMitTestNamen() {
		List<String> testNamen = new ArrayList<>();
		testNamen.add("Leitner, Steffen");
		testNamen.add("Schelhaas, Ralf");
		testNamen.add("Reitz, Doro");
		testNamen.add("Massee, Michael");
		testNamen.add("Kallenbach, Klaus");
		testNamen.add("Lehnert, Hans");
		testNamen.add("Kolb, Pollux");
		testNamen.add("Gebauer, Dietmar");
		testNamen.add("Hilberg, Ottmar");
		testNamen.add("Kumar, Sanjeev");
		testNamen.add("Pfeiffer, Reiner");
		testNamen.add("Fischer, Uwe");
		testNamen.add("Schütz, Susanne");
		testNamen.add("Latsch, Peter");
		testNamen.add("Weinmann, Horst");
		testNamen.add("Henk, Axel");
		testNamen.add("Mielchen, Reinhard");
		testNamen.add("Lipperheide, Oliver");
		testNamen.add("Schelhaas, Petra");
		testNamen.add("Fischer, Gerlinde");
		testNamen.add("Kallenbach, Ortrud");
		testNamen.add("Kehrer, Michael");
		testNamen.add("Schubert, Bruni");
		testNamen.add("Müller, Norbert");
		testNamen.add("Pfeiffer, Julia");
		testNamen.add("Rauch, Josef");
		testNamen.add("Raab, Horst");
		testNamen.add("Pfeiffer, Uwe");
		testNamen.add("Schibblock, Ingeborg");
		testNamen.add("Balser, Horst");
		testNamen.add("Balser, Silke");
		testNamen.add("Maas, Monika");
		testNamen.add("Homawoo, Hector");
		testNamen.add("Lohmann, Marie-Claire");
		testNamen.add("Christoph, Karsten");
		testNamen.add("Fritsche, Rainer");
		testNamen.add("Schön, Wolfhard");
		testNamen.add("Wagner, Liesel");
		testNamen.add("Schubert, Alois");
		testNamen.add("Beppeling, Kai");
		testNamen.add("Di-Pol-Moro, Jacques");
		testNamen.add("Windisch, Martin");
		testNamen.add("Tran,Can");
		testNamen.add("Heller, Philipp");
		testNamen.add("Schütz, Michael");
		testNamen.add("Mrowiec, Chicco");
		testNamen.add("Veit, Ingolf");
		testNamen.add("Hameister, Ulf");
		testNamen.add("Latsch, Pat");
		testNamen.add("Martin,Herbert");
		testNamen.add("Bodo, Attila");
		testNamen.add("Schäfer, Nico");
		testNamen.add("Rugar, Silvia");
		testNamen.add("Noack, Michael");
		testNamen.add("Moser, Michael");
		testNamen.add("Dannegger, Fabienne");
		testNamen.add("Gärth, Thomas");
		testNamen.add("Mrowiec, Corinne");
		testNamen.add("Schugger, Michael");
		testNamen.add("Bäulke, Axel");
		testNamen.add("Gerdsmeier, Fritz");
		testNamen.add("Koppel, Rita");
		testNamen.add("Di-Pol-Moro, Birgit");
		testNamen.add("Müller, Eddi");
		testNamen.add("Heil, Daniel");
		testNamen.add("Schäfer, Dirk");
		testNamen.add("Utzat, Barbara");
		testNamen.add("Müller, Bärbel");
		testNamen.add("Günther, Norbert");
		testNamen.add("Jakob, Ulf");
		testNamen.add("Bauer, Andreas");
		testNamen.add("Koppel, Manfred");
		testNamen.add("Renner, Reiner");
		testNamen.add("Vongkhasum, Chintana");
		testNamen.add("Fennel, Christiane");
		testNamen.add("Wesner, Bea");
		testNamen.add("Westrup, Katharina");
		testNamen.add("Dannegger, Martin");
		testNamen.add("Selbmann, Dirk");
		testNamen.add("Stiller, Volker");
		testNamen.add("Fennel, Jule");
		testNamen.add("Nguyen, Hai");
		testNamen.add("Toennies, Stefan");
		testNamen.add("Al Searan, Louay");
		testNamen.add("Sboui, Doro");
		testNamen.add("Rückhardt, Michael");
		testNamen.add("Interwies, Klaus");
		testNamen.add("Krohn, Thomas");
		testNamen.add("Steinmetz, Robert");
		testNamen.add("Beier, Dieter");
		return testNamen;
	}

}
