package it.fabioformosa.nplusonequeryproblemdetector.internal.tests;

import it.fabioformosa.nplusonequeryproblemdetector.engine.NPlusOneQueryProblemAssertions;
import it.fabioformosa.nplusonequeryproblemdetector.engine.NPlusOneQueryProblemDetector;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.CompanyDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.EmployeeDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.PaginatedListDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.entities.Employee;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.repos.EmployeeRepository;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.services.CompanyService;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.services.EmployeeService;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AbstractIntegrationTestSuite;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AsciiLogUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class NPlusOneQueryProblemAssertionsInternalTest extends AbstractIntegrationTestSuite {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private NPlusOneQueryProblemDetector detector;

    @Test
    void givenLazyCollectionsAreFetched_whenMaxQueriesIsTooLow_thenAssertionErrorIsRaised() {
        detector.startMonitoring();

        PaginatedListDto<CompanyDto> companyDtoList = companyService.list(0, 5);
        AsciiLogUtils.displayEntitiesViaLogs(
                companyDtoList.getItems(),
                new String[] { "ID", "Name", "Employees" },
                c -> new Object[] { c.getId(), c.getName(), c.getEmployees().size() }
        );

        detector.stopMonitoring();

        Assertions.assertThatThrownBy(() -> NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(2))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected maximum");
        NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).queryExecutionCountIsEqualTo(2);
        NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).collectionFetchCountIsEqualTo(5);
    }

    @Test
    void givenEagerEntityFetchesInService_whenMaxQueriesIsTooLow_thenAssertionErrorIsRaised() {
        detector.startMonitoring();

        int pageSize = 5;
        PaginatedListDto<EmployeeDto> employeePage = employeeService.list(0, pageSize);
        AsciiLogUtils.displayEntitiesViaLogs(employeePage.getItems(),
                new String[] { "ID", "First name", "Last name", "Company" },
                e -> new Object[] { e.getId(), e.getFirstname(), e.getLastname(), e.getCompanyName() }
        );

        detector.stopMonitoring();

        Assertions.assertThatThrownBy(() -> NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(2))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected maximum");
        NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).queryExecutionCountIsEqualTo(2);
        NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).entityFetchCountIsEqualTo(pageSize);
    }

    @Test
    void givenEagerEntityFetchesInRepository_whenMaxQueriesIsTooLow_thenAssertionErrorIsRaised() {
        detector.startMonitoring();

        int pageSize = 5;
        Page<Employee> paginatedEmployeeList = employeeRepository.findAll(PageRequest.of(0, pageSize, Sort.by("id")));
        AsciiLogUtils.displayEntitiesViaLogs(paginatedEmployeeList.getContent(),
                new String[] { "ID", "First name", "Last name", "Company" },
                e -> new Object[] { e.getId(), e.getFirstname(), e.getLastname(), e.getCompany().getName() }
        );

        detector.stopMonitoring();

        Assertions.assertThatThrownBy(() -> NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(2))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected maximum");
        NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).queryExecutionCountIsEqualTo(2);
        NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).entityFetchCountIsEqualTo(pageSize);
    }
}
