package de.petanqueturniermanager.spielerdb.ui;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;
import de.petanqueturniermanager.spielerdb.SpielerDbException;
import de.petanqueturniermanager.spielerdb.VereinDatensatz;
import de.petanqueturniermanager.spielerdb.VereinRepository;
import de.petanqueturniermanager.spielerdb.VereinRepository.DuplikatException;
import de.petanqueturniermanager.spielerdb.VereinRepository.InBenutzungException;

/**
 * Verwaltung der Vereins-Stammdaten: Liste mit allen Vereinen + Buttons für
 * Anlegen, Bearbeiten, Löschen. Löschen prüft per {@code countSpieler} — wenn
 * Spieler zugeordnet sind, wird abgebrochen mit RESTRICT-Hinweis. Sonst
 * Yes/No-Bestätigung mit Default „Nein".
 */
public final class VereinVerwaltenDialog extends AbstractUnoDialog {

    private static final Logger logger = LogManager.getLogger(VereinVerwaltenDialog.class);

    private static final int B = 320;
    private static final int H = 280;
    private static final int LIST_X = 8, LIST_Y = 12, LIST_W = 200, LIST_H = 240;
    private static final int BTN_X = 215, BTN_W = 95, BTN_H = 14;

    private final VereinRepository vereinRepo;

    @Nullable private UnoControlsHelper controls;
    private List<VereinDatensatz> aktuelleListe = List.of();

    public VereinVerwaltenDialog(XComponentContext xContext, VereinRepository vereinRepo) {
        super(xContext);
        this.vereinRepo = vereinRepo;
    }

    public void zeigen() throws com.sun.star.uno.Exception {
        erstelleUndAusfuehren();
    }

    @Override protected String getTitel() { return I18n.get("spielerdb.verein.titel"); }
    @Override protected int getBreite() { return B; }
    @Override protected int getHoehe() { return H; }

    @Override
    protected void erstelleFelder(XMultiComponentFactory mcf, XMultiServiceFactory xMSF,
            XNameContainer cont, XToolkit xToolkit, XWindowPeer peer,
            XPropertySet dlgProps, XDialog xDialog) throws com.sun.star.uno.Exception {

        XControlContainer xcc = Lo.qi(XControlContainer.class, xDialog);
        this.controls = new UnoControlsHelper(xMSF, cont, xcc);

        controls.listBox("lstVereine", LIST_X, LIST_Y, LIST_W, LIST_H);

        int by = LIST_Y;
        controls.button("btnNeu", I18n.get("spielerdb.verein.btn.neu"), BTN_X, by, BTN_W, BTN_H);
        by += 18;
        controls.button("btnBearbeiten", I18n.get("spielerdb.verein.btn.bearbeiten"), BTN_X, by, BTN_W, BTN_H);
        by += 18;
        controls.button("btnLoeschen", I18n.get("spielerdb.verein.btn.loeschen"), BTN_X, by, BTN_W, BTN_H);
        controls.button("btnSchliessen", I18n.get("spielerdb.suche.btn.schliessen"),
                BTN_X, H - 22, BTN_W, BTN_H, (short) PushButtonType.OK_value);

        controls.registriereActionListener("btnNeu", this::beimNeu);
        controls.registriereActionListener("btnBearbeiten", this::beimBearbeiten);
        controls.registriereActionListener("btnLoeschen", this::beimLoeschen);
        // Doppelklick auf Liste → Bearbeiten
        controls.registriereListBoxAuswahl("lstVereine", () -> { /* selection only */ }, this::beimBearbeiten);

        ladeListe();
    }

    private void ladeListe() {
        UnoControlsHelper c = this.controls;
        if (c == null) {
            return;
        }
        try {
            aktuelleListe = vereinRepo.findAll();
            c.setzeListItems("lstVereine",
                    aktuelleListe.stream().map(VereinDatensatz::name).toArray(String[]::new));
        } catch (SpielerDbException e) {
            logger.error("Vereine laden fehlgeschlagen", e);
            zeigeFehler(I18n.get("spielerdb.fehler.dbinit", e.getMessage()));
        }
    }

