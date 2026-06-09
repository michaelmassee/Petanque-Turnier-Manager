package de.petanqueturniermanager.helper.upload;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;

public record ExportErgebnis(List<Path> exportierteDateien) {

    static final String DOC_PROP_SCHLUESSEL = "Export Dateien";
    private static final String FORMAT_V2_PREFIX = "v2:";

    public void speichern(WorkingSpreadsheet ws) {
        new DocumentPropertiesHelper(ws).setStringPropertyOhneEvent(DOC_PROP_SCHLUESSEL,
                serialisiere(exportierteDateien));
    }

    public static Optional<ExportErgebnis> laden(WorkingSpreadsheet ws) {
        String wert = new DocumentPropertiesHelper(ws).getStringProperty(DOC_PROP_SCHLUESSEL, "");
        if (StringUtils.isBlank(wert)) {
            return Optional.empty();
        }
        List<Path> dateien = dateienAusProperty(wert);
        if (dateien.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ExportErgebnis(dateien));
    }

    static String serialisiere(List<Path> dateien) {
        return FORMAT_V2_PREFIX + dateien.stream()
                .map(Path::toString)
                .map(ExportErgebnis::base64Kodiere)
                .collect(Collectors.joining(";"));
    }

    static List<Path> dateienAusProperty(String wert) {
        if (wert.startsWith(FORMAT_V2_PREFIX)) {
            return Arrays.stream(wert.substring(FORMAT_V2_PREFIX.length()).split(";"))
                    .filter(StringUtils::isNotBlank)
                    .map(ExportErgebnis::base64Dekodiere)
                    .map(Path::of)
                    .filter(p -> p.toFile().exists())
                    .collect(Collectors.toList());
        }
        return Arrays.stream(wert.split(";"))
                .filter(StringUtils::isNotBlank)
                .map(Path::of)
                .filter(p -> p.toFile().exists())
                .collect(Collectors.toList());
    }

    private static String base64Kodiere(String wert) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(wert.getBytes(StandardCharsets.UTF_8));
    }

    private static String base64Dekodiere(String wert) {
        return new String(Base64.getUrlDecoder().decode(wert), StandardCharsets.UTF_8);
    }
}
