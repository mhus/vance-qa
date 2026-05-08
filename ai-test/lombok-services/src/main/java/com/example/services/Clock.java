package com.example.services;

import java.time.Instant;

/**
 * Tiny dependency that the services depend on. Has no dependencies
 * itself so DI graph terminates here. NOT a refactor target.
 */
public class Clock {

    public Instant now() {
        return Instant.now();
    }
}
