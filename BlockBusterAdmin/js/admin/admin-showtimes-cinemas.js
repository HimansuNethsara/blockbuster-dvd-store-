import { getAllMovies, getAllDVDs, addDVD, updateDVD, deleteDVD } from '../firebaseUtils.js';
import { storage } from '../../firebaseConfig.js';
import { ref, uploadBytes, getDownloadURL } from 'https://www.gstatic.com/firebasejs/10.12.2/firebase-storage.js';
import { collection, getDocs } from 'https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js';
import { db } from '../../firebaseConfig.js';

const notify = Notification();
let allMovies = [];
let allDVDs = [];
let allStatuses = [];

async function loadData() {
    console.log('loadData called');
    try {
        const [moviesResult, dvdsResult] = await Promise.all([
            getAllMovies(),
            getAllDVDs()
        ]);

        console.log('Movies result success:', moviesResult.success, 'data length:', moviesResult.data?.length);
        console.log('DVDs result success:', dvdsResult.success, 'data length:', dvdsResult.data?.length);

        if (!moviesResult.success || !dvdsResult.success) {
            console.error('Failed to load data');
            return;
        }

        allMovies = moviesResult.data;
        allDVDs = dvdsResult.data;

        console.log('Loaded', allMovies.length, 'movies and', allDVDs.length, 'DVDs');

        loadDVDSelects();
        setupMovieSearch();

    } catch (error) {
        console.error('loadData error', error);
    }
}

async function loadStatuses() {
    try {
        const statusCol = collection(db, 'status');
        const snapshot = await getDocs(statusCol);

        allStatuses = snapshot.docs.map(doc => ({
            id: doc.id,
            ...doc.data()  // expects {statusId, value} like {1, "Pending"}
        }));

        const addStatusSelect = document.getElementById('dvd-status');
        const updateStatusSelect = document.getElementById('update-status');

        if (addStatusSelect) addStatusSelect.innerHTML = '';
        if (updateStatusSelect) updateStatusSelect.innerHTML = '';

        allStatuses.forEach(status => {
            const optionAdd = document.createElement('option');
            optionAdd.value = status.value;  // e.g., "Pending" or "Active"
            optionAdd.textContent = status.value;
            addStatusSelect.appendChild(optionAdd);

            const optionUpdate = document.createElement('option');
            optionUpdate.value = status.value;
            optionUpdate.textContent = status.value;
            updateStatusSelect.appendChild(optionUpdate);
        });

        // Optional: set default to first status
        if (addStatusSelect && allStatuses.length > 0) addStatusSelect.value = allStatuses[0].value;
        if (updateStatusSelect && allStatuses.length > 0) updateStatusSelect.value = allStatuses[0].value;

    } catch (error) {
        console.error('Error loading statuses:', error);
        notify.error({ title: 'Error', message: 'Cannot load DVD statuses.' });
    }
}

