package de.petanqueturniermanager.spielerdb.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import de.petanqueturniermanager.spielerdb.SpielerDatensatz;
import de.petanqueturniermanager.spielerdb.SpielerDbException;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;
import de.petanqueturniermanager.spielerdb.SpielerRepository;
import de.petanqueturniermanager.spielerdb.SpielerRepository.LizenzDuplikatException;
import de.petanqueturniermanager.spielerdb.VereinDatensatz;
import de.petanqueturniermanager.spielerdb.VereinRepository;
import de.petanqueturniermanager.spielerdb.VereinRepository.DuplikatException;

/**
 * Erfasst oder bearbeitet einen Spieler. Verein wird per Autocomplete-ComboBox
 * gewählt; Freitext löst beim OK eine Rückfrage „Verein anlegen?" aus.
 */
public final class  SpielerErfassenDialog extends AbstractUnoDialog {

    private static final Logger logger = LogManager.getLogger(SpielerErfassenDialog.class);

    private static final int B = 240;
    private static final int H = 210;
    private static final int LBL_X = 8, LBL_W = 60, LBL_H = 10;
    private static final int FELD_X = 75, FELD_W = 155, FELD_H = 12;
    private static final int LABEL_LIST_H = 46;
    private static final int LABEL_HINT_H = 10;

    private final SpielerRepository spielerRepo;
    private final VereinRepository vereinRepo;
    private final LabelRepository labelRepo;
    @Nullable private final SpielerMitVerein bearbeiten;

    @Nullable private SpielerDatensatz ergebnis;

    @Nullable private UnoControlsHelper controls;
    @Nullable private XDialog xDialog;

    /** Reihenfolge entspricht Items in der Label-Listbox; Index → ID. */
    private List<LabelDatensatz> alleLabels = List.of();

    public SpielerErfassenDialog(XComponentContext xContext, SpielerRepository spielerRepo,
            VereinRepository vereinRepo, LabelRepository labelRepo,
            @Nullable SpielerMitVerein bearbeiten) {
        super(xContext);
        this.spielerRepo = spielerRepo;
        this.vereinRepo = vereinRepo;
        this.labelRepo = labelRepo;
        this.bearbeiten = bearbeiten;
    }

    /** Zeigt den Dialog modal; liefert das gespeicherte Ergebnis (oder leer bei Abbruch). */
    public Optional<SpielerDatensatz> zeigen() throws com.sun.star.uno.Exception {
        erstelleUndAusfuehren();
        return Optional.ofNullable(ergebnis);
    }

    @Override protected String getTitel() {
        return I18n.get(bearbeiten == null ? "spielerdb.erfassen.titel.neu" : "spielerdb.erfassen.titel.bearbeiten");
    }
    @Override protected int getBreite() { return B; }
    @Override protected int getHoehe() { return H; }

    @Override
    protected void erstelleFelder(XMultiComponentFactory mcf, XMultiServiceFactory xMSF,
            XNameContainer cont, XToolkit xToolkit, XWindowPeer peer,
            XPropertySet dlgProps, XDialog xDialog) throws com.sun.star.uno.Exception {

        this.xDialog = xDialog;
        XControlContainer xcc = Lo.qi(XControlContainer.class, xDialog);
        this.controls = new UnoControlsHelper(xMSF, cont, xcc);

        int y = 12;
        controls.fixedText("lblVorname", I18n.get("spielerdb.erfassen.vorname"), LBL_X, y, LBL_W, LBL_H);
        controls.edit("txtVorname", bearbeiten == null ? "" : bearbeiten.vorname(), FELD_X, y - 2, FELD_W, FELD_H);
        y += 18;
        controls.fixedText("lblNachname", I18n.get("spielerdb.erfassen.nachname"), LBL_X, y, LBL_W, LBL_H);
        controls.edit("txtNachname", bearbeiten == null ? "" : bearbeiten.nachname(), FELD_X, y - 2, FELD_W, FELD_H);
        y += 18;
        controls.fixedText("lblVerein", I18n.get("spielerdb.erfassen.verein"), LBL_X, y, LBL_W, LBL_H);
        String[] vereine = ladeVereinsnamen();
        String selektierterVerein = bearbeiten != null && bearbeiten.vereinName() != null
                ? bearbeiten.vereinName() : "";
        controls.comboBox("cbxVerein", vereine, selektierterVerein, FELD_X, y - 2, FELD_W, FELD_H);
        y += 18;
        controls.fixedText("lblLabel", I18n.get("spielerdb.erfassen.label"), LBL_X, y, LBL_W, LBL_H);
        ladeAlleLabels();
        controls.multiSelectListBox("lstLabels",
                alleLabels.stream().map(LabelDatensatz::name).toArray(String[]::new),
                FELD_X, y - 2, FELD_W, LABEL_LIST_H);
        if (bearbeiten != null && !bearbeiten.labelNrs().isEmpty()) {
            controls.setzeAusgewaehlteIndizes("lstLabels",
                    indizesFuer(bearbeiten.labelNrs()));
        }
        controls.fixedText("lblLabelHinweis",
                I18n.get("spielerdb.erfassen.label.hinweis"),
                FELD_X, y - 2 + LABEL_LIST_H + 1, FELD_W, LABEL_HINT_H);
        y += LABEL_LIST_H + LABEL_HINT_H + 4;
        controls.fixedText("lblLizenznr", I18n.get("spielerdb.erfassen.lizenznr"), LBL_X, y, LBL_W, LBL_H);
        controls.edit("txtLizenznr", bearbeiten == null ? "" : nullToEmpty(bearbeiten.lizenznr()),
                FELD_X, y - 2, FELD_W, FELD_H);

        // Buttons
        controls.button("btnOk", I18n.get("spielerdb.erfassen.btn.ok"),
                B - 110, H - 22, 50, 14, (short) PushButtonType.STANDARD_value);
        controls.button("btnAbbrechen", I18n.get("spielerdb.erfassen.btn.abbrechen"),
                B - 56, H - 22, 50, 14, (short) PushButtonType.CANCEL_value);
        controls.registriereActionListener("btnOk", this::beimOkKlick);
    }

