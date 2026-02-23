package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

public class DirektvergleichResultTest {

	@Test
	public void testGetCode_alleEnumWerte() {
		assertThat(DirektvergleichResult.GLEICH.getCode()).isEqualTo(0);
		assertThat(DirektvergleichResult.VERLOREN.getCode()).isEqualTo(1);
		assertThat(DirektvergleichResult.GEWONNEN.getCode()).isEqualTo(2);
		assertThat(DirektvergleichResult.KEINERGEBNIS.getCode()).isEqualTo(3);
		assertThat(DirektvergleichResult.FEHLER.getCode()).isEqualTo(-1);
	}

	@Test
	public void testGetAnzeigeText_mitExplizitemText() {
		assertThat(DirektvergleichResult.GLEICH.getAnzeigeText()).isEqualTo("Unentschieden");
		assertThat(DirektvergleichResult.KEINERGEBNIS.getAnzeigeText()).isEqualTo("Kein Ergebnis");
	}

	@Test
	public void testGetAnzeigeText_camelCaseVonEnumName() {
		assertThat(DirektvergleichResult.GEWONNEN.getAnzeigeText()).isEqualTo("Gewonnen");
		assertThat(DirektvergleichResult.VERLOREN.getAnzeigeText()).isEqualTo("Verloren");
		assertThat(DirektvergleichResult.FEHLER.getAnzeigeText()).isEqualTo("Fehler");
	}

	@Test
	public void testGetByCode() {
		assertThat(DirektvergleichResult.getByCode(0)).isEqualTo(DirektvergleichResult.GLEICH);
		assertThat(DirektvergleichResult.getByCode(1)).isEqualTo(DirektvergleichResult.VERLOREN);
		assertThat(DirektvergleichResult.getByCode(2)).isEqualTo(DirektvergleichResult.GEWONNEN);
		assertThat(DirektvergleichResult.getByCode(3)).isEqualTo(DirektvergleichResult.KEINERGEBNIS);
		assertThat(DirektvergleichResult.getByCode(-1)).isEqualTo(DirektvergleichResult.FEHLER);
	}

	@Test
	public void testGetByCode_unbekannterCode_null() {
		assertThat(DirektvergleichResult.getByCode(99)).isNull();
	}

	@Test
	public void testSortedByCodeList_sortiertUndVollstaendig() {
		List<DirektvergleichResult> liste = DirektvergleichResult.sortedByCodeList();
		assertThat(liste).hasSize(DirektvergleichResult.values().length);
		assertThat(liste).containsExactlyInAnyOrder(DirektvergleichResult.values());
		for (int i = 0; i < liste.size() - 1; i++) {
			assertThat(liste.get(i).getCode())
					.as("Code an Position %d muss kleiner oder gleich Code an Position %d sein", i, i + 1)
					.isLessThanOrEqualTo(liste.get(i + 1).getCode());
		}
	}

	@Test
	public void testStream_enthaeltAlleEnumWerte() {
		List<DirektvergleichResult> liste = DirektvergleichResult.stream().collect(Collectors.toList());
		assertThat(liste)
				.hasSize(DirektvergleichResult.values().length)
				.containsExactlyInAnyOrder(DirektvergleichResult.values());
	}
}
