package it.fabioformosa.nplusonequeryproblemdetector.internal.tests.engine;

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
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
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

    @Autowired
    private EntityManager entityManager;

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

    @Test
    void givenHibernateStatisticsAreDisabled_whenDetectorMonitors_thenStatisticsAreTemporarilyEnabledAndRestored() {
        Statistics statistics = getSessionStatistics();
        boolean statisticsEnabledBeforeTest = statistics.isStatisticsEnabled();

        try {
            statistics.setStatisticsEnabled(false);

            detector.startMonitoring();
            try {
                Assertions.assertThat(statistics.isStatisticsEnabled()).isTrue();
            } finally {
                detector.stopMonitoring();
            }

            Assertions.assertThat(statistics.isStatisticsEnabled()).isFalse();
        } finally {
            statistics.setStatisticsEnabled(statisticsEnabledBeforeTest);
        }
    }

    @Test
    void givenStatisticsAlreadyContainQueries_whenDetectorMonitors_thenOnlyDeltaWindowIsCounted() {
        companyService.list(0, 5);

        detector.startMonitoring();
        companyService.listWithFetchViaJQL(0, 5);
        detector.stopMonitoring();

        NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).queryExecutionCountIsEqualTo(2);
        NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).collectionFetchCountIsEqualTo(0);
    }

    private Statistics getSessionStatistics() {
        Session session = entityManager.unwrap(Session.class);
        return session.getSessionFactory().getStatistics();
    }
}
