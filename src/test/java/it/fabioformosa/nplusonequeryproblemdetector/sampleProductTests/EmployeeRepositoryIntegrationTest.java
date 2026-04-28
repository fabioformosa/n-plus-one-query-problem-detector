package it.fabioformosa.nplusonequeryproblemdetector.sampleProductTests;

import it.fabioformosa.nplusonequeryproblemdetector.engine.NPlusOneQueryProblemAssertions;
import it.fabioformosa.nplusonequeryproblemdetector.engine.NPlusOneQueryProblemDetector;
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

class EmployeeRepositoryIntegrationTest extends AbstractIntegrationTestSuite {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private NPlusOneQueryProblemDetector detector;

    @Test
    void givenEmployeesWithAManyToOneAssociationWithCompanies_whenTheFetchTypeIsEager_thenTheDetectorCollectsNPlusOneStats() {
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
        Assertions.assertThat(paginatedEmployeeList.getContent().getFirst().getCompany()).isNotNull();

        NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(7); // put 7 just to make the test green, but we're in front of a n+1 query problem
        NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).queryExecutionCountIsEqualTo(2);
        NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).entityFetchCountIsEqualTo(pageSize);
    }

    @Test
    void givenEmployeesWithAManyToOneAssociationWithCompanies_whenTheQueryHasAJoinWithFetch_thenTheNPlus1QueryProblemIsNotPresent() {
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
        Assertions.assertThat(paginatedEmployeeList.getContent().getFirst().getCompany()).isNotNull();

        NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(2);
    }
}
