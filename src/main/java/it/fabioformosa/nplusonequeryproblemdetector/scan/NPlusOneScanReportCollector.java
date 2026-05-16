package it.fabioformosa.nplusonequeryproblemdetector.scan;

import it.fabioformosa.nplusonequeryproblemdetector.engine.HibernateStatsSnapshot;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public final class NPlusOneScanReportCollector {

    private static final Logger LOGGER = Logger.getLogger(NPlusOneScanReportCollector.class.getName());
    private static final String REPORT_SEPARATOR = "================================================================================\n";
    private static final PrintStream STANDARD_OUTPUT = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
    private static final List<NPlusOneFinding> FINDINGS = new CopyOnWriteArrayList<>();
    private static final AtomicBoolean SHUTDOWN_HOOK_REGISTERED = new AtomicBoolean(false);
    private static final AtomicReference<NPlusOneScanProperties> LAST_PROPERTIES = new AtomicReference<>(NPlusOneScanProperties.defaults());
    private static final AtomicLong OBSERVED_TESTS = new AtomicLong();

    private NPlusOneScanReportCollector() {
        throw new IllegalStateException("Utility class");
    }

    public static void registerShutdownHookOnce(NPlusOneScanProperties properties) {
        LAST_PROPERTIES.set(properties);
        if (properties.reportOutput() == NPlusOneScanReportOutput.DISABLED) {
            return;
        }
        if (SHUTDOWN_HOOK_REGISTERED.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> writeShutdownReport(LAST_PROPERTIES.get(), STANDARD_OUTPUT), "nplusone-scan-report"));
        }
    }

    public static void recordObservedTest() {
        OBSERVED_TESTS.incrementAndGet();
    }

    public static void addFindings(List<NPlusOneFinding> findings) {
        FINDINGS.addAll(findings);
    }

    public static List<NPlusOneFinding> findings() {
        return List.copyOf(FINDINGS);
    }

    public static void reset() {
        FINDINGS.clear();
        OBSERVED_TESTS.set(0L);
        LAST_PROPERTIES.set(NPlusOneScanProperties.defaults());
    }

    public static void printReport(PrintStream printStream) {
        printStream.print(renderReport(LAST_PROPERTIES.get()));
    }

    static void writeShutdownReport(NPlusOneScanProperties properties, PrintStream stdout) {
        switch (properties.reportOutput()) {
            case LOGGER -> LOGGER.info(() -> renderReport(properties));
            case STDOUT -> stdout.print(renderReport(properties));
            case DISABLED -> {
                // Report output is intentionally suppressed by configuration.
            }
        }
    }

    public static String renderReport(NPlusOneScanProperties properties) {
        List<NPlusOneFinding> findings = new ArrayList<>(FINDINGS);
        findings.sort(Comparator.comparing(NPlusOneFinding::getConfidence).thenComparing(f -> f.getTestIdentifier().displayName()));
        List<AggregatedFinding> aggregatedFindings = aggregateByTest(findings);

        long affectedTests = aggregatedFindings.stream().filter(AggregatedFinding::hasIncludedFindings).count();
        long excludedFindings = findings.stream().filter(NPlusOneFinding::isExcluded).count();

        StringBuilder report = new StringBuilder();
        report.append(REPORT_SEPARATOR);
        report.append("N+1 Query Problem Detector - Scan Report\n");
        report.append(REPORT_SEPARATOR).append("\n");
        report.append("Scan mode: ENABLED\n");
        report.append("Fail on detected: ").append(properties.failOnDetected()).append("\n");
        report.append("Observed tests: ").append(OBSERVED_TESTS.get()).append("\n");
        report.append("Affected tests: ").append(affectedTests).append("\n");
        report.append("Excluded findings: ").append(excludedFindings).append("\n\n");

        if (findings.isEmpty()) {
            report.append("No N+1 query candidates were detected.\n");
        }

        for (AggregatedFinding finding : aggregatedFindings) {
            appendFinding(report, finding, properties);
        }

        appendSummary(report, aggregatedFindings, excludedFindings, properties);
        return report.toString();
    }

    private static List<AggregatedFinding> aggregateByTest(List<NPlusOneFinding> findings) {
        Map<NPlusOneTestIdentifier, List<NPlusOneFinding>> findingsByTest = new LinkedHashMap<>();
        for (NPlusOneFinding finding : findings) {
            findingsByTest.computeIfAbsent(finding.getTestIdentifier(), ignored -> new ArrayList<>()).add(finding);
        }
        return findingsByTest.entrySet().stream()
                .map(entry -> new AggregatedFinding(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static void appendFinding(StringBuilder report, AggregatedFinding finding, NPlusOneScanProperties properties) {
        report.append("--------------------------------------------------------------------------------\n");
        report.append("[").append(finding.isFullyExcluded() ? "EXCLUDED " : "").append(finding.confidence()).append("] ")
                .append(finding.testIdentifier().displayName()).append("\n");
        report.append("--------------------------------------------------------------------------------\n");
        appendExclusionDetails(report, finding);
        appendLines(report, finding.reasons().size() == 1 ? "Reason" : "Reasons", finding.reasons());
        appendHibernateStatistics(report, finding);
        appendLikelyAffectedFetches(report, finding);
        appendSqlFingerprints(report, finding, properties);
        appendSuggestedFixes(report);
    }

    private static void appendExclusionDetails(StringBuilder report, AggregatedFinding finding) {
        if (finding.isFullyExcluded()) {
            appendLines(report, "Exclusion", finding.exclusionReasons());
        } else if (finding.excludedCount() > 0) {
            report.append("Excluded findings in this test: ").append(finding.excludedCount()).append("\n\n");
        }
    }

    private static void appendHibernateStatistics(StringBuilder report, AggregatedFinding finding) {
        report.append("Hibernate statistics:\n");
        report.append("  Query executions:        ").append(finding.stats().getQueryExecutionCount()).append("\n");
        report.append("  Prepared statements:     ").append(finding.stats().getPrepareStatementCount()).append("\n");
        report.append("  Entity fetches:          ").append(finding.stats().getEntityFetchCount()).append("\n");
        report.append("  Collection fetches:      ").append(finding.stats().getCollectionFetchCount()).append("\n");
        report.append("  Second-level cache hits: ").append(finding.stats().getSecondLevelCacheHitCount()).append("\n\n");
    }

    private static void appendLikelyAffectedFetches(StringBuilder report, AggregatedFinding finding) {
        if (!finding.associationRoles().isEmpty()) {
            appendLines(report, finding.associationRoles().size() == 1 ? "Likely affected association" : "Likely affected associations", finding.associationRoles());
        }
        if (!finding.entityNames().isEmpty()) {
            appendLines(report, finding.entityNames().size() == 1 ? "Likely fetched entity" : "Likely fetched entities", finding.entityNames());
        }
        if (finding.associationRoles().isEmpty() && finding.entityNames().isEmpty()) {
            report.append("Likely affected association:\n  Unknown\n\n");
        }
    }

    private static void appendSqlFingerprints(StringBuilder report, AggregatedFinding finding, NPlusOneScanProperties properties) {
        if (!properties.printSqlFingerprints() || finding.repeatedSqlFingerprints().isEmpty()) {
            return;
        }

        report.append("Repeated SQL fingerprints:\n");
        finding.repeatedSqlFingerprints().stream()
                .limit(properties.maxSqlFingerprints())
                .forEach(fingerprint -> report.append("  ").append(fingerprint.count()).append("x ").append(fingerprint.sql()).append("\n"));
        if (finding.repeatedSqlFingerprints().size() > properties.maxSqlFingerprints()) {
            report.append("  ... ").append(finding.repeatedSqlFingerprints().size() - properties.maxSqlFingerprints()).append(" more omitted\n");
        }
        report.append("\n");
    }

    private static void appendSuggestedFixes(StringBuilder report) {
        report.append("Suggested fixes:\n");
        report.append("  1. Use JOIN FETCH [RECOMMENDED] when this use case always needs the association.\n");
        report.append("  2. Use EntityGraph when the fetch shape should be declarative or reusable.\n");
        report.append("  3. Use batch fetching when lazy loading is acceptable but should be grouped.\n");
        report.append("  4. Use DTO/projection queries when only selected fields are needed.\n\n");
    }

    private static void appendLines(StringBuilder report, String label, List<String> lines) {
        report.append(label).append(":\n");
        lines.forEach(line -> report.append("  ").append(line).append("\n"));
        report.append("\n");
    }

    private static void appendSummary(StringBuilder report, List<AggregatedFinding> findings, long excludedFindings, NPlusOneScanProperties properties) {
        report.append(REPORT_SEPARATOR);
        report.append("Summary by confidence\n");
        report.append(REPORT_SEPARATOR);
        report.append("HIGH:   ").append(countIncluded(findings, NPlusOneConfidence.HIGH)).append("\n");
        report.append("MEDIUM: ").append(countIncluded(findings, NPlusOneConfidence.MEDIUM)).append("\n");
        report.append("LOW:    ").append(countIncluded(findings, NPlusOneConfidence.LOW)).append("\n");
        report.append("EXCLUDED: ").append(excludedFindings).append("\n\n");
        report.append("Build result:\n");
        report.append("  Tests ").append(properties.failOnDetected() ? "may fail on non-excluded findings at or above " + properties.failOnConfidence() : "were not failed because n-plus-one-query-detector.scan.fail-on-detected=false").append(".\n");
        report.append(REPORT_SEPARATOR);
    }

    private static long countIncluded(List<AggregatedFinding> findings, NPlusOneConfidence confidence) {
        return findings.stream()
                .filter(AggregatedFinding::hasIncludedFindings)
                .filter(finding -> finding.confidence() == confidence)
                .count();
    }

    private record AggregatedFinding(NPlusOneTestIdentifier testIdentifier, List<NPlusOneFinding> findings) {

        private boolean hasIncludedFindings() {
            return findings.stream().anyMatch(finding -> !finding.isExcluded());
        }

        private boolean isFullyExcluded() {
            return !findings.isEmpty() && findings.stream().allMatch(NPlusOneFinding::isExcluded);
        }

        private long excludedCount() {
            return findings.stream().filter(NPlusOneFinding::isExcluded).count();
        }

        private NPlusOneConfidence confidence() {
            return reportableFindings().stream()
                    .map(NPlusOneFinding::getConfidence)
                    .min(Comparator.naturalOrder())
                    .orElse(NPlusOneConfidence.LOW);
        }

        private HibernateStatsSnapshot stats() {
            return reportableFindings().getFirst().getStats();
        }

        private List<String> reasons() {
            return distinctStrings(reportableFindings().stream()
                    .map(NPlusOneFinding::getReason)
                    .toList());
        }

        private List<String> exclusionReasons() {
            return distinctStrings(findings.stream()
                    .map(NPlusOneFinding::getExclusionReason)
                    .filter(reason -> reason != null && !reason.isBlank())
                    .toList());
        }

        private List<String> associationRoles() {
            return distinctStrings(reportableFindings().stream()
                    .map(NPlusOneFinding::getAssociationRole)
                    .filter(associationRole -> associationRole != null && !associationRole.isBlank())
                    .toList());
        }

        private List<String> entityNames() {
            return distinctStrings(reportableFindings().stream()
                    .map(NPlusOneFinding::getEntityName)
                    .filter(entityName -> entityName != null && !entityName.isBlank())
                    .toList());
        }

        private List<SqlFingerprint> repeatedSqlFingerprints() {
            Set<SqlFingerprint> fingerprints = new LinkedHashSet<>();
            reportableFindings().forEach(finding -> fingerprints.addAll(finding.getRepeatedSqlFingerprints()));
            return List.copyOf(fingerprints);
        }

        private List<NPlusOneFinding> reportableFindings() {
            List<NPlusOneFinding> includedFindings = findings.stream()
                    .filter(finding -> !finding.isExcluded())
                    .toList();
            return includedFindings.isEmpty() ? findings : includedFindings;
        }

        private List<String> distinctStrings(List<String> values) {
            return List.copyOf(new LinkedHashSet<>(values));
        }
    }
}
