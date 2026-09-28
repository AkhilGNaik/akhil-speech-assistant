package com.kannada.speechassistant;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages operations on caregiver_connections collection:
 * - Sending connection requests
 * - Accepting connection requests
 * - Rejecting connection requests
 * - Querying relations
 */
public class UserDashboardConnectionManager {

    private final FirebaseFirestore db;

    public UserDashboardConnectionManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Patient action: Send a new caregiver connection request.
     */
    public void sendConnectionRequest(String patientUid, String patientName, String patientEmail, 
                                      String caregiverEmail, OnCompleteListener<DocumentReference> listener) {
        Map<String, Object> request = new HashMap<>();
        request.put("patientUid", patientUid);
        request.put("patientName", patientName);
        request.put("patientEmail", patientEmail);
        request.put("caregiverEmail", caregiverEmail.toLowerCase().trim());
        request.put("caregiverUid", ""); // Updated upon acceptance
        request.put("status", "Pending");
        request.put("timestamp", Timestamp.now());

        db.collection("caregiver_connections")
                .add(request)
                .addOnCompleteListener(listener);
    }

    /**
     * Caregiver action: Accept a connection request.
     */
    public void acceptConnectionRequest(String connectionDocId, String caregiverUid, OnCompleteListener<Void> listener) {
        db.collection("caregiver_connections")
                .document(connectionDocId)
                .update(
                        "status", "Accepted",
                        "caregiverUid", caregiverUid,
                        "timestamp", Timestamp.now()
                )
                .addOnCompleteListener(listener);
    }

    /**
     * Caregiver action: Reject/cancel a connection request.
     */
    public void rejectConnectionRequest(String connectionDocId, OnCompleteListener<Void> listener) {
        db.collection("caregiver_connections")
                .document(connectionDocId)
                .update(
                        "status", "Rejected",
                        "timestamp", Timestamp.now()
                )
                .addOnCompleteListener(listener);
    }

    /**
     * Check if a caregiver with the given email exists in the users database.
     */
    public void checkCaregiverExists(String email, OnCompleteListener<com.google.firebase.firestore.QuerySnapshot> listener) {
        db.collection("users")
                .whereEqualTo("email", email.toLowerCase().trim())
                .whereEqualTo("role", RoleManager.ROLE_ADMIN_CAREGIVER)
                .get()
                .addOnCompleteListener(listener);
    }
}
