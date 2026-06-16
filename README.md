# Restaurant Reviewer Application

A complete web application for restaurant reviews and ratings with a fair ranking algorithm. Built with Spring Boot, JPA/Hibernate, and SQLite.

## Features

### User Roles
- **Critics**: Can browse restaurants, submit reviews with 1-5 star ratings, and view other reviews
- **Owners**: Can create and manage their restaurants, view ratings and reviews
- **Administrators**: Can manage users, restaurants, and reviews; moderate content
- **Guests**: Can browse restaurants and view reviews without creating an account

### Core Functionality

#### Fair Ranking Algorithm
Restaurants are ranked using a sophisticated algorithm that combines:
- **Average Rating (50%)**: Direct star rating from critics
- **Review Count (20%)**: Normalized using logarithmic scale to prevent new restaurants from being buried
- **Critic Reputation (20%)**: Weight given to critics with more review history
- **Recency Factor (10%)**: Slight boost for recently reviewed restaurants to keep rankings fresh

Formula: `final_score = (rating_factor × 0.5) + (review_count_factor × 0.2) + (reputation_factor × 0.2) + (recency_factor × 0.1) × 10`

#### Restaurant Management
- Owners can create unlimited restaurants
- Each restaurant displays: name, location, cuisine type, phone number, description
- Real-time average rating and review count updates
- Search functionality by name or location

#### Review System
- Critics can submit one review per restaurant
- Reviews include 1-5 star rating and optional text
- Reviews are sortable by newest first
- Critics can see all their submitted reviews in their dashboard

#### Admin Dashboard
- View all users (Critics, Owners, Admins)
- Manage all restaurants across the platform
- Moderate reviews - delete inappropriate content
- View system statistics

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.5.14
- **ORM**: Spring Data JPA with Hibernate
- **Database**: SQLite with H2 compatibility
- **Java Version**: 17

### Frontend
- **HTML5** with semantic markup
- **CSS3** with responsive design
- **JavaScript (Vanilla)** - no frameworks required
- **RESTful API** communication

## Project Structure

```
src/main/java/com/example/restaurantreviewapp/
├── User.java                    # Base user class
├── Critic.java                 # Critic subclass
├── Owner.java                  # Owner subclass
├── Admin.java                  # Admin subclass
├── Restaurant.java             # Restaurant entity
├── Review.java                 # Review entity
├── RestaurantRepository.java    # Restaurant data access
├── ReviewRepository.java        # Review data access
├── CriticRepository.java        # Critic data access
├── OwnerRepository.java         # Owner data access
├── UserRepository.java          # User data access
├── RankingService.java         # Fair ranking algorithm
├── StatisticsService.java      # User statistics tracking
├── RestaurantController.java   # Restaurant API endpoints
├── ReviewController.java       # Review API endpoints
├── UserController.java         # User API endpoints
├── LoginController.java        # Authentication
├── RegistrationController.java # User registration
└── DataInitializer.java        # Sample data

src/main/resources/static/
├── index.html                  # Login page
├── dashboard.html              # Main dashboard (role-based)
├── mainPage.html              # Guest browsing page
├── signUpCritic.html          # Critic registration
├── signUpOwner.html           # Owner registration
├── success.html               # Registration success
├── js/
│   └── dashboard.js           # Dashboard functionality
└── styles/
    ├── index.css              # Login styles
    └── dashboard.css          # Dashboard styles
```

## API Endpoints

### Restaurants
- `GET /api/restaurants` - List all restaurants (ranked)
- `GET /api/restaurants?search=term` - Search restaurants
- `GET /api/restaurants/{id}` - Get restaurant details
- `POST /api/restaurants/create` - Create new restaurant (Owner)
- `PUT /api/restaurants/{id}` - Update restaurant (Owner)
- `DELETE /api/restaurants/{id}` - Delete restaurant (Owner)
- `GET /api/restaurants/owner/{ownerId}` - Get owner's restaurants

### Reviews
- `GET /api/reviews/restaurant/{restaurantId}` - Get restaurant reviews
- `GET /api/reviews/critic/{criticId}` - Get critic's reviews
- `POST /api/reviews/create` - Submit review (Critic)
- `PUT /api/reviews/{id}` - Update review (Critic)
- `DELETE /api/reviews/{id}` - Delete review (Critic or Admin)
- `GET /api/reviews/check` - Check if critic reviewed restaurant

### Users
- `GET /api/users` - List all users (Admin)
- `GET /api/users/{id}` - Get user details (Admin)

### Authentication
- `POST /api/login` - User login
- `POST /api/register/critic` - Register as critic
- `POST /api/register/owner` - Register as owner

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher

### Running the Application

1. Clone the repository
2. Navigate to the project directory
3. Build the project:
   ```bash
   mvn clean package
   ```
4. Run the application:
   ```bash
   mvn spring-boot:run
   ```
5. Open your browser and navigate to `http://localhost:8080`

### Sample Admin Credentials
- Username: `admin1` | Password: `123`
- Username: `admin2` | Password: `password456`

## Security Features

- SHA-256 password hashing
- Session-based authentication
- Role-based access control
- Authorization checks on all modify operations
- Unique constraint on critic-restaurant reviews
- Input validation on all endpoints

## Database Schema

### Users Table (Base)
- id (Primary Key)
- username (Unique)
- password (hashed)
- firstName, lastName
- dtype (discriminator for inheritance)

### Critics Table
- Inherits from Users
- totalReviews (tracking)

### Owners Table
- Inherits from Users
- totalRestaurants (tracking)

### Restaurants Table
- id (Primary Key)
- name, location, description
- cuisineType, phoneNumber
- owner_id (Foreign Key)
- averageRating, reviewCount
- createdDate, updatedDate

### Reviews Table
- id (Primary Key)
- restaurant_id (Foreign Key)
- critic_id (Foreign Key)
- rating (1-5)
- reviewText
- createdDate, updatedDate
- Unique constraint on (restaurant_id, critic_id)

## Future Enhancements

1. **Advanced Filtering**: Filter by rating range, cuisine type, location radius
2. **User Profiles**: View critic reputation badges, owner statistics
3. **Recommendations**: ML-based restaurant recommendations
4. **Analytics Dashboard**: View trends, popular restaurants, top critics
5. **Email Notifications**: Notify owners of new reviews
6. **Review Moderation**: Approve/reject reviews before publishing
7. **Photo Uploads**: Add restaurant and review photos
8. **Maps Integration**: Show restaurant locations on map
9. **Mobile App**: Native iOS/Android applications
10. **API Documentation**: Swagger/OpenAPI integration

## Known Limitations

1. Authentication is session-based; no JWT implementation
2. No CSRF protection (can be added with Spring Security)
3. Limited sorting options (base implementation)
4. No image upload functionality
5. Single-page application - full page refreshes on navigation
6. No database backups or migrations

## Testing

Run the test suite:
```bash
mvn test
```

## Database File

The application uses SQLite with the database file `reviewsApp.db` in the project root directory. This file is automatically created on first run.

## Notes

- All dates and times are stored in UTC
- Review counts and statistics are updated in real-time
- The ranking algorithm is deterministic and reproducible
- Guest mode allows browsing without login
- Reviews cannot be changed after submission (can only be deleted)

## Support

For issues or questions, please contact the development team.

## License

This project is part of a restaurant review system educational initiative.
