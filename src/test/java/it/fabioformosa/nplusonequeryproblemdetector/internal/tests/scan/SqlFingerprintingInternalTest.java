package it.fabioformosa.nplusonequeryproblemdetector.internal.tests.scan;

import it.fabioformosa.nplusonequeryproblemdetector.scan.SqlFingerprint;
import it.fabioformosa.nplusonequeryproblemdetector.scan.SqlFingerprinting;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class SqlFingerprintingInternalTest {

    @Test
    void givenSameSelectWithDifferentLiteralValues_whenFingerprinting_thenStatementsAreGroupedTogether() {
        List<SqlFingerprint> fingerprints = SqlFingerprinting.repeatedSelectFingerprints(List.of(
                "select e.id, e.name from employees e where e.fk_company = 1",
                "select e.id, e.name from employees e where e.fk_company = 2",
                "select e.id, e.name from employees e where e.fk_company = 3",
                "insert into audit_log(id) values (1)"
        ));

        Assertions.assertThat(fingerprints).containsExactly(
                new SqlFingerprint("select e.id, e.name from employees e where e.fk_company = ?", 3)
        );
    }
}
