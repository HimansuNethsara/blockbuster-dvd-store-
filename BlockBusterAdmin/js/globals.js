// Global wrapper functions for Firebase operations
// This file bridges module functions to global scope for onclick handlers

// Import functions - these will be set by the module imports
window.globalFunctions = window.globalFunctions || {};
let globalFunctions = window.globalFunctions;

// Export setter
window.setGlobalFunctions = function (funcs) {
    globalFunctions = {
        ...globalFunctions,
        ...funcs
    };

    window.globalFunctions = globalFunctions;

    console.log("Global functions registered", globalFunctions);
};

// Admin functions
window.adminSignIn = async function (event) {
    event.preventDefault();
    if (globalFunctions.adminSignIn) {
        return globalFunctions.adminSignIn(event);
    }
};

window.adminLogout = function () {
    if (globalFunctions.adminLogout) {
        return globalFunctions.adminLogout();
    }
};

window.checkAdminSession = function () {
    if (globalFunctions.checkAdminSession) {
        return globalFunctions.checkAdminSession();
    }
};

// User functions
window.loadUsers = async function () {
    if (globalFunctions.loadUsers) {
        return globalFunctions.loadUsers();
    }
};

window.editUser = function (userId) {
    if (globalFunctions.editUser) {
        return globalFunctions.editUser(userId);
    }
};

window.removeUser = function (userId, email) {
    if (globalFunctions.removeUser) {
        return globalFunctions.removeUser(userId, email);
    }
};

window.printUserList = function () {
    if (globalFunctions.printUserList) {
        return globalFunctions.printUserList();
    }
};

window.saveUserChanges = function () {
    if (globalFunctions.saveUserChanges) {
        return globalFunctions.saveUserChanges();
    }
};

// Movie functions
async function loadMovies() {
    if (globalFunctions.loadMovies) {
        return globalFunctions.loadMovies();
    }
}

function editMovie(movieId) {
    if (globalFunctions.editMovie) {
        return globalFunctions.editMovie(movieId);
    }
}

function removeMovie(movieId, title) {
    if (globalFunctions.removeMovie) {
        return globalFunctions.removeMovie(movieId, title);
    }
}

function toggleMovieStatus(movieId, checkbox) {
    if (globalFunctions.toggleMovieStatus) {
        return globalFunctions.toggleMovieStatus(movieId, checkbox);
    }
}

function printMovieList() {
    if (globalFunctions.printMovieList) {
        return globalFunctions.printMovieList();
    }
}

// Cast functions
async function loadCastData() {
    if (globalFunctions.loadCastData) {
        return globalFunctions.loadCastData();
    }
}

function addActor() {
    if (globalFunctions.addActor) {
        return globalFunctions.addActor();
    }
}

function addDirector() {
    if (globalFunctions.addDirector) {
        return globalFunctions.addDirector();
    }
}

function removeActor(actorId, name) {
    if (globalFunctions.removeActor) {
        return globalFunctions.removeActor(actorId, name);
    }
}

function removeDirector(directorId, name) {
    if (globalFunctions.removeDirector) {
        return globalFunctions.removeDirector(directorId, name);
    }
}

// Dashboard functions
async function adminStatistics() {
    if (globalFunctions.adminStatistics) {
        return globalFunctions.adminStatistics();
    }
}
