# Restaurant Reviewer - Quick Start Guide

## Running the Application

```bash
# Navigate to the project directory
cd restaurant-reviewer

# Build and run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Demo Accounts

### Admin Accounts
- **Username**: `admin1` | **Password**: `123`
- **Username**: `admin2` | **Password**: `password456`

### Testing Workflow

#### 1. Create a Critic Account
1. Go to http://localhost:8080
2. Click "Εγγραφή" (or sign up as Critic)
3. Fill in: Username, Password, First Name, Last Name
4. Click "Εγγραφή"

#### 2. Create an Owner Account
1. Go to http://localhost:8080
2. Select "Ιδιοκτήτης" (Owner) role
3. Click "Εγγραφή" (or sign up as Owner)
4. Fill in account details
5. Click "Εγγραφή"

#### 3. Owner: Create a Restaurant
1. Login with Owner credentials
2. Click "➕ Προσθήκη Εστιατορίου" (Add Restaurant)
3. Fill in:
   - Όνομα (Name): e.g., "Taverna Maria"
   - Τοποθεσία (Location): e.g., "Athens, Greece"
   - Περιγραφή (Description): Optional
   - Είδος Κουζίνας (Cuisine): e.g., "Greek"
   - Τηλέφωνο (Phone): Optional
4. Click "Δημιουργία Εστιατορίου" (Create Restaurant)

#### 4. Critic: Browse and Review
1. Login with Critic credentials
2. On the Browse page, you'll see all restaurants ranked by rating
3. Click "Προβολή" (View) on a restaurant
4. Click the stars to rate (1-5 stars)
5. Add review text (optional)
6. Click "Υποβολή Κριτικής" (Submit Review)

#### 5. Admin: Manage System
1. Login with Admin credentials
2. Click "🔧 Admin Panel"
3. View three tabs:
   - **Χρήστες** (Users): All system users
   - **Εστιατόρια** (Restaurants): All restaurants with ratings
   - **Κριτικές** (Reviews): All reviews across system

## Key Features Explained

### Fair Ranking Algorithm
Restaurants are ranked using this formula:
```
Score = (Rating × 0.5) + (Review Count Factor × 0.2) + 
        (Critic Reputation × 0.2) + (Recency × 0.1)
```

**Why it's fair:**
- New restaurants aren't buried if they get high ratings
- Experienced critics' reviews weighted more heavily
- Recent reviews are given slight boost
- No single factor dominates the ranking

### Review System
- **Each critic can review each restaurant once**
- Reviews include a 1-5 star rating and optional text
- You can delete your own reviews
- Admins can delete any review
- Ratings update instantly when reviews are added/deleted

### Restaurant Management (Owners)
- Create unlimited restaurants
- View all your restaurants with current ratings
- Edit restaurant details (description, cuisine, phone)
- Can't delete restaurants that have reviews

### Guest Mode
- Click "Συνέχεια ως απλός επισκέπτης" (Continue as guest) to browse without login
- Can see all restaurants and their ratings
- Must login to submit reviews

## Troubleshooting

### "Λάθος όνομα χρήστη ή κωδικός" (Wrong username or password)
- Make sure you're selecting the correct role (Critic/Owner/Admin)
- Check that username and password are correct

### "Το όνομα χρήστη χρησιμοποιείται ήδη" (Username already taken)
- Choose a different username

### Restaurant doesn't appear after creation
- Refresh the page
- Make sure you logged in as an Owner

### Can't delete a restaurant
- Can't delete restaurants that have reviews
- First, admins must delete all reviews for that restaurant

### Session expires after closing browser
- Session is stored in localStorage
- Clear browser cache if you experience issues

## API Testing

You can test endpoints using curl or Postman:

```bash
# Get all restaurants (ranked)
curl http://localhost:8080/api/restaurants

# Search restaurants
curl http://localhost:8080/api/restaurants?search=Greek

# Get restaurant by ID
curl http://localhost:8080/api/restaurants/1

# Get reviews for a restaurant
curl http://localhost:8080/api/reviews/restaurant/1

# Login as admin (returns to dashboard with user params)
curl -X POST http://localhost:8080/api/login \
  -d "username=admin1&******"
```

## Database

SQLite database file: `reviewsApp.db` (auto-created in project root)

View schema:
```bash
# Using sqlite3 CLI
sqlite3 reviewsApp.db ".schema"

# View tables
sqlite3 reviewsApp.db ".tables"
```

## Development Notes

- Frontend uses vanilla JavaScript (no frameworks)
- Backend uses Spring Boot 3.5.14 with JPA/Hibernate
- Password stored as SHA-256 hash
- All dates in UTC
- Session management via localStorage + HTTP session

## Files Structure

```
src/main/
├── java/com/example/restaurantreviewapp/
│   ├── Entities: User, Critic, Owner, Admin, Restaurant, Review
│   ├── Repositories: UserRepository, CriticRepository, OwnerRepository, RestaurantRepository, ReviewRepository
│   ├── Services: RankingService, StatisticsService
│   ├── Controllers: LoginController, RegistrationController, RestaurantController, ReviewController, UserController
│   └── DataInitializer: Loads default admin accounts
└── resources/static/
    ├── index.html (Login)
    ├── dashboard.html (Main app)
    ├── mainPage.html (Guest mode)
    ├── signUpCritic.html
    ├── signUpOwner.html
    ├── success.html (Registration success)
    ├── js/dashboard.js (Frontend logic)
    └── styles/
        ├── index.css
        └── dashboard.css
```

## Performance Notes

- Ranking calculation is done on-demand (not cached)
- For large datasets (>10k restaurants), consider:
  - Caching ranking results
  - Implementing pagination
  - Adding database indexes

## Security Considerations

For production deployment:
- Enable HTTPS
- Add CSRF protection with Spring Security
- Implement rate limiting
- Add password complexity requirements
- Implement two-factor authentication
- Use environment variables for credentials
- Enable proper CORS if needed

## Common Tasks

### Add a new restaurant as Owner
1. Login as Owner
2. Click "➕ Προσθήκη Εστιατορίου"
3. Fill form and submit

### Review a restaurant as Critic
1. Login as Critic
2. Find restaurant in Browse section
3. Click "Προβολή"
4. Rate and submit review

### Moderate content as Admin
1. Login as Admin
2. Click "🔧 Admin Panel"
3. Go to "Κριτικές" tab
4. Find inappropriate review
5. Click "Διαγ." (Delete)

## Further Help

- Check README.md for detailed documentation
- Review API endpoints in controllers
- Check database schema in entities
- Look at dashboard.js for frontend logic

Enjoy the Restaurant Reviewer application! 🍽️⭐
