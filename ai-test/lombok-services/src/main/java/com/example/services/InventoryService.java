package com.example.services;

/**
 * Records inventory operations. Manual DI constructor — refactor
 * target.
 */
public class InventoryService {

    private final OrderService orders;
    private final AuditService audit;
    private final Clock clock;

    public InventoryService(OrderService orders, AuditService audit, Clock clock) {
        this.orders = orders;
        this.audit = audit;
        this.clock = clock;
    }

    public String adjust(String item, int delta) {
        String stamp = audit.stamp("inventory:" + item + ":" + delta);
        String trace = orders.place("system", "adjust:" + item);
        return stamp + " @ " + clock.now() + " — " + trace;
    }
}
