// Global state
let currentUser = null;
let currentSection = 'browse';

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    checkUserSession();
    document.getElementById('createRestaurantForm').addEventListener('submit', handleCreateRestaurant);
    loadRestaurants();
});

// Check user session
function checkUserSession() {
    const urlParams = new URLSearchParams(window.location.search);
    const userId = urlParams.get('userId') || localStorage.getItem('userId');
    const role = urlParams.get('role') || localStorage.getItem('role');
    const username = urlParams.get('username') || localStorage.getItem('username');
    const firstName = urlParams.get('firstName') || localStorage.getItem('firstName');
    const lastName = urlParams.get('lastName') || localStorage.getItem('lastName');

    if (!userId || !role) {
        window.location.href = '/index.html';
        return;
    }

    currentUser = {
        id: userId,
        role: role,
        username: username,
        firstName: firstName,
        lastName: lastName
    };

    localStorage.setItem('userId', userId);
    localStorage.setItem('role', role);
    localStorage.setItem('username', username);
    localStorage.setItem('firstName', firstName);
    localStorage.setItem('lastName', lastName);

    updateUIForRole(role);
    const userInfo = document.getElementById('user-info');
    userInfo.textContent = `${firstName} ${lastName} (${role})`;
}

function updateUIForRole(role) {
    if (role === 'owner') {
        document.getElementById('menu-myRestaurants').style.display = 'block';
        document.getElementById('menu-createRestaurant').style.display = 'block';
    } else if (role === 'critic') {
        document.getElementById('menu-myReviews').style.display = 'block';
    } else if (role === 'admin') {
        document.getElementById('menu-admin').style.display = 'block';
    }
}

function showSection(sectionName) {
    // 1. Κρύβουμε όλα τα sections
    document.querySelectorAll('.section').forEach(section => {
        section.classList.remove('active');
    });

    // 2. Αφαιρούμε το active από όλα τα κουμπιά του μενού
    document.querySelectorAll('.menu-item').forEach(item => {
        item.classList.remove('active');
    });

    // 3. Εμφανίζουμε το σωστό section
    const sectionElement = document.getElementById(`${sectionName}-section`);
    if (sectionElement) {
        sectionElement.classList.add('active');
    }

    // 4. Κάνουμε highlight το σωστό κουμπί με βάση το ID
    const activeMenuItem = document.getElementById(`menu-${sectionName}`);
    if (activeMenuItem) {
        activeMenuItem.classList.add('active');
    }

    currentSection = sectionName;

    // 5. Φορτώνουμε τα δεδομένα αν χρειάζεται
    if (sectionName === 'myRestaurants') {
        loadMyRestaurants();
    } else if (sectionName === 'myReviews') {
        loadMyReviews();
    } else if (sectionName === 'admin') {
        loadAdminData();
    }
}

function loadRestaurants(searchTerm = '') {
    const url = searchTerm ? `/api/restaurants?search=${encodeURIComponent(searchTerm)}` : '/api/restaurants';
    fetch(url)
        .then(response => response.json())
        .then(data => {
            renderRestaurantsList(data, document.getElementById('restaurants-list'));
        })
        .catch(error => {
            console.error('Error loading restaurants:', error);
            document.getElementById('restaurants-list').innerHTML = '<p>Σφάλμα κατά την φόρτωση</p>';
        });
}

function renderRestaurantsList(restaurants, container) {
    if (!restaurants || restaurants.length === 0) {
        container.innerHTML = '<p>Δεν βρέθηκαν εστιατόρια</p>';
        return;
    }

    container.innerHTML = restaurants.map(restaurant => `
        <div class="restaurant-card">
            <h3>${restaurant.name}</h3>
            <div class="location">📍 ${restaurant.location}</div>
            <div class="rating">
                <span class="stars">${generateStars(restaurant.averageRating)}</span>
                <span>${restaurant.averageRating.toFixed(1)}/5.0</span>
                <span style="color: #999; font-size: 0.9rem;">(${restaurant.reviewCount} κριτικές)</span>
            </div>
            ${restaurant.cuisineType ? `<span class="cuisine">${restaurant.cuisineType}</span>` : ''}
            <div class="actions">
                <button class="btn-small btn-view" onclick="showRestaurantDetail(${restaurant.id})">Προβολή</button>
                ${currentUser.role === 'owner' && currentUser.id == restaurant.ownerId ? `<button class="btn-small btn-edit" onclick="editRestaurant(${restaurant.id})">Επεξ.</button>` : ''}
                ${currentUser.role === 'owner' && currentUser.id == restaurant.ownerId ? `<button class="btn-small btn-delete" onclick="deleteRestaurant(${restaurant.id})">Διαγ.</button>` : ''}
            </div>
        </div>
    `).join('');
}

