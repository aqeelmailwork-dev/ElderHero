// Copy this file to firebase-config.js (not committed) and fill in your values.
// With the emulator, the demo- project below needs no real Firebase account.
export const firebaseConfig = {
  apiKey: "demo-api-key-not-a-real-key",
  authDomain: "demo-elderhero.firebaseapp.com",
  databaseURL: "https://demo-elderhero-default-rtdb.firebaseio.com",
  projectId: "demo-elderhero",
  storageBucket: "demo-elderhero.appspot.com",
  messagingSenderId: "000000000000",
  appId: "1:000000000000:web:000000000000000000000000"
};

// Point at the Firebase Local Emulator Suite instead of production.
export const USE_EMULATOR = true;
export const EMULATOR_HOST = "192.168.1.100";
