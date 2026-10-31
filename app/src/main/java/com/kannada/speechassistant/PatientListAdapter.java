package com.kannada.speechassistant;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PatientListAdapter extends RecyclerView.Adapter<PatientListAdapter.PatientViewHolder> {

    public interface OnPatientSelectedListener {
        void onPatientSelected(UserRecord patient);
    }

    private final Context context;
    private final List<UserRecord> patients = new ArrayList<>();
    private final OnPatientSelectedListener listener;
    private String selectedPatientUid = null;

    private final int[] avatarColors = {
            Color.parseColor("#4F46E5"),
            Color.parseColor("#0D9488"),
            Color.parseColor("#7C3AED"),
            Color.parseColor("#DB2777"),
            Color.parseColor("#D97706")
    };

    public PatientListAdapter(Context context, OnPatientSelectedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setPatients(List<UserRecord> newPatients) {
        this.patients.clear();
        this.patients.addAll(newPatients);
        sortPatientsList();
        notifyDataSetChanged();
    }

    public void setSelectedPatientUid(String selectedPatientUid) {
        this.selectedPatientUid = selectedPatientUid;
        notifyDataSetChanged();
    }

    public void updatePatientChatInfo(String patientUid, String lastMessage, Timestamp lastTime, int unreadCount) {
        for (int i = 0; i < patients.size(); i++) {
            UserRecord p = patients.get(i);
            if (patientUid.equals(p.getId())) {
                if (p.getData() != null) {
                    p.getData().put("lastMessage", lastMessage);
                    p.getData().put("lastMessageTime", lastTime);
                    p.getData().put("unreadCount", unreadCount);
                }
                break;
            }
        }
        sortPatientsList();
        notifyDataSetChanged();
    }

    public void updatePatientOnlineStatus(String patientUid, boolean isOnline) {
        for (int i = 0; i < patients.size(); i++) {
            UserRecord p = patients.get(i);
            if (patientUid.equals(p.getId())) {
                if (p.getData() != null) {
                    p.getData().put("online", isOnline);
                }
                break;
            }
        }
        sortPatientsList();
        notifyDataSetChanged();
    }

    private void sortPatientsList() {
        Collections.sort(patients, (p1, p2) -> {
            long time1 = 0;
            long time2 = 0;
            if (p1.getData() != null) {
                Object val = p1.getData().get("lastMessageTime");
                if (val instanceof Timestamp) {
                    time1 = ((Timestamp) val).toDate().getTime();
                } else if (p1.getData().containsKey("lastMessage")) {
                    String lastMsg = (String) p1.getData().get("lastMessage");
                    if (lastMsg != null && !lastMsg.isEmpty()) {
                        time1 = System.currentTimeMillis();
                    }
                }
            }
            if (p2.getData() != null) {
                Object val = p2.getData().get("lastMessageTime");
                if (val instanceof Timestamp) {
                    time2 = ((Timestamp) val).toDate().getTime();
                } else if (p2.getData().containsKey("lastMessage")) {
                    String lastMsg = (String) p2.getData().get("lastMessage");
                    if (lastMsg != null && !lastMsg.isEmpty()) {
                        time2 = System.currentTimeMillis();
                    }
                }
            }

            return Long.compare(time2, time1); // descending (newest first)
        });
    }

    @NonNull
    @Override
    public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_patient, parent, false);
        return new PatientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
        UserRecord patient = patients.get(position);
        String name = patient.getString("name");

        // Set patient name
        holder.txtName.setText(name != null ? name : "Patient");

        // Set User Type
        String role = patient.getString("role");
        if (RoleManager.ROLE_MUTE_USER.equals(role)) {
            holder.txtUserType.setText("Mute User");
            holder.txtUserType.setTextColor(Color.parseColor("#2563EB")); // Blue
        } else if (RoleManager.ROLE_SPEECH_IMPAIRED.equals(role) || RoleManager.ROLE_DEAF_USER.equals(role)) {
            holder.txtUserType.setText("Deaf User");
            holder.txtUserType.setTextColor(Color.parseColor("#0D9488")); // Teal
        } else if (RoleManager.ROLE_PHYSICALLY_DISABLED.equals(role)) {
            holder.txtUserType.setText("Physically Disabled");
            holder.txtUserType.setTextColor(Color.parseColor("#7C3AED")); // Purple
        } else if (RoleManager.ROLE_BLIND_USER.equals(role)) {
            holder.txtUserType.setText("Blind User");
            holder.txtUserType.setTextColor(Color.parseColor("#D97706")); // Amber/Orange
        } else {
            holder.txtUserType.setText(role != null ? role : "Patient");
            holder.txtUserType.setTextColor(Color.parseColor("#64748B"));
        }

        // Set Avatar initials
        String initial = name == null || name.isEmpty() ? "P" : name.substring(0, 1).toUpperCase();
        holder.txtAvatar.setText(initial);
        int colorIndex = Math.abs((name != null ? name : "").hashCode()) % avatarColors.length;
        holder.avatarCard.setCardBackgroundColor(avatarColors[colorIndex]);

        // Set online status color
        boolean isOnline = false;
        if (patient.getData() != null && patient.getData().containsKey("online")) {
            Object onlineVal = patient.getData().get("online");
            if (onlineVal instanceof Boolean) {
                isOnline = (Boolean) onlineVal;
            }
        }
        holder.indicatorCard.setCardBackgroundColor(isOnline ? Color.parseColor("#10B981") : Color.parseColor("#94A3B8"));

        // Set last message preview
        String lastMsg = "No messages yet.";
        if (patient.getData() != null && patient.getData().containsKey("lastMessage")) {
            String m = (String) patient.getData().get("lastMessage");
            if (m != null && !m.isEmpty()) {
                lastMsg = m;
            }
        }
        holder.txtLastMessage.setText(lastMsg);

        // Set last message time
        Timestamp lastTime = null;
        if (patient.getData() != null && patient.getData().containsKey("lastMessageTime")) {
            Object t = patient.getData().get("lastMessageTime");
            if (t instanceof Timestamp) {
                lastTime = (Timestamp) t;
            }
        }
        holder.txtLastMessageTime.setText(formatLastMessageTime(lastTime));

        // Set unread count badge & text style highlights
        int unread = 0;
        if (patient.getData() != null && patient.getData().containsKey("unreadCount")) {
            Object u = patient.getData().get("unreadCount");
            if (u instanceof Integer) {
                unread = (Integer) u;
            }
        }
        if (unread > 0) {
            holder.txtUnreadCount.setVisibility(View.GONE);
            holder.cardUnreadBadge.setVisibility(View.VISIBLE);

            holder.txtName.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.txtLastMessage.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.txtLastMessage.setTextColor(Color.parseColor("#1E293B"));
        } else {
            holder.cardUnreadBadge.setVisibility(View.GONE);

            holder.txtName.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.txtLastMessage.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.txtLastMessage.setTextColor(Color.parseColor("#64748B"));
        }

        // Highlight selected patient item
        String uid = patient.getId();
        if (uid != null && uid.equals(selectedPatientUid)) {
            holder.layoutContainer.setBackgroundResource(R.color.selected_item_background);
        } else {
            holder.layoutContainer.setBackgroundColor(Color.TRANSPARENT);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPatientSelected(patient);
            }
        });
    }

    private String formatLastMessageTime(Timestamp timestamp) {
        if (timestamp == null) return "";
        Date date = timestamp.toDate();
        Calendar cal = Calendar.getInstance();
        Calendar msgCal = Calendar.getInstance();
        msgCal.setTime(date);

        if (cal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)) {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            return sdf.format(date);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            return sdf.format(date);
        }
    }

    @Override
    public int getItemCount() {
        return patients.size();
    }

    public static class PatientViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutContainer;
        MaterialCardView avatarCard, indicatorCard, cardUnreadBadge;
        TextView txtAvatar, txtName, txtUserType, txtLastMessage, txtLastMessageTime, txtUnreadCount;

        public PatientViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutContainer = itemView.findViewById(R.id.layoutPatientItemContainer);
            avatarCard = itemView.findViewById(R.id.avatarCard);
            indicatorCard = itemView.findViewById(R.id.indicatorCard);
            cardUnreadBadge = itemView.findViewById(R.id.cardUnreadBadge);
            txtAvatar = itemView.findViewById(R.id.txtAvatar);
            txtName = itemView.findViewById(R.id.txtName);
            txtUserType = itemView.findViewById(R.id.txtUserType);
            txtLastMessage = itemView.findViewById(R.id.txtLastMessage);
            txtLastMessageTime = itemView.findViewById(R.id.txtLastMessageTime);
            txtUnreadCount = itemView.findViewById(R.id.txtUnreadCount);
        }
    }
}
