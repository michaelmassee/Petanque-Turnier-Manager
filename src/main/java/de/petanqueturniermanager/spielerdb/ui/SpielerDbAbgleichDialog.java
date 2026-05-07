package de.petanqueturniermanager.spielerdb.ui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.SpielerDatensatz;
import de.petanqueturniermanager.spielerdb.SpielerDbException;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;
import de.petanqueturniermanager.spielerdb.SpielerRepository;
import de.petanqueturniermanager.spielerdb.matching.SpielerMatchKeyNormalizer;
import de.petanqueturniermanager.spielerdb.VereinDatensatz;
import de.petanqueturniermanager.spielerdb.VereinRepository;

/**
 * Abgleich-Dialog: liest alle Spieler aus der aktiven Meldeliste, ermittelt
 * fehlende Datensätze gegenüber der Spieler-DB und übernimmt nach manueller
 * Auswahl die markierten Einträge in die DB.
 *
 * <p>Match erfolgt case-insensitiv über das Tripel
 * Vorname + Nachname + Vereinsname. Fehlende Vereine werden beim Import
 * automatisch in der DB angelegt. Doppel-Vorkommen in der Meldeliste werden
 * über denselben Match-Schlüssel zusammengefasst, sodass jeder fehlende Spieler
 * nur einmal importiert wird (mit der Zeile der ersten Fundstelle).
 */
public final class SpielerDbAbgleichDialog extends AbstractUnoDialog {

    private static final Logger logger = LogManager.getLogger(SpielerDbAbgleichDialog.class);

    private static final int B = 540;
    private static final int H = 320;
    private static final int KOPF_X = 8, KOPF_Y = 6, KOPF_W = 520, KOPF_H = 10;
    private static final int LIST_X = 8, LIST_Y = 22, LIST_W = 350, LIST_H = 270;
    private static final int BTN_X = 365, BTN_W = 165, BTN_H = 14;

    private final SpielerRepository spielerRepo;
    private final VereinRepository vereinRepo;
    private final MeldelisteZiel ziel;

    @Nullable private UnoControlsHelper controls;

    /** Fehlend-Liste in Anzeige-Reihenfolge (1:1 zu den Listbox-Einträgen). */
    private List<FehlendeSpielerDaten> fehlend = List.of();

    public SpielerDbAbgleichDialog(XComponentContext xContext, SpielerRepository spielerRepo,
            VereinRepository vereinRepo, MeldelisteZiel ziel) {
        super(xContext);
        this.spielerRepo = spielerRepo;
        this.vereinRepo = vereinRepo;
        this.ziel = ziel;
    }

    public void zeigen() throws com.sun.star.uno.Exception {
        erstelleUndAusfuehren();
    }

    @Override protected String getTitel() { return I18n.get("spielerdb.abgleich.titel"); }
    @Override protected int getBreite() { return B; }
    @Override protected int getHoehe() { return H; }
    @Override protected boolean istVeraenderbar() { return false; }

    @Override
    protected void erstelleFelder(XMultiComponentFactory mcf, XMultiServiceFactory xMSF,
            XNameContainer cont, XToolkit xToolkit, XWindowPeer peer,
            XPropertySet dlgProps, XDialog xDialog) throws com.sun.star.uno.Exception {

        XControlContainer xcc = Lo.qi(XControlContainer.class, xDialog);
        UnoControlsHelper c = new UnoControlsHelper(xMSF, cont, xcc);
        this.controls = c;

        c.fixedText("lblKopf", "", KOPF_X, KOPF_Y, KOPF_W, KOPF_H);
        c.multiSelectListBox("lstFehlend", new String[0], LIST_X, LIST_Y, LIST_W, LIST_H);

        int by = LIST_Y;
        c.button("btnAlle", I18n.get("spielerdb.abgleich.btn.alle_markieren"),
                BTN_X, by, BTN_W, BTN_H);
        by += 18;
        c.button("btnKeine", I18n.get("spielerdb.abgleich.btn.keine_markieren"),
                BTN_X, by, BTN_W, BTN_H);
        by += 22;
        c.button("btnImport", I18n.get("spielerdb.abgleich.btn.importieren"),
                BTN_X, by, BTN_W, BTN_H);

        c.button("btnSchliessen", I18n.get("spielerdb.suche.btn.schliessen"),
                BTN_X, H - 22, BTN_W, BTN_H, (short) PushButtonType.OK_value);

        c.registriereActionListener("btnAlle", this::beimAlleMarkieren);
        c.registriereActionListener("btnKeine", this::beimKeineMarkieren);
        c.registriereActionListener("btnImport", this::beimImport);

        ladeFehlende();
    }

    // ---- Match-Logik ----

    private void ladeFehlende() {
        UnoControlsHelper c = this.controls;
        if (c == null) {
            return;
        }
        List<MeldelisteSpielerDaten> ausMeldeliste = ziel.leseAlleSpielerRoh();
        Set<String> dbIndex;
        try {
            dbIndex = baueDbIndex();
        } catch (SpielerDbException e) {
            logger.error("Spieler-DB lesen fehlgeschlagen", e);
            zeigeFehler(I18n.get("spielerdb.fehler.dbinit",
                    e.getMessage() == null ? "" : e.getMessage()));
            return;
        }

        Map<String, FehlendeSpielerDaten> fehlendDedup = new LinkedHashMap<>();
        int bereitsInDb = 0;
        for (MeldelisteSpielerDaten m : ausMeldeliste) {
            String schluessel = matchSchluessel(m.vorname(), m.nachname(), m.vereinName());
            if (dbIndex.contains(schluessel)) {
                bereitsInDb++;
                continue;
            }
            fehlendDedup.putIfAbsent(schluessel, new FehlendeSpielerDaten(
                    m.vorname(), m.nachname(), m.vereinName(), m.zeile1Basiert()));
        }
        this.fehlend = List.copyOf(fehlendDedup.values());

        c.label("lblKopf", I18n.get("spielerdb.abgleich.kopf",
                ausMeldeliste.size(), bereitsInDb, fehlend.size()));
        c.setzeListItems("lstFehlend",
                fehlend.stream().map(SpielerDbAbgleichDialog::formatZeile).toArray(String[]::new));
        boolean nichtLeer = !fehlend.isEmpty();
        c.enabled("btnImport", nichtLeer);
        c.enabled("btnAlle", nichtLeer);
        c.enabled("btnKeine", nichtLeer);
    }

