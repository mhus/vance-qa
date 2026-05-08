# Lombok Services Refactor Target

Tiny Java project used by ai-test as a refactoring target. Services
under `src/main/java/com/example/services/` all carry **manually
written DI constructors** — the kind that should be replaced with
Lombok's `@RequiredArgsConstructor`.

## Stack

- Java 21
- Maven (no parent, no Spring)
- Lombok 1.18 (annotation processor configured in `pom.xml`)

## Build

```bash
mvn compile
```

Should pass cleanly out of the box. After Arthur's refactor it must
still pass — no behavior change, only DI-boilerplate removal.

## The smell

Every service in `services/` looks like this:

```java
public class FooService {
    private final BarService bar;
    private final BazService baz;

    public FooService(BarService bar, BazService baz) {
        this.bar = bar;
        this.baz = baz;
    }
    // ... methods ...
}
```

Refactor target:

```java
@RequiredArgsConstructor
public class FooService {
    private final BarService bar;
    private final BazService baz;
    // ... methods ...
}
```

Plus the matching `import lombok.RequiredArgsConstructor;`.

## Done = all four services refactored + `mvn compile` passes
