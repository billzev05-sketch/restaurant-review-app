package com.example.restaurantreviewapp;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CriticRepository extends JpaRepository<Critic, Long> {
}