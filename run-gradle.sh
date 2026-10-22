#!/bin/bash

# =============================================================================
# run-gradle.sh - Run Gradle project on Ubuntu
# Project: GDSEMR_ver_1.1001 (Java 25 + JavaFX 25)
# Location: ~/git/GDSEMR_ver_1.1001
# =============================================================================

set -euo pipefail  # Exit on error, unset vars, pipe failures

# --- Configuration ---
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GRADLE_WRAPPER="./gradlew"
LOG_FILE="$PROJECT_DIR/run.log"
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
# export ORG_GRADLE_JAVA_HOME=/path/to/jdk-25 # uncomment to force a specific JDK 25

# Ensure DISPLAY is available for JavaFX under Wayland/terminal sessions
if [[ -z "${DISPLAY:-}" ]]; then
    if [[ -S /tmp/.X11-unix/X0 ]]; then
        export DISPLAY=:0
    elif [[ -S /tmp/.X11-unix/X1 ]]; then
        export DISPLAY=:1
    fi
fi

# --- Colors for output ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# --- Functions ---
log() {
    echo -e "${GREEN}[INFO]${NC} $1" | tee -a "$LOG_FILE"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2 | tee -a "$LOG_FILE"
}

# --- Main ---
main() {
    echo "===========================================================" | tee -a "$LOG_FILE"
    echo "Gradle Project Runner - $TIMESTAMP" | tee -a "$LOG_FILE"
    echo "Project: $PROJECT_DIR" | tee -a "$LOG_FILE"
    echo "===========================================================" | tee -a "$LOG_FILE"

    # Check if in correct directory
    if [[ ! -f "$PROJECT_DIR/settings.gradle" && ! -f "$PROJECT_DIR/settings.gradle.kts" ]]; then
        error "Not in a Gradle project root! Missing settings.gradle or settings.gradle.kts"
        error "Expected location: $PROJECT_DIR"
        exit 1
    fi

    cd "$PROJECT_DIR" || { error "Failed to cd into $PROJECT_DIR"; exit 1; }

    # Ensure gradlew is executable
    if [[ ! -x "$GRADLE_WRAPPER" ]]; then
        log "Making gradlew executable..."
        chmod +x "$GRADLE_WRAPPER" || { error "Failed to chmod +x gradlew"; exit 1; }
    fi

    # Optional: Clean before build (uncomment if needed)
    # log "Cleaning previous builds..."
    # "$GRADLE_WRAPPER" clean >> "$LOG_FILE" 2>&1

    # Run provided tasks if passed, otherwise choose a sensible default.
    log "Starting Gradle build and run..."
    log "Check full log at: $LOG_FILE"

    if [[ "$#" -gt 0 ]]; then
        TASKS=("$@")
    else
        # Detect if it's a Spring Boot app and use bootRun, else use 'run'
        if grep -q "org.springframework.boot" build.gradle 2>/dev/null || \
           grep -q "id 'org.springframework.boot'" build.gradle.kts 2>/dev/null; then
            TASKS=("bootRun")
        else
            TASKS=("run")
        fi
    fi

    log "Executing: $GRADLE_WRAPPER ${TASKS[*]}"

    if "$GRADLE_WRAPPER" "${TASKS[@]}"; then
        log "Gradle task '${TASKS[*]}' completed successfully!"
    else
        error "Gradle task '${TASKS[*]}' failed. See log above."
        exit 1
    fi
}

# --- Execute ---
{
    main "$@"
} 2>&1 | tee -a "$LOG_FILE"

echo
echo "Done. Full log saved to: $LOG_FILE"
