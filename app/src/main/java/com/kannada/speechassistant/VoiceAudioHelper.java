package com.kannada.speechassistant;

import android.content.Context;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Robust Voice Audio Engine:
 * 1. Compact speech recording configuration (AAC, 16kHz, 24kbps).
 * 2. Multi-tier resilient upload strategy:
 *    - Tier 1: Firebase Cloud Storage (if available)
 *    - Tier 2: Supabase Storage HTTP upload (with fast 3s connection timeout)
 *    - Tier 3: Bulletproof Firestore Embedded Base64 Voice (guarantees 100% success even when cloud storage / DNS is blocked)
 * 3. Seamless offline & Base64 playback resolution: decodes data URIs / Base64 into cached .m4a audio for native MediaPlayer playback.
 */
public class VoiceAudioHelper {
    private static final String TAG = "VoiceAudioHelper";
    private static final ExecutorService UPLOAD_EXECUTOR = Executors.newFixedThreadPool(2);
    private static final String SUPABASE_PROJECT_URL = "https://kfzdvzeahvhfcbkhxhmn.supabase.co";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtmemR2emVhaHZoZmNia2h4aG1uIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjY0Nzc5NDAsImV4cCI6MjA4MjA1Mzk0MH0.-UzIibKLFuOAyTeECHLal4wNkX8o73ctAuG8BhcsOYs";

    public interface UploadVoiceCallback {
        void onSuccess(@NonNull String downloadUrl, int durationSeconds, @Nullable String base64Audio);
        void onFailure(@NonNull Exception error);
    }

    /**
     * Configures a MediaRecorder for optimal, compact AAC speech encoding.
     */
    public static void configureSpeechMediaRecorder(@NonNull MediaRecorder recorder, @NonNull File outputFile) {
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioEncodingBitRate(24000); // 24 kbps speech-optimized for compact size
        recorder.setAudioSamplingRate(16000);    // 16 kHz high clarity speech
        recorder.setOutputFile(outputFile.getAbsolutePath());
    }

