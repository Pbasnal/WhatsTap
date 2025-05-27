#!/bin/bash

# Script to check if all prerequisites for building signed APKs are available

echo "🔍 Checking Build Prerequisites"
echo "==============================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo_check() {
    echo -e "${BLUE}🔍 Checking $1...${NC}"
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

# Track overall status
ALL_GOOD=true

# Check Java
echo_check "Java JDK"
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    echo_success "Java found: $JAVA_VERSION"
else
    echo_error "Java not found! Please install JDK 8 or higher."
    ALL_GOOD=false
fi

# Check keytool
echo_check "keytool (for keystore generation)"
if command -v keytool &> /dev/null; then
    echo_success "keytool found"
else
    echo_error "keytool not found! It should come with Java JDK."
    ALL_GOOD=false
fi

# Check Gradle wrapper
echo_check "Gradle wrapper"
if [ -f "./gradlew" ]; then
    if [ -x "./gradlew" ]; then
        echo_success "gradlew found and executable"
    else
        echo_warning "gradlew found but not executable. Run: chmod +x gradlew"
        ALL_GOOD=false
    fi
else
    echo_error "gradlew not found! Make sure you're in the Android project root."
    ALL_GOOD=false
fi

# Check Android project structure
echo_check "Android project structure"
if [ -f "app/build.gradle" ] || [ -f "app/build.gradle.kts" ]; then
    echo_success "Android project structure found"
else
    echo_error "Android project structure not found! Make sure you're in the project root."
    ALL_GOOD=false
fi

# Check Android SDK (optional but recommended)
echo_check "Android SDK"
if [ -n "$ANDROID_HOME" ] || [ -n "$ANDROID_SDK_ROOT" ]; then
    SDK_PATH="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
    echo_success "Android SDK found at: $SDK_PATH"
else
    echo_warning "Android SDK environment variables not set (ANDROID_HOME or ANDROID_SDK_ROOT)"
    echo "         This might be okay if Android Studio is properly configured."
fi

# Check aapt (for APK info)
echo_check "aapt (for APK information)"
if command -v aapt &> /dev/null; then
    echo_success "aapt found"
else
    echo_warning "aapt not found. APK information won't be displayed after build."
    echo "         This is optional and doesn't affect the build process."
fi

# Check if build scripts exist
echo_check "Build scripts"
if [ -f "generate_keystore.sh" ] && [ -f "build_signed_apk.sh" ]; then
    if [ -x "generate_keystore.sh" ] && [ -x "build_signed_apk.sh" ]; then
        echo_success "Build scripts found and executable"
    else
        echo_warning "Build scripts found but not executable. Run: chmod +x *.sh"
    fi
else
    echo_error "Build scripts not found!"
    ALL_GOOD=false
fi

echo ""
echo "==============================="

if [ "$ALL_GOOD" = true ]; then
    echo_success "All prerequisites are satisfied! 🎉"
    echo ""
    echo "You can now:"
    echo "1. Generate a keystore: ./generate_keystore.sh"
    echo "2. Build signed APK: ./build_signed_apk.sh"
else
    echo_error "Some prerequisites are missing or need attention."
    echo ""
    echo "Please fix the issues above before building."
fi

echo ""
echo "📚 For detailed instructions, see: BUILD_INSTRUCTIONS.md" 