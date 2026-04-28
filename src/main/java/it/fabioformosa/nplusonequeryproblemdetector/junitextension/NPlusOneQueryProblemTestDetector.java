package it.fabioformosa.nplusonequeryproblemdetector.junitextension;

import it.fabioformosa.nplusonequeryproblemdetector.engine.NPlusOneQueryProblemAssertions;
import it.fabioformosa.nplusonequeryproblemdetector.engine.NPlusOneQueryProblemDetector;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.reflect.Method;

public class NPlusOneQueryProblemTestDetector implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        getDetector(context).startMonitoring();
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        NPlusOneQueryProblemDetector detector = getDetector(context);
        detector.stopMonitoring();

        if (context.getExecutionException().isPresent()) {
            return;
        }

        Method testMethod = context.getRequiredTestMethod();
        assertMaxQueriesIfExpected(detector, testMethod);
        assertQueryExecutionCountIfExpected(detector, testMethod);
        assertEntityFetchCountIfExpected(detector, testMethod);
        assertCollectionFetchCountIfExpected(detector, testMethod);
    }

    private NPlusOneQueryProblemDetector getDetector(ExtensionContext context) {
        return SpringExtension.getApplicationContext(context).getBean(NPlusOneQueryProblemDetector.class);
    }

    private void assertMaxQueriesIfExpected(NPlusOneQueryProblemDetector detector, Method testMethod) {
        ExpectMaxQueries annotation = testMethod.getAnnotation(ExpectMaxQueries.class);
        if (annotation != null) {
            NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(annotation.value());
        }
    }

    private void assertQueryExecutionCountIfExpected(NPlusOneQueryProblemDetector detector, Method testMethod) {
        ExpectQueryExecutionCount annotation = testMethod.getAnnotation(ExpectQueryExecutionCount.class);
        if (annotation != null) {
            NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).queryExecutionCountIsEqualTo(annotation.value());
        }
    }

    private void assertEntityFetchCountIfExpected(NPlusOneQueryProblemDetector detector, Method testMethod) {
        ExpectEntityFetchCount annotation = testMethod.getAnnotation(ExpectEntityFetchCount.class);
        if (annotation != null) {
            NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).entityFetchCountIsEqualTo(annotation.value());
        }
    }

    private void assertCollectionFetchCountIfExpected(NPlusOneQueryProblemDetector detector, Method testMethod) {
        ExpectCollectionFetchCount annotation = testMethod.getAnnotation(ExpectCollectionFetchCount.class);
        if (annotation != null) {
            NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).collectionFetchCountIsEqualTo(annotation.value());
        }
    }
}
