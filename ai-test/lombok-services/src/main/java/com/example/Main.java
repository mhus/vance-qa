package com.example;

import com.example.services.AuditService;
import com.example.services.Clock;
import com.example.services.InventoryService;
import com.example.services.OrderService;
import com.example.services.UserService;

/**
 * Wires the services together. NOT a refactor target — only the
 * {@code services/*Service.java} classes are. Manual instantiation
 * here is fine because it's the composition root.
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        Clock clock = new Clock();
        AuditService audit = new AuditService(clock);
        UserService users = new UserService(audit, clock);
        OrderService orders = new OrderService(users, audit);
        InventoryService inventory = new InventoryService(orders, audit, clock);

        System.out.println(inventory.adjust("widget", 5));
    }
}
