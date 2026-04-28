package it.fabioformosa.nplusonequeryproblemdetector.internal.tests;

import it.fabioformosa.nplusonequeryproblemdetector.junitextension.ExpectMaxQueries;
import it.fabioformosa.nplusonequeryproblemdetector.junitextension.NPlusOneQueryProblemDetectorExtension;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.CompanyDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.PaginatedListDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.services.CompanyService;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AbstractIntegrationTestSuite;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

class NPlusOneQueryProblemDetectorExtensionInternalTest {

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
