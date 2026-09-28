package com.kannada.speechassistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<ChatMessage> messages = new ArrayList<>();
    private final String currentUserId;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public ChatAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<ChatMessage> newMessages) {
        this.messages.clear();
        this.messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);

        String senderId = msg.getSenderId();
        String messageText = msg.getMessage();
        String status = msg.getStatus();
        Timestamp timestamp = msg.getTimestamp();

        messageText = messageText != null ? messageText : "";
        status = status != null ? status : "";

        Date currentMsgDate = timestamp != null ? timestamp.toDate() : new Date();

        // Date Separator Logic
        boolean showDateHeader = false;
        if (position == 0) {
            showDateHeader = true;
        } else {
            ChatMessage prevMsg = messages.get(position - 1);
            Timestamp prevTimestamp = prevMsg.getTimestamp();
            Date prevMsgDate = prevTimestamp != null ? prevTimestamp.toDate() : new Date();
            if (!isSameDay(currentMsgDate, prevMsgDate)) {
                showDateHeader = true;
            }
        }

        if (showDateHeader) {
            holder.layoutDateHeader.setVisibility(View.VISIBLE);
            String dateLabel = getFormattedDateHeader(currentMsgDate);
            holder.txtDateHeader.setText("─── " + dateLabel + " ───");
        } else {
            holder.layoutDateHeader.setVisibility(View.GONE);
        }

        String timeStr = timeFormat.format(currentMsgDate);

        String type = msg.getType();
        boolean isEmergency = "emergency".equalsIgnoreCase(type);
        boolean isVoice = msg.isVoice();
        String audioUrl = msg.getAudioUrl();

        if (isVoice) {
            if (audioUrl != null && !audioUrl.isEmpty()) {
                messageText = "🎤 Voice Message (▶ Tap to Play)";
            } else if (!messageText.startsWith("🎤")) {
                messageText = "🎤 " + messageText;
            }
        }

        final String finalAudioUrl = audioUrl;

        if (currentUserId.equals(senderId)) {
            // Sent by current user (Right bubble)
            holder.layoutRight.setVisibility(View.VISIBLE);
            holder.layoutLeft.setVisibility(View.GONE);

            holder.txtRightMessage.setText(messageText);
            holder.txtRightTime.setText(timeStr);
            
            if (isEmergency) {
                holder.cardRightBubble.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EF4444")));
                holder.txtRightMessage.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                holder.txtRightTime.setTextColor(android.graphics.Color.parseColor("#FEF2F2"));
            } else {
                holder.cardRightBubble.setCardBackgroundColor(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.primary));
                holder.txtRightMessage.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                holder.txtRightTime.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary));
            }

            if (finalAudioUrl != null && !finalAudioUrl.isEmpty()) {
                holder.cardRightBubble.setOnClickListener(v -> playOrPauseAudio(v.getContext(), finalAudioUrl));
            }
            
            // Format status text with checkmarks
            holder.txtRightStatus.setVisibility(View.VISIBLE);
            if (isEmergency && ("read".equalsIgnoreCase(status) || "seen".equalsIgnoreCase(status) || msg.isSeen() || msg.isAcknowledged())) {
                holder.txtRightStatus.setText("✓✓ Acknowledged");
                holder.txtRightStatus.setTextColor(android.graphics.Color.parseColor("#34B7F1")); // Blue read ticks
            } else if ("read".equalsIgnoreCase(status) || "seen".equalsIgnoreCase(status) || msg.isSeen() || msg.isAcknowledged()) {
                holder.txtRightStatus.setText("✓✓");
                holder.txtRightStatus.setTextColor(android.graphics.Color.parseColor("#34B7F1")); // Blue read ticks
            } else if ("delivered".equalsIgnoreCase(status) || msg.isDelivered()) {
                holder.txtRightStatus.setText("✓✓");
                holder.txtRightStatus.setTextColor(android.graphics.Color.parseColor("#94A3B8")); // Gray double ticks
            } else if ("sent".equalsIgnoreCase(status)) {
                holder.txtRightStatus.setText("✓");
                holder.txtRightStatus.setTextColor(android.graphics.Color.parseColor("#94A3B8")); // Gray single tick
            } else {
                holder.txtRightStatus.setText("◷");
                holder.txtRightStatus.setTextColor(android.graphics.Color.parseColor("#94A3B8")); // Clock icon for sending
            }
        } else {
            // Received from other user (Left bubble)
            holder.layoutLeft.setVisibility(View.VISIBLE);
            holder.layoutRight.setVisibility(View.GONE);

            holder.txtLeftMessage.setText(messageText);
            holder.txtLeftTime.setText(timeStr);

            if (isEmergency) {
                holder.cardLeftBubble.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FEE2E2")));
                holder.txtLeftMessage.setTextColor(android.graphics.Color.parseColor("#991B1B"));
                holder.txtLeftTime.setTextColor(android.graphics.Color.parseColor("#FCA5A5"));
            } else {
                holder.cardLeftBubble.setCardBackgroundColor(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.surface));
                holder.txtLeftMessage.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
                holder.txtLeftTime.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary));
            }

            if (finalAudioUrl != null && !finalAudioUrl.isEmpty()) {
                holder.cardLeftBubble.setOnClickListener(v -> playOrPauseAudio(v.getContext(), finalAudioUrl));
            }
        }
    }

    private static android.media.MediaPlayer activeMediaPlayer = null;
    private static String playingAudioUrl = null;

    public static void stopAudioPlayback() {
        if (activeMediaPlayer != null) {
            try {
                if (activeMediaPlayer.isPlaying()) {
                    activeMediaPlayer.stop();
                }
                activeMediaPlayer.release();
            } catch (Exception ignored) {}
            activeMediaPlayer = null;
            playingAudioUrl = null;
        }
    }

    private static void playOrPauseAudio(android.content.Context context, String audioUrl) {
        if (audioUrl == null || audioUrl.isEmpty()) {
            android.widget.Toast.makeText(context, "No audio recording available", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            if (activeMediaPlayer != null && audioUrl.equals(playingAudioUrl)) {
                if (activeMediaPlayer.isPlaying()) {
                    activeMediaPlayer.pause();
                    android.widget.Toast.makeText(context, "⏸ Audio Paused", android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    activeMediaPlayer.start();
                    android.widget.Toast.makeText(context, "▶ Playing Audio...", android.widget.Toast.LENGTH_SHORT).show();
                }
                return;
            }

            if (activeMediaPlayer != null) {
                activeMediaPlayer.stop();
                activeMediaPlayer.release();
                activeMediaPlayer = null;
                playingAudioUrl = null;
            }

            activeMediaPlayer = new android.media.MediaPlayer();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                activeMediaPlayer.setAudioAttributes(
                        new android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                );
            } else {
                activeMediaPlayer.setAudioStreamType(android.media.AudioManager.STREAM_MUSIC);
            }
            activeMediaPlayer.setDataSource(context, android.net.Uri.parse(audioUrl));
            activeMediaPlayer.prepareAsync();
            android.widget.Toast.makeText(context, "Loading Voice Message...", android.widget.Toast.LENGTH_SHORT).show();
            playingAudioUrl = audioUrl;

            activeMediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                android.util.Log.d("BlindVoiceMessage", "VOICE_MESSAGE_PLAY: url = " + audioUrl);
                android.widget.Toast.makeText(context, "▶ Playing Voice Message", android.widget.Toast.LENGTH_SHORT).show();
            });

            activeMediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                activeMediaPlayer = null;
                playingAudioUrl = null;
                android.widget.Toast.makeText(context, "✓ Playback Finished", android.widget.Toast.LENGTH_SHORT).show();
            });

            activeMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                mp.release();
                activeMediaPlayer = null;
                playingAudioUrl = null;
                android.widget.Toast.makeText(context, "Unable to play audio", android.widget.Toast.LENGTH_SHORT).show();
                return true;
            });

        } catch (Exception e) {
            android.widget.Toast.makeText(context, "Playback error: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
        }
    }


    private boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) return false;
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private String getFormattedDateHeader(Date messageDate) {
        if (messageDate == null) return "Today";

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar yesterday = (Calendar) today.clone();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        Calendar msgCal = Calendar.getInstance();
        msgCal.setTime(messageDate);
        msgCal.set(Calendar.HOUR_OF_DAY, 0);
        msgCal.set(Calendar.MINUTE, 0);
        msgCal.set(Calendar.SECOND, 0);
        msgCal.set(Calendar.MILLISECOND, 0);

        if (msgCal.getTimeInMillis() == today.getTimeInMillis()) {
            return "Today";
        } else if (msgCal.getTimeInMillis() == yesterday.getTimeInMillis()) {
            return "Yesterday";
        } else {
            SimpleDateFormat fullDateFormat = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());
            return fullDateFormat.format(messageDate);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutDateHeader;
        TextView txtDateHeader;
        LinearLayout layoutLeft, layoutRight;
        MaterialCardView cardLeftBubble, cardRightBubble;
        TextView txtLeftMessage, txtLeftTime, txtRightMessage, txtRightTime, txtRightStatus;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutDateHeader = itemView.findViewById(R.id.layoutDateHeader);
            txtDateHeader = itemView.findViewById(R.id.txtDateHeader);
            layoutLeft = itemView.findViewById(R.id.layoutLeft);
            layoutRight = itemView.findViewById(R.id.layoutRight);
            cardLeftBubble = itemView.findViewById(R.id.cardLeftBubble);
            cardRightBubble = itemView.findViewById(R.id.cardRightBubble);
            txtLeftMessage = itemView.findViewById(R.id.txtLeftMessage);
            txtLeftTime = itemView.findViewById(R.id.txtLeftTime);
            txtRightMessage = itemView.findViewById(R.id.txtRightMessage);
            txtRightTime = itemView.findViewById(R.id.txtRightTime);
            txtRightStatus = itemView.findViewById(R.id.txtRightStatus);
        }
    }
}
