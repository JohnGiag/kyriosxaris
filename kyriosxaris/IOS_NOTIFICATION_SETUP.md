# iOS Custom Sound Notifications Setup Guide

This guide explains how to implement custom sound notifications for iOS that work when the app is open or closed. Unlike Android, iOS doesn't have the "closed app resource access" issue, but it has different requirements for sound files and configuration.

## Key Differences from Android

- **No notification channels**: iOS doesn't use notification channels (Android-specific feature)
- **No pre-creation needed**: iOS can access bundled resources even when the app is closed
- **Sound files must be bundled**: Sounds are part of the app binary, not dynamically loaded
- **Different file formats**: iOS prefers `.caf` or `.aiff` formats (not `.mp3`)
- **APNs required**: Must configure Apple Push Notification Service certificates/keys

## Prerequisites

- Mac with Xcode installed (latest version recommended)
- Apple Developer account ($99/year)
- iOS device for testing (simulator doesn't support push notifications)
- Firebase project with iOS app configured
- APNs certificate or key configured in Firebase

## Step-by-Step Setup

### 1. Add iOS Platform to Capacitor

```bash
cd kyriosxaris
npx cap add ios
```

This creates the `ios/` folder with the Xcode project.

### 2. Add Sound Files to iOS Bundle

**Location**: `ios/App/App/` (or `ios/App/` depending on Capacitor version)

**Supported formats**:
- `.caf` (Core Audio Format) — **Recommended**
- `.aiff` (Audio Interchange File Format)
- `.wav` (Waveform Audio)
- `.mp3` (MPEG Audio) — Not recommended, may not work reliably

**Important requirements**:
- File names must be lowercase with no spaces
- Maximum 30 seconds duration
- Must be properly formatted (use `afconvert` tool to convert)

**Convert sound files** (on Mac):
```bash
# Convert to .caf format (recommended - IMA4 codec)
afconvert /path/to/sound.wav /path/to/sound.caf -d ima4 -f caff -v

# Convert to .aiff format (uncompressed - larger but reliable)
afconvert /path/to/sound.wav /path/to/sound.aiff -d LEI16 -f AIFF -v

# Check file info
afinfo output.caf
```

**Add to Xcode**:
1. Open `ios/App/App.xcworkspace` in Xcode
2. Right-click `App` folder → "Add Files to App..."
3. Select your sound files (`.caf`, `.aiff`, or `.wav`)
4. **Important**: Ensure "Copy items if needed" is checked
5. **Important**: Ensure "App" target is selected
6. Click "Add"

**Verify in Xcode**:
- Select a sound file in the project navigator
- In File Inspector (right panel), ensure "Target Membership" includes "App"
- If not checked, check the "App" checkbox

### 3. Configure Firebase for iOS

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Click the iOS app icon (or "Add app" if iOS app doesn't exist)
4. Enter your iOS bundle ID (from `capacitor.config.ts` → `appId`)
5. Download `GoogleService-Info.plist`
6. Add to Xcode:
   - Drag `GoogleService-Info.plist` into `ios/App/App/` folder
   - Ensure "Copy items if needed" is checked
   - Ensure "App" target is selected
   - Click "Finish"

### 4. Configure Push Notifications Capability

In Xcode:
1. Select the project in navigator (top item)
2. Select "App" target
3. Go to "Signing & Capabilities" tab
4. Click "+ Capability"
5. Add "Push Notifications"
6. Add "Background Modes"
   - Check "Remote notifications" checkbox

### 5. Configure APNs in Firebase

**Option A: APNs Authentication Key (Recommended)**
1. Go to [Apple Developer Portal](https://developer.apple.com/account/resources/authkeys/list)
2. Create a new key with "Apple Push Notifications service (APNs)" enabled
3. Download the `.p8` key file (you can only download once!)
4. Note the Key ID
5. Go to Firebase Console → Project Settings → Cloud Messaging
6. Under "Apple app configuration" → "APNs Authentication Key"
7. Upload the `.p8` file
8. Enter Key ID and Team ID

**Option B: APNs Certificates**
1. Create APNs certificate in Apple Developer Portal
2. Download `.p12` certificate
3. Upload to Firebase Console → Cloud Messaging → APNs Certificates

### 6. Update Info.plist

Add to `ios/App/App/Info.plist`:

```xml
<key>UIBackgroundModes</key>
<array>
    <string>remote-notification</string>
</array>
```

**Or in Xcode**:
1. Open `Info.plist` in Xcode
2. Click "+" to add new key
3. Type "UIBackgroundModes" (or select from dropdown)
4. Set type to "Array"
5. Click "+" to add item
6. Type "remote-notification" (or select from dropdown)

### 7. Create Notification Service Extension (Optional)

For advanced notification handling when app is closed:

**Create Extension**:
1. Xcode → File → New → Target
2. Select "Notification Service Extension"
3. Name it "NotificationService"
4. Language: Swift or Objective-C
5. Click "Finish"
6. Activate scheme when prompted

**Note**: The extension is optional for basic custom sounds. iOS can play custom sounds from the main app bundle without an extension.

### 8. Update Capacitor Configuration

Ensure `capacitor.config.ts` includes iOS:

```typescript
import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'gr.kyrios.xaris',
  appName: 'kyriosxaris',
  webDir: 'www',
  ios: {
    // iOS-specific config if needed
  },
};

export default config;
```

### 9. Sync Capacitor

```bash
npx cap sync ios
```

This syncs your web assets and updates the iOS project.

### 10. Build and Run

1. Open `ios/App/App.xcworkspace` in Xcode
2. Select your development team in "Signing & Capabilities"
3. Connect a physical iOS device (simulator doesn't support push)
4. Select your device as the run destination
5. Click Run (⌘R) to build and install

## FCM Payload Format for iOS

Your Firebase functions already handle iOS sounds. The payload structure:

```json
{
  "message": {
    "token": "iOS_FCM_TOKEN",
    "notification": {
      "title": "Notification Title",
      "body": "Notification Body"
    },
    "apns": {
      "payload": {
        "aps": {
          "sound": "ding.caf"
        }
      }
    }
  }
}
```

**Important**:
- Sound name should include extension (`.caf`, `.aiff`, or `.wav`)
- Sound file must be in the app bundle
- Use lowercase names with no spaces

### Using Your Firebase Functions

Your existing function already supports iOS. Send notification with:

```json
{
  "token": "iOS_TOKEN",
  "title": "Test Notification",
  "body": "This is a test",
  "iosSound": "ding.caf"
}
```

Or use the general `sound` parameter:

```json
{
  "token": "iOS_TOKEN",
  "title": "Test Notification",
  "body": "This is a test",
  "sound": "shockding.caf"
}
```

**Priority**: `iosSound` takes precedence over `sound`.

## Sound File Naming Convention

**Recommended naming**:
- `ding.caf`
- `custom_sound.caf`
- `shockding.caf`

**In FCM payload**:
- Use full filename with extension: `"ding.caf"` (recommended)
- Or just name: `"ding"` (iOS will try common extensions, but less reliable)

## How It Works

### Notification Flow

1. **App Installed**:
   - Sound files are bundled with the app
   - iOS can access them even when app is closed

2. **Notification Received**:
   - FCM sends notification with sound name in `apns.payload.aps.sound`
   - iOS looks for sound file in app bundle
   - If found, plays custom sound
   - If not found, plays default sound

3. **When App is Closed**:
   - iOS can still access bundled sound files
   - Custom sound plays correctly (unlike Android, no pre-creation needed)

### Sound URI Format

iOS uses simple filenames, not URIs:
- Sound name: `"ding.caf"`
- iOS looks in app bundle root
- No path needed (files are in `App/` folder)

## Troubleshooting

### Custom Sound Doesn't Play

1. **Check file format**: Use `.caf` or `.aiff` (not `.mp3`)
2. **Check file location**: Must be in app bundle root (`App/` folder)
3. **Check target membership**: File must be included in "App" target
4. **Check sound name**: Must match exactly (case-sensitive)
5. **Check file duration**: Must be ≤ 30 seconds
6. **Check file size**: Keep under 500KB if possible
7. **Check FCM payload**: Sound name must include extension or be exact match
8. **Check Xcode build**: Ensure sound files are copied to bundle (check Build Phases → Copy Bundle Resources)

### Notifications Not Received

1. **Check APNs configuration**: Must be configured in Firebase
2. **Check device token**: Must be valid FCM token from iOS device
3. **Check capabilities**: Push Notifications must be enabled
4. **Check provisioning profile**: Must include Push Notifications entitlement
5. **Check device**: Must be physical device (simulator doesn't support push)
6. **Check network**: Device must have internet connection
7. **Check Firebase setup**: `GoogleService-Info.plist` must be in project

### Sound Plays Default Instead of Custom

1. **Verify sound file is in app bundle**: Check Xcode project navigator
2. **Verify sound name matches exactly**: Case-sensitive, include extension
3. **Check file format**: Convert to `.caf` if using `.wav` or `.mp3`
4. **Verify sound file is included in build**: Check Build Phases
5. **Check Xcode build log**: Look for warnings about missing files
6. **Test with simple sound**: Try with a known-working `.caf` file

### Converting Sound Files

**On Mac, use Terminal**:
```bash
# Convert to .caf (IMA4 codec - good compression, recommended)
afconvert input.wav output.caf -d ima4 -f caff -v

# Convert to .aiff (uncompressed - larger but reliable)
afconvert input.wav output.aiff -d LEI16 -f AIFF -v

# Convert to .caf (Linear PCM - uncompressed, highest quality)
afconvert input.wav output.caf -d LEI16 -f caff -v

# Check file info
afinfo output.caf
```

**Online converters** (if you don't have Mac access):
- Use online WAV to CAF converters
- Ensure output is proper CAF format

### Common Issues

**Issue**: Sound file not found
- **Solution**: Verify file is in Xcode project and included in target

**Issue**: Sound plays but is distorted
- **Solution**: Convert to proper format (`.caf` with IMA4 codec)

**Issue**: Notification received but no sound
- **Solution**: Check device is not in silent mode, check volume

**Issue**: Build fails with missing file
- **Solution**: Ensure sound files are added to project correctly

## Testing Checklist

- [ ] iOS platform added to Capacitor
- [ ] Sound files converted to `.caf` or `.aiff` format
- [ ] Sound files added to Xcode project
- [ ] Sound files included in app target membership
- [ ] `GoogleService-Info.plist` added to project
- [ ] Push Notifications capability enabled
- [ ] Background Modes → Remote notifications enabled
- [ ] APNs configured in Firebase (key or certificate)
- [ ] Info.plist updated with UIBackgroundModes
- [ ] App built and installed on physical iOS device
- [ ] FCM token obtained from iOS app
- [ ] Notification received when app is open
- [ ] Custom sound plays when app is open
- [ ] Notification received when app is closed
- [ ] Custom sound plays when app is closed
- [ ] Different sounds work correctly
- [ ] Default sound works when no custom sound specified

## Adding New Sounds

1. Convert sound file to `.caf` format:
   ```bash
   afconvert new_sound.wav new_sound.caf -d ima4 -f caff -v
   ```

2. Add to Xcode project:
   - Drag file into `App` folder
   - Ensure "Copy items if needed" is checked
   - Ensure "App" target is selected

3. Rebuild app:
   ```bash
   npx cap sync ios
   ```
   Then build in Xcode

4. Send notification with new sound:
   ```json
   {
     "token": "iOS_TOKEN",
     "title": "Test",
     "body": "Test notification",
     "iosSound": "new_sound.caf"
   }
   ```

## File Structure After Setup

```
ios/
├── App/
│   ├── App/
│   │   ├── AppDelegate.swift (or .m)
│   │   ├── Info.plist
│   │   ├── GoogleService-Info.plist
│   │   ├── ding.caf              ← Sound files here
│   │   ├── custom_sound.caf
│   │   └── shockding.caf
│   ├── App.xcodeproj
│   └── (other Xcode files)
└── App.xcworkspace
```

## Important Notes

- **iOS doesn't have Android's "closed app" issue**: iOS can access bundled resources even when the app is closed, so no pre-creation needed
- **No pre-creation needed**: Unlike Android channels, iOS doesn't require pre-registration
- **Sound files are bundled**: They're part of the app binary, not dynamically loaded
- **Extension optional**: Notification Service Extension is optional for basic custom sounds
- **Physical device required**: iOS Simulator doesn't support push notifications
- **APNs required**: Must configure APNs certificates/keys in Firebase
- **File format matters**: Use `.caf` or `.aiff` for best compatibility
- **Case-sensitive**: Sound names are case-sensitive
- **Include extension**: Best practice is to include file extension in FCM payload

## Dependencies

- Capacitor iOS plugin (`@capacitor/ios`)
- Firebase iOS SDK (via CocoaPods, auto-installed)
- Xcode (latest version recommended)
- iOS 13+ for modern notification features

## Additional Resources

- [Apple Push Notifications Documentation](https://developer.apple.com/documentation/usernotifications)
- [Firebase Cloud Messaging for iOS](https://firebase.google.com/docs/cloud-messaging/ios/client)
- [Capacitor iOS Documentation](https://capacitorjs.com/docs/ios)
- [Apple Developer Portal](https://developer.apple.com/account/)

## License

This implementation is provided as-is. Adapt as needed for your project.

