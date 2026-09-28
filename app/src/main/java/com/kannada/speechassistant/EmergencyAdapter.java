package com.kannada.speechassistant;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
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

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter for displaying redesigned emergency alerts in the Caregiver Console.
 */
public class EmergencyAdapter extends RecyclerView.Adapter<EmergencyAdapter.EmergencyViewHolder> {

    public interface OnAlertActionListener {
        void onViewClick(DocumentSnapshot doc);
        void onAcknowledgeClick(DocumentSnapshot doc);
        void onResolveClick(DocumentSnapshot doc);
    }

    private final List<DocumentSnapshot> allAlerts = new ArrayList<>();
    private final List<DocumentSnapshot> filteredAlerts = new ArrayList<>();
    private final OnAlertActionListener actionListener;
    private Map<String, String> userEmailMap = new HashMap<>();
    private String currentQuery = "";

    public EmergencyAdapter(OnAlertActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void setUserEmailMap(Map<String, String> emailMap) {
        if (emailMap != null) {
            this.userEmailMap = emailMap;
        }
    }

    public void setAlerts(List<DocumentSnapshot> alerts) {
        this.allAlerts.clear();
        if (alerts != null) {
            this.allAlerts.addAll(alerts);
        }
        filter(this.currentQuery);
    }

    public void filter(String query) {
        this.currentQuery = query != null ? query : "";
        String q = this.currentQuery.toLowerCase().trim();
        filteredAlerts.clear();

        for (DocumentSnapshot doc : allAlerts) {
            if (q.isEmpty()) {
                filteredAlerts.add(doc);
                continue;
            }

            String patientName = doc.getString("patientName");
            if (patientName == null) patientName = doc.getString("userName");
            if (patientName == null) patientName = doc.getString("name");
            patientName = patientName != null ? patientName : "";

            String patientEmail = doc.getString("patientEmail");
            if (patientEmail == null) patientEmail = doc.getString("userEmail");
            if (patientEmail == null) patientEmail = doc.getString("email");
            patientEmail = patientEmail != null ? patientEmail : "";

            String patientId = doc.getString("patientId");
            if (patientId == null) patientId = doc.getString("userId");
            if (patientId == null) patientId = doc.getString("uid");
            if (patientId == null) patientId = doc.getId();
            patientId = patientId != null ? patientId : "";

            String message = doc.getString("message");
            if (message == null) message = doc.getString("text");
            message = message != null ? message : "";

            if (patientEmail.isEmpty() && !patientId.isEmpty() && userEmailMap != null) {
                String mappedEmail = userEmailMap.get(patientId);
                if (mappedEmail != null) {
                    patientEmail = mappedEmail;
                }
            }

            boolean matches = patientName.toLowerCase().contains(q) ||
                              patientEmail.toLowerCase().contains(q) ||
                              patientId.toLowerCase().contains(q) ||
                              message.toLowerCase().contains(q);

            if (matches) {
                filteredAlerts.add(doc);
            }
        }
        notifyDataSetChanged();
    }

    public int getFilteredCount() {
        return filteredAlerts.size();
    }

    public int getAllCount() {
        return allAlerts.size();
    }

    public int getPositionOfAlert(String alertId) {
        if (alertId == null) return -1;
        for (int i = 0; i < filteredAlerts.size(); i++) {
            String id = filteredAlerts.get(i).getString("alertId");
            if (alertId.equals(id)) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public EmergencyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_emergency_card, parent, false);
        return new EmergencyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmergencyViewHolder holder, int position) {
        DocumentSnapshot doc = filteredAlerts.get(position);

        String alertId = doc.getString("alertId");
        String patientId = doc.getString("patientId");
        String patientName = doc.getString("patientName");
        String message = doc.getString("message");
        String status = doc.getString("status");
        Timestamp timestamp = doc.getTimestamp("timestamp");

        patientName = patientName != null ? patientName : "Unknown Patient";
        patientId = patientId != null ? patientId : "Unknown ID";
        message = message != null ? message : "No Message";
        status = status != null ? status : "NEW";

        holder.txtPatientName.setText("Patient: " + patientName);
        holder.txtPatientId.setText("User ID: " + patientId);
        holder.txtEmergencyMessage.setText("Message: " + message);
        holder.txtEmergencyStatus.setText("Status: " + status);

        // Date and Time formatting
        String dateStr = "Date: N/A";
        String timeStr = "Time: N/A";
        if (timestamp != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.US);
            dateStr = "Date: " + dateFormat.format(timestamp.toDate());
            timeStr = "Time: " + timeFormat.format(timestamp.toDate());
        }
        holder.txtEmergencyDate.setText(dateStr);
        holder.txtEmergencyTime.setText(timeStr);

        // Reset holder animation if any to prevent recycling conflicts
        if (holder.animator != null) {
            holder.animator.cancel();
            holder.animator = null;
        }

        int surfaceColor = holder.itemView.getContext().getResources().getColor(R.color.surface, holder.itemView.getContext().getTheme());

        // Bind Status Badge and Flashing Animator
        if ("NEW".equalsIgnoreCase(status)) {
            holder.txtEmergencyStatus.setTextColor(Color.parseColor("#E11D48")); // Deep Rose Red
            holder.txtEmergencyStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFE4E6"))); // Light Rose Red
            
            // Show Acknowledge and View, hide Resolve
            holder.btnAcknowledgeAlert.setVisibility(View.VISIBLE);
            holder.btnResolveAlert.setVisibility(View.GONE);
            holder.btnViewAlert.setVisibility(View.VISIBLE);

            // Start flashing animator: Bright Red -> Dark Red -> Bright Red
            holder.animator = ValueAnimator.ofArgb(Color.parseColor("#B91C1C"), Color.parseColor("#450A0A"));
            holder.animator.setDuration(800);
            holder.animator.setRepeatMode(ValueAnimator.REVERSE);
            holder.animator.setRepeatCount(ValueAnimator.INFINITE);
            holder.animator.setEvaluator(new ArgbEvaluator());
            holder.animator.addUpdateListener(animation -> {
                int animatedColor = (int) animation.getAnimatedValue();
                holder.cardRoot.setCardBackgroundColor(animatedColor);
            });
            holder.animator.start();

        } else if ("ACKNOWLEDGED".equalsIgnoreCase(status)) {
            holder.txtEmergencyStatus.setTextColor(Color.parseColor("#F59E0B")); // Amber/Orange
            holder.txtEmergencyStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FEF3C7"))); // Light Amber
            holder.cardRoot.setCardBackgroundColor(surfaceColor);

            // Show Resolve and View, hide Acknowledge
            holder.btnAcknowledgeAlert.setVisibility(View.GONE);
            holder.btnResolveAlert.setVisibility(View.VISIBLE);
            holder.btnViewAlert.setVisibility(View.VISIBLE);

        } else { // RESOLVED
            holder.txtEmergencyStatus.setTextColor(Color.parseColor("#059669")); // Deep Green
            holder.txtEmergencyStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D1FAE5"))); // Light Green
            holder.cardRoot.setCardBackgroundColor(surfaceColor);

            // Hide both Acknowledge and Resolve, only show View
            holder.btnAcknowledgeAlert.setVisibility(View.GONE);
            holder.btnResolveAlert.setVisibility(View.GONE);
            holder.btnViewAlert.setVisibility(View.VISIBLE);
        }

        // Click actions
        holder.btnViewAlert.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onViewClick(doc);
            }
        });

        holder.btnAcknowledgeAlert.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onAcknowledgeClick(doc);
            }
        });

        holder.btnResolveAlert.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onResolveClick(doc);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredAlerts.size();
    }

    @Override
    public void onViewRecycled(@NonNull EmergencyViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.animator != null) {
            holder.animator.cancel();
            holder.animator = null;
        }
    }

    public static class EmergencyViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot;
        TextView txtEmergencyTitle, txtPatientName, txtPatientId, txtEmergencyMessage, txtEmergencyDate, txtEmergencyTime, txtEmergencyStatus;
        MaterialButton btnViewAlert, btnAcknowledgeAlert, btnResolveAlert;
        ValueAnimator animator;

        public EmergencyViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardEmergencyRoot);
            txtEmergencyTitle = itemView.findViewById(R.id.txtEmergencyTitle);
            txtPatientName = itemView.findViewById(R.id.txtPatientName);
            txtPatientId = itemView.findViewById(R.id.txtPatientId);
            txtEmergencyMessage = itemView.findViewById(R.id.txtEmergencyMessage);
            txtEmergencyDate = itemView.findViewById(R.id.txtEmergencyDate);
            txtEmergencyTime = itemView.findViewById(R.id.txtEmergencyTime);
            txtEmergencyStatus = itemView.findViewById(R.id.txtEmergencyStatus);
            
            btnViewAlert = itemView.findViewById(R.id.btnViewAlert);
            btnAcknowledgeAlert = itemView.findViewById(R.id.btnAcknowledgeAlert);
            btnResolveAlert = itemView.findViewById(R.id.btnResolveAlert);
        }
    }
}
