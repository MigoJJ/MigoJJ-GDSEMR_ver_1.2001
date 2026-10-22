# Documentation Guidelines (`docs/`)

## Scope
The `docs/` directory maintains project-wide architectural decisions, clinical feature guides, and agent guidelines.

---

## Key Documents & Maintenance
- **`architecture.md`**:
  - The canonical architectural reference for GDSEMR.
  - Documents module topology, hexagonal layering across all 15 clinical features, SQLite connection models, `DbPaths` resolution, authentication design, and TestFX UI testing notes.
  - **Rule**: Whenever a new clinical feature is introduced, persistence logic is modified, or auth behavior changes, update `architecture.md` accordingly.
- **`AGENTS.md`**:
  - Guidelines and commands for AI coding assistants working in this repository.
  - Kept aligned with `GEMINI.md`.
- **Clinical Guides** (e.g., `gout-calculator.md`):
  - Domain-specific documentation explaining clinical algorithms, scoring criteria, and reference values used in the application.

---

## Clinical Reference Materials
- Seed documents and guidelines live in `app/db/references/` (e.g., AMC guidelines, KCD coding sheets, drug info).
- Any modifications to clinical calculation criteria should reference relevant medical guidelines and be documented in corresponding markdown files in `docs/`.
