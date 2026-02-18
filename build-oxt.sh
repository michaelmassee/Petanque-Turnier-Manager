#!/bin/bash
# Build script that temporarily disables the global init.gradle
# which redirects all Maven repository access to an unreachable Artifactory server

set -e

GLOBAL_INIT=~/.gradle/init.gradle
BACKUP_INIT=~/.gradle/init.gradle.backup

# Backup global init.gradle if it exists
if [ -f "$GLOBAL_INIT" ]; then
    echo "Temporarily disabling global init.gradle..."
    mv "$GLOBAL_INIT" "$BACKUP_INIT"
    RESTORE_NEEDED=true
fi

# Function to restore init.gradle on exit
cleanup() {
    if [ "$RESTORE_NEEDED" = true ]; then
        echo "Restoring global init.gradle..."
        mv "$BACKUP_INIT" "$GLOBAL_INIT"
    fi
}

# Register cleanup function
trap cleanup EXIT INT TERM

# Run the build
echo "Building OXT extension..."
./gradlew buildOXT

echo "Build completed successfully!"
