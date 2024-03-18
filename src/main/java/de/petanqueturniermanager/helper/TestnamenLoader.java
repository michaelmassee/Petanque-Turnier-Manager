package de.petanqueturniermanager.helper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

/**
 * Erstellung 09.03.2024 / Michael Massee
 */

public class TestnamenLoader {

	private static final Logger logger = LogManager.getLogger(TestnamenLoader.class);

	public static final String TESTNAMEN_1000_CSV = "testnamen_1000.csv";

	public List<String> listeMitTestNamen(int anzahl) {
		return listeMitTestNamen(anzahl, 0);
	}

	public List<String> listeMitTestNamen(int anzahl, int skip) {
		List<String> testNamen = new ArrayList<>();

		InputStream csvFile = TestnamenLoader.class.getResourceAsStream(TESTNAMEN_1000_CSV);

		if (csvFile == null) {
			String errmsg = TESTNAMEN_1000_CSV + " File not found";
			logger.error(errmsg);
			throw new NullPointerException(errmsg);
		}

		CSVParser parser = new CSVParserBuilder().withSeparator(';').withIgnoreQuotations(true).build();

		try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(csvFile)).withSkipLines(1 + skip)
				.withCSVParser(parser).build()) {
			String[] nextLine;
			int anzRead = 1;
			while ((nextLine = reader.readNext()) != null && anzRead <= anzahl) {
				testNamen.add(nextLine[2] + ", " + nextLine[1]);
				anzRead++;
			}

		} catch (IOException | CsvException e) {
			logger.error(e.getMessage(), e);
		}
		return testNamen;
	}

	// Testdaten Generator
	// http://migano.de/testdaten.php

	@Deprecated
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
		testNamen.add("Malone, Thorben");
		testNamen.add("Hagedorn, Rosemarie");
		testNamen.add("Gäbler, Katharina");
		testNamen.add("Schmidt, Peter");
		testNamen.add("Schubert, Linus");
		testNamen.add("Both, Dominik");
		testNamen.add("Derksen, Cedric");
		testNamen.add("Wieczorek, Kristine");
		testNamen.add("Cooper, Hartmut");
		testNamen.add("Lehmann, Ralf");
		testNamen.add("Gerth, Natalie");
		testNamen.add("Schüller, Joshua");
		testNamen.add("Schreiber, Silas");
		testNamen.add("Axmann, Jamie");
		testNamen.add("Lerch, Cedrik");
		testNamen.add("Wiener, Lennart");
		testNamen.add("Heymann, Anthony");
		testNamen.add("Reuter, Denise");
		testNamen.add("Tietz, Felix");
		testNamen.add("Hertwig, Louise");
		testNamen.add("Dahms, Carlotta");
		testNamen.add("Penner, Elias");
		testNamen.add("Moody, Lieselotte");
		testNamen.add("Clarke, Paula");
		testNamen.add("Sacher, Kurt");
		testNamen.add("Axmann, Jacqueline");
		testNamen.add("Wood, Kilian");
		testNamen.add("Gerhardt, Erna");
		testNamen.add("Goodman, Luc");
		testNamen.add("Wulf, Anette");
		testNamen.add("Bacher, Anneliese");
		testNamen.add("Bridges, Anneliese");
		testNamen.add("Buchner, Edith");
		testNamen.add("Penner, Thomas");
		testNamen.add("Schütz, John");
		testNamen.add("Steuermann, Claudia");
		testNamen.add("Senioren, Piet");
		testNamen.add("Schaub, Timo");
		testNamen.add("Geis, Kira");
		testNamen.add("Bruckner, Karina");
		testNamen.add("Hughes, Astrid");
		testNamen.add("Brehmer, Tristan");
		testNamen.add("Jacobi, Thorsten");
		testNamen.add("Förster, Chris");
		testNamen.add("Friedel, Selina");
		testNamen.add("Wienecke, Marianne");
		testNamen.add("Gehrmann, Michelle");
		testNamen.add("Fisher, Helena");
		testNamen.add("Normann, Petra");
		testNamen.add("Siemon, Henrik");
		testNamen.add("Pauli, Swenja");
		testNamen.add("Langhans, Leonie");
		testNamen.add("Yilmaz, Gabriele");
		testNamen.add("Deckert, Volker");
		testNamen.add("Love, Bruno");
		testNamen.add("Ruppert, Susanne");
		testNamen.add("Scheerer, Mattis");
		testNamen.add("Obermaier, Swen");
		testNamen.add("Kehl, Lennart");
		testNamen.add("Fassbender, Anouk");
		testNamen.add("Zoeller, Tara");
		testNamen.add("Häger, Stina");
		testNamen.add("Powell, Rike");
		testNamen.add("Wilde, Lewin");
		testNamen.add("Hoff, Sophia");
		testNamen.add("Jakobs, Walter");
		testNamen.add("Tag, Madita");
		testNamen.add("Rhodes, Lya");
		testNamen.add("Maass, Wilhelm");
		testNamen.add("Seeber, Rudolph");
		testNamen.add("Otterbach, Malin");
		testNamen.add("Hüttner, Margarethe");
		testNamen.add("Struck, Marlon");
		testNamen.add("Cross, Stephan");
		testNamen.add("Schultheiss, Merle");

		return testNamen;
	}

}
