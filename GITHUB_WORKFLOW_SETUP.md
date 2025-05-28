# GitHub Workflow Setup Guide

This guide explains how to set up automated building, signing, and releasing of WhatsTap2 using GitHub Actions with **Continuous Integration and Deployment (CI/CD)**.

## 🔧 Prerequisites

1. **GitHub Repository**: Your code should be in a GitHub repository
2. **Signing Keystore**: You need a keystore file for signing (create one using `./generate_keystore.sh`)
3. **Repository Access**: Admin access to configure secrets

## 🔐 Setting Up GitHub Secrets

You need to configure the following secrets in your GitHub repository:

### Step 1: Navigate to Repository Settings
1. Go to your GitHub repository
2. Click on **Settings** tab
3. In the left sidebar, click **Secrets and variables** → **Actions**

### Step 2: Add Required Secrets

Click **New repository secret** for each of the following:

#### `KEYSTORE_BASE64`
Your keystore file encoded in base64:
```bash
# Generate this by running:
base64 -i your-keystore-file.keystore | tr -d '\n'

# Or if you used the generate_keystore.sh script:
base64 -i whatstap2-release-key.keystore | tr -d '\n'
```
Copy the entire output and paste it as the secret value.

#### `KEYSTORE_PASSWORD`
The password you used when creating the keystore.

#### `KEY_ALIAS`
The key alias from your keystore (default from script: `whatstap2-key`).

#### `KEY_PASSWORD`
The key password (might be same as keystore password).

### Example Secret Configuration
```
Secret Name: KEYSTORE_BASE64
Secret Value: MIIKXgIBAzCCCh4GCSqGSIb3DQEHAaCCCg8EggoLMIIKBzCCBW... (very long base64 string)

Secret Name: KEYSTORE_PASSWORD
Secret Value: your_keystore_password_here

Secret Name: KEY_ALIAS
Secret Value: whatstap2-key

Secret Name: KEY_PASSWORD
Secret Value: your_key_password_here
```

## 🚀 How the CI/CD Workflow Works

The workflow now implements a **proper CI/CD pipeline** with three trigger methods:

### Method 1: **Automatic CI/CD** (Recommended) 🔄
**Every push to the main branch triggers the workflow:**

1. **Push code to main branch**:
   ```bash
   git add .
   git commit -m "Add new feature"
   git push origin main
   ```

2. **Automatic process**:
   - ✅ **Build**: Compiles and signs APK/AAB
   - ✅ **Test**: Verifies signatures and build integrity
   - ✅ **Tag**: Creates version tag (e.g., `v1.0.20241215.42`) **only if build succeeds**
   - ✅ **Release**: Publishes artifacts **only if build succeeds**

