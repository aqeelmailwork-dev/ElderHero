import { initLayout } from './layout.js';
import { db, auth } from "./API/firebase.js";
import { ref, get } from "https://www.gstatic.com/firebasejs/9.6.11/firebase-database.js";
import { onAuthStateChanged } from "https://www.gstatic.com/firebasejs/9.6.11/firebase-auth.js";

// 1. Initialize Layout
document.addEventListener("DOMContentLoaded", () => {
  initLayout('carelist.html');
  
  // 2. Auth Guard & Data Loading
  onAuthStateChanged(auth, (user) => {
    if (!user) {
      window.location.href = "login.html";
      return;
    }
    loadCaretakers();
  });
});

// Global state for search filtering
let allCaretakers = [];

// --- Main Data Loader ---
async function loadCaretakers() {
  const tableBody = document.getElementById("caretakerTableBody");
  
  try {
    const snapshot = await get(ref(db, "Caretakers"));

    if (snapshot.exists()) {
      const caretakers = snapshot.val();
      allCaretakers = [];
      tableBody.innerHTML = "";

      // Process Data
      Object.keys(caretakers).forEach(uid => {
        const data = caretakers[uid];

        allCaretakers.push({
          name: data.CaretakerName || "-",
          email: data.CaretakerEmail || "-",
          phone: data.CaretakerPhoneNo || "-",
          profession: data.CaretakerProfession || "-",
          experience: data.CaretakerExperience || "-",
          age: data.CaretakerAge || "-",
          gender: data.CaretakerGender || "-",
          race: data.CaretakerRace || "-",
          image: data.CaretakerProfileUrl || "https://via.placeholder.com/40",
          language: formatLanguages(data.CaretakerLanguage)
        });
      });

      // Initial Render
      renderTable(allCaretakers);

    } else {
      tableBody.innerHTML = `<tr><td colspan="10" style="text-align:center; padding:20px;">No caretakers found</td></tr>`;
    }

  } catch (error) {
    console.error("Error:", error);
    tableBody.innerHTML = `<tr><td colspan="10" style="color:var(--danger); text-align:center;">Error: ${error.message}</td></tr>`;
  }
}

// --- Helper: Format Languages ---
function formatLanguages(langData) {
  if (!langData) return "-";
  if (typeof langData === "object") {
    return Object.values(langData).join(", ");
  }
  return langData;
}

// --- Render Table ---
function renderTable(list) {
  const tableBody = document.getElementById("caretakerTableBody");
  tableBody.innerHTML = "";

  if (list.length === 0) {
    tableBody.innerHTML = `<tr><td colspan="10" style="text-align:center; padding:20px;">No results found</td></tr>`;
    return;
  }

  list.forEach(c => {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td><img src="${c.image}" class="profile-thumb" alt="Profile"></td>
      <td><strong>${c.name}</strong></td>
      <td>${c.email}</td>
      <td>${c.phone}</td>
      <td>${c.profession}</td>
      <td>${c.experience}</td>
      <td>${c.age}</td>
      <td>${c.gender}</td>
      <td>${c.race}</td>
      <td style="max-width:200px; word-wrap:break-word;">${c.language}</td>
    `;
    tableBody.appendChild(row);
  });
}

// --- Search Functionality ---
// Attached to window so the HTML 'onkeyup' works, or attach via event listener
const searchInput = document.getElementById("searchInput");
if (searchInput) {
  searchInput.addEventListener('keyup', (e) => {
    const query = e.target.value.toLowerCase();
    
    const filtered = allCaretakers.filter(c => {
      return (
        c.name.toLowerCase().includes(query) ||
        c.email.toLowerCase().includes(query) ||
        c.phone.toLowerCase().includes(query) ||
        c.profession.toLowerCase().includes(query) ||
        c.experience.toLowerCase().includes(query) ||
        c.race.toLowerCase().includes(query) ||
        c.language.toLowerCase().includes(query)
      );
    });

    renderTable(filtered);
  });
}