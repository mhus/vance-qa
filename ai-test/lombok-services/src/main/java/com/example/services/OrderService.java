package com.example.services;

/**
 * Records orders. Manual DI constructor — refactor target.
 */
public class OrderService {

    private final UserService users;
    private final AuditService audit;

    public OrderService(UserService users, AuditService audit) {
        this.users = users;
        this.audit = audit;
    }

    public String place(String user, String item) {
        String greeting = users.greet(user);
        return audit.stamp("order:" + item) + " for " + greeting;
    }
}
