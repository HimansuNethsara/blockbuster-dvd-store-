// Module initialization - waits for modules to load
let modulesReady = false;

// Wrapper functions that will be populated by modules
window.adminSignIn = async (event) => {
    if (!modulesReady) {
        alert("System is loading. Please wait...");
        return;
    }
    return window._adminSignInImpl(event);
};

window.adminLogout = () => {
    if (window._adminLogoutImpl) {
        return window._adminLogoutImpl();
    }
};

window.checkAdminSession = () => {
    if (window._checkAdminSessionImpl) {
        return window._checkAdminSessionImpl();
    }
};

// Mark modules as ready when all imports are loaded
window.setModulesReady = () => {
    modulesReady = true;
    console.log("Firebase modules are ready");
};
