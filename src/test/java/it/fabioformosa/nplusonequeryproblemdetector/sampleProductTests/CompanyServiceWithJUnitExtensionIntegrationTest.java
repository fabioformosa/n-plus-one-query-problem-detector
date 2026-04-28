package it.fabioformosa.nplusonequeryproblemdetector.sampleProductTests;

import it.fabioformosa.nplusonequeryproblemdetector.junitextension.ExpectCollectionFetchCount;
import it.fabioformosa.nplusonequeryproblemdetector.junitextension.ExpectMaxQueries;
import it.fabioformosa.nplusonequeryproblemdetector.junitextension.ExpectQueryExecutionCount;
import it.fabioformosa.nplusonequeryproblemdetector.junitextension.NPlusOneQueryProblemDetectorExtension;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.CompanyDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.PaginatedListDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.services.CompanyService;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AbstractIntegrationTestSuite;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AsciiLogUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

@ExtendWith(NPlusOneQueryProblemDetectorExtension.class)
class CompanyServiceWithJUnitExtensionIntegrationTest extends AbstractIntegrationTestSuite {

    @Autowired
    private CompanyService companyService;

    @Test
    @ExpectMaxQueries(7) // put 7 just to make the test green, but we're in front of a n+1 query problem
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
