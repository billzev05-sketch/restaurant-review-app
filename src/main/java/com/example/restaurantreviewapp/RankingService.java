package com.example.restaurantreviewapp;

import com.example.restaurantreviewapp.Critic.Critic;
import com.example.restaurantreviewapp.Critic.CriticRepository;
import com.example.restaurantreviewapp.Restaurant.Restaurant;
import com.example.restaurantreviewapp.Restaurant.RestaurantRepository;
import com.example.restaurantreviewapp.Review.Review;
import com.example.restaurantreviewapp.Review.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RankingService {

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private CriticRepository criticRepository;

    /**
     * Calculate a fair ranking score for restaurants based on:
     * 1. Average rating (50% weight)
     * 2. Number of reviews - normalized (20% weight)
     * 3. Average reviewer reputation (20% weight)
     * 4. Recency factor (10% weight)
     */
    public Double calculateRankingScore(Restaurant restaurant) {
        Double averageRating = getAverageRating(restaurant);
        Integer reviewCount = Math.toIntExact(reviewRepository.countByRestaurant(restaurant));

        if (reviewCount == 0) {
            return 0.0;
        }

        // Factor 1: Average Rating (0.5 weight) - normalized to 0-1
        Double ratingFactor = (averageRating / 5.0) * 0.5;

        // Factor 2: Review Count (0.2 weight) - normalized with logarithmic scale
        Double reviewCountFactor = (Math.log10(reviewCount + 1) / 2.0) * 0.2; // Max log10(101)≈2

        // Factor 3: Critic Reputation (0.2 weight)
        Double criticReputationFactor = calculateCriticReputationFactor(restaurant);

        // Factor 4: Recency Factor (0.1 weight)
        Double recencyFactor = calculateRecencyFactor(restaurant);

        // Combined score (0-10 scale)
        Double finalScore = (ratingFactor + reviewCountFactor + criticReputationFactor + recencyFactor) * 10;

        return Math.min(finalScore, 10.0); // Cap at 10
    }

    /**
     * Calculate average rating for a restaurant
     */
    public Double getAverageRating(Restaurant restaurant) {
        Optional<Double> avgRating = reviewRepository.getAverageRatingForRestaurant(restaurant);
        return avgRating.orElse(0.0);
    }

    /**
     * Calculate critic reputation factor based on their review count and quality
     */
    private Double calculateCriticReputationFactor(Restaurant restaurant) {
        List<Review> reviews = reviewRepository.findByRestaurant(restaurant);

        if (reviews.isEmpty()) {
            return 0.0;
        }

        Double avgCriticReputation = reviews.stream()
                .mapToDouble(review -> {
                    Critic critic = review.getCritic();
                    // Reputation based on number of reviews the critic has made
                    long criticReviewCount = reviewRepository.findByCritic(critic).size();
                    // Normalize: critics with 50+ reviews have max reputation
                    return Math.min((double) criticReviewCount / 50.0, 1.0);
                })
                .average()
                .orElse(0.0);

        return avgCriticReputation * 0.2;
    }

    /**
     * Calculate recency factor - newer reviews slightly boost ranking
     */
    private Double calculateRecencyFactor(Restaurant restaurant) {
        List<Review> reviews = reviewRepository.findByRestaurant(restaurant);

        if (reviews.isEmpty()) {
            return 0.0;
        }

        // Get the most recent review
        long mostRecentTimeMillis = reviews.stream()
                .mapToLong(r -> r.getCreatedDate().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
                .max()
                .orElse(0L);

        long nowMillis = System.currentTimeMillis();
        long daysSinceLastReview = (nowMillis - mostRecentTimeMillis) / (1000 * 60 * 60 * 24);

        // Reviews within last 30 days get slight boost, decays over time
        Double recencyScore = Math.max(0.0, 1.0 - (daysSinceLastReview / 365.0)) * 0.1;

        return recencyScore;
    }

    /**
     * Get all restaurants ranked by fair ranking algorithm
     */
    public List<Restaurant> getRankedRestaurants() {
        List<Restaurant> allRestaurants = restaurantRepository.findAll();

        return allRestaurants.stream()
                .sorted((r1, r2) -> Double.compare(
                        calculateRankingScore(r2),
                        calculateRankingScore(r1)
                ))
                .collect(Collectors.toList());
    }

    /**
     * Update restaurant rating and review count
     */
    public void updateRestaurantRating(Restaurant restaurant) {
        Double avgRating = getAverageRating(restaurant);
        long reviewCount = reviewRepository.countByRestaurant(restaurant);

        restaurant.setAverageRating(avgRating);
        restaurant.setReviewCount((int) reviewCount);
        restaurant.setUpdatedDate(java.time.LocalDateTime.now());

        restaurantRepository.save(restaurant);
    }
}
