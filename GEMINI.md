# GDSEMR Project Instructions

## Project Overview
GDSEMR (`MigoJJ-GDSEMR_ver_1.2001` / Ittia EMR) is a clinical Electronic Medical Record (EMR) desktop application built with **JavaFX 25** targeting **Java 25**. It provides outpatient management tools across ~15 clinical domains (thyroid, medication, KCD coding, allergies, clinical labs, vaccines, gout, GLP-1, etc.).

## Subdirectory Instructions
Detailed domain-specific guidance is organized in subdirectories:
- **`app/GEMINI.md`**: Architecture, Hexagonal package conventions, SQLite persistence (`DbPaths`), JavaFX/FXML guidelines, and TestFX UI testing rules.
- **`docs/GEMINI.md`**: Guidelines for maintaining project documentation, architecture logs, and clinical references.

---

## Environment & Toolchains
- **Host OS**: Ubuntu 26.04.1 LTS (Kernel 7.0.0-38-generic, x86_64).
- **Environment**: VMware Virtual Platform (VMware SVGA II Adapter, 16GB RAM, 2 threads).
- **Node.js**: v22.23.1.
- **Java**: Java 25 (`javaVersion=25` in `gradle.properties`).
- **JavaFX**: OpenJFX 25 (`javafxVersion=25.0.4` in `gradle.properties`).
- **Build Tool**: Gradle 9+ using Kotlin DSL (`build.gradle.kts`).
- **JDK Location**: Configured via `org.gradle.java.installations.paths` in `gradle.properties` (or override with `export ORG_GRADLE_JAVA_HOME=/path/to/jdk-25`).
- **Launch Options**: Process launch mechanism uses `-Djdk.lang.Process.launchMechanism=FORK` in `gradle.properties` to avoid environment permissions issues.
- **Virtual Display / UI Note**: On VMware SVGA II without native OS pointer injection privileges, TestFX robot synthetic OS events fail with `UnsupportedOperationException at GtkApplication`. All UI automation must use `robot.interact(Runnable)` directly on the JavaFX Application Thread.

---

## Build, Run & Test Commands
Always use the Gradle wrapper (`./gradlew`) or `./run-gradle.sh`:

- **Compile All**:
  ```bash
  ./gradlew compileJava compileTestJava
  ```
- **Run Application**:
  ```bash
  ./gradlew run
  # or specifically:
  ./gradlew :app:run
  ```
- **Run Tests**:
  ```bash
  ./gradlew test
  # Run a specific test class:
  ./gradlew :app:test --tests "com.emr.gds.features.medication.MedicationDatabaseManagerTest"
  ```
- **Clean Build**:
  ```bash
  ./gradlew clean
  ```

---

## Architectural Principles & Strict Rules
1. **Single Module**:
   - The project is strictly single-module (`app`).
   - Legacy modules (`core`, `server`, `list`, `utilities`) were removed in prior cleanup phases. Never reintroduce them or write network/HTTP client skeletons without explicit architectural design.
2. **Hexagonal Architecture (Ports & Adapters)**:
   - All clinical features live in `app/src/main/java/com/emr/gds/features/<feature>/` and strictly adhere to hexagonal layering:
     - `domain/`: Pure Java POJOs, calculation rules, repository interfaces. No JavaFX or JDBC dependencies.
     - `application/`: Pure business logic, report generators, orchestration.
     - `adapter/in/ui/`: JavaFX controllers, stages, FXML controllers, UI panes.
     - `adapter/out/persistence/`: SQLite JDBC repository implementations.
3. **Database & Persistence**:
   - SQLite databases reside in `app/db/*.db`.
   - Always resolve database paths through `com.emr.gds.core.db.DbPaths.resolve("<name>.db")`. Never hardcode relative file paths.
   - Exception: `features/kcd` uses a bundled classpath database at `src/main/resources/database/kcd_database.db`.
   - Never write test data to the live seed databases in `app/db/`.
4. **FXML Runtime Binding**:
   - Renaming or moving any FXML controller class requires updating the `fx:controller` attribute in its corresponding `.fxml` file under `app/src/main/resources/`. Gradle compilation does NOT validate FXML controller bindings.
5. **UI Testing with TestFX**:
   - In Linux/portal environments, synthetic OS-level events (`FxRobot.clickOn()`, `type()`) fail due to desktop portal restrictions.
   - Always execute UI interactions via `FxRobot.interact(Runnable)` or `Platform.runLater(Runnable)` directly on the JavaFX Application Thread.

---

## Coding Style & Git Conventions
- **Indentation**: 4 spaces, no tabs.
- **Naming**: Standard Java conventions (`PascalCase` for classes, `camelCase` for methods/variables, `UPPER_SNAKE_CASE` for constants). Package names lower-case dot-separated (`com.emr.gds.*`).
- **Commits**: Clear, imperative messages scoped by feature (e.g., `feat(medication): add allergy warning check`, `fix(auth): correct credential verification timeout`).
- **Safety**: Do not commit build artifacts (`build/`, `bin/`), temporary databases, or lock files (`.~lock.*`).
