package it.fabioformosa.nplusonequeryproblemdetector.junitextension;

import it.fabioformosa.nplusonequeryproblemdetector.engine.NPlusOneQueryProblemDetector;

public final class NPlusOneQueryDetectorContext {

    private static final ThreadLocal<NPlusOneQueryProblemDetector> CURRENT_DETECTOR = new ThreadLocal<>();

    private NPlusOneQueryDetectorContext() {
        throw new IllegalStateException("Utility class");
    }

    public static void restart() {
        NPlusOneQueryProblemDetector detector = CURRENT_DETECTOR.get();
        if (detector == null) {
            throw new IllegalStateException("NPlusOneQueryMonitoring.restart() can be used only in a test executed with NPlusOneQueryProblemTestDetector");
        }
        detector.startMonitoring();
    }

    static void setCurrentDetector(NPlusOneQueryProblemDetector detector) {
        CURRENT_DETECTOR.set(detector);
    }

    static void clear() {
        CURRENT_DETECTOR.remove();
    }
}
