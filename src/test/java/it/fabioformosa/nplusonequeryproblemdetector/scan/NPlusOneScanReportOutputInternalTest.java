package it.fabioformosa.nplusonequeryproblemdetector.scan;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

class NPlusOneScanReportOutputInternalTest {

    @BeforeEach
    void resetCollector() {
        NPlusOneScanReportCollector.reset();
    }

    @AfterEach
    void cleanCollector() {
        NPlusOneScanReportCollector.reset();
    }

    @Test
    void givenStdoutOutput_whenShutdownReportIsWritten_thenReportIsPrintedToStdout() {
        NPlusOneScanReportCollector.recordObservedTest();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        NPlusOneScanReportCollector.writeShutdownReport(properties(NPlusOneScanReportOutput.STDOUT), new PrintStream(output));

        Assertions.assertThat(output.toString(StandardCharsets.UTF_8))
                .contains("N+1 Query Problem Detector - Scan Report")
                .contains("Observed tests: 1");
    }

    @Test
    void givenDisabledOutput_whenShutdownReportIsWritten_thenNothingIsPrintedToStdout() {
        NPlusOneScanReportCollector.recordObservedTest();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        NPlusOneScanReportCollector.writeShutdownReport(properties(NPlusOneScanReportOutput.DISABLED), new PrintStream(output));

        Assertions.assertThat(output.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    void givenLoggerOutput_whenShutdownReportIsWritten_thenReportIsLogged() {
        NPlusOneScanReportCollector.recordObservedTest();
        Logger logger = Logger.getLogger(NPlusOneScanReportCollector.class.getName());
        CapturingLogHandler handler = new CapturingLogHandler();
        Level previousLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        logger.addHandler(handler);

        try {
            NPlusOneScanReportCollector.writeShutdownReport(properties(NPlusOneScanReportOutput.LOGGER), System.out);
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(previousLevel);
        }

        Assertions.assertThat(handler.messages())
                .anySatisfy(message -> Assertions.assertThat(message)
                        .contains("N+1 Query Problem Detector - Scan Report")
                        .contains("Observed tests: 1"));
    }

    private NPlusOneScanProperties properties(NPlusOneScanReportOutput reportOutput) {
        NPlusOneScanProperties defaults = NPlusOneScanProperties.defaults();
        return new NPlusOneScanProperties(
                true,
                defaults.failOnDetected(),
                defaults.failOnConfidence(),
                defaults.includeCleanTests(),
                reportOutput,
                defaults.printSqlFingerprints(),
                defaults.maxSqlFingerprints(),
                defaults.thresholds(),
                defaults.excludedTests(),
                defaults.excludedAssociations(),
                defaults.excludedEntities(),
                defaults.excludedSqlFingerprintPatterns()
        );
    }

    private static final class CapturingLogHandler extends Handler {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            messages.add(record.getMessage());
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        List<String> messages() {
            return messages;
        }
    }
}
