/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;
import de.petanqueturniermanager.formulex.rangliste.FormuleXRanglisteSheet;
import de.petanqueturniermanager.formulex.spielrunde.FormuleXAbstractSpielrundeSheet;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;

/**
 * Liefert je Turniersystem die Quelle des Live-Stands. KO, Kaskade und Poule übertragen keine Rangliste: dort
 * gibt es keinen durchgehenden Gesamtplatz (PTM-Online zeigt bei KO ebenfalls keine Rangliste).
 */
public final class LiveStandQuellen {

    /** Schweizer Rangliste; die Maastrichter-Vorrunden-Rangliste hat dasselbe Layout. */
    private static final RanglistenBlattLeser.Spalten SCHWEIZER_RANGLISTE = new RanglistenBlattLeser.Spalten(
            SchweizerRanglisteSheet.TEAM_NR_SPALTE, SchweizerRanglisteSheet.PLATZ_SPALTE,
            SchweizerRanglisteSheet.SIEGE_SPALTE, SchweizerRanglisteSheet.PUNKTE_PLUS_SPALTE,
            SchweizerRanglisteSheet.PUNKTE_MINUS_SPALTE);

    private static final RanglistenBlattLeser.Spalten FORMULEX_RANGLISTE = new RanglistenBlattLeser.Spalten(
            FormuleXRanglisteSheet.TEAM_NR_SPALTE, FormuleXRanglisteSheet.PLATZ_SPALTE,
            FormuleXRanglisteSheet.SIEGE_SPALTE, FormuleXRanglisteSheet.PUNKTE_PLUS_SPALTE,
            FormuleXRanglisteSheet.PUNKTE_MINUS_SPALTE);

    private LiveStandQuellen() {}

    /**
     * @param spieltagNr Spieltag der Verbindung (nur Supermelee)
     * @return leer, wenn das System keine Live-Ansicht unterstützt
     */
    public static Optional<LiveStandQuelle> fuer(WorkingSpreadsheet ws, TurnierSystem ts,
            @Nullable Integer spieltagNr) {
        return Optional.ofNullable(switch (ts) {
            case SUPERMELEE -> spieltagNr == null ? null : new SupermeleeLiveQuelle(ws, spieltagNr);
            case SCHWEIZER -> () -> schweizer(ws);
            case FORMULEX -> () -> formuleX(ws);
            case MAASTRICHTER -> () -> maastrichter(ws);
            case JGJ -> new JgjLiveQuelle(ws);
            case TRIPTETE -> new TripTeteLiveQuelle(ws);
            case POULE -> new PouleLiveQuelle(ws);
            case KASKADE -> new KaskadeLiveQuelle(ws);
            case KO -> () -> ko(ws);
            case LIGA, KEIN -> null;
        });
    }

    private static LiveTurnierStand schweizer(WorkingSpreadsheet ws) throws GenerateException {
        boolean mitBahn = mitBahn(new SchweizerKonfigurationSheet(ws).getSpielrundeSpielbahn());
        List<List<LivePartie>> runden = new TeamSpielrundenLeser(ws,
                SheetMetadataHelper::schluesselSchweizerSpielrunde, SheetNamen::spielrunde,
                SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE, mitBahn).leseRunden();
        RanglistenBlattLeser rangliste = new RanglistenBlattLeser(SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_RANGLISTE,
                SheetNamen.rangliste(), SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE, SCHWEIZER_RANGLISTE);
        return new LiveTurnierStand(new LiveRundenFolge().anhaengen(runden).runden(), rangliste.lese(ws));
    }

    private static LiveTurnierStand formuleX(WorkingSpreadsheet ws) throws GenerateException {
        boolean mitBahn = mitBahn(new FormuleXKonfigurationSheet(ws).getSpielrundeSpielbahn());
        List<List<LivePartie>> runden = new TeamSpielrundenLeser(ws,
                SheetMetadataHelper::schluesselFormuleXSpielrunde, SheetNamen::formulexSpielrunde,
                FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE, mitBahn).leseRunden();
        RanglistenBlattLeser rangliste = new RanglistenBlattLeser(SheetMetadataHelper.SCHLUESSEL_FORMULEX_RANGLISTE,
                SheetNamen.formulexRangliste(), FormuleXRanglisteSheet.ERSTE_DATEN_ZEILE, FORMULEX_RANGLISTE);
        return new LiveTurnierStand(new LiveRundenFolge().anhaengen(runden).runden(), rangliste.lese(ws));
    }

    /** Vorrunden im Schweizer Layout, danach die gleichzeitig gespielten Finalrunden; Rangliste der Vorrunden. */
    private static LiveTurnierStand maastrichter(WorkingSpreadsheet ws) throws GenerateException {
        boolean mitBahn = mitBahn(new MaastrichterKonfigurationSheet(ws).getSpielrundeSpielbahn());
        List<List<LivePartie>> vorrunden = new TeamSpielrundenLeser(ws,
                SheetMetadataHelper::schluesselMaastrichterVorrunde, SheetNamen::maastrichterVorrunde,
                SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE, mitBahn).leseRunden();
        List<List<List<LivePartie>>> finalrunden = TurnierbaumLiveLeser.leseBaeume(ws,
                SheetMetadataHelper::schluesselMaastrichterFinalrunde, SheetNamen::koFinaleGruppe);
        RanglistenBlattLeser rangliste = new RanglistenBlattLeser(
                SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDEN_RANGLISTE,
                SheetNamen.maastrichterVorrundenRangliste(), SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE,
                SCHWEIZER_RANGLISTE);
        return new LiveTurnierStand(
                new LiveRundenFolge().anhaengen(vorrunden).parallelAnhaengen(finalrunden).runden(),
                rangliste.lese(ws));
    }

    /** Ein einzelner Turnierbaum oder gleichzeitig gespielte Turnierbäume A–H. */
    private static LiveTurnierStand ko(WorkingSpreadsheet ws) {
        List<List<LivePartie>> einzel = TurnierbaumLiveLeser.leseRunden(ws,
                SheetMetadataHelper.schluesselKoTurnierbaum(""), null);
        LiveRundenFolge folge = new LiveRundenFolge();
        if (einzel.isEmpty()) {
            folge.parallelAnhaengen(TurnierbaumLiveLeser.leseBaeume(ws, SheetMetadataHelper::schluesselKoTurnierbaum,
                    SheetNamen::koTurnierbaumGruppe));
        } else {
            folge.anhaengen(einzel);
        }
        return LiveTurnierStand.ohneRangliste(folge.runden());
    }

    private static boolean mitBahn(SpielrundeSpielbahn spielbahn) {
        return spielbahn != SpielrundeSpielbahn.X;
    }
}
