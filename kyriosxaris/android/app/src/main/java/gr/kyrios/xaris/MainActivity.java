package gr.kyrios.xaris;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import com.getcapacitor.BridgeActivity;

/**
 * MainActivity - Pre-creates notification channels at app startup.
 * 
 * CRITICAL: This is essential for custom sounds to work when the app is closed.
 * When Android receives a notification while the app is closed, it cannot access
 * app resources (sound files) until the app has been initialized at least once.
 * By pre-creating channels with sound URIs at startup, Android caches these URIs
 * and can play custom sounds even when the app is completely closed.
 */
public class MainActivity extends BridgeActivity {
    private static final String TAG = "MainActivity";
    private static final String CUSTOM_CHANNEL_ID = "custom_sound_channel";
    private static final String CUSTOM_CHANNEL_NAME = "Custom Sound Notifications";
    private static final String CUSTOM_CHANNEL_DESCRIPTION = "Notifications with custom sound";
    
    private static final String DEFAULT_CHANNEL_ID = "default";
    private static final String DEFAULT_CHANNEL_NAME = "Default Notifications";
    private static final String DEFAULT_CHANNEL_DESCRIPTION = "Notifications with default system sound";

    /**
     * List of custom sound files (without extension) that should be pre-registered.
     * Add new sounds here and place corresponding files in res/raw/
     */
    private static final String[] CUSTOM_SOUNDS = {"ding", "custom_sound", "shockding"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createNotificationChannels();
    }

    /**
     * Pre-creates notification channels for all custom sounds at app startup.
     * 
     * This method:
     * 1. Creates a channel for each custom sound with ID pattern: "custom_sound_channel_" + soundName
     * 2. Sets the sound URI on each channel so Android caches it
     * 3. Creates a fallback channel matching AndroidManifest default
     * 4. Creates a default channel for standard notifications
     * 
     * Channel IDs must match the pattern used in CustomFirebaseMessagingService.
     */
    private void createNotificationChannels() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                
                // Pre-create channels for each custom sound
                // CRITICAL: Channel IDs must match pattern in CustomFirebaseMessagingService
                for (String soundName : CUSTOM_SOUNDS) {
                    try {
                        String channelId = CUSTOM_CHANNEL_ID + "_" + soundName;
                        Uri soundUri = SoundUriHelper.getSoundUri(this, soundName);
                        
                        if (soundUri != null) {
                            NotificationChannel channel = new NotificationChannel(
                                channelId,
                                CUSTOM_CHANNEL_NAME + " - " + soundName,
                                NotificationManager.IMPORTANCE_HIGH
                            );
                            channel.setDescription(CUSTOM_CHANNEL_DESCRIPTION + " (" + soundName + ")");
                            channel.enableLights(true);
                            channel.enableVibration(true);
                            
                            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .build();
                            channel.setSound(soundUri, audioAttributes);
                            
                            notificationManager.createNotificationChannel(channel);
                        } else {
                            Log.w(TAG, "Sound file not found: " + soundName);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating channel for sound: " + soundName, e);
                    }
                }
                
                // CRITICAL: Create fallback channel matching AndroidManifest default channel ID
                // This ensures notifications work if a specific sound channel isn't found
                try {
                    NotificationChannel fallbackChannel = new NotificationChannel(
                        CUSTOM_CHANNEL_ID,
                        CUSTOM_CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                    );
                    fallbackChannel.setDescription(CUSTOM_CHANNEL_DESCRIPTION);
                    fallbackChannel.enableLights(true);
                    fallbackChannel.enableVibration(true);
                    Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    AudioAttributes fallbackAudioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();
                    fallbackChannel.setSound(defaultSoundUri, fallbackAudioAttributes);
                    notificationManager.createNotificationChannel(fallbackChannel);
                } catch (Exception e) {
                    Log.e(TAG, "Error creating fallback channel", e);
                }
                
                // Create default channel for standard notifications
                try {
                    NotificationChannel defaultChannel = new NotificationChannel(
                        DEFAULT_CHANNEL_ID,
                        DEFAULT_CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                    );
                    defaultChannel.setDescription(DEFAULT_CHANNEL_DESCRIPTION);
                    defaultChannel.enableLights(true);
                    defaultChannel.enableVibration(true);
                    Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    AudioAttributes defaultAudioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();
                    defaultChannel.setSound(defaultSoundUri, defaultAudioAttributes);
                    notificationManager.createNotificationChannel(defaultChannel);
                } catch (Exception e) {
                    Log.e(TAG, "Error creating default channel", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in createNotificationChannels", e);
        }
    }
}

