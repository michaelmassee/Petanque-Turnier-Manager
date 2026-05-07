package de.petanqueturniermanager.spielerdb.matching;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Normalizer;

import org.junit.jupiter.api.Test;

class SpielerMatchKeyNormalizerTest {

    @Test
    void normalisiere_kollabiertWhitespaceUndCasing() {
        assertThat(SpielerMatchKeyNormalizer.normalisiere("  Hans   Peter  "))
                .isEqualTo("hans peter");
    }

    @Test
    void normalisiere_nullUndLeer_gibtLeerString() {
        assertThat(SpielerMatchKeyNormalizer.normalisiere(null)).isEmpty();
        assertThat(SpielerMatchKeyNormalizer.normalisiere("")).isEmpty();
    }

    @Test
    void normalisiere_unifiziertNfdNachNfc() {
        // 'ü' als kombiniert (u + combining diaeresis) muss gleich behandelt
        // werden wie das precomposed 'ü'.
        String nfd = Normalizer.normalize("Müller", Normalizer.Form.NFD);
        String nfc = Normalizer.normalize("Müller", Normalizer.Form.NFC);
        assertThat(nfd).isNotEqualTo(nfc); // Eingangsdaten unterscheiden sich
        assertThat(SpielerMatchKeyNormalizer.normalisiere(nfd))
                .isEqualTo(SpielerMatchKeyNormalizer.normalisiere(nfc));
    }

    @Test
    void normalisiere_keineDiacriticsFaltung() {
        // „Müller" und „Muller" bleiben bewusst getrennt (DACH-Raum,
        // Diacritics sind bedeutungstragend).
        assertThat(SpielerMatchKeyNormalizer.normalisiere("Müller"))
                .isNotEqualTo(SpielerMatchKeyNormalizer.normalisiere("Muller"));
    }

    @Test
    void spielerSchluesselMitVereinName_gleicheVarianten_kollidieren() {
        String a = SpielerMatchKeyNormalizer.spielerSchluesselMitVereinName(
                "Hans", "Müller", "BC Linden");
        String b = SpielerMatchKeyNormalizer.spielerSchluesselMitVereinName(
                "  hans ", "MÜLLER", "  bc  linden ");
        assertThat(a).isEqualTo(b);
    }

    @Test
    void spielerSchluesselMitVereinName_unterschiedlicherVerein_unterschiedlich() {
        String a = SpielerMatchKeyNormalizer.spielerSchluesselMitVereinName(
                "Hans", "Müller", "BC Linden");
        String b = SpielerMatchKeyNormalizer.spielerSchluesselMitVereinName(
                "Hans", "Müller", "BC Eiche");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void spielerSchluesselMitVereinName_nullUndLeer_gleichBehandelt() {
        String mitNull = SpielerMatchKeyNormalizer.spielerSchluesselMitVereinName(
                "Hans", "Müller", null);
        String mitLeer = SpielerMatchKeyNormalizer.spielerSchluesselMitVereinName(
                "Hans", "Müller", "");
        assertThat(mitNull).isEqualTo(mitLeer);
    }

    @Test
    void spielerSchluesselMitVereinNr_unterschiedlicheNrs_unterschiedlich() {
        String a = SpielerMatchKeyNormalizer.spielerSchluesselMitVereinNr(
                "Hans", "Müller", 1);
        String b = SpielerMatchKeyNormalizer.spielerSchluesselMitVereinNr(
                "Hans", "Müller", 2);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void spielerSchluesselMitVereinNr_nullVereinNr_distinktVon0() {
        String mitNull = SpielerMatchKeyNormalizer.spielerSchluesselMitVereinNr(
                "Hans", "Müller", null);
        String mit0 = SpielerMatchKeyNormalizer.spielerSchluesselMitVereinNr(
                "Hans", "Müller", 0);
        assertThat(mitNull).isNotEqualTo(mit0);
    }

    @Test
    void schluesselTypen_namenUndNr_kollidierenNicht() {
        // Auch wenn vereinName="1" und vereinNr=1, dürfen die Schlüssel
        // nicht kollidieren — sonst können Importer-Maps versehentlich
        // Spieler aus dem Abgleich treffen.
        String name = SpielerMatchKeyNormalizer.spielerSchluesselMitVereinName(
                "Hans", "Müller", "1");
        String nr = SpielerMatchKeyNormalizer.spielerSchluesselMitVereinNr(
                "Hans", "Müller", 1);
        assertThat(name).isNotEqualTo(nr);
    }

    @Test
    void vereinSchluessel_normalisiertWieErwartet() {
        assertThat(SpielerMatchKeyNormalizer.vereinSchluessel("  BC   Linden  "))
                .isEqualTo("bc linden");
    }

    @Test
    void labelSchluessel_normalisiertWieErwartet() {
        assertThat(SpielerMatchKeyNormalizer.labelSchluessel("  Senior 60+  "))
                .isEqualTo("senior 60+");
    }
}
