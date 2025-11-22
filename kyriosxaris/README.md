# Ionic Angular FCM Push Notifications with Custom Sounds

An Ionic Angular application configured to receive Firebase Cloud Messaging (FCM) push notifications with **custom sound support for Android devices that works even when the app is closed**.

## Problem Solved

When Android receives a notification while the app is completely closed, it cannot access app resources (sound files) until the app has been initialized at least once. This causes the first notification with a custom sound to fail silently.

**Solution**: Pre-create notification channels with sound URIs at app startup. Android caches these URIs, allowing custom sounds to play even when the app is closed.

## Features

- ✅ Firebase Cloud Messaging (FCM) integration
- ✅ Custom notification sounds that work when app is closed
- ✅ Android notification channels with pre-creation
- ✅ Notification history tracking
- ✅ FCM token display and copying
- ✅ Foreground and background notification handling

## Architecture Overview

The solution consists of three key components:

1. **MainActivity** - Pre-creates notification channels at app startup
2. **CustomFirebaseMessagingService** - Handles incoming notifications and reuses pre-created channels
3. **SoundUriHelper** - Utility for consistent sound URI generation

## Prerequisites

- Node.js and npm installed
- Ionic CLI installed (`npm install -g @ionic/cli`)
- Android Studio installed (for Android development)
- Firebase project with FCM enabled
- Google Services JSON file (`google-services.json`)

## Quick Start

### 1. Firebase Configuration

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or select an existing one
3. Add an Android app to your Firebase project
4. Download `google-services.json`
5. Place `google-services.json` in `android/app/` directory

### 2. Update Firebase Credentials

Edit `src/environments/environment.ts` and `src/environments/environment.prod.ts` with your Firebase configuration:

```typescript
export const environment = {
  production: false,
  firebase: {
    apiKey: "YOUR_API_KEY",
    authDomain: "YOUR_AUTH_DOMAIN",
    projectId: "YOUR_PROJECT_ID",
    storageBucket: "YOUR_STORAGE_BUCKET",
    messagingSenderId: "YOUR_MESSAGING_SENDER_ID",
    appId: "YOUR_APP_ID",
  },
};
```

### 3. Add Custom Sound Files

1. Place your sound files in `android/app/src/main/res/raw/`
2. Supported formats: `.mp3`, `.wav`, `.ogg`
3. File names must be lowercase with underscores (e.g., `ding.wav`, `custom_sound.mp3`)

### 4. Configure MainActivity

**CRITICAL**: Update the `CUSTOM_SOUNDS` array in `MainActivity.java` with your sound file names (without extension):

```java
private static final String[] CUSTOM_SOUNDS = {"ding", "custom_sound", "shockding"};
```

Add all your custom sound files here. This list determines which channels are pre-created at startup.

### 5. Update Package Names

Replace `gr.kyrios.xaris` with your app's package name in:
- `MainActivity.java`
- `CustomFirebaseMessagingService.java`
- `SoundUriHelper.java`

### 6. Install Dependencies

```bash
npm install
```

### 7. Build and Sync

```bash
npm run build
npx cap sync android
```

### 8. Open in Android Studio

```bash
npx cap open android
```

Build and run the app from Android Studio on a physical device or emulator.

## How It Works

### Channel ID Pattern

Channels are created with IDs following this pattern:
- Custom sounds: `"custom_sound_channel_" + soundName`
  - Example: `"custom_sound_channel_ding"`, `"custom_sound_channel_shockding"`
- Fallback: `"custom_sound_channel"` (matches AndroidManifest default)
- Default: `"default"`

**CRITICAL**: The channel ID pattern must match between MainActivity and CustomFirebaseMessagingService.

### Notification Flow

1. **App Startup** (MainActivity.onCreate):
   - Creates channels for all sounds in `CUSTOM_SOUNDS` array
   - Sets sound URI on each channel
   - Android caches these URIs