3. **Version naming**: `1.0.YYYYMMDD.BUILD_NUMBER`
   - Example: `v1.0.20241215.42` (December 15, 2024, build #42)

### Method 2: Manual Release 🎯
1. Go to your repository on GitHub
2. Click **Actions** tab
3. Select **Build and Release WhatsTap2** workflow
4. Click **Run workflow**
5. Enter version name (e.g., `1.0.0`) and version code (e.g., `1`)
6. **Same process**: Build → Tag → Release (only if successful)

### Method 3: Tag-based Release 🏷️
1. **Create a version tag**:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
2. Uses existing tag, builds and releases

## 📦 What the Workflow Produces

### 🔄 **For Every Successful Main Branch Push:**

#### 📱 APK File
- **File**: `WhatsTap2-v1.0.20241215.42.apk`
- **Use**: Direct installation on Android devices
- **Tagged**: Also creates `WhatsTap2-v1.0.20241215.42-abc12345.apk` with commit SHA

#### 📦 AAB File
- **File**: `WhatsTap2-v1.0.20241215.42.aab`
- **Use**: Upload to Google Play Store
- **Tagged**: Also creates `WhatsTap2-v1.0.20241215.42-abc12345.aab` with commit SHA

#### 🏷️ Git Tag
- **Automatically created**: `v1.0.20241215.42`
- **Only created if build succeeds**
- **Links release to exact commit**

#### 📋 GitHub Release
- **Automatic release notes** with build information
- **Download links** for APK and AAB
- **SHA256 checksums** for verification
- **Installation instructions**

## 🔍 Workflow Features

### 🛡️ **Build-First Approach**
- ✅ **Build and test first** - no tags/releases created if build fails
- ✅ **Fail fast** - stops immediately on build errors
- ✅ **Clean rollback** - failed builds don't create artifacts
- ✅ **Safe CI/CD** - only successful builds get released

### 🏗️ **Smart Build Process**
- ✅ Builds both APK and AAB formats
- ✅ Signs with your production keystore
- ✅ Verifies signatures before tagging
- ✅ Updates version numbers automatically
- ✅ Caches dependencies for faster builds
- ✅ Creates tagged artifacts with commit SHA

### 📊 **Intelligent Release Management**
- ✅ **Automatic versioning** based on date and build number
- ✅ **Conditional tagging** - only if build succeeds
- ✅ **Rich release notes** with trigger information
- ✅ **Full traceability** from commit to release
- ✅ **Multiple trigger methods** for flexibility

### 🔐 **Security & Verification**
- ✅ SHA256 checksums for all artifacts
- ✅ Signature verification during build
- ✅ Build metadata tracking
- ✅ Full audit trail from source to release

## 🎯 **CI/CD Benefits**

### ✨ **For Development**
- **Continuous Integration**: Every main branch push is built and tested
- **Automatic Releases**: No manual steps required for releases
- **Version Control**: Automatic, consistent version numbering
- **Quality Gates**: Only successful builds create releases

### 👥 **For Users**
- **Always Latest**: Main branch always has the latest release
- **Verified Downloads**: SHA256 checksums for security
- **Clear History**: Every release linked to specific commits
- **Reliable Builds**: Only tested, successful builds are released

### 🔄 **For Workflow**
- **Push and Forget**: Just push to main, get automatic release
- **No Manual Tagging**: Tags created automatically after successful builds
- **Fail Safe**: Build failures don't create broken releases
- **Full Automation**: From code push to published release

## 🐛 Troubleshooting

### Common Issues

#### "Build failed - no release created"
- ✅ **This is expected behavior** - the workflow only creates releases for successful builds
- Check the Actions logs for specific build errors
- Fix the build issues and push again

#### "Tag already exists"
- For main branch pushes, this shouldn't happen (automatic versioning)
- For manual triggers, use a new version number
- Check existing tags: `git tag -l`

#### "Invalid keystore format"
- Ensure your keystore is properly base64 encoded
- Check that you copied the entire base64 string
- Verify the keystore file isn't corrupted

#### "Wrong password"
- Double-check your `KEYSTORE_PASSWORD` and `KEY_PASSWORD` secrets
- Ensure there are no extra spaces or characters

### Checking Build Status
```bash
# View recent workflow runs
gh run list --repo your-username/WhatsTap2

# View specific run details
gh run view RUN_ID --repo your-username/WhatsTap2

# Check latest release
gh release list --repo your-username/WhatsTap2
```

### Verifying Release Artifacts
```bash
# Verify APK checksum (download from GitHub release)
sha256sum WhatsTap2-v1.0.20241215.42.apk
# Compare with checksum in release notes

# Verify APK signature
$ANDROID_HOME/build-tools/34.0.0/apksigner verify WhatsTap2-v1.0.20241215.42.apk
```

## 📝 Workflow Customization

### Changing Version Strategy
Edit `.github/workflows/build-and-release.yml`:

```yaml
# Current: 1.0.YYYYMMDD.BUILD_NUMBER
VERSION_NAME="1.0.${TIMESTAMP}.${{ github.run_number }}"

# Alternative: Semantic versioning
VERSION_NAME="1.0.${{ github.run_number }}"

# Alternative: Year-based
VERSION_NAME="$(date +%Y).$(date +%m).${{ github.run_number }}"
```

### Custom Branch Triggers
```yaml
on:
  push:
    branches:
      - main
      - develop  # Also trigger on develop branch
      - release/*  # Trigger on release branches
```

### Adding Build Variants
```yaml
# Build debug version too
- name: Build Debug APK
  run: ./gradlew assembleDebug
```

## 🔄 Updating the Workflow

To update the workflow:
1. Edit `.github/workflows/build-and-release.yml`
2. Commit and push to main branch
3. The updated workflow will be used for future runs

## 📚 Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android App Signing](https://developer.android.com/studio/publish/app-signing)
- [GitHub Secrets Management](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [CI/CD Best Practices](https://docs.github.com/en/actions/guides/about-continuous-integration)

## 🎯 **Key Improvements**

### ✨ **New CI/CD Features**
- **Build-First Strategy**: Tags and releases only created after successful builds
- **Automatic Versioning**: Date and build number based versions
- **Continuous Integration**: Every main branch push triggers build
- **Fail-Safe Releases**: Failed builds don't create broken releases
- **Smart Triggers**: Multiple trigger methods for different workflows

### 🔧 **Better Development Workflow**
- **Push to Release**: Just push to main branch for automatic release
- **No Manual Steps**: Complete automation from code to release
- **Quality Gates**: Only successful builds create releases
- **Full Traceability**: Every release linked to specific commit

---

**🎉 Perfect CI/CD Setup: Push to main → Build → Test → Tag → Release!** 

**New CI/CD benefits:**
- ✅ **Automatic releases** on every main branch push
- ✅ **Build-first approach** - no broken releases
- ✅ **Smart versioning** with date and build numbers
- ✅ **Fail-safe process** - only successful builds get released
- ✅ **Complete automation** from push to published release 