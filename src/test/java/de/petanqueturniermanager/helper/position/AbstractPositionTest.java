/**
 * Erstellung : 26.05.2018 / Michael Massee
 **/

package de.petanqueturniermanager.helper.position;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

public class AbstractPositionTest {

	private TestPosition position;

	@Before
	public void setup() {
		this.position = new TestPosition();
	}

	@Test
	public void testGetAddressWith$() throws Exception {
		this.position.spalte(45).zeile(200);
		assertThat(this.position.getAddressWith$()).isEqualTo("$AT$201");
		this.position.spalte(1).zeile(34);
		assertThat(this.position.getAddressWith$()).isEqualTo("$B$35");
	}

	@Test
	public void testGetSpalteString() throws Exception {
		this.position.spalte(0).zeile(1); // 0 = A
		assertThat(this.position.getSpalteString()).isEqualTo("A");

		this.position.spalte(4).zeile(200);
		assertThat(this.position.getSpalteString()).isEqualTo("E");

		this.position.spalte(25).zeile(1);
		assertThat(this.position.getSpalteString()).isEqualTo("Z");

		this.position.spalte(26).zeile(1);
		assertThat(this.position.getSpalteString()).isEqualTo("AA");

		this.position.spalte(51).zeile(1);
		assertThat(this.position.getSpalteString()).isEqualTo("AZ");

		this.position.spalte(45).zeile(200);
		assertThat(this.position.getSpalteString()).isEqualTo("AT");

		this.position.spalte(1023).zeile(1); // max 1024
		assertThat(this.position.getSpalteString()).isEqualTo("AMJ");
	}

	@Test
	public void testGetSpalteAddress() throws Exception {
		this.position.spalte(45).zeile(200);
		assertThat(this.position.getSpalteAddress()).isEqualTo("AT:AT");
	}

	@Test
	public void testSpalteNrToString() {
		assertThat(AbstractPosition.spalteNrToString(0)).isEqualTo("A");
		assertThat(AbstractPosition.spalteNrToString(1)).isEqualTo("B");
		assertThat(AbstractPosition.spalteNrToString(25)).isEqualTo("Z");
		assertThat(AbstractPosition.spalteNrToString(26)).isEqualTo("AA");
		assertThat(AbstractPosition.spalteNrToString(52)).isEqualTo("BA");
	}

	@Test
	public void testSpalteStringToNumber() {
		assertThat(AbstractPosition.spalteStringToNumber("A")).isZero();
		assertThat(AbstractPosition.spalteStringToNumber("B")).isEqualTo(1);
		assertThat(AbstractPosition.spalteStringToNumber("Z")).isEqualTo(25);
		assertThat(AbstractPosition.spalteStringToNumber("AA")).isEqualTo(26);
		assertThat(AbstractPosition.spalteStringToNumber("BA")).isEqualTo(52);
	}

	public void testAllSpalteStringToNumberLoop() {
		for (int i = 0; i < 40; i++) {
			assertThat(AbstractPosition.spalteStringToNumber(AbstractPosition.spalteNrToString(i))).isEqualTo(i);
		}
	}

	@Test
	public void testEqualsGleicheSpalteUndZeile() {
		AbstractPosition<?> testPos1 = new AbstractPosition<Object>() {
		};
		testPos1.setSpalte(1);
		testPos1.setZeile(3);

		AbstractPosition<?> testPos2 = new AbstractPosition<Object>() {
		};

		testPos2.setSpalte(1);
		testPos2.setZeile(3);

		assertThat(testPos1.equals(testPos2)).isTrue();
		assertThat(testPos1.equals(Integer.valueOf(3))).isFalse();
	}

}

class TestPosition extends AbstractPosition<TestPosition> {
}
