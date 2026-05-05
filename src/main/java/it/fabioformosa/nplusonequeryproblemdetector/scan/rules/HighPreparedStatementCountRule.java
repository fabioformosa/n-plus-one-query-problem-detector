package it.fabioformosa.nplusonequeryproblemdetector.scan.rules;

import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneConfidence;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneDetectionContext;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneFinding;

import java.util.List;

public class HighPreparedStatementCountRule implements NPlusOneDetectionRule {

    @Override
    public List<NPlusOneFinding> evaluate(NPlusOneDetectionContext context) {
        boolean hasStrongerSignal = context.stats().getCollectionFetchCount() > 0
                || context.stats().getEntityFetchCount() > 0
                || context.hasRepeatedSqlEvidence();
        if (hasStrongerSignal || context.stats().getPrepareStatementCount() < context.thresholds().minPreparedStatements()) {
            return List.of();
        }
        return List.of(new NPlusOneFinding(
                context.testIdentifier(),
                NPlusOneConfidence.LOW,
                "Prepared statement count exceeded the configured threshold without a more specific lazy-fetch signal.",
                context.stats(),
                null,
                null,
                context.repeatedSqlFingerprints()
        ));
    }
}
