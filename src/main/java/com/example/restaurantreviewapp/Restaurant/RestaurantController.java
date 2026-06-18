package com.example.restaurantreviewapp.Restaurant;

import com.example.restaurantreviewapp.*;
import com.example.restaurantreviewapp.Owner.Owner;
import com.example.restaurantreviewapp.Owner.OwnerRepository;
import com.example.restaurantreviewapp.Review.ReviewRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/api/restaurants")
public class RestaurantController {

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private RankingService rankingService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private StatisticsService statisticsService;

    @GetMapping
    @ResponseBody
    public ResponseEntity<?> getAllRestaurants(@RequestParam(required = false) String search) {
        List<Restaurant> restaurants;

        if (search != null && !search.isEmpty()) {
            String cleanSearch = normalizeString(search);
            restaurants = restaurantRepository.findAll().stream()
                    .filter(r -> normalizeString(r.getName()).contains(cleanSearch)
                            || normalizeString(r.getLocation()).contains(cleanSearch))
                    .toList();
        } else {
            restaurants = rankingService.getRankedRestaurants();
        }

        return ResponseEntity.ok(formatRestaurantList(restaurants));
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> getRestaurantById(@PathVariable Long id) {
        Optional<Restaurant> restaurant = restaurantRepository.findById(id);
        if (restaurant.isPresent()) {
            return ResponseEntity.ok(formatRestaurant(restaurant.get()));
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<?> createRestaurant(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {

        Long sessionUserId = (Long) session.getAttribute("userId");
        String sessionRole = (String) session.getAttribute("role");

        if (sessionUserId == null || !"owner".equals(sessionRole)) {
            return ResponseEntity.status(401).body(Map.of("error", "Μόνο ιδιοκτήτες μπορούν να δημιουργήσουν εστιατόριο."));
        }

        String name = (String) payload.get("name");
        String location = (String) payload.get("location");
        String description = (String) payload.get("description");
        String cuisineType = (String) payload.get("cuisineType");
        String phoneNumber = (String) payload.get("phoneNumber");

        if (name == null || name.trim().isEmpty() || location == null || location.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Το όνομα και η τοποθεσία είναι υποχρεωτικά."));
        }

        Optional<Owner> ownerOpt = ownerRepository.findById(sessionUserId);
        if (!ownerOpt.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ο ιδιοκτήτης δεν βρέθηκε."));
        }

        if (restaurantRepository.findByName(name).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Το όνομα του εστιατορίου υπάρχει ήδη."));
        }

        Restaurant restaurant = new Restaurant(name, location, description, cuisineType, ownerOpt.get());
        if (phoneNumber != null) {
            restaurant.setPhoneNumber(phoneNumber);
        }

        Restaurant saved = restaurantRepository.save(restaurant);
        statisticsService.updateOwnerStatistics(ownerOpt.get());

        return ResponseEntity.ok(formatRestaurant(saved));
    }

    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> updateRestaurant(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            HttpSession session) {

        Long sessionUserId = (Long) session.getAttribute("userId");
        String sessionRole = (String) session.getAttribute("role");

        if (sessionUserId == null || !"owner".equals(sessionRole)) {
            return ResponseEntity.status(401).body(Map.of("error", "Μη εξουσιοδοτημένη πρόσβαση."));
        }

        Optional<Restaurant> restaurantOpt = restaurantRepository.findById(id);
        if (!restaurantOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Restaurant restaurant = restaurantOpt.get();

        if (!restaurant.getOwner().getId().equals(sessionUserId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Δεν έχετε δικαίωμα επεξεργασίας σε αυτό το εστιατόριο."));
        }

        if (payload.get("name") != null) restaurant.setName((String) payload.get("name"));
        if (payload.get("location") != null) restaurant.setLocation((String) payload.get("location"));
        if (payload.get("description") != null) restaurant.setDescription((String) payload.get("description"));
        if (payload.get("cuisineType") != null) restaurant.setCuisineType((String) payload.get("cuisineType"));
        if (payload.get("phoneNumber") != null) restaurant.setPhoneNumber((String) payload.get("phoneNumber"));

        restaurant.setUpdatedDate(java.time.LocalDateTime.now());
        Restaurant updated = restaurantRepository.save(restaurant);

        return ResponseEntity.ok(formatRestaurant(updated));
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteRestaurant(@PathVariable Long id, HttpSession session) {

        Long sessionUserId = (Long) session.getAttribute("userId");
        String sessionRole = (String) session.getAttribute("role");

        if (sessionUserId == null || (!"owner".equals(sessionRole) && !"admin".equals(sessionRole))) {
            return ResponseEntity.status(401).body(Map.of("error", "Μη εξουσιοδοτημένη πρόσβαση."));
        }

        Optional<Restaurant> restaurantOpt = restaurantRepository.findById(id);
        if (!restaurantOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Restaurant restaurant = restaurantOpt.get();

        if (!"admin".equals(sessionRole) && !restaurant.getOwner().getId().equals(sessionUserId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Δεν έχετε δικαίωμα διαγραφής αυτού του εστιατορίου."));
        }

        long reviewCount = reviewRepository.countByRestaurant(restaurant);
        if (reviewCount > 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Δεν μπορείτε να διαγράψετε εστιατόριο που έχει κριτικές."));
        }

        Owner owner = restaurant.getOwner();
        restaurantRepository.delete(restaurant);
        statisticsService.updateOwnerStatistics(owner);

        return ResponseEntity.ok(Map.of("message", "Το εστιατόριο διαγράφηκε με επιτυχία."));
    }

    @GetMapping("/owner/{ownerId}")
    @ResponseBody
    public ResponseEntity<?> getRestaurantsByOwner(@PathVariable Long ownerId) {
        Optional<Owner> ownerOpt = ownerRepository.findById(ownerId);
        if (!ownerOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        List<Restaurant> restaurants = restaurantRepository.findByOwner(ownerOpt.get());
        return ResponseEntity.ok(formatRestaurantList(restaurants));
    }

    private Map<String, Object> formatRestaurant(Restaurant restaurant) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", restaurant.getId());
        data.put("name", restaurant.getName());
        data.put("location", restaurant.getLocation());
        data.put("description", restaurant.getDescription());
        data.put("cuisineType", restaurant.getCuisineType());
        data.put("phoneNumber", restaurant.getPhoneNumber());
        data.put("owner", restaurant.getOwner().getUsername());
        data.put("ownerId", restaurant.getOwner().getId());
        data.put("averageRating", restaurant.getAverageRating());
        data.put("reviewCount", restaurant.getReviewCount());
        data.put("rankingScore", rankingService.calculateRankingScore(restaurant));
        data.put("createdDate", restaurant.getCreatedDate());
        return data;
    }

    private List<Map<String, Object>> formatRestaurantList(List<Restaurant> restaurants) {
        return restaurants.stream().map(this::formatRestaurant).toList();
    }

    private String normalizeString(String input) {
        if (input == null) return "";
        String normalized = input.toLowerCase();
        normalized = normalized
                .replace("ά", "α").replace("έ", "ε").replace("ή", "η")
                .replace("ί", "ι").replace("ό", "ο").replace("ύ", "υ")
                .replace("ώ", "ω").replace("ϊ", "ι").replace("ϋ", "υ")
                .replace("ΐ", "ι").replace("ΰ", "υ");
        return normalized.trim();
    }
}