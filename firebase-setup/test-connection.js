/**
 * Test Firebase Connection
 *
 * This script tests the connection to Firebase and verifies the setup.
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

async function testConnection() {
  try {
    console.log('🔍 Testing Firestore connection...\n');

    // Try to read from a test collection
    const testRef = db.collection('test').doc('connection-test');
    await testRef.set({
      timestamp: admin.firestore.Timestamp.now(),
      message: 'Connection test successful'
    });

    const doc = await testRef.get();

    if (doc.exists) {
      console.log('✅ Firestore connection successful!');
      console.log('📄 Test document data:', doc.data());

      // Clean up test document
      await testRef.delete();
      console.log('🗑️  Test document cleaned up\n');

      // Show project info
      console.log('📊 Project Information:');
      console.log(`   Project ID: ${admin.app().options.projectId}`);
      console.log(`   Database: (default)\n`);

      console.log('✨ Your Firebase setup is working correctly!\n');
      console.log('Next steps:');
      console.log('   1. Run: npm run insert-mock');
      console.log('   2. Check Firebase Console for data\n');
    }

  } catch (error) {
    console.error('❌ Error testing connection:', error);
    process.exit(1);
  }
}

testConnection()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error('❌ Test failed:', error);
    process.exit(1);
  });
