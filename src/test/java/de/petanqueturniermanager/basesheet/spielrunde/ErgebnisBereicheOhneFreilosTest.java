/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;

/**
 * Freilos-Zeilen werden aus den editierbaren Ergebnis-Bereichen ausgespart, damit sie im Turniermodus gesperrt
 * bleiben.
 */
class ErgebnisBereicheOhneFreilosTest {

	private static final int ERG_A = 3;
	private static final int ERG_B = 4;
	private static final int ERSTE_ZEILE = 2;

	@Test
	void ohneFreilosEinDurchgehenderBereich() {
		assertThat(adressen(6, Set.of()))
				.containsExactly(adresse(2, 6));
	}

	@Test
	void freilosInLetzterZeileWirdAusgespart() {
		assertThat(adressen(6, Set.of(6)))
				.containsExactly(adresse(2, 5));
	}

	@Test
	void freilosInDerMitteTeiltDenBereich() {
		assertThat(adressen(6, Set.of(2, 4)))
				.containsExactly(adresse(3, 3), adresse(5, 6));
	}

	@Test
	void nurFreilosKeinBereich() {
		assertThat(adressen(2, Set.of(2))).isEmpty();
	}

	@Test
	void leereGegnerZelleIstFreilos() {
		assertThat(SpielrundeHelper.istOhneGegner(new CellData(""))).isTrue();
	}

	@Test
	void gegnerNummerNullIstFreilos() {
		assertThat(SpielrundeHelper.istOhneGegner(new CellData(0.0))).isTrue();
		assertThat(SpielrundeHelper.istOhneGegner(new CellData("0"))).isTrue();
	}

	@Test
	void gegnerNummerOderNameIstBegegnung() {
		assertThat(SpielrundeHelper.istOhneGegner(new CellData(5.0))).isFalse();
		assertThat(SpielrundeHelper.istOhneGegner(new CellData("12"))).isFalse();
		assertThat(SpielrundeHelper.istOhneGegner(new CellData("Leander Fessler / Siska Erl"))).isFalse();
		assertThat(SpielrundeHelper.istOhneGegner(new CellData("A-1"))).isFalse();
	}

	private static List<String> adressen(int letzteZeile, Set<Integer> freilosZeilen) {
		return SpielrundeHelper.ergebnisBereicheOhneFreilos(ERG_A, ERG_B, ERSTE_ZEILE, letzteZeile, freilosZeilen)
				.stream().map(RangePosition::getAddress).toList();
	}

	private static String adresse(int vonZeile, int bisZeile) {
		return RangePosition.from(ERG_A, vonZeile, ERG_B, bisZeile).getAddress();
	}
}
