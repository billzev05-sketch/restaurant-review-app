package com.example.restaurantreviewapp.Review;

import com.example.restaurantreviewapp.*;
import com.example.restaurantreviewapp.Critic.Critic;
import com.example.restaurantreviewapp.Critic.CriticRepository;
import com.example.restaurantreviewapp.Restaurant.Restaurant;
import com.example.restaurantreviewapp.Restaurant.RestaurantRepository;
import jakarta.servlet.http.HttpSession;
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

    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<?> createReview(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {

        Long sessionUserId = (Long) session.getAttribute("userId");
        String sessionRole = (String) session.getAttribute("role");

        if (sessionUserId == null || !"critic".equals(sessionRole)) {
            return ResponseEntity.status(401).body(Map.of("error", "Μόνο κριτικοί μπορούν να υποβάλουν κριτική."));
        }

        if (payload.get("restaurantId") == null || payload.get("rating") == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Λείπουν υποχρεωτικά πεδία."));
        }

        Long restaurantId = Long.valueOf(payload.get("restaurantId").toString());
        Integer rating = Integer.valueOf(payload.get("rating").toString());
        String reviewText = payload.get("reviewText") != null ? (String) payload.get("reviewText") : null;

        if (rating < 1 || rating > 5) {
            return ResponseEntity.badRequest().body(Map.of("error", "Το rating πρέπει να είναι από 1 έως 5."));
        }

        Optional<Restaurant> restaurantOpt = restaurantRepository.findById(restaurantId);
        Optional<Critic> criticOpt = criticRepository.findById(sessionUserId);

        if (!restaurantOpt.isPresent() || !criticOpt.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Το εστιατόριο ή ο κριτικός δεν βρέθηκαν."));
        }

        Restaurant restaurant = restaurantOpt.get();
        Critic critic = criticOpt.get();

        Optional<Review> existingReview = reviewRepository.findByRestaurantAndCritic(restaurant, critic);
        if (existingReview.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Έχετε ήδη αξιολογήσει αυτό το εστιατόριο."));
        }

        Review review = new Review(restaurant, critic, rating, reviewText);
        Review saved = reviewRepository.save(review);

        rankingService.updateRestaurantRating(restaurant);
        statisticsService.updateCriticStatistics(critic);

        return ResponseEntity.ok(formatReview(saved));
    }

    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> updateReview(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            HttpSession session) {

        Long sessionUserId = (Long) session.getAttribute("userId");

        if (sessionUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Μη εξουσιοδοτημένη πρόσβαση."));
        }

        Optional<Review> reviewOpt = reviewRepository.findById(id);
        if (!reviewOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Review review = reviewOpt.get();

        if (!review.getCritic().getId().equals(sessionUserId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Δεν έχετε δικαίωμα να επεξεργαστείτε αυτή την κριτική."));
        }

        if (payload.get("rating") != null) {
            Integer rating = Integer.valueOf(payload.get("rating").toString());
            if (rating < 1 || rating > 5) {
                return ResponseEntity.badRequest().body(Map.of("error", "Rating must be between 1 and 5"));
            }
            review.setRating(rating);
        }

        if (payload.get("reviewText") != null) {
            review.setReviewText((String) payload.get("reviewText"));
        }

        review.setUpdatedDate(java.time.LocalDateTime.now());
        Review updated = reviewRepository.save(review);

        rankingService.updateRestaurantRating(review.getRestaurant());

        return ResponseEntity.ok(formatReview(updated));
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteReview(@PathVariable Long id, HttpSession session) {

        Long sessionUserId = (Long) session.getAttribute("userId");
        String sessionRole = (String) session.getAttribute("role");

        if (sessionUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Πρέπει να συνδεθείτε."));
        }

        Optional<Review> reviewOpt = reviewRepository.findById(id);
        if (!reviewOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Review review = reviewOpt.get();

        boolean isAdmin = "admin".equals(sessionRole);
        boolean isAuthor = review.getCritic().getId().equals(sessionUserId);

        if (!isAdmin && !isAuthor) {
            return ResponseEntity.status(403).body(Map.of("error", "Δεν έχετε δικαίωμα διαγραφής αυτής της κριτικής."));
        }

        Restaurant restaurant = review.getRestaurant();
        Critic critic = review.getCritic();

        reviewRepository.delete(review);

        rankingService.updateRestaurantRating(restaurant);
        statisticsService.updateCriticStatistics(critic);

        return ResponseEntity.ok(Map.of("message", "Η κριτική διαγράφηκε με επιτυχία"));
    }

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