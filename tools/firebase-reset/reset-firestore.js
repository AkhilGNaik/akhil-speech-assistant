const { execSync } = require('child_process');
const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

const REQUIRED_CONFIRMATION = 'DELETE_ALL_DEVELOPMENT_DATA';

const KNOWN_COLLECTIONS = [
    'users',
    'caregiver_connections',
    'caregiver_messages',
    'emergencies',
    'EmergencyAlerts',
    'emergency_alerts',
    'gesture_history',
    'chats'
];

async function resetFirestore(confirmToken) {
    if (confirmToken !== REQUIRED_CONFIRMATION) {
        console.error('ERROR: Deletion aborted.');
        console.error(`You must pass the exact confirmation phrase: "${REQUIRED_CONFIRMATION}"`);
        process.exit(1);
    }

    console.log('=== STARTING CLOUD FIRESTORE RESET ===');

    try {
        console.log('Executing Firebase CLI Firestore collection purge...');
        const output = execSync('npx --yes firebase-tools firestore:delete --all-collections --project kannadaspeechassistant -f', {
            encoding: 'utf8'
        });
        console.log(output.trim());
        console.log('Firestore cleanup complete: All collections and nested documents deleted.');
        return { collectionsInspected: KNOWN_COLLECTIONS.length, totalDocsDeleted: 'ALL' };
    } catch (err) {
        console.error('Firebase CLI execution error, attempting Admin SDK fallback:', err.message);

        const serviceKeyPath = path.join(__dirname, 'serviceAccountKey.json');
        if (fs.existsSync(serviceKeyPath)) {
            if (admin.apps.length === 0) {
                admin.initializeApp({
                    credential: admin.credential.cert(require(serviceKeyPath)),
                    projectId: 'kannadaspeechassistant'
                });
            }
            const db = admin.firestore();
            let totalDeleted = 0;
            for (const colName of KNOWN_COLLECTIONS) {
                const colRef = db.collection(colName);
                const snapshot = await colRef.get();
                if (snapshot.size > 0) {
                    await db.recursiveDelete(colRef);
                    totalDeleted += snapshot.size;
                }
            }
            return { collectionsInspected: KNOWN_COLLECTIONS.length, totalDocsDeleted: totalDeleted };
        } else {
            throw err;
        }
    }
}

if (require.main === module) {
    const args = process.argv.slice(2);
    const confirmArg = args.find(a => a.startsWith('--confirm='))?.split('=')[1] || args[0];
    resetFirestore(confirmArg).catch(err => {
        console.error('Firestore Reset Failed:', err);
        process.exit(1);
    });
}

module.exports = { resetFirestore };
