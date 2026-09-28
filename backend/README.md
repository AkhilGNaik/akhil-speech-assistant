# Speech Assistant Python Backend

A Flask-based backend service designed to verify Firebase Authentication ID tokens and manage user roles (Speech-Impaired User, Physically Disabled User, Admin/Caregiver) for the Speech Assistant Android application.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Installation](#installation)
3. [Firebase Configuration](#firebase-configuration)
4. [Running the Server](#running-the-server)
5. [Testing & Sandboxing](#testing--sandboxing)
6. [API Reference](#api-reference)

---

## Prerequisites
- **Python 3.8+** installed on your system.
- **pip** (Python package installer).

---

## Installation

1. Open your terminal and navigate to this backend directory:
   ```bash
   cd "c:/ak/ak mit project/backend"
   ```

2. Install the required libraries using pip:
   ```bash
   pip install -r requirements.txt
   ```

---

## Firebase Configuration

This backend uses the **Firebase Admin SDK** to cryptographically verify client authentication tokens.

1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Select your project.
3. Click on the gear icon (Project Settings) -> **Service Accounts**.
4. Click **Generate New Private Key**.
5. Save the downloaded JSON file in this directory (`backend/`) as **`service-account.json`**.

### Sandbox Mock Mode (Automatic Fallback)
If `service-account.json` is **not** present, the server automatically starts in **Sandbox Mock Mode**. In this mode, the server decodes the token claims (email, UID) unsafely (without signature verification). This allows developers to test integration immediately without setting up Firebase credentials.

---

## Running the Server

Start the Flask application by running:
```bash
python app.py
```
By default, the server runs on port **5000** and binds to `0.0.0.0`, meaning it is accessible to:
- The Android Emulator (using `http://10.0.2.2:5000/api/login`)
- Other devices on the local area network (using `http://<your-host-ip>:5000/api/login`)

---

## Testing & Sandboxing

### Dynamic Role Assignment
To simplify development testing, the backend assigns roles automatically based on email keywords if the user logs in for the first time:

| Email Keyword | Assigned Role | Target Interface |
| :--- | :--- | :--- |
| **`admin`** or **`caregiver`** | `Admin/Caregiver` | Caregiver Portal & Config Logs |
| **`physical`** | `Physically Disabled User` | Oversized emergency widgets |
| *default* | `Speech-Impaired User` | Kannada AAC Speech-Card Grid |

*Example test logins:*
- `caregiver.test@example.com` -> Admin/Caregiver Role
- `physical.touch@example.com` -> Physically Disabled User Role
- `patient.speech@example.com` -> Speech-Impaired User Role

---

## API Reference

### 1. Verification & Login
- **Endpoint**: `/api/login`
- **Method**: `POST`
- **Payload**:
  ```json
  {
    "idToken": "<firebase_id_token>"
  }
  ```
- **Response**:
  ```json
  {
    "status": "success",
    "uid": "user_firebase_uid",
    "email": "user@example.com",
    "role": "Speech-Impaired User"
  }
  ```

### 2. Manual Role Override
- **Endpoint**: `/api/register`
- **Method**: `POST`
- **Payload**:
  ```json
  {
    "uid": "user_firebase_uid",
    "email": "optional_email",
    "role": "Admin/Caregiver"
  }
  ```
- **Response**:
  ```json
  {
    "status": "success",
    "message": "Successfully set role for user_firebase_uid to Admin/Caregiver."
  }
  ```

### 3. List Mapped Users
- **Endpoint**: `/api/users`
- **Method**: `GET`
- **Response**:
  ```json
  [
    {
      "uid": "uid_1",
      "email": "user1@example.com",
      "role": "Speech-Impaired User"
    }
  ]
  ```
