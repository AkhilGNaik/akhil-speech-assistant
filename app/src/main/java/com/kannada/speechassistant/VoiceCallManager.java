package com.kannada.speechassistant;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

/**
 * Orchestrates the Call State Machine (IDLE -> CALLING -> RINGING -> CONNECTING -> CONNECTED -> ENDED/REJECTED),
 * integrating WebRTC audio engine, Firestore signaling, ring timeout, and audio controls.
 */
public class VoiceCallManager implements WebRTCManager.WebRTCListener, VoiceCallSignalingManager.SignalingListener {

    private static final String TAG = "VoiceCallManager";
    public static final int RING_TIMEOUT_MS = 45000; // 45 seconds ringing timeout

    public enum CallState {
        IDLE,
        CALLING,
        RINGING,
        CONNECTING,
        CONNECTED,
        REJECTED,
        MISSED,
        ENDED,
        FAILED
    }

    public interface CallListener {
        void onCallStateChanged(CallState state, String message);
        void onError(String message);
    }

    private static VoiceCallManager instance;

    public static synchronized VoiceCallManager getInstance(Context context) {
        if (instance == null) {
            instance = new VoiceCallManager(context.getApplicationContext());
        }
        return instance;
    }

    private final Context context;
    private WebRTCManager webRTCManager;
    private VoiceCallSignalingManager signalingManager;
    private CallListener callListener;

    private CallState currentState = CallState.IDLE;
    private boolean isCaller = false;
    private String currentCallId = null;
    private String remoteUid = null;
    private String remoteName = null;
    private String remoteRole = null;
    private String localUid = null;
    private String localName = null;
    private String localRole = null;

    private boolean isOfferProcessed = false;
    private boolean isAnswerProcessed = false;

    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    private VoiceCallManager(Context context) {
        this.context = context;
        this.signalingManager = new VoiceCallSignalingManager();
    }

    public synchronized void setCallListener(CallListener listener) {
        this.callListener = listener;
    }

    public CallState getCurrentState() {
        return currentState;
    }

    public boolean isCallActive() {
        return currentState != CallState.IDLE && currentState != CallState.ENDED &&
                currentState != CallState.REJECTED && currentState != CallState.MISSED &&
                currentState != CallState.FAILED;
    }

    public String getCurrentCallId() {
        return currentCallId;
    }

    public String getRemoteName() {
        return remoteName;
    }

    public boolean isCaller() {
        return isCaller;
    }

