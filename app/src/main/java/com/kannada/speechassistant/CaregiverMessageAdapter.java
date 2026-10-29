package com.kannada.speechassistant;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CaregiverMessageAdapter extends RecyclerView.Adapter<CaregiverMessageAdapter.MessageViewHolder> {

    private final Context context;
    private final List<DocumentSnapshot> messages = new ArrayList<>();
    private final List<UserRecord> usersList = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());

    private final int[] avatarColors = {
            Color.parseColor("#4F46E5"),
            Color.parseColor("#0D9488"),
            Color.parseColor("#7C3AED"),
            Color.parseColor("#DB2777"),
            Color.parseColor("#D97706")
    };

    public CaregiverMessageAdapter(Context context) {
        this.context = context;
    }

    public void setMessages(List<DocumentSnapshot> messages) {
        this.messages.clear();
        this.messages.addAll(messages);
        notifyDataSetChanged();
    }

    public void setUsers(List<UserRecord> users) {
        this.usersList.clear();
        this.usersList.addAll(users);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_caregiver_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        DocumentSnapshot doc = messages.get(position);
        String senderUid = doc.getString("senderUid");
        String messageText = doc.getString("message");
        Timestamp timestamp = doc.getTimestamp("timestamp");

        // Look up sender profile in our user list
        String senderName = "Unknown User";
        String senderRole = "Patient";
        String senderEmail = null;
        if (senderUid != null) {
            for (UserRecord user : usersList) {
                if (senderUid.equals(user.getId())) {
                    String name = user.getString("name");
                    String role = user.getString("role");
                    String email = user.getString("email");
                    if (name != null) senderName = name;
                    if (role != null) senderRole = role;
                    if (email != null) senderEmail = email;
                    break;
                }
            }
        }

        holder.txtName.setText(senderName);
        holder.txtMessage.setText(messageText != null ? messageText : "");

        if (timestamp != null) {
            holder.txtTime.setText(dateFormat.format(timestamp.toDate()));
        } else {
            holder.txtTime.setText("");
        }

        // Set avatar letter & color
        String initial = senderName.isEmpty() ? "?" : senderName.substring(0, 1).toUpperCase();
        holder.txtAvatar.setText(initial);
        int colorIndex = Math.abs(senderName.hashCode()) % avatarColors.length;
        holder.avatarCard.setCardBackgroundColor(avatarColors[colorIndex]);

        if (RoleManager.ROLE_MUTE_USER.equals(senderRole)) {
            holder.txtRoleBadge.setText("Mute User");
        } else if (RoleManager.ROLE_SPEECH_IMPAIRED.equals(senderRole) || RoleManager.ROLE_DEAF_USER.equals(senderRole)) {
            holder.txtRoleBadge.setText("Deaf User");
        } else {
            holder.txtRoleBadge.setText(senderRole != null ? senderRole.replace(" User", "") : "User");
        }
        int badgeBg, badgeText;
        if (RoleManager.ROLE_MUTE_USER.equals(senderRole)) {
            badgeBg = Color.parseColor("#1E3A8A");
            badgeText = Color.parseColor("#93C5FD");
        } else if (RoleManager.ROLE_SPEECH_IMPAIRED.equals(senderRole) || RoleManager.ROLE_DEAF_USER.equals(senderRole)) {
            badgeBg = Color.parseColor("#312E81");
            badgeText = Color.parseColor("#C7D2FE");
        } else if (RoleManager.ROLE_PHYSICALLY_DISABLED.equals(senderRole)) {
            badgeBg = Color.parseColor("#134E4A");
            badgeText = Color.parseColor("#5EEAD4");
        } else if (RoleManager.ROLE_BLIND_USER.equals(senderRole)) {
            badgeBg = Color.parseColor("#4A044E");
            badgeText = Color.parseColor("#F472B6");
        } else {
            badgeBg = Color.parseColor("#334155");
            badgeText = Color.parseColor("#CBD5E1");
        }
        holder.txtRoleBadge.setBackgroundTintList(ColorStateList.valueOf(badgeBg));
        holder.txtRoleBadge.setTextColor(badgeText);

        final String finalName = senderName;
        final String finalRole = senderRole;
        final String finalEmail = senderEmail;
        holder.btnReply.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("receiverId", senderUid);
            intent.putExtra("receiverName", finalName);
            intent.putExtra("receiverRole", finalRole);
            intent.putExtra("receiverEmail", finalEmail);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView avatarCard;
        TextView txtAvatar, txtName, txtRoleBadge, txtMessage, txtTime;
        MaterialButton btnReply;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarCard = itemView.findViewById(R.id.avatarCard);
            txtAvatar = itemView.findViewById(R.id.txtAvatar);
            txtName = itemView.findViewById(R.id.txtName);
            txtRoleBadge = itemView.findViewById(R.id.txtRoleBadge);
            txtMessage = itemView.findViewById(R.id.txtMessage);
            txtTime = itemView.findViewById(R.id.txtTime);
            btnReply = itemView.findViewById(R.id.btnReply);
        }
    }
}
