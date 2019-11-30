package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.petanqueturniermanager.model.Meldung;
import de.petanqueturniermanager.model.TeamPaarung;

public class JederGegenJedenTest {

	@Test
	public void testAnzRunden() throws Exception {

		assertThat(new JederGegenJeden(newMeldungenList(3)).anzRunden()).isEqualTo(3);
		assertThat(new JederGegenJeden(newMeldungenList(4)).anzRunden()).isEqualTo(3);
		assertThat(new JederGegenJeden(newMeldungenList(5)).anzRunden()).isEqualTo(5);
		assertThat(new JederGegenJeden(newMeldungenList(6)).anzRunden()).isEqualTo(5);
		assertThat(new JederGegenJeden(newMeldungenList(7)).anzRunden()).isEqualTo(7);
		assertThat(new JederGegenJeden(newMeldungenList(8)).anzRunden()).isEqualTo(7);
	}

	private List<Meldung> newMeldungenList(int anz) {
		ArrayList<Meldung> meldungen = new ArrayList<>();

		for (int i = 0; i < anz; i++) {
			meldungen.add(new Meldung(i + 1));
		}

		return meldungen;
	}

	@Test
	public void testGenerate_6() throws Exception {
		List<List<TeamPaarung>> result = new JederGegenJeden(newMeldungenList(6)).generate();
		assertThat(result).isNotNull().isNotEmpty();
	}

	@Test
	public void testGenerate_7() throws Exception {
		List<List<TeamPaarung>> result = new JederGegenJeden(newMeldungenList(7)).generate();
		assertThat(result).isNotNull().isNotEmpty();
	}

}
