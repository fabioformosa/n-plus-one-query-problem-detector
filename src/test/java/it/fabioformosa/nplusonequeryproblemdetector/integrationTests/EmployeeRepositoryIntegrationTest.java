package it.fabioformosa.nplusonequeryproblemdetector.integrationTests;

import it.fabioformosa.nplusonequeryproblemdetector.NPlusOneQueryProblemAssertions;
import it.fabioformosa.nplusonequeryproblemdetector.NPlusOneQueryProblemDetector;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.entities.Employee;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.repos.EmployeeRepository;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.services.EmployeeService;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AbstractIntegrationTestSuite;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AsciiLogUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * The ManyToOne association is affected by the n+1 query problem even thought the FetchType is Eager by default
 * The solution is to apply the join fetch to the associated entity also
 */
class EmployeeRepositoryIntegrationTest extends AbstractIntegrationTestSuite {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private NPlusOneQueryProblemDetector detector;

    /**
     * By default, All ManyToOne associations are defined with fetchType=Eager
     * If the query doesn't specify explicitly a "join fetch", the associated entity is immediately fetched performing an extra query
     */
    @Test
    void givenEmployeesWithAManyToOneAssociationWithCompanies_whenTheFetchTypeIsEager_thenTheNPlus1QueryProblemIsPresent() {

        detector.startMonitoring();

        int pageSize = 5;
        Page<Employee> paginatedEmployeeList = employeeRepository.findAll(PageRequest.of(0, pageSize, Sort.by("id")));
        AsciiLogUtils.displayEntitiesViaLogs(paginatedEmployeeList.getContent(),
                new String[] { "ID", "First name", "Last name", "Company" },
                e -> new Object[] { e.getId(), e.getFirstname(), e.getLastname(), e.getCompany().getName() }
        );

        detector.stopMonitoring();

        Assertions.assertThat(paginatedEmployeeList.getTotalElements()).isEqualTo(1000);
        Assertions.assertThat(paginatedEmployeeList.getContent()).hasSize(pageSize);
        Assertions.assertThat(paginatedEmployeeList.getTotalPages()).isEqualTo(200);

        //Assert the association is eager
        Assertions.assertThat(paginatedEmployeeList.getContent().getFirst().getCompany()).isNotNull();

        int expectedMaxQueries = 2;
        Assertions.assertThatThrownBy(() ->
                        NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(expectedMaxQueries)
                ).isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected maximum");
        NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).queryExecutionCountIsEqualTo(expectedMaxQueries);
        // !!! n+1 query problem !!!
        NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).entityFetchCountIsEqualTo(pageSize);

    }

    /**
     * The solution, to the problem explained above, is to explicitly define "join fetch" in the query.
     * Since the "join fetch" is needed, it's highly recommended to switch the FetchType to lazy in a way such that
     * we don't want fetch with a join immediately, the extra query is performed only when the associated entity is
     * actually used within a transaction
     */
    @Test
    void givenEmployeesWithAManyToOneAssociationWithCompanies_whenTheQueryHasAJoinWithFetch_thenTheNPlus1QueryProblemIsNotPresent(){

        detector.startMonitoring();

        int pageSize = 5;
        Page<Employee> paginatedEmployeeList = employeeRepository.findAll(EmployeeService.fetchCompanySpecification(), PageRequest.of(0, pageSize, Sort.by("id")));
        AsciiLogUtils.displayEntitiesViaLogs(paginatedEmployeeList.getContent(),
                new String[] { "ID", "First name", "Last name", "Company" },
                e -> new Object[] { e.getId(), e.getFirstname(), e.getLastname(), e.getCompany().getName() }
        );

        detector.stopMonitoring();

        Assertions.assertThat(paginatedEmployeeList.getTotalElements()).isEqualTo(1000);
        Assertions.assertThat(paginatedEmployeeList.getContent()).hasSize(pageSize);
        Assertions.assertThat(paginatedEmployeeList.getTotalPages()).isEqualTo(200);

        //Assert the association is eager
        Assertions.assertThat(paginatedEmployeeList.getContent().getFirst().getCompany()).isNotNull();

        // OK: n+1 query problem not present
        NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(2);
    }

}
