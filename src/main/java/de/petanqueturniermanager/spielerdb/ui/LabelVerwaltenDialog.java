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
import de.petanqueturniermanager.spielerdb.LabelDatensatz;
import de.petanqueturniermanager.spielerdb.LabelRepository;
import de.petanqueturniermanager.spielerdb.LabelRepository.DuplikatException;
import de.petanqueturniermanager.spielerdb.LabelRepository.InBenutzungException;
import de.petanqueturniermanager.spielerdb.SpielerDbException;

/**
 * Verwaltung der Label-Stammdaten — spiegelbildlich zu
 * {@link VereinVerwaltenDialog}: Liste mit Buttons für Anlegen, Bearbeiten,
 * Löschen. Löschen prüft per {@code countSpieler} und bricht bei zugeordneten
 * Spielern ab (RESTRICT).
 */
public final class LabelVerwaltenDialog extends AbstractUnoDialog {

    private static final Logger logger = LogManager.getLogger(LabelVerwaltenDialog.class);

    private static final int B = 320;
    private static final int H = 280;
    private static final int LIST_X = 8, LIST_Y = 12, LIST_W = 200, LIST_H = 240;
    private static final int BTN_X = 215, BTN_W = 95, BTN_H = 14;

    private final LabelRepository labelRepo;

    @Nullable private UnoControlsHelper controls;
    private List<LabelDatensatz> aktuelleListe = List.of();

    public LabelVerwaltenDialog(XComponentContext xContext, LabelRepository labelRepo) {
        super(xContext);
        this.labelRepo = labelRepo;
    }

    public void zeigen() throws com.sun.star.uno.Exception {
        erstelleUndAusfuehren();
    }

    @Override protected String getTitel() { return I18n.get("spielerdb.label.titel"); }
    @Override protected int getBreite() { return B; }
    @Override protected int getHoehe() { return H; }

    @Override
    protected void erstelleFelder(XMultiComponentFactory mcf, XMultiServiceFactory xMSF,
            XNameContainer cont, XToolkit xToolkit, XWindowPeer peer,
            XPropertySet dlgProps, XDialog xDialog) throws com.sun.star.uno.Exception {

        XControlContainer xcc = Lo.qi(XControlContainer.class, xDialog);
        this.controls = new UnoControlsHelper(xMSF, cont, xcc);

        controls.listBox("lstLabels", LIST_X, LIST_Y, LIST_W, LIST_H);

        int by = LIST_Y;
        controls.button("btnNeu", I18n.get("spielerdb.label.btn.neu"), BTN_X, by, BTN_W, BTN_H);
        by += 18;
        controls.button("btnBearbeiten", I18n.get("spielerdb.label.btn.bearbeiten"), BTN_X, by, BTN_W, BTN_H);
        by += 18;
        controls.button("btnLoeschen", I18n.get("spielerdb.label.btn.loeschen"), BTN_X, by, BTN_W, BTN_H);
        controls.button("btnSchliessen", I18n.get("spielerdb.suche.btn.schliessen"),
                BTN_X, H - 22, BTN_W, BTN_H, (short) PushButtonType.OK_value);

        controls.registriereActionListener("btnNeu", this::beimNeu);
        controls.registriereActionListener("btnBearbeiten", this::beimBearbeiten);
        controls.registriereActionListener("btnLoeschen", this::beimLoeschen);
        controls.registriereListBoxAuswahl("lstLabels", () -> { /* selection only */ }, this::beimBearbeiten);

        ladeListe();
    }

    private void ladeListe() {
        UnoControlsHelper c = this.controls;
        if (c == null) {
            return;
        }
        try {
            aktuelleListe = labelRepo.findAll();
            c.setzeListItems("lstLabels",
                    aktuelleListe.stream().map(LabelDatensatz::name).toArray(String[]::new));
        } catch (SpielerDbException e) {
            logger.error("Labels laden fehlgeschlagen", e);
            zeigeFehler(I18n.get("spielerdb.fehler.dbinit", e.getMessage()));
        }
    }

    private void beimNeu() {
        Optional<String> eingabe = neuerNameDialog(null);
        if (eingabe.isEmpty()) {
            return;
        }
        try {
            labelRepo.insert(eingabe.get());
            ladeListe();
        } catch (DuplikatException e) {
            zeigeFehler(I18n.get("spielerdb.fehler.label_name_doppelt", eingabe.get()));
        } catch (SpielerDbException e) {
            logger.error("Label anlegen fehlgeschlagen", e);
            zeigeFehler(e.getMessage());
        }
    }

    private void beimBearbeiten() {
        Optional<LabelDatensatz> sel = ausgewaehlt();
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
            labelRepo.update(nr, eingabe.get());
            ladeListe();
        } catch (DuplikatException e) {
            zeigeFehler(I18n.get("spielerdb.fehler.label_name_doppelt", eingabe.get()));
        } catch (SpielerDbException e) {
            logger.error("Label bearbeiten fehlgeschlagen", e);
            zeigeFehler(e.getMessage());
        }
    }

    private void beimLoeschen() {
        Optional<LabelDatensatz> sel = ausgewaehlt();
        if (sel.isEmpty()) {
            return;
        }
        Integer nr = sel.get().nr();
        if (nr == null) {
            return;
        }
        try {
            int anz = labelRepo.countSpieler(nr);
            if (anz > 0) {
                zeigeFehler(I18n.get("spielerdb.fehler.label_in_benutzung", sel.get().name(), anz));
                return;
            }
            MessageBoxResult res = MessageBox.from(xContext, MessageBoxTypeEnum.QUESTION_YES_NO)
                    .caption(I18n.get("spielerdb.frage.titel"))
                    .message(I18n.get("spielerdb.frage.label_loeschen", sel.get().name()))
                    .show();
            if (res != MessageBoxResult.YES) {
                return;
            }
            labelRepo.delete(nr);
            ladeListe();
        } catch (InBenutzungException e) {
            zeigeFehler(I18n.get("spielerdb.fehler.label_in_benutzung", sel.get().name(), "?"));
        } catch (SpielerDbException e) {
            logger.error("Label löschen fehlgeschlagen", e);
            zeigeFehler(e.getMessage());
        }
    }

    private Optional<LabelDatensatz> ausgewaehlt() {
        UnoControlsHelper c = this.controls;
        if (c == null) {
            return Optional.empty();
        }
        short idx = c.ausgewaehlterIndex("lstLabels");
        if (idx < 0 || idx >= aktuelleListe.size()) {
            return Optional.empty();
        }
        return Optional.of(aktuelleListe.get(idx));
    }

    private Optional<String> neuerNameDialog(@Nullable String vorbelegt) {
        try {
            LabelNameEingabeDialog d = new LabelNameEingabeDialog(xContext, vorbelegt);
            return d.zeigen().map(String::strip).filter(s -> !s.isEmpty());
        } catch (com.sun.star.uno.Exception e) {
            logger.error("Label-Name-Dialog fehlgeschlagen", e);
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