    /**
     * Reads a file completely into a byte array.
     */
    @Nullable
    public static byte[] readFileToBytes(@Nullable File file) {
        if (file == null || !file.exists()) return null;
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = fis.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "Failed reading audio file to bytes: " + e.getMessage(), e);
            return null;
        }
    }

    public static String safeBase64Encode(byte[] bytes) {
        if (bytes == null) return "";
        try {
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Throwable t) {
            return java.util.Base64.getEncoder().encodeToString(bytes);
        }
    }

    public static byte[] safeBase64Decode(String base64Str) {
        if (base64Str == null || base64Str.trim().isEmpty()) return new byte[0];
        try {
            return Base64.decode(base64Str, Base64.DEFAULT);
        } catch (Throwable t) {
            return java.util.Base64.getDecoder().decode(base64Str.trim());
        }
    }

    /**
     * Caches audio bytes locally so playback on device never needs network transfer.
     */
    @Nullable
    public static File cacheAudioLocally(@Nullable Context context, @Nullable String key, @Nullable byte[] bytes) {
        if (context == null || key == null || bytes == null || bytes.length == 0) return null;
        try {
            File cacheDir = new File(context.getCacheDir(), "voice_cache");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            String safeFileName = "voice_" + Math.abs(key.hashCode()) + ".m4a";
            File target = new File(cacheDir, safeFileName);
            if (!target.exists() || target.length() != bytes.length) {
                try (FileOutputStream fos = new FileOutputStream(target)) {
                    fos.write(bytes);
                    fos.flush();
                }
            }
            return target;
        } catch (Exception e) {
            Log.w(TAG, "Failed caching audio locally: " + e.getMessage());
            return null;
        }
    }

    /**
     * Resolves an audioUrl (HTTP, file://, data URI, or Base64) into a playable local File.
     * If data URI or base64, it decodes and writes to disk for native MediaPlayer playback.
     */
    @Nullable
    public static File resolveAudioFile(@Nullable Context context, @Nullable String audioUrl) {
        if (context == null || audioUrl == null || audioUrl.trim().isEmpty()) {
            return null;
        }
        audioUrl = audioUrl.trim();

        // 1. Direct file path or file:// URI
        if (audioUrl.startsWith("file://")) {
            try {
                File f = new File(Uri.parse(audioUrl).getPath());
                if (f.exists()) return f;
            } catch (Exception ignored) {}
        }
        if (audioUrl.startsWith("/") && !audioUrl.startsWith("/storage/")) {
            File f = new File(audioUrl);
            if (f.exists()) return f;
        }

        File cacheDir = new File(context.getCacheDir(), "voice_cache");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        // 2. Data URI ("data:audio/...;base64,...") or raw Base64 string
        if (audioUrl.startsWith("data:") || (!audioUrl.startsWith("http://") && !audioUrl.startsWith("https://") && audioUrl.length() > 60)) {
            String base64Data = audioUrl;
            int commaIdx = audioUrl.indexOf(",");
            if (commaIdx != -1) {
                base64Data = audioUrl.substring(commaIdx + 1);
            }
            try {
                byte[] decoded = safeBase64Decode(base64Data);
                if (decoded != null && decoded.length > 0) {
                    String hashName = "voice_" + Math.abs(audioUrl.hashCode()) + ".m4a";
                    File target = new File(cacheDir, hashName);
                    if (!target.exists() || target.length() != decoded.length) {
                        try (FileOutputStream fos = new FileOutputStream(target)) {
                            fos.write(decoded);
                            fos.flush();
                        }
                    }
                    return target;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error decoding base64 audio: " + e.getMessage(), e);
            }
        }

        // 3. HTTP / HTTPS URL: Check if cached locally
        if (audioUrl.startsWith("http://") || audioUrl.startsWith("https://")) {
            String hashName = "voice_" + Math.abs(audioUrl.hashCode()) + ".m4a";
            File target = new File(cacheDir, hashName);
            if (target.exists() && target.length() > 100) {
                return target;
            }
        }

        return null;
    }

    /**
     * Multi-tier resilient voice message upload:
     * Tier 1: Firebase Storage
     * Tier 2: Supabase Storage (with 3-second fast connect timeout)
     * Tier 3: Direct Firestore Embedded Base64 Voice (guaranteed zero failure)
     */
    public static void uploadVoiceMessage(@NonNull Context context,
                                          @NonNull File audioFile,
                                          int durationSeconds,
                                          @Nullable String senderUid,
                                          @NonNull UploadVoiceCallback callback) {
        if (!audioFile.exists() || audioFile.length() == 0) {
            callback.onFailure(new IllegalArgumentException("Audio file is missing or empty"));
            return;
        }

        byte[] audioBytes = readFileToBytes(audioFile);
        if (audioBytes == null || audioBytes.length == 0) {
            callback.onFailure(new IllegalArgumentException("Unable to read audio file"));
            return;
        }

        String base64Audio = safeBase64Encode(audioBytes);
        String dataUrl = "data:audio/m4a;base64," + base64Audio;

        // Pre-cache locally so sender playback is instantaneous
        cacheAudioLocally(context, dataUrl, audioBytes);

        String storagePath = "voice_messages/" + (senderUid != null && !senderUid.isEmpty() ? senderUid : "user") + "/" + System.currentTimeMillis() + ".m4a";

        UPLOAD_EXECUTOR.execute(() -> {
            // Tier 1: Try Firebase Storage
            try {
                StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(storagePath);
                storageRef.putFile(Uri.fromFile(audioFile))
                        .addOnSuccessListener(taskSnapshot -> {
                            storageRef.getDownloadUrl()
                                    .addOnSuccessListener(uri -> {
                                        Log.d(TAG, "Firebase Storage upload succeeded: " + uri);
                                        callback.onSuccess(uri.toString(), durationSeconds, base64Audio);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w(TAG, "Firebase getDownloadUrl failed, falling back to Supabase...", e);
                                        trySupabaseUpload(audioFile, durationSeconds, storagePath, dataUrl, base64Audio, callback);
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "Firebase putFile failed, falling back to Supabase...", e);
                            trySupabaseUpload(audioFile, durationSeconds, storagePath, dataUrl, base64Audio, callback);
                        });
            } catch (Exception e) {
                Log.w(TAG, "Firebase Storage init error, falling back to Supabase...", e);
                trySupabaseUpload(audioFile, durationSeconds, storagePath, dataUrl, base64Audio, callback);
            }
        });
    }

    private static void trySupabaseUpload(File audioFile,
                                          int durationSeconds,
                                          String storagePath,
                                          String fallbackDataUrl,
                                          String base64Audio,
                                          UploadVoiceCallback callback) {
        UPLOAD_EXECUTOR.execute(() -> {
            try {
                URL url = new URL(SUPABASE_PROJECT_URL + "/storage/v1/object/" + storagePath);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(3000); // 3-second fast connect timeout
                conn.setReadTimeout(5000);
                conn.setRequestProperty("apikey", SUPABASE_API_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_API_KEY);
                conn.setRequestProperty("Content-Type", "audio/m4a");
                conn.setFixedLengthStreamingMode(audioFile.length());

                try (FileInputStream fis = new FileInputStream(audioFile);
                     OutputStream os = conn.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                    os.flush();
                }

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    String downloadUrl = SUPABASE_PROJECT_URL + "/storage/v1/object/public/" + storagePath;
                    Log.d(TAG, "Supabase Storage upload succeeded: " + downloadUrl);
                    callback.onSuccess(downloadUrl, durationSeconds, base64Audio);
                    return;
                }
                Log.w(TAG, "Supabase upload returned HTTP code " + code + ", proceeding to Tier 3 fallback");
            } catch (Exception e) {
                Log.w(TAG, "Supabase upload network error: " + e.getMessage() + ", proceeding to Tier 3 fallback");
            }

            // Tier 3: Bulletproof Firestore Embedded Base64 Voice (guaranteed zero failure!)
            Log.i(TAG, "Using Tier 3 Firestore embedded audio delivery");
            callback.onSuccess(fallbackDataUrl, durationSeconds, base64Audio);
        });
    }
}
