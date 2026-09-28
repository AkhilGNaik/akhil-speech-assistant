package com.kannada.speechassistant;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.Map;

public class ChatMessage {
    private final String id;
    private final String senderId;
    private final String receiverId;
    private final String senderRole;
    private final String receiverRole;
    private final String message;
    private final String messageText;
    private final String language;
    private final String messageType;
    private final String type;
    private final String status;
    private final boolean delivered;
    private final boolean seen;
    private final boolean acknowledged;
    private final Timestamp timestamp;
    private final String audioUrl;
    private final long audioDuration;

    public ChatMessage(DocumentSnapshot doc) {
        this.id = doc.getId();
        this.senderId = doc.getString("senderId") != null ? doc.getString("senderId") : "";
        this.receiverId = doc.getString("receiverId") != null ? doc.getString("receiverId") : "";
        this.senderRole = doc.getString("senderRole") != null ? doc.getString("senderRole") : "";
        this.receiverRole = doc.getString("receiverRole") != null ? doc.getString("receiverRole") : "";
        this.message = doc.getString("message") != null ? doc.getString("message") : "";
        this.messageText = doc.getString("messageText") != null ? doc.getString("messageText") : "";
        this.language = doc.getString("language") != null ? doc.getString("language") : "English";
        this.messageType = doc.getString("messageType") != null ? doc.getString("messageType") : "text";
        this.type = doc.getString("type") != null ? doc.getString("type") : "text";
        this.status = doc.getString("status") != null ? doc.getString("status") : "sent";
        this.audioUrl = doc.getString("audioUrl") != null ? doc.getString("audioUrl") : "";
        Long dur = doc.getLong("audioDuration");
        this.audioDuration = dur != null ? dur : 0L;
        
        Boolean del = doc.getBoolean("delivered");
        this.delivered = del != null ? del : false;
        
        Boolean sn = doc.getBoolean("seen");
        this.seen = sn != null ? sn : false;

        Boolean ack = doc.getBoolean("acknowledged");
        this.acknowledged = ack != null ? ack : false;
        
        this.timestamp = doc.getTimestamp("timestamp");
    }

    public ChatMessage(Map<String, Object> map) {
        this.id = map.containsKey("messageId") ? (String) map.get("messageId") : "";
        this.senderId = map.containsKey("senderId") ? (String) map.get("senderId") : "";
        this.receiverId = map.containsKey("receiverId") ? (String) map.get("receiverId") : "";
        this.senderRole = map.containsKey("senderRole") ? (String) map.get("senderRole") : "";
        this.receiverRole = map.containsKey("receiverRole") ? (String) map.get("receiverRole") : "";
        this.message = map.containsKey("message") ? (String) map.get("message") : "";
        this.messageText = map.containsKey("messageText") ? (String) map.get("messageText") : "";
        this.language = map.containsKey("language") ? (String) map.get("language") : "English";
        this.messageType = map.containsKey("messageType") ? (String) map.get("messageType") : "text";
        this.type = map.containsKey("type") ? (String) map.get("type") : "text";
        this.status = map.containsKey("status") ? (String) map.get("status") : "sent";
        this.audioUrl = map.containsKey("audioUrl") ? (String) map.get("audioUrl") : "";
        Object dur = map.get("audioDuration");
        if (dur instanceof Number) {
            this.audioDuration = ((Number) dur).longValue();
        } else {
            this.audioDuration = 0L;
        }
        
        Boolean del = (Boolean) map.get("delivered");
        this.delivered = del != null ? del : false;
        
        Boolean sn = (Boolean) map.get("seen");
        this.seen = sn != null ? sn : false;

        Boolean ack = (Boolean) map.get("acknowledged");
        this.acknowledged = ack != null ? ack : false;
        
        Object ts = map.get("timestamp");
        if (ts instanceof Timestamp) {
            this.timestamp = (Timestamp) ts;
        } else if (ts instanceof Long) {
            this.timestamp = new Timestamp(new java.util.Date((Long) ts));
        } else {
            this.timestamp = new Timestamp(new java.util.Date());
        }
    }

    public String getId() { return id; }
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public String getSenderRole() { return senderRole; }
    public String getReceiverRole() { return receiverRole; }
    public String getMessage() { return message; }
    public String getMessageText() { return messageText; }
    public String getLanguage() { return language; }
    public String getMessageType() { return messageType; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public String getAudioUrl() { return audioUrl; }
    public long getAudioDuration() { return audioDuration; }
    public boolean isDelivered() { return delivered; }
    public boolean isSeen() { return seen; }
    public boolean isAcknowledged() { return acknowledged; }
    public Timestamp getTimestamp() { return timestamp; }
    public boolean isVoice() {
        return "voice".equalsIgnoreCase(type) || "voice".equalsIgnoreCase(messageType) || (audioUrl != null && !audioUrl.isEmpty()) || (message != null && message.startsWith("🎤"));
    }
}
