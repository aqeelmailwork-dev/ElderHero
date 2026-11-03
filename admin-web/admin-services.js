// admin-services.js
import { db } from "./API/firebase.js"; 
import { ref, get, push, set, remove, update, onValue } 
  from "https://www.gstatic.com/firebasejs/9.6.11/firebase-database.js";

const categoryList = document.getElementById("categoryList");
const categoryNameInput = document.getElementById("categoryName");
const subserviceNameInput = document.getElementById("subserviceName");
const subserviceList = document.getElementById("subserviceList");
const addCategoryBtn = document.getElementById("addCategoryBtn");
const addSubBtn = document.getElementById("addSubBtn");
const categorySelect = document.getElementById("categorySelect");

// Helpers
function currentDate() { return new Date().toISOString().split("T")[0]; }
function currentTime() { return new Date().toTimeString().split(" ")[0]; }

// Get Current Admin ID
const adminUid = localStorage.getItem("adminUid");
if (!adminUid) console.error("No Admin Logged In");

let adminMap = {};

//  Load Admins to map IDs to Names
async function loadAdmins() {
    const snap = await get(ref(db, "Admins"));
    if (snap.exists()) adminMap = snap.val();
}
loadAdmins();

function getAdminName(uid) {
    return adminMap[uid]?.AdminName || "Unknown Admin";
}

const categoriesRef = ref(db, "ServiceCategories");

//  Load all categories + live updates
onValue(categoriesRef, (snapshot) => {
  if(categoryList) categoryList.innerHTML = "";
  if(categorySelect) categorySelect.innerHTML = "<option value=''>Select Category</option>";

  snapshot.forEach(catSnap => {
    const catName = catSnap.key;
    
    // Populate List
    if(categoryList) {
        const li = document.createElement("li");
        li.innerHTML = `
        <strong>${catName}</strong>
        <button onclick="deleteCategory('${catName}')">❌ Delete</button>
        <button onclick="loadSubServices('${catName}')">📋 View Subservices</button>
        `;
        categoryList.appendChild(li);
    }

    // Populate Dropdown
    if(categorySelect) {
        const option = document.createElement("option");
        option.value = catName;
        option.textContent = catName;
        categorySelect.appendChild(option);
    }
  });
});

// Add new category
if(addCategoryBtn) {
    addCategoryBtn.addEventListener("click", async () => {
    const name = categoryNameInput.value.trim();
    if (!name) return alert("Enter category name");

    const newCatRef = ref(db, `ServiceCategories/${name}`);
    await set(newCatRef, { 
        subServices: {},
        createdBy: adminUid,
        // Save Dates for compatibility with table view
        CategoryDateAdd: currentDate(),
        CategoryTimeAdd: currentTime(),
        CategoryDateUpdate: currentDate(),
        CategoryTimeUpdate: currentTime()
    });
    
    categoryNameInput.value = "";
    alert("Category added!");
    });
}

// Delete category
window.deleteCategory = async (catName) => {
  if (!confirm(`Delete category "${catName}"?`)) return;
  await remove(ref(db, `ServiceCategories/${catName}`));
  alert("Category deleted!");
};

// Load subservices
window.loadSubServices = async (catName) => {
  await loadAdmins();
  const subsRef = ref(db, `ServiceCategories/${catName}/subServices`);
  const snapshot = await get(subsRef);
  
  if(!subserviceList) return;

  subserviceList.innerHTML = `<h3>Subservices under ${catName}</h3>`;

  if(snapshot.exists()){
      snapshot.forEach(subSnap => {
        const id = subSnap.key;
        const val = subSnap.val();
        
        const subName = (typeof val === 'object') ? val.name : val;
        const subAdminId = (typeof val === 'object') ? val.adminId : null;
        
        const adminNameDisplay = subAdminId ? `<small style='color:gray; display:block; font-size:12px;'>Added by: ${getAdminName(subAdminId)}</small>` : "";

        const div = document.createElement("div");
        div.style.borderBottom = "1px solid #eee";
        div.style.padding = "8px 5px";
        div.innerHTML = `
          <div>
            <strong>${subName}</strong>
            ${adminNameDisplay}
          </div>
          <button style="margin-left:auto; color:red;" onclick="deleteSubService('${catName}', '${id}')">❌</button>
        `;
        div.style.display = "flex";
        div.style.alignItems = "center";
        
        subserviceList.appendChild(div);
      });
  } else {
      subserviceList.innerHTML += "<p>No subservices found.</p>";
  }
  subserviceList.dataset.activeCategory = catName;
};

// Add subservice
if(addSubBtn) {
    addSubBtn.addEventListener("click", async () => {
    const subName = subserviceNameInput.value.trim();
    const catName = categorySelect.value;
    if (!subName || !catName) return alert("Select category and enter subservice name");

    const subRef = push(ref(db, `ServiceCategories/${catName}/subServices`));
    
    // Store as Object
    await set(subRef, {
        name: subName,
        adminId: adminUid
    });

    // Update parent category Update Time
    await update(ref(db, `ServiceCategories/${catName}`), {
        CategoryDateUpdate: currentDate(),
        CategoryTimeUpdate: currentTime()
    });

    subserviceNameInput.value = "";
    alert("Subservice added!");
    
    if (subserviceList && subserviceList.dataset.activeCategory === catName) {
        loadSubServices(catName);
    }
    });
}

// ✅ Delete subservice
window.deleteSubService = async (catName, id) => {
  if(!confirm("Delete this subservice?")) return;
  await remove(ref(db, `ServiceCategories/${catName}/subServices/${id}`));
  
  // Update parent category Update Time
  await update(ref(db, `ServiceCategories/${catName}`), {
      CategoryDateUpdate: currentDate(),
      CategoryTimeUpdate: currentTime()
  });

  alert("Subservice deleted!");
  loadSubServices(catName);
};