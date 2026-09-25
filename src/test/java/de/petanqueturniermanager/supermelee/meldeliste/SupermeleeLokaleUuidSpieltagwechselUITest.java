/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.supermelee.SpielTagNr;

/**
 * Die lokale PTM-Online-ID (UUID) eines Spielers steht bei Supermelee direkt rechts vom letzten Spieltag. Ein neuer
 * Spieltag belegt genau diese Spalte: die UUIDs müssen dabei unverändert eine Spalte weiter rechts landen, und die
 * neue Spieltag-Spalte darf keine UUID-Reste enthalten.
 */
class SupermeleeLokaleUuidSpieltagwechselUITest extends BaseCalcUITest {

	private static final int ANZ_MELDUNGEN = 10;
	private static final int MAX_SPALTE = 60;

	private TestSuperMeleeMeldeListeErstellen meldeListeErstellen;

	@BeforeEach
	void setup() throws GenerateException {
		meldeListeErstellen = new TestSuperMeleeMeldeListeErstellen(wkingSpreadsheet, doc);
		meldeListeErstellen.initMitAlleDieSpielen(ANZ_MELDUNGEN);
	}

	@Test
	void neuerSpieltagPerMenueBehaeltUuidsOhneReste() throws GenerateException {
		Map<Integer, String> vorher = uuidsProSpielerNr();
		int uuidSpalteVorher = uuidSpalte();

		new MeldeListeSheet_NeuerSpieltag(wkingSpreadsheet).naechsteSpieltag();

		pruefeNachSpieltagwechsel(vorher, uuidSpalteVorher);
	}

	@Test
	void spieltagwechselPerKonfigurationBehaeltUuidsOhneReste() throws GenerateException {
		Map<Integer, String> vorher = uuidsProSpielerNr();
		int uuidSpalteVorher = uuidSpalte();

		MeldeListeSheet_Update update = new MeldeListeSheet_Update(wkingSpreadsheet);
		update.setAktiveSpieltag(SpielTagNr.from(2));
		update.run();

		pruefeNachSpieltagwechsel(vorher, uuidSpalteVorher);
	}

	private void pruefeNachSpieltagwechsel(Map<Integer, String> vorher, int uuidSpalteVorher)
			throws GenerateException {
		assertThat(vorher).as("UUIDs vor dem Wechsel").hasSize(ANZ_MELDUNGEN)
				.allSatisfy((nr, uuid) -> assertThat(uuid).isNotBlank());
		assertThat(uuidSpalte()).as("UUID-Spalte rückt hinter den neuen Spieltag").isEqualTo(uuidSpalteVorher + 1);
		assertThat(uuidsProSpielerNr()).as("UUIDs je Spieler unverändert").isEqualTo(vorher);
		assertThat(datenSpalte(uuidSpalteVorher).values()).as("keine UUID-Reste in der neuen Spieltag-Spalte")
				.noneMatch(vorher::containsValue);
	}

	private Map<Integer, String> uuidsProSpielerNr() throws GenerateException {
		Map<Integer, String> nummern = datenSpalte(MeldeListeKonstanten.SPIELER_NR_SPALTE);
		Map<Integer, String> uuids = datenSpalte(uuidSpalte());
		Map<Integer, String> ergebnis = new LinkedHashMap<>();
		nummern.forEach((zeile, nr) -> {
			if (nr.matches("\\d+")) {
				ergebnis.put(Integer.valueOf(nr), uuids.getOrDefault(zeile, ""));
			}
		});
		return ergebnis;
	}

	private int uuidSpalte() throws GenerateException {
		String header = I18n.get("ptmonline.meldeliste.header.lokaleuuid");
		RangeData kopf = RangeHelper.from(meldeliste(), doc, RangePosition.from(0,
				MeldeListeKonstanten.ZWEITE_HEADER_ZEILE, MAX_SPALTE, MeldeListeKonstanten.ZWEITE_HEADER_ZEILE))
				.getDataFromRange();
		RowData zeile = kopf.get(0);
		for (int spalte = 0; spalte < zeile.size(); spalte++) {
			if (header.equals(StringUtils.strip(zeile.get(spalte).getStringVal()))) {
				return spalte;
			}
		}
		throw new AssertionError("Keine UUID-Spalte in der Meldeliste");
	}

	/** Getrimmter Text je Datenzeile (0-basiert) einer Spalte. */
	private Map<Integer, String> datenSpalte(int spalte) throws GenerateException {
		int ersteZeile = MeldeListeKonstanten.ERSTE_DATEN_ZEILE;
		RangeData daten = RangeHelper.from(meldeliste(), doc,
				RangePosition.from(spalte, ersteZeile, spalte, ersteZeile + ANZ_MELDUNGEN - 1)).getDataFromRange();
		Map<Integer, String> ergebnis = new LinkedHashMap<>();
		for (int i = 0; i < daten.size(); i++) {
			ergebnis.put(ersteZeile + i, StringUtils.strip(StringUtils.defaultString(daten.get(i).get(0).getStringVal())));
		}
		return ergebnis;
	}

	private XSpreadsheet meldeliste() throws GenerateException {
		return meldeListeErstellen.getXSpreadSheet();
	}
}
