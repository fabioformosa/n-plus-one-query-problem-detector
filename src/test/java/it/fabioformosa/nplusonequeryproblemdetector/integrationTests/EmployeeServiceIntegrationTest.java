package it.fabioformosa.nplusonequeryproblemdetector.integrationTests;

import it.fabioformosa.nplusonequeryproblemdetector.NPlusOneQueryProblemAssertions;
import it.fabioformosa.nplusonequeryproblemdetector.NPlusOneQueryProblemDetector;
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

    /**
     * By default the ManyToOne association has defined with a fetchType=Eager
     * If the query doesn't specify explicitly a fetch, the associated entity is fetched with an extra query for each
     * element of the returned collection
     */
    @Test
    void given1000EmployeesWithAssociatedCompanies_whenTheFetchTypeIsEager_thenNPlus1ProblemIsPresent() {
        detector.startMonitoring();

        int pageSize = 5;
        PaginatedListDto<EmployeeDto> employeePage = employeeService.list(0, pageSize);
        AsciiLogUtils.displayEntitiesViaLogs(employeePage.getItems(),
                new String[]{"ID", "First name", "Last name", "Company"},
                e -> new Object[]{e.getId(), e.getFirstname(), e.getLastname(), e.getCompanyName()}
        );

        detector.stopMonitoring();

        Assertions.assertThat(employeePage.getTotalItems()).isEqualTo(1000);
        Assertions.assertThat(employeePage.getItems()).hasSize(5);
        Assertions.assertThat(employeePage.getTotalPages()).isEqualTo(200);

        Assertions.assertThatThrownBy(() ->
                        NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(2)
                ).isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected maximum");
        NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).queryExecutionCountIsEqualTo(2);
        // !!! n+1 query problem !!!
        NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).entityFetchCountIsEqualTo(pageSize);
    }

    /**
     * Specifying a join fetch into the query, the problem explained above is solved!
     */
    @Test
    void given1000EmployeesWithAssociatedCompanies_whenTheQueryFetchesExplicitly_thenNPlus1ProblemIsNotPresent() {

        detector.startMonitoring();

        int pageSize = 5;
        PaginatedListDto<EmployeeDto> employeePage = employeeService.listWithCompany(0, pageSize);
        AsciiLogUtils.displayEntitiesViaLogs(employeePage.getItems(),
                new String[]{"ID", "First name", "Last name", "Company"},
                e -> new Object[]{e.getId(), e.getFirstname(), e.getLastname(), e.getCompanyName()}
        );

        detector.stopMonitoring();

        Assertions.assertThat(employeePage.getTotalItems()).isEqualTo(1000);
        Assertions.assertThat(employeePage.getItems()).hasSize(5);
        Assertions.assertThat(employeePage.getTotalPages()).isEqualTo(200);

        // OK: n+1 query problem not present
        NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(2);
    }


    /**
     * Specifying a join fetch into the query (e.g. via specification), the problem explained above is solved!
     */
    @Test
    void given1000EmployeesWithAssociatedCompanies_whenTheQueryFetchesExplicitlyViaSpecification_thenNPlus1ProblemIsNotPresent() {

        detector.startMonitoring();

        int pageSize = 5;
        PaginatedListDto<EmployeeDto> employeePage = employeeService.listWithSpecification(0, pageSize);
        AsciiLogUtils.displayEntitiesViaLogs(employeePage.getItems(),
                new String[]{"ID", "First name", "Last name", "Company"},
                e -> new Object[]{e.getId(), e.getFirstname(), e.getLastname(), e.getCompanyName()}
        );

        detector.stopMonitoring();

        Assertions.assertThat(employeePage.getTotalItems()).isEqualTo(1000);
        Assertions.assertThat(employeePage.getItems()).hasSize(5);
        Assertions.assertThat(employeePage.getTotalPages()).isEqualTo(200);

        // OK: n+1 query problem not present
        NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(2);
    }

}
