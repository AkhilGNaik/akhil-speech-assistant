package com.kannada.speechassistant;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Adapter for displaying users in the Admin/Caregiver Dashboard.
 * Supports filtering by role and search queries (name/email).
 */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(UserRecord userDoc);
    }

    private final List<UserRecord> allUsers = new ArrayList<>();
    private final List<UserRecord> filteredUsers = new ArrayList<>();
    private final OnUserClickListener clickListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    // Color palette for avatars
    private final int[] avatarColors = {
            Color.parseColor("#4F46E5"), // Indigo
            Color.parseColor("#0D9488"), // Teal
            Color.parseColor("#7C3AED"), // Purple
            Color.parseColor("#DB2777"), // Pink
            Color.parseColor("#D97706")  // Amber
    };

    public UserAdapter(OnUserClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setUsers(List<UserRecord> users) {
        this.allUsers.clear();
        Set<String> seenEmails = new HashSet<>();
        Set<String> seenIds = new HashSet<>();
        if (users != null) {
            for (UserRecord u : users) {
                if (u == null) continue;
                String email = u.getString("email");
                String id = u.getId();
                String normEmail = (email != null && !email.trim().isEmpty()) ? email.toLowerCase().trim() : null;

                if (normEmail != null && seenEmails.contains(normEmail)) {
                    continue;
                }
                if (id != null && !id.trim().isEmpty() && seenIds.contains(id.trim())) {
                    continue;
                }

                if (normEmail != null) seenEmails.add(normEmail);
                if (id != null && !id.trim().isEmpty()) seenIds.add(id.trim());
                this.allUsers.add(u);
            }
        }
        this.filteredUsers.clear();
        this.filteredUsers.addAll(this.allUsers);
        notifyDataSetChanged();
    }

    /**
     * Filters the user database list in real-time.
     */
    public void filter(String query, String roleFilter) {
        filteredUsers.clear();
        String lowercaseQuery = query.toLowerCase().trim();

        for (UserRecord doc : allUsers) {
            String name = doc.getString("name");
            String email = doc.getString("email");
            String role = doc.getString("role");

            name = name != null ? name : "";
            email = email != null ? email : "";
            role = role != null ? role : "";

            // Check Role Filter
            boolean matchesRole = true;
            if (!"All".equalsIgnoreCase(roleFilter) && !roleFilter.isEmpty()) {
                if (RoleManager.ROLE_SPEECH_IMPAIRED.equalsIgnoreCase(roleFilter)
                        || RoleManager.ROLE_DEAF_USER.equalsIgnoreCase(roleFilter)
                        || "deaf".equalsIgnoreCase(roleFilter)
                        || "speech".equalsIgnoreCase(roleFilter)
                        || "speech_impaired".equalsIgnoreCase(roleFilter)) {
                    matchesRole = RoleManager.ROLE_SPEECH_IMPAIRED.equalsIgnoreCase(role)
                            || RoleManager.ROLE_DEAF_USER.equalsIgnoreCase(role)
                            || "deaf".equalsIgnoreCase(role)
                            || "speech".equalsIgnoreCase(role)
                            || "speech_impaired".equalsIgnoreCase(role);
                } else if (RoleManager.ROLE_BLIND_USER.equalsIgnoreCase(roleFilter)
                        || "blind".equalsIgnoreCase(roleFilter)) {
                    matchesRole = RoleManager.ROLE_BLIND_USER.equalsIgnoreCase(role)
                            || "blind".equalsIgnoreCase(role);
                } else if (RoleManager.ROLE_MUTE_USER.equalsIgnoreCase(roleFilter)
                        || "mute".equalsIgnoreCase(roleFilter)) {
                    matchesRole = RoleManager.ROLE_MUTE_USER.equalsIgnoreCase(role)
                            || "mute".equalsIgnoreCase(role);
                } else if (RoleManager.ROLE_ADMIN_CAREGIVER.equalsIgnoreCase(roleFilter)
                        || "caregiver".equalsIgnoreCase(roleFilter)
                        || "admin".equalsIgnoreCase(roleFilter)) {
                    matchesRole = RoleManager.ROLE_ADMIN_CAREGIVER.equalsIgnoreCase(role)
                            || "caregiver".equalsIgnoreCase(role)
                            || "admin".equalsIgnoreCase(role);
                } else {
                    matchesRole = role.equalsIgnoreCase(roleFilter);
                }
            }

            // Check Search Query
            boolean matchesQuery = true;
            if (!lowercaseQuery.isEmpty()) {
                matchesQuery = name.toLowerCase().contains(lowercaseQuery) ||
                               email.toLowerCase().contains(lowercaseQuery);
            }

            if (matchesRole && matchesQuery) {
                filteredUsers.add(doc);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserRecord doc = filteredUsers.get(position);

        String name = doc.getString("name");
        String email = doc.getString("email");
        String role = doc.getString("role");
        Timestamp regTime = doc.getTimestamp("registrationDate");

        name = name != null ? name : "Anonymous User";
        email = email != null ? email : "No Email";
        role = role != null ? role : "Unknown Role";

        holder.txtName.setText(name);
        holder.txtEmail.setText(email);

        // Bind Avatar Letter and Color
        String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        holder.txtAvatar.setText(initial);
        int colorIndex = Math.abs(name.hashCode()) % avatarColors.length;
        holder.avatarCard.setCardBackgroundColor(avatarColors[colorIndex]);

        // Bind Registration Date
        if (regTime != null) {
            holder.txtRegDate.setText("Registered: " + dateFormat.format(regTime.toDate()));
        } else {
            holder.txtRegDate.setText("Registered: N/A");
        }

        // Format Role Badge Color dynamically
        int badgeBg;
        int badgeText;
        if (RoleManager.ROLE_MUTE_USER.equals(role)) {
            badgeBg = Color.parseColor("#DBEAFE"); // Light Blue
            badgeText = Color.parseColor("#1D4ED8");
        } else if (RoleManager.ROLE_SPEECH_IMPAIRED.equals(role) || RoleManager.ROLE_DEAF_USER.equals(role)) {
            badgeBg = Color.parseColor("#E0E7FF"); // Light Indigo
            badgeText = Color.parseColor("#4F46E5");
        } else if (RoleManager.ROLE_PHYSICALLY_DISABLED.equals(role)) {
            badgeBg = Color.parseColor("#CCFBF1"); // Light Teal
            badgeText = Color.parseColor("#0F766E");
        } else if (RoleManager.ROLE_BLIND_USER.equals(role)) {
            badgeBg = Color.parseColor("#FCE7F3"); // Light Pink
            badgeText = Color.parseColor("#DB2777");
        } else if (RoleManager.ROLE_ADMIN_CAREGIVER.equals(role)) {
            badgeBg = Color.parseColor("#FEF3C7"); // Warm Amber
            badgeText = Color.parseColor("#B45309");
        } else {
            badgeBg = Color.parseColor("#F1F5F9"); // Light Slate
            badgeText = Color.parseColor("#475569");
        }

        if (RoleManager.ROLE_MUTE_USER.equals(role)) {
            holder.txtRoleBadge.setText("Mute User");
        } else if (RoleManager.ROLE_SPEECH_IMPAIRED.equals(role) || RoleManager.ROLE_DEAF_USER.equals(role)) {
            holder.txtRoleBadge.setText("Deaf User");
        } else if (RoleManager.ROLE_BLIND_USER.equals(role)) {
            holder.txtRoleBadge.setText("Blind User");
        } else if (RoleManager.ROLE_ADMIN_CAREGIVER.equals(role)) {
            holder.txtRoleBadge.setText("Caregiver");
        } else {
            holder.txtRoleBadge.setText(role.replace(" User", ""));
        }
        holder.txtRoleBadge.setBackgroundTintList(ColorStateList.valueOf(badgeBg));
        holder.txtRoleBadge.setTextColor(badgeText);

        // Bind Language Badge below Role Badge
        if (holder.txtLanguageBadge != null) {
            String rawLang = doc.getString("language");
            String langDisplay = LanguageManager.getLanguageDisplayName(rawLang);
            holder.txtLanguageBadge.setText(langDisplay);

            int langBg;
            int langText;
            String normCode = LanguageManager.normalizeLanguageCode(rawLang);
            switch (normCode) {
                case LanguageManager.LANG_HINDI:
                    langBg = Color.parseColor("#FEF3C7"); // Warm Amber
                    langText = Color.parseColor("#B45309");
                    break;
                case LanguageManager.LANG_MALAYALAM:
                    langBg = Color.parseColor("#DCFCE7"); // Mint Green
                    langText = Color.parseColor("#15803D");
                    break;
                case LanguageManager.LANG_ENGLISH:
                    langBg = Color.parseColor("#F3E8FF"); // Soft Purple
                    langText = Color.parseColor("#7E22CE");
                    break;
                case LanguageManager.LANG_KANNADA:
                default:
                    langBg = Color.parseColor("#E0F2FE"); // Light Sky Blue
                    langText = Color.parseColor("#0369A1");
                    break;
            }
            holder.txtLanguageBadge.setBackgroundTintList(ColorStateList.valueOf(langBg));
            holder.txtLanguageBadge.setTextColor(langText);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onUserClick(doc);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredUsers.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView avatarCard;
        TextView txtAvatar, txtName, txtEmail, txtRegDate, txtRoleBadge, txtLanguageBadge;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarCard = itemView.findViewById(R.id.avatarCard);
            txtAvatar = itemView.findViewById(R.id.txtAvatar);
            txtName = itemView.findViewById(R.id.txtName);
            txtEmail = itemView.findViewById(R.id.txtEmail);
            txtRegDate = itemView.findViewById(R.id.txtRegDate);
            txtRoleBadge = itemView.findViewById(R.id.txtRoleBadge);
            txtLanguageBadge = itemView.findViewById(R.id.txtLanguageBadge);
        }
    }
}
