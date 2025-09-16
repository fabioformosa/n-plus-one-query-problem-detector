package it.fabioformosa.nplusonequeryproblemdetector.integrationTests;

import it.fabioformosa.nplusonequeryproblemdetector.NPlusOneQueryProblemAssertions;
import it.fabioformosa.nplusonequeryproblemdetector.NPlusOneQueryProblemDetector;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.entities.Company;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.repos.CompanyRepository;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AbstractIntegrationTestSuite;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AsciiLogUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Here we test the JPA Repo built on the entity Company that has a one-to-many association (with Employees) which has fetchType=Lazy
 * by default. Finding all entities in a paginated we would expect 2 queries: the count query + the query to retrieve the paginated list.
 * This kind of data retrieval is not affected by n+1 query problem because we don't access to the employees, forcing the lazy loading to perform
 * extra queries
 */
class CompanyRepositoryWithDetectorIntegrationTest extends AbstractIntegrationTestSuite {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private NPlusOneQueryProblemDetector detector;

    /**
     * By default, the OneToMany association is defined with FetchType=Lazy
     * If we don't access to the nested collection, no extra queries are performed
     */
    @Test
    void givenALazyOneToManyAssociation_whenWeDontAccessToTheNestedCollection_thenTheNPlusQueryProblemDoesntOccur() {

        detector.startMonitoring();

        Page<Company> companyPage = companyRepository.findAll(PageRequest.of(0, 5, Sort.by("id")));
        AsciiLogUtils.displayEntitiesViaLogs(
                companyPage.getContent(),
                new String[] { "ID", "Name"},
                c -> new Object[] { c.getId(), c.getName()}
        );

        detector.stopMonitoring();

        org.assertj.core.api.Assertions.assertThat(companyPage.getTotalElements()).isEqualTo(10);
        org.assertj.core.api.Assertions.assertThat(companyPage.getContent()).hasSize(5);
        org.assertj.core.api.Assertions.assertThat(companyPage.getTotalPages()).isEqualTo(2);

        // OK: n+1 query problem not present
        int expectedMaxQueries = 2;
        NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(expectedMaxQueries);
    }

}
