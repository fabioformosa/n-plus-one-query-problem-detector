package it.fabioformosa.nplusonequeryproblemdetector.scan;

import java.util.List;

public final class NPlusOneScanFailurePolicy {

    private NPlusOneScanFailurePolicy() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean shouldFail(NPlusOneScanProperties properties, List<NPlusOneFinding> findings) {
        if (!properties.failOnDetected()) {
            return false;
        }
        return findings.stream()
                .filter(finding -> !finding.isExcluded())
                .anyMatch(finding -> finding.getConfidence().ordinal() <= properties.failOnConfidence().ordinal());
    }
}
