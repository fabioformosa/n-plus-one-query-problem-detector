package it.fabioformosa.nplusonequeryproblemdetector.internal.tests.limitations;

import it.fabioformosa.nplusonequeryproblemdetector.engine.HibernateStatsSnapshot;
import it.fabioformosa.nplusonequeryproblemdetector.engine.NPlusOneQueryProblemDetector;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.services.CompanyService;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AbstractIntegrationTestSuite;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates why detector tests must be serialized when they share a Hibernate SessionFactory.
 * See README.md#parallel-test-execution-caveat.
 */
class NPlusOneQueryProblemDetectorConcurrencyInternalTest extends AbstractIntegrationTestSuite {

    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(5);

    @Autowired
    private CompanyService companyService;

    @Autowired
    private NPlusOneQueryProblemDetector detector;

    @Test
    void givenOverlappingMonitoringWindows_whenSecondWindowStarts_thenFirstWindowDetectorSnapshotsAreOverwritten() throws Exception {
        HibernateStatsSnapshot isolatedStats = monitorCompanyList();
        Assertions.assertThat(isolatedStats.getQueryExecutionCount()).isEqualTo(2);
        Assertions.assertThat(isolatedStats.getCollectionFetchCount()).isEqualTo(5);

        CountDownLatch firstWindowHasExecutedQueries = new CountDownLatch(1);
        CountDownLatch secondWindowHasStartedAndOverwrittenDetectorSnapshot = new CountDownLatch(1);
        CountDownLatch firstWindowHasCapturedStats = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        try {
            Future<HibernateStatsSnapshot> firstWindowStats = executorService.submit(() -> {
                detector.startMonitoring();
                companyService.list(0, 5);
                firstWindowHasExecutedQueries.countDown();

                await(secondWindowHasStartedAndOverwrittenDetectorSnapshot);
                detector.stopMonitoring();
                HibernateStatsSnapshot monitoredStats = detector.getMonitoredStats();
                firstWindowHasCapturedStats.countDown();
                return monitoredStats;
            });

            Future<?> secondWindow = executorService.submit(() -> {
                await(firstWindowHasExecutedQueries);
                detector.startMonitoring();
                secondWindowHasStartedAndOverwrittenDetectorSnapshot.countDown();

                await(firstWindowHasCapturedStats);
                detector.stopMonitoring();
                return null;
            });

            HibernateStatsSnapshot corruptedStats = firstWindowStats.get(WAIT_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            secondWindow.get(WAIT_TIMEOUT.toSeconds(), TimeUnit.SECONDS);

            Assertions.assertThat(corruptedStats.getQueryExecutionCount())
                    .as("the detector is a singleton, so the second monitoring window overwrites the first start snapshot")
                    .isZero();
            Assertions.assertThat(corruptedStats.getCollectionFetchCount())
                    .as("the detector is a singleton, so overlapping windows must still be avoided")
                    .isZero();
        } finally {
            executorService.shutdownNow();
        }
    }

    private HibernateStatsSnapshot monitorCompanyList() {
        detector.startMonitoring();
        companyService.list(0, 5);
        detector.stopMonitoring();
        return detector.getMonitoredStats();
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        if (!latch.await(WAIT_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
            throw new AssertionError("Timed out waiting for concurrent test step");
        }
    }
}
