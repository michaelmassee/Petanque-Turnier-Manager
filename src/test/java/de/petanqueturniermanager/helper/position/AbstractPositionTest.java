/**
* Erstellung : 26.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.position;

import static org.assertj.core.api.Assertions.*;

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
		this.position.spalte(45).zeile(200);
		assertThat(this.position.getSpalteString()).isEqualTo("AT");
	}

	@Test
	public void testGetSpalteAddress() throws Exception {
		this.position.spalte(45).zeile(200);
		assertThat(this.position.getSpalteAddress()).isEqualTo("AT:AT");
	}
}

class TestPosition extends AbstractPosition<TestPosition> {
}
