# Firebase Cloud Functions - Push Notifications

This directory contains Firebase Cloud Functions for sending push notifications via FCM (Firebase Cloud Messaging).

## Setup

1. Install dependencies:

```bash
cd functions
npm install
```

2. Build the TypeScript code:

```bash
npm run build
```

## Deployment

Deploy the function to Firebase:

```bash
npm run deploy
```

Or from the project root:

```bash
firebase deploy --only functions
```

## Local Development

Run the function locally using the Firebase emulator:

```bash
npm run serve
```

## Function: sendPushNotification

### Endpoint

After deployment, the function will be available at:

```
https://<region>-<project-id>.cloudfunctions.net/sendPushNotification
```

### Request Format

**Method:** POST

**Headers:**

```
Content-Type: application/json
```

**Body:**

```json
{
  "token": "FCM_TOKEN_HERE",
  "title": "Notification Title",
  "body": "Notification Body",
  "data": {
    "key1": "value1",
    "key2": "value2"
  },
  "soundType": "default",
  "sound": "sound_name",
  "androidSound": "android_sound",
  "iosSound": "ios_sound",
  "channelId": "channel_id"
}
```

### Required Fields

- `token`: The FCM token of the device to send the notification to
- `title`: The title of the notification
- `body`: The body text of the notification

### Optional Fields

- `data`: Custom key-value pairs to send with the notification (all values will be converted to strings)
- `soundType`: String (default: `"default"`) - Sound type: `"default"` (system default sound), `"custom"` (custom sound), or `"none"` (silent notification)
- `sound`: String - Sound file name (without extension) for both Android and iOS. Overrides `soundType` defaults
- `androidSound`: String - Android-specific sound file name (without extension). Overrides `sound` and `soundType` defaults
- `iosSound`: String - iOS-specific sound name. Overrides `sound` and `soundType` defaults
- `channelId`: String - Android notification channel ID. Defaults based on `soundType`: `"default"` for default/none, `"custom_sound_channel"` for custom

### Response Format

**Success (200):**

```json
{
  "success": true,
  "messageId": "projects/kyriosxaris/messages/0:1234567890",
  "message": "Notification sent successfully"
}
```

**Error (400/500):**

```json
{
  "error": "Error message",
  "details": "Detailed error information"
}
```

### Example Usage

**Send notification with default (normal) sound:**

```bash
curl -X POST https://<region>-<project-id>.cloudfunctions.net/sendPushNotification \
  -H "Content-Type: application/json" \
  -d '{
    "token": "YOUR_FCM_TOKEN",
    "title": "Hello",
    "body": "This is a notification with default sound",
    "soundType": "default"
  }'
```

**Send notification with custom sound:**

```bash
curl -X POST https://<region>-<project-id>.cloudfunctions.net/sendPushNotification \
  -H "Content-Type: application/json" \
  -d '{
    "token": "YOUR_FCM_TOKEN",
    "title": "Hello",
    "body": "This is a notification with custom sound",
    "soundType": "custom",
    "sound": "my_custom_sound"
  }'
```

**Send silent notification (no sound):**

```bash
curl -X POST https://<region>-<project-id>.cloudfunctions.net/sendPushNotification \
  -H "Content-Type: application/json" \
  -d '{
    "token": "YOUR_FCM_TOKEN",
    "title": "Hello",
    "body": "This is a silent notification",
    "soundType": "none"
  }'
```

**Send notification with different sounds for Android and iOS:**

```bash
curl -X POST https://<region>-<project-id>.cloudfunctions.net/sendPushNotification \
  -H "Content-Type: application/json" \
  -d '{
    "token": "YOUR_FCM_TOKEN",
    "title": "Hello",
    "body": "Different sounds for Android and iOS",
    "androidSound": "android_notification",
    "iosSound": "ios_notification.caf"
  }'
```

Using JavaScript/TypeScript:

**Default (normal) sound notification:**

```typescript
const response = await fetch(
  "https://<region>-<project-id>.cloudfunctions.net/sendPushNotification",
  {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      token: "YOUR_FCM_TOKEN",
      title: "Hello",
      body: "This is a notification with default sound",
      soundType: "default", // or omit, defaults to "default"
      data: {
        userId: "123",
        type: "message",
      },
    }),
  }
);

const result = await response.json();
console.log(result);
```

**Custom sound notification:**

```typescript
const response = await fetch(
  "https://<region>-<project-id>.cloudfunctions.net/sendPushNotification",
  {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      token: "YOUR_FCM_TOKEN",
      title: "Hello",
      body: "This is a notification with custom sound",
      soundType: "custom",
      sound: "my_custom_sound",
      data: {
        userId: "123",
        type: "message",
      },
    }),
  }
);

const result = await response.json();
console.log(result);
```

**Different sounds for Android and iOS:**

```typescript
const response = await fetch(
  "https://<region>-<project-id>.cloudfunctions.net/sendPushNotification",
  {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      token: "YOUR_FCM_TOKEN",
      title: "Hello",
      body: "Different sounds per platform",
      androidSound: "android_notification",
      iosSound: "ios_notification",
      channelId: "custom_channel",
      data: {
        userId: "123",
        type: "message",
      },
    }),
  }
);

const result = await response.json();
console.log(result);
```

## Notes

- The function includes CORS headers to allow cross-origin requests
- Invalid or unregistered FCM tokens will return a 400 error
- The function supports both Android and iOS platforms
- **Sound Type Priority**: Explicit `androidSound`/`iosSound` > `sound` > `soundType` defaults
- **Sound Types**:
  - `"default"`: Uses system default sound (Android: `"default"` channel, iOS: `"default"`)
  - `"custom"`: Uses custom sound (Android: `"custom_sound_channel"`, iOS: `"custom_sound"`)
  - `"none"`: Silent notification (no sound, but still shows notification)
- **Default behavior**: If `soundType` is not specified, it defaults to `"default"` (normal system sound)
- **Platform-specific sounds**: Use `androidSound` and `iosSound` to specify different sounds for each platform
- **Channel ID**: Android requires a notification channel. Defaults to `"default"` for normal sounds, `"custom_sound_channel"` for custom sounds
- All custom data values are automatically converted to strings (FCM requirement)
- Sound file names should not include file extensions (e.g., use `"notification"` not `"notification.mp3"`)
