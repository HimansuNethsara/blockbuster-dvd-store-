import { getDashboardStats, getAllUsers, getAllMovies, getAllOrders } from '../firebaseUtils.js';

async function adminStatisticsFunc() {
    checkAdminSession();
    await loadUserStats();
}

async function loadUserStats() {
    try {
        const result = await getDashboardStats();

        if (!result.success) {
            console.error("Failed to load stats:", result.message);
            return;
        }

        const stats = result.data;
        const totalUsers = stats.totalUsers;
        const totalMovies = stats.totalMovies;

        // Update user counts
        document.getElementById("total-user-count").textContent = totalUsers;
        document.getElementById("new-user-count").textContent = Math.ceil(totalUsers * 0.1);

        // Get movies and count this year's releases
        const moviesResult = await getAllMovies();
        let thisYearReleases = 0;
        if (moviesResult.success) {
            const currentYear = new Date().getFullYear();
            thisYearReleases = moviesResult.data.filter(movie => {
                return movie.releaseYear === currentYear;
            }).length;
        }
        
        document.getElementById("total-movie-count").textContent = totalMovies;
        document.getElementById("this-year-releases").textContent = thisYearReleases;

        // Get orders data for DVD sales and revenue
        const ordersResult = await getAllOrders();
        let totalDVDSales = 0;
        let dailyDVDSales = 0;
        let totalRevenue = 0;
        let dailyRevenue = 0;
        const userOrderCounts = {};

        if (ordersResult.success && ordersResult.data && Array.isArray(ordersResult.data) && ordersResult.data.length > 0) {
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            
            ordersResult.data.forEach((order) => {
                // Get quantity from orderItems array (sum of all items)
                let quantity = 0;
                if (order.orderItems && Array.isArray(order.orderItems)) {
                    quantity = order.orderItems.reduce((sum, item) => sum + (parseInt(item.quantity) || 0), 0);
                }
                
                // Get total price from totalAmount
                let price = parseFloat(order.totalAmount) || 0;
                
                if (quantity > 0) {
                    totalDVDSales += quantity;
                }
                
                if (price > 0) {
                    totalRevenue += price;
                }
                
                // Parse orderDate (Firestore Timestamp format: {seconds, nanoseconds})
                let orderDate = null;
                if (order.orderDate) {
                    if (typeof order.orderDate.toDate === 'function') {
                        // Firestore Timestamp object
                        orderDate = order.orderDate.toDate();
                    } else if (order.orderDate.seconds !== undefined) {
                        // Timestamp with seconds and nanoseconds
                        orderDate = new Date(order.orderDate.seconds * 1000);
                    } else if (order.orderDate instanceof Date) {
                        orderDate = order.orderDate;
                    } else if (typeof order.orderDate === 'number') {
                        orderDate = new Date(order.orderDate);
                    }
                }
                
                if (orderDate) {
                    const orderDateOnly = new Date(orderDate);
                    orderDateOnly.setHours(0, 0, 0, 0);
                    if (orderDateOnly.getTime() === today.getTime()) {
                        if (quantity > 0) {
                            dailyDVDSales += quantity;
                        }
                        if (price > 0) {
                            dailyRevenue += price;
                        }
                    }
                }
                
                // Count DVDs per user for contributions
                const userId = order.userId || order.user_id || order.uid;
                if (userId && quantity > 0) {
                    userOrderCounts[userId] = (userOrderCounts[userId] || 0) + quantity;
                }
            });
        }

        // Update DVD sales displays
        document.getElementById("total-booking-count").textContent = totalDVDSales;
        document.getElementById("new-booking-count").textContent = dailyDVDSales;

        // Update revenue displays
        document.getElementById("total-revenue-count").textContent = 'Rs.' + totalRevenue.toFixed(2);
        document.getElementById("daily-revenue-count").textContent = 'Rs.' + dailyRevenue.toFixed(2);

        // Calculate progress values
        const totalUserGoal = 1000;
        const totalUserProgress = Math.min((totalUsers / totalUserGoal) * 100, 100);

        const totalUserBar = document.getElementById("total-user-bar");
        if (totalUserBar) {
            totalUserBar.style.width = totalUserProgress + "%";
            totalUserBar.setAttribute("aria-valuenow", totalUserProgress.toFixed(0));
        }

        const newUserBar = document.getElementById("new-user-bar");
        if (newUserBar) {
            const newUserProgress = totalUsers > 0 ? 10 : 0;
            newUserBar.style.width = newUserProgress + "%";
            newUserBar.setAttribute("aria-valuenow", newUserProgress.toFixed(0));
        }

        const thisYearGoal = 100;
        const thisYearProgress = Math.min((thisYearReleases / thisYearGoal) * 100, 100);
        const thisYearBar = document.getElementById("this-year-bar");
        if (thisYearBar) {
            thisYearBar.style.width = thisYearProgress + "%";
            thisYearBar.setAttribute("aria-valuenow", thisYearProgress.toFixed(0));
        }

        const totalDVDGoal = 1000;
        const totalDVDProgress = Math.min((totalDVDSales / totalDVDGoal) * 100, 100);
        const totalBookingBar = document.getElementById("total-booking-bar");
        if (totalBookingBar) {
            totalBookingBar.style.width = totalDVDProgress + "%";
            totalBookingBar.setAttribute("aria-valuenow", totalDVDProgress.toFixed(0));
        }

        const newDVDGoal = 100;
        const newDVDProgress = Math.min((dailyDVDSales / newDVDGoal) * 100, 100);
        const newBookingBar = document.getElementById("new-booking-bar");
        if (newBookingBar) {
            newBookingBar.style.width = newDVDProgress + "%";
            newBookingBar.setAttribute("aria-valuenow", newDVDProgress.toFixed(0));
        }

        const totalMovieGoal = 1000;
        const totalMovieProgress = Math.min((totalMovies / totalMovieGoal) * 100, 100);
        const totalMovieBar = document.getElementById("total-movie-bar");
        if (totalMovieBar) {
            totalMovieBar.style.width = totalMovieProgress + "%";
            totalMovieBar.setAttribute("aria-valuenow", totalMovieProgress.toFixed(0));
        }

        const dailyRevenueGoal = 5000;
        const dailyRevenueProgress = Math.min((dailyRevenue / dailyRevenueGoal) * 100, 100);
        const dailyRevenueBar = document.getElementById("daily-revenue-bar");
        if (dailyRevenueBar) {
            dailyRevenueBar.style.width = dailyRevenueProgress + "%";
            dailyRevenueBar.setAttribute("aria-valuenow", dailyRevenueProgress.toFixed(0));
        }

        const totalRevenueGoal = 50000;
        const totalRevenueProgress = Math.min((totalRevenue / totalRevenueGoal) * 100, 100);
        const totalRevenueBar = document.getElementById("total-revenue-bar");
        if (totalRevenueBar) {
            totalRevenueBar.style.width = totalRevenueProgress + "%";
            totalRevenueBar.setAttribute("aria-valuenow", totalRevenueProgress.toFixed(0));
        }

        // Load and display recent users
        const usersResult = await getAllUsers();
        if (usersResult.success) {
            function parseFirestoreDate(value) {
                if (!value) return new Date(0);

                if (typeof value.toDate === "function") {
                    return value.toDate();
                }

                if (value instanceof Date) {
                    return value;
                }

                if (typeof value === "number") {
                    return new Date(value);
                }

                if (value.seconds !== undefined) {
                    return new Date(value.seconds * 1000);
                }

                return new Date(0);
            }
            const users = usersResult.data
                .sort((a, b) => {
                    const dateA = parseFirestoreDate(a.registeredDate);
                    const dateB = parseFirestoreDate(b.registeredDate);
                    return dateB - dateA;
                })
                .slice(0, 3);

            users.forEach((user, index) => {
                const userNum = index + 1;
                const nameEl = document.getElementById(`user${userNum}-name`);
                const emailEl = document.getElementById(`user${userNum}-email`);
                const contributionsEl = document.getElementById(`user${userNum}-bookings`);
                
                if (nameEl) nameEl.textContent = (user.firstName || "N/A") + " " + (user.lastName || "");
                if (emailEl) emailEl.textContent = "@" + (user.email ? user.email.split("@")[0] : "N/A");
                
                // Show contributions based on DVDs purchased
                const userId = user.id;
                const dvdCount = userOrderCounts[userId] || 0;
                if (contributionsEl) {
                    contributionsEl.textContent = dvdCount + " Contribution" + (dvdCount === 1 ? "" : "s");
                }
            });
        }

    } catch (error) {
        console.error("Failed to load stats", error);
    }
}

// Export to global scope
window.adminStatistics = adminStatisticsFunc;