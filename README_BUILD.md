# Quick Build Guide

## 🚀 Quick Start

1. **Check prerequisites**:
   ```bash
   ./check_prerequisites.sh
   ```

2. **Generate signing key** (first time only):
   ```bash
   ./generate_keystore.sh
   ```

3. **Build signed APK**:
   ```bash
   ./build_signed_apk.sh
   ```

## 📁 Files Created

- `whatstap2-release-key.keystore` - Your signing key (keep safe!)
- `keystore.properties` - Configuration file
- `WhatsTap2-signed.apk` - Final signed APK ready for distribution

## 🔐 Security

- **Never commit** keystore files to git
- **Backup** your keystore file securely
- **Remember** your passwords (they cannot be recovered)

## 📖 Full Documentation

See `BUILD_INSTRUCTIONS.md` for detailed instructions and troubleshooting.

---

**Ready to build? Run `./check_prerequisites.sh` first!** ✨ 