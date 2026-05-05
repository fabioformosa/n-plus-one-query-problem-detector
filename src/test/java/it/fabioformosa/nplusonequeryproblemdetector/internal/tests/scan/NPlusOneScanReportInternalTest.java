package it.fabioformosa.nplusonequeryproblemdetector.internal.tests.scan;

import it.fabioformosa.nplusonequeryproblemdetector.engine.HibernateStatsSnapshot;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneConfidence;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneFinding;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneScanProperties;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneScanReportCollector;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneTestIdentifier;
import it.fabioformosa.nplusonequeryproblemdetector.scan.SqlFingerprint;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class NPlusOneScanReportInternalTest {

    @AfterEach
    void cleanCollector() {
        NPlusOneScanReportCollector.reset();
    }

    @Test
    void givenFindings_whenReportIsRendered_thenItContainsStatsSqlAndRecommendedJoinFetchFix() {
        NPlusOneScanReportCollector.recordObservedTest();
        NPlusOneScanReportCollector.addFindings(List.of(new NPlusOneFinding(
                new NPlusOneTestIdentifier("CompanyServiceTest", "list"),
                NPlusOneConfidence.HIGH,
                "Lazy collection fetch pattern detected.",
                HibernateStatsSnapshot.builder()
                        .queryExecutionCount(2)
                        .prepareStatementCount(7)
                        .collectionFetchCount(5)
                        .build(),
                "com.example.Company.employees",
                null,
                List.of(new SqlFingerprint("select * from employees where fk_company = ?", 5))
        )));

        String report = NPlusOneScanReportCollector.renderReport(enabledProperties());

        Assertions.assertThat(report)
                .contains("N+1 Query Problem Detector - Scan Report")
                .contains("[HIGH] CompanyServiceTest.list")
                .contains("Prepared statements:     7")
                .contains("com.example.Company.employees")
                .contains("5x select * from employees where fk_company = ?")
                .contains("JOIN FETCH [RECOMMENDED]");
    }

    @Test
    void givenMoreSqlFingerprintsThanReportLimit_whenReportIsRendered_thenOnlyConfiguredNumberIsPrinted() {
        NPlusOneScanReportCollector.recordObservedTest();
        NPlusOneScanReportCollector.addFindings(List.of(new NPlusOneFinding(
                new NPlusOneTestIdentifier("CompanyServiceTest", "list"),
                NPlusOneConfidence.HIGH,
                "Lazy collection fetch pattern detected.",
                HibernateStatsSnapshot.builder().collectionFetchCount(5).build(),
                "com.example.Company.employees",
                null,
                List.of(
                        new SqlFingerprint("select one", 5),
                        new SqlFingerprint("select two", 4),
                        new SqlFingerprint("select three", 3)
                )
        )));

        NPlusOneScanProperties defaults = enabledProperties();
        NPlusOneScanProperties properties = new NPlusOneScanProperties(true, false, defaults.failOnConfidence(), false,
                true, 2, defaults.thresholds(), List.of(), List.of(), List.of(), List.of());

        String report = NPlusOneScanReportCollector.renderReport(properties);

        Assertions.assertThat(report)
                .contains("5x select one")
                .contains("4x select two")
                .doesNotContain("3x select three")
                .contains("... 1 more omitted");
    }

    @Test
    void givenExcludedFinding_whenReportIsRendered_thenConfidenceSummaryDoesNotCountIt() {
        NPlusOneFinding excludedFinding = new NPlusOneFinding(
                new NPlusOneTestIdentifier("CompanyServiceTest", "list"),
                NPlusOneConfidence.HIGH,
                "Lazy collection fetch pattern detected.",
                HibernateStatsSnapshot.builder().collectionFetchCount(5).build(),
                "com.example.Company.employees",
                null,
                List.of()
        );
        excludedFinding.exclude("matched nplusone.scan.excluded-associations");
        NPlusOneScanReportCollector.addFindings(List.of(excludedFinding));

        String report = NPlusOneScanReportCollector.renderReport(enabledProperties());

        Assertions.assertThat(report)
                .contains("HIGH:   0")
                .contains("MEDIUM: 0")
                .contains("LOW:    0")
                .contains("EXCLUDED: 1");
    }

    private NPlusOneScanProperties enabledProperties() {
        NPlusOneScanProperties defaults = NPlusOneScanProperties.defaults();
        return new NPlusOneScanProperties(true, false, defaults.failOnConfidence(), false, true, 5,
                defaults.thresholds(), List.of(), List.of(), List.of(), List.of());
    }
}
