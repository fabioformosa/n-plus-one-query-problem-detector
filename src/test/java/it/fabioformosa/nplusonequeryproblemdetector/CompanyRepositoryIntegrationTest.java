package it.fabioformosa.nplusonequeryproblemdetector;

import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.entities.Company;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.repos.CompanyRepository;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AbstractIntegrationTestSuite;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
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
class CompanyRepositoryIntegrationTest extends AbstractIntegrationTestSuite {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private EntityManager entityManager;

    /**
     * By default, the OneToMany association is defined with FetchType=Lazy
     * If we don't access to the nested collection, no extra queries are performed
     */
    @Test
    void givenALazyOneToManyAssociation_whenWeDontAccessToTheNestedCollection_thenTheNPlusQueryProblemDoesntOccur() {
        Session session = entityManager.unwrap(Session.class);
        Statistics statistics = session.getSessionFactory().getStatistics();
        statistics.clear();

        Page<Company> companyPage = companyRepository.findAll(PageRequest.of(0, 5, Sort.by("id")));
        Assertions.assertThat(companyPage.getTotalElements()).isEqualTo(10);
        Assertions.assertThat(companyPage.getContent()).hasSize(5);
        Assertions.assertThat(companyPage.getTotalPages()).isEqualTo(2);

        // OK: n+1 query problem not present
        Assertions.assertThat(statistics.getQueryExecutionCount()).isEqualTo(2);
        Assertions.assertThat(statistics.getCollectionFetchCount()).isZero();
    }

}
