# Simple Pet Clinic

Standalone Java CLI pet clinic. **Intentionally bad code** — designed as a refactoring target.

## Stack

- Java 21
- Maven (no parent, no Spring)
- Picocli 4.7.6 (CLI)
- Jackson Databind 2.18.1 (JSON persistence)
- maven-shade-plugin (fat jar)

## Build

```bash
mvn clean package
```

Produces `target/simple-petclinic.jar`.

## Run

```bash
java -jar target/simple-petclinic.jar <command> [options]
```

Data is persisted in `petclinic.json` in the current working directory. Each command reads and rewrites the entire file.

### Commands

| Command       | Options                                                                       | Description                          |
|---------------|-------------------------------------------------------------------------------|--------------------------------------|
| `add-owner`   | `--first <s>` `--last <s>` `[--address <s>]` `[--phone <s>]`                  | Add an owner                         |
| `list-owners` |                                                                               | List all owners                      |
| `show-owner`  | `--id <n>`                                                                    | Show owner with pets                 |
| `add-pet`     | `--owner-id <n>` `--name <s>` `--birth <yyyy-MM-dd>` `--type <s>`             | Add a pet to an owner                |
| `list-pets`   |                                                                               | List all pets                        |
| `add-vet`     | `--first <s>` `--last <s>` `[--spec <csv>]`                                   | Add a vet (specialties comma-sep)    |
| `list-vets`   |                                                                               | List all vets                        |
| `add-visit`   | `--pet-id <n>` `--vet-id <n>` `--date <yyyy-MM-dd>` `--desc <s>` `--cost <d>` | Add a visit                          |
| `list-visits` | `[--pet-id <n>]`                                                              | List visits (optionally per pet)     |
| `summary`     |                                                                               | Print counts and total revenue       |

### Example session

```bash
java -jar target/simple-petclinic.jar add-owner --first Alice --last Smith --phone 555-1234
java -jar target/simple-petclinic.jar add-vet   --first Greg  --last House --spec "surgery,internal"
java -jar target/simple-petclinic.jar add-pet   --owner-id 1 --name Rex --birth 2020-05-12 --type dog
java -jar target/simple-petclinic.jar add-visit --pet-id 3 --vet-id 2 --date 2026-03-10 --desc "annual checkup" --cost 89.50
java -jar target/simple-petclinic.jar summary
```

## Project Layout

```
simple-petclinic/
  pom.xml
  src/main/java/com/example/petclinic/
    PetClinicApp.java     ~400 lines, everything lives here
```

One file, ten subcommands as static inner classes, all logic inline. That is the point.

## Intentional Code Smells

This project is **deliberately badly written** so that a refactoring agent has clear targets. Below is a non-exhaustive catalogue. A real refactor should not stop at fixing one or two of these — most are tangled together.

### Structural

- **God class.** `PetClinicApp` hosts all ten subcommands as static inner classes. There is no domain layer, no service layer, no repository. CLI parsing, validation, business logic, persistence, and presentation all live in the same `run()` methods.
- **No domain model.** Owners, pets, vets, and visits are represented as `Map<String, Object>`. Casting (`(List<Map<String, Object>>) data.get("owners")`) is sprinkled across every command, with `@SuppressWarnings("unchecked")` papering over the type unsafety.
- **Mixed concerns per method.** Every `add-*` command does input validation, file I/O, lookup, mutation, more file I/O, and console output in one straight-line method.

### Type safety / primitive obsession

- **Dates as `String`.** `birthDate`, visit `date` carried as `String`, parsed inline via `new SimpleDateFormat("yyyy-MM-dd").parse(...)` repeatedly. Format string duplicated in three places.
- **Money as `double`.** `cost` is a `double`. Output formatting via `printf` without `Locale.ROOT`, so on a German JVM the decimal separator becomes `,` (e.g. `89,50`) — a latent locale bug.
- **IDs as `int`.** No type distinction between an owner id, a pet id, a vet id, a visit id. Easy to pass the wrong one.
- **Stringly-typed enums.** Pet `type` is a free-form `String` (`"dog"`, `"cat"`, `"DOG"`, …) — no canonicalisation, no enum.

### Duplication

- **Validation.** Every `add-*` command repeats the `if (x == null || x.trim().isEmpty()) { sysout(ERROR) ; System.exit(1); }` pattern.
- **Existence-check loops.** `AddPet`, `AddVisit`, `ShowOwner` all hand-roll the same "iterate the list, compare ids, set a found flag" pattern.
- **Table printing.** Each `list-*` command formats its own table header, separator, and row format — six near-identical printf blocks.
- **Persistence boilerplate.** Every command opens `loadData()`, casts the relevant list out of the map, and calls `saveData()`. There is no abstraction over this.

### Magic values

- **Hardcoded file path.** `"petclinic.json"` lives as a `public static final` constant, but the format strings, map keys, and error messages around it are scattered as raw literals.
- **Map keys as strings.** `"owners"`, `"pets"`, `"firstName"`, `"birthDate"`, … no constants, no schema, no compile-time check.
- **Error messages.** Free-form strings, no error codes, no localization.

### Error handling

- **`System.exit(1)` from inside command methods.** Spread across every command. Makes the code untestable in-process and crashes the JVM on the first error rather than letting Picocli aggregate.
- **No exception types.** All errors are reported by printing to `System.out` (not `System.err`) and exiting.
- **Silent date-parse swallow.** `catch (Exception e)` discards the exception and prints a generic message.

### Persistence

- **Full read / full rewrite per command.** Even adding one pet rewrites the entire `petclinic.json`. No transaction, no append, no concurrency story.
- **Schema implicit in code.** The shape of `petclinic.json` is only defined by what `loadData()` writes for an empty file. Reading older files that drift from this shape throws an unchecked cast somewhere downstream.
- **`nextId` as a single global counter.** Owner ids and pet ids share one sequence — owner #1 then pet #2 then pet #3 then vet #4. Confusing in `list-pets` output where the "Owner" column is an id from the same space.

## Suggested refactoring directions

Not prescriptive — these are the kinds of moves a refactoring agent should be able to make:

1. Introduce domain classes (`Owner`, `Pet`, `Vet`, `Visit`) with proper encapsulation.
2. Extract a `Repository` (or per-aggregate repositories) hiding Jackson and the file I/O.
3. Replace `String` dates with `java.time.LocalDate`, parsed once at the CLI boundary.
4. Replace `double` cost with `BigDecimal` (or a `Money` value object) and format via `Locale.ROOT`.
5. Extract validation into reusable helpers or per-command Picocli validators.
6. Replace `System.exit` calls with thrown `CommandException` (or similar), caught by a Picocli `IExecutionExceptionHandler`.
7. Consolidate table rendering into a `TablePrinter`.
8. Split each subcommand into its own file.
9. Type-safe IDs (e.g. `record OwnerId(int value) {}` etc.) — optional, depending on how far the refactor goes.
10. Per-aggregate id sequences (or UUIDs) instead of one global counter.
