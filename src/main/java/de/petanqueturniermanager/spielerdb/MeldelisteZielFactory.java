package de.petanqueturniermanager.spielerdb;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.kaskade.konfiguration.KaskadeKonfigurationSheet;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Erkennt anhand des Dokumenten-Turniersystems das passende Konfigurations-Sheet
 * und baut einen {@link MeldelisteZiel}-Adapter, der auf das Standard-Meldeliste-Sheet
 * schreibt. Liefert {@link Optional#empty()} wenn das Dokument kein bekanntes
 * (unterstütztes) Turniersystem hat oder kein Meldeliste-Sheet existiert.
 *
 * <p>Layout-Werte (Formation, Teamname-/Vereinsnamen-Anzeige) werden über die
 * system-spezifische {@code *KonfigurationSheet}-Klasse gelesen — damit greifen
 * deren Konfig-Defaults korrekt, und gleichzeitig persistiert
 * {@code BasePropertiesSpalte.readStringProperty} fehlende Werte einmalig in die
 * UserDefinedProperties (siehe {@code initStringPropertyIfAbsent}).
 */
public final class MeldelisteZielFactory {

    private static final Logger logger = LogManager.getLogger(MeldelisteZielFactory.class);

    private MeldelisteZielFactory() {}

    public static Optional<MeldelisteZiel> fuerAktivesSheet(WorkingSpreadsheet ws) {
        TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
        if (ts == null || ts == TurnierSystem.KEIN) {
            return Optional.empty();
        }
        Optional<MeldelisteLayout> layoutOpt = leseLayout(ts, ws);
        if (layoutOpt.isEmpty()) {
            return Optional.empty();
        }
        MeldelisteLayout l = layoutOpt.get();
        return SheetMeldelisteAdapter.fuer(ws, SheetNamen.meldeliste(), ts,
                l.formation(), l.teamnameAktiv(), l.vereinsnameAktiv());
    }

    /**
     * Liefert das Turniersystem des aktiven Dokuments, sofern es überhaupt eines hat,
     * aber die Spieler-DB-Übernahme nicht unterstützt (z.&nbsp;B. Liga). Für Dokumente
     * ohne Turniersystem (Stammdaten o.&nbsp;ä.) und für unterstützte Systeme wird
     * {@link Optional#empty()} zurückgegeben.
     */
    public static Optional<TurnierSystem> nichtUnterstuetztesSystem(WorkingSpreadsheet ws) {
        TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
        if (ts == null || ts == TurnierSystem.KEIN) {
            return Optional.empty();
        }
        return switch (ts) {
            case LIGA -> Optional.of(ts);
            default -> Optional.empty();
        };
    }

    /**
     * Layout-Triplet aus dem system-spezifischen KonfigurationSheet.
     */
    private record MeldelisteLayout(Formation formation, boolean teamnameAktiv, boolean vereinsnameAktiv) {}

    /**
     * Liest das Meldeliste-Layout aus dem zum Turniersystem passenden Konfigurations-Sheet.
     * Supermelee hat keine konfigurierbare Formation — dort gilt immer
     * {@link Formation#MELEE} und es gibt keine Teamname-/Vereinsname-Spalten.
     * Für Systeme ohne Spieler-DB-Übernahme-Unterstützung (Liga)
     * liefert die Methode {@link Optional#empty()}.
     */
    private static Optional<MeldelisteLayout> leseLayout(TurnierSystem ts, WorkingSpreadsheet ws) {
        try {
            return switch (ts) {
                case SUPERMELEE -> Optional.of(new MeldelisteLayout(Formation.MELEE, false, false));
                case KO -> {
                    KoKonfigurationSheet k = new KoKonfigurationSheet(ws);
                    yield Optional.of(new MeldelisteLayout(k.getMeldeListeFormation(),
                            k.isMeldeListeTeamnameAnzeigen(), k.isMeldeListeVereinsnameAnzeigen()));
                }
                case SCHWEIZER -> {
                    SchweizerKonfigurationSheet k = new SchweizerKonfigurationSheet(ws);
                    yield Optional.of(new MeldelisteLayout(k.getMeldeListeFormation(),
                            k.isMeldeListeTeamnameAnzeigen(), k.isMeldeListeVereinsnameAnzeigen()));
                }
                case MAASTRICHTER -> {
                    MaastrichterKonfigurationSheet k = new MaastrichterKonfigurationSheet(ws);
                    yield Optional.of(new MeldelisteLayout(k.getMeldeListeFormation(),
                            k.isMeldeListeTeamnameAnzeigen(), k.isMeldeListeVereinsnameAnzeigen()));
                }
                case JGJ -> {
                    JGJKonfigurationSheet k = new JGJKonfigurationSheet(ws);
                    yield Optional.of(new MeldelisteLayout(k.getMeldeListeFormation(),
                            k.isMeldeListeTeamnameAnzeigen(), k.isMeldeListeVereinsnameAnzeigen()));
                }
                case POULE -> {
                    PouleKonfigurationSheet k = new PouleKonfigurationSheet(ws);
                    yield Optional.of(new MeldelisteLayout(k.getMeldeListeFormation(),
                            k.isMeldeListeTeamnameAnzeigen(), k.isMeldeListeVereinsnameAnzeigen()));
                }
                case KASKADE -> {
                    KaskadeKonfigurationSheet k = new KaskadeKonfigurationSheet(ws);
                    yield Optional.of(new MeldelisteLayout(k.getMeldeListeFormation(),
                            k.isMeldeListeTeamnameAnzeigen(), k.isMeldeListeVereinsnameAnzeigen()));
                }
                case FORMULEX -> {
                    FormuleXKonfigurationSheet k = new FormuleXKonfigurationSheet(ws);
                    yield Optional.of(new MeldelisteLayout(k.getMeldeListeFormation(),
                            k.isMeldeListeTeamnameAnzeigen(), k.isMeldeListeVereinsnameAnzeigen()));
                }
                case LIGA, TRIPTETE, KEIN -> Optional.empty();
            };
        } catch (RuntimeException e) {
            logger.warn("Konfig-Layout für {} konnte nicht gelesen werden", ts, e);
            return Optional.empty();
        }
    }
}
