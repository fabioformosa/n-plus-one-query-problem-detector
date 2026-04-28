package it.fabioformosa.nplusonequeryproblemdetector.integrationTests.junitextension;

import it.fabioformosa.nplusonequeryproblemdetector.junitextension.ExpectCollectionFetchCount;
import it.fabioformosa.nplusonequeryproblemdetector.junitextension.ExpectMaxQueries;
import it.fabioformosa.nplusonequeryproblemdetector.junitextension.ExpectQueryExecutionCount;
import it.fabioformosa.nplusonequeryproblemdetector.junitextension.NPlusOneQueryProblemTestDetector;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.CompanyDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.PaginatedListDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.entities.Company;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.repos.CompanyRepository;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.services.CompanyService;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AbstractIntegrationTestSuite;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AsciiLogUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(NPlusOneQueryProblemTestDetector.class)
class CompanyServiceWithJUnitExtensionIntegrationTest extends AbstractIntegrationTestSuite {

    @Autowired
    private CompanyService companyService;

    @Test
    @ExpectMaxQueries(7)
    @ExpectQueryExecutionCount(2)
    @ExpectCollectionFetchCount(5)
    void givenExtensionAndStatsAnnotations_whenLazyCollectionsAreFetched_thenTheDetectorAssertsStats() {
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
    }
}
