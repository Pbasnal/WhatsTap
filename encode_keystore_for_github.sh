#!/bin/bash

# Script to encode keystore for GitHub secrets
# This helps prepare your keystore for use in GitHub Actions

echo "🔐 Keystore Encoder for GitHub Secrets"
echo "======================================"

# Default keystore name from generate_keystore.sh
DEFAULT_KEYSTORE="whatstap2-release-key.keystore"

# Check if keystore exists
if [ -f "$DEFAULT_KEYSTORE" ]; then
    KEYSTORE_FILE="$DEFAULT_KEYSTORE"
    echo "✅ Found keystore: $KEYSTORE_FILE"
else
    echo "❓ Keystore file to encode:"
    read -p "Enter keystore filename (or press Enter for $DEFAULT_KEYSTORE): " KEYSTORE_INPUT
    KEYSTORE_FILE="${KEYSTORE_INPUT:-$DEFAULT_KEYSTORE}"
fi

# Verify file exists
if [ ! -f "$KEYSTORE_FILE" ]; then
    echo "❌ Error: Keystore file '$KEYSTORE_FILE' not found!"
    echo "💡 Make sure to run './generate_keystore.sh' first to create a keystore."
    exit 1
fi

echo ""
echo "📋 Encoding keystore for GitHub secrets..."

# Encode keystore to base64
BASE64_KEYSTORE=$(base64 -i "$KEYSTORE_FILE" | tr -d '\n')

echo ""
echo "✅ Keystore encoded successfully!"
echo ""
echo "🔧 GitHub Secrets Setup"
echo "======================="
echo ""
echo "Go to your GitHub repository → Settings → Secrets and variables → Actions"
echo "Add the following secrets:"
echo ""
echo "1. Secret Name: KEYSTORE_BASE64"
echo "   Secret Value:"
echo "   $BASE64_KEYSTORE"
echo ""
echo "2. Secret Name: KEYSTORE_PASSWORD"
echo "   Secret Value: [Your keystore password]"
echo ""
echo "3. Secret Name: KEY_ALIAS"
echo "   Secret Value: whatstap2-key"
echo ""
echo "4. Secret Name: KEY_PASSWORD"
echo "   Secret Value: [Your key password]"
echo ""
echo "⚠️  IMPORTANT SECURITY NOTES:"
echo "   • Never share these values publicly"
echo "   • Only add them as GitHub repository secrets"
echo "   • The base64 string above contains your private signing key"
echo ""
echo "📚 For detailed setup instructions, see: GITHUB_WORKFLOW_SETUP.md"
echo ""
echo "🎉 Once secrets are configured, you can create releases by pushing git tags:"
echo "   git tag v1.0.0"
echo "   git push origin v1.0.0" 