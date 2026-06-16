package com.example.restaurantreviewapp.Critic;

import com.example.restaurantreviewapp.Critic.Critic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CriticRepository extends JpaRepository<Critic,Long> {



    @Override
    Optional<Critic> findById(Long aLong);
}
