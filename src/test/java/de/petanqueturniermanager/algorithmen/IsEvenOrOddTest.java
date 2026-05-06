package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class IsEvenOrOddTest {

	@Test
	public void testIsEven_Sonderfall_0() {
		assertThat(IsEvenOrOdd.isEven(0)).isTrue();
	}

	@Test
	public void testIsEven_geradeZahlen() {
		assertThat(IsEvenOrOdd.isEven(2)).isTrue();
		assertThat(IsEvenOrOdd.isEven(4)).isTrue();
		assertThat(IsEvenOrOdd.isEven(100)).isTrue();
	}

	@Test
	public void testIsEven_ungeradeZahlen() {
		assertThat(IsEvenOrOdd.isEven(1)).isFalse();
		assertThat(IsEvenOrOdd.isEven(3)).isFalse();
		assertThat(IsEvenOrOdd.isEven(99)).isFalse();
	}

	@Test
	public void testIsOdd_ungeradeZahlen() {
		assertThat(IsEvenOrOdd.isOdd(1)).isTrue();
		assertThat(IsEvenOrOdd.isOdd(3)).isTrue();
	}

	@Test
	public void testIsOdd_geradeZahlen() {
		assertThat(IsEvenOrOdd.isOdd(0)).isFalse();
		assertThat(IsEvenOrOdd.isOdd(2)).isFalse();
	}

	@Test
	public void testIsEven_negativeGeradeZahl() {
		assertThat(IsEvenOrOdd.isEven(-2)).isTrue();
	}

	@Test
	public void testIsEven_negativeUngegradeZahl() {
		assertThat(IsEvenOrOdd.isEven(-1)).isFalse();
	}

	@Test
	public void testIsEven_IntegerGrenzwerte() {
		// Integer.MAX_VALUE = 2^31-1 (ungerade)
		assertThat(IsEvenOrOdd.isEven(Integer.MAX_VALUE)).isFalse();
		assertThat(IsEvenOrOdd.isOdd(Integer.MAX_VALUE)).isTrue();
		// Integer.MIN_VALUE = -2^31 (gerade)
		assertThat(IsEvenOrOdd.isEven(Integer.MIN_VALUE)).isTrue();
		assertThat(IsEvenOrOdd.isOdd(Integer.MIN_VALUE)).isFalse();
	}

	@Test
	public void testKonsistenz_IsEvenIstNichtIsOdd() {
		for (int n = -10; n <= 10; n++) {
			boolean isEven = IsEvenOrOdd.isEven(n);
			boolean isOdd = IsEvenOrOdd.isOdd(n);
			assertThat(isEven ^ isOdd)
					.as("IsEven(%d) und IsOdd(%d) muessen sich gegenseitig ausschliessen", n, n)
					.isTrue();
		}
	}
}
