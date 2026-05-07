package de.petanqueturniermanager.spielerdb.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel.MeldelisteSchreibException;
import de.petanqueturniermanager.spielerdb.SpielerDatensatz;
import de.petanqueturniermanager.spielerdb.SpielerDbException;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;
import de.petanqueturniermanager.spielerdb.SpielerRepository;
import de.petanqueturniermanager.spielerdb.VereinRepository;

/**
 * Such- und Übernahme-Dialog für die Spieler-DB.
 *
 * <p>Zwei Modi:
 * <ul>
 *   <li><b>Verwaltung</b> ({@code ziel == null}): nur CRUD auf der DB, kein
 *       Übernehmen-Button.</li>
 *   <li><b>Übernahme</b> ({@code ziel != null}): zusätzlich „In Meldeliste
 *       übernehmen". Bei Formation DOUBLETTE/TRIPLETTE wird ein Sammel-Panel
 *       eingeblendet, das 2 bzw. 3 Spieler sammelt und atomar als Block in
 *       aufeinanderfolgende Zeilen schreibt.</li>
 * </ul>
 *
 * <p>Live-Filter: Eingabe ab 2 Zeichen löst Prefix-Suche aus
 * (siehe {@link SpielerRepository#findByNamePart}). Doppelten-Schutz prüft
 * vor jeder Übernahme gegen die bestehenden Meldeliste-Namen.
 */
public final class SpielerSucheDialog extends AbstractUnoDialog {

    private static final Logger logger = LogManager.getLogger(SpielerSucheDialog.class);

    private static final int B = 540;
    private static final int H = 360;
    private static final int SUCH_FELD_X = 8, SUCH_FELD_Y = 22, SUCH_FELD_W = 350, SUCH_FELD_H = 12;
    private static final int FILTER_Y = 38, FILTER_W = 250, FILTER_H = 10;
    private static final int VEREIN_LABEL_X = 8, VEREIN_LABEL_Y = 54, VEREIN_LABEL_W = 35;
    private static final int VEREIN_X = 45, VEREIN_Y = 52, VEREIN_W = 315, VEREIN_H = 12;
    private static final int LIST_X = 8, LIST_Y = 68, LIST_W = 350, LIST_H = 227;
    private static final int BTN_X = 365, BTN_W = 165, BTN_H = 14;
    private static final int TEAM_LIST_H = 120;
    private static final int FOOTER_Y = 305;
    private static final int LIMIT_TREFFER = 200;

    private final SpielerRepository spielerRepo;
    private final VereinRepository vereinRepo;
    @Nullable private final MeldelisteZiel ziel;

    @Nullable private UnoControlsHelper controls;
    @Nullable private XDialog xDialog;

    /** Rohe (ungefilterte) Trefferliste aus dem Repository – Quelle für Re-Renderings nach Sammelliste-Mutationen. */
    private List<SpielerMitVerein> rohTreffer = List.of();
    /** Aktuell sichtbare, gefilterte Liste – maßgeblich für Indizierung in {@link #ausgewaehlt()}. */
    private List<SpielerMitVerein> trefferListe = List.of();
    private final List<SpielerMitVerein> teamAuswahl = new ArrayList<>();
    /** Cache der bereits in der Meldeliste stehenden Spieler (normiert). Leer im Verwaltungs-Modus. */
    private Set<String> bereitsGemeldeteNormiert = Set.of();
    /**
     * Items der Vereins-Filter-Dropdown:
     * <ul>
     *   <li>Index 0: „(alle Vereine)" → kein Filter</li>
     *   <li>Index 1: „(ohne Verein)" → nur Spieler mit {@code vereinNr == null}</li>
     *   <li>Index ≥ 2: Vereinsnamen alphabetisch — exakter Vergleich gegen
     *       {@link SpielerMitVerein#vereinName()}</li>
     * </ul>
     */
    private List<String> vereinsnamenSortiert = List.of();

    public SpielerSucheDialog(XComponentContext xContext, SpielerRepository spielerRepo,
            VereinRepository vereinRepo, @Nullable MeldelisteZiel ziel) {
        super(xContext);
        this.spielerRepo = spielerRepo;
        this.vereinRepo = vereinRepo;
        this.ziel = ziel;
    }

    public void zeigen() throws com.sun.star.uno.Exception {
        erstelleUndAusfuehren();
    }

