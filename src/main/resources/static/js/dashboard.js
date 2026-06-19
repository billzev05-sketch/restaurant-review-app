// Global state
let currentUser = null;
let currentSection = 'browse';

// Βοηθητική συνάρτηση για αποτροπή XSS (Cross-Site Scripting)
function escapeHTML(str) {
    if (str === null || str === undefined) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    checkUserSession();

    const createForm = document.getElementById('createRestaurantForm');
    if (createForm) {
        createForm.addEventListener('submit', handleCreateRestaurant);
    }

    const editForm = document.getElementById('editRestaurantForm');
    if (editForm) {
        editForm.addEventListener('submit', handleUpdateRestaurant);
    }
});

// Αποτροπή εμφάνισης cached σελίδας από το Back Button
window.addEventListener('pageshow', function(event) {
    // Αν η σελίδα φορτώθηκε από την cache (persisted) ή μέσω ιστορικού (navigation.type === 2)
    if (event.persisted || (window.performance && window.performance.navigation.type === 2)) {
        // Κάνε hard refresh για να διαλυθεί το στιγμιότυπο της οθόνης
        window.location.reload();
    }
});

function checkUserSession() {
    fetch('/api/session/user')
        .then(response => response.json())
        .then(data => {
            if (!data.loggedIn) {
                window.location.href = '/index.html?error=unauthorized';
                return;
            }

            currentUser = {
                id: data.userId,
                role: data.role,
                username: data.username,
                firstName: data.firstName,
                lastName: data.lastName
            };

            updateUIForRole(data.role);

            const userInfo = document.getElementById('user-info');
            if (userInfo) {
                // Ασφαλής εισαγωγή κειμένου, όχι innerHTML
                userInfo.textContent = `${data.firstName} ${data.lastName} (${data.role})`;
            }

            localStorage.removeItem('role');
            localStorage.removeItem('userId');

            loadRestaurants();
        })
        .catch(error => {
            console.error('Error checking session:', error);
            window.location.href = '/index.html';
        });
}

function updateUIForRole(role) {
    if (role === 'owner') {
        if(document.getElementById('menu-myRestaurants')) document.getElementById('menu-myRestaurants').style.display = 'block';
        if(document.getElementById('menu-createRestaurant')) document.getElementById('menu-createRestaurant').style.display = 'block';
    } else if (role === 'critic') {
        if(document.getElementById('menu-myReviews')) document.getElementById('menu-myReviews').style.display = 'block';
    } else if (role === 'admin') {
        if(document.getElementById('menu-admin')) document.getElementById('menu-admin').style.display = 'block';
    }
}

function showSection(sectionName) {
    document.querySelectorAll('.section').forEach(section => {
        section.classList.remove('active');
    });

    document.querySelectorAll('.menu-item').forEach(item => {
        item.classList.remove('active');
    });

    const sectionElement = document.getElementById(`${sectionName}-section`);
    if (sectionElement) {
        sectionElement.classList.add('active');
    }

    const activeMenuItem = document.getElementById(`menu-${sectionName}`);
    if (activeMenuItem) {
        activeMenuItem.classList.add('active');
    }

    currentSection = sectionName;

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
            const list = document.getElementById('restaurants-list');
            if (list) list.innerHTML = '<p>Σφάλμα κατά την φόρτωση</p>';
        });
}