function searchRestaurants() {
    const searchTerm = document.getElementById('searchInput').value;
    loadRestaurants(searchTerm);
}

function showRestaurantDetail(restaurantId) {
    fetch(`/api/restaurants/${restaurantId}`)
        .then(response => response.json())
        .then(restaurant => {
            fetch(`/api/reviews/restaurant/${restaurantId}`)
                .then(response => response.json())
                .then(reviews => {
                    displayRestaurantModal(restaurant, reviews);
                });
        })
        .catch(error => {
            console.error('Error loading restaurant:', error);
            alert('Σφάλμα κατά την φόρτωση του εστιατορίου');
        });
}

function displayRestaurantModal(restaurant, reviews) {
    const modal = document.getElementById('restaurantModal');
    const modalBody = document.getElementById('modalBody');

    let reviewsHTML = reviews.length > 0 ? reviews.map(review => `
        <div class="review-item">
            <div class="review-header">
                <span class="reviewer-name">${review.criticName}</span>
                <span class="review-rating">${generateStars(review.rating)}</span>
            </div>
            <div class="review-date">${new Date(review.createdDate).toLocaleDateString('el-GR')}</div>
            ${review.reviewText ? `<p>${review.reviewText}</p>` : ''}
        </div>
    `).join('') : '<p>Δεν υπάρχουν κριτικές ακόμα</p>';

    let reviewFormHTML = currentUser.role === 'critic' ? `
        <div class="review-form">
            <h4>Προσθήκη Κριτικής</h4>
            <div class="rating-input" id="ratingInput"></div>
            <textarea id="reviewText" placeholder="Η κριτική σας..." style="width: 100%; padding: 0.75rem; border-radius: 4px; border: 1px solid #e0e0e0;"></textarea>
            <button class="btn-submit" style="margin-top: 1rem;" onclick="submitReview(${restaurant.id})">Υποβολή Κριτικής</button>
        </div>
    ` : '';

    modalBody.innerHTML = `
        <div class="restaurant-detail">
            <h2>${restaurant.name}</h2>
            <div class="detail-info">
                <div class="info-item"><div class="info-label">Τοποθεσία</div><div class="info-value">${restaurant.location}</div></div>
                <div class="info-item"><div class="info-label">Είδος Κουζίνας</div><div class="info-value">${restaurant.cuisineType || 'N/A'}</div></div>
                <div class="info-item"><div class="info-label">Τηλέφωνο</div><div class="info-value">${restaurant.phoneNumber || 'N/A'}</div></div>
                <div class="info-item"><div class="info-label">Αξιολόγηση</div><div class="info-value">${generateStars(restaurant.averageRating)} ${restaurant.averageRating.toFixed(1)}/5.0</div></div>
            </div>
            ${restaurant.description ? `<p><strong>Περιγραφή:</strong> ${restaurant.description}</p>` : ''}
        </div>
        <div class="reviews-section">
            <h3>Κριτικές (${reviews.length})</h3>
            ${reviewsHTML}
            ${reviewFormHTML}
        </div>
    `;

    if (currentUser.role === 'critic') {
        setTimeout(() => setupRatingInput(), 100);
    }

    modal.style.display = 'block';
}

function setupRatingInput() {
    const ratingInput = document.getElementById('ratingInput');
    if (!ratingInput) return;
    ratingInput.innerHTML = '';
    for (let i = 1; i <= 5; i++) {
        const btn = document.createElement('button');
        btn.className = 'star-btn';
        btn.textContent = '★';
        btn.onclick = (e) => {
            e.preventDefault();
            document.querySelectorAll('.star-btn').forEach(b => b.classList.remove('selected'));
            for (let j = 0; j < i; j++) {
                document.querySelectorAll('.star-btn')[j].classList.add('selected');
            }
            btn.dataset.selectedRating = i;
        };
        ratingInput.appendChild(btn);
    }
}

