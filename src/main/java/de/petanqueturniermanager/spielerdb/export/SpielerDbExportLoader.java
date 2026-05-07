package de.petanqueturniermanager.spielerdb.export;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.spielerdb.LabelDatensatz;
import de.petanqueturniermanager.spielerdb.LabelRepository;
import de.petanqueturniermanager.spielerdb.SpielerDbException;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;
import de.petanqueturniermanager.spielerdb.SpielerRepository;
import de.petanqueturniermanager.spielerdb.VereinDatensatz;
import de.petanqueturniermanager.spielerdb.VereinRepository;

/**
 * Lädt einmalig alle für einen Export benötigten Daten aus den Repositories
 * und ergänzt automatisch die Junction-Relation {@link ExportEntity#SPIELER_LABELS},
 * sobald sowohl Spieler als auch Labels exportiert werden sollen — die
 * Junction wird dabei aus den ohnehin in {@link SpielerMitVerein} hinterlegten
 * {@code labelNrs}-Listen rekonstruiert, ein zusätzlicher DB-Zugriff entfällt.
 */
public final class SpielerDbExportLoader {

    private final SpielerRepository spielerRepo;
    private final VereinRepository vereinRepo;
    private final LabelRepository labelRepo;

    public SpielerDbExportLoader(SpielerRepository spielerRepo, VereinRepository vereinRepo,
            LabelRepository labelRepo) {
        this.spielerRepo = spielerRepo;
        this.vereinRepo = vereinRepo;
        this.labelRepo = labelRepo;
    }

    /**
     * Lädt alle Daten für den gegebenen Scope.
     *
     * @param scope      gewählte Entities (SPIELER_LABELS wird ggf. ergänzt)
     * @param appVersion Plugin-Version, die in {@link ExportMeta} landet
     */
    public SpielerDbExportData lade(EnumSet<ExportEntity> scope, @Nullable String appVersion)
            throws SpielerDbException {
        boolean ladeSpieler = scope.contains(ExportEntity.SPIELER);
        boolean ladeVereine = scope.contains(ExportEntity.VEREINE);
        boolean ladeLabels = scope.contains(ExportEntity.LABELS);
        boolean ladeJunction = ladeSpieler && ladeLabels;

        List<SpielerMitVerein> spieler = ladeSpieler ? spielerRepo.findAll() : List.of();
        List<VereinDatensatz> vereine = ladeVereine ? vereinRepo.findAll() : List.of();
        List<LabelDatensatz> labels = ladeLabels ? labelRepo.findAll() : List.of();
        List<SpielerLabelZuordnung> junction = ladeJunction
                ? extrahiereJunction(spieler)
                : List.of();

        ExportMeta meta = new ExportMeta(ExportMeta.AKTUELLE_VERSION, Instant.now(), appVersion);
        return new SpielerDbExportData(meta, spieler, vereine, labels, junction);
    }

    private static List<SpielerLabelZuordnung> extrahiereJunction(List<SpielerMitVerein> spieler) {
        List<SpielerLabelZuordnung> ergebnis = new ArrayList<>();
        for (SpielerMitVerein s : spieler) {
            for (Integer labelNr : s.labelNrs()) {
                ergebnis.add(new SpielerLabelZuordnung(s.nr(), labelNr));
            }
        }
        return ergebnis;
    }
}
