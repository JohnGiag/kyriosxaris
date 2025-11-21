# Ionic Angular FCM Push Notifications App

An Ionic Angular application configured to receive Firebase Cloud Messaging (FCM) push notifications with custom sound support for Android devices.

## Features

- ✅ Firebase Cloud Messaging (FCM) integration
- ✅ Custom notification sounds
- ✅ Android notification channels
- ✅ Notification history tracking
- ✅ FCM token display and copying
- ✅ Foreground and background notification handling

## Prerequisites

- Node.js and npm installed
- Ionic CLI installed (`npm install -g @ionic/cli`)
- Android Studio installed (for Android development)
- Firebase project with FCM enabled
- Google Services JSON file (`google-services.json`)

## Setup Instructions

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
3. File names should be lowercase with underscores (e.g., `custom_sound.mp3`)
4. Update `MainActivity.java` to reference your sound file name (without extension)

Example:

- Sound file: `android/app/src/main/res/raw/notification.mp3`
- In `MainActivity.java`, change `custom_sound` to `notification`

### 4. Install Dependencies

```bash
npm install
```

### 5. Build the App

```bash
npm run build
```

### 6. Sync Capacitor

```bash
npx cap sync android
```

### 7. Open in Android Studio

```bash
npx cap open android
```

### 8. Run the App

Build and run the app from Android Studio on a physical device or emulator.

## Testing Push Notifications

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
    "notification": {
      "title": "Test Notification",
      "body": "This is a test notification with custom sound"
    },
    "android": {
      "notification": {
        "sound": "custom_sound",
        "channel_id": "custom_sound_channel"
      }
    }
  }
}
```

**Important Notes:**

- The `sound` field must match your sound file name (without extension)
- The `channel_id` must match the channel ID in `MainActivity.java` (`custom_sound_channel`)
- For API requests, you'll need an OAuth 2.0 access token

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
│               │   └── io/ionic/starter/
│               │       └── MainActivity.java  # Notification channel setup
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
│       └── environment.prod.ts           # Firebase config (prod)
└── capacitor.config.ts                   # Capacitor configuration
```

## Key Files

- **Push Notification Service**: `src/app/push-notification.service.ts`

  - Handles FCM registration
  - Manages notification listeners
  - Tracks notification history

- **MainActivity**: `android/app/src/main/java/io/ionic/starter/MainActivity.java`

  - Creates notification channel with custom sound
  - Configures audio attributes

- **AndroidManifest**: `android/app/src/main/AndroidManifest.xml`
  - Declares notification permissions
  - Configures FCM service

## Troubleshooting

### Notifications not received

- Verify `google-services.json` is in `android/app/` directory
- Check that notification permissions are granted
- Ensure FCM token is valid
- Verify notification channel is created (check logs)

### Custom sound not playing

- Verify sound file is in `android/app/src/main/res/raw/`
- Check sound file name matches in `MainActivity.java` and FCM payload
- Ensure notification channel is properly configured
- Test with a simple sound file first

### Token not generated

- Check Firebase configuration in `environment.ts`
- Verify app is running on a physical device or emulator
- Check console logs for registration errors
- Ensure internet connection is available

## Additional Resources

- [Ionic Documentation](https://ionicframework.com/docs)
- [Capacitor Push Notifications](https://capacitorjs.com/docs/apis/push-notifications)
- [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging)
- [Android Notification Channels](https://developer.android.com/develop/ui/views/notifications/channels)

## License

This project is open source and available under the MIT License.
