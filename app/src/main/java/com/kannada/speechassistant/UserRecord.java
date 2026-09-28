package com.kannada.speechassistant;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.Map;

public class UserRecord {
    private final DocumentSnapshot snapshot;
    private final Map<String, Object> data;

    public UserRecord(DocumentSnapshot snapshot) {
        this.snapshot = snapshot;
        this.data = null;
    }

    public UserRecord(Map<String, Object> data) {
        this.snapshot = null;
        this.data = data;
    }

    public String getString(String field) {
        if (snapshot != null) {
            return snapshot.getString(field);
        }
        return data != null ? (String) data.get(field) : null;
    }

    public String getLanguage() {
        String lang = getString("language");
        return LanguageManager.normalizeLanguageCode(lang);
    }

    public Timestamp getTimestamp(String field) {
        if (snapshot != null) {
            return snapshot.getTimestamp(field);
        }
        Object val = data != null ? data.get(field) : null;
        if (val instanceof Timestamp) {
            return (Timestamp) val;
        }
        return null;
    }

    public String getId() {
        if (snapshot != null) {
            return snapshot.getId();
        }
        return getString("uid");
    }

    public DocumentSnapshot getSnapshot() {
        return snapshot;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
