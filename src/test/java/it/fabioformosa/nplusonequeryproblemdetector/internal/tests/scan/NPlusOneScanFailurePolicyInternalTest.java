package it.fabioformosa.nplusonequeryproblemdetector.internal.tests.scan;

import it.fabioformosa.nplusonequeryproblemdetector.engine.HibernateStatsSnapshot;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneConfidence;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneFinding;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneScanFailurePolicy;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneScanProperties;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneTestIdentifier;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class NPlusOneScanFailurePolicyInternalTest {

    @Test
    void givenFailOnDetectedIsFalse_whenFindingExists_thenBuildShouldNotFail() {
        Assertions.assertThat(NPlusOneScanFailurePolicy.shouldFail(properties(false, NPlusOneConfidence.HIGH), List.of(finding(NPlusOneConfidence.HIGH))))
                .isFalse();
    }

    @Test
    void givenFailOnHighOnly_whenMediumFindingExists_thenBuildShouldNotFail() {
        Assertions.assertThat(NPlusOneScanFailurePolicy.shouldFail(properties(true, NPlusOneConfidence.HIGH), List.of(finding(NPlusOneConfidence.MEDIUM))))
                .isFalse();
    }

    @Test
    void givenFailOnMedium_whenMediumFindingExists_thenBuildShouldFail() {
        Assertions.assertThat(NPlusOneScanFailurePolicy.shouldFail(properties(true, NPlusOneConfidence.MEDIUM), List.of(finding(NPlusOneConfidence.MEDIUM))))
                .isTrue();
    }

    @Test
    void givenExcludedFinding_whenFailOnDetected_thenBuildShouldNotFail() {
        NPlusOneFinding finding = finding(NPlusOneConfidence.HIGH);
        finding.exclude("verified false positive");

        Assertions.assertThat(NPlusOneScanFailurePolicy.shouldFail(properties(true, NPlusOneConfidence.HIGH), List.of(finding)))
                .isFalse();
    }

    private NPlusOneFinding finding(NPlusOneConfidence confidence) {
        return new NPlusOneFinding(new NPlusOneTestIdentifier("ExampleTest", "test"), confidence, "reason",
                HibernateStatsSnapshot.builder().collectionFetchCount(2).build(), "Company.employees", null, List.of());
    }

    private NPlusOneScanProperties properties(boolean failOnDetected, NPlusOneConfidence failOnConfidence) {
        NPlusOneScanProperties defaults = NPlusOneScanProperties.defaults();
        return new NPlusOneScanProperties(true, failOnDetected, failOnConfidence, false, true, 5,
                defaults.thresholds(), List.of(), List.of(), List.of(), List.of());
    }
}
