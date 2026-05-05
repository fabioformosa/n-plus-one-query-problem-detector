package it.fabioformosa.nplusonequeryproblemdetector.scan.rules;

import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneConfidence;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneDetectionContext;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneFinding;

import java.util.List;

public class RepeatedCollectionFetchRule implements NPlusOneDetectionRule {

    @Override
    public List<NPlusOneFinding> evaluate(NPlusOneDetectionContext context) {
        return context.collectionFetchCounts().entrySet().stream()
                .filter(entry -> entry.getValue() >= context.thresholds().minCollectionFetches())
                .map(entry -> new NPlusOneFinding(
                        context.testIdentifier(),
                        context.hasRepeatedSqlEvidence() ? NPlusOneConfidence.HIGH : NPlusOneConfidence.MEDIUM,
                        context.hasRepeatedSqlEvidence()
                                ? "Lazy collection fetch pattern detected and confirmed by repeated SELECT SQL."
                                : "Lazy collection fetch count exceeded the configured threshold.",
                        context.stats(),
                        entry.getKey(),
                        null,
                        context.repeatedSqlFingerprints()
                ))
                .toList();
    }
}
