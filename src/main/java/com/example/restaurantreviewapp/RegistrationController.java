package com.example.restaurantreviewapp;

import com.example.restaurantreviewapp.Critic.Critic;
import com.example.restaurantreviewapp.Critic.CriticRepository;
import com.example.restaurantreviewapp.Owner.Owner;
import com.example.restaurantreviewapp.Owner.OwnerRepository;
import com.example.restaurantreviewapp.User.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Controller
public class RegistrationController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CriticRepository criticRepository;

    @Autowired
    private OwnerRepository ownerRepository;
//Hashing κωικού με χρήση SHA-256
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    @PostMapping("/api/register/critic")
    public String registerCritic(@RequestParam String username,
                                 @RequestParam String password,
                                 @RequestParam String firstName,
                                 @RequestParam String lastName,
                                 RedirectAttributes redirectAttributes) {

        if (userRepository.findByUsername(username).isPresent()) {
            // Σε περίπτωση κοινού ονόματος στέλνεται flag στο frontend
            redirectAttributes.addFlashAttribute("errorMessage", "Το όνομα χρήστη χρησιμοποιείται ήδη!");
            return "redirect:/signUpCritic.html?error";
        }

        String hashedPassword = hashPassword(password);
        Critic critic = new Critic(username, hashedPassword, firstName, lastName, 0);
        criticRepository.save(critic);

        return "redirect:/success.html";
    }

    @PostMapping("/api/register/owner")
    public String registerOwner(@RequestParam String username,
                                @RequestParam String password,
                                @RequestParam String firstName,
                                @RequestParam String lastName,
                                RedirectAttributes redirectAttributes) {

        if (userRepository.findByUsername(username).isPresent()) {
            // Σε περίπτωση κοινού ονόματος στέλνεται flag στο frontend
            redirectAttributes.addFlashAttribute("errorMessage", "Το όνομα χρήστη χρησιμοποιείται ήδη!");
            return "redirect:/signUpOwner.html?error";
        }

        String hashedPassword = hashPassword(password);
        Owner owner = new Owner(username, hashedPassword, firstName, lastName, 0);
        ownerRepository.save(owner);

        return "redirect:/success.html";
    }
}