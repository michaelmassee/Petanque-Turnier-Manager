package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.ptmonline.AbgeglicheneBesetzung.Person;
import de.petanqueturniermanager.ptmonline.dto.PersonDto;

public class AbgeglicheneBesetzungTest {

	private static final AbgeglicheneBesetzung ONLINE = AbgeglicheneBesetzung.ausOnline(List.of(
			new PersonDto(1, "Anna", "Adler", null, "u-anna"), new PersonDto(2, "Ben", "Berg", null, "u-ben")));

	@Test
	public void speichertBesetzungSamtBenutzerIdsUndLiestSieWieder() {
		AbgeglicheneBesetzung gelesen = AbgeglicheneBesetzung.lese(ONLINE.alsText());

		assertThat(gelesen).isEqualTo(ONLINE);
		assertThat(gelesen.personen()).extracting(person -> person.benutzerId().orElse(null))
				.containsExactly("u-anna", "u-ben");
	}

	@Test
	public void unlesbarerOderLeererInhaltGiltAlsNochNieAbgeglichen() {
		assertThat(AbgeglicheneBesetzung.lese("kein json").istLeer()).isTrue();
		assertThat(AbgeglicheneBesetzung.lese("").istLeer()).isTrue();
		assertThat(AbgeglicheneBesetzung.leer().alsText()).isEmpty();
	}

	@Test
	public void personBehaeltIhreBenutzerIdAuchUmsortiertUndAnders_geschrieben() {
		List<PersonDto> uebertragung = ONLINE.fuerUebertragung(
				List.of(new Person("ben", "BERG", null), new Person("Anna", "Adler", null)));

		assertThat(uebertragung).containsExactly(new PersonDto(1, "ben", "BERG", null, "u-ben"),
				new PersonDto(2, "Anna", "Adler", null, "u-anna"));
	}

	@Test
	public void ersetztePersonVerliertDieBenutzerIdDesSlots() {
		List<PersonDto> uebertragung = ONLINE.fuerUebertragung(
				List.of(new Person("Anna", "Adler", null), new Person("Yvonne", "Ypsilon", null)));

		assertThat(uebertragung).extracting(PersonDto::userId).containsExactly("u-anna", null);
	}

	@Test
	public void eineBenutzerIdWirdNurEinerPersonGegeben() {
		List<PersonDto> uebertragung = ONLINE.fuerUebertragung(
				List.of(new Person("Anna", "Adler", null), new Person("Anna", "Adler", null)));

		assertThat(uebertragung).extracting(PersonDto::userId).containsExactly("u-anna", null);
	}

	@Test
	public void namensSchluesselIstReihenfolgeunabhaengig() {
		AbgeglicheneBesetzung umgekehrt = AbgeglicheneBesetzung.von(
				List.of(new Person("Ben", "Berg", null), new Person("Anna", "Adler", "u-anna")));

		assertThat(umgekehrt.namensSchluessel()).isEqualTo(ONLINE.namensSchluessel());
	}
}
