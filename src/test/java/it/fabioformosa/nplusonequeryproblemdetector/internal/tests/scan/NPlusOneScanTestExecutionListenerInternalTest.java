package it.fabioformosa.nplusonequeryproblemdetector.internal.tests.scan;

import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.CompanyDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.PaginatedListDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.services.CompanyService;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneConfidence;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneScanReportCollector;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AbstractIntegrationTestSuite;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

class NPlusOneScanTestExecutionListenerInternalTest {

    private static final String COMPANY_EMPLOYEES_ROLE = "it.fabioformosa.nplusonequeryproblemdetector.sampleproject.entities.Company.employees";

    @AfterEach
    void cleanCollector() {
        NPlusOneScanReportCollector.reset();
    }

    @Test
    void givenScanModeIsDisabled_whenSpringTestRuns_thenNoFindingIsCollected() {
        NPlusOneScanReportCollector.reset();

        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(DisabledScanModeCase.class))
                .execute()
                .testEvents()
                .succeeded()
                .assertStatistics(stats -> stats.succeeded(1));

        Assertions.assertThat(NPlusOneScanReportCollector.findings()).isEmpty();
    }

    @Test
    void givenScanModeIsEnabled_whenLazyCollectionsAreFetched_thenFindingIsCollected() {
        NPlusOneScanReportCollector.reset();

        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(CollectionNPlusOneScanModeCase.class))
                .execute()
                .testEvents()
                .succeeded()
                .assertStatistics(stats -> stats.succeeded(1));

        Assertions.assertThat(NPlusOneScanReportCollector.findings())
                .anySatisfy(finding -> {
                    Assertions.assertThat(finding.getAssociationRole()).isEqualTo(COMPANY_EMPLOYEES_ROLE);
                    Assertions.assertThat(finding.getConfidence()).isIn(NPlusOneConfidence.HIGH, NPlusOneConfidence.MEDIUM);
                });
    }

    @Test
    void givenFailOnHighConfidence_whenFindingIsNotExcluded_thenSpringTestFails() {
        NPlusOneScanReportCollector.reset();

        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(FailingScanModeCase.class))
                .execute()
                .testEvents()
                .failed()
                .assertThatEvents()
                .hasSize(1)
                .allMatch(event -> event.getPayload(TestExecutionResult.class)
                        .flatMap(result -> result.getThrowable())
                        .filter(AssertionError.class::isInstance)
                        .map(Throwable::getMessage)
                        .filter(message -> message.contains("N+1 query scan detected"))
                        .isPresent());
    }

    @Test
    void givenExcludedAssociation_whenFailOnDetectedIsEnabled_thenSpringTestDoesNotFail() {
        NPlusOneScanReportCollector.reset();

        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(ExcludedAssociationScanModeCase.class))
                .execute()
                .testEvents()
                .succeeded()
                .assertStatistics(stats -> stats.succeeded(1));

        Assertions.assertThat(NPlusOneScanReportCollector.findings())
                .anySatisfy(finding -> {
                    Assertions.assertThat(finding.getAssociationRole()).isEqualTo(COMPANY_EMPLOYEES_ROLE);
                    Assertions.assertThat(finding.isExcluded()).isTrue();
                });
    }

    @TestPropertySource(properties = "n-plus-one-query-detector.scan.enabled=false")
    static class DisabledScanModeCase extends AbstractIntegrationTestSuite {
        @Autowired
        private CompanyService companyService;

        @Test
        void givenLazyCollectionsAreFetched_whenScanIsDisabled_thenNoScanFindingIsCollected() {
            PaginatedListDto<CompanyDto> companyDtoList = companyService.list(0, 5);
            Assertions.assertThat(companyDtoList.getItems()).hasSize(5);
        }
    }

    @TestPropertySource(properties = "n-plus-one-query-detector.scan.enabled=true")
    static class CollectionNPlusOneScanModeCase extends AbstractIntegrationTestSuite {
        @Autowired
        private CompanyService companyService;

        @Test
        void givenLazyCollectionsAreFetched_whenScanIsEnabled_thenFindingIsCollected() {
            PaginatedListDto<CompanyDto> companyDtoList = companyService.list(0, 5);
            Assertions.assertThat(companyDtoList.getItems()).hasSize(5);
        }
    }

    @TestPropertySource(properties = {
            "n-plus-one-query-detector.scan.enabled=true",
            "n-plus-one-query-detector.scan.fail-on-detected=true",
            "n-plus-one-query-detector.scan.fail-on-confidence=MEDIUM"
    })
    static class FailingScanModeCase extends AbstractIntegrationTestSuite {
        @Autowired
        private CompanyService companyService;

        @Test
        void givenLazyCollectionsAreFetched_whenFailOnDetectedIsEnabled_thenTheTestFails() {
            PaginatedListDto<CompanyDto> companyDtoList = companyService.list(0, 5);
            Assertions.assertThat(companyDtoList.getItems()).hasSize(5);
        }
    }

    @TestPropertySource(properties = {
            "n-plus-one-query-detector.scan.enabled=true",
            "n-plus-one-query-detector.scan.fail-on-detected=true",
            "n-plus-one-query-detector.scan.fail-on-confidence=MEDIUM",
            "n-plus-one-query-detector.scan.excluded-associations=" + COMPANY_EMPLOYEES_ROLE
    })
    static class ExcludedAssociationScanModeCase extends AbstractIntegrationTestSuite {
        @Autowired
        private CompanyService companyService;

        @Test
        void givenLazyCollectionsAreFetched_whenAssociationIsExcluded_thenTheTestDoesNotFail() {
            PaginatedListDto<CompanyDto> companyDtoList = companyService.list(0, 5);
            Assertions.assertThat(companyDtoList.getItems()).hasSize(5);
        }
    }
}
