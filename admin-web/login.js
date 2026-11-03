import { auth, db } from "./API/firebase.js";
import { signInWithEmailAndPassword } from "https://www.gstatic.com/firebasejs/9.6.11/firebase-auth.js";
import { ref, get } from "https://www.gstatic.com/firebasejs/9.6.11/firebase-database.js";

// DOM Elements
const loginForm = document.getElementById('loginForm');
const emailInput = document.getElementById('email');
const passwordInput = document.getElementById('password');
const errorMessage = document.getElementById('errorMessage');
const loginBtn = document.getElementById('loginBtn');
const btnText = loginBtn.querySelector('.btn-text');
const loader = loginBtn.querySelector('.loader');
const yearSpan = document.getElementById('year');

// Set current year in footer
yearSpan.textContent = new Date().getFullYear();

/**
 * Toggles the loading state of the button
 * @param {boolean} isLoading 
 */
function setLoading(isLoading) {
  if (isLoading) {
    loginBtn.disabled = true;
    btnText.textContent = 'Signing in...';
    loader.classList.remove('hidden');
  } else {
    loginBtn.disabled = false;
    btnText.textContent = 'Sign In';
    loader.classList.add('hidden');
  }
}

/**
 * Displays error message to the user
 * @param {string} message 
 */
function showError(message) {
  errorMessage.textContent = message;
}

// Handle Form Submission
loginForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  
  const email = emailInput.value.trim();
  const password = passwordInput.value;

  // Clear previous errors
  errorMessage.textContent = '';

  if (!email || !password) {
    showError("Please enter both email and password.");
    return;
  }

  setLoading(true);

  try {
    // 1. Authenticate with Firebase Auth
    const userCredential = await signInWithEmailAndPassword(auth, email, password);
    const uid = userCredential.user.uid;

    // 2. Verify Admin Role in Database
    const adminRef = ref(db, `Admins/${uid}`);
    const snapshot = await get(adminRef);

    if (snapshot.exists()) {
      // 3. Store Session and Redirect
      localStorage.setItem("adminUid", uid);
      window.location.href = "dashboard.html";
    } else {
      // User exists in Auth but is not in the Admins database
      throw new Error("Access denied. You do not have admin privileges.");
    }

  } catch (error) {
    console.error("Login Error:", error);
    
    let msg = "Failed to sign in. Please try again.";
    
    // Map Firebase error codes to user-friendly messages
    switch (error.code) {
      case 'auth/user-not-found':
      case 'auth/wrong-password':
        msg = "Invalid email or password.";
        break;
      case 'auth/invalid-email':
        msg = "The email address is badly formatted.";
        break;
      case 'auth/too-many-requests':
        msg = "Too many failed attempts. Please try again later.";
        break;
      default:
        // Use custom error message if available (e.g. "Access denied")
        if (error.message.includes("Access denied")) {
          msg = error.message;
        }
    }
    
    showError(msg);
    setLoading(false);
  }
});