package com.example.restaurantreviewapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private CriticRepository criticRepository;

    @Autowired
    private RankingService rankingService;

    @Autowired
    private StatisticsService statisticsService;

    // Get all reviews for a restaurant
    @GetMapping("/restaurant/{restaurantId}")
    @ResponseBody
    public ResponseEntity<?> getRestaurantReviews(@PathVariable Long restaurantId) {
        Optional<Restaurant> restaurantOpt = restaurantRepository.findById(restaurantId);
        if (!restaurantOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        List<Review> reviews = reviewRepository.findByRestaurantOrderByNewest(restaurantOpt.get());
        return ResponseEntity.ok(formatReviewList(reviews));
    }

    // Get all reviews by a critic
    @GetMapping("/critic/{criticId}")
    @ResponseBody
    public ResponseEntity<?> getCriticReviews(@PathVariable Long criticId) {
        Optional<Critic> criticOpt = criticRepository.findById(criticId);
        if (!criticOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        List<Review> reviews = reviewRepository.findByCritic(criticOpt.get());
        return ResponseEntity.ok(formatReviewList(reviews));
    }

    // Submit a new review
    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<?> createReview(
            @RequestParam Long restaurantId,
            @RequestParam Long criticId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String reviewText) {

        // Validate rating
        if (rating < 1 || rating > 5) {
            return ResponseEntity.badRequest().body(Map.of("error", "Rating must be between 1 and 5"));
        }

        Optional<Restaurant> restaurantOpt = restaurantRepository.findById(restaurantId);
        Optional<Critic> criticOpt = criticRepository.findById(criticId);

        if (!restaurantOpt.isPresent() || !criticOpt.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Restaurant or Critic not found"));
        }

        Restaurant restaurant = restaurantOpt.get();
        Critic critic = criticOpt.get();

        // Check if critic already reviewed this restaurant
        Optional<Review> existingReview = reviewRepository.findByRestaurantAndCritic(restaurant, critic);
        if (existingReview.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "You have already reviewed this restaurant"));
        }

        Review review = new Review(restaurant, critic, rating, reviewText);
        Review saved = reviewRepository.save(review);

        // Update restaurant rating
        rankingService.updateRestaurantRating(restaurant);
        
        // Update critic statistics
        statisticsService.updateCriticStatistics(critic);

        return ResponseEntity.ok(formatReview(saved));
    }

    // Update a review
    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> updateReview(
            @PathVariable Long id,
            @RequestParam Long criticId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String reviewText) {

        Optional<Review> reviewOpt = reviewRepository.findById(id);
        if (!reviewOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Review review = reviewOpt.get();

        // Check authorization
        if (!review.getCritic().getId().equals(criticId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Not authorized to update this review"));
        }

        if (rating != null) {
            if (rating < 1 || rating > 5) {
                return ResponseEntity.badRequest().body(Map.of("error", "Rating must be between 1 and 5"));
            }
            review.setRating(rating);
        }

        if (reviewText != null) {
            review.setReviewText(reviewText);
        }

        review.setUpdatedDate(java.time.LocalDateTime.now());
        Review updated = reviewRepository.save(review);

        // Update restaurant rating
        rankingService.updateRestaurantRating(review.getRestaurant());

        return ResponseEntity.ok(formatReview(updated));
    }

    // Delete a review
    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteReview(@PathVariable Long id, @RequestParam Long userId, @RequestParam String userRole) {
        Optional<Review> reviewOpt = reviewRepository.findById(id);
        if (!reviewOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Review review = reviewOpt.get();

        // Check authorization (critic who wrote it, or admin)
        if (!"admin".equals(userRole) && !review.getCritic().getId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Not authorized to delete this review"));
        }

        Restaurant restaurant = review.getRestaurant();
        Critic critic = review.getCritic();
        
        reviewRepository.delete(review);

        // Update restaurant rating
        rankingService.updateRestaurantRating(restaurant);
        
        // Update critic statistics
        statisticsService.updateCriticStatistics(critic);

        return ResponseEntity.ok(Map.of("message", "Review deleted successfully"));
    }

    // Check if critic has reviewed restaurant
    @GetMapping("/check")
    @ResponseBody
    public ResponseEntity<?> checkReview(@RequestParam Long restaurantId, @RequestParam Long criticId) {
        Optional<Restaurant> restaurantOpt = restaurantRepository.findById(restaurantId);
        Optional<Critic> criticOpt = criticRepository.findById(criticId);

        if (!restaurantOpt.isPresent() || !criticOpt.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Restaurant or Critic not found"));
        }

        Optional<Review> review = reviewRepository.findByRestaurantAndCritic(restaurantOpt.get(), criticOpt.get());
        if (review.isPresent()) {
            return ResponseEntity.ok(Map.of("hasReviewed", true, "review", formatReview(review.get())));
        }

        return ResponseEntity.ok(Map.of("hasReviewed", false));
    }

    // Helper methods
    private Map<String, Object> formatReview(Review review) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", review.getId());
        data.put("restaurantId", review.getRestaurant().getId());
        data.put("restaurantName", review.getRestaurant().getName());
        data.put("criticId", review.getCritic().getId());
        data.put("criticName", review.getCritic().getFirstName() + " " + review.getCritic().getLastName());
        data.put("rating", review.getRating());
        data.put("reviewText", review.getReviewText());
        data.put("createdDate", review.getCreatedDate());
        data.put("updatedDate", review.getUpdatedDate());
        return data;
    }

    private List<Map<String, Object>> formatReviewList(List<Review> reviews) {
        return reviews.stream().map(this::formatReview).toList();
    }
}
