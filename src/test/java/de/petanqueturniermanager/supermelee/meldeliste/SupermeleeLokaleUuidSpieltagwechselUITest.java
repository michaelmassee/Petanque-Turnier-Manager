/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.TeamUuidFormel;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.supermelee.SpielTagNr;

/**
 * Die lokale UUID eines Spielers steht in seiner Nr-Zelle ({@code PTM.ALG.TNRUUID}). Ein Spieltagwechsel baut die
 * Meldeliste um und hängt eine Spalte an: Jede UUID muss danach unverändert an derselben Spieler-Nr hängen, und
 * außerhalb der Nr-Spalte darf keine UUID stehen (keine Zusatzspalte).
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
	void neuerSpieltagPerMenueBehaeltUuids() throws GenerateException {
		Map<Integer, String> vorher = uuidsProSpielerNr();

		new MeldeListeSheet_NeuerSpieltag(wkingSpreadsheet).naechsteSpieltag();

		pruefeNachSpieltagwechsel(vorher);
	}

	@Test
	void spieltagwechselPerKonfigurationBehaeltUuids() throws GenerateException {
		Map<Integer, String> vorher = uuidsProSpielerNr();

		MeldeListeSheet_Update update = new MeldeListeSheet_Update(wkingSpreadsheet);
		update.setAktiveSpieltag(SpielTagNr.from(2));
		update.run();

		pruefeNachSpieltagwechsel(vorher);
	}

	private void pruefeNachSpieltagwechsel(Map<Integer, String> vorher) throws GenerateException {
		assertThat(vorher).as("UUIDs vor dem Wechsel").hasSize(ANZ_MELDUNGEN)
				.allSatisfy((nr, uuid) -> assertThat(TeamUuidFormel.istUuid(uuid)).isTrue());
		assertThat(uuidsProSpielerNr()).as("UUIDs je Spieler unverändert").isEqualTo(vorher);
		assertThat(anzahlUuidsAusserhalbNrSpalte()).as("keine UUID außerhalb der Nr-Spalte").isZero();
	}

	/** UUID aus der Formel der Nr-Zelle je angezeigter Spieler-Nr. */
	private Map<Integer, String> uuidsProSpielerNr() throws GenerateException {
		RangePosition bereich = datenBereich(MeldeListeKonstanten.SPIELER_NR_SPALTE, MeldeListeKonstanten.SPIELER_NR_SPALTE);
		String[][] formeln = new SheetHelper(wkingSpreadsheet).getFormulaArrayFromRange(meldeliste(), bereich);
		RangeData werte = RangeHelper.from(meldeliste(), doc, bereich).getDataFromRange();
		Map<Integer, String> ergebnis = new LinkedHashMap<>();
		for (int i = 0; i < werte.size(); i++) {
			int nr = werte.get(i).get(0).getIntVal(0);
			if (nr > 0) {
				ergebnis.put(nr, TeamUuidFormel.findeUuid(formeln[i][0]).orElse(""));
			}
		}
		return ergebnis;
	}

	private long anzahlUuidsAusserhalbNrSpalte() throws GenerateException {
		String[][] formeln = new SheetHelper(wkingSpreadsheet).getFormulaArrayFromRange(meldeliste(),
				datenBereich(MeldeListeKonstanten.SPIELER_NR_SPALTE + 1, MAX_SPALTE));
		long anzahl = 0;
		for (String[] zeile : formeln) {
			for (String zelle : zeile) {
				if (TeamUuidFormel.findeUuid(zelle).isPresent()) {
					anzahl++;
				}
			}
		}
		return anzahl;
	}

	/** Überschriftenzeilen und Datenzeilen der Meldungen, Spalten {@code von} bis {@code bis}. */
	private static RangePosition datenBereich(int von, int bis) {
		return RangePosition.from(von, MeldeListeKonstanten.ERSTE_HEADER_ZEILE, bis,
				MeldeListeKonstanten.ERSTE_DATEN_ZEILE + ANZ_MELDUNGEN - 1);
	}

	private XSpreadsheet meldeliste() throws GenerateException {
		return meldeListeErstellen.getXSpreadSheet();
	}
}
