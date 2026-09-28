package com.kannada.speechassistant;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages WebRTC PeerConnection, microphone audio stream capture, remote audio playback,
 * STUN configuration, JavaAudioDeviceModule binding, and audio hardware routing.
 */
public class WebRTCManager {

    private static final String TAG = "WebRTCManager";
    public static final String AUDIO_TRACK_ID = "ARDAMSa0";
    public static final String MEDIA_STREAM_ID = "ARDAMS";
    private static boolean isPeerConnectionFactoryInitialized = false;

    public interface WebRTCListener {
        void onIceCandidateGenerated(IceCandidate candidate);
        void onIceConnectionChange(PeerConnection.IceConnectionState state);
        void onOfferCreated(SessionDescription sdp);
        void onAnswerCreated(SessionDescription sdp);
        void onRemoteAudioReceived();
        void onError(String errorMessage);
    }

    private final Context context;
    private PeerConnectionFactory factory;
    private AudioDeviceModule audioDeviceModule;
    private PeerConnection peerConnection;
    private AudioSource audioSource;
    private AudioTrack localAudioTrack;
    private MediaStream localStream;
    private AudioManager audioManager;
    private WebRTCListener listener;

    private boolean isMuted = false;
    private boolean isSpeakerOn = true;

    public WebRTCManager(Context context) {
        this.context = context.getApplicationContext();
        this.audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
    }

    private static synchronized void initializePeerConnectionFactory(Context context) {
        if (!isPeerConnectionFactoryInitialized) {
            try {
                PeerConnectionFactory.InitializationOptions initializationOptions =
                        PeerConnectionFactory.InitializationOptions.builder(context)
                                .setEnableInternalTracer(false)
                                .createInitializationOptions();
                PeerConnectionFactory.initialize(initializationOptions);
                isPeerConnectionFactoryInitialized = true;
                Log.d(TAG, "PeerConnectionFactory global initialization completed.");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing PeerConnectionFactory: " + e.getMessage(), e);
            }
        }
    }

    public void init(WebRTCListener listener) {
        this.listener = listener;

        try {
            // 1. Initialize WebRTC PeerConnectionFactory globally once
            initializePeerConnectionFactory(context);

            // 2. Build Android JavaAudioDeviceModule for Hardware Echo Cancellation & Noise Suppression
            audioDeviceModule = JavaAudioDeviceModule.builder(context)
                    .setUseHardwareAcousticEchoCanceler(true)
                    .setUseHardwareNoiseSuppressor(true)
                    .createAudioDeviceModule();

            // 3. Create PeerConnectionFactory with AudioDeviceModule bound
            PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
            factory = PeerConnectionFactory.builder()
                    .setOptions(options)
                    .setAudioDeviceModule(audioDeviceModule)
                    .createPeerConnectionFactory();

            createLocalAudioTrack();
            setupPeerConnection();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize WebRTCManager: " + e.getMessage(), e);
            if (listener != null) {
                listener.onError("Unable to initialize WebRTC audio engine: " + e.getMessage());
            }
        }
    }

