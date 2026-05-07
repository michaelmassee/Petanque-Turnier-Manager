/**
 * Zentraler Match-Key-Normalizer für die Spieler-DB. Wird von Abgleich und
 * Import gleichermaßen verwendet, damit „Müller" und „  müller " auf denselben
 * Schlüssel kollabieren — aber „Müller" und „Muller" bewusst unterschiedlich
 * bleiben (Diacritics sind im DACH-Raum bedeutungstragend).
 */
@org.jspecify.annotations.NullMarked
package de.petanqueturniermanager.spielerdb.matching;
