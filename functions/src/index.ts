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

    // Log request details (visible in emulator terminal)
    console.log("=== Push Notification Request ===");
    console.log("Request body:", JSON.stringify(req.body, null, 2));
    console.log("Sound parameters:", {
      soundType: soundType || "default (not specified)",
      sound: sound || "not specified",
      androidSound: androidSound || "not specified",
      iosSound: iosSound || "not specified",
      channelId: channelId || "not specified",
    });

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
    console.log("Notification sound type:", notificationSoundType);

    // Determine Android sound FIRST (needed for channel selection)
    let finalAndroidSound: string | undefined;
    if (notificationSoundType === "none") {
      finalAndroidSound = undefined; // Silent notification
      console.log("Android sound: SILENT (soundType: none)");
    } else if (androidSound) {
      finalAndroidSound = androidSound; // Android-specific sound (highest priority)
      console.log(
        "Android sound:",
        finalAndroidSound,
        "(from androidSound parameter)"
      );
    } else if (sound) {
      finalAndroidSound = sound; // General sound parameter (second priority)
      console.log(
        "Android sound:",
        finalAndroidSound,
        "(from sound parameter)"
      );
    } else if (notificationSoundType === "custom") {
      finalAndroidSound = "custom_sound"; // Default custom sound (fallback)
      console.log(
        "Android sound:",
        finalAndroidSound,
        "(default for soundType: custom)"
      );
    } else {
      // "default" or any other value
      finalAndroidSound = "default";
      console.log(
        "Android sound:",
        finalAndroidSound,
        "(default system sound)"
      );
    }

    // Determine Android channel ID (based on sound type and actual sound)
    // Check if a custom sound is being specified (not default)
    const hasCustomSound = finalAndroidSound && finalAndroidSound !== "default";

    let androidChannelId: string;
    if (channelId) {
      // Explicitly provided channelId takes precedence
      androidChannelId = channelId;
      console.log(
        "Android channel:",
        androidChannelId,
        "(explicitly provided)"
      );
    } else if (notificationSoundType === "custom" || hasCustomSound) {
      // Use unique channel per sound to avoid conflicts (matches Android service logic)
      // Format: "custom_sound_channel_<soundname>"
      androidChannelId = `custom_sound_channel_${finalAndroidSound}`;
      console.log(
        "Android channel:",
        androidChannelId,
        "(custom sound detected, unique channel per sound)"
      );
    } else if (notificationSoundType === "none") {
      androidChannelId = "default"; // Still need a channel, but no sound
      console.log(
        "Android channel:",
        androidChannelId,
        "(silent notification)"
      );
    } else {
      // "default" or any other value
      androidChannelId = "default";
      console.log("Android channel:", androidChannelId, "(default channel)");
    }

    // Determine iOS sound
    let finalIosSound: string | undefined;
    if (notificationSoundType === "none") {
      finalIosSound = undefined; // Silent notification
      console.log("iOS sound: SILENT (soundType: none)");
    } else if (iosSound) {
      finalIosSound = iosSound; // iOS-specific sound
      console.log("iOS sound:", finalIosSound, "(from iosSound parameter)");
    } else if (sound) {
      finalIosSound = sound; // General sound parameter
      console.log("iOS sound:", finalIosSound, "(from sound parameter)");
    } else if (notificationSoundType === "custom") {
      finalIosSound = "shockding"; // Default custom sound
      console.log(
        "iOS sound:",
        finalIosSound,
        "(default for soundType: custom)"
      );
    } else {
      // "default" or any other value
      finalIosSound = "default";
      console.log("iOS sound:", finalIosSound, "(default system sound)");
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

    // Initialize data object
    message.data = {};

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

    // Add sound to data payload for Android custom handling (if custom sound is specified)
    if (finalAndroidSound && finalAndroidSound !== "default") {
      message.data.sound = finalAndroidSound;
      message.data.androidSound = finalAndroidSound;
      console.log(
        "Added to data payload - sound:",
        finalAndroidSound,
        ", androidSound:",
        finalAndroidSound
      );
    } else {
      console.log(
        "No custom sound added to data payload (using default or silent)"
      );
    }

    console.log("=== FCM Message Payload ===");
    console.log(
      "Android notification sound:",
      message.android?.notification?.sound || "not set"
    );
    console.log(
      "Android notification channel:",
      message.android?.notification?.channelId || "not set"
    );
    console.log("Data payload sound:", message.data?.sound || "not set");
    console.log(
      "Data payload androidSound:",
      message.data?.androidSound || "not set"
    );
    console.log("iOS sound:", message.apns?.payload?.aps?.sound || "not set");

    // Send the notification
    console.log("=== Sending FCM Message ===");
    const response = await admin.messaging().send(message);

    console.log("Successfully sent message. Message ID:", response);
    console.log("=== Notification Sent Successfully ===");

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
