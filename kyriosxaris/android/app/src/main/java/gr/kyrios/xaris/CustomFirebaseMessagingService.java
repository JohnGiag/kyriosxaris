package gr.kyrios.xaris;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * CustomFirebaseMessagingService - Handles FCM push notifications with custom sounds.
 * 
 * CRITICAL FLOW:
 * 1. Receives notification from FCM
 * 2. Extracts sound name from data payload (androidSound or sound field)
 * 3. Determines channel ID: "custom_sound_channel_" + soundName (or uses provided channelId)
 * 4. Reuses pre-created channel from MainActivity (if exists) or creates new one
 * 5. Builds notification with custom sound URI
 * 
 * The channel reuse is critical - MainActivity pre-creates channels so Android
 * has cached sound URIs even when the app is closed.
 */
public class CustomFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "CustomFCMService";
    private static final String CUSTOM_CHANNEL_ID = "custom_sound_channel";
    private static final String DEFAULT_CHANNEL_ID = "default";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Don't call super.onMessageReceived() - we handle the notification ourselves
        // This ensures we have full control over the notification display and sound

        // Extract notification data from payload
        String title = null;
        String body = null;
        String sound = null;
        String channelId = null;

        // CRITICAL: Check data payload FIRST (more reliable for custom sounds)
        // Data payload works even when app is closed, notification payload may not
        if (remoteMessage.getData() != null && remoteMessage.getData().size() > 0) {
            title = remoteMessage.getData().get("title");
            body = remoteMessage.getData().get("body");
            // Prioritize androidSound, then sound from data payload
            sound = remoteMessage.getData().get("androidSound");
            if (sound == null) {
                sound = remoteMessage.getData().get("sound");
            }
            channelId = remoteMessage.getData().get("channelId");
        }

        // Fallback to notification payload if data payload doesn't have title/body
        if (remoteMessage.getNotification() != null) {
            if (title == null) title = remoteMessage.getNotification().getTitle();
            if (body == null) body = remoteMessage.getNotification().getBody();
            if (sound == null) {
                sound = remoteMessage.getNotification().getSound();
            }
            if (channelId == null) channelId = remoteMessage.getNotification().getChannelId();
        }

        // Validate required fields
        if (title == null || body == null) {
            Log.w(TAG, "Missing title or body, skipping notification");
            return;
        }

        // CRITICAL: Determine channel ID - must match pattern from MainActivity
        // Pattern: "custom_sound_channel_" + soundName (e.g., "custom_sound_channel_ding")
        String finalChannelId;
        if (channelId != null) {
            finalChannelId = channelId;
        } else if (sound != null && !sound.equals("default")) {
            finalChannelId = CUSTOM_CHANNEL_ID + "_" + sound;
        } else {
            finalChannelId = DEFAULT_CHANNEL_ID;
        }

        // Ensure channel exists (reuses pre-created from MainActivity if available)
        createNotificationChannel(finalChannelId, sound);

        // Build notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, finalChannelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with your app icon
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // CRITICAL: Set sound URI on notification (overrides channel default)
        // This ensures the correct sound plays even if channel was created without sound
        if (sound != null && !sound.equals("default")) {
            Uri soundUri = SoundUriHelper.getSoundUri(this, sound);
            if (soundUri != null) {
                notificationBuilder.setSound(soundUri);
            } else {
                Log.w(TAG, "Sound file not found: " + sound);
            }
        }

        // Add click action
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, 
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);

        // Show notification
        NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notificationBuilder.build());
    }

    /**
     * Creates or reuses a notification channel for the given sound.
     * 
     * CRITICAL: This method checks if a channel was pre-created by MainActivity.
     * If it exists with the correct sound, it reuses it (channels can't be modified).
     * This is essential for custom sounds to work when the app is closed.
     * 
     * @param channelId The channel ID (must match pattern from MainActivity)
     * @param soundName The sound file name (without extension)
     */
    private void createNotificationChannel(String channelId, String soundName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = 
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            // CRITICAL: Check if channel was pre-created by MainActivity
            // If it exists with matching sound, reuse it (channels are immutable)
            NotificationChannel existingChannel = notificationManager.getNotificationChannel(channelId);
            if (existingChannel != null) {
                Uri currentSound = existingChannel.getSound();
                Uri newSoundUri = null;
                if (soundName != null && !soundName.equals("default")) {
                    newSoundUri = SoundUriHelper.getSoundUri(this, soundName);
                }
                
                // Only recreate if sound needs to change
                boolean needsRecreation = false;
                if (newSoundUri != null) {
                    if (currentSound == null || !currentSound.equals(newSoundUri)) {
                        needsRecreation = true;
                    }
                } else if (currentSound != null) {
                    needsRecreation = true;
                }
                
                if (needsRecreation) {
                    notificationManager.deleteNotificationChannel(channelId);
                    try {
                        Thread.sleep(100); // Wait for deletion to complete
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                } else {
                    // Reuse pre-created channel - this is the critical path for closed app
                    return;
                }
            }

            // Create new channel if it doesn't exist or was deleted
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Notification Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notification channel");
            channel.enableLights(true);
            channel.enableVibration(true);

            // Set sound if specified
            if (soundName != null && !soundName.equals("default")) {
                Uri soundUri = SoundUriHelper.getSoundUri(this, soundName);
                if (soundUri != null) {
                    AudioAttributes audioAttributes = new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build();
                    channel.setSound(soundUri, audioAttributes);
                } else {
                    Log.w(TAG, "Sound file not found: " + soundName);
                }
            }

            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        // Send token to your server if needed
    }
}

