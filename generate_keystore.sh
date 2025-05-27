#!/bin/bash

# Script to generate Android signing keystore
# This creates a keystore file that will be used to sign your APK

echo "🔑 Android Keystore Generator"
echo "=============================="

# Set default values
KEYSTORE_NAME="whatstap2-release-key.keystore"
KEY_ALIAS="whatstap2-key"
VALIDITY_DAYS=10000

echo "This script will generate a keystore for signing your Android APK."
echo "You'll need to provide some information for the certificate."
echo ""

# Check if keystore already exists
if [ -f "$KEYSTORE_NAME" ]; then
    echo "⚠️  Keystore '$KEYSTORE_NAME' already exists!"
    read -p "Do you want to overwrite it? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ Keystore generation cancelled."
        exit 1
    fi
    rm "$KEYSTORE_NAME"
fi

echo "📝 Please provide the following information for your certificate:"
echo ""

# Get keystore password
while true; do
    read -s -p "Enter keystore password (min 6 characters): " KEYSTORE_PASSWORD
    echo
    if [ ${#KEYSTORE_PASSWORD} -ge 6 ]; then
        read -s -p "Confirm keystore password: " KEYSTORE_PASSWORD_CONFIRM
        echo
        if [ "$KEYSTORE_PASSWORD" = "$KEYSTORE_PASSWORD_CONFIRM" ]; then
            break
        else
            echo "❌ Passwords don't match. Please try again."
        fi
    else
        echo "❌ Password must be at least 6 characters long."
    fi
done

# Get key password
echo ""
read -p "Use same password for key? (Y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Nn]$ ]]; then
    while true; do
        read -s -p "Enter key password (min 6 characters): " KEY_PASSWORD
        echo
        if [ ${#KEY_PASSWORD} -ge 6 ]; then
            read -s -p "Confirm key password: " KEY_PASSWORD_CONFIRM
            echo
            if [ "$KEY_PASSWORD" = "$KEY_PASSWORD_CONFIRM" ]; then
                break
            else
                echo "❌ Passwords don't match. Please try again."
            fi
        else
            echo "❌ Password must be at least 6 characters long."
        fi
    done
else
    KEY_PASSWORD="$KEYSTORE_PASSWORD"
fi

# Get certificate information
echo ""
echo "📋 Certificate Information:"
read -p "First and Last Name (e.g., John Doe): " FIRST_LAST_NAME
read -p "Organizational Unit (e.g., IT Department): " ORG_UNIT
read -p "Organization (e.g., Your Company): " ORGANIZATION
read -p "City or Locality (e.g., San Francisco): " CITY
read -p "State or Province (e.g., CA): " STATE
read -p "Country Code (2 letters, e.g., US): " COUNTRY

# Validate country code
while [ ${#COUNTRY} -ne 2 ]; do
    read -p "Country code must be exactly 2 letters (e.g., US): " COUNTRY
done

echo ""
echo "🔨 Generating keystore..."

# Generate the keystore
keytool -genkey -v \
    -keystore "$KEYSTORE_NAME" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity $VALIDITY_DAYS \
    -storepass "$KEYSTORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -dname "CN=$FIRST_LAST_NAME, OU=$ORG_UNIT, O=$ORGANIZATION, L=$CITY, S=$STATE, C=$COUNTRY"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Keystore generated successfully!"
    echo ""
    echo "📁 Keystore file: $KEYSTORE_NAME"
    echo "🔑 Key alias: $KEY_ALIAS"
    echo "⏰ Valid for: $VALIDITY_DAYS days (~27 years)"
    echo ""
    echo "⚠️  IMPORTANT: Keep this keystore file and passwords safe!"
    echo "   You'll need them to update your app in the future."
    echo ""
    
    # Create a properties file for the build script
    echo "# Keystore configuration for build script" > keystore.properties
    echo "storeFile=$KEYSTORE_NAME" >> keystore.properties
    echo "keyAlias=$KEY_ALIAS" >> keystore.properties
    echo "# Note: Passwords should be provided when running build script" >> keystore.properties
    
    echo "📝 Created keystore.properties file for build script"
    echo ""
    echo "🔐 Security Tips:"
    echo "   • Never commit keystore.properties with passwords to version control"
    echo "   • Store the keystore file in a secure location"
    echo "   • Make backups of your keystore file"
    echo "   • Remember your passwords - they cannot be recovered!"
    
else
    echo ""
    echo "❌ Failed to generate keystore!"
    echo "Please check that you have Java keytool installed and try again."
    exit 1
fi 