const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

const REQUIRED_CONFIRMATION = 'DELETE_ALL_DEVELOPMENT_DATA';

function getFirebaseCLIToken() {
    const configPath = path.join(process.env.USERPROFILE || process.env.HOME || '', '.config', 'configstore', 'firebase-tools.json');
    if (fs.existsSync(configPath)) {
        try {
            const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
            if (config.tokens && config.tokens.access_token) {
                return config.tokens.access_token;
            }
        } catch (e) {
            // ignore JSON error
        }
    }
    return null;
}

function initFirebaseAdmin() {
    if (admin.apps.length > 0) return admin.app();

    const serviceKeyPath = path.join(__dirname, 'serviceAccountKey.json');
    if (fs.existsSync(serviceKeyPath)) {
        const serviceAccount = require(serviceKeyPath);
        return admin.initializeApp({
            credential: admin.credential.cert(serviceAccount),
            projectId: 'kannadaspeechassistant'
        });
    }

    const cliToken = getFirebaseCLIToken();
    if (cliToken) {
        return admin.initializeApp({
            credential: {
                getAccessToken: () => Promise.resolve({
                    expires_in: 3600,
                    access_token: cliToken
                })
            },
            projectId: 'kannadaspeechassistant'
        });
    }

    return admin.initializeApp({
        projectId: 'kannadaspeechassistant'
    });
}

async function listAllUserUids(nextPageToken) {
    let uids = [];
    const result = await admin.auth().listUsers(1000, nextPageToken);
    uids = uids.concat(result.users.map(u => u.uid));
    if (result.pageToken) {
        const moreUids = await listAllUserUids(result.pageToken);
        uids = uids.concat(moreUids);
    }
    return uids;
}

async function resetAuth(confirmToken) {
    if (confirmToken !== REQUIRED_CONFIRMATION) {
        console.error('ERROR: Deletion aborted.');
        console.error(`You must pass the exact confirmation phrase: "${REQUIRED_CONFIRMATION}"`);
        process.exit(1);
    }

    console.log('=== STARTING FIREBASE AUTHENTICATION RESET ===');
    initFirebaseAdmin();

    const uids = await listAllUserUids();
    console.log(`Found ${uids.length} user accounts in Firebase Authentication.`);

    if (uids.length === 0) {
        console.log('No user accounts to delete. Firebase Auth is clean.');
        return { found: 0, deleted: 0 };
    }

    let totalDeleted = 0;
    for (let i = 0; i < uids.length; i += 1000) {
        const batch = uids.slice(i, i + 1000);
        const deleteResult = await admin.auth().deleteUsers(batch);
        totalDeleted += deleteResult.successCount;
        if (deleteResult.failureCount > 0) {
            console.warn(`Warning: ${deleteResult.failureCount} users failed to delete.`);
            deleteResult.errors.forEach(err => console.error(err.error.toJSON()));
        }
    }

    console.log(`Successfully deleted ${totalDeleted} / ${uids.length} Firebase Auth user accounts.`);
    return { found: uids.length, deleted: totalDeleted };
}

if (require.main === module) {
    const args = process.argv.slice(2);
    const confirmArg = args.find(a => a.startsWith('--confirm='))?.split('=')[1] || args[0];
    resetAuth(confirmArg).catch(err => {
        console.error('Auth Reset Failed:', err);
        process.exit(1);
    });
}

module.exports = { resetAuth };
