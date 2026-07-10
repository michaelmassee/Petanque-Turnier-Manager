/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ki;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.Log4J;
import de.petanqueturniermanager.comp.newrelease.ExtensionsHelper;

final class FehlerberichtBuilder {

    private static final int MAX_LOG_ZEICHEN = 45_000;
    private static final int MAX_BODY_ZEICHEN = 60_000;
    private static final Set<String> SENSITIVE_KEY_TEILE = Set.of(
            "key", "token", "secret", "password", "passwd", "credential");

    private FehlerberichtBuilder() {
    }

    static Fehlerbericht erstellen(XComponentContext context) {
        String zeit = Instant.now().toString();
        String version = ermittleVersion(context);
        String titel = "Automatischer PTM Fehlerbericht " + zeit;
        StringBuilder body = new StringBuilder();
        body.append("## Automatischer Fehlerbericht\n\n")
                .append("- Zeitpunkt: ").append(zeit).append('\n')
                .append("- PTM-Version: ").append(version).append('\n')
                .append("- Logfile: ").append(logPfad()).append("\n\n")
                .append("## Systeminformationen\n\n")
                .append("```text\n")
                .append(systeminformationen(context, version))
                .append("```\n\n")
                .append("## Logfile-Auszug\n\n")
                .append("```text\n")
                .append(logAuszug())
                .append("\n```\n");
        String bodyText = body.toString();
        if (bodyText.length() > MAX_BODY_ZEICHEN) {
            bodyText = bodyText.substring(0, MAX_BODY_ZEICHEN)
                    + "\n\n[Bericht wegen GitHub-Groessenlimit gekuerzt]\n";
        }
        return new Fehlerbericht(titel, bodyText);
    }

    private static String ermittleVersion(XComponentContext context) {
        try {
            return ExtensionsHelper.from(context).versionNummerOptional().orElse("unbekannt");
        } catch (RuntimeException e) {
            return "unbekannt";
        }
    }

    private static String systeminformationen(XComponentContext context, String version) {
        StringBuilder out = new StringBuilder();
        out.append("ptm.version=").append(version).append('\n');
        out.append("available.processors=").append(Runtime.getRuntime().availableProcessors()).append('\n');
        out.append("max.memory=").append(Runtime.getRuntime().maxMemory()).append('\n');
        out.append("total.memory=").append(Runtime.getRuntime().totalMemory()).append('\n');
        out.append("free.memory=").append(Runtime.getRuntime().freeMemory()).append('\n');
        out.append("uno.context=").append(context == null ? "null" : context.getClass().getName()).append('\n');
        Properties props = System.getProperties();
        TreeSet<String> namen = new TreeSet<>(props.stringPropertyNames());
        for (String name : namen) {
            out.append("sys.").append(name).append('=').append(systemPropertyWert(name, props.getProperty(name))).append('\n');
        }
        return out.toString();
    }

    private static String systemPropertyWert(String name, String wert) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String teil : SENSITIVE_KEY_TEILE) {
            if (lower.contains(teil)) {
                return "[gefiltert]";
            }
        }
        return wert == null ? "" : wert;
    }

    private static String logPfad() {
        File logFile = Log4J.getLogFile();
        return logFile == null ? "nicht ermittelt" : logFile.getAbsolutePath();
    }

    private static String logAuszug() {
        File logFile = Log4J.getLogFile();
        if (logFile == null) {
            return "Logfile konnte nicht ermittelt werden.";
        }
        try {
            String log = Files.readString(logFile.toPath(), StandardCharsets.UTF_8);
            if (log.length() <= MAX_LOG_ZEICHEN) {
                return log;
            }
            return "[Logfile gekuerzt auf die letzten " + MAX_LOG_ZEICHEN + " Zeichen]\n"
                    + log.substring(log.length() - MAX_LOG_ZEICHEN);
        } catch (IOException e) {
            return "Logfile konnte nicht gelesen werden: " + e.getMessage();
        }
    }
}
