#!/bin/bash

# Script to build signed APK for WhatsTap2
# This script builds a release APK and signs it with your keystore

echo "📱 WhatsTap2 Signed APK Builder"
echo "==============================="

# Configuration
KEYSTORE_PROPERTIES="keystore.properties"
BUILD_TYPE="release"
OUTPUT_DIR="app/build/outputs/apk/release"
FINAL_APK_NAME="WhatsTap2-signed.apk"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

echo_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

echo_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

echo_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check if we're in the right directory
if [ ! -f "app/build.gradle" ] && [ ! -f "app/build.gradle.kts" ]; then
    echo_error "This script must be run from the root of your Android project!"
    echo_info "Make sure you're in the directory containing the 'app' folder."
    exit 1
fi

# Check if keystore.properties exists
if [ ! -f "$KEYSTORE_PROPERTIES" ]; then
    echo_error "Keystore properties file not found: $KEYSTORE_PROPERTIES"
    echo_info "Please run './generate_keystore.sh' first to create a signing key."
    exit 1
fi

# Read keystore configuration
echo_info "Reading keystore configuration..."
source "$KEYSTORE_PROPERTIES"

# Check if keystore file exists
if [ ! -f "$storeFile" ]; then
    echo_error "Keystore file not found: $storeFile"
    echo_info "Please run './generate_keystore.sh' to create a signing key."
    exit 1
fi

# Get passwords securely
echo ""
echo_info "Please provide signing credentials:"
read -s -p "🔐 Enter keystore password: " KEYSTORE_PASSWORD
echo
read -s -p "🔑 Enter key password: " KEY_PASSWORD
echo

# Validate passwords are not empty
if [ -z "$KEYSTORE_PASSWORD" ] || [ -z "$KEY_PASSWORD" ]; then
    echo_error "Passwords cannot be empty!"
    exit 1
fi

echo ""
echo_info "Starting build process..."

# Clean previous builds
echo_info "Cleaning previous builds..."
./gradlew clean

if [ $? -ne 0 ]; then
    echo_error "Clean failed!"
    exit 1
fi

# Build the release APK
echo_info "Building release APK..."
./gradlew assembleRelease \
    -Pandroid.injected.signing.store.file="$storeFile" \
    -Pandroid.injected.signing.store.password="$KEYSTORE_PASSWORD" \
    -Pandroid.injected.signing.key.alias="$keyAlias" \
    -Pandroid.injected.signing.key.password="$KEY_PASSWORD"

if [ $? -ne 0 ]; then
    echo_error "Build failed!"
    echo_info "Please check the error messages above and try again."
    exit 1
fi

# Check if APK was created
RELEASE_APK="$OUTPUT_DIR/app-release.apk"
if [ ! -f "$RELEASE_APK" ]; then
    echo_error "Release APK not found at: $RELEASE_APK"
    exit 1
fi

# Copy and rename the APK
echo_info "Copying APK to project root..."
cp "$RELEASE_APK" "$FINAL_APK_NAME"

if [ $? -eq 0 ]; then
    echo ""
    echo_success "Signed APK built successfully!"
    echo ""
    echo "📁 APK Location: $(pwd)/$FINAL_APK_NAME"
    echo "📊 APK Size: $(du -h "$FINAL_APK_NAME" | cut -f1)"
    echo ""
    
    # Get APK info
    echo_info "APK Information:"
    if command -v aapt &> /dev/null; then
        echo "📋 Package Info:"
        aapt dump badging "$FINAL_APK_NAME" | grep -E "(package|application-label|versionCode|versionName)" | head -4
        echo ""
    fi
    
    echo "🔐 Signing Info:"
    echo "   Keystore: $storeFile"
    echo "   Key Alias: $keyAlias"
    echo ""
    
    echo_success "Your signed APK is ready for distribution!"
    echo_info "You can install it on devices or upload it to app stores."
    echo ""
    echo_warning "Security Reminder:"
    echo "   • Keep your keystore file and passwords secure"
    echo "   • Never share your keystore or passwords"
    echo "   • Make backups of your keystore file"
    
else
    echo_error "Failed to copy APK to project root!"
    echo_info "You can find the APK at: $RELEASE_APK"
fi

# Optional: Open file location
if [[ "$OSTYPE" == "darwin"* ]]; then
    read -p "📂 Open APK location in Finder? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        open .
    fi
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    read -p "📂 Open APK location in file manager? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        xdg-open .
    fi
fi

echo ""
echo_info "Build completed!" 