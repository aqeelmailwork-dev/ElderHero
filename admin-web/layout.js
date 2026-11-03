import { auth, db } from "./API/firebase.js";
import { signOut } from "https://www.gstatic.com/firebasejs/9.6.11/firebase-auth.js";
import { ref, get } from "https://www.gstatic.com/firebasejs/9.6.11/firebase-database.js";

export function initLayout(activeLinkHref) {
  // 1. Auth Check (Protect all pages using this layout)
  const uid = localStorage.getItem("adminUid");
  if (!uid) {
    window.location.href = "login.html";
    return;
  }

  // 2. Inject Sidebar & Header HTML
  // Note: We add 'style="display: none"' to Super Admin items by default.
  // They are revealed in loadAdminHeaderData() if the user is authorized.
  const layoutHTML = `
    <div class="overlay" id="overlay"></div>

    <aside class="sidebar" id="sidebar">
      <div class="sidebar-header">
        <div class="logo-container">
          <img src="images/elderhero.png" alt="ElderHero Logo" class="sidebar-logo">
        </div>
        <h2 class="brand-name">ElderHero</h2>
      </div>

      <nav class="sidebar-nav">
        <ul>
          <li class="nav-label">Main</li>
          <li><a href="dashboard.html" class="nav-link">Dashboard</a></li>

          <li class="nav-label">Lists & Data</li>
          <li><a href="carelist.html" class="nav-link">Caretakers List</a></li>
          <li><a href="FamilyElderlyList.html" class="nav-link">Family/Elderly List</a></li>
          <li><a href="bookings.html" class="nav-link">Booking Management</a></li>

          <li class="nav-label">Management</li>
          <li class="superadmin-only" style="display: none;">
            <a href="categories.html" class="nav-link">Categories</a>
          </li>
          <li><a href="CaretakerManagement.html" class="nav-link">Caretaker Management</a></li>
          <li><a href="FamilyElderlyManagment.html" class="nav-link">Family/Elderly Management</a></li>

          <li class="nav-label">Requests</li>
          <li><a href="submissions.html" class="nav-link">PDF Submissions</a></li>
          <li><a href="view-reports.html" class="nav-link">View Reports</a></li>

          <li class="nav-label superadmin-only" style="display: none;">System</li>
          <li class="superadmin-only" style="display: none;">
            <a href="admin_signup.html" class="nav-link">Register New Admin</a>
          </li>
        </ul>
      </nav>

      <div class="sidebar-footer">
        <button id="globalLogoutBtn" class="logout-btn">Logout</button>
      </div>
    </aside>

    <header class="top-header" id="topHeader">
      <button class="menu-toggle" id="menuToggle">☰</button>
      <div class="header-right">
        <div class="admin-profile">
          <div class="admin-text">
            <span id="layoutAdminName">Admin</span>
            <small id="layoutAdminType">Loading...</small>
          </div>
        </div>
      </div>
    </header>
  `;

  // Inject at start of body
  document.body.insertAdjacentHTML('afterbegin', layoutHTML);

  // Move header inside main-content if it exists
  const mainContent = document.querySelector('.main-content');
  const header = document.getElementById('topHeader');
  if (mainContent && header) {
    mainContent.insertBefore(header, mainContent.firstChild);
  }

  // 3. Highlight Active Link
  if (activeLinkHref) {
    const links = document.querySelectorAll('.nav-link');
    links.forEach(link => {
      // Simple match or partial match if needed
      if (link.getAttribute('href') === activeLinkHref) {
        link.classList.add('active');
      }
    });
  }

  // 4. Setup Events and Load Header Data
  setupInteractions();
  // We pass the current page to the loader to handle redirects if unauthorized
  loadAdminHeaderData(uid, activeLinkHref);
}

function setupInteractions() {
  const menuToggle = document.getElementById('menuToggle');
  const sidebar = document.getElementById('sidebar');
  const overlay = document.getElementById('overlay');
  const logoutBtn = document.getElementById('globalLogoutBtn');

  // Mobile Menu Toggle
  if (menuToggle) {
    menuToggle.addEventListener('click', () => {
      sidebar.classList.toggle('open');
      overlay.classList.toggle('show');
    });
  }

  if (overlay) {
    overlay.addEventListener('click', () => {
      sidebar.classList.remove('open');
      overlay.classList.remove('show');
    });
  }

  // Global Logout
  if (logoutBtn) {
    logoutBtn.addEventListener('click', async () => {
      try {
        await signOut(auth);
        localStorage.removeItem("adminUid");
        window.location.href = "login.html";
      } catch (error) {
        alert("Logout Error: " + error.message);
      }
    });
  }
}

async function loadAdminHeaderData(uid, currentPage) {
  try {
    const snapshot = await get(ref(db, "Admins/" + uid));
    if (snapshot.exists()) {
      const data = snapshot.val();
      const nameEl = document.getElementById("layoutAdminName");
      const typeEl = document.getElementById("layoutAdminType");
      
      const adminName = data.AdminName || "Admin";
      const adminType = data.AdminType || "staff"; // default to staff if missing

      // Update UI Text
      if(nameEl) nameEl.innerText = adminName;
      if(typeEl) typeEl.innerText = adminType.toUpperCase();

      // --- SUPER ADMIN CHECK ---
      if (adminType === "superadmin") {
        // Show restricted items
        const restrictedItems = document.querySelectorAll('.superadmin-only');
        restrictedItems.forEach(item => {
          item.style.display = 'block'; // or 'list-item'
        });
      } else {
        // If NOT superadmin, ensure they are not on a restricted page
        const restrictedPages = ["categories.html", "register-admin.html"];
        
        if (restrictedPages.includes(currentPage)) {
          alert("Access Denied: Super Admin privileges required.");
          window.location.href = "dashboard.html";
        }
      }

    } else {
      // Admin ID in localStorage but not in Database? Force logout.
      console.error("Admin record not found.");
      localStorage.removeItem("adminUid");
      window.location.href = "login.html";
    }
  } catch (e) {
    console.error("Header Load Error:", e);
  }
}