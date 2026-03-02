package de.petanqueturniermanager.comp;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Prüft, dass alle ptm:-Commands in den XCU-Dateien als Konstante in
 * {@link ProtocolHandler} definiert sind – und umgekehrt.
 */
public class ProtocolHandlerCommandsTest {

	private static final Pattern PTM_PATTERN = Pattern.compile("ptm:([\\w]+)");

	/** Alle CMD_*-Konstanten aus ProtocolHandler per Reflection einlesen. */
	private Set<String> constantValues() throws IllegalAccessException {
		Set<String> values = new HashSet<>();
		for (Field field : ProtocolHandler.class.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers())
					&& Modifier.isFinal(field.getModifiers())
					&& field.getType() == String.class
					&& field.getName().startsWith("CMD_")) {
				values.add((String) field.get(null));
			}
		}
		return values;
	}

	/** Alle ptm:-Commands aus sämtlichen XCU-Dateien im registry-Verzeichnis. */
	private Set<String> xcuCommands() throws Exception {
		Set<String> commands = new HashSet<>();
		Path registryDir = Paths.get("registry");
		try (Stream<Path> files = Files.walk(registryDir)) {
			files.filter(p -> p.toString().endsWith(".xcu")).forEach(xcu -> {
				try {
					String content = Files.readString(xcu);
					Matcher m = PTM_PATTERN.matcher(content);
					while (m.find()) {
						String cmd = m.group(1);
						if (!"*".equals(cmd)) {
							commands.add(cmd);
						}
					}
				} catch (Exception e) {
					throw new RuntimeException("Fehler beim Lesen von " + xcu, e);
				}
			});
		}
		return commands;
	}

	@Test
	void alleXcuCommandsHabenEineKonstante() throws Exception {
		Set<String> constants = constantValues();
		Set<String> xcu = xcuCommands();

		Set<String> nichtInKonstanten = new HashSet<>(xcu);
		nichtInKonstanten.removeAll(constants);

		assertThat(nichtInKonstanten)
				.as("XCU-Commands ohne CMD_-Konstante in ProtocolHandler")
				.isEmpty();
	}

	@Test
	void alleKonstantenHabenEinenXcuEintrag() throws Exception {
		Set<String> constants = constantValues();
		Set<String> xcu = xcuCommands();

		Set<String> nichtInXcu = new HashSet<>(constants);
		nichtInXcu.removeAll(xcu);

		assertThat(nichtInXcu)
				.as("CMD_-Konstanten ohne ptm:-Eintrag in XCU-Dateien")
				.isEmpty();
	}
}
