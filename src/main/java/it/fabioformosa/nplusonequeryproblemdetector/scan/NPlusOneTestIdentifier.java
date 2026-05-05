package it.fabioformosa.nplusonequeryproblemdetector.scan;

public record NPlusOneTestIdentifier(String className, String methodName) {

    public String displayName() {
        return className + "." + methodName;
    }
}