function renderRestaurantsList(restaurants, container) {
    if (!container) return;

    if (!restaurants || restaurants.length === 0) {
        container.innerHTML = '<p>Δεν βρέθηκαν εστιατόρια</p>';
        return;
    }

    container.innerHTML = restaurants.map(restaurant => `
        <div class="restaurant-card">
            <h3>${escapeHTML(restaurant.name)}</h3>
            <div class="location">📍 ${escapeHTML(restaurant.location)}</div>
            <div class="rating">
                <span class="stars">${generateStars(restaurant.averageRating)}</span>
                <span>${restaurant.averageRating ? restaurant.averageRating.toFixed(1) : '0.0'}/5.0</span>
                <span style="color: #999; font-size: 0.9rem;">(${escapeHTML(restaurant.reviewCount) || 0} κριτικές)</span>
            </div>
            ${restaurant.cuisineType ? `<span class="cuisine">${escapeHTML(restaurant.cuisineType)}</span>` : ''}
            <div class="actions">
                <button class="btn-small btn-view" onclick="showRestaurantDetail(${restaurant.id})">Προβολή</button>
                ${currentUser && currentUser.role === 'owner' && currentUser.id == restaurant.ownerId ? `<button class="btn-small btn-edit" onclick="editRestaurant(${restaurant.id})">Επεξ.</button>` : ''}
                ${currentUser && currentUser.role === 'owner' && currentUser.id == restaurant.ownerId ? `<button class="btn-small btn-delete" onclick="deleteRestaurant(${restaurant.id})">🗑️</button>` : ''}
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
    if (!modal || !modalBody) return;

    let reviewsHTML = reviews.length > 0 ? reviews.map(review => `
        <div class="review-item">
            <div class="review-header">
                <span class="reviewer-name">${escapeHTML(review.criticName)}</span>
                <span class="review-rating">${generateStars(review.rating)}</span>
            </div>
            <div class="review-date">${new Date(review.createdDate).toLocaleDateString('el-GR')}</div>
            ${review.reviewText ? `<p>${escapeHTML(review.reviewText)}</p>` : ''}
        </div>
    `).join('') : '<p>Δεν υπάρχουν κριτικές ακόμα</p>';

    let reviewFormHTML = currentUser && currentUser.role === 'critic' ? `
        <div class="review-form">
            <h4>Προσθήκη Κριτικής</h4>
            <div class="rating-input" id="ratingInput"></div>
            <textarea id="reviewText" placeholder="Η κριτική σας..." style="width: 100%; padding: 0.75rem; border-radius: 4px; border: 1px solid #e0e0e0;"></textarea>
            <button class="btn-submit" style="margin-top: 1rem;" onclick="submitReview(${restaurant.id})">Υποβολή Κριτικής</button>
        </div>
    ` : '';

    modalBody.innerHTML = `
        <div class="restaurant-detail">
            <h2>${escapeHTML(restaurant.name)}</h2>
            <div class="detail-info">
                <div class="info-item"><div class="info-label">Τοποθεσία</div><div class="info-value">${escapeHTML(restaurant.location)}</div></div>
                <div class="info-item"><div class="info-label">Είδος Κουζίνας</div><div class="info-value">${escapeHTML(restaurant.cuisineType) || 'N/A'}</div></div>
                <div class="info-item"><div class="info-label">Τηλέφωνο</div><div class="info-value">${escapeHTML(restaurant.phoneNumber) || 'N/A'}</div></div>
                <div class="info-item"><div class="info-label">Αξιολόγηση</div><div class="info-value">${generateStars(restaurant.averageRating)} ${restaurant.averageRating ? restaurant.averageRating.toFixed(1) : '0.0'}/5.0</div></div>
            </div>
            ${restaurant.description ? `<p><strong>Περιγραφή:</strong> ${escapeHTML(restaurant.description)}</p>` : ''}
        </div>
        <div class="reviews-section">
            <h3>Κριτικές (${reviews.length})</h3>
            ${reviewsHTML}
            ${reviewFormHTML}
        </div>
    `;

    if (currentUser && currentUser.role === 'critic') {
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

    // Αλλαγή σε JSON format για συνέπεια
    const data = {
        restaurantId: restaurantId,
        rating: selectedRating,
        reviewText: reviewText
    };

    fetch('/api/reviews/create', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    })
        .then(response => response.json())
        .then(data => {
            if (data.id) {
                alert('Κριτική υποβλήθηκε με επιτυχία!');
                closeModal();
                showRestaurantDetail(restaurantId);
            } else {
                alert('Σφάλμα: ' + (escapeHTML(data.error) || 'Δεν ήταν δυνατή η υποβολή'));
            }
        })
        .catch(error => {
            console.error('Error submitting review:', error);
            alert('Σφάλμα κατά την υποβολή της κριτικής');
        });
}

function closeModal() {
    const modal = document.getElementById('restaurantModal');
    if (modal) modal.style.display = 'none';
}

function loadMyRestaurants() {
    fetch(`/api/restaurants/owner/${currentUser.id}`)
        .then(response => response.json())
        .then(data => {
            renderRestaurantsList(data, document.getElementById('my-restaurants-list'));
        })
        .catch(error => {
            console.error('Error loading restaurants:', error);
            const myList = document.getElementById('my-restaurants-list');
            if (myList) myList.innerHTML = '<p>Σφάλμα κατά την φόρτωση</p>';
        });
}

function editRestaurant(restaurantId) {
    fetch(`/api/restaurants/${restaurantId}`)
        .then(response => {
            if (!response.ok) throw new Error('Αποτυχία φόρτωσης στοιχείων εστιατορίου');
            return response.json();
        })
        .then(restaurant => {
            if (document.getElementById('editRestId')) document.getElementById('editRestId').value = restaurant.id;
            if (document.getElementById('editRestName')) document.getElementById('editRestName').value = restaurant.name || '';
            if (document.getElementById('editRestLocation')) document.getElementById('editRestLocation').value = restaurant.location || '';
            if (document.getElementById('editRestDescription')) document.getElementById('editRestDescription').value = restaurant.description || '';
            if (document.getElementById('editRestCuisine')) document.getElementById('editRestCuisine').value = restaurant.cuisineType || '';
            if (document.getElementById('editRestPhone')) document.getElementById('editRestPhone').value = restaurant.phoneNumber || '';

            showSection('editRestaurant');
        })
        .catch(error => {
            console.error('Error fetching restaurant details for edit:', error);
            alert('Σφάλμα κατά τη φόρτωση των στοιχείων του εστιατορίου.');
        });
}

function handleUpdateRestaurant(e) {
    e.preventDefault();

    const restaurantId = document.getElementById('editRestId').value;

    const data = {
        name: document.getElementById('editRestName').value,
        location: document.getElementById('editRestLocation').value,
        description: document.getElementById('editRestDescription').value,
        cuisineType: document.getElementById('editRestCuisine').value,
        phoneNumber: document.getElementById('editRestPhone').value
    };

    fetch(`/api/restaurants/${restaurantId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    })
        .then(response => {
            if (response.status === 403) {
                throw new Error('Δεν έχετε δικαίωμα να επεξεργαστείτε αυτό το εστιατόριο.');
            }
            if (!response.ok) {
                throw new Error('Κάτι πήγε λάθος κατά την ενημέρωση.');
            }
            return response.json();
        })
        .then(() => {
            alert('Το εστιατόριο ενημερώθηκε με επιτυχία!');
            showSection('myRestaurants');
        })
        .catch(error => {
            console.error('Error updating restaurant:', error);
            alert(error.message || 'Σφάλμα κατά την αποθήκευση των αλλαγών.');
        });
}

