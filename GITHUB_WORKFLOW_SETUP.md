# GitHub Workflow Setup Guide

This guide explains how to set up automated building, signing, and releasing of WhatsTap2 using GitHub Actions.

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

## 🚀 How to Use the Workflow

The workflow can be triggered in two ways:

### Method 1: Automatic Release (Recommended)
1. **Create a version tag**:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
2. The workflow will automatically:
   - Build both APK and AAB
   - Sign them with your keystore
   - Create a GitHub release
   - Upload the files to the release

### Method 2: Manual Trigger
1. Go to your repository on GitHub
2. Click **Actions** tab
3. Select **Build and Release WhatsTap2** workflow
4. Click **Run workflow**
5. Enter version name (e.g., `1.0.0`) and version code (e.g., `1`)
6. Click **Run workflow**

## 📦 What the Workflow Produces

For each release, you'll get:

### 📱 APK File
- **File**: `WhatsTap2-v1.0.0.apk`
- **Use**: Direct installation on Android devices
- **Size**: Optimized for distribution

### 📦 AAB File
- **File**: `WhatsTap2-v1.0.0.aab`
- **Use**: Upload to Google Play Store
- **Size**: Smaller, optimized by Play Store

### 📋 Release Notes
Automatically generated with:
- Version information
- File sizes
- Installation instructions
- Feature list
- Setup guide

## 🔍 Workflow Features

### 🛡️ Security
- ✅ Keystore stored securely as base64-encoded secret
- ✅ Passwords stored as GitHub secrets
- ✅ Keystore files cleaned up after build
- ✅ No sensitive data in logs

### 🏗️ Build Process
- ✅ Builds both APK and AAB formats
- ✅ Signs with your production keystore
- ✅ Verifies signatures
- ✅ Updates version numbers automatically
- ✅ Caches dependencies for faster builds

### 📊 Release Management
- ✅ Creates GitHub releases automatically
- ✅ Uploads both APK and AAB files
- ✅ Generates comprehensive release notes
- ✅ Includes installation instructions

## 🐛 Troubleshooting

### Common Issues

#### "Invalid keystore format"
- Ensure your keystore is properly base64 encoded
- Check that you copied the entire base64 string
- Verify the keystore file isn't corrupted

#### "Wrong password"
- Double-check your `KEYSTORE_PASSWORD` and `KEY_PASSWORD` secrets
- Ensure there are no extra spaces or characters

#### "Key alias not found"
- Verify your `KEY_ALIAS` secret matches the alias in your keystore
- List aliases with: `keytool -list -keystore your-keystore.keystore`

#### "Build failed"
- Check the Actions logs for specific error messages
- Ensure your code builds locally first
- Verify all dependencies are properly configured

### Checking Your Keystore Info
```bash
# List all aliases in your keystore
keytool -list -keystore whatstap2-release-key.keystore

# Get detailed info about a specific alias
keytool -list -v -keystore whatstap2-release-key.keystore -alias whatstap2-key
```

## 📝 Workflow Customization

### Changing Version Strategy
Edit `.github/workflows/build-and-release.yml`:

```yaml
# Use build number as version code
VERSION_CODE=${{ github.run_number }}

# Or use a custom strategy
VERSION_CODE=$(date +%Y%m%d%H)
```

### Adding Build Variants
```yaml
# Build debug version too
- name: Build Debug APK
  run: ./gradlew assembleDebug
```

### Custom Release Notes
Modify the `body:` section in the "Create Release" step to customize release notes.

## 🔄 Updating the Workflow

To update the workflow:
1. Edit `.github/workflows/build-and-release.yml`
2. Commit and push changes
3. The updated workflow will be used for future runs

## 📚 Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android App Signing](https://developer.android.com/studio/publish/app-signing)
- [GitHub Secrets Management](https://docs.github.com/en/actions/security-guides/encrypted-secrets)

---

**🎉 Once set up, you'll have fully automated releases with just a git tag!** 