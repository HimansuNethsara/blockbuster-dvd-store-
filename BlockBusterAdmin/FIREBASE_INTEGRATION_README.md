# BlockBuster Admin Firebase Integration - Complete Setup

## Overview
Your web application is now fully connected to Firebase with complete CRUD (Create, Read, Update Delete) operations implemented for all collections.

## Files Created/Modified

### Core Firebase files:
1. **firebaseConfig.js** - Updated with Firestore, Auth, and Storage exports
2. **js/firebaseUtils.js** - NEW utility file with all database operation functions

### Updated Admin JS Files:
1. **js/admin/admin-sign-in.js** - Firebase authentication with session management
2. **js/admin/admin-user-table.js** - User CRUD operations
3. **js/admin/admin-movie-table.js** - Movie CRUD operations
4. **js/admin/admin-cast-form.js** - Actor/Director CRUD with image uploads
5. **js/admin/admin-home.js** - Dashboard statistics from Firebase
6. **js/globals.js** - NEW global function wrappers for module compatibility

### Updated HTML Files:
1. **html/admin-login.html** - Firebase module scripts added
2. **html/admin-dashboard.html** - Firebase module scripts added
3. **html/admin-user-table.html** - Actions column added, Firebase modules added
4. **html/admin-movie-table.html** - Actions column added, Firebase modules added
5. **html/admin-cast-form.html** - Actor/Director display sections added, Firebase modules added

## Collections in Firestore

The system automatically creates and manages these collections:
- **admin** - Admin credentials (email, password, username, created_at)
- **users** - User profiles (uid, firstName, lastName, email, profilePicUrl, registeredDate)
- **movies** - Movie details (title, description, images[], ratings, year, runtime, etc.)
- **actors** - Actor info (name, imagePath)
- **directors** - Director info (name, imagePath)
- **genres** - Genre list (name, imageUrl)
- **dvds** - DVD inventory (price, movieId, stockCount, statusId)
- **mpaaRatings** - MPAA rating reference data
- **status** - Status reference data
- **plotKeywords** - Plot keywords for movies
- **orders** - Customer orders

## Key Features Implemented

### 1. Admin Authentication
- Login with username/password verification from Firestore
- Session management via sessionStorage
- Automatic session check on admin pages
- Logout functionality

### 2. User Management
- View all users with registration date
- Edit user details (first name, last name)
- Delete users
- Print user list

### 3. Movie Management
- View all movies with posters
- Edit movie details (title, ratings, year, runtime)
- Delete movies
- Toggle movie status (active/inactive)
- Print movie list
- Image error handling

### 4. Cast Management (Actors & Directors)
- Upload images to Firebase Storage
- Add actors with profile images
- Add directors with profile images
- Display actors and directors in card format
- Delete actors and directors
- Image management

### 5. Dashboard Statistics
- Total users count
- Total movies count
- Total orders count
- Progress bars for statistics
- Recent users display

## How Functions Work

### Global Function Access
All functions are exposed to the global scope through `window` object to work with HTML onclick handlers:
- `adminSignIn(event)`
- `loadUsers()`
- `removeUser(userId, email)`
- `editUser(userId)`
- `loadMovies()`
- `removeMovie(movieId, title)`
- `editMovie(movieId)`
- `toggleMovieStatus(movieId, checkbox)`
- `loadCastData()`
- `addActor()`
- `addDirector()`
- `removeActor(actorId, name)`
- `removeDirector(directorId, name)`
- `adminStatistics()`

## Database Operations (firebaseUtils.js)

All CRUD functions follow this pattern:

```javascript
// Example
const result = await getAllUsers();
if (result.success) {
  const users = result.data;
  // Handle users
} else {
  console.error(result.message);
}
```

## Image Handling

Images are stored in Firebase Storage at:
- `actors/{timestamp}_{filename}` - Actor images
- `directors/{timestamp}_{filename}` - Director images

Download URLs are automatically saved in Firestore.

## Session Management

Admin sessions are stored in `sessionStorage`:
```javascript
sessionStorage.setItem("adminUser", JSON.stringify(adminData));
```

This persists across page refreshes but clears when the browser closes.

## Error Handling

All operations include try-catch error handling with user-friendly notifications via the Notification system. Errors display as popups.

## Next Steps

1. Populate your Firestore collections with initial data
2. Test login with your admin credentials
3. Test CRUD operations on each page
4. Verify image uploads to Firebase Storage
5. Check dashboard statistics update correctly

## Important Notes

- Make sure your Firestore rules allow read/write access (update for production security)
- Images are uploaded to Firebase Storage - ensure Storage rules are configured
- All timestamps use Firestore's serverTimestamp() for consistency
- Module scripts require a web server (won't work with file:// protocol)

## Firestore Rules (for development - SECURE FOR PRODUCTION)

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

## Support Collections Added

The firebaseUtils.js also includes functions for:
- Genres management
- MPAA Ratings
- Status reference data
- Plot Keywords
- DVD inventory management

Use similar patterns to query and manage these collections as needed.