function setupMovieSearch() {
    console.log('Setting up movie search');
    const searchInput = document.getElementById('movie-search');
    const dropdown = document.getElementById('movie-dropdown');
    const hiddenInput = document.getElementById('selected-movie-id');
    const dvdIdInput = document.getElementById('dvd-id');

    if (!searchInput) {
        console.error('Movie search input not found!');
        return;
    }

    if (!dropdown) {
        console.error('Movie dropdown not found!');
        return;
    }

    console.log('Search input and dropdown found');
    console.log('Adding event listener...');

    searchInput.addEventListener('input', function() {
        console.log('Input event fired! Value:', this.value);
        const query = this.value.trim().toLowerCase();

        if (query.length < 2) {
            console.log('Query too short, hiding dropdown');
            dropdown.style.display = 'none';
            return;
        }

        console.log('Filtering movies, allMovies length:', allMovies.length);
        if (allMovies.length === 0) {
            console.log('No movies loaded yet, using sample data for testing');
            // Sample data for testing
            allMovies = [
                { id: 'm1', title: 'The Shawshank Redemption' },
                { id: 'm2', title: 'The Godfather' },
                { id: 'm3', title: 'The Dark Knight' },
                { id: 'm4', title: 'Pulp Fiction' },
                { id: 'm5', title: 'Forrest Gump' },
                { id: 'm6', title: 'Inception' },
                { id: 'm7', title: 'The Matrix' },
                { id: 'm8', title: 'Titanic' }
            ];
        }

        const filteredMovies = allMovies.filter(movie => {
            const hasTitle = movie.title && typeof movie.title === 'string';
            const matchesQuery = hasTitle && movie.title.toLowerCase().includes(query);
            // Only show movies that don't already have a DVD
            const hasNoDVD = !allDVDs.some(dvd => dvd.movieId === movie.id);
            return matchesQuery && hasNoDVD;
        });

        console.log('Found', filteredMovies.length, 'matching movies');

        if (filteredMovies.length === 0) {
            if (query.length >= 2) {
                // Show a message if no movies are available
                dropdown.innerHTML = '<div class="dropdown-item" style="color: #6c757d; cursor: default;">No available movies (all movies already have DVDs)</div>';
                dropdown.style.display = 'block';
            } else {
                dropdown.style.display = 'none';
            }
            return;
        }

        dropdown.innerHTML = '';
        filteredMovies.forEach(movie => {
            const item = document.createElement('a');
            item.className = 'dropdown-item';
            item.href = '#';
            item.textContent = movie.title;
            item.dataset.movieId = movie.id;
            item.dataset.movieTitle = movie.title;

            item.addEventListener('click', function(e) {
                e.preventDefault();
                e.stopPropagation(); // Prevent event bubbling
                console.log('Movie selected:', movie.title, 'ID:', movie.id);
                searchInput.value = movie.title;
                hiddenInput.value = movie.id;

                // Auto-generate DVD ID from movie ID (e.g., m5 -> dvd5)
                const movieNumber = movie.id.replace('m', '');
                const generatedDvdId = `dvd${movieNumber}`;
                dvdIdInput.value = generatedDvdId;
                console.log('Generated DVD ID:', generatedDvdId);

                dropdown.style.display = 'none';
            });

            dropdown.appendChild(item);
        });

        console.log('Showing dropdown with', filteredMovies.length, 'items');
        dropdown.style.display = 'block';
    });

    // Hide dropdown when clicking outside
    document.addEventListener('click', function(e) {
        if (!searchInput.contains(e.target) && !dropdown.contains(e.target)) {
            console.log('Clicking outside, hiding dropdown');
            dropdown.style.display = 'none';
        }
    });
}

function loadDVDSelects() {
    const updateSelect = document.getElementById('update-dvd-select');
    const deleteSelect = document.getElementById('delete-dvd-select');

    if (updateSelect) updateSelect.innerHTML = '<option value="0">Select DVD</option>';
    if (deleteSelect) deleteSelect.innerHTML = '<option value="0">Select DVD</option>';

    allDVDs.forEach(dvd => {
        const movie = allMovies.find(m => m.id === dvd.movieId);
        const displayText = movie ? `${dvd.id} - ${movie.title}` : dvd.id;

        if (updateSelect) {
            const option = document.createElement('option');
            option.value = dvd.id;
            option.textContent = displayText;
            updateSelect.appendChild(option);
        }

        if (deleteSelect) {
            const option = document.createElement('option');
            option.value = dvd.id;
            option.textContent = displayText;
            deleteSelect.appendChild(option);
        }
    });
}

async function handleAddDVD() {
    const movieId = document.getElementById('selected-movie-id').value;
    const dvdId = document.getElementById('dvd-id').value;
    const price = parseFloat(document.getElementById('dvd-price').value) || 0;
    const stock = parseInt(document.getElementById('dvd-stock').value) || 0;
    const selectedStatusValue = document.getElementById('dvd-status').value;

    if (!movieId || !dvdId || price <= 0 || stock < 0) {
        notify.error({
            title: 'Validation Error',
            message: 'Please fill in all required fields with valid values.'
        });
        return;
    }

    // Trim DVD ID to avoid accidental spaces
    const trimmedDvdId = dvdId.trim();

    // Check if a DVD already exists for this movie
    const existingDVD = allDVDs.find(dvd => dvd.movieId === movieId);
    if (existingDVD) {
        const movie = allMovies.find(m => m.id === movieId);
        notify.error({
            title: 'Duplicate DVD Error',
            message: `A DVD already exists for "${movie?.title || 'Unknown Movie'}" (DVD ID: ${existingDVD.dvdId}). Each movie can only have one DVD.`
        });
        return;
    }

    // Check if the DVD ID itself is already used
    const duplicateDvdId = allDVDs.find(dvd => dvd.dvdId === trimmedDvdId);
    if (duplicateDvdId) {
        const movie = allMovies.find(m => m.id === duplicateDvdId.movieId);
        notify.error({
            title: 'Duplicate DVD ID',
            message: `DVD ID "${trimmedDvdId}" is already used for "${movie?.title || 'Unknown Movie'}".`
        });
        return;
    }

    const selectedStatus = allStatuses.find(s => s.value === selectedStatusValue);

    const dvdData = {
        dvdId: trimmedDvdId,
        movieId,
        price,
        stockCount: stock,        
        statusId: selectedStatus ? selectedStatus.id : null  
    };

    try {
        const result = await addDVD(dvdData);
        if (result.success) {
            notify.success({
                title: 'Success',
                message: `DVD ${dvdId} added successfully.`
            });
            resetAddForm();
            await loadData(); // Reload to update selects
        } else {
            notify.error({
                title: 'Save Failed',
                message: result.message || 'Cannot save DVD.'
            });
        }
    } catch (error) {
        notify.error({
            title: 'Save Failed',
            message: error.message
        });
        console.error('addDVD error', error);
    }
}

