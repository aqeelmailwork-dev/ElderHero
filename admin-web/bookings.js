import { initLayout } from "./layout.js";
import { db } from "./API/firebase.js";
import { ref, get } from "https://www.gstatic.com/firebasejs/9.6.11/firebase-database.js";

// Initialize Layout
initLayout("bookings.html");

// DOM Elements
const tableBody = document.getElementById("tableBody");
const searchInput = document.getElementById("searchInput");
const statusFilter = document.getElementById("statusFilter");
const refreshBtn = document.getElementById("refreshBtn");
const modal = document.getElementById("bookingModal");
const closeModal = document.getElementById("closeModal");
const modalContent = document.getElementById("modalContent");

let allBookings = []; 

// --- 1. Load Data ---
async function loadBookings() {
    tableBody.innerHTML = `<tr><td colspan="6" style="text-align:center;"><div class="loader" style="margin: 0 auto; border-color: #c0a062; border-top-color: transparent;"></div></td></tr>`;

    try {
        const bookingsRef = ref(db, "Bookings");
        const snapshot = await get(bookingsRef);

        if (snapshot.exists()) {
            const data = snapshot.val();
            allBookings = Object.entries(data).map(([key, value]) => ({
                ...value,
                id: key 
            }));

            // Sort by timestamp descending
            allBookings.sort((a, b) => (b.BookingTimeStamp || 0) - (a.BookingTimeStamp || 0));

            renderTable(allBookings);
        } else {
            tableBody.innerHTML = `<tr><td colspan="6" style="text-align:center;">No bookings found.</td></tr>`;
        }
    } catch (error) {
        console.error("Error loading bookings:", error);
        tableBody.innerHTML = `<tr><td colspan="6" style="text-align:center; color: var(--danger);">Error loading data. Check console.</td></tr>`;
    }
}

// --- 2. Render Table ---
async function renderTable(bookingsData) {
    tableBody.innerHTML = "";

    if (bookingsData.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="6" style="text-align:center;">No matching records found.</td></tr>`;
        return;
    }

    for (const booking of bookingsData) {
        
        // Resolve User Data (Name + Photo)
        const familyUser = await getUserDetails("FamilyElderly", booking.FamiliyElderlyID);
        const caretakerUser = await getUserDetails("Caretaker", booking.CaretakerID);

        // Safe strings for HTML attributes (escape quotes)
        const safeFamilyName = familyUser.name.replace(/'/g, "\\'");
        const safeCaretakerName = caretakerUser.name.replace(/'/g, "\\'");
        
        // Use a placeholder string if photo is missing so the function call doesn't break
        const familyPhoto = familyUser.photo || "";
        const caretakerPhoto = caretakerUser.photo || "";

        // Status Logic
        let badgeClass = "status-pending";
        const status = booking.BookingStatus || "Pending";
        if (status === "Completed") badgeClass = "status-approved";
        else if (status === "Accepted") badgeClass = "status-active"; 
        else if (status === "Cancelled" || status === "Rejected") badgeClass = "status-rejected";

        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>
                <div style="font-weight:700; color:var(--text-dark);">${booking.BookingDate}</div>
                <div style="font-size:12px; color:var(--text-grey);">${booking.BookingTime}</div>
                <div style="font-size:11px; color:var(--primary-color); margin-top:2px;">#${booking.BookingID.substring(0, 8)}...</div>
            </td>
            
            <td onclick="showUserInfo('${safeFamilyName}', '${booking.FamiliyElderlyID}', 'Family Booker', '${familyPhoto}')" 
                style="cursor:pointer; transition: background 0.2s;" 
                onmouseover="this.style.background='rgba(192, 160, 98, 0.1)'" 
                onmouseout="this.style.background='transparent'"
                title="Click to view profile">
                <div style="font-weight:600; color:var(--primary-color); text-decoration:underline; text-decoration-style:dotted;">
                    ${familyUser.name}
                </div>
                <div style="font-size:11px; color:var(--text-light);">ID: ...${booking.FamiliyElderlyID ? booking.FamiliyElderlyID.slice(-4) : 'N/A'}</div>
            </td>

            <td onclick="showUserInfo('${safeCaretakerName}', '${booking.CaretakerID}', 'Caretaker', '${caretakerPhoto}')" 
                style="cursor:pointer; transition: background 0.2s;" 
                onmouseover="this.style.background='rgba(192, 160, 98, 0.1)'" 
                onmouseout="this.style.background='transparent'"
                title="Click to view profile">
                <div style="font-weight:600; color:var(--primary-color); text-decoration:underline; text-decoration-style:dotted;">
                    ${caretakerUser.name}
                </div>
                <div style="font-size:11px; color:var(--text-light);">ID: ...${booking.CaretakerID ? booking.CaretakerID.slice(-4) : 'N/A'}</div>
            </td>

            <td>${booking.BookingDuration}</td>
            <td><span class="status-badge ${badgeClass}">${status}</span></td>
            <td>
                <button class="btn-action btn-view" onclick="openBookingModal('${booking.BookingID}')">
                    View Details
                </button>
            </td>
        `;
        tableBody.appendChild(tr);
    }
}

