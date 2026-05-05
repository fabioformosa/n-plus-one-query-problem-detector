package it.fabioformosa.nplusonequeryproblemdetector.sampleProductTests.detectorObject;

import it.fabioformosa.nplusonequeryproblemdetector.engine.NPlusOneQueryProblemAssertions;
import it.fabioformosa.nplusonequeryproblemdetector.engine.NPlusOneQueryProblemDetector;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.EmployeeDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.PaginatedListDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.services.EmployeeService;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AbstractIntegrationTestSuite;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AsciiLogUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EmployeeServiceIntegrationTest extends AbstractIntegrationTestSuite {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private NPlusOneQueryProblemDetector detector;

    @Test
    void givenEmployeesWithAssociatedCompanies_whenTheFetchTypeIsEager_thenTheDetectorCollectsNPlusOneStats() {
        detector.startMonitoring();

        int pageSize = 5;
        PaginatedListDto<EmployeeDto> employeePage = employeeService.list(0, pageSize);
        AsciiLogUtils.displayEntitiesViaLogs(employeePage.getItems(),
                new String[] { "ID", "First name", "Last name", "Company" },
                e -> new Object[] { e.getId(), e.getFirstname(), e.getLastname(), e.getCompanyName() }
        );

        detector.stopMonitoring();

        Assertions.assertThat(employeePage.getTotalItems()).isEqualTo(1000);
        Assertions.assertThat(employeePage.getItems()).hasSize(pageSize);
        Assertions.assertThat(employeePage.getTotalPages()).isEqualTo(200);

        NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(7); // put 7 just to make the test green, but we're in front of a n+1 query problem
        NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).queryExecutionCountIsEqualTo(2);
        NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).entityFetchCountIsEqualTo(pageSize);
    }

    @Test
    void givenEmployeesWithAssociatedCompanies_whenTheQueryFetchesExplicitly_thenNPlus1ProblemIsNotPresent() {
        detector.startMonitoring();

        int pageSize = 5;
        PaginatedListDto<EmployeeDto> employeePage = employeeService.listWithCompany(0, pageSize);
        AsciiLogUtils.displayEntitiesViaLogs(employeePage.getItems(),
                new String[] { "ID", "First name", "Last name", "Company" },
                e -> new Object[] { e.getId(), e.getFirstname(), e.getLastname(), e.getCompanyName() }
        );

        detector.stopMonitoring();

        Assertions.assertThat(employeePage.getTotalItems()).isEqualTo(1000);
        Assertions.assertThat(employeePage.getItems()).hasSize(5);
        Assertions.assertThat(employeePage.getTotalPages()).isEqualTo(200);

        NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(2);
    }

    @Test
    void givenEmployeesWithAssociatedCompanies_whenTheQueryFetchesExplicitlyViaSpecification_thenNPlus1ProblemIsNotPresent() {
        detector.startMonitoring();

        int pageSize = 5;
        PaginatedListDto<EmployeeDto> employeePage = employeeService.listWithSpecification(0, pageSize);
        AsciiLogUtils.displayEntitiesViaLogs(employeePage.getItems(),
                new String[] { "ID", "First name", "Last name", "Company" },
                e -> new Object[] { e.getId(), e.getFirstname(), e.getLastname(), e.getCompanyName() }
        );

        detector.stopMonitoring();

        Assertions.assertThat(employeePage.getTotalItems()).isEqualTo(1000);
        Assertions.assertThat(employeePage.getItems()).hasSize(5);
        Assertions.assertThat(employeePage.getTotalPages()).isEqualTo(200);

        NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(2);
    }
}
