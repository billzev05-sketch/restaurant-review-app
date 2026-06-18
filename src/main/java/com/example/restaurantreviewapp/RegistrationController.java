package com.example.restaurantreviewapp;

import com.example.restaurantreviewapp.Critic.Critic;
import com.example.restaurantreviewapp.Critic.CriticRepository;
import com.example.restaurantreviewapp.Owner.Owner;
import com.example.restaurantreviewapp.Owner.OwnerRepository;
import com.example.restaurantreviewapp.User.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistrationController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CriticRepository criticRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @PostMapping("/api/register/critic")
    public String registerCritic(@RequestParam String username,
                                 @RequestParam String password,
                                 @RequestParam String firstName,
                                 @RequestParam String lastName,
                                 RedirectAttributes redirectAttributes) {

        if (userRepository.findByUsername(username).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Το όνομα χρήστη χρησιμοποιείται ήδη!");
            return "redirect:/signUpCritic.html?error";
        }

        // Hashing με BCrypt
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
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
            redirectAttributes.addFlashAttribute("errorMessage", "Το όνομα χρήστη χρησιμοποιείται ήδη!");
            return "redirect:/signUpOwner.html?error";
        }

        // Hashing με BCrypt
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        Owner owner = new Owner(username, hashedPassword, firstName, lastName, 0);
        ownerRepository.save(owner);

        return "redirect:/success.html";
    }
}