    @Override protected String getTitel() {
        if (ziel == null) {
            return I18n.get("spielerdb.suche.titel");
        }
        return I18n.get("spielerdb.suche.titel_uebernahme",
                ziel.getSystemBezeichnung(), ziel.getFormation().getBezeichnung());
    }
    @Override protected int getBreite() { return B; }
    @Override protected int getHoehe() { return H; }
    @Override protected boolean istVeraenderbar() { return false; }

    private boolean istUebernahmeModus() { return ziel != null; }
    private boolean istBlockModus() { return ziel != null && spielerProBlock() > 1; }
    private int spielerProBlock() { return ziel == null ? 1 : ziel.getFormation().getAnzSpieler(); }

    @Override
    protected void erstelleFelder(XMultiComponentFactory mcf, XMultiServiceFactory xMSF,
            XNameContainer cont, XToolkit xToolkit, XWindowPeer peer,
            XPropertySet dlgProps, XDialog xDialog) throws com.sun.star.uno.Exception {

        this.xDialog = xDialog;
        XControlContainer xcc = Lo.qi(XControlContainer.class, xDialog);
        this.controls = new UnoControlsHelper(xMSF, cont, xcc);

        // Suchfeld
        controls.fixedText("lblSuche", I18n.get("spielerdb.suche.feld"),
                SUCH_FELD_X, SUCH_FELD_Y - 10, SUCH_FELD_W, 10);
        controls.edit("txtSuche", "", SUCH_FELD_X, SUCH_FELD_Y, SUCH_FELD_W, SUCH_FELD_H);

        // Filter-Checkbox: nur im Übernahme-Modus sinnvoll, weil sich die
        // „bereits gemeldeten Spieler" auf die aktive Meldeliste beziehen.
        if (istUebernahmeModus()) {
            controls.checkBox("chkFilterGemeldet",
                    I18n.get("spielerdb.suche.filter.gemeldete_ausblenden"),
                    SUCH_FELD_X, FILTER_Y, FILTER_W, FILTER_H, true);
            aktualisiereGemeldeteCache();
        }

        // Vereins-Filter (Dropdown). Sichtbar in beiden Modi: hilft auch in der
        // reinen Verwaltung beim Eingrenzen großer Bestände.
        controls.fixedText("lblFilterVerein",
                I18n.get("spielerdb.suche.filter.verein.label"),
                VEREIN_LABEL_X, VEREIN_LABEL_Y, VEREIN_LABEL_W, 10);
        controls.dropdownListBox("lstFilterVerein",
                ladeVereinsFilterItems(),
                VEREIN_X, VEREIN_Y, VEREIN_W, VEREIN_H);

        // Trefferliste
        controls.listBox("lstTreffer", LIST_X, LIST_Y, LIST_W, LIST_H);

        // Buttons rechts
        int by = LIST_Y;
        if (istUebernahmeModus()) {
            String label = istBlockModus()
                    ? I18n.get("spielerdb.suche.btn.team_schreiben")
                    : I18n.get("spielerdb.suche.btn.uebernehmen");
            controls.button("btnUebernehmen", label, BTN_X, by, BTN_W, BTN_H);
            controls.enabled("btnUebernehmen", false);
            by += 18;
        }
        controls.button("btnNeu", I18n.get("spielerdb.suche.btn.neu"), BTN_X, by, BTN_W, BTN_H);
        by += 18;
        controls.button("btnBearbeiten", I18n.get("spielerdb.suche.btn.bearbeiten"), BTN_X, by, BTN_W, BTN_H);
        controls.enabled("btnBearbeiten", false);
        by += 18;
        controls.button("btnLoeschen", I18n.get("spielerdb.suche.btn.loeschen"), BTN_X, by, BTN_W, BTN_H);
        controls.enabled("btnLoeschen", false);
        by += 18;

        if (istBlockModus()) {
            controls.fixedText("lblTeam", "", BTN_X, by, BTN_W, 10);
            by += 14;
            controls.button("btnVerwerfen", I18n.get("spielerdb.suche.btn.verwerfen"), BTN_X, by, BTN_W, BTN_H);
            controls.enabled("btnVerwerfen", false);
            controls.registriereActionListener("btnVerwerfen", this::beimVerwerfen);
            by += 18;
            // Sichtbare Liste der bereits gewählten Team-Mitglieder. Doppelklick
            // entfernt einen Eintrag wieder aus der Sammlung.
            controls.listBox("lstTeamAuswahl", BTN_X, by, BTN_W, TEAM_LIST_H);
            controls.registriereListBoxAuswahl("lstTeamAuswahl",
                    () -> { /* Single-Click ohne Aktion */ },
                    this::beimTeamAuswahlDoppelklick);
            aktualisiereTeamAnzeige();
        }

        // Hinweis-Text
        if (istUebernahmeModus()) {
            String hinweis = istBlockModus()
                    ? I18n.get("spielerdb.suche.hinweis_doppelklick_team")
                    : I18n.get("spielerdb.suche.hinweis_doppelklick");
            controls.fixedText("lblHinweis", hinweis, LIST_X, FOOTER_Y, LIST_W, 10);
        }

        // Schließen
        controls.button("btnSchliessen", I18n.get("spielerdb.suche.btn.schliessen"),
                B - 70, H - 22, 60, 14, (short) PushButtonType.OK_value);

        // Listener
        controls.registriereTextListener("txtSuche", this::beimSuchTextGeaendert);
        controls.registriereListBoxAuswahl("lstTreffer", this::beimAuswahl, this::beimDoppelklick);
        controls.registriereActionListener("btnNeu", this::beimNeu);
        controls.registriereActionListener("btnBearbeiten", this::beimBearbeiten);
        controls.registriereActionListener("btnLoeschen", this::beimLoeschen);
        if (istUebernahmeModus()) {
            controls.registriereActionListener("btnUebernehmen", this::beimUebernehmen);
            // Filter-Wechsel re-triggert die aktuelle Suche, damit Aus-/Einblenden sofort wirkt.
            controls.registriereCheckBoxListener("chkFilterGemeldet", this::beimSuchTextGeaendert);
        }
        // Vereins-Filter: Auswahl-Wechsel triggert Re-Render der Trefferliste.
        controls.registriereListBoxAuswahl("lstFilterVerein",
                this::beimSuchTextGeaendert, () -> { /* Doppelklick ohne Aktion */ });

        // Initial-Trefferliste
        ladeAlle();
    }

