package com.kannada.speechassistant;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persists and manages mock connection states locally in SharedPreferences.
 * Used as a self-healing fallback when Firestore is offline or unreachable.
 */
public class LocalConnectionSimulator {

    private static final String PREF_NAME = "LocalConnections";
    private static final String KEY_CONNECTIONS = "connections_list";

    public static void saveConnection(Context context, Map<String, Object> conn) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        try {
            String existingJson = prefs.getString(KEY_CONNECTIONS, "[]");
            JSONArray array = new JSONArray(existingJson);

            JSONObject obj = new JSONObject();
            String id = conn.containsKey("docId") ? (String) conn.get("docId") : UUID.randomUUID().toString();
            obj.put("docId", id);
            obj.put("senderUid", conn.get("senderUid"));
            obj.put("senderEmail", conn.get("senderEmail"));
            obj.put("senderName", conn.get("senderName"));
            obj.put("senderRole", conn.get("senderRole"));
            obj.put("receiverUid", conn.get("receiverUid"));
            obj.put("receiverEmail", conn.get("receiverEmail"));
            obj.put("receiverName", conn.get("receiverName"));
            obj.put("receiverRole", conn.get("receiverRole"));
            obj.put("status", conn.get("status"));
            obj.put("timestamp", System.currentTimeMillis());

            obj.put("patientUid", conn.get("patientUid"));
            obj.put("patientName", conn.get("patientName"));
            obj.put("patientEmail", conn.get("patientEmail"));
            obj.put("caregiverUid", conn.get("caregiverUid"));
            obj.put("caregiverEmail", conn.get("caregiverEmail"));

            boolean overwritten = false;
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                if (item.getString("docId").equals(id) || 
                    (item.getString("senderEmail").equalsIgnoreCase((String)conn.get("senderEmail")) && 
                     item.getString("receiverEmail").equalsIgnoreCase((String)conn.get("receiverEmail")))) {
                    array.put(i, obj);
                    overwritten = true;
                    break;
                }
            }
            if (!overwritten) {
                array.put(obj);
            }

            prefs.edit().putString(KEY_CONNECTIONS, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Map<String, Object>> getConnections(Context context) {
        List<Map<String, Object>> list = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        try {
            String json = prefs.getString(KEY_CONNECTIONS, "[]");
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Map<String, Object> map = new HashMap<>();
                map.put("docId", obj.optString("docId"));
                map.put("senderUid", obj.optString("senderUid"));
                map.put("senderEmail", obj.optString("senderEmail"));
                map.put("senderName", obj.optString("senderName"));
                map.put("senderRole", obj.optString("senderRole"));
                map.put("receiverUid", obj.optString("receiverUid"));
                map.put("receiverEmail", obj.optString("receiverEmail"));
                map.put("receiverName", obj.optString("receiverName"));
                map.put("receiverRole", obj.optString("receiverRole"));
                map.put("status", obj.optString("status"));
                
                map.put("patientUid", obj.optString("patientUid"));
                map.put("patientName", obj.optString("patientName"));
                map.put("patientEmail", obj.optString("patientEmail"));
                map.put("caregiverUid", obj.optString("caregiverUid"));
                map.put("caregiverEmail", obj.optString("caregiverEmail"));
                list.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void updateConnectionStatus(Context context, String docId, String status) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        try {
            String json = prefs.getString(KEY_CONNECTIONS, "[]");
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (obj.getString("docId").equals(docId)) {
                    obj.put("status", status);
                    array.put(i, obj);
                    break;
                }
            }
            prefs.edit().putString(KEY_CONNECTIONS, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateConnectionUid(Context context, String docId, String caregiverUid) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        try {
            String json = prefs.getString(KEY_CONNECTIONS, "[]");
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (obj.getString("docId").equals(docId)) {
                    obj.put("receiverUid", caregiverUid);
                    obj.put("caregiverUid", caregiverUid);
                    array.put(i, obj);
                    break;
                }
            }
            prefs.edit().putString(KEY_CONNECTIONS, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteConnection(Context context, String docId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        try {
            String json = prefs.getString(KEY_CONNECTIONS, "[]");
            JSONArray array = new JSONArray(json);
            JSONArray newArray = new JSONArray();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (!obj.getString("docId").equals(docId)) {
                    newArray.put(obj);
                }
            }
            prefs.edit().putString(KEY_CONNECTIONS, newArray.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveLocalMessage(Context context, String chatId, Map<String, Object> msg) {
        SharedPreferences prefs = context.getSharedPreferences("LocalChatMessages_" + chatId, Context.MODE_PRIVATE);
        try {
            String existingJson = prefs.getString("messages", "[]");
            org.json.JSONArray array = new org.json.JSONArray(existingJson);

            org.json.JSONObject obj = new org.json.JSONObject();
            obj.put("senderId", msg.get("senderId"));
            obj.put("receiverId", msg.get("receiverId"));
            obj.put("senderRole", msg.get("senderRole"));
            obj.put("receiverRole", msg.get("receiverRole"));
            obj.put("message", msg.get("message"));
            obj.put("messageText", msg.get("messageText"));
            obj.put("language", msg.get("language"));
            obj.put("type", msg.get("type"));
            obj.put("messageType", msg.get("messageType"));
            obj.put("status", "sent");
            obj.put("delivered", true);
            obj.put("seen", true);
            obj.put("timestamp", System.currentTimeMillis());

            array.put(obj);
            prefs.edit().putString("messages", array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Map<String, Object>> getLocalMessages(Context context, String chatId) {
        List<Map<String, Object>> list = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences("LocalChatMessages_" + chatId, Context.MODE_PRIVATE);
        try {
            String json = prefs.getString("messages", "[]");
            org.json.JSONArray array = new org.json.JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                org.json.JSONObject obj = array.getJSONObject(i);
                Map<String, Object> map = new HashMap<>();
                map.put("senderId", obj.optString("senderId"));
                map.put("receiverId", obj.optString("receiverId"));
                map.put("senderRole", obj.optString("senderRole"));
                map.put("receiverRole", obj.optString("receiverRole"));
                map.put("message", obj.optString("message"));
                map.put("messageText", obj.optString("messageText"));
                map.put("language", obj.optString("language"));
                map.put("type", obj.optString("type"));
                map.put("messageType", obj.optString("messageType"));
                map.put("status", obj.optString("status"));
                map.put("delivered", obj.optBoolean("delivered", true));
                map.put("seen", obj.optBoolean("seen", true));
                
                long ts = obj.optLong("timestamp", System.currentTimeMillis());
                map.put("timestamp", new com.google.firebase.Timestamp(new java.util.Date(ts)));
                list.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void clearLocalMessages(Context context, String chatId) {
        SharedPreferences prefs = context.getSharedPreferences("LocalChatMessages_" + chatId, Context.MODE_PRIVATE);
        prefs.edit().remove("messages").apply();
    }
}
