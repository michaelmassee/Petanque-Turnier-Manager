package de.petanqueturniermanager.spielerdb.ui;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.spielerdb.LabelRepository;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;
import de.petanqueturniermanager.spielerdb.SpielerDbConnection;
import de.petanqueturniermanager.spielerdb.SpielerDbException;
import de.petanqueturniermanager.spielerdb.SpielerRepository;
import de.petanqueturniermanager.spielerdb.VereinRepository;
import de.petanqueturniermanager.spielerdb.export.SpielerDbExportLoader;
import de.petanqueturniermanager.spielerdb.vorlage.SheetVorlageAdapter;
import de.petanqueturniermanager.spielerdb.vorlage.SpielerDbVorlageSheet;
import de.petanqueturniermanager.exception.GenerateException;

/**
 * Bindeglied zwischen {@link de.petanqueturniermanager.comp.ProtocolHandler}
 * und den Spieler-DB-Dialogen. Stellt die DB-Connection her, fängt
 * Connection-Fehler einheitlich (Lock-Hinweis vs. generischer Init-Fehler) ab
 * und öffnet je nach Menüpunkt einen der drei Dialoge.
 */
public final class SpielerDbDispatcher {

    private static final Logger logger = LogManager.getLogger(SpielerDbDispatcher.class);

    private SpielerDbDispatcher() {}

    public static void oeffneSpielerVerwaltung(WorkingSpreadsheet ws) {
        Optional<SpielerDbConnection> conn = oeffneOderMelde(ws.getxContext());
        if (conn.isEmpty()) {
            return;
        }
        ProcessBox pb = ProcessBox.from();
        boolean warSichtbar = pb.istSichtbar();
        if (warSichtbar) {
            pb.hide();
        }
        try {
            new SpielerSucheDialog(ws.getxContext(),
                    new SpielerRepository(conn.get()),
                    new VereinRepository(conn.get()),
                    new LabelRepository(conn.get()),
                    null /* Verwaltungsmodus */).zeigen();
        } catch (com.sun.star.uno.Exception | RuntimeException e) {
            logger.error("Spieler-Suche-Dialog fehlgeschlagen", e);
        } finally {
            if (warSichtbar) {
                pb.visible();
            }
        }
    }

    public static void uebernehmenInMeldeliste(WorkingSpreadsheet ws) {
        XComponentContext ctx = ws.getxContext();
        Optional<MeldelisteZiel> ziel = MeldelisteZielFactory.fuerAktivesSheet(ws);
        if (ziel.isEmpty()) {
            MessageBox.from(ctx, MessageBoxTypeEnum.WARN_OK)
                    .caption(I18n.get("spielerdb.menu.toplevel"))
                    .message(I18n.get("spielerdb.fehler.keine_meldeliste"))
                    .show();
            return;
        }
        Optional<SpielerDbConnection> conn = oeffneOderMelde(ctx);
        if (conn.isEmpty()) {
            return;
        }
        aktiviereMeldelisteSheet(ws);
        ProcessBox pb = ProcessBox.from();
        boolean warSichtbar = pb.istSichtbar();
        if (warSichtbar) {
            pb.hide();
        }
        try {
            new SpielerSucheDialog(ctx,
                    new SpielerRepository(conn.get()),
                    new VereinRepository(conn.get()),
                    new LabelRepository(conn.get()),
                    ziel.get()).zeigen();
        } catch (com.sun.star.uno.Exception e) {
            logger.error("Spieler-Suche-Dialog fehlgeschlagen", e);
        } finally {
            if (warSichtbar) {
                pb.visible();
            }
        }
    }

    public static void abgleichMitMeldeliste(WorkingSpreadsheet ws) {
        XComponentContext ctx = ws.getxContext();
        Optional<MeldelisteZiel> ziel = MeldelisteZielFactory.fuerAktivesSheet(ws);
        if (ziel.isEmpty()) {
            MessageBox.from(ctx, MessageBoxTypeEnum.WARN_OK)
                    .caption(I18n.get("spielerdb.menu.toplevel"))
                    .message(I18n.get("spielerdb.fehler.keine_meldeliste"))
                    .show();
            return;
        }
        Optional<SpielerDbConnection> conn = oeffneOderMelde(ctx);
        if (conn.isEmpty()) {
            return;
        }
        ProcessBox pb = ProcessBox.from();
        boolean warSichtbar = pb.istSichtbar();
        if (warSichtbar) {
            pb.hide();
        }
        try {
            new SpielerDbAbgleichDialog(ctx,
                    new SpielerRepository(conn.get()),
                    new VereinRepository(conn.get()),
                    ziel.get()).zeigen();
        } catch (com.sun.star.uno.Exception | RuntimeException e) {
            logger.error("Spieler-DB-Abgleich-Dialog fehlgeschlagen", e);
        } finally {
            if (warSichtbar) {
                pb.visible();
            }
        }
    }

    public static void vorlageErstellen(WorkingSpreadsheet ws) {
        XComponentContext ctx = ws.getxContext();
        try {
            SpielerDbVorlageSheet.bereitstellen(ws);
        } catch (GenerateException | RuntimeException e) {
            logger.error("Vorlage-Sheet konnte nicht angelegt werden", e);
            MessageBox.from(ctx, MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("spielerdb.fehler.titel"))
                    .message(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())
                    .show();
        }
    }

