import { getAllDVDs, getAllMovies, getAllStatus, deleteDVD, updateDVD } from '../firebaseUtils.js';

let allDVDs = [];
let movieMap = {}; // Map to store movieId -> movieTitle
let statusMap = {}; // Map to store status IDs
let activeStatusId = null;
let pendingStatusId = null;

async function loadMovieMap() {
    try {
        const result = await getAllMovies();
        if (result.success && result.data) {
            result.data.forEach(movie => {
                movieMap[movie.id] = movie.title;
            });
            console.log("Movie map loaded:", movieMap);
        }
    } catch (error) {
        console.error("Error loading movies:", error);
    }
}

async function loadStatuses() {
    try {
        const result = await getAllStatus();
        if (result.success && result.data) {

            result.data.forEach(status => {
                if (!status.value) return;

                // SAME as movie version
                statusMap[status.value.toLowerCase()] = status.id;
            });

            activeStatusId = statusMap.active;
            pendingStatusId = statusMap.pending;

            console.log("Status map loaded:", statusMap);
            console.log("Active:", activeStatusId, "Pending:", pendingStatusId);
        }
    } catch (error) {
        console.error("Error loading statuses:", error);
    }
}

async function loadDVDsFunc() {
    const popup = Notification();
    checkAdminSession();
    
    try {
        // Load dependency maps first
        await loadMovieMap();
        await loadStatuses();

        const result = await getAllDVDs();

        if (!result.success) {
            popup.error({ message: "Failed to load DVDs: " + result.message });
            return;
        }

        allDVDs = result.data;
        const tbody = document.getElementById("dvd-table-body") || document.querySelector("table tbody");
        tbody.innerHTML = "";

        if (allDVDs.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center text-muted">No DVDs found</td></tr>`;
            return;
        }

        allDVDs.forEach(dvd => {
            const row = document.createElement("tr");
            const movieTitle = movieMap[dvd.movieId] || "Unknown Movie";
            const isActive = dvd.statusId === statusMap.active;
            
            row.innerHTML = `
                <th scope="row">${dvd.id.substring(0, 8)}...</th>
                <td>${movieTitle}</td>
                <td>Rs. ${dvd.price || 0}</td>
                <td>${dvd.stockCount || 0}</td>
                <td>
                    <label class="switch">
                      <input type="checkbox" onchange="toggleDVDStatus('${dvd.id}', this)" ${isActive ? "checked" : ""}>
                      <span class="slider round"></span>
                    </label>
                </td>
                <td>
                    <button class="btn btn-sm btn-warning" onclick="editDVD('${dvd.id}')">Edit</button>
                    <button class="btn btn-sm btn-danger" onclick="removeDVD('${dvd.id}', '${movieTitle}')">Delete</button>
                </td>
            `;
            tbody.appendChild(row);
        });

        popup.success({ message: "DVD data loaded successfully." });

    } catch (error) {
        popup.error({ message: "Error: " + error.message });
    }
}

let dvdToDelete = null;

function removeDVDFunc(dvdId, movieTitle) {
    dvdToDelete = { id: dvdId, title: movieTitle };
    
    const deleteModal = document.getElementById("deleteDVDModal");
    if (deleteModal) {
        document.getElementById("deleteDVDTitle").textContent = movieTitle;
        $('#deleteDVDModal').modal('show');
    } else {
        // Fallback to simple confirm
        if (confirm(`Are you sure you want to delete DVD for "${movieTitle}"?`)) {
            confirmDeleteDVD();
        }
    }
}

async function confirmDeleteDVD() {
    const popup = Notification();

    if (!dvdToDelete) return;

    try {
        const result = await deleteDVD(dvdToDelete.id);

        if (result.success) {
            popup.success({ message: result.message });
            await loadDVDsFunc();
        } else {
            popup.error({ message: "Failed to delete DVD: " + result.message });
        }
    } catch (error) {
        popup.error({ message: "Error: " + error.message });
    } finally {
        const deleteModal = document.getElementById("deleteDVDModal");
        if (deleteModal) {
            $('#deleteDVDModal').modal('hide');
        }
        dvdToDelete = null;
    }
}

