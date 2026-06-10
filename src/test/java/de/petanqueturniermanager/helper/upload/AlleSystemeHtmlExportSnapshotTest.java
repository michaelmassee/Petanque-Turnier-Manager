/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.liga.meldeliste.LigaExportInVerzeichnis;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;

class AlleSystemeHtmlExportSnapshotTest {

    private static final String REFERENZ_DATEI =
            "/de/petanqueturniermanager/helper/upload/AlleSystemeHtmlExport_ref.html";

    @BeforeAll
    static void initI18n() {
        I18n.initFuerTest(Locale.GERMAN);
    }

    @Test
    void htmlExportFormat_allerSysteme_entsprichtReferenzdatei() throws Exception {
        assertThat(snapshot()).isEqualTo(ladeReferenz());
    }

    private String snapshot() {
        return String.join("\n\n", List.of(
                seite(TurnierSystem.SUPERMELEE, sectionsSupermelee()),
                seite(TurnierSystem.LIGA, LigaExportInVerzeichnis.htmlSections(
                        SheetNamen.meldeliste(), LigaSpielPlanSheet.sheetName(), "pdf/Spielplan.pdf",
                        SheetNamen.rangliste(), "pdf/Rangliste.pdf", SheetNamen.direktvergleich(), true)),
                seite(TurnierSystem.MAASTRICHTER, sectionsMaastrichter()),
                seite(TurnierSystem.SCHWEIZER, sectionsSchweizer()),
                seite(TurnierSystem.JGJ, sectionsJgj()),
                seite(TurnierSystem.KO, sectionsKo()),
                seite(TurnierSystem.POULE, sectionsPoule()),
                seite(TurnierSystem.KASKADE, sectionsKaskade()),
                seite(TurnierSystem.FORMULEX, sectionsFormuleX()),
                seite(TurnierSystem.TRIPTETE, sectionsTripTete())));
    }

    private String seite(TurnierSystem system, List<ExportHtmlSeite.Section> sections) {
        String html = ExportHtmlSeite.nurFuerTests()
                .titel(system.getBezeichnung())
                .logoUrl("https://example.org/" + system.name().toLowerCase(Locale.ROOT) + ".png")
                .sections(sections)
                .erstelleAusRendertHtml(tabellenHtml(sections));
        return system.name() + "\n" + htmlAuszug(html);
    }

    private String htmlAuszug(String html) {
        return html.lines()
                .map(String::trim)
                .filter(this::istRelevanteSnapshotZeile)
                .reduce((links, rechts) -> links + "\n" + rechts)
                .orElse("");
    }

    private boolean istRelevanteSnapshotZeile(String zeile) {
        return zeile.startsWith("<title>")
                || zeile.startsWith("<a href=\"#")
                || zeile.startsWith("<section ")
                || zeile.startsWith("<h2>")
                || zeile.startsWith("<a class=\"pdf-btn\"")
                || zeile.startsWith("<table>");
    }

    private List<String> tabellenHtml(List<ExportHtmlSeite.Section> sections) {
        return sections.stream()
                .map(section -> "<table><tbody><tr><td>" + section.titel() + "</td></tr></tbody></table>")
                .toList();
    }

    private List<ExportHtmlSeite.Section> sectionsSupermelee() {
        return List.of(
                section("endrangliste", I18n.get("export.supermelee.nav.endrangliste"),
                        SheetNamen.endrangliste(), "pdf/Endrangliste.pdf"),
                section("spieltag-1", I18n.get("export.supermelee.spieltag", 1),
                        SheetNamen.spieltagRangliste(1), "pdf/Spieltag-1.pdf"));
    }

    private List<ExportHtmlSeite.Section> sectionsMaastrichter() {
        return List.of(
                section("meldeliste", SheetNamen.meldeliste(), SheetNamen.meldeliste(), null),
                section("vorrunden-rangliste", SheetNamen.maastrichterVorrundenRangliste(),
                        SheetNamen.maastrichterVorrundenRangliste(), "pdf/Vorrunden-Rangliste.pdf"),
                section("ko-a", SheetNamen.koFinaleGruppe("A"), SheetNamen.koFinaleGruppe("A"), "pdf/A-Finale.pdf"));
    }

