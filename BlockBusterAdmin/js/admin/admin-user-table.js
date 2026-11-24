import { getAllUsers, deleteUser, updateUser } from '../firebaseUtils.js';

let allUsers = [];

async function loadUsersFunc() {
    const popup = Notification();
    if (window.checkAdminSession && typeof window.checkAdminSession === "function") {
        window.checkAdminSession();
    }
    
    try {
        const result = await getAllUsers();

        if (!result.success) {
            popup.error({ message: "Failed to load users: " + result.message });
            return;
        }

        allUsers = result.data;
        const tbody = document.querySelector("table tbody");
        tbody.innerHTML = "";

        if (allUsers.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center text-muted">No users found</td></tr>`;
            return;
        }

        allUsers.forEach(user => {
            let registeredDate = "N/A";

            if (user.registeredDate) {
                if (typeof user.registeredDate === "string") {
                    registeredDate = user.registeredDate;
                } else if (user.registeredDate.toDate) {
                    registeredDate = new Date(user.registeredDate.toDate()).toLocaleDateString();
                }
            }
            
            const row = document.createElement("tr");
            row.innerHTML = `
                <th scope="row">${user.id.substring(0, 8)}...</th>
                <td>${user.firstName || "N/A"}</td>
                <td>${user.lastName || "N/A"}</td>
                <td>${user.email}</td>
                <td><span class="badge badge-success">Active</span></td>
                <td>${registeredDate}</td>
                <td>
                    <button class="btn btn-sm btn-warning" onclick="editUser('${user.id}')">Edit</button>
                    <button class="btn btn-sm btn-danger" onclick="removeUser('${user.id}', '${user.email}')">Delete</button>
                </td>
            `;
            tbody.appendChild(row);
        });

        popup.success({ message: "User data loaded successfully." });

    } catch (error) {
        popup.error({ message: "Error: " + error.message });
    }
}

let userToDelete = null;

function removeUserFunc(userId, email) {
    userToDelete = { id: userId, email };

    document.getElementById("deleteUserEmail").textContent = email;
    $('#deleteUserModal').modal('show');
}

async function confirmDeleteUser() {
    const popup = Notification();

    if (!userToDelete) return;

    try {
        const result = await deleteUser(userToDelete.id);

        if (result.success) {
            popup.success({ message: result.message });
            await loadUsersFunc();
        } else {
            popup.error({ message: "Failed to delete user: " + result.message });
        }
    } catch (error) {
        popup.error({ message: "Error: " + error.message });
    } finally {
        $('#deleteUserModal').modal('hide');
        userToDelete = null;
    }
}

function editUserFunc(userId) {
    const user = allUsers.find(u => u.id === userId);
    if (!user) return;

    document.getElementById("editUserId").value = userId;
    document.getElementById("editFirstName").value = user.firstName || "";
    document.getElementById("editLastName").value = user.lastName || "";

    $('#editUserModal').modal('show');
}

async function updateUserData(userId, userData) {
    const popup = Notification();
    
    try {
        const result = await updateUser(userId, userData);
        
        if (result.success) {
            popup.success({ message: result.message });
            await loadUsersFunc();
        } else {
            popup.error({ message: "Failed to update user: " + result.message });
        }
    } catch (error) {
        popup.error({ message: "Error: " + error.message });
    }
}

async function saveUserChangesFunc() {
    const userId = document.getElementById("editUserId").value;
    const firstName = document.getElementById("editFirstName").value.trim();
    const lastName = document.getElementById("editLastName").value.trim();

    if (!firstName || !lastName) {
        Notification().error({ message: "Both names are required" });
        return;
    }

    await updateUserData(userId, {
        firstName,
        lastName,
        fullName: firstName + " " + lastName
    });

    $('#editUserModal').modal('hide');
}

function printUserListFunc() {
    const title_top = "BlockBuster - User Management";
    const title = "User List";

    const tableHTML = document.querySelector("table").outerHTML;

    const printWindow = window.open('', '', 'width=900,height=650');

    printWindow.document.write(`
        <html>
        <head>
            <title>${title_top}</title>

            <!-- Load external CSS -->
               <link rel="stylesheet" href="../css/vendor/bootstrap.min.css">
                <!-- Font Awesome CSS-->
                <link rel="stylesheet" href="../fonts/font-awesome/css/font-awesome.min.css">
                <!-- Custom Font Icons CSS-->
                <link rel="stylesheet" href="../css/admin/font.css">
                <!-- Google fonts - Muli-->
                <link rel="stylesheet" href="https://fonts.googleapis.com/css?family=Muli:300,400,700">
                <!-- theme stylesheet-->
                <link rel="stylesheet" href="../css/admin/style.default.css" id="theme-stylesheet">

            <!-- Add optional inline styles -->
            <style>
                body {
                    padding: 20px;
                }
                h2 {
                    text-align: center;
                    margin-bottom: 10px;
                }
            </style>
        </head>
        <body>
            <h2>${title}</h2>
            <div class="table-responsive">
                ${tableHTML}
            </div>
            <script>window.print();</script>
        </body>
        </html>
    `);
}

window.setGlobalFunctions({
    ...window.globalFunctions,
    loadUsers: loadUsersFunc,
    removeUser: removeUserFunc,
    confirmDeleteUser: confirmDeleteUser,
    editUser: editUserFunc,
    saveUserChanges: saveUserChangesFunc,
    printUserList: printUserListFunc
});

loadUsersFunc();

if (window.setModulesReady) {
    window.setModulesReady();
}





