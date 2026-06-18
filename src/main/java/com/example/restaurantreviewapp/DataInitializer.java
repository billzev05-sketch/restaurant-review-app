package com.example.restaurantreviewapp;

import com.example.restaurantreviewapp.Admin.Admin;
import com.example.restaurantreviewapp.User.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;
//Αρχείο για δημιουργία των default administrators του συστήματος

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error initialization hashing", e);
        }
    }

    @Override
    public void run(String... args) throws Exception {

        if (userRepository.findByUsername("admin1").isEmpty()) {
            Admin admin1 = new Admin("admin1", hashPassword("123"), "System", "Admin");
            userRepository.save(admin1);
            System.out.println(">>Default administrator 1 added to the database!");
        }


        if (userRepository.findByUsername("admin2").isEmpty()) {
            Admin admin2 = new Admin("admin2", hashPassword("password456"), "George", "Manager");
            userRepository.save(admin2);
            System.out.println(">>Default administrator 2 added to the database!");
        }
    }




}
