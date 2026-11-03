import { db } from "./API/firebase.js";
import { ref, get } from "https://www.gstatic.com/firebasejs/9.6.11/firebase-database.js";
import { initLayout } from "./layout.js";

// 1. Initialize Layout
document.addEventListener('DOMContentLoaded', () => {
    initLayout('dashboard.html');
    loadDashboardData();
});

// 2. Main Data Loader
async function loadDashboardData() {
    const els = {
        submission: document.getElementById("submissionCount"),
        report: document.getElementById("reportCount"),
        caretaker: document.getElementById("caretakerCount"),
        family: document.getElementById("familyCount")
    };

    try {
        // Fetch all required database nodes
        const [
            subSnap, 
            repSnap, 
            careSnap, 
            famSnap, 
            bookSnap, 
            adSnap, 
            catSnap, 
            revSnap
        ] = await Promise.all([
            get(ref(db, "Submissions")),
            get(ref(db, "Reports")),
            get(ref(db, "Caretakers")),
            get(ref(db, "FamilyElderly")),
            get(ref(db, "Bookings")),
            get(ref(db, "Advertisements")), 
            get(ref(db, "Categories")),
            get(ref(db, "Reviews"))
        ]);

        const data = {
            submissions: subSnap.exists() ? subSnap.val() : {},
            reports: repSnap.exists() ? repSnap.val() : {},
            caretakers: careSnap.exists() ? careSnap.val() : {},
            family: famSnap.exists() ? famSnap.val() : {},
            bookings: bookSnap.exists() ? bookSnap.val() : {},
            ads: adSnap.exists() ? adSnap.val() : {},
            cats: catSnap.exists() ? catSnap.val() : {},
            reviews: revSnap.exists() ? revSnap.val() : {}
        };

        updateStats(data, els);
        renderCharts(data);

    } catch (error) {
        console.error("Error loading dashboard data:", error);
    }
}

// 3. Logic for Stats Cards
function updateStats(data, els) {
    // A. Pending Submissions
    const pendingSubs = Object.values(data.submissions)
        .filter(s => (s.SubmissionStatus || "Pending") === "Pending").length;
    if(els.submission) els.submission.innerText = pendingSubs;

    // B. Pending Reports
    let pendingReportsCount = 0;
    Object.values(data.reports).forEach(bookingGroup => {
        Object.values(bookingGroup).forEach(report => {
            if ((report.ReportStatus || "Pending") === "Pending") pendingReportsCount++;
        });
    });
    if(els.report) els.report.innerText = pendingReportsCount;

    // C. Totals
    if(els.caretaker) els.caretaker.innerText = Object.keys(data.caretakers).length;
    if(els.family) els.family.innerText = Object.keys(data.family).length;
}

// 4. Logic for Charts
function renderCharts(data) {
    const colorPrimary = '#c0a062';
    const colorDark = '#3e342d';
    const colorSuccess = '#28a745';
    const colorDanger = '#d93025';
    const colorWarning = '#f4b400';

    // --- Flatten Advertisements ---
    const flatAds = {};
    if (data.ads) {
        Object.values(data.ads).forEach(caretakerGroup => {
            if (typeof caretakerGroup === 'object') {
                Object.keys(caretakerGroup).forEach(adKey => {
                    flatAds[adKey] = caretakerGroup[adKey];
                });
            }
        });
    }

    // --- Chart 1: All Service Categories ---
    const categoryCounts = {};

    // Step A: Initialize ALL categories with 0
    // This ensures even empty categories show up on the chart
    if (data.cats) {
        Object.values(data.cats).forEach(cat => {
            const name = cat.CategoryName || "Unnamed";
            categoryCounts[name] = 0;
        });
    }
    
    // Step B: Count actual bookings
    Object.values(data.bookings).forEach(booking => {
        const adId = booking.AdvertismentID; 
        const ad = flatAds[adId];
        
        if (ad && ad.CategoriesID) {
            const catId = ad.CategoriesID;
            const catName = data.cats[catId]?.CategoryName;
            
            // Only increment if the category actually exists in our list
            if (catName && categoryCounts.hasOwnProperty(catName)) {
                categoryCounts[catName]++;
            } else if (catName) {
                // Fallback if category exists in Ad but wasn't in main list
                categoryCounts[catName] = 1;
            }
        }
    });

    createChart('categoryChart', 'bar', {
        labels: Object.keys(categoryCounts),
        datasets: [{
            label: 'Total Bookings',
            data: Object.values(categoryCounts),
            backgroundColor: colorPrimary,
            borderRadius: 5
        }]
    });

    // --- Chart 2: Booking Status Overview ---
    const statusCounts = { 'Completed': 0, 'Pending': 0, 'Cancelled': 0, 'Rejected': 0, 'Accepted': 0 };
    Object.values(data.bookings).forEach(b => {
        const status = b.BookingStatus || "Pending";
        if (statusCounts.hasOwnProperty(status)) {
            statusCounts[status]++;
        } else {
            statusCounts[status] = (statusCounts[status] || 0) + 1;
        }
    });

    createChart('statusChart', 'doughnut', {
        labels: Object.keys(statusCounts),
        datasets: [{
            data: Object.values(statusCounts),
            backgroundColor: [colorSuccess, colorWarning, colorDanger, '#7c7168', '#17a2b8'],
            borderWidth: 0
        }]
    });

    // --- Chart 3: Top Rated Caretakers ---
    const ratingsMap = {}; 
    Object.values(data.reviews).forEach(r => {
        const cID = r.CaretakerID;
        const rating = parseInt(r.ReviewRating) || 0;
        
        if (!ratingsMap[cID]) ratingsMap[cID] = { total: 0, count: 0 };
        ratingsMap[cID].total += rating;
        ratingsMap[cID].count++;
    });

    const rankedList = Object.keys(ratingsMap).map(id => {
        const avg = ratingsMap[id].total / ratingsMap[id].count;
        const name = data.caretakers[id]?.CaretakerName || "Unknown"; 
        return { name, avg };
    });

    rankedList.sort((a, b) => b.avg - a.avg);
    const top5 = rankedList.slice(0, 5);

    createChart('caretakerRankChart', 'bar', {
        labels: top5.map(i => i.name),
        datasets: [{
            label: 'Average Rating (Stars)',
            data: top5.map(i => i.avg),
            backgroundColor: colorDark,
            borderRadius: 4,
            barThickness: 20
        }]
    }, { indexAxis: 'y' });

    // --- Chart 4: Booking Trend ---
    const dateCounts = {};
    Object.values(data.bookings).forEach(b => {
        if (b.BookingDate) {
            dateCounts[b.BookingDate] = (dateCounts[b.BookingDate] || 0) + 1;
        }
    });

    const sortedDates = Object.keys(dateCounts).sort();

    createChart('trendChart', 'line', {
        labels: sortedDates,
        datasets: [{
            label: 'Daily Bookings',
            data: sortedDates.map(d => dateCounts[d]),
            borderColor: colorPrimary,
            backgroundColor: 'rgba(192, 160, 98, 0.1)',
            fill: true,
            tension: 0.3
        }]
    });
}

// 5. Helper Function
function createChart(canvasId, type, data, extraOptions = {}) {
    const ctx = document.getElementById(canvasId);
    if (!ctx) return; 

    new Chart(ctx, {
        type: type,
        data: data,
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: { font: { family: "'Segoe UI', sans-serif", size: 11 } }
                }
            },
            ...extraOptions
        }
    });
}