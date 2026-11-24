import { db } from "../firebaseConfig.js";
import {
  collection,
  addDoc,
  setDoc,
  updateDoc,
  deleteDoc,
  doc,
  getDocs,
  getDoc,
  query,
  where,
  Timestamp,
  serverTimestamp,
  increment
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";

// ==================== USER OPERATIONS ====================
export async function addUser(userData) {
  try {
    const usersRef = collection(db, "users");
    const newUser = {
      ...userData,
      registeredDate: serverTimestamp()
    };
    const docRef = await addDoc(usersRef, newUser);
    return { success: true, id: docRef.id, message: "User added successfully" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function getAllUsers() {
  try {
    console.log("Fetching users from Firestore...");

    const usersRef = collection(db, "users");
    const snapshot = await getDocs(usersRef);

    console.log("Snapshot size:", snapshot.size);

    const users = [];

    snapshot.forEach(doc => {
      console.log("User doc:", doc.id, doc.data());
      users.push({ id: doc.id, ...doc.data() });
    });

    return { success: true, data: users };

  } catch (error) {
    console.error("Firestore error:", error);
    return { success: false, message: error.message };
  }
}

export async function getUserById(userId) {
  try {
    const userRef = doc(db, "users", userId);
    const snapshot = await getDoc(userRef);
    if (snapshot.exists()) {
      return { success: true, data: { id: snapshot.id, ...snapshot.data() } };
    }
    return { success: false, message: "User not found" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function updateUser(userId, userData) {
  try {
    const userRef = doc(db, "users", userId);
    await updateDoc(userRef, userData);
    return { success: true, message: "User updated successfully" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function deleteUser(userId) {
  try {
    const userRef = doc(db, "users", userId);
    await deleteDoc(userRef);
    return { success: true, message: "User deleted successfully" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

// ==================== MOVIE OPERATIONS ====================
export async function addMovie(movieData) {
  try {
    const counterRef = doc(db, "counters", "movies");
    const counterSnap = await getDoc(counterRef);

    let current = 0;

    if (counterSnap.exists()) {
      current = parseInt(counterSnap.data().current) || 0;
    }

    const nextNum = current + 1;
    const movieId = `m${nextNum}`;

    const newMovieData = {
      movieId: movieId,
      ...movieData
    };

    const movieRef = doc(db, "movies", movieId);

    await setDoc(movieRef, newMovieData);

    if (counterSnap.exists()) {
      await updateDoc(counterRef, {
        current: nextNum
      });
    } else {
      await setDoc(counterRef, {
        current: nextNum
      });
    }

    return {
      success: true,
      id: movieId,
      movieId: movieId,
      message: "Movie added successfully"
    };

  } catch (error) {
    return {
      success: false,
      message: error.message
    };
  }
}

export async function getAllMovies() {
  try {
    const moviesRef = collection(db, "movies");
    const snapshot = await getDocs(moviesRef);
    const movies = [];
    snapshot.forEach(doc => {
      movies.push({ id: doc.id, ...doc.data() });
    });
    return { success: true, data: movies };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function getMovieById(movieId) {
  try {
    const movieRef = doc(db, "movies", movieId);
    const snapshot = await getDoc(movieRef);
    if (snapshot.exists()) {
      return { success: true, data: { id: snapshot.id, ...snapshot.data() } };
    }
    return { success: false, message: "Movie not found" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function updateMovie(movieId, movieData) {
  try {
    const movieRef = doc(db, "movies", movieId);
    await updateDoc(movieRef, movieData);
    return { success: true, message: "Movie updated successfully" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function deleteMovie(movieId) {
  try {
    const movieRef = doc(db, "movies", movieId);
    await deleteDoc(movieRef);
    return { success: true, message: "Movie deleted successfully" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

// ==================== ACTOR OPERATIONS ====================
export async function addActor(actorData) {
  try {
    const counterRef = doc(db, "counters", "actors");
    const counterSnap = await getDoc(counterRef);

    let current = 0;

    if (counterSnap.exists()) {
      current = parseInt(counterSnap.data().current) || 0;
    }

    const nextNum = Number(current) + 1;
    const actorId = `a${nextNum}`;

    const newActorData = {
      actorId: actorId,
      ...actorData
    };

    const actorRef = doc(db, "actors", actorId);
    await setDoc(actorRef, newActorData);

    if (counterSnap.exists()) {
      await updateDoc(counterRef, {
        current: nextNum
      });
    } else {
      await setDoc(counterRef, {
        current: nextNum
      });
    }

    return {
      success: true,
      id: actorId,
      actorId: actorId,
      message: "Actor added successfully"
    };

  } catch (error) {
    return {
      success: false,
      message: error.message
    };
  }
}

export async function getAllActors() {
  try {
    const actorsRef = collection(db, "actors");
    const snapshot = await getDocs(actorsRef);
    const actors = [];
    snapshot.forEach(doc => {
      actors.push({ id: doc.id, ...doc.data() });
    });
    return { success: true, data: actors };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function getActorById(actorId) {
  try {
    const actorRef = doc(db, "actors", actorId);
    const snapshot = await getDoc(actorRef);
    if (snapshot.exists()) {
      return { success: true, data: { id: snapshot.id, ...snapshot.data() } };
    }
    return { success: false, message: "Actor not found" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function updateActor(actorId, actorData) {
  try {
    const actorRef = doc(db, "actors", actorId);
    await updateDoc(actorRef, actorData);
    return { success: true, message: "Actor updated successfully" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function deleteActor(actorId) {
  try {
    const actorRef = doc(db, "actors", actorId);
    await deleteDoc(actorRef);
    return { success: true, message: "Actor deleted successfully" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

// ==================== DIRECTOR OPERATIONS ====================
export async function addDirector(directorData) {
  try {
    const counterRef = doc(db, "counters", "directors");
    const counterSnap = await getDoc(counterRef);

    let current = 0;

    if (counterSnap.exists()) {
      current = parseInt(counterSnap.data().current) || 0;
    }

    const nextNum = Number(current) + 1;
    const directorId = `d${nextNum}`;

    const newDirectorData = {
      directorId: directorId,
      ...directorData
    };

    const directorRef = doc(db, "directors", directorId);
    await setDoc(directorRef, newDirectorData);

    if (counterSnap.exists()) {
      await updateDoc(counterRef, {
        current: nextNum
      });
    } else {
      await setDoc(counterRef, {
        current: nextNum
      });
    }

    return {
      success: true,
      id: directorId,
      directorId: directorId,
      message: "Director added successfully"
    };

  } catch (error) {
    return {
      success: false,
      message: error.message
    };
  }
}

export async function getAllDirectors() {
  try {
    const directorsRef = collection(db, "directors");
    const snapshot = await getDocs(directorsRef);
    const directors = [];
    snapshot.forEach(doc => {
      directors.push({ id: doc.id, ...doc.data() });
    });
    return { success: true, data: directors };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function getDirectorById(directorId) {
  try {
    const directorRef = doc(db, "directors", directorId);
    const snapshot = await getDoc(directorRef);
    if (snapshot.exists()) {
      return { success: true, data: { id: snapshot.id, ...snapshot.data() } };
    }
    return { success: false, message: "Director not found" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function updateDirector(directorId, directorData) {
  try {
    const directorRef = doc(db, "directors", directorId);
    await updateDoc(directorRef, directorData);
    return { success: true, message: "Director updated successfully" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function deleteDirector(directorId) {
  try {
    const directorRef = doc(db, "directors", directorId);
    await deleteDoc(directorRef);
    return { success: true, message: "Director deleted successfully" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

// ==================== GENRE OPERATIONS ====================
export async function addGenre(genreData) {
  try {
    const genresRef = collection(db, "genres");
    const docRef = await addDoc(genresRef, genreData);
    return { success: true, id: docRef.id, message: "Genre added successfully" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function getAllGenres() {
  try {
    const genresRef = collection(db, "genres");
    const snapshot = await getDocs(genresRef);
    const genres = [];
    snapshot.forEach(doc => {
      genres.push({ id: doc.id, ...doc.data() });
    });
    return { success: true, data: genres };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function getGenreById(genreId) {
  try {
    const genreRef = doc(db, "genres", genreId);
    const snapshot = await getDoc(genreRef);
    if (snapshot.exists()) {
      return { success: true, data: { id: snapshot.id, ...snapshot.data() } };
    }
    return { success: false, message: "Genre not found" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function updateGenre(genreId, genreData) {
  try {
    const genreRef = doc(db, "genres", genreId);
    await updateDoc(genreRef, genreData);
    return { success: true, message: "Genre updated successfully" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function deleteGenre(genreId) {
  try {
    const genreRef = doc(db, "genres", genreId);
    await deleteDoc(genreRef);
    return { success: true, message: "Genre deleted successfully" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

// ==================== DVD OPERATIONS ====================
export async function addDVD(dvdData) {
  try {
    // Generate custom dvdId if not provided
    let dvdId = dvdData.dvdId;
    if (!dvdId) {
      const dvdsRef = collection(db, "dvds");
      const snapshot = await getDocs(dvdsRef);
      let maxNum = 0;
      snapshot.forEach(doc => {
        const data = doc.data();
        if (data.dvdId && data.dvdId.startsWith('dvd')) {
          const num = parseInt(data.dvdId.substring(3));
          if (!isNaN(num) && num > maxNum) {
            maxNum = num;
          }
        }
      });
      const nextNum = maxNum + 1;
      dvdId = `dvd${nextNum}`;
    }

    const newDVDData = {
      dvdId: dvdId,
      ...dvdData
    };

    // Use setDoc with custom ID instead of addDoc
    const docRef = doc(db, "dvds", dvdId);
    await setDoc(docRef, newDVDData);
    return { success: true, id: dvdId, dvdId: dvdId, message: "DVD added successfully" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function getAllDVDs() {
  try {
    const dvdsRef = collection(db, "dvds");
    const snapshot = await getDocs(dvdsRef);
    const dvds = [];
    snapshot.forEach(doc => {
      dvds.push({ id: doc.id, ...doc.data() });
    });
    return { success: true, data: dvds };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function getDVDById(dvdId) {
  try {
    const dvdRef = doc(db, "dvds", dvdId);
    const snapshot = await getDoc(dvdRef);
    if (snapshot.exists()) {
      return { success: true, data: { id: snapshot.id, ...snapshot.data() } };
    }
    return { success: false, message: "DVD not found" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function updateDVD(dvdId, dvdData) {
  try {
    const dvdRef = doc(db, "dvds", dvdId);
    await updateDoc(dvdRef, dvdData);
    return { success: true, message: "DVD updated successfully" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function deleteDVD(dvdId) {
  try {
    const dvdRef = doc(db, "dvds", dvdId);
    await deleteDoc(dvdRef);
    return { success: true, message: "DVD deleted successfully" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

// ==================== MPAA RATING OPERATIONS ====================


export async function getAllMpaaRatings() {
  try {
    const ratingsRef = collection(db, "mpaa_ratings");
    const snapshot = await getDocs(ratingsRef);
    const ratings = [];
    snapshot.forEach(doc => {
      ratings.push({ id: doc.id, ...doc.data() });
    });
    return { success: true, data: ratings };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

// ==================== STATUS OPERATIONS ====================
export async function addStatus(statusData) {
  try {
    const statusRef = collection(db, "status");
    const docRef = await addDoc(statusRef, statusData);
    return { success: true, id: docRef.id, message: "Status added successfully" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function getAllStatus() {
  try {
    const statusRef = collection(db, "status");
    const snapshot = await getDocs(statusRef);
    const statuses = [];
    snapshot.forEach(doc => {
      statuses.push({ id: doc.id, ...doc.data() });
    });
    return { success: true, data: statuses };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

// ==================== PLOT KEYWORD OPERATIONS ====================
export async function addPlotKeyword(keywordData) {
  try {
    const keywordsRef = collection(db, "plotKeywords");
    const docRef = await addDoc(keywordsRef, keywordData);
    return { success: true, id: docRef.id, message: "Plot Keyword added successfully" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function getAllPlotKeywords() {
  try {
    const keywordsRef = collection(db, "plot_keywords");
    const snapshot = await getDocs(keywordsRef);
    const keywords = [];
    snapshot.forEach(doc => {
      keywords.push({ id: doc.id, ...doc.data() });
    });
    return { success: true, data: keywords };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

// ==================== ADMIN OPERATIONS ====================
export async function verifyAdmin(username, password) {
  try {
    const adminsRef = collection(db, "admin");
    const q = query(adminsRef, where("username", "==", username), where("password", "==", password));
    const snapshot = await getDocs(q);
    
    if (snapshot.empty) {
      return { success: false, message: "Invalid username or password" };
    }

    const adminDoc = snapshot.docs[0];
    return { 
      success: true, 
      message: "Login successful", 
      data: { 
        id: adminDoc.id, 
        ...adminDoc.data() 
      } 
    };
  } catch (error) {
    console.error("Admin verification error:", error);
    return { success: false, message: error.message };
  }
}

// ==================== ORDERS OPERATIONS ====================
export async function getAllOrders() {
  try {
    const ordersRef = collection(db, "orders");
    const snapshot = await getDocs(ordersRef);
    const orders = [];
    snapshot.forEach(doc => {
      orders.push({ id: doc.id, ...doc.data() });
    });
    return { success: true, data: orders };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function getOrderById(orderId) {
  try {
    const orderRef = doc(db, "orders", orderId);
    const snapshot = await getDoc(orderRef);
    if (snapshot.exists()) {
      return { success: true, data: { id: snapshot.id, ...snapshot.data() } };
    }
    return { success: false, message: "Order not found" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function addOrder(orderData) {
  try {
    const ordersRef = collection(db, "orders");
    const newOrder = {
      ...orderData,
      createdAt: serverTimestamp()
    };
    const docRef = await addDoc(ordersRef, newOrder);
    return { success: true, id: docRef.id, message: "Order added successfully" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

// ==================== DASHBOARD STATISTICS ====================
export async function getDashboardStats() {
  try {
    const usersRef = collection(db, "users");
    const moviesRef = collection(db, "movies");
    const ordersRef = collection(db, "orders");

    const usersSnapshot = await getDocs(usersRef);
    const moviesSnapshot = await getDocs(moviesRef);
    const ordersSnapshot = await getDocs(ordersRef);

    return {
      success: true,
      data: {
        totalUsers: usersSnapshot.size,
        totalMovies: moviesSnapshot.size,
        totalOrders: ordersSnapshot.size
      }
    };
  } catch (error) {
    return { success: false, message: error.message };
  }
}
