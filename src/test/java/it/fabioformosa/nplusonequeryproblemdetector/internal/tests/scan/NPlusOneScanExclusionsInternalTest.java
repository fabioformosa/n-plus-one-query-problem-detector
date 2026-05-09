package it.fabioformosa.nplusonequeryproblemdetector.internal.tests.scan;

import it.fabioformosa.nplusonequeryproblemdetector.engine.HibernateStatsSnapshot;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneConfidence;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneExclusions;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneFinding;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneScanProperties;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneTestIdentifier;
import it.fabioformosa.nplusonequeryproblemdetector.scan.SqlFingerprint;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class NPlusOneScanExclusionsInternalTest {

    @Test
    void givenExcludedAssociation_whenExclusionsAreApplied_thenFindingIsExcluded() {
        NPlusOneFinding finding = finding("com.example.Company.employees", null, List.of());
        new NPlusOneExclusions(properties(List.of(), List.of("com.example.Company.employees"), List.of(), List.of())).apply(finding);

        Assertions.assertThat(finding.isExcluded()).isTrue();
        Assertions.assertThat(finding.getExclusionReason()).contains("excluded-associations");
    }

    @Test
    void givenExcludedTestWildcard_whenExclusionsAreApplied_thenFindingIsExcluded() {
        NPlusOneFinding finding = finding("com.example.Company.employees", null, List.of());
        new NPlusOneExclusions(properties(List.of("ExampleTest.*"), List.of(), List.of(), List.of())).apply(finding);

        Assertions.assertThat(finding.isExcluded()).isTrue();
        Assertions.assertThat(finding.getExclusionReason()).contains("excluded-tests");
    }

    @Test
    void givenExcludedEntity_whenExclusionsAreApplied_thenFindingIsExcluded() {
        NPlusOneFinding finding = finding(null, "com.example.Company", List.of());
        new NPlusOneExclusions(properties(List.of(), List.of(), List.of("com.example.Company"), List.of())).apply(finding);

        Assertions.assertThat(finding.isExcluded()).isTrue();
        Assertions.assertThat(finding.getExclusionReason()).contains("excluded-entities");
    }

    @Test
    void givenExcludedSqlFingerprintPattern_whenExclusionsAreApplied_thenFindingIsExcluded() {
        NPlusOneFinding finding = finding(null, null, List.of(new SqlFingerprint("select * from audit_log where entity_id = ?", 3)));
        new NPlusOneExclusions(properties(List.of(), List.of(), List.of(), List.of(".*from audit_log.*entity_id = \\?.*"))).apply(finding);

        Assertions.assertThat(finding.isExcluded()).isTrue();
        Assertions.assertThat(finding.getExclusionReason()).contains("excluded-sql-fingerprint-patterns");
    }

    private NPlusOneFinding finding(String associationRole, String entityName, List<SqlFingerprint> fingerprints) {
        return new NPlusOneFinding(
                new NPlusOneTestIdentifier("ExampleTest", "test"),
                NPlusOneConfidence.HIGH,
                "reason",
                HibernateStatsSnapshot.builder().collectionFetchCount(3).build(),
                associationRole,
                entityName,
                fingerprints
        );
    }

    private NPlusOneScanProperties properties(List<String> excludedTests, List<String> excludedAssociations,
                                              List<String> excludedEntities, List<String> excludedSqlPatterns) {
        NPlusOneScanProperties defaults = NPlusOneScanProperties.defaults();
        return new NPlusOneScanProperties(
                true,
                defaults.failOnDetected(),
                defaults.failOnConfidence(),
                defaults.includeCleanTests(),
                defaults.reportOutput(),
                defaults.printSqlFingerprints(),
                defaults.maxSqlFingerprints(),
                defaults.thresholds(),
                excludedTests,
                excludedAssociations,
                excludedEntities,
                excludedSqlPatterns
        );
    }
}
