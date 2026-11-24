import { addActor, getAllActors, deleteActor, updateActor, addDirector, getAllDirectors, deleteDirector, updateDirector } from '../firebaseUtils.js';
import { storage } from '../../firebaseConfig.js';
import { ref, uploadBytes, getDownloadURL } from 'https://www.gstatic.com/firebasejs/10.12.2/firebase-storage.js';

const notify = Notification();
let actors = [];
let directors = [];
let actorPage = 1;
let directorPage = 1;
const PAGE_SIZE = 12;

let selectedActorId = null;
let selectedActorName = "";
let selectedDirectorId = null;
let selectedDirectorName = "";

async function loadCastData() {
    checkAdminSession();
    await loadActors();
    await loadDirectors();
}

async function addActorItem() {
    const name = document.getElementById("actor-name").value.trim();
    const imageFile = document.getElementById("img1").files[0];

    if (!name || !imageFile) {
        notify.error({
            title: "Missing Fields",
            message: "Please fill in both name and image."
        });
        return;
    }

    // Check for duplicate names
    try {
        const actorsResult = await getAllActors();
        if (actorsResult.success) {
            const existingActor = actorsResult.data.find(actor => actor.name.toLowerCase() === name.toLowerCase());
            if (existingActor) {
                notify.error({
                    title: "Duplicate Actor",
                    message: `An actor with the name "${name}" already exists.`
                });
                return;
            }
        }
    } catch (error) {
        console.warn("Could not check for duplicate actors:", error);
        // Continue with add, but warn
    }

    try {
        // Add actor to Firestore first to get ID
        const addResult = await addActor({
            name: name
        });

        if (!addResult.success) {
            notify.error({
                title: "Failed",
                message: addResult.message
            });
            return;
        }

        const actorId = addResult.actorId;

        // Upload image to Firebase Storage with ID-based name
        const imagePath = `actors/${actorId}.jpg`;
        const imageRef = ref(storage, imagePath);
        await uploadBytes(imageRef, imageFile);
        const imageUrl = await getDownloadURL(imageRef);

        // Update actor with image path
        const updateResult = await updateActor(actorId, {
            imagePath: imagePath
        });

        if (updateResult.success) {
            notify.success({
                title: "Success",
                message: "Actor added successfully"
            });
            document.getElementById("actor-name").value = "";
            document.getElementById("img1").value = "";
            await loadActors();
        } else {
            notify.error({
                title: "Failed",
                message: updateResult.message
            });
        }
    } catch (error) {
        notify.error({
            title: "Error",
            message: error.message
        });
    }
}

async function addDirectorItem() {
    const name = document.getElementById("director-name").value.trim();
    const imageFile = document.getElementById("img2").files[0];

    if (!name || !imageFile) {
        notify.error({
            title: "Missing Fields",
            message: "Please fill in both name and image."
        });
        return;
    }

    // Check for duplicate names
    try {
        const directorsResult = await getAllDirectors();
        if (directorsResult.success) {
            const existingDirector = directorsResult.data.find(director => director.name.toLowerCase() === name.toLowerCase());
            if (existingDirector) {
                notify.error({
                    title: "Duplicate Director",
                    message: `A director with the name "${name}" already exists.`
                });
                return;
            }
        }
    } catch (error) {
        console.warn("Could not check for duplicate directors:", error);
        // Continue with add, but warn
    }

    try {
        // Add director to Firestore first to get ID
        const addResult = await addDirector({
            name: name
        });

        if (!addResult.success) {
            notify.error({
                title: "Failed",
                message: addResult.message
            });
            return;
        }

        const directorId = addResult.directorId;

        // Upload image to Firebase Storage with ID-based name
        const imagePath = `directors/${directorId}.jpg`;
        const imageRef = ref(storage, imagePath);
        await uploadBytes(imageRef, imageFile);
        const imageUrl = await getDownloadURL(imageRef);

        // Update director with image path
        const updateResult = await updateDirector(directorId, {
            imagePath: imagePath
        });

        if (updateResult.success) {
            notify.success({
                title: "Success",
                message: "Director added successfully"
            });
            document.getElementById("director-name").value = "";
            document.getElementById("img2").value = "";
            await loadDirectors();
        } else {
            notify.error({
                title: "Failed",
                message: updateResult.message
            });
        }
    } catch (error) {
        notify.error({
            title: "Error",
            message: error.message
        });
    }
}

