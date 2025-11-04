# WhatsTap2 APK Distribution Guide

## Prerequisites

1. Android device running Android 8.0 (Oreo) or higher
2. Ability to install apps from unknown sources

## Installation Steps

### Option 1: Direct APK Transfer
1. Transfer the `app-release.apk` to your Android device
2. Tap on the APK file to begin installation
3. If prompted, allow installation from unknown sources
4. Follow on-screen instructions to complete installation

### Option 2: ADB Installation
If you're comfortable with command line:
```bash
adb install app-release.apk
```

## Verification

- **Minimum Android Version**: 8.0 (Oreo)
- **App Version**: 1.0
- **Package Name**: com.originb.whatstap2

## Security Notes
- This APK is signed with a release key
- Verify the SHA256 checksum matches the one provided during distribution

## Troubleshooting
- If installation fails, ensure:
  1. Your device allows unknown source installations
  2. You have sufficient storage
  3. The APK is not corrupted during transfer

## Permissions Required
- Contacts access
- Phone call initiation
- WhatsApp integration

## Support
For issues, contact the developer directly.