    // ---- Suche / Anzeige ----

    private void beimSuchTextGeaendert() {
        UnoControlsHelper c = this.controls;
        if (c == null) {
            return;
        }
        String text = c.leseText("txtSuche").strip();
        if (text.isEmpty()) {
            ladeAlle();
            return;
        }
        if (text.length() < 2) {
            // Live-Filter erst ab 2 Zeichen, sonst Trefferliste leeren um „Welt"-Resultate zu vermeiden
            setzeTreffer(List.of());
            return;
        }
        sucheUndAnzeigen(text);
    }

    private void ladeAlle() {
        try {
            setzeTreffer(spielerRepo.findByNamePart("", LIMIT_TREFFER));
        } catch (SpielerDbException e) {
            logger.error("Initial-Liste laden fehlgeschlagen", e);
            zeigeFehler(I18n.get("spielerdb.fehler.dbinit", e.getMessage()));
        }
    }

    private void sucheUndAnzeigen(String praefix) {
        try {
            setzeTreffer(spielerRepo.findByNamePart(praefix, LIMIT_TREFFER));
        } catch (SpielerDbException e) {
            logger.error("Suche fehlgeschlagen", e);
        }
    }

    private void setzeTreffer(List<SpielerMitVerein> treffer) {
        UnoControlsHelper c = this.controls;
        if (c == null) {
            return;
        }
        this.rohTreffer = treffer;
        List<SpielerMitVerein> sichtbar = filtereSichtbar(c, treffer);
        this.trefferListe = sichtbar;
        c.setzeListItems("lstTreffer",
                sichtbar.stream().map(SpielerSucheDialog::formatZeile).toArray(String[]::new));
        c.enabled("btnBearbeiten", false);
        c.enabled("btnLoeschen", false);
        if (!istBlockModus()) {
            c.enabled("btnUebernehmen", false);
        }
    }