function setupUpdateMovieSearch() {
    const searchInput = document.getElementById('update-movie-search');
    const dropdown = document.getElementById('update-movie-dropdown');
    const hiddenInput = document.getElementById('update-selected-movie-id');
    const dvdIdInput = document.getElementById('update-dvd-id');

    if (!searchInput || !dropdown) return;

    searchInput.addEventListener('input', function() {
        const query = this.value.trim().toLowerCase();
        if (query.length < 2) {
            dropdown.style.display = 'none';
            return;
        }

        // Filter only movies that have a DVD
        const filteredMovies = allMovies.filter(movie => {
            const hasTitle = movie.title && typeof movie.title === 'string';
            const hasDVD = allDVDs.some(dvd => dvd.movieId === movie.id);
            const matchesQuery = hasTitle && movie.title.toLowerCase().includes(query);
            return matchesQuery && hasDVD;
        });

        if (filteredMovies.length === 0) {
            dropdown.innerHTML = '<div class="dropdown-item" style="color: #6c757d; cursor: default;">No matching movies with DVDs</div>';
            dropdown.style.display = 'block';
            return;
        }

        dropdown.innerHTML = '';
        filteredMovies.forEach(movie => {
            const dvd = allDVDs.find(d => d.movieId === movie.id);
            const item = document.createElement('a');
            item.className = 'dropdown-item';
            item.href = '#';
            item.textContent = movie.title;
            item.dataset.movieId = movie.id;
            item.dataset.dvdId = dvd ? dvd.dvdId : '';

            item.addEventListener('click', function(e) {
                e.preventDefault();
                hiddenInput.value = movie.id;
                searchInput.value = movie.title;
                dvdIdInput.value = dvd.dvdId;
                dropdown.style.display = 'none';
            });

            dropdown.appendChild(item);
        });

        dropdown.style.display = 'block';
    });

    document.addEventListener('click', function(e) {
        if (!searchInput.contains(e.target) && !dropdown.contains(e.target)) {
            dropdown.style.display = 'none';
        }
    });
}

async function updateDVDStock() {
    const dvdId = document.getElementById('update-dvd-id').value; // from search
    const newStock = document.getElementById('update-stock').value;
    const newStatusValue = document.getElementById('update-status').value;

    if (!dvdId) {
        notify.error({
            title: 'Validation Error',
            message: 'Please select a DVD to update.'
        });
        return;
    }

    const updateData = {};
    if (newStock !== '') {
        const dvd = allDVDs.find(d => d.dvdId === dvdId); // find the DVD
        if (dvd && typeof dvd.stockCount === 'number') {
            updateData.stockCount = dvd.stockCount + parseInt(newStock); // add to existing stock
        } else {
            updateData.stockCount = parseInt(newStock); // fallback if missing
        }
    }
    if (newStatusValue) {
        const selectedStatus = allStatuses.find(s => s.value === newStatusValue);
        if (selectedStatus) updateData.statusId = selectedStatus.id;
    }

    if (Object.keys(updateData).length === 0) {
        notify.error({
            title: 'Validation Error',
            message: 'Please provide at least one field to update.'
        });
        return;
    }

    try {
        const result = await updateDVD(dvdId, updateData);
        if (result.success) {
            notify.success({
                title: 'Success',
                message: 'DVD updated successfully.'
            });
            resetUpdateForm();
            await loadData(); // Reload to update selects
        } else {
            notify.error({
                title: 'Update Failed',
                message: result.message || 'Cannot update DVD.'
            });
        }
    } catch (error) {
        notify.error({
            title: 'Update Failed',
            message: error.message
        });
        console.error('updateDVDStock error', error);
    }
}

