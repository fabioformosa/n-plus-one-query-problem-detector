package it.fabioformosa.nplusonequeryproblemdetector.sampleProductTests.scan;

import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.services.EmployeeService;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AbstractIntegrationTestSuite;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "nplusone.scan.enabled=true",
        "nplusone.scan.fail-on-detected=true",
        "nplusone.scan.fail-on-confidence=MEDIUM",
        "nplusone.scan.excluded-entities=it.fabioformosa.nplusonequeryproblemdetector.sampleproject.entities.Company"
})
class EmployeeServiceWithExcludedEntityScanModeIntegrationTest extends AbstractIntegrationTestSuite {

    @Autowired
    private EmployeeService employeeService;

    @Test
    void givenReviewedEntityFetchFinding_whenEntityIsExcluded_thenFailOnDetectedDoesNotFailTheTest() {
        employeeService.list(0, 5);
    }
}
