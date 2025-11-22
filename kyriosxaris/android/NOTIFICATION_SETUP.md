# Android Custom Sound Notifications Setup Guide

This guide explains how to implement custom sound notifications for Android that work even when the app is closed. This solution solves the critical issue where custom sounds don't play on the first notification received while the app is closed.

## Problem Solved

When Android receives a notification while the app is completely closed, it cannot access app resources (sound files) until the app has been initialized at least once. This causes the first notification with a custom sound to fail silently.

**Solution**: Pre-create notification channels with sound URIs at app startup. Android caches these URIs, allowing custom sounds to play even when the app is closed.

## Architecture Overview

The solution consists of three key components:

1. **MainActivity** - Pre-creates notification channels at app startup
2. **CustomFirebaseMessagingService** - Handles incoming notifications and reuses pre-created channels
3. **SoundUriHelper** - Utility for consistent sound URI generation

## Files to Copy

Copy these files to your Android project:

```
android/app/src/main/java/[your-package]/
├── MainActivity.java
├── CustomFirebaseMessagingService.java
└── SoundUriHelper.java
```

## Step-by-Step Setup

### 1. Add Sound Files

Place your custom sound files in:

```
android/app/src/main/res/raw/
```

Supported formats: `.mp3`, `.wav`, `.ogg`

**Important**: File names must be lowercase with underscores (e.g., `ding.wav`, `custom_sound.mp3`)

### 2. Update Package Names

Replace `gr.kyrios.xaris` with your app's package name in all three files:

- `MainActivity.java`
- `CustomFirebaseMessagingService.java`
- `SoundUriHelper.java`

### 3. Configure MainActivity

**CRITICAL**: Update the `CUSTOM_SOUNDS` array with your sound file names (without extension):

```java
private static final String[] CUSTOM_SOUNDS = {"ding", "custom_sound", "shockding"};
```

Add all your custom sound files here. This list determines which channels are pre-created at startup.

### 4. Update AndroidManifest.xml

Add the FCM service declaration:

```xml
<service
    android:name=".CustomFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>

<!-- Default notification channel metadata -->
<meta-data
    android:name="com.google.firebase.messaging.default_notification_channel_id"
    android:value="custom_sound_channel" />
```

**Important**: The `default_notification_channel_id` must match `CUSTOM_CHANNEL_ID` in MainActivity.java.

### 5. Update Notification Icon

In `CustomFirebaseMessagingService.java`, replace:

```java
.setSmallIcon(android.R.drawable.ic_dialog_info)
```

With your app's notification icon:

```java
.setSmallIcon(R.drawable.your_notification_icon)
```

### 6. Ensure MainActivity Extends Correct Base Class

If you're using Capacitor/Ionic, ensure MainActivity extends `BridgeActivity`:

```java
public class MainActivity extends BridgeActivity
```

For React Native, it should extend `MainActivity` or `ReactActivity`. Adjust accordingly.

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

Example: `android.resource://com.example.app/raw/ding`

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

### Sound Plays Default Instead of Custom

1. **Check sound file**: Verify file exists and name matches
2. **Check channel**: Verify channel was created with correct sound URI
3. **Check notification builder**: Sound URI must be set on builder
4. **Check logs**: Look for "Sound file not found" warnings

## Testing Checklist

- [ ] App starts without crashes
- [ ] Notification channels created (check Android Settings > Apps > Your App > Notifications)
- [ ] Notification received when app is open
- [ ] Custom sound plays when app is open
- [ ] Notification received when app is closed
- [ ] Custom sound plays when app is closed (first time)
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

## Dependencies

- Firebase Cloud Messaging (FCM)
- AndroidX Core Library
- Android API Level 26+ (Android 8.0+) for notification channels

## License

This implementation is provided as-is. Adapt as needed for your project.
