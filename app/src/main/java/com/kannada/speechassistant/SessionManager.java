package com.kannada.speechassistant;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashMap;

/**
 * Manages user session state using Android SharedPreferences.
 * Stores user login status, email, unique Firebase UID, and accessibility roles.
 */
public class SessionManager {
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private final Context context;

    // Shared preferences file name
    private static final String PREF_NAME = "SpeechAssistantSession";
    
    // Session Keys
    public static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    public static final String KEY_USER_UID = "userUid";
    public static final String KEY_USER_EMAIL = "userEmail";
    public static final String KEY_USER_ROLE = "userRole";
    public static final String KEY_USER_LANGUAGE = "userLanguage";

    public SessionManager(Context context) {
        this.context = context;
        this.pref = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.editor = pref.edit();
    }

    /**
     * Creates login session and saves details in SharedPreferences
     */
    public void createLoginSession(String uid, String email, String role) {
        createLoginSession(uid, email, role, null);
    }

    /**
     * Creates login session with user language and saves details in SharedPreferences
     */
    public void createLoginSession(String uid, String email, String role, String language) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_UID, uid);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_ROLE, role);
        editor.putString(KEY_USER_LANGUAGE, LanguageManager.normalizeLanguageCode(language));
        editor.apply(); // Asynchronously saves the data
    }

    /**
     * Saves user language preference independently
     */
    public void saveLanguage(String language) {
        editor.putString(KEY_USER_LANGUAGE, LanguageManager.normalizeLanguageCode(language));
        editor.apply();
    }

    /**
     * Retrieves saved user language code with "kn" fallback
     */
    public String getLanguage() {
        String lang = pref.getString(KEY_USER_LANGUAGE, LanguageManager.DEFAULT_LANGUAGE);
        return LanguageManager.normalizeLanguageCode(lang);
    }

    /**
     * Checks if the user is logged in.
     * @return true if logged in, false otherwise.
     */
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Gets stored user session data.
     * @return HashMap containing user details.
     */
    public HashMap<String, String> getUserDetails() {
        HashMap<String, String> user = new HashMap<>();
        user.put(KEY_USER_UID, pref.getString(KEY_USER_UID, null));
        user.put(KEY_USER_EMAIL, pref.getString(KEY_USER_EMAIL, null));
        user.put(KEY_USER_ROLE, pref.getString(KEY_USER_ROLE, null));
        user.put(KEY_USER_LANGUAGE, getLanguage());
        return user;
    }

    /**
     * Gets the current user role.
     * @return String user role (Speech-Impaired User, Physically Disabled User, Admin/Caregiver).
     */
    public String getUserRole() {
        return pref.getString(KEY_USER_ROLE, null);
    }

    /**
     * Clears all session data and logs the user out.
     */
    public void logoutUser() {
        editor.clear();
        editor.apply();
    }
}
