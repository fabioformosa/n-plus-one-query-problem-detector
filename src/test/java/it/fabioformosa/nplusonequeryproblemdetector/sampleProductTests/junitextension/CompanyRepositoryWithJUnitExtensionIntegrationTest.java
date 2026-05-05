package it.fabioformosa.nplusonequeryproblemdetector.sampleProductTests.junitextension;

import it.fabioformosa.nplusonequeryproblemdetector.junitextension.ExpectMaxQueries;
import it.fabioformosa.nplusonequeryproblemdetector.junitextension.ExpectQueryExecutionCount;
import it.fabioformosa.nplusonequeryproblemdetector.junitextension.NPlusOneQueryDetectorContext;
import it.fabioformosa.nplusonequeryproblemdetector.junitextension.NPlusOneQueryProblemDetectorExtension;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.entities.Company;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.repos.CompanyRepository;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AbstractIntegrationTestSuite;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AsciiLogUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(NPlusOneQueryProblemDetectorExtension.class)
class CompanyRepositoryWithJUnitExtensionIntegrationTest extends AbstractIntegrationTestSuite {

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    @ExpectMaxQueries(2)
    @ExpectQueryExecutionCount(2)
    void givenExtensionAndMaxQueryAnnotations_whenRepositoryIsInvoked_thenTheDetectorMonitorsTheTestMethod() {
        Page<Company> companyPage = companyRepository.findAll(PageRequest.of(0, 5, Sort.by("id")));
        AsciiLogUtils.displayEntitiesViaLogs(
                companyPage.getContent(),
                new String[] { "ID", "Name"},
                c -> new Object[] { c.getId(), c.getName()}
        );

        Assertions.assertThat(companyPage.getTotalElements()).isEqualTo(10);
        Assertions.assertThat(companyPage.getContent()).hasSize(5);
        Assertions.assertThat(companyPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    @ExpectMaxQueries(2)
    @ExpectQueryExecutionCount(2)
    void givenArrangementQueries_whenMonitoringIsRestarted_thenOnlyTheBusinessLogicQueriesAreCounted() {

        Page<Company> arrangementPage = companyRepository.findAll(PageRequest.of(0, 5, Sort.by("id")));
        Assertions.assertThat(arrangementPage.getContent()).hasSize(5);

        NPlusOneQueryDetectorContext.restart();

        Page<Company> companyPage = companyRepository.findAll(PageRequest.of(0, 5, Sort.by("id")));
        AsciiLogUtils.displayEntitiesViaLogs(
                companyPage.getContent(),
                new String[] { "ID", "Name"},
                c -> new Object[] { c.getId(), c.getName()}
        );

        Assertions.assertThat(companyPage.getTotalElements()).isEqualTo(10);
        Assertions.assertThat(companyPage.getContent()).hasSize(5);
        Assertions.assertThat(companyPage.getTotalPages()).isEqualTo(2);
    }
}
