import { initLayout } from './layout.js';
import { db, auth } from "./API/firebase.js";
import { ref, get } from "https://www.gstatic.com/firebasejs/9.6.11/firebase-database.js";
import { onAuthStateChanged } from "https://www.gstatic.com/firebasejs/9.6.11/firebase-auth.js";

// Global State
let allFamilies = [];

// 1. Initialize Layout & Listeners
document.addEventListener("DOMContentLoaded", () => {
  // Inject Sidebar/Header
  initLayout('FamilyElderlyList.html');
  
  // Setup Filter Listeners
  const searchInput = document.getElementById("searchInput");
  const typeFilter = document.getElementById("typeFilter");
  const refreshBtn = document.getElementById("refreshBtn");

  if(searchInput) searchInput.addEventListener("keyup", applyFilters);
  if(typeFilter) typeFilter.addEventListener("change", applyFilters);
  if(refreshBtn) refreshBtn.addEventListener("click", () => window.location.reload());

  // Auth Guard
  onAuthStateChanged(auth, (user) => {
    if (!user) {
      window.location.href = "login.html";
      return;
    }
    loadFamilies();
  });
});

// --- Main Data Loader ---
async function loadFamilies() {
  const tableBody = document.getElementById("familyTableBody");
  
  try {
    const snapshot = await get(ref(db, "FamilyElderly"));

    if (snapshot.exists()) {
      const families = snapshot.val();
      allFamilies = [];
      tableBody.innerHTML = "";

      // Process Data
      Object.keys(families).forEach(uid => {
        const data = families[uid];

        allFamilies.push({
          name: data.FamilyElderlyName || "-",
          email: data.FamilyElderlyEmail || "-",
          phone: data.FamilyElderlyPhone || "-",
          age: data.FamilyElderlyAge || "-",
          gender: data.FamilyElderlyGender || "-",
          type: data.FamilyElderlyType || "Family", // Default to Family if missing
          image: data.FamilyElderlyURL || "https://via.placeholder.com/40"
        });
      });

      // Initial Render
      applyFilters();

    } else {
      tableBody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:20px;">No records found</td></tr>`;
    }

  } catch (error) {
    console.error("Error:", error);
    tableBody.innerHTML = `<tr><td colspan="7" style="color:var(--danger); text-align:center;">Error: ${error.message}</td></tr>`;
  }
}

// --- Filter Logic ---
function applyFilters() {
  const query = document.getElementById("searchInput").value.toLowerCase();
  const typeFilter = document.getElementById("typeFilter").value;

  const filtered = allFamilies.filter(f => {
    // 1. Search Text Match
    const matchesSearch =
      f.name.toLowerCase().includes(query) ||
      f.email.toLowerCase().includes(query) ||
      f.phone.toLowerCase().includes(query) ||
      String(f.age).toLowerCase().includes(query) ||
      f.gender.toLowerCase().includes(query) ||
      f.type.toLowerCase().includes(query);

    // 2. Dropdown Match
    const matchesType = typeFilter === "All" || f.type === typeFilter;

    return matchesSearch && matchesType;
  });

  renderTable(filtered);
}

// --- Render Logic ---
function renderTable(list) {
  const tableBody = document.getElementById("familyTableBody");
  tableBody.innerHTML = "";

  if (list.length === 0) {
    tableBody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:20px;">No results found</td></tr>`;
    return;
  }

  list.forEach(f => {
    const row = document.createElement("tr");
    
    // Determine badge class based on type
    // Note: This relies on the classes defined in familylist.css
    let typeClass = f.type === "Family" ? "type-family" : "type-elderly";

    row.innerHTML = `
      <td><img src="${f.image}" class="profile-thumb" alt="Profile"></td>
      <td><strong>${f.name}</strong></td>
      <td>${f.email}</td>
      <td>${f.phone}</td>
      <td>${f.age}</td>
      <td>${f.gender}</td>
      <td><span class="type-badge ${typeClass}">${f.type}</span></td>
    `;
    tableBody.appendChild(row);
  });
}