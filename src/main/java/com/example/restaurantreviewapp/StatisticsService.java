package com.example.restaurantreviewapp;

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

    /**
     * Update critic's total review count
     */
    public void updateCriticStatistics(Critic critic) {
        long reviewCount = reviewRepository.findByCritic(critic).size();
        critic.setTotalReviews((int) reviewCount);
        criticRepository.save(critic);
    }

    /**
     * Update owner's total restaurant count
     */
    public void updateOwnerStatistics(Owner owner) {
        long restaurantCount = restaurantRepository.findByOwner(owner).size();
        owner.setTotalRestaurants((int) restaurantCount);
        ownerRepository.save(owner);
    }
}
