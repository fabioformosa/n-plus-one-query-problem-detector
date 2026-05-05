package it.fabioformosa.nplusonequeryproblemdetector.sampleProductTests.scan;

import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.services.CompanyService;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AbstractIntegrationTestSuite;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "nplusone.scan.enabled=true",
        "nplusone.scan.fail-on-detected=true",
        "nplusone.scan.fail-on-confidence=MEDIUM",
        "nplusone.scan.excluded-sql-fingerprint-patterns=.*from employees.*fk_company=\\?.*"
})
class CompanyServiceWithExcludedSqlFingerprintScanModeIntegrationTest extends AbstractIntegrationTestSuite {

    @Autowired
    private CompanyService companyService;

    @Test
    void givenReviewedSqlFingerprintFinding_whenSqlPatternIsExcluded_thenFailOnDetectedDoesNotFailTheTest() {
        companyService.list(0, 5);
    }
}
