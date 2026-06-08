package com.example.restaurantreviewapp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews", uniqueConstraints = {@UniqueConstraint(columnNames = {"restaurant_id", "critic_id"})})
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne
    @JoinColumn(name = "critic_id", nullable = false)
    private Critic critic;

    @Column(nullable = false)
    private Integer rating; // 1-5 stars

    @Column(name = "review_text", length = 2000)
    private String reviewText;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    public Review() {
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
    }

    public Review(Restaurant restaurant, Critic critic, Integer rating, String reviewText) {
        this.restaurant = restaurant;
        this.critic = critic;
        this.rating = rating;
        this.reviewText = reviewText;
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }

    public Restaurant getRestaurant() { return restaurant; }
    public void setRestaurant(Restaurant restaurant) { this.restaurant = restaurant; }

    public Critic getCritic() { return critic; }
    public void setCritic(Critic critic) { this.critic = critic; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedDate = updatedDate; }
}
