#!/bin/bash

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# APK Path
APK_PATH="app/build/outputs/apk/release/app-release.apk"

# Check if APK exists
if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK not found. Please build the app first using ./build_signed_apk.sh"
    exit 1
fi

# Calculate checksum
echo "🔐 Calculating SHA256 Checksum:"
CHECKSUM=$(shasum -a 256 "$APK_PATH" | awk '{print $1}')
echo "${GREEN}$CHECKSUM${NC}"

# File size
SIZE=$(du -h "$APK_PATH" | cut -f1)
echo "📦 APK Size: ${YELLOW}$SIZE${NC}"

# Optional: Copy to project root for easier sharing
cp "$APK_PATH" WhatsTap2-release.apk

echo "
📱 WhatsTap2 APK Ready for Distribution

Sharing Instructions:
1. Transfer WhatsTap2-release.apk to your friends
2. Share the SHA256 Checksum for verification:
   ${GREEN}$CHECKSUM${NC}

Security Tips:
- Verify the checksum matches
- Ensure installation from trusted sources
- Check app permissions before installing
"

# Optional: Open file location
if [[ "$OSTYPE" == "darwin"* ]]; then
    open .
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    xdg-open .
fi
