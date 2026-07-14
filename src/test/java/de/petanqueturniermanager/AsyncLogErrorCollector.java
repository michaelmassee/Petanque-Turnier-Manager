package de.petanqueturniermanager;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

final class AsyncLogErrorCollector {

	private static final String APPENDER_NAME = "ptm-ui-test-error-collector";
	private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
	private static final ConcurrentLinkedQueue<LogEvent> EVENTS = new ConcurrentLinkedQueue<>();
	private static volatile long logMarker;

	private AsyncLogErrorCollector() {
	}

	static void install() {
		if (!INSTALLED.compareAndSet(false, true)) {
			return;
		}
		LoggerContext context = (LoggerContext) LogManager.getContext(false);
		Configuration config = context.getConfiguration();
		var appender = new CollectorAppender();
		appender.start();
		config.addAppender(appender);
		config.getRootLogger().addAppender(appender, Level.ERROR, null);
		context.updateLoggers();
	}

	static void clear() {
		EVENTS.clear();
		logMarker = currentLogSize();
	}

	static List<String> drainMessages() {
		List<String> messages = new ArrayList<>(EVENTS.stream()
				.map(event -> event.getLoggerName() + " - " + event.getMessage().getFormattedMessage()
						+ throwableSuffix(event))
				.toList());
		messages.addAll(logErrorsSinceMarker());
		return messages;
	}

	private static String throwableSuffix(LogEvent event) {
		Throwable thrown = event.getThrown();
		if (thrown == null) {
			return "";
		}
		return " (" + thrown.getClass().getSimpleName() + ": " + thrown.getMessage() + ")";
	}

	private static long currentLogSize() {
		Path logFile = logFile();
		try {
			return Files.exists(logFile) ? Files.size(logFile) : 0L;
		} catch (IOException e) {
			return 0L;
		}
	}

	private static List<String> logErrorsSinceMarker() {
		Path logFile = logFile();
		if (!Files.exists(logFile)) {
			return List.of();
		}

		List<String> errors = new ArrayList<>();
		try (RandomAccessFile file = new RandomAccessFile(logFile.toFile(), "r")) {
			if (logMarker > file.length()) {
				logMarker = 0L;
			}
			file.seek(logMarker);
			String line;
			while ((line = file.readLine()) != null) {
				if (line.contains(" ERROR ") || line.contains(" FATAL ")) {
					errors.add("LibreOffice log - " + line);
				}
			}
		} catch (IOException e) {
			errors.add("LibreOffice log konnte nicht geprüft werden: " + e.getMessage());
		}
		return errors;
	}

	private static Path logFile() {
		String logDir = System.getProperty("ptm.log.dir");
		if (logDir != null && !logDir.isBlank()) {
			return Path.of(logDir, "info.log");
		}
		return Path.of(System.getProperty("user.home"), ".petanqueturniermanager", "info.log");
	}

	private static final class CollectorAppender extends AbstractAppender {

		private CollectorAppender() {
			super(APPENDER_NAME, (Filter) null,
					PatternLayout.newBuilder().withPattern("%m").build(),
					true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(LogEvent event) {
			String loggerName = event.getLoggerName();
			if (event.getLevel().isMoreSpecificThan(Level.ERROR)
					&& loggerName != null
					&& loggerName.startsWith("de.petanqueturniermanager")) {
				EVENTS.add(event.toImmutable());
			}
		}
	}
}
