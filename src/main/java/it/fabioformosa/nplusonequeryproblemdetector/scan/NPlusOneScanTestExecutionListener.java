package it.fabioformosa.nplusonequeryproblemdetector.scan;

import it.fabioformosa.nplusonequeryproblemdetector.engine.NPlusOneQueryProblemDetector;
import it.fabioformosa.nplusonequeryproblemdetector.engine.NPlusOneMonitoredStats;
import it.fabioformosa.nplusonequeryproblemdetector.scan.rules.NPlusOneDetectionRules;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.util.List;
import java.util.logging.Logger;

public class NPlusOneScanTestExecutionListener implements TestExecutionListener, Ordered {

    private static final Logger LOGGER = Logger.getLogger(NPlusOneScanTestExecutionListener.class.getName());

    private static final ThreadLocal<NPlusOneScanSession> CURRENT_SESSION = new ThreadLocal<>();

    private final NPlusOneDetectionRules detectionRules = new NPlusOneDetectionRules();

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void beforeTestExecution(TestContext testContext) {
        NPlusOneScanProperties properties = NPlusOneScanProperties.from(testContext.getApplicationContext().getEnvironment());
        if (!properties.enabled()) {
            return;
        }

        NPlusOneScanReportCollector.registerShutdownHookOnce(properties);
        NPlusOneScanReportCollector.recordObservedTest();

        NPlusOneQueryProblemDetector detector = resolveDetector(testContext.getApplicationContext());
        if (detector == null) {
            LOGGER.info(() -> "N+1 query scan is enabled, but no monitorable EntityManagerFactory was found for "
                    + testContext.getTestClass().getName() + "." + testContext.getTestMethod().getName()
                    + "; skipping this test.");
            return;
        }

        detector.startMonitoring();

        SqlStatementCapture.start();
        CURRENT_SESSION.set(new NPlusOneScanSession(properties, detector));
    }

    @Override
    public void afterTestExecution(TestContext testContext) {
        NPlusOneScanSession session = CURRENT_SESSION.get();
        if (session == null) {
            return;
        }

        try {
            session.detector().stopMonitoring();
            NPlusOneMonitoredStats monitoredStats = session.detector().getDetailedMonitoredStats();
            List<SqlFingerprint> repeatedSqlFingerprints = SqlFingerprinting.repeatedSelectFingerprints(SqlStatementCapture.stop());

            NPlusOneDetectionContext detectionContext = new NPlusOneDetectionContext(
                    new NPlusOneTestIdentifier(testContext.getTestClass().getName(), testContext.getTestMethod().getName()),
                    monitoredStats.aggregate(),
                    monitoredStats.collectionFetchCounts(),
                    monitoredStats.entityFetchCounts(),
                    repeatedSqlFingerprints,
                    session.properties().thresholds()
            );
            List<NPlusOneFinding> findings = detectionRules.evaluate(detectionContext);
            NPlusOneExclusions exclusions = new NPlusOneExclusions(session.properties());
            findings.forEach(exclusions::apply);
            NPlusOneScanReportCollector.addFindings(findings);

            if (NPlusOneScanFailurePolicy.shouldFail(session.properties(), findings)) {
                throw new AssertionError("N+1 query scan detected non-excluded findings at or above " + session.properties().failOnConfidence());
            }
        } finally {
            CURRENT_SESSION.remove();
        }
    }

    private record NPlusOneScanSession(
            NPlusOneScanProperties properties,
            NPlusOneQueryProblemDetector detector
    ) {
    }

    private NPlusOneQueryProblemDetector resolveDetector(ApplicationContext applicationContext) {
        NPlusOneQueryProblemDetector detector = applicationContext.getBeanProvider(NPlusOneQueryProblemDetector.class).getIfAvailable();
        if (detector != null && detector.canMonitor()) {
            return detector;
        }

        EntityManagerFactory entityManagerFactory = applicationContext.getBeanProvider(EntityManagerFactory.class).getIfAvailable();
        if (entityManagerFactory == null) {
            return null;
        }
        return new NPlusOneQueryProblemDetector(entityManagerFactory);
    }
}
