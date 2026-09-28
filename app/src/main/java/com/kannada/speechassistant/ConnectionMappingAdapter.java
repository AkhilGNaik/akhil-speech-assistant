package com.kannada.speechassistant;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying caregiver-to-patient connections and requests.
 */
public class ConnectionMappingAdapter extends RecyclerView.Adapter<ConnectionMappingAdapter.ConnectionViewHolder> {

    private final List<DocumentSnapshot> connectionsList = new ArrayList<>();

    public void setConnections(List<DocumentSnapshot> connections) {
        this.connectionsList.clear();
        this.connectionsList.addAll(connections);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConnectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_connection_mapping, parent, false);
        return new ConnectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConnectionViewHolder holder, int position) {
        DocumentSnapshot doc = connectionsList.get(position);

        String caregiverEmail = doc.getString("caregiverEmail");
        String patientName = doc.getString("patientName");
        String patientEmail = doc.getString("patientEmail");
        String status = doc.getString("status");

        caregiverEmail = caregiverEmail != null ? caregiverEmail : "No Caregiver Email";
        patientName = patientName != null ? patientName : "Anonymous Patient";
        patientEmail = patientEmail != null ? patientEmail : "No Patient Email";
        status = status != null ? status : "Pending";

        holder.txtConnectionCaregiver.setText(caregiverEmail);
        holder.txtConnectionPatient.setText(patientName + " (" + patientEmail + ")");

        // Formatting Status Badge dynamically
        int statusColor;
        int statusBg;
        if ("Accepted".equalsIgnoreCase(status)) {
            statusColor = Color.parseColor("#059669"); // Emerald Green
            statusBg = Color.parseColor("#D1FAE5");
        } else if ("Pending".equalsIgnoreCase(status)) {
            statusColor = Color.parseColor("#D97706"); // Amber Orange
            statusBg = Color.parseColor("#FEF3C7");
        } else {
            statusColor = Color.parseColor("#DC2626"); // Red
            statusBg = Color.parseColor("#FEE2E2");
        }

        holder.txtConnectionStatus.setText(status.toUpperCase());
        holder.txtConnectionStatus.setTextColor(statusColor);
        holder.txtConnectionStatus.setBackgroundTintList(ColorStateList.valueOf(statusBg));
    }

    @Override
    public int getItemCount() {
        return connectionsList.size();
    }

    public static class ConnectionViewHolder extends RecyclerView.ViewHolder {
        TextView txtConnectionStatus, txtConnectionCaregiver, txtConnectionPatient;

        public ConnectionViewHolder(@NonNull View itemView) {
            super(itemView);
            txtConnectionStatus = itemView.findViewById(R.id.txtConnectionStatus);
            txtConnectionCaregiver = itemView.findViewById(R.id.txtConnectionCaregiver);
            txtConnectionPatient = itemView.findViewById(R.id.txtConnectionPatient);
        }
    }
}