    private List<ExportHtmlSeite.Section> sectionsSchweizer() {
        return List.of(
                section("meldeliste", SheetNamen.meldeliste(), SheetNamen.meldeliste(), null),
                section("rangliste", SheetNamen.rangliste(), SheetNamen.rangliste(), "pdf/Rangliste.pdf"));
    }

    private List<ExportHtmlSeite.Section> sectionsJgj() {
        return List.of(
                section("meldeliste", SheetNamen.meldeliste(), SheetNamen.meldeliste(), null),
                section("spielplan", SheetNamen.spielplan(), SheetNamen.spielplan(), null),
                section("rangliste", SheetNamen.rangliste(), SheetNamen.rangliste(), "pdf/Rangliste.pdf"),
                section("direktvergleich", SheetNamen.direktvergleich(), SheetNamen.direktvergleich(),
                        "pdf/Direktvergleich.pdf"));
    }

    private List<ExportHtmlSeite.Section> sectionsKo() {
        return List.of(
                section("meldeliste", SheetNamen.meldeliste(), SheetNamen.meldeliste(), null),
                section("ko-turnierbaum", SheetNamen.koTurnierbaumEinzel(), SheetNamen.koTurnierbaumEinzel(),
                        "pdf/KO-Turnierbaum.pdf"));
    }

    private List<ExportHtmlSeite.Section> sectionsPoule() {
        return List.of(
                section("meldeliste", SheetNamen.meldeliste(), SheetNamen.meldeliste(), null),
                section("rangliste", SheetNamen.pouleVorrundenRangliste(), SheetNamen.pouleVorrundenRangliste(),
                        "pdf/Poule-Vorrunden-Rangliste.pdf"));
    }

    private List<ExportHtmlSeite.Section> sectionsKaskade() {
        return List.of(
                section("meldeliste", SheetNamen.meldeliste(), SheetNamen.meldeliste(), null),
                section("gruppenrangliste", SheetNamen.kaskadeGruppenrangliste(),
                        SheetNamen.kaskadeGruppenrangliste(), "pdf/Kaskaden-Gruppenrangliste.pdf"),
                section("feld-a", SheetNamen.kaskadenFeld("A"), SheetNamen.kaskadenFeld("A"),
                        "pdf/Kaskade-A-Feld.pdf"));
    }

    private List<ExportHtmlSeite.Section> sectionsFormuleX() {
        return List.of(
                section("meldeliste", SheetNamen.meldeliste(), SheetNamen.meldeliste(), null),
                section("rangliste", SheetNamen.formulexRangliste(), SheetNamen.formulexRangliste(),
                        "pdf/Formule-X-Rangliste.pdf"));
    }

    private List<ExportHtmlSeite.Section> sectionsTripTete() {
        return List.of(
                section("meldeliste", SheetNamen.meldeliste(), SheetNamen.meldeliste(), null),
                section("spielplan", SheetNamen.spielplan(), SheetNamen.spielplan(), "pdf/Spielplan.pdf"),
                section("rangliste", SheetNamen.rangliste(), SheetNamen.rangliste(), "pdf/Rangliste.pdf"));
    }

    private ExportHtmlSeite.Section section(String id, String titel, String sheetName, String pdfUrl) {
        return new ExportHtmlSeite.Section(id, titel, sheetName, pdfUrl);
    }

    private String ladeReferenz() throws IOException {
        try (var stream = AlleSystemeHtmlExportSnapshotTest.class.getResourceAsStream(REFERENZ_DATEI)) {
            assertThat(stream).as("Referenzdatei fehlt: %s", REFERENZ_DATEI).isNotNull();
            return normalisiere(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private String normalisiere(String html) {
        return html.replace("\r\n", "\n").trim();
    }
}