async function loadActors() {
    try {
        const result = await getAllActors();
        if (result.success) {
            actors = result.data;
            actorPage = 1;
            await displayActors();
        } else {
            notify.error({ message: "Failed to load actors: " + result.message });
        }
    } catch (error) {
        notify.error({ message: "Error: " + error.message });
    }
}

async function loadDirectors() {
    try {
        const result = await getAllDirectors();
        if (result.success) {
            directors = result.data;
            directorPage = 1;
            await displayDirectors();
        } else {
            notify.error({ message: "Failed to load directors: " + result.message });
        }
    } catch (error) {
        notify.error({ message: "Error: " + error.message });
    }
}

async function resolveCastImageUrl(item, folder) {
    const fallback = '../images/avatar.png';
    if (!item) return fallback;

    let candidate = item.imagePath || item.image || item.imageUrl || '';

    if (candidate) {
        if (candidate.startsWith('http://') || candidate.startsWith('https://')) {
            return candidate;
        }
        if (candidate.startsWith('/')) {
            return candidate;
        }

        let storagePath = candidate;
        if (!storagePath.includes('/')) {
            storagePath = `${folder}/${storagePath}`;
        }
        if (!storagePath.startsWith(`${folder}/`)) {
            storagePath = `${folder}/${storagePath}`;
        }

        try {
            return await getDownloadURL(ref(storage, storagePath));
        } catch (err) {
            console.warn(`Could not resolve ${folder} image from Storage: ${storagePath}`, err);
        }
    }

    // Try custom ID with multiple extensions
    const customId = folder === 'actors' ? item.actorId : item.directorId;
    if (customId) {
        const extensions = ['.jpg', '.jpeg', '.png'];
        for (const ext of extensions) {
            const idPath = `${folder}/${customId}${ext}`;
            try {
                return await getDownloadURL(ref(storage, idPath));
            } catch (err) {
                console.warn(`Could not resolve ${folder} image from custom ID path: ${idPath}`, err);
            }
        }
    }

    // Try document ID with multiple extensions for backward compatibility
    const extensions = ['.jpeg', '.jpg', '.png'];
    for (const ext of extensions) {
        if (item.id) {
            const idPath = `${folder}/${item.id}${ext}`;
            try {
                return await getDownloadURL(ref(storage, idPath));
            } catch (err) {
                console.warn(`Could not resolve ${folder} image from ID path: ${idPath}`, err);
            }
        }
    }

    return fallback;
}

async function displayActors() {
    const container = document.getElementById("actors-list");
    if (!container) return;

    container.innerHTML = "";

    if (actors.length === 0) {
        container.innerHTML = `<p class="text-muted">No actors added yet.</p>`;
        renderActorsPagination();
        return;
    }

    const startIndex = (actorPage - 1) * PAGE_SIZE;
    const pageActors = actors.slice(startIndex, startIndex + PAGE_SIZE);

    for (const actor of pageActors) {
        const actorCard = document.createElement("div");
        actorCard.className = "col-md-2 mb-3";
        actorCard.innerHTML = `
            <div class="card">
                <img id="actor-img-${actor.id}" src="../images/avatar.png" class="card-img-top" alt="${actor.name}" style="width: 80%; height: 180px; object-fit: cover; margin: 0 auto; display: block;" onerror="this.onerror=null;this.src='../images/avatar.png'">
                <div class="card-body p-1 text-center">
                    <h5 class="card-title" style="font-size: 0.8rem; margin-bottom: 0.3rem;">${actor.name}</h5>
                    <button class="btn btn-sm btn-danger" onclick="removeActor('${actor.id}', '${actor.name}')">Delete</button>
                </div>
            </div>
        `;
        container.appendChild(actorCard);

        const src = await resolveCastImageUrl(actor, 'actors');
        const imgEl = document.getElementById(`actor-img-${actor.id}`);
        if (imgEl) imgEl.src = src;
    }

    renderActorsPagination();
}

