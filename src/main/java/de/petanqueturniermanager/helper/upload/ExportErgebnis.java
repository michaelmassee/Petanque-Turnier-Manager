package de.petanqueturniermanager.helper.upload;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;

public record ExportErgebnis(List<Path> exportierteDateien) {

    static final String DOC_PROP_SCHLUESSEL = "Export Dateien";

    public void speichern(WorkingSpreadsheet ws) {
        String wert = exportierteDateien.stream()
                .map(Path::toString)
                .collect(Collectors.joining(";"));
        new DocumentPropertiesHelper(ws).setStringPropertyOhneEvent(DOC_PROP_SCHLUESSEL, wert);
    }

    public static Optional<ExportErgebnis> laden(WorkingSpreadsheet ws) {
        String wert = new DocumentPropertiesHelper(ws).getStringProperty(DOC_PROP_SCHLUESSEL, "");
        if (StringUtils.isBlank(wert)) {
            return Optional.empty();
        }
        List<Path> dateien = Arrays.stream(wert.split(";"))
                .filter(StringUtils::isNotBlank)
                .map(Path::of)
                .filter(p -> p.toFile().exists())
                .collect(Collectors.toList());
        if (dateien.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ExportErgebnis(dateien));
    }
}
