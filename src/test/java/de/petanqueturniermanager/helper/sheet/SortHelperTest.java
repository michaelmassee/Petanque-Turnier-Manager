package de.petanqueturniermanager.helper.sheet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
		when(mock.splitSortBloecke(any(int[].class))).thenCallRealMethod();

		List<List<Integer>> resultSplitSortBloecke = mock.splitSortBloecke(testSortSpalten);

		assertThat(resultSplitSortBloecke).isNotNull().isNotEmpty().hasSize(3);

		List<List<Integer>> expected = new ArrayList<>();

		expected.add(Arrays.stream(new int[] { 3 }).boxed().collect(Collectors.toList()));
		expected.add(Arrays.stream(new int[] { 1, 10, 11 }).boxed().collect(Collectors.toList()));
		expected.add(Arrays.stream(new int[] { 0, 8, 4 }).boxed().collect(Collectors.toList()));

		assertThat(resultSplitSortBloecke).isEqualTo(expected);

	}

}
