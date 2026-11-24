import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import { getAnalytics } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-analytics.js";
import { getFirestore } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";
// NOT using Firebase Auth - using Firestore username/password verification instead
// import { getAuth } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";
import { getStorage } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-storage.js";

const firebaseConfig = {
  apiKey: "AIzaSyBhHN12DOUa5K1ELsrfraHMNlhxIJYf7RI",
  authDomain: "blockbuster-49895.firebaseapp.com",
  projectId: "blockbuster-49895",
  storageBucket: "blockbuster-49895.firebasestorage.app",
  messagingSenderId: "906545575086",
  appId: "1:906545575086:web:fde8bcb88e199beedce93f",
  measurementId: "G-PEYLNT869P"
};

const app = initializeApp(firebaseConfig);
const analytics = getAnalytics(app);
const db = getFirestore(app);
// NOT initializing auth - not needed for username/password Firestore verification
// const auth = getAuth(app);
const storage = getStorage(app);

export { db, storage, app };