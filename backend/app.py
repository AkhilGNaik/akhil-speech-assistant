import os
import json
import base64
import sqlite3
import io
from flask import Flask, request, jsonify

app = Flask(__name__)

# Config File Paths
DB_FILE = 'roles.db'
SERVICE_ACCOUNT_FILE = 'service-account.json'

# Firebase initialization flag
firebase_initialized = False

# Initialize SQLite database for storing persistent user roles and gesture logs
def init_db():
    conn = sqlite3.connect(DB_FILE)
    cursor = conn.cursor()
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS user_roles (
            uid TEXT PRIMARY KEY,
            email TEXT,
            role TEXT
        )
    ''')
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS gesture_logs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            uid TEXT,
            gesture TEXT,
            kannada_text TEXT,
            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    conn.commit()
    conn.close()
    print("SQLite Database initialized successfully.")

# Initialize Firebase Admin SDK if service-account.json is present
try:
    import firebase_admin
    from firebase_admin import credentials, auth
    
    if os.path.exists(SERVICE_ACCOUNT_FILE):
        cred = credentials.Certificate(SERVICE_ACCOUNT_FILE)
        firebase_admin.initialize_app(cred)
        firebase_initialized = True
        print(">>> Firebase Admin SDK initialized successfully with Service Account.")
    else:
        print(">>> WARNING: 'service-account.json' not found.")
        print(">>> Flask server will run in sandbox MOCK MODE (JWT signature verification is disabled).")
except Exception as e:
    print(f">>> Firebase Admin SDK load failed: {e}")
    print(">>> Flask server will run in sandbox MOCK MODE.")

# Fallback helper to decode standard JWT payload unsafely (no signature verification)
def unsafe_decode_jwt(token):
    try:
        parts = token.split('.')
        if len(parts) >= 2:
            payload_segment = parts[1]
            # Fix base64 padding
            payload_segment += '=' * (4 - len(payload_segment) % 4)
            decoded_bytes = base64.urlsafe_b64decode(payload_segment)
            return json.loads(decoded_bytes.decode('utf-8'))
    except Exception as e:
        print(f"Error parsing token unsafely: {e}")
    return {}

# Query or assign a default user role
def get_or_create_user_role(uid, email):
    conn = sqlite3.connect(DB_FILE)
    cursor = conn.cursor()
    cursor.execute('SELECT role FROM user_roles WHERE uid = ?', (uid,))
    row = cursor.fetchone()
    
    if row:
        conn.close()
        return row[0]
    
    # Auto-resolve role based on email keywords if first time logging in
    role = "Speech-Impaired User" # Default
    if email:
        email_lower = email.lower()
        if "admin" in email_lower or "caregiver" in email_lower:
            role = "Admin/Caregiver"
        elif "physical" in email_lower:
            role = "Physically Disabled User"
            
    cursor.execute('INSERT INTO user_roles (uid, email, role) VALUES (?, ?, ?)', (uid, email, role))
    conn.commit()
    conn.close()
    print(f"Registered new user session: {email} -> {role}")
    return role

# API: Authenticate User and resolve Role
@app.route('/api/login', methods=['POST'])
def api_login():
    data = request.get_json()
    if not data or 'idToken' not in data:
        return jsonify({"status": "error", "message": "Missing 'idToken' in request body."}), 400
        
    id_token = data['idToken']
    
    uid = None
    email = None
    
    if firebase_initialized:
        try:
            # Verify ID Token securely against Firebase servers
            decoded_token = auth.verify_id_token(id_token)
            uid = decoded_token.get('uid')
            email = decoded_token.get('email')
        except Exception as e:
            return jsonify({"status": "error", "message": f"Firebase Token verification failed: {str(e)}"}), 401
    else:
        # Fallback Mock Mode: Extract values directly from token structure
        decoded_token = unsafe_decode_jwt(id_token)
        uid = decoded_token.get('user_id') or decoded_token.get('uid') or "mock_uid_12345"
        email = decoded_token.get('email') or "mock_user@example.com"
        
    # Get user role from local SQLite mapping database
    role = get_or_create_user_role(uid, email)
    
    return jsonify({
        "status": "success",
        "uid": uid,
        "email": email,
        "role": role
    })

# API: Explicitly register or update user role
@app.route('/api/register', methods=['POST'])
def api_register():
    data = request.get_json()
    if not data or 'uid' not in data or 'role' not in data:
        return jsonify({"status": "error", "message": "Missing required fields 'uid' or 'role'."}), 400
        
    uid = data['uid']
    email = data.get('email', '')
    role = data['role']
    
    valid_roles = ["Speech-Impaired User", "Physically Disabled User", "Admin/Caregiver"]
    if role not in valid_roles:
        return jsonify({"status": "error", "message": f"Invalid role. Must be one of: {valid_roles}"}), 400
        
    conn = sqlite3.connect(DB_FILE)
    cursor = conn.cursor()
    cursor.execute('INSERT OR REPLACE INTO user_roles (uid, email, role) VALUES (?, ?, ?)', (uid, email, role))
    conn.commit()
    conn.close()
    
    return jsonify({
        "status": "success",
        "message": f"Successfully set role for {uid} to {role}."
    })

# API: Admin get all mappings
@app.route('/api/users', methods=['GET'])
def api_get_users():
    conn = sqlite3.connect(DB_FILE)
    cursor = conn.cursor()
    cursor.execute('SELECT uid, email, role FROM user_roles')
    rows = cursor.fetchall()
    conn.close()
    
    users = [{"uid": r[0], "email": r[1], "role": r[2]} for r in rows]
    return jsonify(users)

# API: Text to Speech Synthesis (supporting Kannada and fallback)
@app.route('/api/tts', methods=['POST'])
def api_tts():
    data = request.get_json()
    if not data or 'text' not in data:
        return jsonify({"status": "error", "message": "Missing 'text' in request body."}), 400
        
    text = data['text']
    print(f"TTS Synthesis requested for: '{text}'")
    
    # Try using gTTS if available
    try:
        from gtts import gTTS
        fp = io.BytesIO()
        # Try to synthesize using Google TTS (works online)
        tts = gTTS(text=text, lang='kn') # Kannada support!
        tts.write_to_fp(fp)
        fp.seek(0)
        return fp.read(), 200, {'Content-Type': 'audio/mpeg'}
    except Exception as e:
        print(f"gTTS not available or offline ({e}). Generating standard-library mock synth wave.")
        
    # Standard library fallback (generates a valid, playable sine wave WAV file)
    try:
        import wave
        import struct
        import math
        
        sample_rate = 8000
        duration = 0.8  # seconds
        frequency = 350.0  # Hz
        
        num_samples = int(duration * sample_rate)
        wav_buf = io.BytesIO()
        
        with wave.open(wav_buf, 'wb') as wav_file:
            wav_file.setnchannels(1)
            wav_file.setsampwidth(2)
            wav_file.setframerate(sample_rate)
            
            for i in range(num_samples):
                t = float(i) / sample_rate
                value = int(32767.0 * 0.4 * math.sin(2.0 * math.pi * frequency * t))
                data = struct.pack('<h', value)
                wav_file.writeframesraw(data)
                
        wav_buf.seek(0)
        return wav_buf.read(), 200, {'Content-Type': 'audio/wav'}
    except Exception as ex:
        print(f"TTS synthesis fallback failed: {ex}")
        return jsonify({"status": "error", "message": f"TTS synthesis failed: {str(ex)}"}), 500

# API: Translate gesture label to Kannada text and log the event
@app.route('/api/gesture_to_kannada', methods=['POST'])
def api_gesture_to_kannada():
    data = request.get_json()
    if not data or 'gesture' not in data:
        return jsonify({"status": "error", "message": "Missing 'gesture' in request body."}), 400
    
    gesture = data['gesture']
    uid = data.get('uid', 'anonymous_user')
    
    # Map gesture labels to Kannada sentences
    gesture_mapping = {
        "Fist": "ತುರ್ತು ಸಹಾಯ ಬೇಕು! ತಕ್ಷಣ ಸಂಪರ್ಕಿಸಿ. (Emergency help needed! Contact immediately.)",
        "Open_Palm": "ದಯವಿಟ್ಟು ನನ್ನ ರಕ್ಷಕರನ್ನು ಕರೆಯಿರಿ. (Please call my caregiver.)",
        "Thumbs_Up": "ನಾನು ಸುರಕ್ಷಿತವಾಗಿದ್ದೇನೆ ಮತ್ತು ಆರಾಮವಾಗಿದ್ದೇನೆ. (I am safe and comfortable.)",
        "Thumbs_Down": "ನನಗೆ ಅಸ್ವಸ್ಥತೆ ಎನಿಸುತ್ತಿದೆ, ಸಹಾಯ ಬೇಕು. (I am feeling unwell, need help.)",
        "Victory": "ಎಲ್ಲವೂ ಚೆನ್ನಾಗಿದೆ ಮತ್ತು ಯಶಸ್ವಿಯಾಗಿದೆ. (Everything is fine and successful.)",
        "Wave": "ನಮಸ್ಕಾರ, ನನ್ನ ಗಮನ ಸೆಳೆಯಲು ಬಯಸುತ್ತೇನೆ. (Hello, wanting to draw attention.)",
        "Ok": "ಸರಿ, ಒಪ್ಪಿಗೆ ನೀಡಲಾಗಿದೆ. (Okay, approved.)"
    }
    
    kannada_text = gesture_mapping.get(gesture, f"ಗುರುತಿಸಲಾಗದ ಸನ್ನೆ: {gesture} (Unrecognized gesture)")
    
    # Log translation request to SQLite db
    try:
        conn = sqlite3.connect(DB_FILE)
        cursor = conn.cursor()
        cursor.execute('''
            INSERT INTO gesture_logs (uid, gesture, kannada_text)
            VALUES (?, ?, ?)
        ''', (uid, gesture, kannada_text))
        conn.commit()
        conn.close()
        print(f"Logged gesture: {gesture} -> {kannada_text} for User: {uid}")
    except Exception as e:
        print(f"Database logging failed for gesture: {e}")
        
    return jsonify({
        "status": "success",
        "gesture": gesture,
        "kannada_text": kannada_text
    })

if __name__ == '__main__':
    # Ensure database table is created
    init_db()
    
    # Run server on port 5000, accessible locally and in LAN (0.0.0.0)
    app.run(host='0.0.0.0', port=5000, debug=True)
