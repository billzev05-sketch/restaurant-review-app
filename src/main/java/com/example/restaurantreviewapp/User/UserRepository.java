package com.example.restaurantreviewapp.User;

import com.example.restaurantreviewapp.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Μέθοδος για γρήγορο έλεγχο στη βάση για όνομα που υπάρχει ήδη
    Optional<User> findByUsername(String username);
}