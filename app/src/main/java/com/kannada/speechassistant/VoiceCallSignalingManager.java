package com.kannada.speechassistant;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles Firestore signaling for WebRTC Voice Calls (`voice_calls/{callId}`).
 * Exposes real-time sync for status updates, SDP offers/answers, and ICE candidate exchange.
 */
public class VoiceCallSignalingManager {

    private static final String TAG = "VoiceCallSignaling";

    public interface SignalingListener {
        void onCallStatusChanged(String status);
        void onOfferReceived(SessionDescription sdp);
        void onAnswerReceived(SessionDescription sdp);
        void onIceCandidateReceived(IceCandidate candidate);
        void onError(String message);
    }

    private final FirebaseFirestore db;
    private ListenerRegistration callDocListener;
    private ListenerRegistration candidatesListener;
    private SignalingListener listener;
    private String callId;

    public VoiceCallSignalingManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void init(String callId, SignalingListener listener) {
        this.callId = callId;
        this.listener = listener;
    }

    public String getCallId() {
        return callId;
    }

    /**
     * Creates a new call document in `voice_calls/{callId}`.
     */
    public void initiateCall(String callerUid, String callerName, String callerRole,
                             String calleeUid, String calleeName, String calleeRole,
                             Runnable onSuccess) {
        DocumentReference callRef = db.collection("voice_calls").document(callId);

        Map<String, Object> callData = new HashMap<>();
        callData.put("callId", callId);
        callData.put("callerUid", callerUid);
        callData.put("callerName", callerName != null ? callerName : "Caller");
        callData.put("callerRole", callerRole);
        callData.put("calleeUid", calleeUid);
        callData.put("calleeName", calleeName != null ? calleeName : "Caregiver");
        callData.put("calleeRole", calleeRole);
        callData.put("status", "RINGING");
        callData.put("createdAt", FieldValue.serverTimestamp());

        callRef.set(callData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Call document created: " + callId);
                    if (onSuccess != null) onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create call document: " + e.getMessage(), e);
                    if (listener != null) listener.onError("Signaling error: " + e.getMessage());
                });
    }

    /**
     * Stores local SDP offer in call document.
     */
    public void sendOffer(SessionDescription offer) {
        if (callId == null) return;
        Map<String, Object> sdpMap = new HashMap<>();
        sdpMap.put("type", "offer");
        sdpMap.put("sdp", offer.description);

        db.collection("voice_calls").document(callId)
                .update("offer", sdpMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "SDP offer sent successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to send offer: " + e.getMessage(), e));
    }

    /**
     * Stores local SDP answer in call document.
     */
    public void sendAnswer(SessionDescription answer) {
        if (callId == null) return;
        Map<String, Object> sdpMap = new HashMap<>();
        sdpMap.put("type", "answer");
        sdpMap.put("sdp", answer.description);

        db.collection("voice_calls").document(callId)
                .update("answer", sdpMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "SDP answer sent successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to send answer: " + e.getMessage(), e));
    }

    /**
     * Updates call status (ACCEPTED, REJECTED, ENDED, MISSED, CANCELLED).
     */
    public void updateCallStatus(String status) {
        if (callId == null) return;
        Map<String, Object> update = new HashMap<>();
        update.put("status", status);
        if ("ENDED".equals(status) || "REJECTED".equals(status) || "MISSED".equals(status) || "CANCELLED".equals(status)) {
            update.put("endedAt", FieldValue.serverTimestamp());
        }

        db.collection("voice_calls").document(callId)
                .update(update)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Call status updated to: " + status))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update status to " + status + ": " + e.getMessage(), e));
    }

    /**
     * Adds an ICE candidate to the candidate subcollection.
     */
    public void sendIceCandidate(IceCandidate candidate, boolean isCaller) {
        if (callId == null) return;
        String subCollection = isCaller ? "callerCandidates" : "calleeCandidates";

        Map<String, Object> candidateMap = new HashMap<>();
        candidateMap.put("sdpMid", candidate.sdpMid);
        candidateMap.put("sdpMLineIndex", candidate.sdpMLineIndex);
        candidateMap.put("sdp", candidate.sdp);
        candidateMap.put("timestamp", FieldValue.serverTimestamp());

        db.collection("voice_calls").document(callId)
                .collection(subCollection)
                .add(candidateMap)
                .addOnSuccessListener(ref -> Log.d(TAG, "ICE candidate added to " + subCollection))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to add candidate: " + e.getMessage(), e));
    }

    /**
     * Starts listening to the main call document for status changes, offer, and answer.
     */
    public void startListeningToCallDoc() {
        if (callId == null) return;
        removeListeners();

        callDocListener = db.collection("voice_calls").document(callId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Call doc listener error: " + e.getMessage());
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        String status = snapshot.getString("status");
                        if (status != null && listener != null) {
                            listener.onCallStatusChanged(status);
                        }

                        // Check Offer
                        Map<String, Object> offerMap = (Map<String, Object>) snapshot.get("offer");
                        if (offerMap != null && listener != null) {
                            String sdp = (String) offerMap.get("sdp");
                            if (sdp != null) {
                                listener.onOfferReceived(new SessionDescription(SessionDescription.Type.OFFER, sdp));
                            }
                        }

                        // Check Answer
                        Map<String, Object> answerMap = (Map<String, Object>) snapshot.get("answer");
                        if (answerMap != null && listener != null) {
                            String sdp = (String) answerMap.get("sdp");
                            if (sdp != null) {
                                listener.onAnswerReceived(new SessionDescription(SessionDescription.Type.ANSWER, sdp));
                            }
                        }
                    }
                });
    }

    /**
     * Starts listening to remote candidates subcollection.
     */
    public void startListeningToRemoteCandidates(boolean isCaller) {
        if (callId == null) return;
        String remoteSubCollection = isCaller ? "calleeCandidates" : "callerCandidates";

        candidatesListener = db.collection("voice_calls").document(callId)
                .collection(remoteSubCollection)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            DocumentSnapshot doc = dc.getDocument();
                            String sdpMid = doc.getString("sdpMid");
                            Long sdpMLineIndex = doc.getLong("sdpMLineIndex");
                            String sdp = doc.getString("sdp");

                            if (sdp != null && sdpMLineIndex != null && listener != null) {
                                IceCandidate candidate = new IceCandidate(
                                        sdpMid != null ? sdpMid : "0",
                                        sdpMLineIndex.intValue(),
                                        sdp
                                );
                                listener.onIceCandidateReceived(candidate);
                            }
                        }
                    }
                });
    }

    public void removeListeners() {
        if (callDocListener != null) {
            callDocListener.remove();
            callDocListener = null;
        }
        if (candidatesListener != null) {
            candidatesListener.remove();
            candidatesListener = null;
        }
    }
}
