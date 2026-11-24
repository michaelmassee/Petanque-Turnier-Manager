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
	public void unveraendertePersonenBehaltenIhreLizenznummer() {
		AbgeglicheneBesetzung online = AbgeglicheneBesetzung.ausOnline(List.of(
				new PersonDto(1, "Anna", "Adler", "L-111", "u-anna"), new PersonDto(2, "Gast", "Grün", "L-222", null)));
		List<Person> lokal = List.of(new Person("Anna", "Adler", null), new Person("Gast", "Grün", null));

		assertThat(online.fuerUebertragung(lokal)).extracting(PersonDto::licenseNr)
				.as("z. B. nur der Teamname geändert: Lizenzen gehen unverändert zurück").containsExactly("L-111", "L-222");
		assertThat(AbgeglicheneBesetzung.lese(online.mitBenutzerIdsFuer(lokal).alsText()).fuerUebertragung(lokal))
				.as("auch aus der gespeicherten Besetzung").extracting(PersonDto::licenseNr)
				.containsExactly("L-111", "L-222");
	}

	@Test
	public void ersetztePersonVerliertDieLizenznummerDesSlots() {
		AbgeglicheneBesetzung online = AbgeglicheneBesetzung.ausOnline(List.of(
				new PersonDto(1, "Anna", "Adler", "L-111", null), new PersonDto(2, "Gast", "Grün", "L-222", null)));

		assertThat(online.fuerUebertragung(List.of(new Person("Anna", "Adler", null),
				new Person("Yvonne", "Ypsilon", null)))).extracting(PersonDto::licenseNr).containsExactly("L-111", null);
	}

	@Test
	public void vereinGehoertZurErstenPersonUndBleibtNurBeiIhr() {
		AbgeglicheneBesetzung online = AbgeglicheneBesetzung.ausOnline(List.of(
				new PersonDto(1, "Anna", "Adler", null, null), new PersonDto(2, "Ben", "Berg", null, null)), "BC Linden");
		AbgeglicheneBesetzung gespeichert = AbgeglicheneBesetzung.lese(online.alsText());

		assertThat(gespeichert.vereinFuer(List.of(new Person("Anna", "Adler", null), new Person("Ben", "Berg", null))))
				.contains("BC Linden");
		assertThat(gespeichert.vereinFuer(List.of(new Person("Yvonne", "Ypsilon", null), new Person("Ben", "Berg",
				null)))).as("neue Person an erster Stelle: Verein unbekannt").isEmpty();
		assertThat(gespeichert.mitBenutzerIdsFuer(List.of(new Person("Anna", "Adler", null)))
				.vereinFuer(List.of(new Person("Anna", "Adler", null)))).as("bleibt nach einer Änderung gespeichert")
				.contains("BC Linden");
	}

	@Test
	public void gespeicherteBesetzungOhneLizenzfeldBleibtLesbar() {
		AbgeglicheneBesetzung alt = AbgeglicheneBesetzung.lese("[{\"v\":\"Anna\",\"n\":\"Adler\",\"u\":\"u-anna\"}]");

		assertThat(alt.personen()).singleElement().satisfies(person -> {
			assertThat(person.benutzerId()).contains("u-anna");
			assertThat(person.lizenzNr()).isEmpty();
		});
	}

	@Test
	public void namensSchluesselIstReihenfolgeunabhaengig() {
		AbgeglicheneBesetzung umgekehrt = AbgeglicheneBesetzung.von(
				List.of(new Person("Ben", "Berg", null), new Person("Anna", "Adler", "u-anna")));

		assertThat(umgekehrt.namensSchluessel()).isEqualTo(ONLINE.namensSchluessel());
	}
}
