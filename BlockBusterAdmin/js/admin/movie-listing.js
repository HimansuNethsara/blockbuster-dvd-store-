import { getAllGenres, getAllActors, getAllDirectors, getAllPlotKeywords, getAllMpaaRatings, getAllStatus, addMovie, updateMovie } from '../firebaseUtils.js';
import { storage } from '../../firebaseConfig.js';
import { ref, uploadBytes, getDownloadURL } from 'https://www.gstatic.com/firebasejs/10.12.2/firebase-storage.js';

const notify = Notification();
let activeStatusId = ''; // To store the active status ID

async function loadProductData() {
    try {
        const [genreResult, actorResult, directorResult, keywordResult, mpaaResult, statusResult] = await Promise.all([
            getAllGenres(),
            getAllActors(),
            getAllDirectors(),
            getAllPlotKeywords(),
            getAllMpaaRatings(),
            getAllStatus()
        ]);

        if (!genreResult.success || !actorResult.success || !directorResult.success || !keywordResult.success || !mpaaResult.success || !statusResult.success) {
            notify.error({
                title: 'Data Load Failed',
                message: 'Failed to load required data from Firestore. Check console for details.'
            });
            console.error('loadProductData: one or more collections failed', {
                genreResult,
                actorResult,
                directorResult,
                keywordResult,
                mpaaResult,
                statusResult
            });
            return;
        }

        // Find the active status
        const activeStatus = statusResult.data.find(status => status.value && status.value.toLowerCase() === 'active');
        if (activeStatus) {
            activeStatusId = activeStatus.id;
        }

        loadMpaaSelect('mpaaRating', mpaaResult.data);
        loadSelect('genre', genreResult.data, 'name');
        loadSelect('actor', actorResult.data, 'name');
        loadSelect('director', directorResult.data, 'name');
        loadSelect('keyword', keywordResult.data, 'keyword');

        // initialize multiselect wrappers after inserting options
        bindMultiSelect('#genre');
        bindMultiSelect('#actor');
        bindMultiSelect('#director');
        bindMultiSelect('#keyword');

    } catch (error) {
        notify.error({
            title: 'Unexpected Error',
            message: error.message || 'Something went wrong while loading product data.'
        });
        console.error('loadProductData error', error);
    }
}

function loadSelect(selectId, list, property) {
    const select = document.getElementById(selectId);
    if (!select) {
        console.warn('loadSelect: select not found', selectId);
        return;
    }
    select.innerHTML = '';
    list.forEach(item => {
        const option = document.createElement('option');
        option.value = item.id;
        option.setAttribute('data-value', item.id);
        option.innerText = item[property] || item.name || item.value || 'Unknown';
        select.appendChild(option);
    });
}

function loadMpaaSelect(selectId, list) {
    const select = document.getElementById(selectId);
    if (!select) return;
    select.innerHTML = '<option value="0">Select</option>';
    list.forEach(item => {
        const option = document.createElement('option');
        option.value = item.id;
        option.innerText = item.value || item.name || item.rating || 'Unknown';
        select.appendChild(option);
    });
}

function bindMultiSelect(selector) {
    const existing = document.querySelector(selector);

    if (!existing) {
        console.warn('bindMultiSelect: element not found', selector);
        return;
    }

    const multiselect = new MultiSelect(existing);

    // store instance safely on wrapper
    const wrapper = document.querySelector(selector);
    if (wrapper) {
        wrapper.multiSelectInstance = multiselect;
    }
}

function getSelectedValues(name) {
    const wrapper = document.querySelector(`.multi-select#${name}`);

    if (!wrapper) {
        console.warn(`MultiSelect wrapper not found for ${name}`);
        return [];
    }

    const hiddenInputs = wrapper.querySelectorAll(`input[name="${name}[]"]`);

    return Array.from(hiddenInputs).map(input => input.value);
}

