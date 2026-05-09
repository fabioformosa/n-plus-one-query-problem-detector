package it.fabioformosa.nplusonequeryproblemdetector.internal.tests.scan;

import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.CompanyDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.PaginatedListDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.services.CompanyService;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneConfidence;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneScanProperties;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneScanReportCollector;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneScanTestExecutionListener;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AbstractIntegrationTestSuite;
import jakarta.persistence.EntityManagerFactory;
import org.assertj.core.api.Assertions;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

class NPlusOneScanTestExecutionListenerInternalTest {

    private static final String COMPANY_EMPLOYEES_ROLE = "it.fabioformosa.nplusonequeryproblemdetector.sampleproject.entities.Company.employees";

    @AfterEach
    void cleanCollector() {
        NPlusOneScanReportCollector.reset();
    }

    @Test
    void givenLibraryIsOnClasspath_whenSpringFactoriesAreLoaded_thenScanListenerIsDiscovered() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        List<String> listenerClassNames = new ArrayList<>();

        for (URL springFactoriesUrl : Collections.list(classLoader.getResources("META-INF/spring.factories"))) {
            Properties springFactories = new Properties();
            try (InputStream inputStream = springFactoriesUrl.openStream()) {
                springFactories.load(inputStream);
            }
            String testExecutionListeners = springFactories.getProperty("org.springframework.test.context.TestExecutionListener");
            if (testExecutionListeners != null) {
                listenerClassNames.addAll(Arrays.stream(testExecutionListeners.split(","))
                        .map(entry -> entry.replace("\\", "").trim())
                        .filter(entry -> !entry.isBlank())
                        .toList());
            }
        }

        Assertions.assertThat(listenerClassNames)
                .contains(NPlusOneScanTestExecutionListener.class.getName());
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

    @Test
    void givenScanModeIsEnabledOnNonJpaSpringTest_whenSpringTestRuns_thenListenerSkipsContext() {
        NPlusOneScanReportCollector.reset();

        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(NonJpaScanModeCase.class))
                .execute()
                .testEvents()
                .succeeded()
                .assertStatistics(stats -> stats.succeeded(1));

        Assertions.assertThat(NPlusOneScanReportCollector.findings()).isEmpty();
        Assertions.assertThat(NPlusOneScanReportCollector.renderReport(NPlusOneScanProperties.defaults()))
                .contains("Observed tests: 1")
                .contains("No N+1 query candidates were detected.");
    }

    @Test
    void givenScanModeIsEnabledOnNonJpaSpringTest_whenSpringTestRuns_thenDiagnosticIsLogged() {
        NPlusOneScanReportCollector.reset();
        Logger listenerLogger = Logger.getLogger(NPlusOneScanTestExecutionListener.class.getName());
        CapturingLogHandler capturingLogHandler = new CapturingLogHandler();
        Level previousLevel = listenerLogger.getLevel();
        listenerLogger.setLevel(Level.INFO);
        listenerLogger.addHandler(capturingLogHandler);

        try {
            EngineTestKit.engine("junit-jupiter")
                    .selectors(selectClass(NonJpaScanModeCase.class))
                    .execute()
                    .testEvents()
                    .succeeded()
                    .assertStatistics(stats -> stats.succeeded(1));
        } finally {
            listenerLogger.removeHandler(capturingLogHandler);
            listenerLogger.setLevel(previousLevel);
        }

        Assertions.assertThat(capturingLogHandler.messages())
                .anySatisfy(message -> Assertions.assertThat(message)
                        .contains("N+1 query scan is enabled")
                        .contains("no monitorable EntityManagerFactory"));
    }

    @Test
    void givenScanModeIsEnabledWithoutDetectorBean_whenEntityManagerFactoryExists_thenListenerUsesFallbackDetector() {
        NPlusOneScanReportCollector.reset();

        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(EntityManagerFactoryOnlyScanModeCase.class))
                .execute()
                .testEvents()
                .succeeded()
                .assertStatistics(stats -> stats.succeeded(1));
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

    @SpringJUnitConfig(NonJpaConfig.class)
    @TestPropertySource(properties = "n-plus-one-query-detector.scan.enabled=true")
    static class NonJpaScanModeCase {
        @Autowired
        private ApplicationContext applicationContext;

        @Test
        void givenNoJpaInfrastructure_whenScanIsEnabled_thenTestStillRuns() {
            Assertions.assertThat(applicationContext.getBeanProvider(EntityManagerFactory.class).getIfAvailable()).isNull();
        }
    }

    @SpringJUnitConfig(EntityManagerFactoryOnlyConfig.class)
    @TestPropertySource(properties = "n-plus-one-query-detector.scan.enabled=true")
    static class EntityManagerFactoryOnlyScanModeCase {
        @Autowired
        private ApplicationContext applicationContext;

        @Test
        void givenEntityManagerFactoryOnly_whenScanIsEnabled_thenTestStillRuns() {
            Assertions.assertThat(applicationContext.getBeanProvider(EntityManagerFactory.class).getIfAvailable()).isNotNull();
        }
    }

    @Configuration
    static class NonJpaConfig {
    }

    @Configuration
    static class EntityManagerFactoryOnlyConfig {
        @Bean
        EntityManagerFactory entityManagerFactory() {
            Statistics statistics = Mockito.mock(Statistics.class);
            Mockito.when(statistics.getCollectionRoleNames()).thenReturn(new String[0]);
            Mockito.when(statistics.getEntityNames()).thenReturn(new String[0]);

            SessionFactory sessionFactory = Mockito.mock(SessionFactory.class);
            Mockito.when(sessionFactory.getStatistics()).thenReturn(statistics);

            EntityManagerFactory entityManagerFactory = Mockito.mock(EntityManagerFactory.class);
            Mockito.when(entityManagerFactory.unwrap(SessionFactory.class)).thenReturn(sessionFactory);
            return entityManagerFactory;
        }
    }

    private static final class CapturingLogHandler extends Handler {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            messages.add(record.getMessage());
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        List<String> messages() {
            return messages;
        }
    }
}