function submitReview(restaurantId) {
    const selectedRating = document.querySelectorAll('.star-btn.selected').length;
    if (selectedRating === 0) {
        alert('Παρακαλώ επιλέξτε αξιολόγηση');
        return;
    }
    const reviewText = document.getElementById('reviewText').value;
    fetch('/api/reviews/create', {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: `restaurantId=${restaurantId}&criticId=${currentUser.id}&rating=${selectedRating}&reviewText=${encodeURIComponent(reviewText)}`
    })
        .then(response => response.json())
        .then(data => {
            if (data.id) {
                alert('Κριτική υποβλήθηκε με επιτυχία!');
                closeModal();
                showRestaurantDetail(restaurantId);
            } else {
                alert('Σφάλμα: ' + (data.error || 'Δεν ήταν δυνατή η υποβολή'));
            }
        })
        .catch(error => {
            console.error('Error submitting review:', error);
            alert('Σφάλμα κατά την υποβολή της κριτικής');
        });
}

function closeModal() {
    document.getElementById('restaurantModal').style.display = 'none';
}

function loadMyRestaurants() {
    fetch(`/api/restaurants/owner/${currentUser.id}`)
        .then(response => response.json())
        .then(data => {
            renderRestaurantsList(data, document.getElementById('my-restaurants-list'));
        })
        .catch(error => {
            console.error('Error loading restaurants:', error);
            document.getElementById('my-restaurants-list').innerHTML = '<p>Σφάλμα κατά την φόρτωση</p>';
        });
}

function editRestaurant(restaurantId) {
    alert('Δυνατότητα επεξεργασίας - Σχεδιάζεται για μελλοντική εκδόχη');
}

function deleteRestaurant(restaurantId) {
    if (confirm('Είστε σίγουροι ότι θέλετε να διαγράψετε αυτό το εστιατόριο;')) {
        fetch(`/api/restaurants/${restaurantId}?ownerId=${currentUser.id}`, {method: 'DELETE'})
            .then(response => response.json())
            .then(data => {
                if (data.message) {
                    alert('Εστιατόριο διαγράφηκε με επιτυχία');
                    loadMyRestaurants();
                } else {
                    alert('Σφάλμα: ' + (data.error || 'Δεν ήταν δυνατή η διαγραφή'));
                }
            })
            .catch(error => {
                console.error('Error deleting restaurant:', error);
                alert('Σφάλμα κατά τη διαγραφή');
            });
    }
}

function handleCreateRestaurant(e) {
    e.preventDefault();
    const name = document.getElementById('restName').value;
    const location = document.getElementById('restLocation').value;
    const description = document.getElementById('restDescription').value;
    const cuisine = document.getElementById('restCuisine').value;
    const phone = document.getElementById('restPhone').value;

    const params = new URLSearchParams({name, location, description, cuisineType: cuisine, phoneNumber: phone, ownerId: currentUser.id});

    fetch('/api/restaurants/create', {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: params
    })
        .then(response => response.json())
        .then(data => {
            if (data.id) {
                alert('Εστιατόριο δημιουργήθηκε με επιτυχία!');
                document.getElementById('createRestaurantForm').reset();
                showSection('myRestaurants');
            } else {
                alert('Σφάλμα: ' + (data.error || 'Δεν ήταν δυνατή η δημιουργία'));
            }
        })
        .catch(error => {
            console.error('Error creating restaurant:', error);
            alert('Σφάλμα κατά τη δημιουργία του εστιατορίου');
        });
}

function loadMyReviews() {
    fetch(`/api/reviews/critic/${currentUser.id}`)
        .then(response => response.json())
        .then(reviews => {
            const container = document.getElementById('my-reviews-list');
            if (!reviews || reviews.length === 0) {
                container.innerHTML = '<p>Δεν έχετε υποβάλει κριτικές ακόμα</p>';
                return;
            }
            container.innerHTML = reviews.map(review => `
                <div class="review-item" style="margin-bottom: 1rem;">
                    <h4>${review.restaurantName}</h4>
                    <div class="review-header">
                        <span class="review-rating">${generateStars(review.rating)}</span>
                        <span style="color: #999; font-size: 0.9rem;">${new Date(review.createdDate).toLocaleDateString('el-GR')}</span>
                    </div>
                    ${review.reviewText ? `<p>${review.reviewText}</p>` : ''}
                </div>
            `).join('');
        })
        .catch(error => {
            console.error('Error loading reviews:', error);
            document.getElementById('my-reviews-list').innerHTML = '<p>Σφάλμα κατά την φόρτωση</p>';
        });
}

function loadAdminData() {
    loadAdminUsers();
    loadAdminRestaurants();
    loadAdminReviews();
}

