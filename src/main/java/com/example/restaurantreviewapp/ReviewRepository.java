package com.example.restaurantreviewapp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    // Find all reviews for a restaurant
    List<Review> findByRestaurant(Restaurant restaurant);

    // Find all reviews by a critic
    List<Review> findByCritic(Critic critic);

    // Check if a critic has already reviewed a restaurant
    Optional<Review> findByRestaurantAndCritic(Restaurant restaurant, Critic critic);

    // Get average rating for a restaurant
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.restaurant = :restaurant")
    Optional<Double> getAverageRatingForRestaurant(@Param("restaurant") Restaurant restaurant);

    // Count reviews for a restaurant
    long countByRestaurant(Restaurant restaurant);

    // Get all reviews sorted by creation date (newest first)
    @Query("SELECT r FROM Review r WHERE r.restaurant = :restaurant ORDER BY r.createdDate DESC")
    List<Review> findByRestaurantOrderByNewest(@Param("restaurant") Restaurant restaurant);
}
