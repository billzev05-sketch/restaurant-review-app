package com.example.restaurantreviewapp;

import com.example.restaurantreviewapp.Admin.Admin;
import com.example.restaurantreviewapp.User.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    // Αρχείο για δημιουργία των default administrators του συστήματος

    @Override
    public void run(String... args) throws Exception {

        if (userRepository.findByUsername("admin1").isEmpty()) {
            // Δημιουργία admin με ασφαλές BCrypt hashing
            String hashedPass1 = BCrypt.hashpw("123", BCrypt.gensalt());
            Admin admin1 = new Admin("admin1", hashedPass1, "System", "Admin");
            userRepository.save(admin1);
            System.out.println(">>Default administrator 1 added to the database!");
        }

        if (userRepository.findByUsername("admin2").isEmpty()) {
            // Δημιουργία admin με ασφαλές BCrypt hashing
            String hashedPass2 = BCrypt.hashpw("password456", BCrypt.gensalt());
            Admin admin2 = new Admin("admin2", hashedPass2, "George", "Manager");
            userRepository.save(admin2);
            System.out.println(">>Default administrator 2 added to the database!");
        }
    }
}