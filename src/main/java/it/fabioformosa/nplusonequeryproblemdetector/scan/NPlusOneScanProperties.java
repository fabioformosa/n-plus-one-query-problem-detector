package it.fabioformosa.nplusonequeryproblemdetector.scan;

import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;

public record NPlusOneScanProperties(
        boolean enabled,
        boolean failOnDetected,
        NPlusOneConfidence failOnConfidence,
        boolean includeCleanTests,
        boolean printSqlFingerprints,
        int maxSqlFingerprints,
        NPlusOneThresholds thresholds,
        List<String> excludedTests,
        List<String> excludedAssociations,
        List<String> excludedEntities,
        List<String> excludedSqlFingerprintPatterns
) {

    public static NPlusOneScanProperties defaults() {
        return new NPlusOneScanProperties(
                false,
                false,
                NPlusOneConfidence.HIGH,
                false,
                true,
                5,
                NPlusOneThresholds.defaults(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    public static NPlusOneScanProperties from(Environment environment) {
        NPlusOneThresholds defaultThresholds = NPlusOneThresholds.defaults();
        NPlusOneThresholds thresholds = new NPlusOneThresholds(
                getLong(environment, "nplusone.scan.threshold.min-entity-fetches", defaultThresholds.minEntityFetches()),
                getLong(environment, "nplusone.scan.threshold.min-collection-fetches", defaultThresholds.minCollectionFetches()),
                getLong(environment, "nplusone.scan.threshold.min-repeated-select-fingerprint", defaultThresholds.minRepeatedSelectFingerprint()),
                getLong(environment, "nplusone.scan.threshold.min-prepared-statements", defaultThresholds.minPreparedStatements())
        );

        return new NPlusOneScanProperties(
                environment.getProperty("nplusone.scan.enabled", Boolean.class, false),
                environment.getProperty("nplusone.scan.fail-on-detected", Boolean.class, false),
                getConfidence(environment, "nplusone.scan.fail-on-confidence", NPlusOneConfidence.HIGH),
                environment.getProperty("nplusone.scan.report.include-clean-tests", Boolean.class, false),
                environment.getProperty("nplusone.scan.report.print-sql-fingerprints", Boolean.class, true),
                environment.getProperty("nplusone.scan.report.max-sql-fingerprints", Integer.class, 5),
                thresholds,
                getList(environment, "nplusone.scan.excluded-tests"),
                getList(environment, "nplusone.scan.excluded-associations"),
                getList(environment, "nplusone.scan.excluded-entities"),
                getList(environment, "nplusone.scan.excluded-sql-fingerprint-patterns")
        );
    }

    private static long getLong(Environment environment, String propertyName, long defaultValue) {
        return environment.getProperty(propertyName, Long.class, defaultValue);
    }

    private static NPlusOneConfidence getConfidence(Environment environment, String propertyName, NPlusOneConfidence defaultValue) {
        String value = environment.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return NPlusOneConfidence.valueOf(value.trim().toUpperCase());
    }

    private static List<String> getList(Environment environment, String propertyName) {
        String value = environment.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isBlank())
                .toList();
    }
}
