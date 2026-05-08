package com.example.services;

/**
 * Records user actions. Manual DI constructor — refactor target.
 */
public class UserService {

    private final AuditService audit;
    private final Clock clock;

    public UserService(AuditService audit, Clock clock) {
        this.audit = audit;
        this.clock = clock;
    }

    public String greet(String name) {
        String now = clock.now().toString();
        return audit.stamp("greet:" + name) + " (now=" + now + ")";
    }
}
