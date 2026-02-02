/**
 * Clear Firestore Data
 *
 * This script clears all data from Firestore collections.
 * Use with caution!
 */

const admin = require('firebase-admin');

try {
  const serviceAccount = require('./serviceAccountKey.json');

  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });

  console.log('✅ Firebase Admin SDK initialized successfully\n');
} catch (error) {
  console.error('❌ Error initializing Firebase Admin SDK:');
  console.error('   Make sure serviceAccountKey.json exists in this directory');
  process.exit(1);
}

const db = admin.firestore();

async function deleteCollection(collectionName) {
  const collectionRef = db.collection(collectionName);
  const query = collectionRef.limit(500);

  return new Promise((resolve, reject) => {
    deleteQueryBatch(query, resolve).catch(reject);
  });
}

async function deleteQueryBatch(query, resolve) {
  const snapshot = await query.get();

  const batchSize = snapshot.size;
  if (batchSize === 0) {
    resolve();
    return;
  }

  const batch = db.batch();
  snapshot.docs.forEach((doc) => {
    batch.delete(doc.ref);
  });
  await batch.commit();

  process.nextTick(() => {
    deleteQueryBatch(query, resolve);
  });
}

async function clearFirestore() {
  try {
    console.log('⚠️  WARNING: This will delete all data from Firestore!\n');

    const collections = ['metrics', 'recommendations', 'insights', 'deployments'];

    for (const collectionName of collections) {
      console.log(`🗑️  Deleting collection: ${collectionName}...`);
      await deleteCollection(collectionName);
      console.log(`   ✅ Deleted ${collectionName}`);
    }

    console.log('\n✨ All data cleared successfully!\n');

  } catch (error) {
    console.error('\n❌ Error clearing data:', error);
    process.exit(1);
  }
}

// Confirm before running
console.log('⚠️  WARNING: This will delete ALL data from Firestore!');
console.log('Press Ctrl+C to cancel, or wait 5 seconds to continue...\n');

setTimeout(() => {
  clearFirestore()
    .then(() => process.exit(0))
    .catch((error) => {
      console.error('❌ Failed:', error);
      process.exit(1);
    });
}, 5000);