async function toggleDVDStatusFunc(dvdId, checkbox) {
    const popup = Notification();
    
    try {
        if (!activeStatusId || !pendingStatusId) {
            popup.error({ message: "Statuses not loaded properly" });
            checkbox.checked = !checkbox.checked;
            return;
        }

        const newStatusId = checkbox.checked ? activeStatusId : pendingStatusId;
        const result = await updateDVD(dvdId, { statusId: newStatusId });
        
        if (!result.success) {
            checkbox.checked = !checkbox.checked;
            popup.error({ message: "Failed to update status: " + result.message });
        } else {
            popup.success({ message: "Status updated successfully" });
        }
    } catch (error) {
        checkbox.checked = !checkbox.checked;
        popup.error({ message: "Error: " + error.message });
    }
}

function editDVDFunc(dvdId) {
    const dvd = allDVDs.find(d => d.id === dvdId);
    if (!dvd) return;

    const movieTitle = movieMap[dvd.movieId] || "Unknown";

    // Set modal values
    document.getElementById("dvdMovieTitle").textContent = movieTitle;
    document.getElementById("dvdIdDisplay").textContent = "ID: " + dvd.id;

    document.getElementById("editDVDPrice").value = dvd.price || 0;
    document.getElementById("editDVDStock").value = dvd.stockCount || 0;

    // ✅ Set status dropdown (convert statusId → name)
    let currentStatus = Object.keys(statusMap).find(
        key => statusMap[key] === dvd.statusId
    );

    document.getElementById("editDVDStatus").value = currentStatus || "active";

    document.getElementById("editDVDId").value = dvd.id;

    // Show modal
    $('#editDVDModal').modal('show');
}

async function saveDVDChanges() {
    const popup = Notification();

    const dvdId = document.getElementById("editDVDId").value;
    const price = parseFloat(document.getElementById("editDVDPrice").value);
    const stockCount = parseInt(document.getElementById("editDVDStock").value);
    const statusKey = document.getElementById("editDVDStatus").value;

    // Convert status name → ID
    const statusId = statusMap[statusKey];

    // Validation
    if (isNaN(price) || price < 0) {
        popup.error({ message: "Invalid price" });
        return;
    }

    if (isNaN(stockCount) || stockCount < 0) {
        popup.error({ message: "Invalid stock count" });
        return;
    }

    if (!statusId) {
        popup.error({ message: "Invalid status" });
        return;
    }

    try {
        const result = await updateDVD(dvdId, {
            price,
            stockCount,
            statusId
        });

        if (result.success) {
            popup.success({ message: result.message });
            $('#editDVDModal').modal('hide');
            await loadDVDsFunc();
        } else {
            popup.error({ message: "Failed to update DVD: " + result.message });
        }

    } catch (error) {
        popup.error({ message: "Error: " + error.message });
    }
}

function printDVDListFunc() {
    const title_top = "BlockBuster - DVD Inventory Management";
    const title = "DVD Inventory List";

    const table = document.querySelector("table").cloneNode(true);
    const tableHTML = table.outerHTML;

    const printWindow = window.open('', '', 'width=900,height=650');
    printWindow.document.write(`
        <html>
        <head>
            <title>${title_top}</title>
            <link rel="stylesheet" href="../css/vendor/bootstrap.min.css">
            <link rel="stylesheet" href="../fonts/font-awesome/css/font-awesome.min.css">
            <link rel="stylesheet" href="../css/admin/font.css">
            <link rel="stylesheet" href="https://fonts.googleapis.com/css?family=Muli:300,400,700">
            <link rel="stylesheet" href="../css/admin/style.default.css" id="theme-stylesheet">
            <style>
                body {
                    padding: 20px;
                }
                h2 {
                    text-align: center;
                    margin-bottom: 10px;
                }
                .switch, .btn {
                    display: none;
                }
            </style>
        </head>
        <body>
            <h2>${title}</h2>
            <div class="table-responsive">
                ${tableHTML}
            </div>
        </body>
        </html>
    `);

    printWindow.document.close();
    printWindow.focus();
    printWindow.print();
    printWindow.close();
}

// Export to global scope
window.loadDVDs = loadDVDsFunc;
window.removeDVD = removeDVDFunc;
window.toggleDVDStatus = toggleDVDStatusFunc;
window.editDVD = editDVDFunc;
window.printDVDList = printDVDListFunc;
window.confirmDeleteDVD = confirmDeleteDVD;
window.saveDVDChanges = saveDVDChanges;



