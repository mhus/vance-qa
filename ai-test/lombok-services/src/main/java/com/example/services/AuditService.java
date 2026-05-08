package com.example.services;

/**
 * Records audit events. Manual DI constructor — refactor target.
 */
public class AuditService {

    private final Clock clock;

    public AuditService(Clock clock) {
        this.clock = clock;
    }

    public String stamp(String event) {
        return clock.now().toString() + " " + event;
    }
}