function deleteRestaurant(restaurantId) {
    if (confirm('Είστε σίγουροι ότι θέλετε να διαγράψετε αυτό το εστιατόριο;')) {
        // Αφαίρεση του query parameter ownerId - Ο server πρέπει να το βρει από το session
        fetch(`/api/restaurants/${restaurantId}`, { method: 'DELETE' })
            .then(response => {
                if(!response.ok) throw new Error('Η διαγραφή απέτυχε');
                return response.json();
            })
            .then(data => {
                alert('Εστιατόριο διαγράφηκε με επιτυχία');
                loadMyRestaurants();
            })
            .catch(error => {
                console.error('Error deleting restaurant:', error);
                alert('Σφάλμα κατά τη διαγραφή');
            });
    }
}

function handleCreateRestaurant(e) {
    e.preventDefault();

    const data = {
        name: document.getElementById('restName').value,
        location: document.getElementById('restLocation').value,
        description: document.getElementById('restDescription').value,
        cuisineType: document.getElementById('restCuisine').value,
        phoneNumber: document.getElementById('restPhone').value
    };

    fetch('/api/restaurants/create', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    })
        .then(response => response.json())
        .then(data => {
            if (data.id) {
                alert('Εστιατόριο δημιουργήθηκε με επιτυχία!');
                document.getElementById('createRestaurantForm').reset();
                showSection('myRestaurants');
            } else {
                alert('Σφάλμα: ' + (escapeHTML(data.error) || 'Δεν ήταν δυνατή η δημιουργία'));
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
            if (!container) return;
            if (!reviews || reviews.length === 0) {
                container.innerHTML = '<p>Δεν έχετε υποβάλει κριτικές ακόμα</p>';
                return;
            }
            container.innerHTML = reviews.map(review => `
                <div class="review-item" style="margin-bottom: 1rem;">
                    <h4>${escapeHTML(review.restaurantName)}</h4>
                    <div class="review-header">
                        <span class="review-rating">${generateStars(review.rating)}</span>
                        <span style="color: #999; font-size: 0.9rem;">${new Date(review.createdDate).toLocaleDateString('el-GR')}</span>
                    </div>
                    ${review.reviewText ? `<p>${escapeHTML(review.reviewText)}</p>` : ''}
                    <div class="review-actions">
                        <button onclick="editReview(${review.id}, ${review.rating})">Edit</button>
                        <button onclick="deleteReview(${review.id})">Delete</button>
                    </div>
                </div>
            `).join('');
        })
        .catch(error => {
            console.error('Error loading reviews:', error);
            const container = document.getElementById('my-reviews-list');
            if (container) container.innerHTML = '<p>Σφάλμα κατά την φόρτωση</p>';
        });
}