    public static void abgleichMitVorlage(WorkingSpreadsheet ws) {
        XComponentContext ctx = ws.getxContext();
        Optional<SpielerDbConnection> conn = oeffneOderMelde(ctx);
        if (conn.isEmpty()) {
            return;
        }
        SheetVorlageAdapter quelle = new SheetVorlageAdapter(ws);
        // Eager-Probe: Sheet-Existenz und Header-Validierung VOR Dialog-Öffnung,
        // damit Fehlermeldungen als saubere MessageBox erscheinen statt als
        // leerer Dialog mit „0 Spieler gefunden".
        try {
            quelle.leseAlleSpielerRoh();
        } catch (SheetVorlageAdapter.VorlageNichtVerfuegbarException e) {
            MessageBox.from(ctx, MessageBoxTypeEnum.WARN_OK)
                    .caption(I18n.get("spielerdb.menu.toplevel"))
                    .message(e.getMessage())
                    .show();
            return;
        }

        ProcessBox pb = ProcessBox.from();
        boolean warSichtbar = pb.istSichtbar();
        if (warSichtbar) {
            pb.hide();
        }
        try {
            new SpielerDbAbgleichDialog(ctx,
                    new SpielerRepository(conn.get()),
                    new VereinRepository(conn.get()),
                    quelle, quelle).zeigen();
        } catch (com.sun.star.uno.Exception | RuntimeException e) {
            logger.error("Spieler-DB-Abgleich-Dialog (Vorlage) fehlgeschlagen", e);
        } finally {
            if (warSichtbar) {
                pb.visible();
            }
        }
    }

    public static void oeffneVereinsVerwaltung(WorkingSpreadsheet ws) {
        Optional<SpielerDbConnection> conn = oeffneOderMelde(ws.getxContext());
        if (conn.isEmpty()) {
            return;
        }
        try {
            new VereinVerwaltenDialog(ws.getxContext(),
                    new VereinRepository(conn.get())).zeigen();
        } catch (com.sun.star.uno.Exception e) {
            logger.error("Verein-Verwalten-Dialog fehlgeschlagen", e);
        }
    }

    public static void exportSpielerDb(WorkingSpreadsheet ws) {
        Optional<SpielerDbConnection> conn = oeffneOderMelde(ws.getxContext());
        if (conn.isEmpty()) {
            return;
        }
        try {
            SpielerDbExportLoader exportLoader = new SpielerDbExportLoader(
                    new SpielerRepository(conn.get()),
                    new VereinRepository(conn.get()),
                    new LabelRepository(conn.get()));
            new SpielerDbExportDialog(ws.getxContext(), exportLoader, conn.get()).zeigen();
        } catch (com.sun.star.uno.Exception | RuntimeException e) {
            logger.error("Spieler-DB-Export-Dialog fehlgeschlagen", e);
        }
    }

    public static void importSpielerDb(WorkingSpreadsheet ws) {
        Optional<SpielerDbConnection> conn = oeffneOderMelde(ws.getxContext());
        if (conn.isEmpty()) {
            return;
        }
        try {
            new SpielerDbImportDialog(ws.getxContext(), conn.get()).zeigen();
        } catch (com.sun.star.uno.Exception | RuntimeException e) {
            logger.error("Spieler-DB-Import-Dialog fehlgeschlagen", e);
        }
    }

    public static void oeffneLabelVerwaltung(WorkingSpreadsheet ws) {
        Optional<SpielerDbConnection> conn = oeffneOderMelde(ws.getxContext());
        if (conn.isEmpty()) {
            return;
        }
        try {
            new LabelVerwaltenDialog(ws.getxContext(),
                    new LabelRepository(conn.get())).zeigen();
        } catch (com.sun.star.uno.Exception e) {
            logger.error("Label-Verwalten-Dialog fehlgeschlagen", e);
        }
    }

    /**
     * Aktiviert das Meldeliste-Sheet im Vordergrund, damit der Anwender beim
     * Öffnen des Übernahme-Dialogs den Schreibkontext sieht. Fehler werden
     * stillschweigend geloggt — die Aktivierung ist UX-Komfort und darf den
     * Dialog-Workflow nicht blockieren.
     */
    private static void aktiviereMeldelisteSheet(WorkingSpreadsheet ws) {
        try {
            SheetHelper sh = new SheetHelper(ws);
            XSpreadsheet meldeliste = sh.findByName(SheetNamen.meldeliste());
            if (meldeliste != null) {
                sh.setActiveSheet(meldeliste);
            }
        } catch (RuntimeException e) {
            logger.warn("Meldeliste-Sheet konnte nicht aktiviert werden", e);
        }
    }

    private static Optional<SpielerDbConnection> oeffneOderMelde(XComponentContext ctx) {
        try {
            return Optional.of(SpielerDbConnection.getInstance());
        } catch (SpielerDbException e) {
            logger.error("Spieler-DB konnte nicht geöffnet werden", e);
            String text = e.istLockFehler()
                    ? I18n.get("spielerdb.fehler.db_locked")
                    : I18n.get("spielerdb.fehler.dbinit",
                            e.getMessage() == null ? "" : e.getMessage());
            MessageBox.from(ctx, MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("spielerdb.fehler.titel"))
                    .message(text)
                    .show();
            return Optional.empty();
        } catch (RuntimeException | LinkageError e) {
            // LinkageError: schützt vor stillem JVM-Tod bei künftigen
            // Classloader-Konflikten (vgl. HSQLDB-Shading, build.gradle).
            logger.error("Spieler-DB: unerwarteter Fehler beim Initialisieren", e);
            MessageBox.from(ctx, MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("spielerdb.fehler.titel"))
                    .message(I18n.get("spielerdb.fehler.dbinit",
                            e.getClass().getSimpleName() + ": " + e.getMessage()))
                    .show();
            return Optional.empty();
        }
    }
}
