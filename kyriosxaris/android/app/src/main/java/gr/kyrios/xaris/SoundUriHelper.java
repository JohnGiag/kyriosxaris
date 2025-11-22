package gr.kyrios.xaris;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

/**
 * Utility class for getting sound URIs from app resources.
 * 
 * CRITICAL: This ensures consistent sound URI generation across the app.
 * Sound files must be placed in res/raw/ directory.
 * 
 * @param context The application context (required to access resources)
 * @param soundName The name of the sound file (extension is automatically removed)
 * @return The URI for the sound file, or null if not found
 */
public class SoundUriHelper {
    private static final String TAG = "SoundUriHelper";

    /**
     * Gets the URI for a sound file in res/raw/
     * 
     * Sound files should be placed in: android/app/src/main/res/raw/
     * Supported formats: .mp3, .wav, .ogg
     * 
     * Example: For file "ding.wav", pass "ding" or "ding.wav" as soundName
     */
    public static Uri getSoundUri(Context context, String soundName) {
        if (soundName == null || soundName.isEmpty() || soundName.equals("default")) {
            return null;
        }

        // Remove file extension if present
        String cleanSoundName = soundName;
        if (cleanSoundName.contains(".")) {
            cleanSoundName = cleanSoundName.substring(0, cleanSoundName.lastIndexOf("."));
        }

        String packageName = context.getPackageName();
        
        // Verify the resource exists
        int resourceId = context.getResources().getIdentifier(cleanSoundName, "raw", packageName);
        if (resourceId == 0) {
            Log.w(TAG, "Sound file not found in res/raw/: " + cleanSoundName);
            return null;
        }

        // Build Android resource URI
        return Uri.parse("android.resource://" + packageName + "/raw/" + cleanSoundName);
    }
}