    /**
     * Liefert die in der Hauptliste sichtbaren Treffer:
     * <ul>
     *   <li>Spieler, die bereits in der Team-Sammelliste rechts stehen, werden
     *       <b>immer</b> ausgeblendet (Block-Modus; im Single-Modus ist
     *       {@code teamAuswahl} leer und der Filter wirkungslos).</li>
     *   <li>Spieler, die bereits in der Meldeliste stehen, werden zusätzlich
     *       ausgeblendet, wenn der Übernahme-Modus aktiv ist und die
     *       Filter-Checkbox angekreuzt ist.</li>
     * </ul>
     */
    private List<SpielerMitVerein> filtereSichtbar(UnoControlsHelper c, List<SpielerMitVerein> treffer) {
        Set<Integer> teamAuswahlNr = teamAuswahl.stream()
                .map(SpielerMitVerein::nr)
                .collect(Collectors.toUnmodifiableSet());
        boolean filterMeldeliste = istUebernahmeModus()
                && c.istAngekreuzt("chkFilterGemeldet")
                && !bereitsGemeldeteNormiert.isEmpty();
        VereinFilter vereinFilter = aktuellerVereinFilter(c);
        if (teamAuswahlNr.isEmpty() && !filterMeldeliste && vereinFilter.istAlle()) {
            return treffer;
        }
        return treffer.stream()
                .filter(s -> !teamAuswahlNr.contains(s.nr()))
                .filter(s -> !filterMeldeliste
                        || !bereitsGemeldeteNormiert.contains(norm(s.spielernameVollstaendig())))
                .filter(vereinFilter::passt)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Lädt alle Vereinsnamen einmalig und baut die Item-Liste der
     * Vereins-Dropdown auf: „(alle Vereine)", „(ohne Verein)", dann
     * Vereinsnamen alphabetisch.
     */
    private String[] ladeVereinsFilterItems() {
        try {
            vereinsnamenSortiert = vereinRepo.findAll().stream()
                    .map(v -> v.name())
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toUnmodifiableList());
        } catch (SpielerDbException e) {
            logger.warn("Vereine für Filter laden fehlgeschlagen", e);
            vereinsnamenSortiert = List.of();
        }
        List<String> items = new ArrayList<>(vereinsnamenSortiert.size() + 2);
        items.add(I18n.get("spielerdb.suche.filter.verein.alle"));
        items.add(I18n.get("spielerdb.suche.filter.verein.ohne"));
        items.addAll(vereinsnamenSortiert);
        return items.toArray(String[]::new);
    }

    private VereinFilter aktuellerVereinFilter(UnoControlsHelper c) {
        short idx = c.ausgewaehlterIndex("lstFilterVerein");
        if (idx <= 0) {
            return VereinFilter.alle();
        }
        if (idx == 1) {
            return VereinFilter.ohneVerein();
        }
        int vereinIdx = idx - 2;
        if (vereinIdx < 0 || vereinIdx >= vereinsnamenSortiert.size()) {
            return VereinFilter.alle();
        }
        return VereinFilter.name(vereinsnamenSortiert.get(vereinIdx));
    }

    /** Repräsentiert die Auswahl der Vereins-Filter-Dropdown. */
    private record VereinFilter(@Nullable String name, boolean nurOhne) {
        static VereinFilter alle() { return new VereinFilter(null, false); }
        static VereinFilter ohneVerein() { return new VereinFilter(null, true); }
        static VereinFilter name(String name) { return new VereinFilter(name, false); }
        boolean istAlle() { return name == null && !nurOhne; }
        boolean passt(SpielerMitVerein s) {
            if (istAlle()) {
                return true;
            }
            if (nurOhne) {
                return s.vereinNr() == null;
            }
            return name != null && name.equalsIgnoreCase(s.vereinName());
        }
    }