    private void createLocalAudioTrack() {
        if (factory == null) return;
        MediaConstraints audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googAutoGainControl", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googHighpassFilter", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));

        audioSource = factory.createAudioSource(audioConstraints);
        localAudioTrack = factory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
        localAudioTrack.setEnabled(true);

        localStream = factory.createLocalMediaStream(MEDIA_STREAM_ID);
        localStream.addTrack(localAudioTrack);
    }

    private void setupPeerConnection() {
        if (factory == null) return;
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.relay.metered.ca:80").createIceServer());

        String turnUsername = "b91d7d2ff34a3696bc7a9d85";
        String turnCredential = "u5ZH6TZS/WCtw9R+";

        iceServers.add(PeerConnection.IceServer.builder("turn:global.relay.metered.ca:80")
                .setUsername(turnUsername)
                .setPassword(turnCredential)
                .createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("turn:global.relay.metered.ca:80?transport=tcp")
                .setUsername(turnUsername)
                .setPassword(turnCredential)
                .createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("turn:global.relay.metered.ca:443")
                .setUsername(turnUsername)
                .setPassword(turnCredential)
                .createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("turns:global.relay.metered.ca:443?transport=tcp")
                .setUsername(turnUsername)
                .setPassword(turnCredential)
                .createIceServer());

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;

        peerConnection = factory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.d(TAG, "SignalingState changed: " + signalingState);
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d(TAG, "IceConnectionState changed: " + iceConnectionState);
                if (listener != null) {
                    listener.onIceConnectionChange(iceConnectionState);
                }
            }

            @Override
            public void onIceConnectionReceivingChange(boolean receiving) {}

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.d(TAG, "IceGatheringState changed: " + iceGatheringState);
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d(TAG, "ICE Candidate generated: " + iceCandidate.sdp);
                if (listener != null) {
                    listener.onIceCandidateGenerated(iceCandidate);
                }
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {}

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.d(TAG, "onAddStream: " + mediaStream.getId());
                if (!mediaStream.audioTracks.isEmpty()) {
                    AudioTrack remoteAudioTrack = mediaStream.audioTracks.get(0);
                    remoteAudioTrack.setEnabled(true);
                    if (listener != null) {
                        listener.onRemoteAudioReceived();
                    }
                }
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {}

            @Override
            public void onDataChannel(DataChannel dataChannel) {}

            @Override
            public void onRenegotiationNeeded() {}

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                Log.d(TAG, "onAddTrack: Remote track received");
                if (rtpReceiver != null && rtpReceiver.track() instanceof AudioTrack) {
                    AudioTrack remoteAudioTrack = (AudioTrack) rtpReceiver.track();
                    remoteAudioTrack.setEnabled(true);
                    Log.d(TAG, "Remote AudioTrack cast and enabled successfully");
                }
                if (listener != null) {
                    listener.onRemoteAudioReceived();
                }
            }
        });

        if (peerConnection != null && localAudioTrack != null) {
            try {
                peerConnection.addTrack(localAudioTrack, Collections.singletonList(MEDIA_STREAM_ID));
                Log.d(TAG, "Local AudioTrack successfully added to PeerConnection with Unified Plan (Stream ID: " + MEDIA_STREAM_ID + ")");
            } catch (Exception e) {
                Log.e(TAG, "Error adding local AudioTrack to PeerConnection: " + e.getMessage(), e);
            }
        }

        configureAudioHardware();
    }

    public void configureAudioHardware() {
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn(isSpeakerOn);
        }
    }

    public void createOffer() {
        if (peerConnection == null) return;
        MediaConstraints sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));

        peerConnection.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                if (peerConnection == null) return;
                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {}
                    @Override
                    public void onSetSuccess() {
                        Log.d(TAG, "Local description set successfully for offer");
                        if (listener != null) {
                            listener.onOfferCreated(sessionDescription);
                        }
                    }
                    @Override
                    public void onCreateFailure(String s) {}
                    @Override
                    public void onSetFailure(String s) {
                        Log.e(TAG, "Failed to set local description for offer: " + s);
                    }
                }, sessionDescription);
            }

            @Override
            public void onSetSuccess() {}
            @Override
            public void onCreateFailure(String s) {
                Log.e(TAG, "Failed to create offer: " + s);
                if (listener != null) listener.onError("Failed to create call offer: " + s);
            }
            @Override
            public void onSetFailure(String s) {}
        }, sdpConstraints);
    }

    public void createAnswer() {
        if (peerConnection == null) return;
        MediaConstraints sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));

        peerConnection.createAnswer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                if (peerConnection == null) return;
                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {}
                    @Override
                    public void onSetSuccess() {
                        Log.d(TAG, "Local description set successfully for answer");
                        if (listener != null) {
                            listener.onAnswerCreated(sessionDescription);
                        }
                    }
                    @Override
                    public void onCreateFailure(String s) {}
                    @Override
                    public void onSetFailure(String s) {
                        Log.e(TAG, "Failed to set local description for answer: " + s);
                    }
                }, sessionDescription);
            }

            @Override
            public void onSetSuccess() {}
            @Override
            public void onCreateFailure(String s) {
                Log.e(TAG, "Failed to create answer: " + s);
                if (listener != null) listener.onError("Failed to create call answer: " + s);
            }
            @Override
            public void onSetFailure(String s) {}
        }, sdpConstraints);
    }

    public void setRemoteDescription(SessionDescription sessionDescription, Runnable onComplete) {
        if (peerConnection == null) {
            Log.e(TAG, "Cannot set remote description: PeerConnection is null");
            return;
        }
        Log.d(TAG, "Setting remote description (type: " + (sessionDescription != null ? sessionDescription.type : "null") + ")");
        peerConnection.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {}

            @Override
            public void onSetSuccess() {
                Log.d(TAG, "Remote description set successfully");
                if (onComplete != null) {
                    onComplete.run();
                }
            }

            @Override
            public void onCreateFailure(String s) {
                Log.e(TAG, "onCreateFailure during setRemoteDescription: " + s);
            }

            @Override
            public void onSetFailure(String s) {
                Log.e(TAG, "Failed to set remote description: " + s);
            }
        }, sessionDescription);
    }

    public void setRemoteDescription(SessionDescription sessionDescription) {
        setRemoteDescription(sessionDescription, null);
    }

    public void addIceCandidate(IceCandidate candidate) {
        if (peerConnection != null) {
            peerConnection.addIceCandidate(candidate);
        }
    }

    public boolean toggleMute() {
        isMuted = !isMuted;
        if (localAudioTrack != null) {
            localAudioTrack.setEnabled(!isMuted);
        }
        return isMuted;
    }

    public boolean toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn;
        if (audioManager != null) {
            audioManager.setSpeakerphoneOn(isSpeakerOn);
        }
        return isSpeakerOn;
    }

    public boolean isMuted() {
        return isMuted;
    }

    public boolean isSpeakerOn() {
        return isSpeakerOn;
    }

    public void close() {
        try {
            if (audioManager != null) {
                audioManager.setSpeakerphoneOn(false);
                audioManager.setMode(AudioManager.MODE_NORMAL);
            }

            if (peerConnection != null) {
                peerConnection.close();
                peerConnection = null;
            }

            if (localAudioTrack != null) {
                localAudioTrack.dispose();
                localAudioTrack = null;
            }

            if (audioSource != null) {
                audioSource.dispose();
                audioSource = null;
            }

            if (audioDeviceModule != null) {
                audioDeviceModule.release();
                audioDeviceModule = null;
            }

            if (factory != null) {
                factory.dispose();
                factory = null;
            }

            Log.d(TAG, "WebRTC resources released successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error disposing WebRTC resources: " + e.getMessage(), e);
        }
    }
}
