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

public class CustomFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "CustomFCMService";
    private static final String CUSTOM_CHANNEL_ID = "custom_sound_channel";
    private static final String DEFAULT_CHANNEL_ID = "default";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Don't call super.onMessageReceived() - we handle the notification ourselves
        // This ensures we have full control over the notification display and sound

        Log.d(TAG, "From: " + remoteMessage.getFrom());
        Log.d(TAG, "Message data: " + remoteMessage.getData());

        // Handle both notification payload and data-only messages
        String title = null;
        String body = null;
        String sound = null;
        String channelId = null;

        // PRIORITY: Check data payload FIRST (more reliable for custom sounds)
        if (remoteMessage.getData() != null && remoteMessage.getData().size() > 0) {
            title = remoteMessage.getData().get("title");
            body = remoteMessage.getData().get("body");
            // Prioritize androidSound, then sound from data payload
            sound = remoteMessage.getData().get("androidSound");
            if (sound == null) {
                sound = remoteMessage.getData().get("sound");
            }
            channelId = remoteMessage.getData().get("channelId");
            Log.d(TAG, "Data payload - sound: " + sound + ", androidSound: " + remoteMessage.getData().get("androidSound"));
        }

        // Fallback to notification payload if data payload doesn't have title/body
        if (remoteMessage.getNotification() != null) {
            if (title == null) title = remoteMessage.getNotification().getTitle();
            if (body == null) body = remoteMessage.getNotification().getBody();
            // Only use notification sound if we don't have one from data payload
            if (sound == null) {
                sound = remoteMessage.getNotification().getSound();
            }
            if (channelId == null) channelId = remoteMessage.getNotification().getChannelId();
            Log.d(TAG, "Notification payload - sound: " + remoteMessage.getNotification().getSound());
        }

        // Validate we have at least title and body
        if (title == null || body == null) {
            Log.w(TAG, "Missing title or body, skipping notification");
            return;
        }

        Log.d(TAG, "Final Title: " + title);
        Log.d(TAG, "Final Body: " + body);
        Log.d(TAG, "Final Sound: " + sound);
        Log.d(TAG, "Final Channel: " + channelId);

        // Use the sound from notification or data payload
        String finalSound = sound;
        
        // Determine channel ID
        // Use unique channel per sound to avoid conflicts and ensure correct sound plays
        String finalChannelId;
        if (channelId != null) {
            finalChannelId = channelId;
        } else if (finalSound != null && !finalSound.equals("default")) {
            // Create unique channel ID for each custom sound (e.g., "custom_sound_channel_shockding")
            finalChannelId = CUSTOM_CHANNEL_ID + "_" + finalSound;
        } else {
            finalChannelId = DEFAULT_CHANNEL_ID;
        }

        Log.d(TAG, "Using channel: " + finalChannelId + " with sound: " + finalSound);

        // Create or get notification channel with the specified sound
        createNotificationChannel(finalChannelId, finalSound);

        // Build notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, finalChannelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with your app icon
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Set sound if specified (this overrides channel default)
        if (finalSound != null && !finalSound.equals("default")) {
            Uri soundUri = getSoundUri(finalSound);
            if (soundUri != null) {
                notificationBuilder.setSound(soundUri);
                Log.d(TAG, "Setting notification sound: " + finalSound + " -> " + soundUri);
            } else {
                Log.w(TAG, "Sound URI is null for: " + finalSound);
            }
        } else {
            Log.d(TAG, "Using default sound or no sound specified");
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

    private void createNotificationChannel(String channelId, String soundName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = 
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            // Always delete existing channel if it exists (channels can't be modified)
            // This ensures we always use the correct sound
            NotificationChannel existingChannel = notificationManager.getNotificationChannel(channelId);
            if (existingChannel != null) {
                Uri currentSound = existingChannel.getSound();
                Uri newSoundUri = null;
                if (soundName != null && !soundName.equals("default")) {
                    newSoundUri = getSoundUri(soundName);
                }
                
                Log.d(TAG, "Channel exists. Current sound URI: " + currentSound + ", New sound: " + soundName + " -> " + newSoundUri);
                
                // Delete channel if sound is different or if we need to change it
                boolean needsRecreation = false;
                if (newSoundUri != null) {
                    if (currentSound == null || !currentSound.equals(newSoundUri)) {
                        needsRecreation = true;
                        Log.d(TAG, "Sound changed, deleting channel to recreate");
                    } else {
                        Log.d(TAG, "Sound matches, reusing existing channel");
                    }
                } else if (currentSound != null) {
                    // Need to remove sound
                    needsRecreation = true;
                    Log.d(TAG, "Need to remove sound, deleting channel to recreate");
                }
                
                if (needsRecreation) {
                    notificationManager.deleteNotificationChannel(channelId);
                    Log.d(TAG, "Deleted channel: " + channelId);
                    // Wait a bit for deletion to complete
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                } else {
                    // Channel exists and sound matches, reuse it
                    Log.d(TAG, "Reusing existing channel with matching sound");
                    return;
                }
            }

            // Create channel (either new or after deletion)
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
                Uri soundUri = getSoundUri(soundName);
                if (soundUri != null) {
                    AudioAttributes audioAttributes = new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build();
                    channel.setSound(soundUri, audioAttributes);
                    Log.d(TAG, "Created channel with sound: " + soundName + " -> " + soundUri);
                } else {
                    Log.w(TAG, "Could not find sound file: " + soundName + ", creating channel without sound");
                }
            } else {
                Log.d(TAG, "Creating channel without custom sound (using default)");
            }

            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "Created notification channel: " + channelId + " with sound: " + (soundName != null ? soundName : "default"));
        }
    }

    private Uri getSoundUri(String soundName) {
        if (soundName == null || soundName.isEmpty() || soundName.equals("default")) {
            Log.d(TAG, "Sound name is null, empty, or 'default'");
            return null;
        }

        // Remove file extension if present
        String cleanSoundName = soundName;
        if (cleanSoundName.contains(".")) {
            cleanSoundName = cleanSoundName.substring(0, cleanSoundName.lastIndexOf("."));
        }

        Log.d(TAG, "Looking for sound file: " + cleanSoundName + " (original: " + soundName + ")");

        // Build URI for sound in res/raw/
        String packageName = getPackageName();
        
        // Verify the resource exists FIRST
        int resourceId = getResources().getIdentifier(cleanSoundName, "raw", packageName);
        if (resourceId == 0) {
            Log.w(TAG, "Sound file not found in res/raw/: " + cleanSoundName);
            Log.w(TAG, "Available sounds should be: ding, custom_sound, shockding");
            return null;
        }

        Uri soundUri = Uri.parse("android.resource://" + packageName + "/raw/" + cleanSoundName);
        Log.d(TAG, "Found sound file: " + cleanSoundName + " -> " + soundUri);
        return soundUri;
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        // Send token to your server if needed
    }
}

