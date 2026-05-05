package it.fabioformosa.nplusonequeryproblemdetector.scan.rules;

import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneDetectionContext;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneFinding;

import java.util.List;

public class NPlusOneDetectionRules {

    private final List<NPlusOneDetectionRule> rules;

    public NPlusOneDetectionRules() {
        this(List.of(
                new RepeatedCollectionFetchRule(),
                new RepeatedEntityFetchRule(),
                new RepeatedSelectFingerprintRule(),
                new HighPreparedStatementCountRule()
        ));
    }

    public NPlusOneDetectionRules(List<NPlusOneDetectionRule> rules) {
        this.rules = rules;
    }

    public List<NPlusOneFinding> evaluate(NPlusOneDetectionContext context) {
        return rules.stream()
                .flatMap(rule -> rule.evaluate(context).stream())
                .toList();
    }
}