    private String[] ladeVereinsnamen() {
        try {
            return vereinRepo.findAll().stream().map(VereinDatensatz::name).toArray(String[]::new);
        } catch (SpielerDbException e) {
            logger.error("Vereine laden fehlgeschlagen", e);
            return new String[0];
        }
    }

    private void ladeAlleLabels() {
        try {
            alleLabels = labelRepo.findAll();
        } catch (SpielerDbException e) {
            logger.error("Labels laden fehlgeschlagen", e);
            alleLabels = List.of();
        }
    }

    private short[] indizesFuer(List<Integer> labelNrs) {
        Set<Integer> gesucht = new HashSet<>(labelNrs);
        List<Short> indizes = new ArrayList<>();
        for (int i = 0; i < alleLabels.size(); i++) {
            Integer nr = alleLabels.get(i).nr();
            if (nr != null && gesucht.contains(nr)) {
                indizes.add((short) i);
            }
        }
        short[] out = new short[indizes.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = indizes.get(i);
        }
        return out;
    }

    private List<Integer> labelNrsAusAuswahl(short[] indizes) {
        List<Integer> nrs = new ArrayList<>(indizes.length);
        for (short idx : indizes) {
            if (idx >= 0 && idx < alleLabels.size()) {
                Integer nr = alleLabels.get(idx).nr();
                if (nr != null) {
                    nrs.add(nr);
                }
            }
        }
        return nrs;
    }

    private void beimOkKlick() {
        UnoControlsHelper c = this.controls;
        XDialog dlg = this.xDialog;
        if (c == null || dlg == null) {
            return;
        }
        String vorname = c.leseText("txtVorname").strip();
        String nachname = c.leseText("txtNachname").strip();
        String vereinEing = c.leseText("cbxVerein").strip();
        List<Integer> labelNrs = labelNrsAusAuswahl(c.ausgewaehlteIndizes("lstLabels"));
        String lizenz = c.leseText("txtLizenznr").strip();

        if (vorname.isEmpty() || nachname.isEmpty()) {
            zeigeFehler(I18n.get("spielerdb.fehler.pflichtfeld"));
            return;
        }

        try {
            Integer vereinNr = aufloeseOderAnlegenVerein(vereinEing).orElse(null);
            if (vereinNr == null && !vereinEing.isEmpty()) {
                // User hat Anlegen-Rückfrage abgelehnt — Eingabe bleibt, Dialog offen.
                return;
            }

            SpielerDatensatz neu = new SpielerDatensatz(
                    bearbeiten == null ? null : bearbeiten.nr(),
                    vorname, nachname, vereinNr, labelNrs,
                    lizenz.isEmpty() ? null : lizenz);

            if (bearbeiten == null) {
                ergebnis = spielerRepo.insert(neu);
            } else {
                spielerRepo.update(neu);
                ergebnis = neu;
            }
            dlg.endExecute();
        } catch (LizenzDuplikatException e) {
            zeigeFehler(I18n.get("spielerdb.fehler.lizenz_doppelt", lizenz));
        } catch (SpielerDbException e) {
            logger.error("Spieler speichern fehlgeschlagen", e);
            zeigeFehler(I18n.get("spielerdb.fehler.dbinit", e.getMessage()));
        }
    }

    private Optional<Integer> aufloeseOderAnlegenVerein(String eingabe) throws SpielerDbException {
        if (eingabe.isEmpty()) {
            return Optional.empty();
        }
        Optional<VereinDatensatz> bestehend = vereinRepo.findByName(eingabe);
        if (bestehend.isPresent()) {
            return bestehend.get().nr() == null ? Optional.empty() : Optional.of(bestehend.get().nr());
        }
        // Rückfrage anlegen?
        MessageBoxResult res = MessageBox.from(xContext, MessageBoxTypeEnum.QUESTION_YES_NO)
                .caption(I18n.get("spielerdb.frage.titel"))
                .message(I18n.get("spielerdb.frage.verein_anlegen", eingabe))
                .show();
        if (res != MessageBoxResult.YES) {
            return Optional.empty();
        }
        try {
            VereinDatensatz angelegt = vereinRepo.insert(eingabe);
            return Optional.ofNullable(angelegt.nr());
        } catch (DuplikatException e) {
            // Race: zweiter Lookup, dann sicher ohne Rückfrage akzeptieren.
            return vereinRepo.findByName(eingabe).map(VereinDatensatz::nr);
        }
    }

    private void zeigeFehler(String text) {
        MessageBox.from(xContext, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("spielerdb.fehler.titel"))
                .message(text)
                .show();
    }

    private static String nullToEmpty(@Nullable String s) {
        return s == null ? "" : s;
    }
}
