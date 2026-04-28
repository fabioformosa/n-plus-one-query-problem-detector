package it.fabioformosa.nplusonequeryproblemdetector.sampleProductTests;

import it.fabioformosa.nplusonequeryproblemdetector.engine.NPlusOneQueryProblemAssertions;
import it.fabioformosa.nplusonequeryproblemdetector.engine.NPlusOneQueryProblemDetector;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.entities.Company;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.repos.CompanyRepository;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AbstractIntegrationTestSuite;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AsciiLogUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class CompanyRepositoryWithDetectorIntegrationTest extends AbstractIntegrationTestSuite {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private NPlusOneQueryProblemDetector detector;

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

        Assertions.assertThat(companyPage.getTotalElements()).isEqualTo(10);
        Assertions.assertThat(companyPage.getContent()).hasSize(5);
        Assertions.assertThat(companyPage.getTotalPages()).isEqualTo(2);

        NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(2);
    }
}