function setupDeleteMovieSearch() {
    const searchInput = document.getElementById('delete-movie-search');
    const dropdown = document.getElementById('delete-movie-dropdown');
    const hiddenInput = document.getElementById('delete-selected-movie-id');
    const dvdIdInput = document.getElementById('delete-dvd-id');

    if (!searchInput || !dropdown) return;

    searchInput.addEventListener('input', function() {
        const query = this.value.trim().toLowerCase();

        if (query.length < 2) {
            dropdown.style.display = 'none';
            return;
        }

        // ONLY movies that HAVE DVDs
        const filteredMovies = allMovies.filter(movie => {
            const hasDVD = allDVDs.some(dvd => dvd.movieId === movie.id);
            return movie.title.toLowerCase().includes(query) && hasDVD;
        });

        if (filteredMovies.length === 0) {
            dropdown.innerHTML = '<div class="dropdown-item">No movies with DVDs</div>';
            dropdown.style.display = 'block';
            return;
        }

        dropdown.innerHTML = '';

        filteredMovies.forEach(movie => {
            const dvd = allDVDs.find(d => d.movieId === movie.id);

            const item = document.createElement('a');
            item.className = 'dropdown-item';
            item.href = '#';
            item.textContent = movie.title;

            item.addEventListener('click', function(e) {
                e.preventDefault();

                searchInput.value = movie.title;
                hiddenInput.value = movie.id;
                dvdIdInput.value = dvd.dvdId; 

                dropdown.style.display = 'none';
            });

            dropdown.appendChild(item);
        });

        dropdown.style.display = 'block';
    });

    document.addEventListener('click', function(e) {
        if (!searchInput.contains(e.target) && !dropdown.contains(e.target)) {
            dropdown.style.display = 'none';
        }
    });
}

async function handleDeleteDVD() {
    const dvdId = document.getElementById('delete-dvd-id').value;

    if (!dvdId || dvdId === '0') {
        notify.error({
            title: 'Validation Error',
            message: 'Please select a DVD to delete.'
        });
        return;
    }

    // 👉 Set modal content
    document.getElementById('deleteDVDConfirmId').textContent = `DVD ID: ${dvdId}`;
    document.getElementById('deleteDVDConfirmInfo').textContent = 'This DVD will be permanently removed.';

    // 👉 Store selected DVD globally (for confirm button)
    window.selectedDeleteDVDId = dvdId;

    // 👉 Show modal (Bootstrap)
    $('#deleteDVDConfirmModal').modal('show');
}

async function confirmDeleteDVD() {
    const dvdId = window.selectedDeleteDVDId;

    if (!dvdId) return;

    try {
        const result = await deleteDVD(dvdId);

        if (result.success) {
            notify.success({
                title: 'Success',
                message: 'DVD deleted successfully.'
            });

            // Close modal
            $('#deleteDVDConfirmModal').modal('hide');

            resetDeleteForm();
            await loadData();
        } else {
            notify.error({
                title: 'Delete Failed',
                message: result.message || 'Cannot delete DVD.'
            });
        }

    } catch (error) {
        notify.error({
            title: 'Delete Failed',
            message: error.message
        });
        console.error('deleteDVD error', error);
    }
}

function resetAddForm() {
    document.getElementById('movie-search').value = '';
    document.getElementById('selected-movie-id').value = '';
    document.getElementById('dvd-id').value = '';
    document.getElementById('dvd-price').value = '';
    document.getElementById('dvd-stock').value = '';
    const addStatusSelect = document.getElementById('dvd-status');
    addStatusSelect.value = allStatuses.length > 0 ? allStatuses[0].value : '';
}

function resetUpdateForm() {
    const searchInput = document.getElementById('update-movie-search');
    if (searchInput) searchInput.value = '';

    const hiddenMovieId = document.getElementById('update-selected-movie-id');
    if (hiddenMovieId) hiddenMovieId.value = '';

    const dvdIdInput = document.getElementById('update-dvd-id');
    if (dvdIdInput) dvdIdInput.value = '';

    const updateStockInput = document.getElementById('update-stock');
    if (updateStockInput) updateStockInput.value = '';

    const updateStatusSelect = document.getElementById('update-status');
    if (updateStatusSelect) {
        updateStatusSelect.value = allStatuses.length > 0 ? allStatuses[0].value : '';
    }
}

function resetDeleteForm() {
    const searchInput = document.getElementById('delete-movie-search');
    if (searchInput) searchInput.value = '';

    const hiddenMovieId = document.getElementById('delete-selected-movie-id');
    if (hiddenMovieId) hiddenMovieId.value = '';

    const dvdIdInput = document.getElementById('delete-dvd-id');
    if (dvdIdInput) dvdIdInput.value = '';
}

// Expose functions for HTML onclick handlers
window.loadData = loadData;
window.addDVD = handleAddDVD;
window.updateDVDStock = updateDVDStock;
window.handleDeleteDVD = handleDeleteDVD;
window.confirmDeleteDVD = confirmDeleteDVD;

// Initialize when module loads
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}

function init() {
    console.log('Initializing DVD management page');
    loadStatuses();
    loadData();
    setupMovieSearch();        
    setupUpdateMovieSearch(); 
    setupDeleteMovieSearch();
}