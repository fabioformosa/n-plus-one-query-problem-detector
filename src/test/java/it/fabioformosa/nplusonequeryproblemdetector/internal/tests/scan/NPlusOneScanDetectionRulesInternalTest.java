package it.fabioformosa.nplusonequeryproblemdetector.internal.tests.scan;

import it.fabioformosa.nplusonequeryproblemdetector.engine.HibernateStatsSnapshot;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneConfidence;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneDetectionContext;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneFinding;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneTestIdentifier;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneThresholds;
import it.fabioformosa.nplusonequeryproblemdetector.scan.SqlFingerprint;
import it.fabioformosa.nplusonequeryproblemdetector.scan.rules.NPlusOneDetectionRules;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class NPlusOneScanDetectionRulesInternalTest {

    private final NPlusOneDetectionRules rules = new NPlusOneDetectionRules();

    @Test
    void givenCollectionFetchesAndRepeatedSql_whenRulesEvaluate_thenHighConfidenceFindingIsCreated() {
        List<NPlusOneFinding> findings = rules.evaluate(context(
                snapshot(2, 7, 0, 5, Map.of("it.example.Company.employees", 5L), Map.of()),
                List.of(new SqlFingerprint("select * from employees where fk_company = ?", 5))
        ));

        Assertions.assertThat(findings).hasSize(1);
        Assertions.assertThat(findings.getFirst().getConfidence()).isEqualTo(NPlusOneConfidence.HIGH);
        Assertions.assertThat(findings.getFirst().getAssociationRole()).isEqualTo("it.example.Company.employees");
    }

    @Test
    void givenCollectionFetchesWithoutRepeatedSql_whenRulesEvaluate_thenMediumConfidenceFindingIsCreated() {
        List<NPlusOneFinding> findings = rules.evaluate(context(
                snapshot(2, 7, 0, 5, Map.of("it.example.Company.employees", 5L), Map.of()),
                List.of()
        ));

        Assertions.assertThat(findings).hasSize(1);
        Assertions.assertThat(findings.getFirst().getConfidence()).isEqualTo(NPlusOneConfidence.MEDIUM);
    }

    @Test
    void givenEntityFetchesAndRepeatedSql_whenRulesEvaluate_thenHighConfidenceFindingIsCreated() {
        List<NPlusOneFinding> findings = rules.evaluate(context(
                snapshot(2, 7, 5, 0, Map.of(), Map.of("it.example.Company", 5L)),
                List.of(new SqlFingerprint("select * from companies where id = ?", 5))
        ));

        Assertions.assertThat(findings).hasSize(1);
        Assertions.assertThat(findings.getFirst().getConfidence()).isEqualTo(NPlusOneConfidence.HIGH);
        Assertions.assertThat(findings.getFirst().getEntityName()).isEqualTo("it.example.Company");
    }

    @Test
    void givenRepeatedSqlWithoutLazyFetchStats_whenRulesEvaluate_thenMediumConfidenceFindingIsCreated() {
        List<NPlusOneFinding> findings = rules.evaluate(context(
                snapshot(0, 5, 0, 0, Map.of(), Map.of()),
                List.of(new SqlFingerprint("select * from audit_log where entity_id = ?", 5))
        ));

        Assertions.assertThat(findings).hasSize(1);
        Assertions.assertThat(findings.getFirst().getConfidence()).isEqualTo(NPlusOneConfidence.MEDIUM);
        Assertions.assertThat(findings.getFirst().getAssociationRole()).isNull();
    }

    @Test
    void givenOnlyHighPreparedStatementCount_whenRulesEvaluate_thenLowConfidenceFindingIsCreated() {
        List<NPlusOneFinding> findings = rules.evaluate(context(
                snapshot(0, 10, 0, 0, Map.of(), Map.of()),
                List.of()
        ));

        Assertions.assertThat(findings).hasSize(1);
        Assertions.assertThat(findings.getFirst().getConfidence()).isEqualTo(NPlusOneConfidence.LOW);
    }

    @Test
    void givenCountersBelowThreshold_whenRulesEvaluate_thenNoFindingIsCreated() {
        List<NPlusOneFinding> findings = rules.evaluate(context(
                snapshot(1, 3, 1, 1, Map.of("it.example.Company.employees", 1L), Map.of("it.example.Company", 1L)),
                List.of(new SqlFingerprint("select * from employees where fk_company = ?", 1))
        ));

        Assertions.assertThat(findings).isEmpty();
    }

    private NPlusOneDetectionContext context(TestStats stats, List<SqlFingerprint> fingerprints) {
        return new NPlusOneDetectionContext(
                new NPlusOneTestIdentifier("ExampleTest", "test"),
                stats.hibernateStatsSnapshot(),
                stats.collectionFetchCounts(),
                stats.entityFetchCounts(),
                fingerprints,
                NPlusOneThresholds.defaults()
        );
    }

    private TestStats snapshot(long queryCount, long prepareCount, long entityFetchCount, long collectionFetchCount,
                               Map<String, Long> collectionFetchCounts, Map<String, Long> entityFetchCounts) {
        return new TestStats(
                HibernateStatsSnapshot.builder()
                        .queryExecutionCount(queryCount)
                        .prepareStatementCount(prepareCount)
                        .entityFetchCount(entityFetchCount)
                        .collectionFetchCount(collectionFetchCount)
                        .build(),
                collectionFetchCounts,
                entityFetchCounts
        );
    }

    private record TestStats(
            HibernateStatsSnapshot hibernateStatsSnapshot,
            Map<String, Long> collectionFetchCounts,
            Map<String, Long> entityFetchCounts
    ) {
    }
}
