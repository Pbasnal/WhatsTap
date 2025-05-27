# WhatsTap2 📱❤️

**A Simple Contact Launcher for Grandparents**

WhatsTap2 is a user-friendly Android app designed specifically to help elderly family members easily make video and voice calls. Built with love for grandparents who want to stay connected with their family but find modern smartphones overwhelming.

## 🎯 Purpose

This app was created to solve a simple but important problem: making it easier for grandparents to call their loved ones. Instead of navigating through complex contact lists and multiple apps, WhatsTap2 provides a clean, simple interface with large, easy-to-tap contact cards.

## ✨ Features

### 📞 Smart Calling
- **WhatsApp Video Calls**: Automatically opens WhatsApp for video calls when contact is labeled as "WhatsApp"
- **Regular Voice Calls**: Makes traditional phone calls for all other contacts
- **One-Tap Calling**: Simply tap a contact card to call them

### 👥 Contact Management
- **Starred Contacts Only**: Shows only the contacts marked as "favorites" in the phone
- **Large Contact Cards**: Easy-to-see photos and names in a grid layout
- **Phone Number Labels**: Displays contact types (Home, Mobile, WhatsApp, etc.)
- **Automatic Sync**: Syncs with your phone's starred contacts

### 🔄 Duplicate Prevention
- **Smart Phone Number Matching**: Recognizes the same number in different formats
- **No Duplicate Contacts**: Prevents the same person from appearing multiple times
- **Automatic Updates**: Updates contact information when syncing

### 🎨 Senior-Friendly Design
- **Large Text and Buttons**: Easy to read and tap
- **Simple Grid Layout**: Clean, uncluttered interface
- **Contact Photos**: Visual recognition with profile pictures
- **Minimal Navigation**: No complex menus or settings

## 📱 Screenshots

*[Add screenshots here showing the main interface, contact cards, and calling features]*

## 🚀 Getting Started

### For Family Members (Setting Up)

1. **Install the App**: Install WhatsTap2 on your grandparent's phone
2. **Star Important Contacts**: In the phone's regular contacts app, star (⭐) the family members they should be able to call easily
3. **Add WhatsApp Labels**: For contacts who use WhatsApp, edit their phone number and add "WhatsApp" in the label field
4. **Sync Contacts**: Open WhatsTap2 and tap the sync button (🔄) to load the starred contacts

### For Grandparents (Using the App)

1. **Open WhatsTap2**: Tap the app icon to open
2. **See Your Family**: All your important contacts will appear as large cards with photos
3. **Make a Call**: Simply tap on someone's photo to call them
   - WhatsApp contacts will open WhatsApp for video calling
   - Other contacts will make a regular phone call
4. **Add New Contacts**: Ask a family member to help add new people to your starred contacts

## 🛠️ Technical Features

### Built With
- **Kotlin** - Modern Android development
- **Jetpack Compose** - Modern UI toolkit
- **Room Database** - Local contact storage
- **Android Contacts API** - System integration

### Key Capabilities
- Reads starred contacts from Android system
- Stores contacts locally for fast access
- Handles different phone number formats
- Integrates with WhatsApp and phone dialer
- Prevents duplicate contacts automatically

## 📋 Requirements

- **Android 7.0** (API level 24) or higher
- **Permissions Required**:
  - Read Contacts (to access starred contacts)
  - Phone (to make voice calls)
  - WhatsApp installed (for video calls)

## 🔧 Installation

### For Developers
1. Clone this repository
2. Open in Android Studio
3. Build and run on device or emulator

### For End Users
1. Download the APK file
2. Enable "Install from unknown sources" if needed
3. Install the app
4. Grant required permissions when prompted

## 🏗️ Building from Source

### Local Building
See [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md) for detailed build instructions including:
- Setting up signing keys
- Building signed APKs
- Prerequisites and troubleshooting

Quick build commands:
```bash
./check_prerequisites.sh    # Check build environment
./generate_keystore.sh      # Create signing key (first time)
./build_signed_apk.sh       # Build signed APK
```

### Automated GitHub Releases
This project includes automated building and releasing via GitHub Actions:
- **Automatic releases** when you push version tags (e.g., `v1.0.0`)
- **Manual releases** through GitHub Actions interface
- **Both APK and AAB** formats generated automatically
- **Signed releases** ready for distribution

Setup instructions: [GITHUB_WORKFLOW_SETUP.md](GITHUB_WORKFLOW_SETUP.md)

Quick release:
```bash
git tag v1.0.0
git push origin v1.0.0
# GitHub will automatically build and create a release!
```

## 🔒 Privacy & Security

- **Local Storage**: All contact data is stored locally on the device
- **No Data Collection**: The app doesn't collect or transmit personal data
- **System Integration**: Uses standard Android APIs for contacts and calling
- **Secure Builds**: APKs are signed with secure keystores

## 🤝 Contributing

This is a family project, but contributions are welcome! If you have elderly family members who could benefit from similar features, feel free to:

1. Fork the repository
2. Create a feature branch
3. Make your improvements
4. Submit a pull request

### Ideas for Contributions
- Larger text size options
- Voice commands for calling
- Emergency contact features
- Simplified setup wizard
- Accessibility improvements

## 📞 Support

If you're setting this up for your grandparents and need help:

1. Check the [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md) for technical setup
2. Read the troubleshooting section below
3. Create an issue on GitHub for bugs or feature requests

## 🐛 Troubleshooting

### Common Issues

**"No contacts showing"**
- Make sure contacts are starred (⭐) in the phone's contacts app
- Tap the sync button (🔄) in WhatsTap2
- Check that contact permissions are granted

**"WhatsApp not opening"**
- Ensure WhatsApp is installed
- Check that the contact has "WhatsApp" in the phone number label
- Try making a regular call instead

**"Duplicate contacts appearing"**
- This should be automatically prevented
- Try syncing again with the sync button
- Check if the same person has multiple starred phone numbers

**"App crashes or won't start"**
- Ensure Android 7.0 or higher
- Grant all requested permissions
- Restart the phone and try again

## 🎉 Success Stories

*"My grandmother can now video call us every Sunday without any help. She just taps on our photos and WhatsApp opens automatically!"*

*"Setting up WhatsTap2 was the best decision. No more explaining how to find contacts or which app to use for calling."*

## 📅 Version History

- **v1.0**: Initial release with basic contact display and calling
- **v1.1**: Added phone number labels and WhatsApp integration
- **v1.2**: Implemented duplicate contact prevention
- **v1.3**: Enhanced phone number normalization

## 🙏 Acknowledgments

- Built with love for grandparents everywhere
- Inspired by the need to keep families connected
- Thanks to the Android and Kotlin communities for excellent tools

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

---

**Made with ❤️ for family connections**

*Because staying in touch with grandparents should be simple, not complicated.* 