function deleteReview(id) {
    if (confirm("Θέλετε σίγουρα να διαγράψετε την κριτική;")) {
        fetch(`/api/reviews/${id}`, { method: 'DELETE' })
            .then(response => {
                if (response.ok) {
                    alert("Η κριτική διαγράφηκε!");
                    loadMyReviews(); // Reload the list automatically
                } else {
                    alert("Δεν ήταν δυνατή η διαγραφή.");
                }
            });
    }
}

function editReview(id, currentRating) {
    const reviewItem = event.target.closest('.review-item');
    const textElement = reviewItem.querySelector('p');
    const currentText = textElement ? textElement.innerText : "";

    reviewItem.innerHTML = `
        <div class="edit-form" style="padding: 10px; border: 1px solid #eee;">
            <label>Rating (Click to set): </label>
            <div id="star-picker-${id}" style="cursor: pointer; font-size: 1.5rem; color: gold;">
                <span onclick="setRating(${id}, 1)">${currentRating >= 1 ? '★' : '☆'}</span>
                <span onclick="setRating(${id}, 2)">${currentRating >= 2 ? '★' : '☆'}</span>
                <span onclick="setRating(${id}, 3)">${currentRating >= 3 ? '★' : '☆'}</span>
                <span onclick="setRating(${id}, 4)">${currentRating >= 4 ? '★' : '☆'}</span>
                <span onclick="setRating(${id}, 5)">${currentRating >= 5 ? '★' : '☆'}</span>
            </div>
            <input type="hidden" id="edit-rating-${id}" value="${currentRating}">
            
            <br>
            <label style="display:block; margin-top:10px;">Review Text:</label>
            <textarea id="edit-text-${id}" style="width: 100%; height: 80px;">${currentText}</textarea>
            
            <div style="margin-top: 10px;">
                <button onclick="saveReview(${id})">Save Changes</button>
                <button onclick="location.reload()">Cancel</button>
            </div>
        </div>
    `;
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
            if (tbody) tbody.innerHTML = users.map(user => `<tr><td>${escapeHTML(user.id)}</td><td>${escapeHTML(user.firstName)} ${escapeHTML(user.lastName)}</td><td>${escapeHTML(user.username)}</td><td>${escapeHTML(user.type) || 'User'}</td></tr>`).join('');
        })
        .catch(() => {
            if (document.getElementById('admin-users-list')) document.getElementById('admin-users-list').innerHTML = '<tr><td colspan="4">Σφάλμα</td></tr>';
        });
}

