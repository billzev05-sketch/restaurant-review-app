package com.example.restaurantreviewapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

@Controller
public class LoginController {

    @Autowired
    private UserRepository userRepository;

    // Hashing στο password για να δούμε αν ταιριάζει με κάποιο hashed της βάσης
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error processing password", e);
        }
    }

    @PostMapping("/api/login")
    public String handleLogin(@RequestParam String username,
                              @RequestParam String password,
                              @RequestParam String role,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {

        if ("admin".equals(role)) {
            Optional<User> userOpt = userRepository.findByUsername(username);

            if (userOpt.isPresent() && userOpt.get() instanceof Admin) {
                User adminUser = userOpt.get();
                String hashedInputPassword = hashPassword(password);

                if (adminUser.getPassword().equals(hashedInputPassword)) {
                    session.setAttribute("userId", adminUser.getId());
                    session.setAttribute("username", adminUser.getUsername());
                    session.setAttribute("role", "admin");
                    session.setAttribute("firstName", adminUser.getFirstName());
                    session.setAttribute("lastName", adminUser.getLastName());
                    return "redirect:/dashboard.html?userId=" + adminUser.getId() + "&role=admin&username=" + adminUser.getUsername() + 
                           "&firstName=" + adminUser.getFirstName() + "&lastName=" + adminUser.getLastName();
                }
            }
            return "redirect:/index.html?error=invalid_credentials";
        }


        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String hashedInputPassword = hashPassword(password);

            // password match
            if (user.getPassword().equals(hashedInputPassword)) {
                if ("critic".equals(role) && user instanceof Critic) {
                    session.setAttribute("userId", user.getId());
                    session.setAttribute("username", user.getUsername());
                    session.setAttribute("role", "critic");
                    session.setAttribute("firstName", user.getFirstName());
                    session.setAttribute("lastName", user.getLastName());
                    return "redirect:/dashboard.html?userId=" + user.getId() + "&role=critic&username=" + user.getUsername() + 
                           "&firstName=" + user.getFirstName() + "&lastName=" + user.getLastName();
                } else if ("owner".equals(role) && user instanceof Owner) {
                    session.setAttribute("userId", user.getId());
                    session.setAttribute("username", user.getUsername());
                    session.setAttribute("role", "owner");
                    session.setAttribute("firstName", user.getFirstName());
                    session.setAttribute("lastName", user.getLastName());
                    return "redirect:/dashboard.html?userId=" + user.getId() + "&role=owner&username=" + user.getUsername() + 
                           "&firstName=" + user.getFirstName() + "&lastName=" + user.getLastName();
                }
            }
        }

        // Σε περίπτωση που δεν έγινε Match με κάποιο συγκεκριμένο user
        return "redirect:/index.html?error=invalid_credentials";
    }
}