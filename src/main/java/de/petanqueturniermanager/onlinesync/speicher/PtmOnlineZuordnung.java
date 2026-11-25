/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync.speicher;

import java.util.Objects;

/**
 * Gespeicherte Zuordnung einer lokalen Meldung (lokale UUID) zu einer Online-Anmeldung. Die Online-ID selbst steht
 * nicht hier, sondern in der Nr-Zelle der Meldung ({@code NrZellFormel}).
 *
 * @param lokaleBezeichnung lokaler Name der Meldung, wie zuletzt abgeglichen
 * @param onlineStatus      übersetzter Online-Anmeldestatus (Anzeige)
 * @param revision          Ausführungsrevision der Online-Anmeldung, mindestens 1
 * @param rohStatus         unübersetzter Online-Anmeldestatus (Storno-Erkennung)
 * @param besetzung         zuletzt abgeglichene Besetzung samt Benutzer-IDs
 * @param vermerk           sprachneutrale Vermerke
 * @param teamname          zuletzt abgeglichener Teamname; leer = unbekannt
 */
public record PtmOnlineZuordnung(String lokaleUuid, String lokaleBezeichnung, String onlineStatus, String tarife,
        String fragen, int revision, String rohStatus, String besetzung, String vermerk, String teamname) {

    public PtmOnlineZuordnung {
        Objects.requireNonNull(lokaleUuid, "lokaleUuid");
        lokaleBezeichnung = Objects.requireNonNullElse(lokaleBezeichnung, "");
        onlineStatus = Objects.requireNonNullElse(onlineStatus, "");
        tarife = Objects.requireNonNullElse(tarife, "");
        fragen = Objects.requireNonNullElse(fragen, "");
        revision = Math.max(1, revision);
        rohStatus = Objects.requireNonNullElse(rohStatus, "");
        besetzung = Objects.requireNonNullElse(besetzung, "");
        vermerk = Objects.requireNonNullElse(vermerk, "");
        teamname = Objects.requireNonNullElse(teamname, "");
    }

    static PtmOnlineZuordnung aus(NeueZuordnung neu) {
        return new PtmOnlineZuordnung(neu.lokaleUuid(), neu.lokaleBezeichnung(), neu.onlineStatus(), neu.tarife(),
                neu.fragen(), neu.executionRevision(), neu.rohStatus(), neu.besetzung(), neu.vermerk(),
                neu.teamname());
    }

    PtmOnlineZuordnung mitAnzeige(ZuordnungsAnzeige anzeige) {
        ZuordnungsAnzeige.OnlineDetails details = anzeige.details()
                .orElse(new ZuordnungsAnzeige.OnlineDetails(tarife, fragen, rohStatus));
        return new PtmOnlineZuordnung(lokaleUuid, anzeige.lokaleBezeichnung(), anzeige.onlineStatus(),
                details.tarife(), details.fragen(), revision, details.rohStatus(), besetzung, vermerk, teamname);
    }

    PtmOnlineZuordnung mitBezeichnung(String neueBezeichnung, String neuerOnlineStatus) {
        return new PtmOnlineZuordnung(lokaleUuid, neueBezeichnung, neuerOnlineStatus, tarife, fragen, revision,
                rohStatus, besetzung, vermerk, teamname);
    }

    PtmOnlineZuordnung mitDetails(ZuordnungsAnzeige.OnlineDetails details) {
        return new PtmOnlineZuordnung(lokaleUuid, lokaleBezeichnung, onlineStatus, details.tarife(), details.fragen(),
                revision, details.rohStatus(), besetzung, vermerk, teamname);
    }

    PtmOnlineZuordnung mitRevision(int neueRevision) {
        return new PtmOnlineZuordnung(lokaleUuid, lokaleBezeichnung, onlineStatus, tarife, fragen, neueRevision,
                rohStatus, besetzung, vermerk, teamname);
    }

    /** Übernimmt die gesetzten Werte des Zusatzes; {@code null} lässt den bisherigen Wert stehen. */
    PtmOnlineZuordnung mitZusatz(ZuordnungsZusatz zusatz) {
        return new PtmOnlineZuordnung(lokaleUuid, lokaleBezeichnung, onlineStatus, tarife, fragen, revision,
                rohStatus, Objects.requireNonNullElse(zusatz.besetzung(), besetzung),
                Objects.requireNonNullElse(zusatz.vermerk(), vermerk),
                Objects.requireNonNullElse(zusatz.teamname(), teamname));
    }

    ZuordnungsZusatz zusatz() {
        return new ZuordnungsZusatz(besetzung, vermerk, teamname);
    }
}
