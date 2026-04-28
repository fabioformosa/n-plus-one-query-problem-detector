package it.fabioformosa.nplusonequeryproblemdetector.integrationTests.junitextension;

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
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@ExtendWith(NPlusOneQueryProblemDetectorExtension.class)
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

    @Test
    void givenExpectMaxQueriesAnnotation_whenQueryCountIsGreaterThanExpected_thenAssertionErrorIsRaised() {
        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(FailingExpectMaxQueriesCase.class))
                .execute()
                .testEvents()
                .failed()
                .assertThatEvents()
                .hasSize(1)
                .allMatch(event -> event.getPayload(TestExecutionResult.class)
                        .flatMap(result -> result.getThrowable())
                        .filter(AssertionError.class::isInstance)
                        .map(Throwable::getMessage)
                        .filter(message -> message.contains("Expected maximum 2 queries"))
                        .isPresent());
    }

    @ExtendWith(NPlusOneQueryProblemDetectorExtension.class)
    static class FailingExpectMaxQueriesCase extends AbstractIntegrationTestSuite {

        @Autowired
        private CompanyService companyService;

        @Test
        @ExpectMaxQueries(2)
        void givenLazyCollectionsAreFetched_whenMaxQueriesIsTooLow_thenTheExtensionFailsTheTest() {
            PaginatedListDto<CompanyDto> list = companyService.list(0, 5);
            Assertions.assertThat(list.getItems()).hasSize(5);
        }
    }
}
