package com.example.restaurantreviewapp;

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

    // Get all restaurants ranked
    @GetMapping
    @ResponseBody
    public ResponseEntity<?> getAllRestaurants(@RequestParam(required = false) String search) {
        List<Restaurant> restaurants;
        if (search != null && !search.isEmpty()) {
            restaurants = restaurantRepository.searchByNameOrLocation(search);
        } else {
            restaurants = rankingService.getRankedRestaurants();
        }
        return ResponseEntity.ok(formatRestaurantList(restaurants));
    }

    // Get restaurant by ID
    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> getRestaurantById(@PathVariable Long id) {
        Optional<Restaurant> restaurant = restaurantRepository.findById(id);
        if (restaurant.isPresent()) {
            return ResponseEntity.ok(formatRestaurant(restaurant.get()));
        }
        return ResponseEntity.notFound().build();
    }

    // Create new restaurant (Owner only)
    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<?> createRestaurant(
            @RequestParam String name,
            @RequestParam String location,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String cuisineType,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam Long ownerId) {

        Optional<Owner> ownerOpt = ownerRepository.findById(ownerId);
        if (!ownerOpt.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Owner not found"));
        }

        // Check if restaurant name already exists
        if (restaurantRepository.findByName(name).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Restaurant name already exists"));
        }

        Restaurant restaurant = new Restaurant(name, location, description, cuisineType, ownerOpt.get());
        if (phoneNumber != null) {
            restaurant.setPhoneNumber(phoneNumber);
        }

        Restaurant saved = restaurantRepository.save(restaurant);
        return ResponseEntity.ok(formatRestaurant(saved));
    }

    // Update restaurant (Owner only)
    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> updateRestaurant(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String cuisineType,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam Long ownerId) {

        Optional<Restaurant> restaurantOpt = restaurantRepository.findById(id);
        if (!restaurantOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Restaurant restaurant = restaurantOpt.get();

        // Check authorization
        if (!restaurant.getOwner().getId().equals(ownerId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Not authorized to update this restaurant"));
        }

        if (name != null) restaurant.setName(name);
        if (location != null) restaurant.setLocation(location);
        if (description != null) restaurant.setDescription(description);
        if (cuisineType != null) restaurant.setCuisineType(cuisineType);
        if (phoneNumber != null) restaurant.setPhoneNumber(phoneNumber);

        restaurant.setUpdatedDate(java.time.LocalDateTime.now());
        Restaurant updated = restaurantRepository.save(restaurant);

        return ResponseEntity.ok(formatRestaurant(updated));
    }

    // Delete restaurant (Owner only, if no reviews)
    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteRestaurant(@PathVariable Long id, @RequestParam Long ownerId) {
        Optional<Restaurant> restaurantOpt = restaurantRepository.findById(id);
        if (!restaurantOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Restaurant restaurant = restaurantOpt.get();

        // Check authorization
        if (!restaurant.getOwner().getId().equals(ownerId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Not authorized to delete this restaurant"));
        }

        // Check if restaurant has reviews
        long reviewCount = reviewRepository.countByRestaurant(restaurant);
        if (reviewCount > 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot delete restaurant with existing reviews"));
        }

        restaurantRepository.delete(restaurant);
        return ResponseEntity.ok(Map.of("message", "Restaurant deleted successfully"));
    }

    // Get restaurants by owner
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

    // Helper methods
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
}
