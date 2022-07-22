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
}

class TestPosition extends AbstractPosition<TestPosition> {
}
