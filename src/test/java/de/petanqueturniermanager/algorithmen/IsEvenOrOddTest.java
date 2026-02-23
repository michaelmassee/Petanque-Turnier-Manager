package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class IsEvenOrOddTest {

	@Test
	public void testIsEven_Sonderfall_0() {
		assertThat(IsEvenOrOdd.IsEven(0)).isTrue();
	}

	@Test
	public void testIsEven_geradeZahlen() {
		assertThat(IsEvenOrOdd.IsEven(2)).isTrue();
		assertThat(IsEvenOrOdd.IsEven(4)).isTrue();
		assertThat(IsEvenOrOdd.IsEven(100)).isTrue();
	}

	@Test
	public void testIsEven_ungeradeZahlen() {
		assertThat(IsEvenOrOdd.IsEven(1)).isFalse();
		assertThat(IsEvenOrOdd.IsEven(3)).isFalse();
		assertThat(IsEvenOrOdd.IsEven(99)).isFalse();
	}

	@Test
	public void testIsOdd_ungeradeZahlen() {
		assertThat(IsEvenOrOdd.IsOdd(1)).isTrue();
		assertThat(IsEvenOrOdd.IsOdd(3)).isTrue();
	}

	@Test
	public void testIsOdd_geradeZahlen() {
		assertThat(IsEvenOrOdd.IsOdd(0)).isFalse();
		assertThat(IsEvenOrOdd.IsOdd(2)).isFalse();
	}

	@Test
	public void testIsEven_negativeGeradeZahl() {
		assertThat(IsEvenOrOdd.IsEven(-2)).isTrue();
	}

	@Test
	public void testIsEven_negativeUngegradeZahl() {
		assertThat(IsEvenOrOdd.IsEven(-1)).isFalse();
	}

	@Test
	public void testKonsistenz_IsEvenIstNichtIsOdd() {
		for (int n = -10; n <= 10; n++) {
			boolean isEven = IsEvenOrOdd.IsEven(n);
			boolean isOdd = IsEvenOrOdd.IsOdd(n);
			assertThat(isEven ^ isOdd)
					.as("IsEven(%d) und IsOdd(%d) muessen sich gegenseitig ausschliessen", n, n)
					.isTrue();
		}
	}
}