// --- 3. Helper: Get User Details (Name & Photo) ---
const userCache = {}; 

async function getUserDetails(baseCollection, uid) {
    if (!uid) return { name: "Unknown", photo: "" };
    if (userCache[uid]) return userCache[uid];

    const pathsToTry = [baseCollection, baseCollection + "s"]; 

    for (const path of pathsToTry) {
        try {
            const userRef = ref(db, `${path}/${uid}`);
            const snapshot = await get(userRef);
            
            if (snapshot.exists()) {
                const val = snapshot.val();
                let foundName = null;
                let foundPhoto = "";

                // --- 1. FIND NAME ---
                if (val.CaretakerName) foundName = val.CaretakerName;
                else if (val.FamilyElderlyName) foundName = val.FamilyElderlyName;
                else if (val.fullName) foundName = val.fullName;
                else if (val.name) foundName = val.name;
                else if (val.username) foundName = val.username;

                // --- 2. FIND PHOTO ---
                // Based on your screenshots: CaretakerProfileUrl / FamilyElderlyURL
                if (val.CaretakerProfileUrl) foundPhoto = val.CaretakerProfileUrl;
                else if (val.FamilyElderlyURL) foundPhoto = val.FamilyElderlyURL;
                // Fallbacks just in case
                else if (val.profileUrl) foundPhoto = val.profileUrl;
                else if (val.photoUrl) foundPhoto = val.photoUrl;

                if (foundName) {
                    const result = { name: foundName, photo: foundPhoto };
                    userCache[uid] = result;
                    return result;
                }
            }
        } catch (e) {}
    }
    return { name: `User ${uid.substring(0, 6)}`, photo: "" };
}

// --- 4. Filtering Logic ---
function filterBookings() {
    const term = searchInput.value.toLowerCase();
    const status = statusFilter.value;

    const filtered = allBookings.filter(b => {
        const matchesSearch = 
            (b.BookingID && b.BookingID.toLowerCase().includes(term)) ||
            (b.BookingDate && b.BookingDate.includes(term));
        const matchesStatus = status === "All" || b.BookingStatus === status;
        return matchesSearch && matchesStatus;
    });
    renderTable(filtered);
}

// --- 5. Booking Details Modal ---
window.openBookingModal = function(bookingId) {
    const booking = allBookings.find(b => b.BookingID === bookingId);
    if (!booking) return;

    modalContent.innerHTML = `
        <div class="card-header-flex">
            <h3>Booking Details</h3>
        </div>
        <div class="form-group">
            <label>Booking ID</label>
            <input type="text" value="${booking.BookingID}" readonly>
        </div>
        <div style="display:grid; grid-template-columns: 1fr 1fr; gap:10px;">
            <div class="form-group">
                <label>Date</label>
                <input type="text" value="${booking.BookingDate}" readonly>
            </div>
            <div class="form-group">
                <label>Time</label>
                <input type="text" value="${booking.BookingTime}" readonly>
            </div>
        </div>
        <div class="form-group">
            <label>Advertisement ID</label>
            <input type="text" value="${booking.AdvertismentID}" readonly>
        </div>
        <div class="form-group">
            <label>Note</label>
            <textarea readonly rows="3">${booking.BookingNote || "No notes provided."}</textarea>
        </div>
        <div class="form-group">
             <label>Current Status</label>
             <div style="font-weight:bold; color:var(--primary-color);">${booking.BookingStatus}</div>
        </div>
    `;
    modal.classList.add("show");
};

