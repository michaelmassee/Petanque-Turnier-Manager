package de.petanqueturniermanager.helper.sheet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

/**
 * Erstellung 09.03.2023 / Michael Massee
 */

import org.junit.Test;
import org.mockito.Mockito;

public class SortHelperTest {

	@Test
	public void testSplitSortArray() {

		SortHelper mock = Mockito.mock(SortHelper.class);
		int[] testSortSpalten = new int[] { 0, 8, 4, 1, 10, 11, 3 };
		List<int[]> resultSplitSortBloecke = mock.splitSortBloecke(testSortSpalten);

		assertThat(resultSplitSortBloecke).isNotNull().isNotEmpty();
	}

}
