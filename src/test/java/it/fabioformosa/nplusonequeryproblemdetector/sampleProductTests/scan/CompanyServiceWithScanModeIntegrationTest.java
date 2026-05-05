package it.fabioformosa.nplusonequeryproblemdetector.sampleProductTests.scan;

import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.CompanyDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.PaginatedListDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.services.CompanyService;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AbstractIntegrationTestSuite;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "nplusone.scan.enabled=true")
class CompanyServiceWithScanModeIntegrationTest extends AbstractIntegrationTestSuite {

    @Autowired
    private CompanyService companyService;

    @Test
    void givenExistingTestWithoutDetectorAnnotations_whenLazyCollectionsAreFetched_thenScanModeReportsCandidate() {
        PaginatedListDto<CompanyDto> companyDtoList = companyService.list(0, 5);

        Assertions.assertThat(companyDtoList.getItems()).hasSize(5);
        Assertions.assertThat(companyDtoList.getTotalItems()).isEqualTo(10);
    }

    @Test
    void givenExistingTestWithoutDetectorAnnotations_whenJoinFetchIsUsed_thenScanModeDoesNotReportLazyFetchCandidate() {
        PaginatedListDto<CompanyDto> companyDtoList = companyService.listWithFetchViaJQL(0, 5);

        Assertions.assertThat(companyDtoList.getItems()).hasSize(5);
        Assertions.assertThat(companyDtoList.getTotalItems()).isEqualTo(10);
    }
}