2. **Notification Received** (CustomFirebaseMessagingService.onMessageReceived):
   - Extracts sound name from FCM payload (`androidSound` or `sound` field)
   - Determines channel ID: `"custom_sound_channel_" + soundName`
   - Reuses pre-created channel (if exists) or creates new one
   - Sets sound URI on notification builder
   - Displays notification

3. **When App is Closed**:
   - Android uses pre-created channel with cached sound URI
   - Custom sound plays correctly even on first notification

## FCM Payload Format

Send notifications with this payload structure:

```json
{
  "data": {
    "title": "Notification Title",
    "body": "Notification Body",
    "androidSound": "ding"
  }
}
```

Or use the `sound` field:

```json
{
  "data": {
    "title": "Notification Title",
    "body": "Notification Body",
    "sound": "shockding"
  }
}
```

**Priority**: `androidSound` takes precedence over `sound`.

### Using Firebase Console

1. Go to Firebase Console > Cloud Messaging
2. Click "Send your first message"
3. Enter notification title and body
4. Click "Send test message"
5. Enter your FCM token (displayed in the app)
6. Click "Test"

### Using FCM API

Send a POST request to `https://fcm.googleapis.com/v1/projects/YOUR_PROJECT_ID/messages` with:

```json
{
  "message": {
    "token": "YOUR_FCM_TOKEN",
    "data": {
      "title": "Test Notification",
      "body": "This is a test notification with custom sound",
      "androidSound": "ding"
    }
  }
}
```

## Project Structure

```
kyriosxaris/
├── android/                          # Android native project
│   └── app/
│       └── src/
│           └── main/
│               ├── res/
│               │   └── raw/          # Custom sound files go here
│               ├── java/
│               │   └── gr/kyrios/xaris/
│               │       ├── MainActivity.java              # Pre-creates channels
│               │       ├── CustomFirebaseMessagingService.java  # Handles notifications
│               │       └── SoundUriHelper.java            # Sound URI utility
│               └── AndroidManifest.xml
├── src/
│   ├── app/
│   │   ├── push-notification.service.ts  # Push notification logic
│   │   ├── app.component.ts              # App initialization
│   │   └── home/
│   │       ├── home.page.ts              # UI logic
│   │       └── home.page.html            # UI template
│   └── environments/
│       ├── environment.ts                # Firebase config (dev)
│       └── environment.prod.ts            # Firebase config (prod)
└── capacitor.config.ts                   # Capacitor configuration
```

## Key Files

- **MainActivity**: `android/app/src/main/java/gr/kyrios/xaris/MainActivity.java`
  - Pre-creates notification channels with sound URIs at startup
  - Critical for custom sounds to work when app is closed

- **CustomFirebaseMessagingService**: `android/app/src/main/java/gr/kyrios/xaris/CustomFirebaseMessagingService.java`
  - Handles incoming FCM notifications
  - Reuses pre-created channels from MainActivity
  - Sets custom sound URIs on notifications

- **SoundUriHelper**: `android/app/src/main/java/gr/kyrios/xaris/SoundUriHelper.java`
  - Utility for consistent sound URI generation
  - Validates sound files exist in resources

- **Push Notification Service**: `src/app/push-notification.service.ts`
  - Handles FCM registration
  - Manages notification listeners
  - Tracks notification history

- **AndroidManifest**: `android/app/src/main/AndroidManifest.xml`
  - Declares notification permissions
  - Configures FCM service
  - Sets default notification channel ID

## Key Implementation Details

### Why Pre-Creation is Critical

- **Without pre-creation**: First notification fails when app is closed because Android can't access resources
- **With pre-creation**: Sound URIs are cached in channels, accessible even when app is closed

### Channel Reuse Logic

`CustomFirebaseMessagingService.createNotificationChannel()`:
1. Checks if channel exists (pre-created by MainActivity)
2. Compares sound URI
3. Reuses if match, recreates if different
4. This ensures pre-created channels are used when available