// --- 6. USER INFO MODAL (With Profile Image) ---
window.showUserInfo = function(name, id, role, photoUrl) {
    
    // 1. Prepare Image HTML (Profile Pic OR Fallback Circle)
    let imageDisplayHtml = '';
    
    if (photoUrl && photoUrl !== 'undefined' && photoUrl.trim() !== '') {
        // Show Profile Image
        imageDisplayHtml = `
            <img src="${photoUrl}" alt="${name}" 
                 style="width:90px; height:90px; margin:0 auto 15px; border-radius:50%; 
                        object-fit:cover; border:3px solid var(--primary-color); 
                        box-shadow: 0 4px 15px rgba(0,0,0,0.15); display:block;">
        `;
    } else {
        // Show Fallback Circle (Initials)
        const colors = ['#e0f2f1', '#fff3e0', '#f3e5f5', '#e3f2fd', '#fbe9e7'];
        const randomColor = colors[name.charCodeAt(0) % colors.length];
        imageDisplayHtml = `
            <div style="width:90px; height:90px; margin:0 auto 15px; background:${randomColor}; 
                        border-radius:50%; display:flex; align-items:center; justify-content:center; 
                        font-size:36px; font-weight:bold; color:var(--primary-color); 
                        border:3px solid var(--gold-border); box-shadow: 0 4px 15px rgba(0,0,0,0.15);">
                ${name.charAt(0).toUpperCase()}
            </div>
        `;
    }

    // 2. Render Modal Content
    modalContent.innerHTML = `
        <div style="text-align:center; padding: 20px 0; animation: fadeIn 0.3s ease;">
            
            ${imageDisplayHtml}

            <h2 style="margin-bottom:5px; color:var(--text-dark);">${name}</h2>
            <span class="status-badge status-active" style="background:#3e342d; color:#fff;">${role}</span>
            
            <div style="margin-top:30px; text-align:left;">
                <label style="font-size:12px; color:var(--text-grey); font-weight:600; text-transform:uppercase;">Full User ID</label>
                <div style="background:#f9fafb; border:1px solid #ddd; padding:12px; border-radius:8px; 
                            font-family:'Courier New', monospace; display:flex; justify-content:space-between; align-items:center;">
                    <span style="color:#333; font-weight:600; font-size:14px; word-break:break-all;">${id}</span>
                    <button onclick="copyToClipboard('${id}')" 
                            style="border:none; background:rgba(192, 160, 98, 0.1); color:var(--primary-color); 
                                   padding:5px 10px; border-radius:4px; cursor:pointer; font-weight:bold; font-size:12px; margin-left:10px;">
                        COPY
                    </button>
                </div>
            </div>
        </div>
    `;
    modal.classList.add("show");
}

window.copyToClipboard = function(text) {
    navigator.clipboard.writeText(text).then(() => {
        const btn = event.target;
        const originalText = btn.innerText;
        btn.innerText = "COPIED!";
        btn.style.backgroundColor = "#28a745";
        btn.style.color = "white";
        setTimeout(() => {
            btn.innerText = originalText;
            btn.style.backgroundColor = "rgba(192, 160, 98, 0.1)";
            btn.style.color = "var(--primary-color)";
        }, 1500);
    });
}

closeModal.addEventListener("click", () => {
    modal.classList.remove("show");
});

refreshBtn.addEventListener("click", loadBookings);
searchInput.addEventListener("input", filterBookings);
statusFilter.addEventListener("change", filterBookings);

window.addEventListener("DOMContentLoaded", loadBookings);