package gr.kyrios.xaris;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    private static final String CUSTOM_CHANNEL_ID = "custom_sound_channel";
    private static final String CUSTOM_CHANNEL_NAME = "Custom Sound Notifications";
    private static final String CUSTOM_CHANNEL_DESCRIPTION = "Notifications with custom sound";
    
    private static final String DEFAULT_CHANNEL_ID = "default";
    private static final String DEFAULT_CHANNEL_NAME = "Default Notifications";
    private static final String DEFAULT_CHANNEL_DESCRIPTION = "Notifications with default system sound";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            
            // Create notification channel for custom sounds (sound will be set dynamically per notification)
            // Don't set a default sound here - let CustomFirebaseMessagingService handle it per notification
            NotificationChannel customChannel = new NotificationChannel(
                CUSTOM_CHANNEL_ID,
                CUSTOM_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            customChannel.setDescription(CUSTOM_CHANNEL_DESCRIPTION);
            customChannel.enableLights(true);
            customChannel.enableVibration(true);
            // Don't set a default sound - it will be set per notification by CustomFirebaseMessagingService
            // This allows different sounds (ding, custom_sound, shockding, etc.) to be used dynamically
            
            notificationManager.createNotificationChannel(customChannel);
            
            // Create default notification channel for normal/default sound notifications
            NotificationChannel defaultChannel = new NotificationChannel(
                DEFAULT_CHANNEL_ID,
                DEFAULT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            defaultChannel.setDescription(DEFAULT_CHANNEL_DESCRIPTION);
            defaultChannel.enableLights(true);
            defaultChannel.enableVibration(true);
            // Use default system notification sound
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AudioAttributes defaultAudioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();
            defaultChannel.setSound(defaultSoundUri, defaultAudioAttributes);
            
            notificationManager.createNotificationChannel(defaultChannel);
        }
    }
}

