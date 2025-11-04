#!/bin/bash

# Ensure script is run from the project root
cd "$(dirname "$0")"

# Check if keystore properties exist
if [ ! -f "keystore.properties" ]; then
    echo "Error: keystore.properties not found"
    exit 1
fi

# Prompt for keystore password
read -sp "Enter keystore password: " KEYSTORE_PASSWORD
echo

# Export the password for Gradle to use
export ORG_GRADLE_PROJECT_RELEASE_STORE_PASSWORD="$KEYSTORE_PASSWORD"
export ORG_GRADLE_PROJECT_RELEASE_KEY_PASSWORD="$KEYSTORE_PASSWORD"

# Build signed release APK
./gradlew assembleRelease

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "Signed APK built successfully!"
    echo "APK location: app/build/outputs/apk/release/app-release.apk"
    
    # Optional: Calculate SHA256 checksum for verification
    echo "SHA256 Checksum:"
    shasum -a 256 app/build/outputs/apk/release/app-release.apk
else
    echo "APK build failed"
    exit 1
fi 