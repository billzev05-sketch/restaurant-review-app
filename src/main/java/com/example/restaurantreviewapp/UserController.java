package com.example.restaurantreviewapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @ResponseBody
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> userList = new ArrayList<>();

        for (User user : users) {
            Map<String, Object> userData = new LinkedHashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("firstName", user.getFirstName());
            userData.put("lastName", user.getLastName());

            if (user instanceof Admin) {
                userData.put("type", "Admin");
            } else if (user instanceof Critic) {
                userData.put("type", "Critic");
                userData.put("totalReviews", ((Critic) user).getTotalReviews());
            } else if (user instanceof Owner) {
                userData.put("type", "Owner");
                userData.put("totalRestaurants", ((Owner) user).getTotalRestaurants());
            } else {
                userData.put("type", "User");
            }

            userList.add(userData);
        }

        return ResponseEntity.ok(userList);
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (!userOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        Map<String, Object> userData = new LinkedHashMap<>();
        userData.put("id", user.getId());
        userData.put("username", user.getUsername());
        userData.put("firstName", user.getFirstName());
        userData.put("lastName", user.getLastName());

        if (user instanceof Admin) {
            userData.put("type", "Admin");
        } else if (user instanceof Critic) {
            userData.put("type", "Critic");
            userData.put("totalReviews", ((Critic) user).getTotalReviews());
        } else if (user instanceof Owner) {
            userData.put("type", "Owner");
            userData.put("totalRestaurants", ((Owner) user).getTotalRestaurants());
        } else {
            userData.put("type", "User");
        }

        return ResponseEntity.ok(userData);
    }
}
