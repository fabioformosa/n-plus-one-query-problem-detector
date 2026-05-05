package it.fabioformosa.nplusonequeryproblemdetector.internal.tests.scan;

import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneConfidence;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneScanProperties;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class NPlusOneScanPropertiesInternalTest {

    @Test
    void givenNoProperties_whenLoaded_thenScanModeIsDisabledByDefault() {
        NPlusOneScanProperties properties = NPlusOneScanProperties.from(new MockEnvironment());

        Assertions.assertThat(properties.enabled()).isFalse();
        Assertions.assertThat(properties.thresholds().minCollectionFetches()).isEqualTo(2);
        Assertions.assertThat(properties.thresholds().minEntityFetches()).isEqualTo(2);
        Assertions.assertThat(properties.thresholds().minRepeatedSelectFingerprint()).isEqualTo(2);
    }

    @Test
    void givenCustomProperties_whenLoaded_thenValuesAreBoundFromEnvironment() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("nplusone.scan.enabled", "true")
                .withProperty("nplusone.scan.fail-on-detected", "true")
                .withProperty("nplusone.scan.fail-on-confidence", "MEDIUM")
                .withProperty("nplusone.scan.threshold.min-collection-fetches", "4")
                .withProperty("nplusone.scan.threshold.min-entity-fetches", "3")
                .withProperty("nplusone.scan.report.max-sql-fingerprints", "2")
                .withProperty("nplusone.scan.excluded-associations", "Company.employees, Order.lines");

        NPlusOneScanProperties properties = NPlusOneScanProperties.from(environment);

        Assertions.assertThat(properties.enabled()).isTrue();
        Assertions.assertThat(properties.failOnDetected()).isTrue();
        Assertions.assertThat(properties.failOnConfidence()).isEqualTo(NPlusOneConfidence.MEDIUM);
        Assertions.assertThat(properties.thresholds().minCollectionFetches()).isEqualTo(4);
        Assertions.assertThat(properties.thresholds().minEntityFetches()).isEqualTo(3);
        Assertions.assertThat(properties.maxSqlFingerprints()).isEqualTo(2);
        Assertions.assertThat(properties.excludedAssociations()).containsExactly("Company.employees", "Order.lines");
    }
}
