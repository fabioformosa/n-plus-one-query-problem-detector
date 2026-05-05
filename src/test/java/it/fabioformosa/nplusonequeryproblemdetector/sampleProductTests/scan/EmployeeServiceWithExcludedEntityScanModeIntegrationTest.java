package it.fabioformosa.nplusonequeryproblemdetector.sampleProductTests.scan;

import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.services.EmployeeService;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AbstractIntegrationTestSuite;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "n-plus-one-query-detector.scan.enabled=true",
        "n-plus-one-query-detector.scan.fail-on-detected=true",
        "n-plus-one-query-detector.scan.fail-on-confidence=MEDIUM",
        "n-plus-one-query-detector.scan.excluded-entities=it.fabioformosa.nplusonequeryproblemdetector.sampleproject.entities.Company"
})
class EmployeeServiceWithExcludedEntityScanModeIntegrationTest extends AbstractIntegrationTestSuite {

    @Autowired
    private EmployeeService employeeService;

    @Test
    void givenReviewedEntityFetchFinding_whenEntityIsExcluded_thenFailOnDetectedDoesNotFailTheTest() {
        Assertions.assertThat(employeeService.list(0, 5).getItems()).hasSize(5);
    }
}
