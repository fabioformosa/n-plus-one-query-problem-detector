package it.fabioformosa.nplusonequeryproblemdetector.scan.rules;

import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneConfidence;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneDetectionContext;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneFinding;

import java.util.List;

public class RepeatedEntityFetchRule implements NPlusOneDetectionRule {

    @Override
    public List<NPlusOneFinding> evaluate(NPlusOneDetectionContext context) {
        return context.entityFetchCounts().entrySet().stream()
                .filter(entry -> entry.getValue() >= context.thresholds().minEntityFetches())
                .map(entry -> new NPlusOneFinding(
                        context.testIdentifier(),
                        context.hasRepeatedSqlEvidence() ? NPlusOneConfidence.HIGH : NPlusOneConfidence.MEDIUM,
                        context.hasRepeatedSqlEvidence()
                                ? "Lazy entity fetch pattern detected and confirmed by repeated SELECT SQL."
                                : "Lazy entity fetch count exceeded the configured threshold.",
                        context.stats(),
                        null,
                        entry.getKey(),
                        context.repeatedSqlFingerprints()
                ))
                .toList();
    }
}