function loadAdminUsers() {
    fetch('/api/users')
        .then(response => response.json())
        .then(users => {
            const tbody = document.getElementById('admin-users-list');
            tbody.innerHTML = users.map(user => `<tr><td>${user.id}</td><td>${user.firstName} ${user.lastName}</td><td>${user.username}</td><td>${user.type || 'User'}</td></tr>`).join('');
        })
        .catch(() => {
            document.getElementById('admin-users-list').innerHTML = '<tr><td colspan="4">Σφάλμα</td></tr>';
        });
}

function loadAdminRestaurants() {
    fetch('/api/restaurants')
        .then(response => response.json())
        .then(restaurants => {
            const tbody = document.getElementById('admin-restaurants-list');
            tbody.innerHTML = restaurants.map(r => `<tr><td>${r.id}</td><td>${r.name}</td><td>${r.location}</td><td>${r.averageRating.toFixed(1)}/5.0</td><td>${r.reviewCount}</td><td><button class="btn-small btn-delete" onclick="adminDeleteRestaurant(${r.id})">Διαγ.</button></td></tr>`).join('');
        })
        .catch(() => {
            document.getElementById('admin-restaurants-list').innerHTML = '<tr><td colspan="6">Σφάλμα</td></tr>';
        });
}

function loadAdminReviews() {
    fetch('/api/restaurants')
        .then(response => response.json())
        .then(restaurants => {
            const allReviews = [];
            let loaded = 0;
            if (restaurants.length === 0) {
                document.getElementById('admin-reviews-list').innerHTML = '<tr><td colspan="6">Καμία κριτική</td></tr>';
                return;
            }
            restaurants.forEach(restaurant => {
                fetch(`/api/reviews/restaurant/${restaurant.id}`)
                    .then(response => response.json())
                    .then(reviews => {
                        allReviews.push(...reviews);
                        loaded++;
                        if (loaded === restaurants.length) {
                            displayAdminReviews(allReviews);
                        }
                    });
            });
        })
        .catch(() => {
            document.getElementById('admin-reviews-list').innerHTML = '<tr><td colspan="6">Σφάλμα</td></tr>';
        });
}

function displayAdminReviews(reviews) {
    const tbody = document.getElementById('admin-reviews-list');
    if (reviews.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6">Δεν υπάρχουν κριτικές</td></tr>';
        return;
    }
    tbody.innerHTML = reviews.map(r => `<tr><td>${r.id}</td><td>${r.restaurantName}</td><td>${r.criticName}</td><td>${r.rating}/5</td><td>${new Date(r.createdDate).toLocaleDateString('el-GR')}</td><td><button class="btn-small btn-delete" onclick="adminDeleteReview(${r.id})">Διαγ.</button></td></tr>`).join('');
}

function adminDeleteRestaurant(restaurantId) {
    if (confirm('Είστε σίγουροι;')) {
        fetch(`/api/restaurants/${restaurantId}?ownerId=0`, {method: 'DELETE'})
            .then(() => loadAdminRestaurants())
            .catch(error => console.error('Error:', error));
    }
}

function adminDeleteReview(reviewId) {
    if (confirm('Είστε σίγουροι;')) {
        fetch(`/api/reviews/${reviewId}?userId=${currentUser.id}&userRole=admin`, {method: 'DELETE'})
            .then(() => setTimeout(() => loadAdminReviews(), 500))
            .catch(error => console.error('Error:', error));
    }
}

function showAdminTab(tabName) {
    document.querySelectorAll('.admin-tab-content').forEach(tab => tab.classList.remove('active'));
    document.querySelectorAll('.admin-tab-btn').forEach(btn => btn.classList.remove('active'));

    const tabContent = document.getElementById(`admin-${tabName}`);
    if (tabContent) tabContent.classList.add('active');

    const tabBtn = document.getElementById(`tab-btn-${tabName}`);
    if (tabBtn) tabBtn.classList.add('active');
}

function logout() {
    localStorage.clear();
    window.location.href = '/index.html';
}

function generateStars(rating) {
    if (!rating) return '☆☆☆☆☆';
    const fullStars = Math.floor(rating);
    const hasHalfStar = rating % 1 >= 0.5;
    let stars = '';
    for (let i = 0; i < fullStars; i++) stars += '★';
    if (hasHalfStar && fullStars < 5) stars += '★';
    for (let i = stars.length; i < 5; i++) stars += '☆';
    return stars;
}

window.onclick = function(event) {
    const modal = document.getElementById('restaurantModal');
    if (event.target === modal) {
        modal.style.display = 'none';
    }
}