### Sound URI Format

Sound URIs follow this format:
```
android.resource://[package-name]/raw/[sound-name]
```

Example: `android.resource://gr.kyrios.xaris/raw/ding`

## Troubleshooting

### Custom Sound Doesn't Play

1. **Check sound file exists**: Verify file is in `res/raw/` directory
2. **Check sound name**: Ensure it matches exactly (case-sensitive, no extension)
3. **Check CUSTOM_SOUNDS array**: Sound must be listed in MainActivity
4. **Check channel ID**: Must match pattern `"custom_sound_channel_" + soundName`
5. **Check FCM payload**: Must include `androidSound` or `sound` field

### Notifications Not Received

1. **Check AndroidManifest**: FCM service must be declared
2. **Check default channel**: Must match `CUSTOM_CHANNEL_ID` in MainActivity
3. **Check permissions**: `POST_NOTIFICATIONS` permission required (Android 13+)
4. **Check FCM token**: Ensure token is valid and registered
5. **Verify `google-services.json`**: Must be in `android/app/` directory

### Sound Plays Default Instead of Custom

1. **Check sound file**: Verify file exists and name matches
2. **Check channel**: Verify channel was created with correct sound URI
3. **Check notification builder**: Sound URI must be set on builder
4. **Check logs**: Look for "Sound file not found" warnings

### Token Not Generated

1. **Check Firebase configuration**: Verify `environment.ts` has correct config
2. **Verify app is running**: Must be on physical device or emulator
3. **Check console logs**: Look for registration errors
4. **Ensure internet connection**: Required for FCM registration

## Testing Checklist

- [ ] App starts without crashes
- [ ] Notification channels created (check Android Settings > Apps > Your App > Notifications)
- [ ] Notification received when app is open
- [ ] Custom sound plays when app is open
- [ ] Notification received when app is closed
- [ ] Custom sound plays when app is closed (first time) ⭐ **Critical test**
- [ ] Custom sound plays when app is closed (subsequent times)
- [ ] Different sounds work correctly
- [ ] Default sound works when no custom sound specified

## Adding New Sounds

1. Add sound file to `android/app/src/main/res/raw/`
2. Add sound name (without extension) to `CUSTOM_SOUNDS` array in MainActivity
3. Rebuild app
4. Send notification with `androidSound` or `sound` field set to new sound name

## Important Notes

- **Channel IDs are immutable**: Once created, channels cannot be modified. If you need to change a sound, delete the channel first (handled automatically in code)
- **Sound names are case-sensitive**: `"Ding"` and `"ding"` are different
- **File extensions are ignored**: Pass sound name without extension in FCM payload
- **Pre-creation is essential**: Don't skip MainActivity channel creation - it's critical for closed app functionality
- **Package name consistency**: Ensure package names match across all Java files and AndroidManifest

## Copying to Another Project

To copy this notification system to another project:

1. Copy the three Java files:
   - `MainActivity.java`
   - `CustomFirebaseMessagingService.java`
   - `SoundUriHelper.java`

2. Update package names in all files

3. Update `CUSTOM_SOUNDS` array in MainActivity with your sound files

4. Add FCM service to AndroidManifest.xml (see setup section)

5. Ensure MainActivity extends the correct base class for your framework

6. Update notification icon in CustomFirebaseMessagingService

See the code comments for detailed implementation notes.

## Dependencies

- Firebase Cloud Messaging (FCM)
- AndroidX Core Library
- Android API Level 26+ (Android 8.0+) for notification channels
- Ionic/Capacitor (for this project)

## Additional Resources

- [Ionic Documentation](https://ionicframework.com/docs)
- [Capacitor Push Notifications](https://capacitorjs.com/docs/apis/push-notifications)
- [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging)
- [Android Notification Channels](https://developer.android.com/develop/ui/views/notifications/channels)

## License

This project is open source and available under the MIT License.