    private void beimNeu() {
        Optional<String> eingabe = neuerNameDialog(null);
        if (eingabe.isEmpty()) {
            return;
        }
        try {
            vereinRepo.insert(eingabe.get());
            ladeListe();
        } catch (DuplikatException e) {
            zeigeFehler(I18n.get("spielerdb.fehler.verein_name_doppelt", eingabe.get()));
        } catch (SpielerDbException e) {
            logger.error("Verein anlegen fehlgeschlagen", e);
            zeigeFehler(e.getMessage());
        }
    }

    private void beimBearbeiten() {
        Optional<VereinDatensatz> sel = ausgewaehlt();
        if (sel.isEmpty()) {
            return;
        }
        Integer nr = sel.get().nr();
        if (nr == null) {
            return;
        }
        Optional<String> eingabe = neuerNameDialog(sel.get().name());
        if (eingabe.isEmpty()) {
            return;
        }
        try {
            vereinRepo.update(nr, eingabe.get());
            ladeListe();
        } catch (DuplikatException e) {
            zeigeFehler(I18n.get("spielerdb.fehler.verein_name_doppelt", eingabe.get()));
        } catch (SpielerDbException e) {
            logger.error("Verein bearbeiten fehlgeschlagen", e);
            zeigeFehler(e.getMessage());
        }
    }

    private void beimLoeschen() {
        Optional<VereinDatensatz> sel = ausgewaehlt();
        if (sel.isEmpty()) {
            return;
        }
        Integer nr = sel.get().nr();
        if (nr == null) {
            return;
        }
        try {
            int anz = vereinRepo.countSpieler(nr);
            if (anz > 0) {
                zeigeFehler(I18n.get("spielerdb.fehler.verein_in_benutzung", sel.get().name(), anz));
                return;
            }
            MessageBoxResult res = MessageBox.from(xContext, MessageBoxTypeEnum.QUESTION_YES_NO)
                    .caption(I18n.get("spielerdb.frage.titel"))
                    .message(I18n.get("spielerdb.frage.verein_loeschen", sel.get().name()))
                    .show();
            if (res != MessageBoxResult.YES) {
                return;
            }
            vereinRepo.delete(nr);
            ladeListe();
        } catch (InBenutzungException e) {
            zeigeFehler(I18n.get("spielerdb.fehler.verein_in_benutzung", sel.get().name(), "?"));
        } catch (SpielerDbException e) {
            logger.error("Verein löschen fehlgeschlagen", e);
            zeigeFehler(e.getMessage());
        }
    }

    private Optional<VereinDatensatz> ausgewaehlt() {
        UnoControlsHelper c = this.controls;
        if (c == null) {
            return Optional.empty();
        }
        short idx = c.ausgewaehlterIndex("lstVereine");
        if (idx < 0 || idx >= aktuelleListe.size()) {
            return Optional.empty();
        }
        return Optional.of(aktuelleListe.get(idx));
    }

    /**
     * Mini-Dialog (per Input-Box-Pattern): zeigt einen Erfassen-Dialog mit nur
     * einem Feld und liefert den getrimmten Namen. Bei leerer Eingabe wird
     * {@link Optional#empty()} zurückgegeben.
     */
    private Optional<String> neuerNameDialog(@Nullable String vorbelegt) {
        try {
            VereinNameEingabeDialog d = new VereinNameEingabeDialog(xContext, vorbelegt);
            return d.zeigen().map(String::strip).filter(s -> !s.isEmpty());
        } catch (com.sun.star.uno.Exception e) {
            logger.error("Vereins-Name-Dialog fehlgeschlagen", e);
            return Optional.empty();
        }
    }

    private void zeigeFehler(String text) {
        MessageBox.from(xContext, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("spielerdb.fehler.titel"))
                .message(text)
                .show();
    }
}
