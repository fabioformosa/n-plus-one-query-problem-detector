package it.fabioformosa.nplusonequeryproblemdetector.scan.rules;

import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneConfidence;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneDetectionContext;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneFinding;

import java.util.List;

public class RepeatedSelectFingerprintRule implements NPlusOneDetectionRule {

    @Override
    public List<NPlusOneFinding> evaluate(NPlusOneDetectionContext context) {
        if (context.stats().getCollectionFetchCount() > 0 || context.stats().getEntityFetchCount() > 0) {
            return List.of();
        }
        if (!context.hasRepeatedSqlEvidence()) {
            return List.of();
        }
        return List.of(new NPlusOneFinding(
                context.testIdentifier(),
                NPlusOneConfidence.MEDIUM,
                "Repeated SELECT SQL fingerprint exceeded the configured threshold.",
                context.stats(),
                null,
                null,
                context.repeatedSqlFingerprints()
        ));
    }
}
