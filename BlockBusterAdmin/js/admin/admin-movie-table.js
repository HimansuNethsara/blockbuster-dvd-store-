import { getAllMovies, deleteMovie, updateMovie, getAllStatus, getAllDVDs, deleteDVD } from '../firebaseUtils.js';
import { storage } from '../../firebaseConfig.js';
import { getDownloadURL, ref } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-storage.js";


let allMovies = [];
let statusMap = {}; 
let activeStatusId = null;
let pendingStatusId = null;

async function getMovieImageUrl(movieId) {
    try {
        const imageRef = ref(storage, `movies/${movieId}/poster.jpg`);
        const url = await getDownloadURL(imageRef);
        return url;
    } catch (error) {
        console.warn(`No poster for movie ${movieId}:`, error.message);
        return "../images/blank.html"; // fallback image
    }
}

async function loadMoviesFunc() {
    const popup = Notification();
    checkAdminSession();
    
    try {
        // Load statuses first
        await loadStatuses();

        const result = await getAllMovies();

        if (!result.success) {
            popup.error({ message: "Failed to load movies: " + result.message });
            return;
        }

        allMovies = result.data;
        const tbody = document.querySelector("table tbody");
        tbody.innerHTML = "";

        if (allMovies.length === 0) {
            tbody.innerHTML = `<tr><td colspan="10" class="text-center text-muted">No movies found</td></tr>`;
            return;
        }

        // Load all images in parallel
        await Promise.all(allMovies.map(async (movie) => {
            if (!movie.downloadUri) {
                const imageUrl = await getMovieImageUrl(movie.movieId);
                if (imageUrl) {
                    movie.downloadUri = imageUrl;
                }
            }
        }));

        allMovies.forEach(movie => {
            const row = document.createElement("tr");
            const posterUrl = movie.downloadUri || "../images/blank.html";

            // Checked = Active
            const isActive = movie.statusId === statusMap.active;

            row.innerHTML = `
                <th scope="row">${movie.id.substring(0, 8)}...</th>
                <td><img src="${posterUrl}" alt="Poster" style="height: 60px; object-fit: cover;" onerror="this.src='../images/blank.html'"></td>
                <td>${movie.title}</td>
                <td>${movie.imdbRating || "N/A"}</td>
                <td>${movie.rtRating || "N/A"}</td>
                <td>${movie.releaseYear || "N/A"}</td>
                <td>${movie.runtime || 0} min</td>
                <td>${movie.mpaaRatingId || "N/A"}</td>
                <td>
                    <label class="switch">
                    <input type="checkbox" onchange="toggleMovieStatus('${movie.id}', this)" ${isActive ? "checked" : ""}>
                    <span class="slider round"></span>
                    </label>
                </td>
                <td>
                    <button class="btn btn-sm btn-warning" onclick="editMovie('${movie.id}')">Edit</button>
                    <button class="btn btn-sm btn-danger" onclick="removeMovie('${movie.id}', '${movie.title}')">Delete</button>
                </td>
            `;
            tbody.appendChild(row);
        });

        popup.success({ message: "Movie data loaded successfully." });

    } catch (error) {
        popup.error({ message: "Error: " + error.message });
    }
}

let movieToDelete = null;

function removeMovieFunc(movieId, title) {
    movieToDelete = { id: movieId, title };

    document.getElementById("deleteMovieTitle").textContent = title;

    $('#deleteMovieModal').modal('show');
}

async function confirmDeleteMovie() {
    const popup = Notification();

    if (!movieToDelete) return;

    try {
        
        const dvdResult = await getAllDVDs();

        if (!dvdResult.success) {
            popup.error({ message: "Failed to fetch DVDs before deletion." });
            return;
        }

        const allDVDs = dvdResult.data;
        const relatedDVDs = allDVDs.filter(dvd => dvd.movieId === movieToDelete.id);
        for (const dvd of relatedDVDs) {
            await deleteDVD(dvd.dvdId);
        }
        const result = await deleteMovie(movieToDelete.id);

        if (result.success) {
            popup.success({ message: "Movie and related DVDs deleted successfully." });
            await loadMoviesFunc();
        } else {
            popup.error({ message: "Failed to delete movie: " + result.message });
        }

    } catch (error) {
        popup.error({ message: "Error: " + error.message });
    } finally {
        $('#deleteMovieModal').modal('hide');
        movieToDelete = null;
    }
}

async function loadStatuses() {
    try {
        const result = await getAllStatus();
        if (result.success && result.data) {
            result.data.forEach(status => {
                statusMap[status.value.toLowerCase()] = status.id;
            });
            console.log("Status map loaded:", statusMap); 

            activeStatusId = statusMap.active;
            pendingStatusId = statusMap.pending;
        }
    } catch (error) {
        console.error("Error loading statuses:", error);
    }
}