function validateLimits() {
    const limits = {
        genre: 4,
        director: 5,
        keyword: 6,
        actor: Infinity
    };

    for (const [key, max] of Object.entries(limits)) {
        const selected = getSelectedValues(key).length;
        if (selected > max) {
            return `${key.charAt(0).toUpperCase() + key.slice(1)} allows a maximum of ${max} selections.`;
        }
    }
    return null;
}

async function uploadPoster(file, movieId) {
    if (!file || !movieId) return null;
    const fileExtension = file.name.split('.').pop().toLowerCase();
    const filePath = `movies/${movieId}/poster.${fileExtension}`;
    const imageRef = ref(storage, filePath);
    await uploadBytes(imageRef, file);
    return filePath; // Return the path, not the URL
}

async function saveProduct() {
    const title = document.getElementById('title').value.trim();
    const description = document.getElementById('description').value.trim();
    const imdbRating = parseFloat(document.getElementById('imdb').value) || 0;
    const rtRating = parseInt(document.getElementById('rt_rating').value, 10) || 0;
    const mpaaRatingId = document.getElementById('mpaaRating').value;
    const trailerUrl = document.getElementById('trailer').value.trim();
    const releaseYear = parseInt(document.getElementById('year').value, 10) || null;
    const runtime = parseInt(document.getElementById('runtime').value, 10) || null;
    const imageFile = document.getElementById('img1').files[0];

    const validationError = validateLimits();
    if (validationError) {
        notify.error({ title: 'Validation Error', message: validationError });
        return;
    }

    if (!title || !description) {
        notify.error({ title: 'Validation Error', message: 'Title and Description are required.' });
        return;
    }

    // Get all selected values
    const genreIds = getSelectedValues('genre');
    const actorIds = getSelectedValues('actor');
    const directorIds = getSelectedValues('director');
    const plotKeywordIds = getSelectedValues('keyword');
    
    console.log('Selected values:', { genreIds, actorIds, directorIds, plotKeywordIds });

    // Add movie first to get ID
    const movieData = {
        title,
        description,
        imdbRating,
        rtRating,
        mpaaRatingId,
        trailerUrl,
        releaseYear,
        runtime,
        genreIds,
        actorIds,
        directorIds,
        plotKeywordIds,
        statusId: activeStatusId,
        downloadUri: null
    };

    try {
        const addResult = await addMovie(movieData);
        if (!addResult.success) {
            notify.error({ title: 'Save Failed', message: addResult.message || 'Cannot save movie.' });
            return;
        }

        const movieId = addResult.id;

        // Upload image if provided
        let images = null;
        if (imageFile) {
            images = await uploadPoster(imageFile, movieId);
        }

        // Update movie with image path
        const updateData = {};
        if (images) {
            updateData.images = [images];
        }

        if (Object.keys(updateData).length > 0) {
            const updateResult = await updateMovie(movieId, updateData);
            if (!updateResult.success) {
                notify.error({ title: 'Update Failed', message: 'Movie saved but image update failed.' });
                return;
            }
        }

        notify.success({ title: 'Success', message: 'Movie added successfully.' });
        resetForm();
    } catch (error) {
        notify.error({ title: 'Save Failed', message: error.message });
        console.error('saveProduct error', error);
    }
}

function resetForm() {
    document.getElementById('title').value = '';
    document.getElementById('description').value = '';
    document.getElementById('imdb').value = '';
    document.getElementById('rt_rating').value = '';
    document.getElementById('mpaaRating').value = '0';
    document.getElementById('trailer').value = '';
    document.getElementById('year').value = '';
    document.getElementById('runtime').value = '';
    document.getElementById('img1').value = '';

    ['genre', 'actor', 'director', 'keyword'].forEach(key => {
        const wrapper = document.querySelector(`.multi-select#${key}`);
        if (wrapper && wrapper.multiSelectInstance) {
            wrapper.multiSelectInstance.clearSelection();
        }
    });
}

// Expose for inline onload / button onclick handlers
window.loadProductData = loadProductData;
window.saveProduct = saveProduct;

