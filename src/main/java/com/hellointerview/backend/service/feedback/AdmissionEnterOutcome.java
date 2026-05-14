package com.hellointerview.backend.service.feedback;

/**
 * Result of Strategy B admission: either no gate applied, a slot was acquired, or the request was rejected.
 */
public record AdmissionEnterOutcome(
        boolean mayProceed,
        String semaphoreKey,
        Integer retryAfterSecondsIfRejected
) {
    public static AdmissionEnterOutcome bypass() {
        return new AdmissionEnterOutcome(true, null, null);
    }

    public static AdmissionEnterOutcome rejected(int retryAfterSeconds) {
        return new AdmissionEnterOutcome(false, null, retryAfterSeconds);
    }

    public static AdmissionEnterOutcome admitted(AdmissionWorkloadKey key) {
        return new AdmissionEnterOutcome(true, key.compositeKey(), null);
    }

    public boolean mustReleaseSemaphore() {
        return semaphoreKey != null;
    }
}
