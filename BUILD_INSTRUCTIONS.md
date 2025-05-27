# WhatsTap2 Build Instructions

This document explains how to build signed APK files for the WhatsTap2 Android app.

## Prerequisites

- **Java Development Kit (JDK)**: Make sure you have JDK 8 or higher installed
- **Android SDK**: Android Studio should be installed with SDK tools
- **Gradle**: Should be available through the project's `gradlew` wrapper

## Quick Start

### 1. Generate Signing Key (First Time Only)

```bash
./generate_keystore.sh
```

This script will:
- Create a new keystore file for signing your APK
- Ask for certificate information (name, organization, etc.)
- Generate a `keystore.properties` file with configuration
- Create a keystore valid for ~27 years

**Important**: Keep your keystore file and passwords safe! You'll need them to update your app.

### 2. Build Signed APK

```bash
./build_signed_apk.sh
```

This script will:
- Clean previous builds
- Build a release APK
- Sign it with your keystore
- Copy the final APK to the project root as `WhatsTap2-signed.apk`

## Detailed Instructions

### First-Time Setup

1. **Clone the repository** (if you haven't already):
   ```bash
   git clone <your-repo-url>
   cd WhatsTap2
   ```

2. **Generate your signing key**:
   ```bash
   ./generate_keystore.sh
   ```
   
   You'll be prompted for:
   - Keystore password (minimum 6 characters)
   - Key password (can be same as keystore password)
   - Certificate information:
     - Your name
     - Organization details
     - Location (city, state, country)

3. **Secure your keystore**:
   - The script creates `whatstap2-release-key.keystore`
   - **Backup this file** - you cannot regenerate it!
   - **Remember your passwords** - they cannot be recovered!

### Building APKs

1. **Run the build script**:
   ```bash
   ./build_signed_apk.sh
   ```

2. **Enter your passwords** when prompted:
   - Keystore password
   - Key password

3. **Wait for build to complete**:
   - The script will clean, build, and sign your APK
   - Final APK will be copied to `WhatsTap2-signed.apk`

## File Structure

After running the scripts, you'll have:

```
WhatsTap2/
├── generate_keystore.sh          # Script to create signing key
├── build_signed_apk.sh           # Script to build signed APK
├── keystore.properties           # Keystore configuration
├── whatstap2-release-key.keystore # Your signing key (KEEP SAFE!)
├── WhatsTap2-signed.apk          # Final signed APK
└── app/
    └── build/
        └── outputs/
            └── apk/
                └── release/
                    └── app-release.apk # Original build output
```

## Security Best Practices

### 🔐 Keystore Security
- **Never commit** your keystore file to version control
- **Never share** your keystore or passwords
- **Make backups** of your keystore file in secure locations
- **Use strong passwords** (minimum 6 characters, but longer is better)

### 📝 Version Control
Add these lines to your `.gitignore`:
```
# Signing files
*.keystore
keystore.properties
*-signed.apk
```

### 🔄 CI/CD Integration
For automated builds, you can:
- Store keystore as encrypted file in CI/CD system
- Use environment variables for passwords
- Modify the build script to read from environment variables

## Troubleshooting

### Common Issues

1. **"keytool: command not found"**
   - Install Java JDK
   - Make sure `keytool` is in your PATH

2. **"gradlew: Permission denied"**
   ```bash
   chmod +x gradlew
   ```

3. **"Build failed"**
   - Check that all dependencies are installed
   - Try running `./gradlew clean` first
   - Check Android SDK is properly configured

4. **"Keystore not found"**
   - Run `./generate_keystore.sh` first
   - Make sure you're in the project root directory

### Getting Help

If you encounter issues:
1. Check the error messages in the terminal
2. Ensure all prerequisites are installed
3. Verify you're running scripts from the project root
4. Check that your Android development environment is set up correctly

## Advanced Usage

### Custom APK Name
Edit `build_signed_apk.sh` and change:
```bash
FINAL_APK_NAME="WhatsTap2-signed.apk"
```

### Different Build Types
The scripts are configured for release builds. For debug builds, modify the `BUILD_TYPE` variable in the build script.

### Multiple Keystores
You can create multiple keystores for different purposes (debug, release, different apps) by running the keystore generation script with different names.

## Distribution

Once you have your signed APK:

### Installing on Devices
```bash
adb install WhatsTap2-signed.apk
```

### Google Play Store
- Upload the signed APK to Google Play Console
- The same keystore must be used for all future updates

### Direct Distribution
- Share the APK file directly
- Users may need to enable "Install from unknown sources"

---

**Remember**: Your keystore is the key to your app's identity. Treat it like a password and keep it secure! 🔐 