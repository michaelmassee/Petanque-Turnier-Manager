/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Zeigt Release-Informationen der neuesten GitHub-Version in der ProcessBox an.
 *
 * @author Michael Massee
 */
public class ReleaseInfosAnzeigen extends SheetRunner {

    public ReleaseInfosAnzeigen(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.KEIN, "Release-Infos");
    }

    @Override
    protected @Nullable IKonfigurationSheet getKonfigurationSheet() {
        return null;
    }

    /** Factory-Methode – kann in Tests überschrieben werden. */
    Optional<ReleaseInfo> aktuellesReleaseLesen() {
        try {
            return ReleaseUpdateService.get().getAktuellesRelease();
        } catch (IllegalStateException e) {
            return Optional.empty();
        }
    }

    @Override
    protected void doRun() throws GenerateException {
        var release = aktuellesReleaseLesen().orElse(null);
        if (release == null) {
            processBox().fehler("Keine Release-Informationen verfügbar.");
            return;
        }
        processBoxinfo("processbox.release.info", release.name());
        processBoxinfo("processbox.release.infos");
        var body = release.body();
        processBoxinfo(body != null && !body.isBlank() ? body : I18n.get("processbox.keine.beschreibung"));
    }
}
