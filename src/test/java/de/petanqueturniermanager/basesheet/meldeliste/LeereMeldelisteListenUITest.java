package de.petanqueturniermanager.basesheet.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXCheckinListeSheet;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetNew;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXTeilnehmerSheet;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJCheckinListeSheet;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_New;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJTeilnehmerSheet;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeCheckinListeSheet;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeMeldeListeSheetNew;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeTeilnehmerSheet;
import de.petanqueturniermanager.ko.meldeliste.KoCheckinListeSheet;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetNew;
import de.petanqueturniermanager.ko.meldeliste.KoTeilnehmerSheet;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterCheckinListeSheet;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetNew;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterTeilnehmerSheet;
import de.petanqueturniermanager.poule.meldeliste.PouleCheckinListeSheet;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetNew;
import de.petanqueturniermanager.poule.meldeliste.PouleTeilnehmerSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerCheckinListeSheet;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerTeilnehmerSheet;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;
import de.petanqueturniermanager.supermelee.meldeliste.AnmeldungenSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;
import de.petanqueturniermanager.supermelee.meldeliste.SupermeleeTeilnehmerSheet;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteCheckinListeSheet;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetNew;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteTeilnehmerSheet;

class LeereMeldelisteListenUITest extends BaseCalcUITest {

    @Test
    void supermeleeTeilnehmerUndCheckinFunktionierenOhneMeldungen() {
        assertThatCode(() -> {
            new MeldeListeSheet_New(wkingSpreadsheet).createMeldelisteWithParams(SuperMeleeMode.Triplette);
            new SupermeleeTeilnehmerSheet(wkingSpreadsheet).run();
            new AnmeldungenSheet(wkingSpreadsheet).run();
        }).doesNotThrowAnyException();
        assertListenExistieren(SheetNamen.teilnehmer(1), SheetNamen.checkinListe(1));
    }

    @Test
    void schweizerTeilnehmerUndCheckinFunktionierenOhneMeldungen() {
        assertThatCode(() -> {
            new SchweizerMeldeListeSheetNew(wkingSpreadsheet)
                    .createMeldelisteWithParams(Formation.DOUBLETTE, false, false);
            new SchweizerTeilnehmerSheet(wkingSpreadsheet).run();
            new SchweizerCheckinListeSheet(wkingSpreadsheet).run();
        }).doesNotThrowAnyException();
        assertListenExistieren(SheetNamen.teilnehmer(), SheetNamen.checkinListe());
    }

    @Test
    void jederGegenJedenTeilnehmerUndCheckinFunktionierenOhneMeldungen() {
        assertThatCode(() -> {
            new JGJMeldeListeSheet_New(wkingSpreadsheet)
                    .createMeldelisteWithParams(Formation.DOUBLETTE, false, false, SpielplanTeamAnzeige.NR);
            new JGJTeilnehmerSheet(wkingSpreadsheet).run();
            new JGJCheckinListeSheet(wkingSpreadsheet).run();
        }).doesNotThrowAnyException();
        assertListenExistieren(SheetNamen.teilnehmer(), SheetNamen.checkinListe());
    }

    @Test
    void koTeilnehmerUndCheckinFunktionierenOhneMeldungen() {
        assertThatCode(() -> {
            new KoMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams();
            new KoTeilnehmerSheet(wkingSpreadsheet).run();
            new KoCheckinListeSheet(wkingSpreadsheet).run();
        }).doesNotThrowAnyException();
        assertListenExistieren(SheetNamen.teilnehmer(), SheetNamen.checkinListe());
    }

    @Test
    void maastrichterTeilnehmerUndCheckinFunktionierenOhneMeldungen() {
        assertThatCode(() -> {
            new MaastrichterMeldeListeSheetNew(wkingSpreadsheet)
                    .erstelleMeldeliste(Formation.DOUBLETTE, false, false, SpielplanTeamAnzeige.NR);
            new MaastrichterTeilnehmerSheet(wkingSpreadsheet).run();
            new MaastrichterCheckinListeSheet(wkingSpreadsheet).run();
        }).doesNotThrowAnyException();
        assertListenExistieren(SheetNamen.teilnehmer(), SheetNamen.checkinListe());
    }

    @Test
    void kaskadeTeilnehmerUndCheckinFunktionierenOhneMeldungen() {
        assertThatCode(() -> {
            new KaskadeMeldeListeSheetNew(wkingSpreadsheet)
                    .createMeldelisteWithParams(Formation.DOUBLETTE, false, false, 4);
            new KaskadeTeilnehmerSheet(wkingSpreadsheet).run();
            new KaskadeCheckinListeSheet(wkingSpreadsheet).run();
        }).doesNotThrowAnyException();
        assertListenExistieren(SheetNamen.teilnehmer(), SheetNamen.checkinListe());
    }

    @Test
    void formuleXTeilnehmerUndCheckinFunktionierenOhneMeldungen() {
        assertThatCode(() -> {
            new FormuleXMeldeListeSheetNew(wkingSpreadsheet)
                    .createMeldelisteWithParams(Formation.DOUBLETTE, false, false, 4);
            new FormuleXTeilnehmerSheet(wkingSpreadsheet).run();
            new FormuleXCheckinListeSheet(wkingSpreadsheet).run();
        }).doesNotThrowAnyException();
        assertListenExistieren(SheetNamen.teilnehmer(), SheetNamen.checkinListe());
    }

    @Test
    void pouleTeilnehmerUndCheckinFunktionierenOhneMeldungen() {
        assertThatCode(() -> {
            new PouleMeldeListeSheetNew(wkingSpreadsheet)
                    .createMeldelisteWithParams(Formation.DOUBLETTE, false, false);
            new PouleTeilnehmerSheet(wkingSpreadsheet).run();
            new PouleCheckinListeSheet(wkingSpreadsheet).run();
        }).doesNotThrowAnyException();
        assertListenExistieren(SheetNamen.teilnehmer(), SheetNamen.checkinListe());
    }

    @Test
    void tripTeteTeilnehmerUndCheckinFunktionierenOhneMeldungen() {
        assertThatCode(() -> {
            new TripTeteMeldeListeSheetNew(wkingSpreadsheet).createMeldeliste();
            new TripTeteTeilnehmerSheet(wkingSpreadsheet).run();
            new TripTeteCheckinListeSheet(wkingSpreadsheet).run();
        }).doesNotThrowAnyException();
        assertListenExistieren(SheetNamen.teilnehmer(), SheetNamen.checkinListe());
    }

    private void assertListenExistieren(String teilnehmerSheetName, String checkinSheetName) {
        assertThat(sheetHlp.findByName(teilnehmerSheetName)).as(teilnehmerSheetName).isNotNull();
        assertThat(sheetHlp.findByName(checkinSheetName)).as(checkinSheetName).isNotNull();
    }
}