    /**
     * Initiates an outgoing call from Blind User to Caregiver.
     */
    public synchronized void startCall(String localUid, String localName, String localRole,
                                      String remoteUid, String remoteName, String remoteRole,
                                      CallListener listener) {
        if (isCallActive()) {
            if (listener != null) listener.onError("A voice call is already in progress.");
            return;
        }

        try {
            this.isCaller = true;
            this.localUid = localUid;
            this.localName = localName;
            this.localRole = localRole;
            this.remoteUid = remoteUid;
            this.remoteName = remoteName;
            this.remoteRole = remoteRole;
            this.callListener = listener;
            this.isOfferProcessed = false;
            this.isAnswerProcessed = false;

            this.currentCallId = localUid + "_" + remoteUid + "_" + System.currentTimeMillis();

            Log.d(TAG, "VOICE_CALL_BUTTON_CLICKED: Starting call to " + remoteName + " (UID: " + remoteUid + ")");
            updateState(CallState.CALLING, "Calling caregiver...");

            // Initialize WebRTC
            webRTCManager = new WebRTCManager(context);
            webRTCManager.init(this);

            // Initialize Signaling
            signalingManager.init(currentCallId, this);
            signalingManager.initiateCall(localUid, localName, localRole, remoteUid, remoteName, remoteRole, () -> {
                try {
                    signalingManager.startListeningToCallDoc();
                    signalingManager.startListeningToRemoteCandidates(true);

                    // Create SDP Offer
                    if (webRTCManager != null) {
                        webRTCManager.createOffer();
                    }
                    updateState(CallState.RINGING, "Ringing caregiver...");
                    startRingTimeoutTimer();
                } catch (Exception e) {
                    Log.e(TAG, "VOICE_CALL_ERROR initiating offer: " + e.getMessage(), e);
                    updateState(CallState.FAILED, "Call creation failed.");
                    cleanup();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "VOICE_CALL_ERROR starting call: " + e.getMessage(), e);
            updateState(CallState.FAILED, "Unable to start voice call.");
            cleanup();
        }
    }

    /**
     * Accepts an incoming call as Caregiver.
     */
    public synchronized void acceptCall(String callId, String localUid, String localName, String localRole,
                                       String remoteUid, String remoteName, String remoteRole,
                                       CallListener listener) {
        if (isCallActive() && !callId.equals(currentCallId)) {
            if (listener != null) listener.onError("A call is already in progress.");
            return;
        }

        try {
            cancelRingTimeoutTimer();

            this.isCaller = false;
            this.currentCallId = callId;
            this.localUid = localUid;
            this.localName = localName;
            this.localRole = localRole;
            this.remoteUid = remoteUid;
            this.remoteName = remoteName;
            this.remoteRole = remoteRole;
            this.callListener = listener;
            this.isOfferProcessed = false;
            this.isAnswerProcessed = false;

            Log.d(TAG, "CALL_ACCEPTED: Accepting call " + callId + " from " + remoteName);
            updateState(CallState.CONNECTING, "Connecting call...");

            // Initialize WebRTC
            webRTCManager = new WebRTCManager(context);
            webRTCManager.init(this);

            // Initialize Signaling
            signalingManager.init(currentCallId, this);
            signalingManager.startListeningToCallDoc();
            signalingManager.startListeningToRemoteCandidates(false);

            // Update status in Firestore to ACCEPTED
            signalingManager.updateCallStatus("ACCEPTED");
        } catch (Exception e) {
            Log.e(TAG, "VOICE_CALL_ERROR accepting call: " + e.getMessage(), e);
            updateState(CallState.FAILED, "Unable to accept call.");
            cleanup();
        }
    }

    /**
     * Rejects an incoming call as Caregiver.
     */
    public synchronized void rejectCall(String callId) {
        cancelRingTimeoutTimer();
        if (signalingManager != null && callId != null) {
            signalingManager.init(callId, null);
            signalingManager.updateCallStatus("REJECTED");
        }
        updateState(CallState.REJECTED, "Caregiver rejected the call.");
        cleanup();
    }

    /**
     * Ends the active call from either side.
     */
    public synchronized void endCall() {
        cancelRingTimeoutTimer();
        if (signalingManager != null && currentCallId != null) {
            signalingManager.updateCallStatus("ENDED");
        }
        updateState(CallState.ENDED, "Voice call ended.");
        cleanup();
    }

    /**
     * Cancels an outgoing call before accepted.
     */
    public synchronized void cancelCall() {
        cancelRingTimeoutTimer();
        if (signalingManager != null && currentCallId != null) {
            signalingManager.updateCallStatus("CANCELLED");
        }
        updateState(CallState.ENDED, "Call cancelled.");
        cleanup();
    }

    public boolean toggleMute() {
        if (webRTCManager != null) {
            return webRTCManager.toggleMute();
        }
        return false;
    }

    public boolean toggleSpeaker() {
        if (webRTCManager != null) {
            return webRTCManager.toggleSpeaker();
        }
        return true;
    }

    public boolean isMuted() {
        return webRTCManager != null && webRTCManager.isMuted();
    }

    public boolean isSpeakerOn() {
        return webRTCManager != null && webRTCManager.isSpeakerOn();
    }

    private void startRingTimeoutTimer() {
        cancelRingTimeoutTimer();
        timeoutRunnable = () -> {
            if (currentState == CallState.CALLING || currentState == CallState.RINGING) {
                Log.d(TAG, "Call timed out - no answer from caregiver");
                if (signalingManager != null && currentCallId != null) {
                    signalingManager.updateCallStatus("MISSED");
                }
                updateState(CallState.MISSED, "Caregiver did not answer.");
                cleanup();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, RING_TIMEOUT_MS);
    }

    private void cancelRingTimeoutTimer() {
        if (timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
    }

    private void updateState(CallState state, String message) {
        this.currentState = state;
        Log.d(TAG, "CallState transition: " + state + " -> " + message);
        if (callListener != null) {
            new Handler(Looper.getMainLooper()).post(() -> callListener.onCallStateChanged(state, message));
        }
    }

    // =========================================================================
    // WebRTCListener Callbacks
    // =========================================================================

    @Override
    public void onIceCandidateGenerated(IceCandidate candidate) {
        if (signalingManager != null) {
            signalingManager.sendIceCandidate(candidate, isCaller);
        }
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState state) {
        Log.d(TAG, "WebRTC IceConnectionState: " + state);
        if (state == PeerConnection.IceConnectionState.CONNECTED || state == PeerConnection.IceConnectionState.COMPLETED) {
            cancelRingTimeoutTimer();
            updateState(CallState.CONNECTED, "Voice call connected");
        } else if (state == PeerConnection.IceConnectionState.FAILED) {
            updateState(CallState.FAILED, "Call connection failed.");
            cleanup();
        } else if (state == PeerConnection.IceConnectionState.DISCONNECTED) {
            updateState(CallState.ENDED, "Peer disconnected.");
            cleanup();
        }
    }

    @Override
    public void onOfferCreated(SessionDescription sdp) {
        if (signalingManager != null) {
            signalingManager.sendOffer(sdp);
        }
    }

    @Override
    public void onAnswerCreated(SessionDescription sdp) {
        if (signalingManager != null) {
            signalingManager.sendAnswer(sdp);
        }
    }

    @Override
    public void onRemoteAudioReceived() {
        Log.d(TAG, "Remote audio track stream playing!");
        cancelRingTimeoutTimer();
        updateState(CallState.CONNECTED, "Voice call connected");
    }

    @Override
    public void onError(String errorMessage) {
        Log.e(TAG, "VOICE_CALL_ERROR: " + errorMessage);
        updateState(CallState.FAILED, errorMessage);
        if (callListener != null) {
            new Handler(Looper.getMainLooper()).post(() -> callListener.onError(errorMessage));
        }
        cleanup();
    }

    // =========================================================================
    // SignalingListener Callbacks
    // =========================================================================

    @Override
    public void onCallStatusChanged(String status) {
        if (status == null) return;
        Log.d(TAG, "Remote call status changed: " + status);

        switch (status) {
            case "ACCEPTED":
                cancelRingTimeoutTimer();
                updateState(CallState.CONNECTING, "Connecting call...");
                break;
            case "REJECTED":
                cancelRingTimeoutTimer();
                updateState(CallState.REJECTED, "Caregiver rejected the call.");
                cleanup();
                break;
            case "ENDED":
                cancelRingTimeoutTimer();
                updateState(CallState.ENDED, "Call ended.");
                cleanup();
                break;
            case "MISSED":
                cancelRingTimeoutTimer();
                updateState(CallState.MISSED, "Call missed.");
                cleanup();
                break;
            case "CANCELLED":
                cancelRingTimeoutTimer();
                updateState(CallState.ENDED, "Call cancelled.");
                cleanup();
                break;
        }
    }

    @Override
    public void onOfferReceived(SessionDescription sdp) {
        if (!isCaller && !isOfferProcessed && webRTCManager != null) {
            isOfferProcessed = true;
            Log.d(TAG, "Received SDP offer from caller. Setting remote description asynchronously...");
            webRTCManager.setRemoteDescription(sdp, () -> {
                Log.d(TAG, "Remote offer description set successfully. Creating SDP answer...");
                if (webRTCManager != null) {
                    webRTCManager.createAnswer();
                }
            });
        }
    }

    @Override
    public void onAnswerReceived(SessionDescription sdp) {
        if (isCaller && !isAnswerProcessed && webRTCManager != null) {
            isAnswerProcessed = true;
            Log.d(TAG, "Received SDP answer from callee. Setting remote description on caller...");
            webRTCManager.setRemoteDescription(sdp, () -> {
                Log.d(TAG, "Remote answer description set successfully on caller.");
            });
        }
    }

    @Override
    public void onIceCandidateReceived(IceCandidate candidate) {
        if (webRTCManager != null) {
            webRTCManager.addIceCandidate(candidate);
        }
    }

    public synchronized void cleanup() {
        cancelRingTimeoutTimer();

        if (signalingManager != null) {
            signalingManager.removeListeners();
        }

        if (webRTCManager != null) {
            webRTCManager.close();
            webRTCManager = null;
        }

        currentCallId = null;
        isOfferProcessed = false;
        isAnswerProcessed = false;

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (currentState != CallState.IDLE) {
                currentState = CallState.IDLE;
            }
        }, 1000);
    }
}