async function toggleMovieStatusFunc(movieId, checkbox) {
    const popup = Notification();
    try {
        if (!activeStatusId || !pendingStatusId) {
            popup.error({ message: "Statuses not loaded properly" });
            checkbox.checked = !checkbox.checked;
            return;
        }

        const newStatusId = checkbox.checked ? activeStatusId : pendingStatusId;
        const result = await updateMovie(movieId, { statusId: newStatusId });

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

function editMovieFunc(movieId) {
    // Find the movie object
    const movie = allMovies.find(m => m.id === movieId);
    if (!movie) return;

    document.getElementById("editMovieTitle").value = movie.title || "";
    document.getElementById("editImdbRating").value = movie.imdbRating || "";
    document.getElementById("editRtRating").value = movie.rtRating || "";
    document.getElementById("editReleaseYear").value = movie.releaseYear || "";
    document.getElementById("editRuntime").value = movie.runtime || "";
    document.getElementById("editMovieId").value = movie.id; // store id in hidden input

    $('#editMovieModal').modal('show');
}

async function saveMovieChanges() {
    const popup = Notification();

    const movieId = document.getElementById("editMovieId").value;
    const title = document.getElementById("editMovieTitle").value.trim();
    const imdbRating = parseFloat(document.getElementById("editImdbRating").value);
    const rtRating = parseFloat(document.getElementById("editRtRating").value);
    const releaseYear = parseInt(document.getElementById("editReleaseYear").value);
    const runtime = parseInt(document.getElementById("editRuntime").value);

    if (!title) {
        popup.error({ message: "Title cannot be empty" });
        return;
    }

    const movieData = {
        title,
        imdbRating: isNaN(imdbRating) ? null : imdbRating,
        rtRating: isNaN(rtRating) ? null : rtRating,
        releaseYear: isNaN(releaseYear) ? null : releaseYear,
        runtime: isNaN(runtime) ? null : runtime
    };

    await updateMovieData(movieId, movieData);

    $('#editMovieModal').modal('hide');
}

async function updateMovieData(movieId, movieData) {
    const popup = Notification();
    
    try {
        const result = await updateMovie(movieId, movieData);
        
        if (result.success) {
            popup.success({ message: result.message });
            await loadMoviesFunc();
        } else {
            popup.error({ message: "Failed to update movie: " + result.message });
        }
    } catch (error) {
        popup.error({ message: "Error: " + error.message });
    }
}

function printMovieList() {
    const title_top = "BlockBuster - Movie Management";
    const title = "Movie List";

    const table = document.querySelector("table").cloneNode(true);

    const baseUrl = window.location.origin + "/" + window.location.pathname.split("/")[1];
    table.querySelectorAll("img").forEach(img => {
        const src = img.getAttribute("src");

        // Only fix LOCAL images
        if (src && !src.startsWith("http")) {
            img.src = baseUrl + "/" + src;
        }
    });

    table.querySelectorAll("tbody tr").forEach(row => {
        const statusCell = row.children[8]; 
        if (statusCell) {

            const checkbox = statusCell.querySelector("input[type=checkbox]");
            if (checkbox) {
                const statusText = checkbox.checked ? "Active" : "Pending";

                statusCell.textContent = statusText;
            }
        }
    });

    const tableHTML = table.outerHTML;

    const printWindow = window.open('', '', 'width=900,height=650');
    printWindow.document.write(`
        <html>
        <head>
            <title>${title_top}</title>
            <link rel="stylesheet" href="${baseUrl}/../css/vendor/bootstrap.min.css">
            <link rel="stylesheet" href="${baseUrl}/../fonts/font-awesome/css/font-awesome.min.css">
            <link rel="stylesheet" href="${baseUrl}/../css/admin/font.css">
            <link rel="stylesheet" href="https://fonts.googleapis.com/css?family=Muli:300,400,700">
            <link rel="stylesheet" href="${baseUrl}/../css/admin/style.default.css" id="theme-stylesheet">
            <style>
                body {
                    padding: 20px;
                }
                h2 {
                    text-align: center;
                    margin-bottom: 10px;
                }
                img {
                    height: 80px;
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
    printWindow.onload = function () {
        printWindow.focus();
        printWindow.print();
        printWindow.close();
    };
}

// Export to global scope
window.loadMovies = loadMoviesFunc;
window.removeMovie = removeMovieFunc;
window.editMovie = editMovieFunc;
window.toggleMovieStatus = toggleMovieStatusFunc;
window.printMovieList = printMovieList;
window.saveMovieChanges = saveMovieChanges;
window.removeMovie = removeMovieFunc;
window.confirmDeleteMovie = confirmDeleteMovie;