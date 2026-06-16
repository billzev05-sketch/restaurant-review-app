package com.example.restaurantreviewapp.Restaurant;

import com.example.restaurantreviewapp.Owner.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    // Find restaurants by owner
    List<Restaurant> findByOwner(Owner owner);

    // Find restaurant by name
    Optional<Restaurant> findByName(String name);

    // Search restaurants by name or location
    //@Query("SELECT r FROM Restaurant r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.location) LIKE LOWER(CONCAT('%', :search, '%'))")
    //List<Restaurant> searchByNameOrLocation(@Param("search") String search);

    @Override
    List<Restaurant> findAll();


    // Find restaurants by cuisine type
    List<Restaurant> findByCuisineType(String cuisineType);

    // Get all restaurants sorted by average rating (for ranking)
    @Query("SELECT r FROM Restaurant r ORDER BY r.averageRating DESC, r.reviewCount DESC")
    List<Restaurant> findAllSortedByRating();
}
