import * as functions from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

// Initialize Firebase Admin SDK
admin.initializeApp();

/**
 * Cloud Function to send push notifications
 *
 * Expected request body:
 * {
 *   "token": "FCM_TOKEN_HERE",
 *   "title": "Notification Title",
 *   "body": "Notification Body",
 *   "data": { "key": "value" }, // optional
 *   "soundType": "default" | "custom" | "none", // optional, default: "default"
 *   "sound": "sound_name",       // optional - sound file name (without extension)
 *   "androidSound": "sound_name", // optional - Android-specific sound (overrides sound)
 *   "iosSound": "sound_name",     // optional - iOS-specific sound (overrides sound)
 *   "channelId": "channel_id"     // optional - Android notification channel ID
 * }
 *
 * Sound Types:
 * - "default": Uses system default sound (Android: "default" channel, iOS: "default")
 * - "custom": Uses custom sound (Android: "custom_sound_channel", iOS: custom sound)
 * - "none": Silent notification (no sound)
 * - If sound/channelId specified directly, they override soundType defaults
 */
export const sendPushNotification = functions.onRequest(async (req, res) => {
  // Set CORS headers
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Content-Type");

  // Handle preflight OPTIONS request
  if (req.method === "OPTIONS") {
    res.status(204).send("");
    return;
  }

  // Only allow POST requests
  if (req.method !== "POST") {
    res.status(405).json({ error: "Method not allowed. Use POST." });
    return;
  }

  try {
    const {
      token,
      title,
      body,
      data,
      soundType,
      sound,
      androidSound,
      iosSound,
      channelId,
    } = req.body;

    // Validate required fields
    if (!token) {
      res.status(400).json({ error: "FCM token is required" });
      return;
    }

    if (!title || !body) {
      res.status(400).json({ error: "Title and body are required" });
      return;
    }

    // Determine notification channel and sound based on soundType
    // Default to "default" if not specified
    const notificationSoundType = soundType || "default";

    // Determine Android channel ID
    let androidChannelId: string;
    if (channelId) {
      // Explicitly provided channelId takes precedence
      androidChannelId = channelId;
    } else if (notificationSoundType === "custom") {
      androidChannelId = "custom_sound_channel";
    } else if (notificationSoundType === "none") {
      androidChannelId = "default"; // Still need a channel, but no sound
    } else {
      // "default" or any other value
      androidChannelId = "default";
    }

    // Determine Android sound
    let finalAndroidSound: string | undefined;
    if (notificationSoundType === "none") {
      finalAndroidSound = undefined; // Silent notification
    } else if (androidSound) {
      finalAndroidSound = androidSound; // Android-specific sound
    } else if (sound) {
      finalAndroidSound = sound; // General sound parameter
    } else if (notificationSoundType === "custom") {
      finalAndroidSound = "custom_sound"; // Default custom sound
    } else {
      // "default" or any other value
      finalAndroidSound = "default";
    }

    // Determine iOS sound
    let finalIosSound: string | undefined;
    if (notificationSoundType === "none") {
      finalIosSound = undefined; // Silent notification
    } else if (iosSound) {
      finalIosSound = iosSound; // iOS-specific sound
    } else if (sound) {
      finalIosSound = sound; // General sound parameter
    } else if (notificationSoundType === "custom") {
      finalIosSound = "custom_sound"; // Default custom sound
    } else {
      // "default" or any other value
      finalIosSound = "default";
    }

    // Prepare the notification payload
    const message: admin.messaging.Message = {
      token: token,
      notification: {
        title: title,
        body: body,
      },
      android: {
        priority: "high" as const,
        notification: {
          ...(finalAndroidSound && { sound: finalAndroidSound }),
          channelId: androidChannelId,
        },
      },
      apns: {
        payload: {
          aps: {
            ...(finalIosSound && { sound: finalIosSound }),
          },
        },
      },
    };

    // Add custom data if provided
    if (data && typeof data === "object") {
      message.data = Object.keys(data).reduce(
        (acc: { [key: string]: string }, key) => {
          acc[key] = String(data[key]);
          return acc;
        },
        {}
      );
    }

    // Send the notification
    const response = await admin.messaging().send(message);

    console.log("Successfully sent message:", response);

    res.status(200).json({
      success: true,
      messageId: response,
      message: "Notification sent successfully",
    });
  } catch (error: any) {
    console.error("Error sending notification:", error);

    // Handle specific FCM errors
    if (
      error.code === "messaging/invalid-registration-token" ||
      error.code === "messaging/registration-token-not-registered"
    ) {
      res.status(400).json({
        error: "Invalid or unregistered FCM token",
        details: error.message,
      });
      return;
    }

    res.status(500).json({
      error: "Failed to send notification",
      details: error.message,
    });
  }
});