async function displayDirectors() {
    const container = document.getElementById("directors-list");
    if (!container) return;
    
    container.innerHTML = "";
    
    if (directors.length === 0) {
        container.innerHTML = `<p class="text-muted">No directors added yet.</p>`;
        renderDirectorsPagination();
        return;
    }

    const startIndex = (directorPage - 1) * PAGE_SIZE;
    const pageDirectors = directors.slice(startIndex, startIndex + PAGE_SIZE);

    for (const director of pageDirectors) {
        const directorCard = document.createElement("div");
        directorCard.className = "col-md-2 mb-3";
        directorCard.innerHTML = `
            <div class="card">
                <img id="director-img-${director.id}" src="../images/avatar.png" class="card-img-top" alt="${director.name}" style="width: 80%; height: 180px; object-fit: cover; margin: 0 auto; display: block;" onerror="this.onerror=null;this.src='../images/avatar.png'">
                <div class="card-body p-1 text-center">
                    <h5 class="card-title" style="font-size: 0.8rem; margin-bottom: 0.3rem;">${director.name}</h5>
                    <button class="btn btn-sm btn-danger" onclick="removeDirector('${director.id}', '${director.name}')">Delete</button>
                </div>
            </div>
        `;
        container.appendChild(directorCard);

        const src = await resolveCastImageUrl(director, 'directors');
        const imgEl = document.getElementById(`director-img-${director.id}`);
        if (imgEl) imgEl.src = src;
    }

    renderDirectorsPagination();
}

function renderActorsPagination() {
    const pager = document.querySelector('#actors-pagination');
    if (!pager) return;

    const totalPages = Math.max(1, Math.ceil(actors.length / PAGE_SIZE));
    pager.innerHTML = '';

    for (let i = 1; i <= totalPages; i++) {
        const button = document.createElement('button');
        button.className = 'btn btn-sm btn-outline-secondary mr-1 mb-1';
        button.textContent = i;
        if (i === actorPage) {
            button.classList.add('active');
        }
        button.onclick = async () => {
            actorPage = i;
            await displayActors();
        };
        pager.appendChild(button);
    }
}

function renderDirectorsPagination() {
    const pager = document.querySelector('#directors-pagination');
    if (!pager) return;

    const totalPages = Math.max(1, Math.ceil(directors.length / PAGE_SIZE));
    pager.innerHTML = '';

    for (let i = 1; i <= totalPages; i++) {
        const button = document.createElement('button');
        button.className = 'btn btn-sm btn-outline-secondary mr-1 mb-1';
        button.textContent = i;
        if (i === directorPage) {
            button.classList.add('active');
        }
        button.onclick = async () => {
            directorPage = i;
            await displayDirectors();
        };
        pager.appendChild(button);
    }
}

function removeActor(actorId, name) {
    selectedActorId = actorId;
    selectedActorName = name;

    // Set modal text
    document.getElementById("deleteActorName").textContent = name;
    document.getElementById("deleteActorId").textContent = `ID: ${actorId}`;

    // Show modal
    $('#deleteActorModal').modal('show');
}

async function confirmDeleteActor() {
    if (!selectedActorId) return;

    try {
        const result = await deleteActor(selectedActorId);

        if (result.success) {
            notify.success({ message: result.message });

            // Hide modal
            $('#deleteActorModal').modal('hide');

            // Reset selection
            selectedActorId = null;
            selectedActorName = "";

            await loadActors();
        } else {
            notify.error({ message: "Failed to delete: " + result.message });
        }
    } catch (error) {
        notify.error({ message: "Error: " + error.message });
    }
}

function removeDirector(directorId, name) {
    selectedDirectorId = directorId;
    selectedDirectorName = name;

    // Set modal text
    document.getElementById("deleteDirectorName").textContent = name;
    document.getElementById("deleteDirectorId").textContent = `ID: ${directorId}`;

    // Show modal
    $('#deleteDirectorModal').modal('show');
}

async function confirmDeleteDirector() {
    if (!selectedDirectorId) return;

    try {
        const result = await deleteDirector(selectedDirectorId);

        if (result.success) {
            notify.success({ message: result.message });

            // Hide modal
            $('#deleteDirectorModal').modal('hide');

            // Reset selection
            selectedDirectorId = null;
            selectedDirectorName = "";

            await loadDirectors();
        } else {
            notify.error({ message: "Failed to delete: " + result.message });
        }
    } catch (error) {
        notify.error({ message: "Error: " + error.message });
    }
}

// Alias functions for backward compatibility
function addActorFunc() {
    addActorItem();
}

function addDirectorFunc() {
    addDirectorItem();
}

// Export to global scope
window.loadCastData = loadCastData;
window.addActor = addActorFunc;
window.addDirector = addDirectorFunc;
window.removeActor = removeActor;
window.confirmDeleteActor = confirmDeleteActor;
window.removeDirector = removeDirector;
window.confirmDeleteDirector = confirmDeleteDirector;