    private Set<String> baueDbIndex() throws SpielerDbException {
        List<SpielerMitVerein> alle = spielerRepo.findAll();
        Set<String> index = new HashSet<>(alle.size() * 2);
        for (SpielerMitVerein s : alle) {
            index.add(matchSchluessel(s.vorname(), s.nachname(), s.vereinName()));
        }
        return index;
    }

    private static String matchSchluessel(String vorname, String nachname, @Nullable String verein) {
        return SpielerMatchKeyNormalizer.spielerSchluesselMitVereinName(vorname, nachname, verein);
    }

    private static String formatZeile(FehlendeSpielerDaten d) {
        if (d.vereinName() == null) {
            return I18n.get("spielerdb.abgleich.format_zeile_ohne_verein",
                    d.nachname(), d.vorname(), d.zeile1Basiert());
        }
        return I18n.get("spielerdb.abgleich.format_zeile",
                d.nachname(), d.vorname(), d.vereinName(), d.zeile1Basiert());
    }

    // ---- Aktionen ----

    private void beimAlleMarkieren() {
        UnoControlsHelper c = this.controls;
        if (c == null || fehlend.isEmpty()) {
            return;
        }
        short[] indizes = new short[fehlend.size()];
        for (int i = 0; i < indizes.length; i++) {
            indizes[i] = (short) i;
        }
        c.setzeAusgewaehlteIndizes("lstFehlend", indizes);
    }

    private void beimKeineMarkieren() {
        UnoControlsHelper c = this.controls;
        if (c == null) {
            return;
        }
        c.setzeAusgewaehlteIndizes("lstFehlend", new short[0]);
    }

    private void beimImport() {
        UnoControlsHelper c = this.controls;
        if (c == null) {
            return;
        }
        short[] indizes = c.ausgewaehlteIndizes("lstFehlend");
        if (indizes.length == 0) {
            return;
        }

        ImportErgebnis erg = importiere(indizes);

        MessageBox.from(xContext, MessageBoxTypeEnum.INFO_OK)
                .caption(I18n.get("spielerdb.abgleich.titel"))
                .message(I18n.get("spielerdb.abgleich.info.fertig",
                        erg.importiert, erg.neueVereine, erg.fehler))
                .show();

        ladeFehlende();
    }

    private ImportErgebnis importiere(short[] indizes) {
        // Cache: Vereinsname (normiert) -> Verein-Nr; vermeidet wiederholte
        // findByName-Lookups innerhalb desselben Import-Vorgangs.
        Map<String, Integer> vereinNrCache = new HashMap<>();
        ImportErgebnis erg = new ImportErgebnis();
        for (short idx : indizes) {
            if (idx < 0 || idx >= fehlend.size()) {
                continue;
            }
            FehlendeSpielerDaten d = fehlend.get(idx);
            try {
                Integer vereinNr = vereinNrFuer(d.vereinName(), vereinNrCache, erg);
                spielerRepo.insert(SpielerDatensatz.neu(
                        d.vorname(), d.nachname(), vereinNr, List.of(), null));
                erg.importiert++;
            } catch (SpielerDbException e) {
                logger.error("Import von '{} {}' fehlgeschlagen", d.vorname(), d.nachname(), e);
                erg.fehler++;
            }
        }
        return erg;
    }

    @Nullable
    private Integer vereinNrFuer(@Nullable String vereinName,
            Map<String, Integer> cache, ImportErgebnis erg) throws SpielerDbException {
        if (vereinName == null || vereinName.isEmpty()) {
            return null;
        }
        String key = SpielerMatchKeyNormalizer.vereinSchluessel(vereinName);
        Integer cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        var vorhanden = vereinRepo.findByName(vereinName);
        VereinDatensatz v;
        if (vorhanden.isPresent()) {
            v = vorhanden.get();
        } else {
            try {
                v = vereinRepo.insert(vereinName);
                erg.neueVereine++;
            } catch (VereinRepository.DuplikatException dup) {
                // Race-Sicherung: Wenn ein paralleler Insert oder ein
                // case-Variant-Treffer dazwischen kam, beim zweiten Versuch
                // erneut lesen statt Importschritt abbrechen.
                v = vereinRepo.findByName(vereinName).orElseThrow(() -> dup);
            }
        }
        Integer nr = v.nr();
        if (nr != null) {
            cache.put(key, nr);
        }
        return nr;
    }

    private void zeigeFehler(String text) {
        MessageBox.from(xContext, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("spielerdb.fehler.titel"))
                .message(text)
                .show();
    }

    private static final class ImportErgebnis {
        int importiert;
        int neueVereine;
        int fehler;
    }

    private record FehlendeSpielerDaten(String vorname, String nachname,
            @Nullable String vereinName, int zeile1Basiert) {}
}
