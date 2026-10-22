# App Module Instructions (`app/`)

## Module Scope & Environment
The `app` module contains the entire desktop EMR application.
- **Main Class**: `com.emr.gds.IttiaApp` (configured in `app/build.gradle.kts`).
- **Runtime Environment**:
  - Host OS: Ubuntu 26.04.1 LTS (Kernel 7.0.0-38-generic, x86_64).
  - VM Platform: VMware Virtual Platform with VMware SVGA II Adapter (16GB RAM).
  - Java Toolchain: JDK 25 with JavaFX 25 (`javafx.controls`, `javafx.fxml`, `javafx.swing`).
  - Key Dependencies: SQLite JDBC (`org.xerial:sqlite-jdbc`), Jackson Databind, SLF4J, JUnit 5, TestFX (`org.testfx:testfx-core`, `org.testfx:testfx-junit5`).

---

## Hexagonal Feature Architecture
All clinical feature packages live under `com.emr.gds.features.<name>/` and follow hexagonal (ports-and-adapters) architecture:

```
com.emr.gds.features.<feature>/
├── domain/                      # Pure Java: entities, value objects, domain calculators, repository interfaces
├── application/                 # Pure services, report formatters, business orchestration (optional)
├── adapter/
│   ├── in/
│   │   └── ui/                  # JavaFX Controllers, Panes, FXML Controllers, Launchers
│   └── out/
│       └── persistence/         # SQLite/JDBC repository implementations (if persisted)
```

### Layer Constraints
1. **`domain/`**:
   - Must NOT depend on JavaFX (`javafx.*`) or JDBC (`java.sql.*`).
   - *Exception*: `features/allergy` uses JavaFX property wrappers (`StringProperty`, `BooleanProperty`) on entities for `TableView` bindings.
   - Exposes repository interfaces (e.g., `HistoryRepository`, `ClinicalLabRepository`) when persistence is needed.
2. **`application/`**:
   - Depends only on `domain/`.
   - Contains pure business logic and calculation/formatting routines (e.g., `ThyroidSummaryService`).
   - Must be unit-testable without a running JavaFX runtime or database.
3. **`adapter/in/ui/`**:
   - Handles JavaFX views, dialogs, and FXML controllers.
   - Depends on `domain/` and `application/`, never the inverse.
4. **`adapter/out/persistence/`**:
   - Implements domain repository interfaces using SQLite/JDBC.
   - Depends on `domain/`.

---

## SQLite Database & Persistence Rules
- **Database Files**: Seed databases are stored in `app/db/*.db`:
  - `auth.db`, `abbreviations.db`, `ClinicalLabItemsSqlite3.db`, `emr_templates.db`, `history.db`, `med_data.db`, `plan_history.db`, `prolist.db`, `references.db`.
- **Path Resolution**:
  - **Always** resolve SQLite paths using `com.emr.gds.core.db.DbPaths.resolve("<filename>.db")`.
  - `DbPaths` locates the project root (`gradlew` or `.git`) and resolves to `app/db/<filename>.db` safely regardless of current working directory.
  - **Never** hardcode relative path strings like `"app/db/..."` or `"db/..."`.
- **Classpath Database Exception (`kcd`)**:
  - `features/kcd` uses `src/main/resources/database/kcd_database.db` bundled inside the JAR/resources.
  - Do NOT route KCD to `DbPaths`. Its JDBC connection URL is defined in `JdbcKcdRepository`.
- **Connection Lifecycle**:
  - Open and close connections per query/transaction using `try-with-resources` (`DriverManager.getConnection(...)`).
  - Do NOT maintain long-lived open connections in feature repositories.
  - Cross-cutting shared connections are strictly managed by `AppDatabaseManager` for core services only.
- **Test Isolation**:
  - Tests must never mutate files in `app/db/`.
  - Use temporary files (e.g., `Files.createTempFile("test_", ".db")`) or in-memory SQLite (`jdbc:sqlite::memory:`) in tests.

---

## JavaFX & FXML Guidelines
- **FXML Controller Synchronization**:
  - Moving or renaming an FXML controller requires updating the `fx:controller` attribute in the corresponding `.fxml` file in `src/main/resources/`.
  - FXML binding mismatches only fail at runtime during `FXMLLoader.load()`; `./gradlew compileJava` will NOT detect missing or mismatched controllers.
- **JavaFX Thread Concurrency**:
  - Background tasks and database queries must run off the JavaFX Application Thread.
  - UI mutations must be dispatched via `Platform.runLater(Runnable)`.
- **UI Verification Gotchas**:
  - Do not use `Control.getChildrenUnmodifiable()` to verify rendered data (it can return empty before the first CSS/layout pass).
  - Verify through control model properties directly (e.g., `ListView.getItems()`, `TitledPane.getContent()`, `Accordion.getPanes()`).

---

## Testing Guidelines (TestFX & JUnit 5 in VMware/Ubuntu Environment)
- **Synthetic OS Events Issue**:
  - On Ubuntu 26.04 within VMware (using VMware SVGA II Adapter and desktop portal), TestFX OS-level input methods (`FxRobot.clickOn()`, `write()`, `type()`) fail with `java.lang.UnsupportedOperationException at GtkApplication.java`.
  - **Solution**: Always execute UI interactions via `robot.interact(Runnable)` or `Platform.runLater(Runnable)` directly on the JavaFX Application Thread:
    ```java
    robot.interact(() -> {
        button.fire();
        textField.setText("search query");
    });
    ```
- **Naming**: Tests should be named after behavior (`doesFilterResultsWhenQueryEntered`, `rejectsInvalidInput`).
- **Running Specific Tests**:
  ```bash
  ./gradlew :app:test --tests "com.emr.gds.features.clinicalLab.*"
  ```

---

## Authentication Layer (`com.emr.gds.core.auth`)
- `PasswordHasher`: PBKDF2WithHmacSHA256 with 120,000 iterations (JDK standard library only, no external security dependencies).
- `SqliteCredentialRepository`: Stores salt and hash in `app/db/auth.db`.
- Designed as a single-clinician desktop password system:
  - First run (no credential in DB): Prompts for password creation & confirmation (min 6 characters).
  - Subsequent runs: Verifies entered password against stored PBKDF2 hash.
