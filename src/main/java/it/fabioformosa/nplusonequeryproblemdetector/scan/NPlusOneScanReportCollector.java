package it.fabioformosa.nplusonequeryproblemdetector.scan;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public final class NPlusOneScanReportCollector {

    private static final List<NPlusOneFinding> FINDINGS = new CopyOnWriteArrayList<>();
    private static final AtomicBoolean SHUTDOWN_HOOK_REGISTERED = new AtomicBoolean(false);
    private static volatile NPlusOneScanProperties lastProperties = NPlusOneScanProperties.defaults();
    private static volatile long observedTests;

    private NPlusOneScanReportCollector() {
        throw new IllegalStateException("Utility class");
    }

    public static void registerShutdownHookOnce(NPlusOneScanProperties properties) {
        lastProperties = properties;
        if (SHUTDOWN_HOOK_REGISTERED.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> printReport(System.out), "nplusone-scan-report"));
        }
    }

    public static void recordObservedTest() {
        observedTests++;
    }

    public static void addFindings(List<NPlusOneFinding> findings) {
        FINDINGS.addAll(findings);
    }

    public static List<NPlusOneFinding> findings() {
        return List.copyOf(FINDINGS);
    }

    public static void reset() {
        FINDINGS.clear();
        observedTests = 0;
        lastProperties = NPlusOneScanProperties.defaults();
    }

    public static void printReport(PrintStream printStream) {
        printStream.print(renderReport(lastProperties));
    }

    public static String renderReport(NPlusOneScanProperties properties) {
        List<NPlusOneFinding> findings = new ArrayList<>(FINDINGS);
        findings.sort(Comparator.comparing(NPlusOneFinding::getConfidence).thenComparing(f -> f.getTestIdentifier().displayName()));

        long includedFindings = findings.stream().filter(finding -> !finding.isExcluded()).count();
        long excludedFindings = findings.stream().filter(NPlusOneFinding::isExcluded).count();

        StringBuilder report = new StringBuilder();
        report.append("================================================================================\n");
        report.append("N+1 Query Problem Detector - Scan Report\n");
        report.append("================================================================================\n\n");
        report.append("Scan mode: ENABLED\n");
        report.append("Fail on detected: ").append(properties.failOnDetected()).append("\n");
        report.append("Observed tests: ").append(observedTests).append("\n");
        report.append("Affected tests: ").append(includedFindings).append("\n");
        report.append("Excluded findings: ").append(excludedFindings).append("\n\n");

        if (findings.isEmpty()) {
            report.append("No N+1 query candidates were detected.\n");
        }

        for (NPlusOneFinding finding : findings) {
            appendFinding(report, finding, properties);
        }

        appendSummary(report, findings, properties);
        return report.toString();
    }

    private static void appendFinding(StringBuilder report, NPlusOneFinding finding, NPlusOneScanProperties properties) {
        report.append("--------------------------------------------------------------------------------\n");
        report.append("[").append(finding.isExcluded() ? "EXCLUDED " : "").append(finding.getConfidence()).append("] ")
                .append(finding.getTestIdentifier().displayName()).append("\n");
        report.append("--------------------------------------------------------------------------------\n");
        if (finding.isExcluded()) {
            report.append("Exclusion: ").append(finding.getExclusionReason()).append("\n\n");
        }
        report.append("Reason:\n  ").append(finding.getReason()).append("\n\n");
        report.append("Hibernate statistics:\n");
        report.append("  Query executions:        ").append(finding.getStats().getQueryExecutionCount()).append("\n");
        report.append("  Prepared statements:     ").append(finding.getStats().getPrepareStatementCount()).append("\n");
        report.append("  Entity fetches:          ").append(finding.getStats().getEntityFetchCount()).append("\n");
        report.append("  Collection fetches:      ").append(finding.getStats().getCollectionFetchCount()).append("\n");
        report.append("  Second-level cache hits: ").append(finding.getStats().getSecondLevelCacheHitCount()).append("\n\n");

        if (finding.getAssociationRole() != null) {
            report.append("Likely affected association:\n  ").append(finding.getAssociationRole()).append("\n\n");
        } else if (finding.getEntityName() != null) {
            report.append("Likely fetched entity:\n  ").append(finding.getEntityName()).append("\n\n");
        } else {
            report.append("Likely affected association:\n  Unknown\n\n");
        }

        if (properties.printSqlFingerprints() && !finding.getRepeatedSqlFingerprints().isEmpty()) {
            report.append("Repeated SQL fingerprints:\n");
            finding.getRepeatedSqlFingerprints().stream()
                    .limit(properties.maxSqlFingerprints())
                    .forEach(fingerprint -> report.append("  ").append(fingerprint.count()).append("x ").append(fingerprint.sql()).append("\n"));
            if (finding.getRepeatedSqlFingerprints().size() > properties.maxSqlFingerprints()) {
                report.append("  ... ").append(finding.getRepeatedSqlFingerprints().size() - properties.maxSqlFingerprints()).append(" more omitted\n");
            }
            report.append("\n");
        }

        report.append("Suggested fixes:\n");
        report.append("  1. Use JOIN FETCH [RECOMMENDED] when this use case always needs the association.\n");
        report.append("  2. Use EntityGraph when the fetch shape should be declarative or reusable.\n");
        report.append("  3. Use batch fetching when lazy loading is acceptable but should be grouped.\n");
        report.append("  4. Use DTO/projection queries when only selected fields are needed.\n\n");
    }

    private static void appendSummary(StringBuilder report, List<NPlusOneFinding> findings, NPlusOneScanProperties properties) {
        report.append("================================================================================\n");
        report.append("Summary by confidence\n");
        report.append("================================================================================\n");
        report.append("HIGH:   ").append(countIncluded(findings, NPlusOneConfidence.HIGH)).append("\n");
        report.append("MEDIUM: ").append(countIncluded(findings, NPlusOneConfidence.MEDIUM)).append("\n");
        report.append("LOW:    ").append(countIncluded(findings, NPlusOneConfidence.LOW)).append("\n");
        report.append("EXCLUDED: ").append(findings.stream().filter(NPlusOneFinding::isExcluded).count()).append("\n\n");
        report.append("Build result:\n");
        report.append("  Tests ").append(properties.failOnDetected() ? "may fail on non-excluded findings at or above " + properties.failOnConfidence() : "were not failed because nplusone.scan.fail-on-detected=false").append(".\n");
        report.append("================================================================================\n");
    }

    private static long countIncluded(List<NPlusOneFinding> findings, NPlusOneConfidence confidence) {
        return findings.stream()
                .filter(finding -> !finding.isExcluded())
                .filter(finding -> finding.getConfidence() == confidence)
                .count();
    }
}
