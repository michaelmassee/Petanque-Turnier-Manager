package de.petanqueturniermanager.webserver;

import java.util.Map;
import java.util.Optional;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;

/**
 * Generischer, system-agnostischer Resolver für eine Sheet-Rolle (z.B. Rangliste,
 * Meldeliste, aktuelle Spielrunde). Da pro Dokument genau ein Turniersystem aktiv ist
 * (Dokument-Property {@code Turniersystem}), genügt ein einziger Konfigurations-Schlüssel
 * pro Rolle: dieser Resolver liest das aktive System und delegiert an den dafür passenden
 * konkreten Resolver.
 * <p>
 * Wiederverwendung statt neuer Auflösungslogik: die Delegates sind die bestehenden
 * {@link MetadatenSheetResolver}, {@link MetadatenPrefixSheetResolver},
 * {@link SupermeleeAktiverSpielrundeSheetResolver} usw.
 */
public class AktivesSystemSheetResolver implements SheetResolver {

    private final Map<TurnierSystem, SheetResolver> delegatePerSystem;
    private final String anzeigeName;

    /** Zuletzt benutzter Delegate – für {@link #getNummer(XSpreadsheet)}. */
    private volatile SheetResolver letzterDelegate;

    public AktivesSystemSheetResolver(Map<TurnierSystem, SheetResolver> delegatePerSystem,
            String anzeigeName) {
        this.delegatePerSystem = Map.copyOf(delegatePerSystem);
        this.anzeigeName = anzeigeName;
    }

    SheetResolver delegateFuerTest(TurnierSystem system) {
        return delegatePerSystem.get(system);
    }

    @Override
    public Optional<XSpreadsheet> resolve(WorkingSpreadsheet ws) {
        var doc = ws.getWorkingSpreadsheetDocument();
        if (doc == null) {
            return Optional.empty();
        }
        TurnierSystem aktiv = new DocumentPropertiesHelper(doc).getTurnierSystemAusDocument();
        SheetResolver delegate = aktiv == null ? null : delegatePerSystem.get(aktiv);
        if (delegate == null) {
            return Optional.empty();
        }
        letzterDelegate = delegate;
        return delegate.resolve(ws);
    }

    @Override
    public String getAnzeigeName() {
        return anzeigeName;
    }

    @Override
    public Optional<Integer> getNummer(XSpreadsheet sheet) {
        SheetResolver delegate = letzterDelegate;
        return delegate == null ? Optional.empty() : delegate.getNummer(sheet);
    }
}
