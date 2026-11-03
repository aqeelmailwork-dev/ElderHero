// API/firebase.js
import { initializeApp } from "https://www.gstatic.com/firebasejs/9.6.11/firebase-app.js";
import { getAuth, connectAuthEmulator } from "https://www.gstatic.com/firebasejs/9.6.11/firebase-auth.js";
import { getDatabase, connectDatabaseEmulator } from "https://www.gstatic.com/firebasejs/9.6.11/firebase-database.js";

import { firebaseConfig, USE_EMULATOR, EMULATOR_HOST } from "./firebase-config.js";

// Initialize Firebase app
const app = initializeApp(firebaseConfig);

// Export auth + db
const auth = getAuth(app);
const db = getDatabase(app);

// Point at the homelab Firebase emulator instead of production.
if (USE_EMULATOR) {
  connectAuthEmulator(auth, `http://${EMULATOR_HOST}:9099`, { disableWarnings: true });
  connectDatabaseEmulator(db, EMULATOR_HOST, 9010);
}

export { auth, db };
