package com.example.restaurantreviewapp;

import com.example.restaurantreviewapp.Critic.Critic;
import com.example.restaurantreviewapp.Critic.CriticRepository;
import com.example.restaurantreviewapp.Owner.Owner;
import com.example.restaurantreviewapp.Owner.OwnerRepository;
import com.example.restaurantreviewapp.Restaurant.RestaurantRepository;
import com.example.restaurantreviewapp.Review.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatisticsService {

    @Autowired
    private CriticRepository criticRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    public void updateCriticStatistics(Critic critic) {
        // Χρήση της σωστής μεθόδου count (επιστρέφει απλά έναν αριθμό)
        long reviewCount = reviewRepository.countByCritic(critic);
        critic.setTotalReviews((int) reviewCount);
        criticRepository.save(critic);
    }

    public void updateOwnerStatistics(Owner owner) {
        // Χρήση της σωστής μεθόδου count
        long restaurantCount = restaurantRepository.countByOwner(owner);
        owner.setTotalRestaurants((int) restaurantCount);
        ownerRepository.save(owner);
    }
}