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
        "n-plus-one-query-detector.scan.excluded-associations=it.fabioformosa.nplusonequeryproblemdetector.sampleproject.entities.Company.employees"
})
class CompanyServiceWithExcludedScanModeIntegrationTest extends AbstractIntegrationTestSuite {

    @Autowired
    private CompanyService companyService;

    @Test
    void givenReviewedFalsePositive_whenAssociationIsExcluded_thenFailOnDetectedDoesNotFailTheTest() {
        Assertions.assertThat(companyService.list(0, 5).getItems()).hasSize(5);
    }
}
