package it.fabioformosa.nplusonequeryproblemdetector.sampleProductTests.scan;

import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.services.CompanyService;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AbstractIntegrationTestSuite;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "n-plus-one-query-detector.scan.enabled=true",
        "n-plus-one-query-detector.scan.fail-on-detected=true",
        "n-plus-one-query-detector.scan.fail-on-confidence=MEDIUM",
        "n-plus-one-query-detector.scan.excluded-sql-fingerprint-patterns=.*from employees.*fk_company=\\?.*"
})
class CompanyServiceWithExcludedSqlFingerprintScanModeIntegrationTest extends AbstractIntegrationTestSuite {

    @Autowired
    private CompanyService companyService;

    @Test
    void givenReviewedSqlFingerprintFinding_whenSqlPatternIsExcluded_thenFailOnDetectedDoesNotFailTheTest() {
        Assertions.assertThat(companyService.list(0, 5).getItems()).hasSize(5);
    }
}
