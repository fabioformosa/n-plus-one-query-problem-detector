package it.fabioformosa.nplusonequeryproblemdetector.integrationTests;

import it.fabioformosa.nplusonequeryproblemdetector.NPlusOneQueryProblemAssertions;
import it.fabioformosa.nplusonequeryproblemdetector.NPlusOneQueryProblemDetector;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.CompanyDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.PaginatedListDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.services.CompanyService;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AbstractIntegrationTestSuite;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AsciiLogUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The OneToMany association is affected by the n+1 query problem if we have a list of entities (companies) and for each of them
 * we access to the nested collection (employees). This kind of pattern is common, supposing the need to convert the entities in DTOs.
 * An extra query must be performed for each iteration, resulting in the n+1 query problem.<br>
 *
 * The solution is to specify a join fetch whenever we know we're going to access to the nested collection
 */
class CompanyServiceWithDetectorIntegrationTest extends AbstractIntegrationTestSuite {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private NPlusOneQueryProblemDetector detector;

    /**
     * By default, the OneToMany association is defined with a fetchType=Lazy.
     * In the service we iterate over the companies and we access to the related nested collection of employees
     * to convert into DTOs.
     * If we forget to specify explicitly a fetch join in the query, an extra query is done for each company
     * to load the associated employees
     */
    @Test
    void givenCompaniesWithAssociationEmployees_whenTheFetchTypeIsLazy_thenTheNPlus1QueryProblemIsPresent(){

        detector.startMonitoring();

        int pageSize = 5;
        PaginatedListDto<CompanyDto> companyDtoList = companyService.list(0, pageSize);
        AsciiLogUtils.displayEntitiesViaLogs(
            companyDtoList.getItems(),
            new String[] { "ID", "Name", "Employees" },
            c -> new Object[] { c.getId(), c.getName(), c.getEmployees().size() }
        );
        Assertions.assertThat(companyDtoList.getTotalItems()).isEqualTo(10);
        Assertions.assertThat(companyDtoList.getItems()).hasSize(pageSize);
        Assertions.assertThat(companyDtoList.getTotalPages()).isEqualTo(2);

        detector.stopMonitoring();

        //If the assertion raise an error, it means the n+1 query problem is present
        Assertions.assertThatThrownBy(() ->
                NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(2)
            ).isInstanceOf(AssertionError.class)
             .hasMessageContaining("Expected maximum");

            NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).queryExecutionCountIsEqualTo(2);
            // !!! n+1 query problem !!!
            NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).collectionFetchCountIsEqualTo(pageSize);
    }

    /**
     * Specifying a join fetch into the query, the problem explained above is solved!
     */
    @Test
    void givenCompaniesWithAssociationEmployees_whenTheQueryFetchesExplicitlyViaJQL_thenTheNPlus1QueryProblemIsNotPresent(){

        detector.startMonitoring();

        int pageSize = 5;
        PaginatedListDto<CompanyDto> companyDtoList = companyService.listWithFetchViaJQL(0, pageSize);
        AsciiLogUtils.displayEntitiesViaLogs(
                companyDtoList.getItems(),
                new String[] { "ID", "Name", "Employees" },
                c -> new Object[] { c.getId(), c.getName(), c.getEmployees().size() }
        );

        detector.stopMonitoring();

        Assertions.assertThat(companyDtoList.getTotalItems()).isEqualTo(10);
        Assertions.assertThat(companyDtoList.getItems()).hasSize(pageSize);
        Assertions.assertThat(companyDtoList.getTotalPages()).isEqualTo(2);

        // OK: n+1 query problem not present
        int expectedQueryCount = 2;
        NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(expectedQueryCount);
    }

    /**
     * Specifying a join fetch through the JPA specification, the problem explained above is solved!
     */
    @Test
    void givenCompaniesWithAssociationEmployees_whenTheQueryFetchesExplicitlyViaSpecification_thenTheNPlus1QueryProblemIsNotPresent(){

        detector.startMonitoring();

        int pageSize = 5;
        PaginatedListDto<CompanyDto> companyDtoList = companyService.listWithFetchViaSpecification(0, pageSize);
        AsciiLogUtils.displayEntitiesViaLogs(
                companyDtoList.getItems(),
                new String[] { "ID", "Name", "Employees" },
                c -> new Object[] { c.getId(), c.getName(), c.getEmployees().size() }
        );

        detector.stopMonitoring();

        Assertions.assertThat(companyDtoList.getTotalItems()).isEqualTo(10);
        Assertions.assertThat(companyDtoList.getItems()).hasSize(pageSize);
        Assertions.assertThat(companyDtoList.getTotalPages()).isEqualTo(2);

        // OK: n+1 query problem not present
        int expectedQueryCount = 2;
        NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(expectedQueryCount);
    }

}