    private void aktualisiereGemeldeteCache() {
        if (ziel == null) {
            bereitsGemeldeteNormiert = Set.of();
            return;
        }
        bereitsGemeldeteNormiert = ziel.getVorhandeneSpielernamen().stream()
                .map(SpielerSucheDialog::norm)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String formatZeile(SpielerMitVerein s) {
        StringBuilder sb = new StringBuilder();
        sb.append(s.nachname()).append(", ").append(s.vorname());
        if (s.vereinName() != null) {
            sb.append("  —  ").append(s.vereinName());
        }
        if (s.lizenznr() != null) {
            sb.append("  [").append(s.lizenznr()).append("]");
        }
        return sb.toString();
    }

    private Optional<SpielerMitVerein> ausgewaehlt() {
        UnoControlsHelper c = this.controls;
        if (c == null) {
            return Optional.empty();
        }
        short idx = c.ausgewaehlterIndex("lstTreffer");
        if (idx < 0 || idx >= trefferListe.size()) {
            return Optional.empty();
        }
        return Optional.of(trefferListe.get(idx));
    }

    private void beimAuswahl() {
        UnoControlsHelper c = this.controls;
        if (c == null) {
            return;
        }
        boolean haveSel = ausgewaehlt().isPresent();
        c.enabled("btnBearbeiten", haveSel);
        c.enabled("btnLoeschen", haveSel);
        if (!istBlockModus()) {
            c.enabled("btnUebernehmen", haveSel && istUebernahmeModus());
        }
    }

    private void beimDoppelklick() {
        if (!istUebernahmeModus()) {
            beimBearbeiten();
            return;
        }
        if (istBlockModus()) {
            fuegeZurTeamAuswahlHinzu();
        } else {
            beimUebernehmen();
        }
    }

    // ---- CRUD-Aktionen ----

    private void beimNeu() {
        try {
            SpielerErfassenDialog d = new SpielerErfassenDialog(xContext, spielerRepo, vereinRepo, null);
            Optional<SpielerDatensatz> erg = d.zeigen();
            if (erg.isPresent()) {
                aktualisiereSucheNachAenderung();
            }
        } catch (com.sun.star.uno.Exception e) {
            logger.error("Erfassen-Dialog fehlgeschlagen", e);
        }
    }

    private void beimBearbeiten() {
        Optional<SpielerMitVerein> sel = ausgewaehlt();
        if (sel.isEmpty()) {
            return;
        }
        try {
            SpielerErfassenDialog d = new SpielerErfassenDialog(xContext, spielerRepo, vereinRepo, sel.get());
            Optional<SpielerDatensatz> erg = d.zeigen();
            if (erg.isPresent()) {
                aktualisiereSucheNachAenderung();
            }
        } catch (com.sun.star.uno.Exception e) {
            logger.error("Bearbeiten-Dialog fehlgeschlagen", e);
        }
    }

    private void beimLoeschen() {
        Optional<SpielerMitVerein> sel = ausgewaehlt();
        if (sel.isEmpty()) {
            return;
        }
        SpielerMitVerein s = sel.get();
        MessageBoxResult res = MessageBox.from(xContext, MessageBoxTypeEnum.QUESTION_YES_NO)
                .caption(I18n.get("spielerdb.frage.titel"))
                .message(I18n.get("spielerdb.frage.spieler_loeschen", s.spielernameVollstaendig()))
                .show();
        if (res != MessageBoxResult.YES) {
            return;
        }
        try {
            spielerRepo.delete(s.nr());
            aktualisiereSucheNachAenderung();
        } catch (SpielerDbException e) {
            logger.error("Spieler löschen fehlgeschlagen", e);
            zeigeFehler(e.getMessage());
        }
    }

    private void aktualisiereSucheNachAenderung() {
        UnoControlsHelper c = this.controls;
        if (c == null) {
            return;
        }
        beimSuchTextGeaendert();
    }

    // ---- Übernahme: einzeln und Block ----

    private void beimUebernehmen() {
        if (ziel == null) {
            return;
        }
        if (istBlockModus()) {
            schreibeTeam();
        } else {
            ausgewaehlt().ifPresent(this::uebernehmeEinzeln);
        }
    }

    private void uebernehmeEinzeln(SpielerMitVerein s) {
        if (ziel == null) {
            return;
        }
        if (!doppeltErlaubt(List.of(s))) {
            return;
        }
        schreibeUndMelde(List.of(s));
    }

    private void fuegeZurTeamAuswahlHinzu() {
        Optional<SpielerMitVerein> sel = ausgewaehlt();
        if (sel.isEmpty() || ziel == null) {
            return;
        }
        if (teamAuswahl.size() >= spielerProBlock()) {
            return;
        }
        // Doppel-Eintrag in Team-Auswahl ablehnen
        Set<Integer> bereits = new HashSet<>();
        teamAuswahl.forEach(p -> bereits.add(p.nr()));
        if (bereits.contains(sel.get().nr())) {
            return;
        }
        teamAuswahl.add(sel.get());
        aktualisiereTeamAnzeige();
        setzeTreffer(rohTreffer);
        if (teamAuswahl.size() == spielerProBlock()) {
            UnoControlsHelper c = this.controls;
            if (c != null) {
                c.enabled("btnUebernehmen", true);
            }
        }
    }

    private void aktualisiereTeamAnzeige() {
        UnoControlsHelper c = this.controls;
        if (c == null) {
            return;
        }
        c.label("lblTeam", I18n.get("spielerdb.suche.team_sammeln",
                teamAuswahl.size(), spielerProBlock()));
        c.enabled("btnVerwerfen", !teamAuswahl.isEmpty());
        c.setzeListItems("lstTeamAuswahl",
                teamAuswahl.stream()
                        .map(SpielerMitVerein::spielernameVollstaendig)
                        .toArray(String[]::new));
    }

    private void beimVerwerfen() {
        teamAuswahl.clear();
        aktualisiereTeamAnzeige();
        setzeTreffer(rohTreffer);
        UnoControlsHelper c = this.controls;
        if (c != null) {
            c.enabled("btnUebernehmen", false);
        }
    }

    /**
     * Doppelklick in der Team-Auswahl-Liste rechts entfernt den getroffenen
     * Spieler wieder aus der Sammlung. Ergänzt das Hinzufügen per Doppelklick
     * in der Trefferliste links und macht das Korrigieren einer Fehlauswahl
     * symmetrisch ohne Verwerfen-Button-Klick.
     */
    private void beimTeamAuswahlDoppelklick() {
        UnoControlsHelper c = this.controls;
        if (c == null) {
            return;
        }
        short idx = c.ausgewaehlterIndex("lstTeamAuswahl");
        if (idx < 0 || idx >= teamAuswahl.size()) {
            return;
        }
        teamAuswahl.remove(idx);
        aktualisiereTeamAnzeige();
        setzeTreffer(rohTreffer);
        if (teamAuswahl.size() < spielerProBlock()) {
            c.enabled("btnUebernehmen", false);
        }
    }

    private void schreibeTeam() {
        if (ziel == null || teamAuswahl.size() != spielerProBlock()) {
            return;
        }
        if (!doppeltErlaubt(teamAuswahl)) {
            return;
        }
        schreibeUndMelde(new ArrayList<>(teamAuswahl));
        teamAuswahl.clear();
        aktualisiereTeamAnzeige();
        setzeTreffer(rohTreffer);
        UnoControlsHelper c = this.controls;
        if (c != null) {
            c.enabled("btnUebernehmen", false);
        }
    }

    /**
     * Prüft Doppelten-Schutz für eine geplante Übernahme. Liefert {@code true},
     * wenn keiner der Spieler doppelt ist <i>oder</i> der Nutzer für jeden
     * Doppel-Treffer „Trotzdem hinzufügen" bestätigt hat.
     */
    private boolean doppeltErlaubt(List<SpielerMitVerein> spieler) {
        if (ziel == null) {
            return true;
        }
        for (SpielerMitVerein s : spieler) {
            int zeile = ziel.findeZeileMitName(s.spielernameVollstaendig());
            if (zeile < 0) {
                continue;
            }
            MessageBoxResult res = MessageBox.from(xContext, MessageBoxTypeEnum.QUESTION_YES_NO)
                    .caption(I18n.get("spielerdb.frage.titel"))
                    .message(I18n.get("spielerdb.frage.spieler_doppelt",
                            s.spielernameVollstaendig(), zeile))
                    .show();
            if (res != MessageBoxResult.YES) {
                return false;
            }
        }
        return true;
    }

    private void schreibeUndMelde(List<SpielerMitVerein> spieler) {
        if (ziel == null) {
            return;
        }
        try {
            int n = ziel.schreibeBlock(spieler);
            MessageBox.from(xContext, MessageBoxTypeEnum.INFO_OK)
                    .caption(I18n.get("spielerdb.menu.toplevel"))
                    .message(I18n.get("spielerdb.info.uebernommen", n))
                    .show();
            // Frisch übernommene Spieler aus der sichtbaren Trefferliste entfernen,
            // wenn der Filter aktiv ist — sonst tauchen sie weiter auf.
            aktualisiereGemeldeteCache();
            setzeTreffer(rohTreffer);
        } catch (MeldelisteSchreibException e) {
            logger.error("Block-Schreiben fehlgeschlagen", e);
            zeigeFehler(e.getMessage());
        }
    }

    private void zeigeFehler(@Nullable String text) {
        MessageBox.from(xContext, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("spielerdb.fehler.titel"))
                .message(text == null ? "" : text)
                .show();
    }

    /**
     * Case-insensitiv-getrimmter Vergleichs-Schlüssel für Spielernamen,
     * konsistent zum Doppelten-Schutz in {@code MeldelisteZiel.findeZeileMitName}.
     */
    private static String norm(String s) {
        return s.strip().toLowerCase(Locale.ROOT);
    }
}
