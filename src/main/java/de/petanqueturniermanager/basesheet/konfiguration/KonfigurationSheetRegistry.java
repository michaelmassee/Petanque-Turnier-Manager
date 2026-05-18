/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.kaskade.konfiguration.KaskadeKonfigurationSheet;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.liga.konfiguration.LigaKonfigurationSheet;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Zentrale Registry, die zu einem {@link TurnierSystem} die passende
 * {@link BaseKonfigurationSheet}-Instanz liefert. Wird vom
 * {@link SeitenstileDebouncer} verwendet, um den korrekten Konfig-Sheet ohne
 * weitere Switch-Tabelle zu konstruieren.
 *
 * <p>Refactor-Notiz: {@code KonfigurationSingleton.getKonfigProperties()} hat
 * eine identische Switch-Liste; perspektivisch kann diese Klasse die
 * Single-Source-of-Truth für „TurnierSystem → Konfig-Sheet/Properties" werden.
 */
public final class KonfigurationSheetRegistry {

    private static final Logger logger = LogManager.getLogger(KonfigurationSheetRegistry.class);

    private static final Map<TurnierSystem, Function<WorkingSpreadsheet, BaseKonfigurationSheet>> FACTORIES = Map.of(
            TurnierSystem.LIGA,         LigaKonfigurationSheet::new,
            TurnierSystem.MAASTRICHTER, MaastrichterKonfigurationSheet::new,
            TurnierSystem.SUPERMELEE,   SuperMeleeKonfigurationSheet::new,
            TurnierSystem.SCHWEIZER,    SchweizerKonfigurationSheet::new,
            TurnierSystem.JGJ,          JGJKonfigurationSheet::new,
            TurnierSystem.KO,           KoKonfigurationSheet::new,
            TurnierSystem.POULE,        PouleKonfigurationSheet::new,
            TurnierSystem.KASKADE,      KaskadeKonfigurationSheet::new,
            TurnierSystem.FORMULEX,     FormuleXKonfigurationSheet::new);

    private KonfigurationSheetRegistry() {
    }

    /**
     * Liefert eine frische {@link BaseKonfigurationSheet}-Instanz, die zum
     * Turniersystem des Dokuments passt. Bei {@link TurnierSystem#KEIN} oder
     * unbekanntem System {@link Optional#empty()}.
     */
    public static Optional<BaseKonfigurationSheet> fuerAktivesDokument(WorkingSpreadsheet ws) {
        if (ws == null) {
            return Optional.empty();
        }
        try {
            TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
            if (ts == null || ts == TurnierSystem.KEIN) {
                return Optional.empty();
            }
            Function<WorkingSpreadsheet, BaseKonfigurationSheet> factory = FACTORIES.get(ts);
            if (factory == null) {
                logger.warn("Kein KonfigurationSheet registriert für Turniersystem {}", ts);
                return Optional.empty();
            }
            return Optional.of(factory.apply(ws));
        } catch (RuntimeException e) {
            logger.warn("KonfigurationSheet konnte nicht erzeugt werden", e);
            return Optional.empty();
        }
    }
}
