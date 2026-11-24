import { verifyAdmin } from '../firebaseUtils.js';

async function adminSignInFunc(event) {
    event.preventDefault();
    const popup = Notification();

    const username = document.getElementById("login-username").value.trim();
    const password = document.getElementById("login-password").value.trim();

    if (!username || !password) {
        popup.error({ message: "Please enter both username and password" });
        return;
    }

    try {
        const result = await verifyAdmin(username, password);

        if (result.success) {
            // Store admin session
            sessionStorage.setItem("adminUser", JSON.stringify(result.data));
            popup.success({ message: "Welcome back!" });
            setTimeout(() => {
                window.location = "admin-dashboard.html";
            }, 1000);
        } else {
            popup.error({ message: result.message || "Invalid admin credentials." });
        }

    } catch (error) {
        popup.error({ message: "A network error occurred: " + error.message });
        console.error("Login error:", error);
    }
}

function adminLogoutFunc() {
    sessionStorage.removeItem("adminUser");
    window.location = "admin-login.html";
}

// Check if admin is logged in
function checkAdminSessionFunc() {
    const admin = sessionStorage.getItem("adminUser");
    if (!admin) {
        window.location = "admin-login.html";
    }
}

// Export to global scope for module-init
if (window.setGlobalFunctions) {
    window.setGlobalFunctions({
        adminSignIn: adminSignInFunc,
        adminLogout: adminLogoutFunc,
        checkAdminSession: checkAdminSessionFunc
    });
}

// Mark modules as ready
if (window.setModulesReady) {
    window.setModulesReady();
}
