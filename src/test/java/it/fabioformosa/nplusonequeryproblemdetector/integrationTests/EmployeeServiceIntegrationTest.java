package it.fabioformosa.nplusonequeryproblemdetector.integrationTests;

import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.EmployeeDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.PaginatedListDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.services.EmployeeService;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AbstractIntegrationTestSuite;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;


class EmployeeServiceIntegrationTest extends AbstractIntegrationTestSuite {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EntityManager entityManager;

    /**
     * By default the ManyToOne association has defined with a fetchType=Eager
     * If the query doesn't specify explicitly a fetch, the associated entity is fetched with an extra query for each
     * element of the returned collection
     */
    @Test
    void given1000EmployeesWithAssociatedCompanies_whenTheFetchTypeIsEager_thenNPlus1ProblemIsPresent(){
        Session session = entityManager.unwrap(Session.class);
        Statistics statistics = session.getSessionFactory().getStatistics();
        statistics.clear();

        int pageSize = 5;
        PaginatedListDto<EmployeeDto> employeePage = employeeService.list(0, pageSize);
        Assertions.assertThat(employeePage.getTotalItems()).isEqualTo(1000);
        Assertions.assertThat(employeePage.getItems()).hasSize(5);
        Assertions.assertThat(employeePage.getTotalPages()).isEqualTo(200);

        Assertions.assertThat(statistics.getQueryExecutionCount()).isEqualTo(2);

        // !!! n+1 query problem !!!
        Assertions.assertThat(statistics.getEntityFetchCount()).isEqualTo(pageSize);
    }

    /**
     * Specifying a join fetch into the query, the problem explained above is solved!
     */
    @Test
    void given1000EmployeesWithAssociatedCompanies_whenTheQueryFetchesExplicitly_thenNPlus1ProblemIsNotPresent(){
        Session session = entityManager.unwrap(Session.class);
        Statistics statistics = session.getSessionFactory().getStatistics();
        statistics.clear();

        int pageSize = 5;
        PaginatedListDto<EmployeeDto> employeePage = employeeService.listWithCompany(0, pageSize);
        Assertions.assertThat(employeePage.getTotalItems()).isEqualTo(1000);
        Assertions.assertThat(employeePage.getItems()).hasSize(5);
        Assertions.assertThat(employeePage.getTotalPages()).isEqualTo(200);

        // OK: n+1 query problem not present
        Assertions.assertThat(statistics.getQueryExecutionCount()).isEqualTo(2);
        Assertions.assertThat(statistics.getEntityFetchCount()).isZero();
    }


    /**
     * Specifying a join fetch into the query (e.g. via specification), the problem explained above is solved!
     */
    @Test
    void given1000EmployeesWithAssociatedCompanies_whenTheQueryFetchesExplicitlyViaSpecification_thenNPlus1ProblemIsNotPresent(){
        Session session = entityManager.unwrap(Session.class);
        Statistics statistics = session.getSessionFactory().getStatistics();
        statistics.clear();

        int pageSize = 5;
        PaginatedListDto<EmployeeDto> employeePage = employeeService.listWithSpecification(0, pageSize);
        Assertions.assertThat(employeePage.getTotalItems()).isEqualTo(1000);
        Assertions.assertThat(employeePage.getItems()).hasSize(5);
        Assertions.assertThat(employeePage.getTotalPages()).isEqualTo(200);

        // OK: n+1 query problem not present
        Assertions.assertThat(statistics.getQueryExecutionCount()).isEqualTo(2);
        Assertions.assertThat(statistics.getEntityFetchCount()).isZero();
    }

}
