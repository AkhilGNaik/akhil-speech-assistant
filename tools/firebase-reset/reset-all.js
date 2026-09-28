const { resetAuth } = require('./reset-auth');
const { resetFirestore } = require('./reset-firestore');

const REQUIRED_CONFIRMATION = 'DELETE_ALL_DEVELOPMENT_DATA';

async function main() {
    const args = process.argv.slice(2);
    const confirmArg = args.find(a => a.startsWith('--confirm='))?.split('=')[1] || args[0];

    if (confirmArg !== REQUIRED_CONFIRMATION) {
        console.log('============================================================');
        console.log('WARNING: DESTRUCTIVE OPERATION');
        console.log('This operation permanently deletes Firebase Authentication users');
        console.log('and Cloud Firestore application data from project "kannadaspeechassistant".');
        console.log('============================================================');
        console.log('\nTo proceed, re-run with confirmation phrase:');
        console.log(`  node reset-all.js --confirm=${REQUIRED_CONFIRMATION}`);
        process.exit(1);
    }

    console.log('============================================================');
    console.log('DEVELOPMENT DATA CLEANUP INITIATED');
    console.log('============================================================\n');

    const authStats = await resetAuth(confirmArg);
    console.log('');
    const firestoreStats = await resetFirestore(confirmArg);

    console.log('\n============================================================');
    console.log('DATA RESET COMPLETED SUCCESSFULLY');
    console.log('============================================================');
    console.log(`Auth accounts deleted: ${authStats.deleted} / ${authStats.found}`);
    console.log(`Firestore collections reset: ${firestoreStats.collectionsInspected}`);
    console.log(`Firestore top-level documents deleted: ${firestoreStats.totalDocsDeleted}`);
}

main().catch(err => {
    console.error('Data Reset Failed:', err);
    process.exit(1);
});