function loadAdminRestaurants() {
    fetch('/api/restaurants')
        .then(response => response.json())
        .then(restaurants => {
            const tbody = document.getElementById('admin-restaurants-list');
            if (tbody) tbody.innerHTML = restaurants.map(r => `<tr><td>${escapeHTML(r.id)}</td><td>${escapeHTML(r.name)}</td><td>${escapeHTML(r.location)}</td><td>${r.averageRating ? r.averageRating.toFixed(1) : '0.0'}/5.0</td><td>${escapeHTML(r.reviewCount) || 0}</td><td><button class="btn-small btn-delete" onclick="adminDeleteRestaurant(${r.id})">🗑️</button></td></tr>`).join('');
        })
        .catch(() => {
            if (document.getElementById('admin-restaurants-list')) document.getElementById('admin-restaurants-list').innerHTML = '<tr><td colspan="6">Σφάλμα</td></tr>';
        });
}

function loadAdminReviews() {
    fetch('/api/restaurants')
        .then(response => response.json())
        .then(restaurants => {
            const allReviews = [];
            let loaded = 0;
            if (restaurants.length === 0) {
                if (document.getElementById('admin-reviews-list')) document.getElementById('admin-reviews-list').innerHTML = '<tr><td colspan="6">Καμία κριτική</td></tr>';
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
                    }).catch(() => {
                    loaded++;
                    if (loaded === restaurants.length) displayAdminReviews(allReviews);
                });
            });
        })
        .catch(() => {
            if (document.getElementById('admin-reviews-list')) document.getElementById('admin-reviews-list').innerHTML = '<tr><td colspan="6">Σφάλμα</td></tr>';
        });
}

function displayAdminReviews(reviews) {
    const tbody = document.getElementById('admin-reviews-list');
    if (!tbody) return;
    if (reviews.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6">Δεν υπάρχουν κριτικές</td></tr>';
        return;
    }
    tbody.innerHTML = reviews.map(r => `<tr><td>${escapeHTML(r.id)}</td><td>${escapeHTML(r.restaurantName)}</td><td>${escapeHTML(r.criticName)}</td><td>${escapeHTML(r.rating)}/5</td><td>${new Date(r.createdDate).toLocaleDateString('el-GR')}</td><td><button class="btn-small btn-delete" onclick="adminDeleteReview(${r.id})">🗑️</button></td></tr>`).join('');
}

function adminDeleteRestaurant(restaurantId) {
    if (confirm('Είστε σίγουροι; Αυτή η ενέργεια δεν αναιρείται.')) {
        // Ο ρόλος ελέγχεται στον server, όχι στο URL
        fetch(`/api/restaurants/${restaurantId}`, { method: 'DELETE' })
            .then(() => loadAdminRestaurants())
            .catch(error => console.error('Error:', error));
    }
}

function adminDeleteReview(reviewId) {
    if (confirm('Είστε σίγουροι;')) {
        // Ο ρόλος ελέγχεται στον server, όχι στο URL
        fetch(`/api/reviews/${reviewId}`, { method: 'DELETE' })
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
    // Επικοινωνία με τον server για να κλείσει πραγματικά το session
    fetch('/api/session/logout', { method: 'POST' })
        .catch(err => console.error('Logout error:', err))
        .finally(() => {
            localStorage.clear();
            window.location.href = '/index.html';
        });
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