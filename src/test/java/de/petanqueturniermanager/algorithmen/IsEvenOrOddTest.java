package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class IsEvenOrOddTest {

	@Test
	public void testIsEven() throws Exception {
		assertThat(IsEvenOrOdd.IsEven(2)).isTrue();
		assertThat(IsEvenOrOdd.IsEven(680)).isTrue();
		assertThat(IsEvenOrOdd.IsEven(5)).isFalse();
	}

	@Test
	public void testIsOdd() throws Exception {
		assertThat(IsEvenOrOdd.IsOdd(3)).isTrue();
		assertThat(IsEvenOrOdd.IsOdd(123)).isTrue();
		assertThat(IsEvenOrOdd.IsOdd(8)).isFalse();
	}
}
